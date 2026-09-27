package com.ican.assistant.modules.decisionsandbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ican.assistant.core.ai.DecisionAiGateway;
import com.ican.assistant.modules.decisionsandbox.DecisionDtos.CompareRequest;
import com.ican.assistant.modules.decisionsandbox.DecisionDtos.Comparison;
import com.ican.assistant.modules.decisionsandbox.DecisionDtos.Constraint;
import com.ican.assistant.modules.decisionsandbox.DecisionDtos.Node;
import com.ican.assistant.modules.decisionsandbox.DecisionDtos.ParseResult;
import com.ican.assistant.modules.decisionsandbox.DecisionDtos.Relation;
import com.ican.assistant.modules.decisionsandbox.DecisionDtos.RelationInput;
import com.ican.assistant.modules.decisionsandbox.DecisionDtos.Resource;
import com.ican.assistant.modules.decisionsandbox.DecisionDtos.ResourceInput;
import com.ican.assistant.modules.decisionsandbox.DecisionDtos.Scenario;
import com.ican.assistant.modules.decisionsandbox.DecisionDtos.ScenarioInput;
import com.ican.assistant.modules.decisionsandbox.DecisionDtos.SimulationResult;
import com.ican.assistant.modules.decisionsandbox.DecisionDtos.Summary;
import com.ican.assistant.modules.decisionsandbox.DecisionDtos.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DecisionScenarioService {
    private final DecisionScenarioRepository scenarios;
    private final DecisionVersionRepository versions;
    private final DecisionRules rules;
    private final DecisionAiGateway ai;
    private final ObjectMapper mapper;

    public DecisionScenarioService(DecisionScenarioRepository scenarios, DecisionVersionRepository versions,
                                   DecisionRules rules, DecisionAiGateway ai, ObjectMapper mapper) {
        this.scenarios = scenarios;
        this.versions = versions;
        this.rules = rules;
        this.ai = ai;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<Summary> list() {
        return scenarios.findAllByOrderByUpdatedAtDesc().stream()
                .map(s -> new Summary(s.id, s.title, s.goal, s.updatedAt, (int) versions.countByScenario_Id(s.id))).toList();
    }

    @Transactional
    public Scenario create(ScenarioInput input) {
        DecisionScenarioEntity entity = new DecisionScenarioEntity();
        entity.createdAt = Instant.now();
        entity.revision = 1;
        fillFields(entity, input);
        entity = scenarios.saveAndFlush(entity);
        DecisionGraphBuilder.rebuild(entity, input);
        scenarios.saveAndFlush(entity);
        return toScenario(entity, false);
    }

    @Transactional(readOnly = true)
    public Scenario get(String id) {
        return toScenario(requireScenario(id), true);
    }

    @Transactional
    public Scenario update(String id, ScenarioInput input) {
        DecisionScenarioEntity entity = requireScenario(id);
        entity.constraints.clear();
        entity.resources.clear();
        entity.nodes.clear();
        entity.relations.clear();
        scenarios.flush();
        fillFields(entity, input);
        entity.revision++;
        DecisionGraphBuilder.rebuild(entity, input);
        scenarios.saveAndFlush(entity);
        return toScenario(entity, false);
    }

    @Transactional(readOnly = true)
    public ParseResult parse(String id, String input) {
        requireScenario(id);
        return ai.parse(input);
    }

    @Transactional
    public Version simulate(String id) {
        DecisionScenarioEntity entity = requireScenario(id);
        Scenario snapshot = toScenario(entity, false);
        List<SimulationResult> computed;
        try { computed = rules.simulate(snapshot); }
        catch (RuntimeException exception) { throw new DecisionCalculationException(exception); }
        DecisionAiGateway.Explanation explanation = ai.explain(computed.get(0), computed.get(1));
        List<SimulationResult> results = computed.stream().map(result -> new SimulationResult(
                result.id(), result.scenarioId(), result.optionKey(), result.optionName(), result.timeRange(),
                result.budgetRange(), result.peopleRange(), result.resources(), result.riskLevel(),
                result.assumptions(), result.impacts(), result.source(), explanation.text(), explanation.source())).toList();
        int next = versions.findFirstByScenario_IdOrderByVersionNumberDesc(id).map(v -> v.versionNumber + 1).orElse(1);
        DecisionVersionEntity version = new DecisionVersionEntity();
        version.scenario = entity;
        version.versionNumber = next;
        version.scenarioRevision = entity.revision;
        version.changeSummary = next == 1 ? "建立基准与变更方案" : "条件修订后重新推演";
        version.createdAt = Instant.now();
        version.snapshotJson = encode(snapshot);
        for (SimulationResult result : results) {
            DecisionSimulationEntity simulation = new DecisionSimulationEntity();
            simulation.id = UUID.randomUUID().toString();
            simulation.version = version;
            simulation.optionKey = result.optionKey();
            simulation.resultJson = encode(result);
            version.simulations.add(simulation);
        }
        version = versions.saveAndFlush(version);
        return toVersion(version);
    }

    @Transactional(readOnly = true)
    public List<Version> history(String id) {
        requireScenario(id);
        return versions.findByScenario_IdOrderByVersionNumberDesc(id).stream().map(this::toVersion).toList();
    }

    @Transactional(readOnly = true)
    public Comparison compare(String id, CompareRequest request) {
        Version left = toVersion(requireVersion(id, request.leftVersionId()));
        Version right = toVersion(requireVersion(id, request.rightVersionId()));
        List<String> changed = new ArrayList<>();
        if (left.snapshot().peopleCount() != right.snapshot().peopleCount())
            changed.add("人数 " + left.snapshot().peopleCount() + " → " + right.snapshot().peopleCount());
        if (left.snapshot().timeLimitDays() != right.snapshot().timeLimitDays())
            changed.add("期限 " + left.snapshot().timeLimitDays() + " → " + right.snapshot().timeLimitDays() + " 天");
        if (left.snapshot().budgetYuan().compareTo(right.snapshot().budgetYuan()) != 0)
            changed.add("预算 " + left.snapshot().budgetYuan().stripTrailingZeros().toPlainString() + " → "
                    + right.snapshot().budgetYuan().stripTrailingZeros().toPlainString() + " 元");
        if (left.snapshot().hasServer() != right.snapshot().hasServer()) changed.add("现有服务器条件改变");
        if (!Objects.equals(left.snapshot().changeRequest(), right.snapshot().changeRequest()))
            changed.add("功能变更 " + left.snapshot().changeRequest() + " → " + right.snapshot().changeRequest());
        if (!resourceContents(left.snapshot()).equals(resourceContents(right.snapshot()))) changed.add("资源清单改变");
        SimulationResult l = changedResult(left);
        SimulationResult r = changedResult(right);
        int minTime = r.timeRange().min() - l.timeRange().min();
        int maxTime = r.timeRange().max() - l.timeRange().max();
        int minBudget = r.budgetRange().min() - l.budgetRange().min();
        int maxBudget = r.budgetRange().max() - l.budgetRange().max();
        String riskChange = l.riskLevel() + " → " + r.riskLevel();
        String summary = changed.isEmpty() ? "输入条件未改变，推演结果一致。" :
                "从版本 " + left.number() + " 到版本 " + right.number() + "，改变了 " + changed.size() + " 项条件。";
        return new Comparison(left.id(), right.id(), summary, changed, minTime, maxTime,
                minBudget, maxBudget, riskChange);
    }

    @Transactional
    public Scenario restore(String id, String versionId) {
        requireScenario(id);
        Version version = toVersion(requireVersion(id, versionId));
        Scenario snapshot = version.snapshot();
        ScenarioInput input = new ScenarioInput(snapshot.title(), snapshot.goal(), snapshot.timeLimitDays(),
                snapshot.budgetYuan(), snapshot.peopleCount(), snapshot.hasServer(), snapshot.changeRequest(),
                snapshot.resources().stream().map(r -> new ResourceInput(r.id(), r.label(), r.type(), r.quantity(), r.unit())).toList(),
                snapshot.relations().stream().map(r -> new RelationInput(r.id(), r.from(), r.to(), r.label(),
                        java.math.BigDecimal.valueOf(r.confidence()), r.assumption())).toList());
        return update(id, input);
    }

    private SimulationResult changedResult(Version version) {
        return version.results().stream().filter(result -> result.optionKey().equals("changed"))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("版本缺少变更方案。"));
    }

    private List<String> resourceContents(Scenario scenario) {
        return scenario.resources().stream().map(resource -> resource.type() + "|" + resource.label() + "|"
                + resource.quantity() + "|" + resource.unit()).sorted().toList();
    }

    private void fillFields(DecisionScenarioEntity entity, ScenarioInput input) {
        entity.title = input.title().trim();
        entity.goal = input.goal().trim();
        entity.timeLimitDays = input.timeLimitDays();
        entity.budgetYuan = input.budgetYuan();
        entity.peopleCount = input.peopleCount();
        entity.hasServer = input.hasServer();
        entity.changeRequest = input.changeRequest();
        entity.updatedAt = Instant.now();
    }

    private DecisionScenarioEntity requireScenario(String id) {
        return scenarios.findById(id).orElseThrow(() -> new NoSuchElementException("未找到该决策场景。"));
    }

    private DecisionVersionEntity requireVersion(String scenarioId, String versionId) {
        DecisionVersionEntity version = versions.findById(versionId)
                .orElseThrow(() -> new NoSuchElementException("未找到该版本。"));
        if (!version.scenarioId.equals(scenarioId)) throw new NoSuchElementException("该版本不属于当前场景。");
        return version;
    }

    private Scenario toScenario(DecisionScenarioEntity entity, boolean withLatest) {
        List<SimulationResult> latest = List.of();
        if (withLatest) {
            var maybe = versions.findFirstByScenario_IdOrderByVersionNumberDesc(entity.id);
            if (maybe.isPresent() && maybe.get().scenarioRevision == entity.revision) latest = toVersion(maybe.get()).results();
        }
        return new Scenario(entity.id, entity.title, entity.goal, entity.timeLimitDays, entity.budgetYuan,
                entity.peopleCount, entity.hasServer, entity.changeRequest,
                entity.constraints.stream().map(c -> new Constraint(DecisionGraphBuilder.logical(entity, c.id), c.type,
                        c.label, c.value, c.source, c.confidence.doubleValue(), c.assumption)).toList(),
                entity.resources.stream().map(r -> new Resource(DecisionGraphBuilder.logical(entity, r.id), r.type,
                        r.label, r.quantity, r.unit, r.source, r.confidence.doubleValue(), r.assumption)).toList(),
                entity.nodes.stream().map(n -> new Node(DecisionGraphBuilder.logical(entity, n.id), n.type,
                        n.label, n.source, n.confidence.doubleValue(), n.assumption)).toList(),
                entity.relations.stream().map(r -> new Relation(DecisionGraphBuilder.logical(entity, r.id), r.fromNodeId,
                        r.toNodeId, r.label, r.source, r.confidence.doubleValue(), r.assumption)).toList(),
                latest, (int) versions.countByScenario_Id(entity.id), entity.revision, entity.createdAt, entity.updatedAt);
    }

    private Version toVersion(DecisionVersionEntity entity) {
        List<SimulationResult> results = entity.simulations.stream().map(s -> decode(s.resultJson, SimulationResult.class))
                .sorted(java.util.Comparator.comparing(result -> result.optionKey().equals("baseline") ? 0 : 1)).toList();
        return new Version(entity.id, entity.versionNumber, entity.scenarioRevision, entity.createdAt,
                entity.changeSummary, decode(entity.snapshotJson, Scenario.class), results);
    }

    private String encode(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("版本序列化失败。", exception); }
    }

    private <T> T decode(String value, Class<T> type) {
        try { return mapper.readValue(value, type); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("版本读取失败。", exception); }
    }
}
