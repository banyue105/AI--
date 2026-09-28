package com.ican.assistant.modules.practicereview;

import static com.ican.assistant.modules.practicereview.PracticeDtos.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Validation, citations and arithmetic remain independent from model output. */
@Component
public class PracticeRules {
    public static final Set<String> VERDICTS = Set.of("supported", "insufficient", "conflicting");

    public String text(String value, String name, int minimum, int maximum) {
        if (value == null || value.codePointCount(0, value.length()) < minimum
                || value.codePointCount(0, value.length()) > maximum || (minimum > 0 && value.isBlank())) {
            throw new IllegalArgumentException(name + "须为 " + minimum + "–" + maximum + " 个字符。");
        }
        return value;
    }

    public String uuid(String value) {
        if (value == null || value.length() != 36) throw new IllegalArgumentException("请求标识须为有效 UUID。");
        try {
            if (!UUID.fromString(value).toString().equalsIgnoreCase(value)) throw new IllegalArgumentException();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("请求标识须为有效 UUID。");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    public String normalizeContent(String content) {
        return text(content.replace("\r\n", "\n").replace('\r', '\n'), "证据正文", 1, 12000);
    }

    public void validateEvidence(List<Evidence> evidence) {
        if (evidence.size() > 10) throw new IllegalArgumentException("每个项目最多保存 10 份证据。");
        int total = evidence.stream().mapToInt(item -> item.content().codePointCount(0, item.content().length())).sum();
        if (total > 60000) throw new PracticeException(HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_TOO_LARGE", "材料正文总量超过 60000 字符，请缩减后再保存。");
    }

    public List<InputMetric> normalizeMetrics(List<InputMetric> input) {
        if (input == null || input.size() > 12) throw new IllegalArgumentException("投入记录最多 12 项。");
        List<InputMetric> result = new ArrayList<>();
        Set<String> expectedKeys = new HashSet<>(), actualKeys = new HashSet<>();
        for (InputMetric item : input) {
            if (item == null) throw new IllegalArgumentException("投入记录不能为空。");
            String correctUnit = switch (item.metric()) {
                case "effort" -> "hour";
                case "duration" -> "day";
                case "cost" -> "CNY";
                default -> throw new IllegalArgumentException("未知投入指标。");
            };
            if (!correctUnit.equals(item.unit())) throw new IllegalArgumentException("投入指标与单位不匹配。");
            text(item.scope(), "投入范围", 1, 120);
            text(item.basis(), "投入口径", 1, 120);
            if (!Set.of("before_practice", "retrospective").contains(item.baselineTiming()))
                throw new IllegalArgumentException("请选择基线记录时机。");
            Expected expected = item.expected();
            if (expected != null) {
                nonnegative(expected.min()); nonnegative(expected.max());
                if (expected.min().compareTo(expected.max()) > 0) throw new IllegalArgumentException("预期区间下限不能大于上限。");
                if (item.baselineRecordedAt() == null) throw new IllegalArgumentException("预期投入必须记录基线时间。");
                if (item.baselineRecordedAt().isAfter(java.time.Instant.now().plusSeconds(60)))
                    throw new IllegalArgumentException("基线记录时间不能在未来。");
                expected = new Expected(amount(expected.min(), item.metric()), amount(expected.max(), item.metric()));
            }
            if (item.actual() != null) nonnegative(item.actual());
            InputMetric normalized = new InputMetric(item.metric(), item.unit(), item.scope().trim(), item.basis().trim(),
                    expected, item.actual() == null ? null : amount(item.actual(), item.metric()),
                    item.baselineTiming(), item.baselineRecordedAt());
            String key = metricKey(normalized);
            if (expected != null && !expectedKeys.add(key)) throw new IllegalArgumentException("同一口径只能保存一组预期值。");
            if (item.actual() != null && !actualKeys.add(key)) throw new IllegalArgumentException("同一口径只能保存一个实际值。");
            result.add(normalized);
        }
        return List.copyOf(result);
    }

    private BigDecimal amount(BigDecimal value, String metric) {
        return metric.equals("cost") ? value.setScale(2, RoundingMode.HALF_UP) : value;
    }

    private void nonnegative(BigDecimal value) {
        if (value == null || value.signum() < 0 || value.precision() > 16 || Math.abs(value.scale()) > 8)
            throw new IllegalArgumentException("投入值必须是合理的非负数字，最多 16 位有效数字和 8 位小数。");
    }

    private String metricKey(InputMetric item) {
        return String.join("\u0000", item.metric(), item.unit(), item.scope(), item.basis());
    }

    public List<MetricComparison> compareMetrics(List<InputMetric> metrics) {
        Map<String, InputMetric> combined = new LinkedHashMap<>();
        for (InputMetric item : metrics) {
            combined.merge(metricKey(item), item, (left, right) -> new InputMetric(left.metric(), left.unit(), left.scope(), left.basis(),
                    left.expected() != null ? left.expected() : right.expected(), left.actual() != null ? left.actual() : right.actual(),
                    left.expected() != null ? left.baselineTiming() : right.baselineTiming(),
                    left.expected() != null ? left.baselineRecordedAt() : right.baselineRecordedAt()));
        }
        return combined.values().stream().map(item -> {
            Expected expected = item.expected();
            BigDecimal actual = item.actual();
            if (expected == null || actual == null) {
                boolean incompatible = combined.values().stream().anyMatch(other -> !metricKey(other).equals(metricKey(item))
                        && (expected != null && other.actual() != null || actual != null && other.expected() != null));
                String reason = incompatible ? "预期与实际的指标、单位、范围或口径不同，不能直接比较。"
                        : expected == null ? "尚未记录同口径的预期投入。" : "尚未记录同口径的实际投入。";
                return new MetricComparison(item.metric(), item.unit(), item.scope(), item.basis(), expected, actual,
                        false, reason, null, null, null, null, item.baselineTiming());
            }
            BigDecimal lower = actual.subtract(expected.min()), upper = actual.subtract(expected.max());
            String position = lower.signum() < 0 ? "below" : upper.signum() > 0 ? "above" : "within";
            BigDecimal percent = expected.min().compareTo(expected.max()) == 0 && expected.min().signum() > 0
                    ? lower.multiply(new BigDecimal("100")).divide(expected.min(), 1, RoundingMode.HALF_UP) : null;
            String reason = expected.min().signum() == 0 && expected.max().signum() == 0 ? "零基线，无可用百分比差异。"
                    : item.baselineTiming().equals("retrospective") ? "基线为事后回忆，供复盘参考。" : "按相同范围、单位和口径计算。";
            return new MetricComparison(item.metric(), item.unit(), item.scope(), item.basis(), expected, actual,
                    true, reason, position, lower, upper, percent, item.baselineTiming());
        }).toList();
    }

    public void validateFindings(Project project, List<Finding> findings) {
        if (findings == null || findings.size() != project.criteria().size()) throw new IllegalArgumentException("检查结果必须覆盖所有验收项。");
        Set<String> expectedIds = new HashSet<>(project.criteria().stream().map(Criterion::id).toList());
        Set<String> visited = new HashSet<>();
        for (Finding finding : findings) {
            if (finding == null || !expectedIds.contains(finding.criterionId()) || !visited.add(finding.criterionId()))
                throw new IllegalArgumentException("检查结果含未知或重复验收项。");
            validateFinding(project, finding);
        }
    }

    public void validateFinding(Project project, Finding finding) {
        if (finding.verdict() == null || !VERDICTS.contains(finding.verdict())) throw new IllegalArgumentException("未知判定状态。");
        text(finding.reason(), "判定依据", 1, 2000);
        if (finding.citations() == null || finding.citations().size() > 12) throw new IllegalArgumentException("引用格式不正确。");
        if (!finding.verdict().equals("insufficient") && finding.citations().isEmpty())
            throw new IllegalArgumentException("材料支持或存在矛盾的判定必须提供引用。");
        if (finding.verdict().equals("insufficient")) text(finding.nextAction(), "补验步骤", 1, 2000);
        else if (finding.nextAction() != null) text(finding.nextAction(), "补验步骤", 0, 2000);
        for (Citation citation : finding.citations()) {
            validateCitation(project, citation);
            Evidence evidence = project.evidence().stream().filter(item -> item.id().equals(citation.evidenceId())).findFirst().orElseThrow();
            if (!evidence.criterionIds().isEmpty() && !evidence.criterionIds().contains(finding.criterionId()))
                throw new IllegalArgumentException("引用的证据未关联当前验收项。");
        }
    }

    public void validateCitation(Project project, Citation citation) {
        if (citation == null) throw new IllegalArgumentException("引用不能为空。");
        Evidence evidence = project.evidence().stream().filter(item -> item.id().equals(citation.evidenceId())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("引用的证据不属于该复盘。"));
        if (evidence.revision() != citation.evidenceRevision()) throw new IllegalArgumentException("引用的证据版本不一致。");
        String[] lines = evidence.content().split("\n", -1);
        if (citation.startLine() < 1 || citation.endLine() < citation.startLine() || citation.endLine() > lines.length)
            throw new IllegalArgumentException("引用行号超出证据范围。");
        String exact = String.join("\n", Arrays.copyOfRange(lines, citation.startLine() - 1, citation.endLine()));
        text(citation.quote(), "引用正文", 1, 12000);
        if (!exact.equals(citation.quote())) throw new IllegalArgumentException("引用与原始材料不一致。");
    }

    public List<Finding> parseFindings(JsonNode root, Project project) {
        exactFields(root, Set.of("findings"));
        JsonNode array = root.get("findings");
        if (!array.isArray() || array.size() > 12) throw new IllegalArgumentException("检查结果格式不正确。");
        List<Finding> findings = new ArrayList<>();
        for (JsonNode node : array) {
            exactFields(node, Set.of("criterionId", "verdict", "reason", "citations", "nextAction"));
            JsonNode citationsNode = node.get("citations");
            if (!citationsNode.isArray() || citationsNode.size() > 12) throw new IllegalArgumentException("引用格式不正确。");
            List<Citation> citations = new ArrayList<>();
            for (JsonNode item : citationsNode) {
                exactFields(item, Set.of("evidenceId", "evidenceRevision", "startLine", "endLine", "quote"));
                citations.add(new Citation(string(item, "evidenceId"), integer(item, "evidenceRevision"),
                        integer(item, "startLine"), integer(item, "endLine"), string(item, "quote")));
            }
            if (!node.get("nextAction").isNull() && !node.get("nextAction").isTextual()) throw new IllegalArgumentException("补验步骤格式不正确。");
            findings.add(new Finding(string(node, "criterionId"), string(node, "verdict"), string(node, "reason"),
                    List.copyOf(citations), node.get("nextAction").isNull() ? null : node.get("nextAction").textValue()));
        }
        validateFindings(project, findings);
        return List.copyOf(findings);
    }

    public List<Suggestion> parseSuggestions(JsonNode root) {
        exactFields(root, Set.of("candidates"));
        JsonNode array = root.get("candidates");
        if (!array.isArray() || array.isEmpty() || array.size() > 12) throw new IllegalArgumentException("候选标准格式不正确。");
        List<Suggestion> result = new ArrayList<>();
        for (JsonNode node : array) {
            exactFields(node, Set.of("title", "standard", "expectedEvidence", "required"));
            if (!node.get("required").isBoolean()) throw new IllegalArgumentException("必需项标记格式不正确。");
            result.add(new Suggestion(text(string(node, "title"), "验收名称", 1, 120),
                    text(string(node, "standard"), "验收标准", 1, 1000),
                    text(string(node, "expectedEvidence"), "材料要求", 1, 1000), node.get("required").booleanValue()));
        }
        return List.copyOf(result);
    }

    private void exactFields(JsonNode node, Set<String> expected) {
        if (node == null || !node.isObject()) throw new IllegalArgumentException("结构化输出必须是对象。");
        Set<String> actual = new HashSet<>(); node.fieldNames().forEachRemaining(actual::add);
        if (!actual.equals(expected)) throw new IllegalArgumentException("结构化输出缺少字段或包含额外字段。");
    }

    private String string(JsonNode node, String key) {
        if (!node.get(key).isTextual()) throw new IllegalArgumentException("输出文本字段格式不正确。");
        return node.get(key).textValue();
    }

    private int integer(JsonNode node, String key) {
        if (!node.get(key).isIntegralNumber() || !node.get(key).canConvertToInt()) throw new IllegalArgumentException("输出行号字段格式不正确。");
        return node.get(key).intValue();
    }
}
