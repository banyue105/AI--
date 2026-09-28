package com.ican.assistant.modules.practicereview;

import static com.ican.assistant.modules.practicereview.PracticeDtos.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ican.assistant.core.ai.PracticeAiGateway;
import com.ican.assistant.modules.decisionsandbox.DecisionScenarioService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PracticeService {
    private final PracticeProjectRepository projects;
    private final PracticeReviewRepository reviews;
    private final PracticeFeedbackRepository feedback;
    private final PracticeRules rules;
    private final PracticeAiGateway ai;
    private final ObjectMapper mapper;
    private final DecisionScenarioService decisions;
    private final TransactionTemplate writes, reads;
    private static final Duration LEASE = Duration.ofSeconds(35);

    public PracticeService(PracticeProjectRepository projects, PracticeReviewRepository reviews,
                           PracticeFeedbackRepository feedback, PracticeRules rules, PracticeAiGateway ai,
                           ObjectMapper mapper, DecisionScenarioService decisions, PlatformTransactionManager manager) {
        this.projects = projects; this.reviews = reviews; this.feedback = feedback;
        this.rules = rules; this.ai = ai; this.mapper = mapper; this.decisions = decisions;
        this.writes = new TransactionTemplate(manager);
        this.reads = new TransactionTemplate(manager); this.reads.setReadOnly(true);
    }

    private <T> T write(Supplier<T> action) { return writes.execute(status -> action.get()); }
    private <T> T read(Supplier<T> action) { return reads.execute(status -> action.get()); }

    public List<ProjectSummary> list() {
        return read(() -> projects.findAllByOrderByUpdatedAtDesc().stream().map(item -> new ProjectSummary(
                item.id, item.title, item.goal, item.inputRevision, item.evidence.size(),
                (int) reviews.countByProjectIdAndExecutionStatus(item.id, "succeeded"), item.updatedAt)).toList());
    }

    public Project create(CreateRequest request) {
        return write(() -> {
            Template template = PracticeTemplates.require(request.templateId());
            PracticeProjectEntity project = new PracticeProjectEntity();
            project.title = rules.text(request.title().trim(), "项目名称", 1, 120);
            project.goal = rules.text(request.goal().trim(), "实践目标", 1, 1000);
            project.templateId = template.id();
            for (Criterion criterion : template.criteria()) {
                PracticeCriterionEntity item = new PracticeCriterionEntity();
                item.project = project; item.title = criterion.title(); item.standard = criterion.standard();
                item.expectedEvidence = criterion.expectedEvidence(); item.required = criterion.required(); item.orderIndex = criterion.order();
                project.criteria.add(item);
            }
            projects.saveAndFlush(project);
            return toProject(project);
        });
    }

    public Project get(String id) { return read(() -> toProject(requireProject(id, false))); }

    public Project update(String id, UpdateRequest request) {
        return write(() -> {
            PracticeProjectEntity project = requireProject(id, true);
            revision(project, request.inputRevision());
            String title = rules.text(request.title().trim(), "项目名称", 1, 120);
            String goal = rules.text(request.goal().trim(), "实践目标", 1, 1000);
            String process = rules.text(request.processNote() == null ? "" : request.processNote(), "过程说明", 0, 5000);
            String metrics = encode(rules.normalizeMetrics(request.metrics()));
            if (!title.equals(project.title) || !goal.equals(project.goal) || !metrics.equals(project.metricsJson) || !process.equals(project.processNote)) {
                project.title = title; project.goal = goal; project.metricsJson = metrics; project.processNote = process;
                touch(project);
            }
            return toProject(project);
        });
    }

    public Suggestions suggest(String id, int expectedRevision) {
        Project snapshot = read(() -> {
            PracticeProjectEntity project = requireProject(id, false); revision(project, expectedRevision); return toProject(project);
        });
        return ai.suggest(snapshot);
    }

    public Project saveCriteria(String id, CriteriaRequest request) {
        return write(() -> {
            PracticeProjectEntity project = requireProject(id, true); revision(project, request.inputRevision());
            if (request.criteria() == null || request.criteria().isEmpty() || request.criteria().size() > 12)
                throw new IllegalArgumentException("验收清单须包含 1–12 项标准。");
            List<Criterion> before = toProject(project).criteria();
            Map<String, PracticeCriterionEntity> existing = new HashMap<>(); project.criteria.forEach(item -> existing.put(item.id, item));
            Set<String> selected = new HashSet<>();
            List<PracticeCriterionEntity> next = new ArrayList<>();
            for (int index = 0; index < request.criteria().size(); index++) {
                CriterionInput input = request.criteria().get(index);
                if (input == null) throw new IllegalArgumentException("验收项不能为空。");
                PracticeCriterionEntity item;
                if (input.id() == null || input.id().isBlank()) {
                    item = new PracticeCriterionEntity(); item.project = project;
                } else {
                    item = existing.get(input.id());
                    if (item == null) throw new IllegalArgumentException("验收项不属于当前项目。");
                }
                if (!selected.add(item.id)) throw new IllegalArgumentException("验收项不能重复。");
                item.title = rules.text(input.title().trim(), "验收名称", 1, 120);
                item.standard = rules.text(input.standard().trim(), "验收标准", 1, 1000);
                item.expectedEvidence = rules.text(input.expectedEvidence().trim(), "材料要求", 1, 1000);
                item.required = input.required(); item.orderIndex = index; next.add(item);
            }
            project.criteria.removeIf(item -> !selected.contains(item.id));
            for (PracticeCriterionEntity item : next) if (!project.criteria.contains(item)) project.criteria.add(item);
            for (PracticeEvidenceEntity item : project.evidence) {
                List<String> original = decodeList(item.criterionIdsJson, String.class);
                List<String> kept = original.stream().filter(selected::contains).toList();
                if (!original.equals(kept)) { item.criterionIdsJson = encode(kept); item.revision++; item.updatedAt = Instant.now(); }
            }
            if (!before.equals(toProject(project).criteria())) touch(project);
            return toProject(project);
        });
    }

    public Project saveEvidence(String id, String evidenceId, EvidenceRequest request) {
        return write(() -> {
            PracticeProjectEntity project = requireProject(id, true); revision(project, request.inputRevision());
            String title = rules.text(request.title().trim(), "证据名称", 1, 120);
            if (!Set.of("note", "config", "log").contains(request.kind())) throw new IllegalArgumentException("请选择说明、配置或日志类型。");
            String content = rules.normalizeContent(request.content());
            validateIds(request.criterionIds(), toProject(project).criteria().stream().map(Criterion::id).toList(), "关联验收项");
            String associations = encode(request.criterionIds().stream().distinct().sorted().toList());
            boolean isNew = evidenceId == null;
            PracticeEvidenceEntity item = isNew ? new PracticeEvidenceEntity() : project.evidence.stream()
                    .filter(evidence -> evidence.id.equals(evidenceId)).findFirst().orElseThrow(() -> new NoSuchElementException("未找到该证据。"));
            boolean changed = isNew || !title.equals(item.title) || !request.kind().equals(item.kind)
                    || !content.equals(item.content) || !associations.equals(item.criterionIdsJson);
            if (changed) {
                item.project = project; item.title = title; item.kind = request.kind(); item.content = content;
                item.criterionIdsJson = associations; item.updatedAt = Instant.now();
                if (isNew) project.evidence.add(item); else item.revision++;
                rules.validateEvidence(toProject(project).evidence()); touch(project);
            }
            return toProject(project);
        });
    }

    public Project removeEvidence(String id, String evidenceId, int expectedRevision) {
        return write(() -> {
            PracticeProjectEntity project = requireProject(id, true); revision(project, expectedRevision);
            if (!project.evidence.removeIf(item -> item.id.equals(evidenceId))) throw new NoSuchElementException("未找到该证据。");
            touch(project); return toProject(project);
        });
    }

    private record Lease(String id, String attemptId, Project snapshot, Review ready) {}

    public Review review(String id, ReviewRequest request) {
        String requestId = rules.uuid(request.requestId());
        String fingerprint = hash(encode(List.of(id, request.inputRevision(), requestId)));
        Lease lease = write(() -> reserve(id, request.inputRevision(), requestId, fingerprint));
        if (lease.ready() != null) return lease.ready();
        try {
            // The model call runs after the reservation transaction has committed.
            Analysis analysis = ai.review(lease.snapshot());
            rules.validateFindings(lease.snapshot(), analysis.findings());
            if (!Set.of("ai", "mock").contains(analysis.source())) throw new IllegalArgumentException("未知检查来源。");
            return write(() -> {
                PracticeProjectEntity project = requireProject(id, true);
                PracticeReviewEntity entity = requireReview(id, lease.id());
                if (!entity.attemptId.equals(lease.attemptId()) || !entity.executionStatus.equals("running"))
                    throw PracticeException.conflict("REQUEST_IN_PROGRESS", "该请求已由另一次重试接管，请使用原请求重新获取结果。");
                entity.resultJson = encode(analysis.findings()); entity.source = analysis.source();
                entity.providerNotice = analysis.providerNotice(); entity.executionStatus = "succeeded";
                entity.finishedAt = Instant.now(); project.updatedAt = entity.finishedAt;
                reviews.saveAndFlush(entity); return toReview(entity, project.inputRevision);
            });
        } catch (RuntimeException exception) {
            write(() -> {
                PracticeProjectEntity ignored = requireProject(id, true);
                PracticeReviewEntity entity = requireReview(id, lease.id());
                if (entity.attemptId.equals(lease.attemptId()) && entity.executionStatus.equals("running")) {
                    entity.executionStatus = "failed"; entity.finishedAt = Instant.now();
                }
                return null;
            });
            if (exception instanceof PracticeException practice) throw practice;
            throw new PracticeException(HttpStatus.UNPROCESSABLE_ENTITY, "REVIEW_FAILED", "本次检查未能完成，材料已保存，请重试。");
        }
    }

    private Lease reserve(String id, int expectedRevision, String requestId, String fingerprint) {
        PracticeProjectEntity project = requireProject(id, true);
        Optional<PracticeReviewEntity> previous = reviews.findByRequestId(requestId);
        PracticeReviewEntity entity;
        if (previous.isPresent()) {
            entity = previous.get();
            if (!entity.projectId.equals(id) || !entity.requestHash.equals(fingerprint))
                throw PracticeException.conflict("IDEMPOTENCY_CONFLICT", "相同请求标识不能用于不同输入。");
            if (entity.executionStatus.equals("succeeded")) return new Lease(entity.id, entity.attemptId, null, toReview(entity, project.inputRevision));
            if (entity.executionStatus.equals("running") && entity.startedAt.plus(LEASE).isAfter(Instant.now()))
                throw PracticeException.conflict("REQUEST_IN_PROGRESS", "材料正在检查，请稍后使用原请求重试。");
        } else {
            revision(project, expectedRevision);
            entity = new PracticeReviewEntity(); entity.projectId = id; entity.inputRevision = project.inputRevision;
            entity.reviewNumber = reviews.findFirstByProjectIdOrderByReviewNumberDesc(id).map(item -> item.reviewNumber + 1).orElse(1);
            entity.snapshotJson = encode(toProject(project)); entity.requestId = requestId; entity.requestHash = fingerprint;
        }
        entity.executionStatus = "running"; entity.attemptId = UUID.randomUUID().toString(); entity.startedAt = Instant.now();
        entity.finishedAt = null; reviews.saveAndFlush(entity);
        return new Lease(entity.id, entity.attemptId, decode(entity.snapshotJson, Project.class), null);
    }

    public List<ReviewSummary> history(String id) {
        return read(() -> {
            PracticeProjectEntity project = requireProject(id, false);
            return reviews.findByProjectIdAndExecutionStatusOrderByReviewNumberDesc(id, "succeeded").stream().map(item -> new ReviewSummary(
                    item.id, item.reviewNumber, item.inputRevision, item.source, item.finishedAt,
                    item.confirmationJson == null ? null : decode(item.confirmationJson, Confirmation.class).confirmedAt(),
                    item.inputRevision != project.inputRevision)).toList();
        });
    }

    public Review getReview(String id, String reviewId) {
        return read(() -> {
            PracticeProjectEntity project = requireProject(id, false);
            PracticeReviewEntity review = requireReview(id, reviewId);
            if (!review.executionStatus.equals("succeeded")) throw PracticeException.conflict("REQUEST_IN_PROGRESS", "该复盘尚未完成，请稍后重试。");
            return toReview(review, project.inputRevision);
        });
    }

    public Review confirm(String id, String reviewId, ConfirmRequest request) {
        return write(() -> {
            PracticeProjectEntity project = requireProject(id, true);
            PracticeReviewEntity entity = requireReview(id, reviewId);
            if (!entity.executionStatus.equals("succeeded")) throw PracticeException.conflict("REQUEST_IN_PROGRESS", "该复盘尚未完成。");
            if (entity.confirmationJson != null) {
                Confirmation saved = decode(entity.confirmationJson, Confirmation.class);
                if (request.inputRevision() == entity.inputRevision && saved.overrides().equals(request.overrides())) return toReview(entity, project.inputRevision);
                throw PracticeException.conflict("REVIEW_ALREADY_CONFIRMED", "该复盘已确认；修改结论请重新检查并生成新版本。");
            }
            revision(project, request.inputRevision()); currentReview(project, entity);
            Project snapshot = decode(entity.snapshotJson, Project.class);
            Set<String> ids = new HashSet<>(snapshot.criteria().stream().map(Criterion::id).toList()), visited = new HashSet<>();
            if (request.overrides() == null || request.overrides().size() > 12) throw new IllegalArgumentException("人工修正格式不正确。");
            for (PracticeDtos.Override override : request.overrides()) {
                if (override == null || !ids.contains(override.criterionId()) || !visited.add(override.criterionId()))
                    throw new IllegalArgumentException("人工修正包含未知或重复验收项。");
                rules.validateFinding(snapshot, new Finding(override.criterionId(), override.verdict(), override.reason(),
                        override.citations(), override.verdict().equals("insufficient") ? "请补充该标准要求的材料。" : null));
            }
            entity.confirmationJson = encode(new Confirmation(Instant.now(), List.copyOf(request.overrides())));
            return toReview(entity, project.inputRevision);
        });
    }

    public List<FeedbackRecord> saveFeedback(String id, FeedbackRequest request) {
        String requestId = rules.uuid(request.requestId());
        String fingerprint = hash(encode(List.of(id, request.reviewId(), request.items(), requestId)));
        return write(() -> {
            PracticeProjectEntity project = requireProject(id, true);
            List<PracticeFeedbackEntity> previous = feedback.findByRequestIdOrderByItemIndex(requestId);
            if (!previous.isEmpty()) {
                if (previous.stream().anyMatch(item -> !item.projectId.equals(id) || !item.requestHash.equals(fingerprint)))
                    throw PracticeException.conflict("IDEMPOTENCY_CONFLICT", "相同反馈请求标识不能用于不同内容。");
                return previous.stream().map(item -> decode(item.payloadJson, FeedbackRecord.class)).toList();
            }
            PracticeReviewEntity review = requireReview(id, request.reviewId()); currentReview(project, review);
            if (!review.executionStatus.equals("succeeded") || review.confirmationJson == null)
                throw new IllegalArgumentException("请先确认当前复盘，再保存反馈。");
            if (request.items() == null || request.items().isEmpty() || request.items().size() > 12) throw new IllegalArgumentException("请选择需要保存的反馈。");
            Project snapshot = decode(review.snapshotJson, Project.class);
            Set<String> targets = new HashSet<>(); List<FeedbackRecord> result = new ArrayList<>();
            for (int index = 0; index < request.items().size(); index++) {
                FeedbackItem input = request.items().get(index);
                if (input == null || !Set.of("ability", "decision").contains(input.target())) throw new IllegalArgumentException("反馈目标不正确。");
                String targetId = input.targetId() == null || input.targetId().isBlank() ? null : rules.text(input.targetId().trim(), "目标对象", 1, 120);
                String targetName = input.targetName() == null ? null : rules.text(input.targetName().trim(), "目标名称", 1, 120);
                if (input.target().equals("ability") && targetName == null)
                    throw new IllegalArgumentException("能力证据建议须填写拟关联技能名称。");
                String key = targetId == null ? "" : targetId;
                if (!targets.add(input.target() + "\u0000" + key)) throw new IllegalArgumentException("同一目标不能重复保存反馈。");
                if (feedback.existsByReviewIdAndTargetAndTargetKey(review.id, input.target(), key))
                    throw PracticeException.conflict("IDEMPOTENCY_CONFLICT", "该复盘的反馈已保存，请查看现有记录。");
                validateIds(input.criterionIds(), snapshot.criteria().stream().map(Criterion::id).toList(), "反馈验收项");
                validateIds(input.evidenceIds(), snapshot.evidence().stream().map(Evidence::id).toList(), "反馈证据");
                PracticeFeedbackEntity entity = new PracticeFeedbackEntity();
                entity.projectId = id; entity.reviewId = review.id; entity.target = input.target(); entity.targetKey = key;
                entity.requestId = requestId; entity.requestHash = fingerprint; entity.itemIndex = index; entity.confirmedAt = Instant.now();
                FeedbackRecord record = new FeedbackRecord(entity.id, id, review.id, input.target(), targetId, targetName,
                        rules.text(input.title().trim(), "反馈名称", 1, 120), rules.text(input.note().trim(), "反馈说明", 1, 5000),
                        List.copyOf(input.criterionIds()), List.copyOf(input.evidenceIds()), "reference_only", null, entity.confirmedAt);
                entity.payloadJson = encode(record); feedback.save(entity); result.add(record);
            }
            feedback.flush(); return List.copyOf(result);
        });
    }

    public List<FeedbackRecord> feedback(String id) {
        return read(() -> {
            requireProject(id, false);
            return feedback.findByProjectIdOrderByConfirmedAtDescItemIndexAsc(id).stream().map(item -> decode(item.payloadJson, FeedbackRecord.class)).toList();
        });
    }

    public Project linkDecision(String id, OriginRequest request) {
        if (!Set.of("baseline", "changed").contains(request.optionKey())) throw new IllegalArgumentException("请选择基准或变更方案。");
        DecisionOrigin origin;
        try {
            var version = decisions.history(request.scenarioId()).stream().filter(item -> item.id().equals(request.versionId()))
                    .findFirst().orElseThrow(() -> new NoSuchElementException("该版本不属于选定决策场景。"));
            var option = version.results().stream().filter(item -> item.optionKey().equals(request.optionKey()))
                    .findFirst().orElseThrow(() -> new NoSuchElementException("该版本缺少选定方案。"));
            origin = new DecisionOrigin(request.scenarioId(), version.id(), option.optionKey(), Instant.now(),
                    new SourceSnapshot(version.snapshot().title(), version.snapshot().goal(), option.optionName(),
                            new SourceRange(option.timeRange().min(), option.timeRange().max(), option.timeRange().unit()),
                            new SourceRange(option.budgetRange().min(), option.budgetRange().max(), option.budgetRange().unit()), option.assumptions()));
        } catch (NoSuchElementException | IllegalArgumentException exception) { throw exception; }
        catch (RuntimeException exception) { throw new PracticeException(HttpStatus.SERVICE_UNAVAILABLE, "SOURCE_UNAVAILABLE", "决策来源暂不可用，已有快照保持不变。"); }
        return write(() -> {
            PracticeProjectEntity project = requireProject(id, true); revision(project, request.inputRevision());
            DecisionOrigin previous = project.decisionOriginJson == null ? null : decode(project.decisionOriginJson, DecisionOrigin.class);
            if (previous == null || !previous.scenarioId().equals(origin.scenarioId()) || !previous.versionId().equals(origin.versionId()) || !previous.optionKey().equals(origin.optionKey())) {
                project.decisionOriginJson = encode(origin); touch(project);
            }
            return toProject(project);
        });
    }

    private PracticeProjectEntity requireProject(String id, boolean lock) {
        return (lock ? projects.findLockedById(id) : projects.findById(id))
                .orElseThrow(() -> new NoSuchElementException("未找到该实践项目。"));
    }

    private PracticeReviewEntity requireReview(String projectId, String reviewId) {
        PracticeReviewEntity entity = reviews.findById(reviewId).orElseThrow(() -> new NoSuchElementException("未找到该复盘版本。"));
        if (!entity.projectId.equals(projectId)) throw new NoSuchElementException("该复盘不属于当前项目。");
        return entity;
    }

    private void revision(PracticeProjectEntity project, int revision) {
        if (project.inputRevision != revision) throw PracticeException.conflict("REVISION_CONFLICT", "项目已被更新，请载入最新记录；当前草稿会保留。");
    }

    private void currentReview(PracticeProjectEntity project, PracticeReviewEntity review) {
        if (project.inputRevision != review.inputRevision) throw PracticeException.conflict("REVIEW_STALE", "复盘之后输入已变化，请重新检查后确认。");
    }

    private void touch(PracticeProjectEntity project) { project.inputRevision++; project.updatedAt = Instant.now(); }

    private void validateIds(List<String> input, List<String> available, String name) {
        if (input == null || input.stream().anyMatch(item -> item == null || !available.contains(item)) || new HashSet<>(input).size() != input.size())
            throw new IllegalArgumentException(name + "包含未知或重复对象。");
    }

    private Project toProject(PracticeProjectEntity entity) {
        List<InputMetric> metrics = decodeList(entity.metricsJson, InputMetric.class);
        return new Project(entity.id, entity.title, entity.goal, entity.templateId, entity.inputRevision,
                entity.criteria.stream().sorted(Comparator.comparingInt(item -> item.orderIndex)).map(item -> new Criterion(item.id,
                        item.title, item.standard, item.expectedEvidence, item.required, item.orderIndex)).toList(),
                entity.evidence.stream().sorted(Comparator.comparing((PracticeEvidenceEntity item) -> item.updatedAt).thenComparing(item -> item.id))
                        .map(item -> new Evidence(item.id, item.revision, item.title, item.kind, item.content,
                                decodeList(item.criterionIdsJson, String.class), item.updatedAt)).toList(),
                metrics, entity.processNote, entity.decisionOriginJson == null ? null : decode(entity.decisionOriginJson, DecisionOrigin.class),
                entity.createdAt, entity.updatedAt, rules.compareMetrics(metrics));
    }

    private Review toReview(PracticeReviewEntity entity, int currentRevision) {
        return new Review(entity.id, entity.projectId, entity.reviewNumber, entity.inputRevision,
                decode(entity.snapshotJson, Project.class), decodeList(entity.resultJson, Finding.class), entity.source,
                entity.providerNotice, entity.finishedAt, entity.confirmationJson == null ? null : decode(entity.confirmationJson, Confirmation.class),
                entity.inputRevision != currentRevision);
    }

    private String encode(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("实践数据序列化失败。", exception); }
    }

    private <T> T decode(String value, Class<T> type) {
        try { return mapper.readValue(value, type); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("实践数据读取失败。", exception); }
    }

    private <T> List<T> decodeList(String value, Class<T> type) {
        try { return mapper.readValue(value, mapper.getTypeFactory().constructCollectionType(List.class, type)); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("实践列表读取失败。", exception); }
    }

    private String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
}
