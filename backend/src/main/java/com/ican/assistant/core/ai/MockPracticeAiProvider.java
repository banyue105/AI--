package com.ican.assistant.core.ai;

import static com.ican.assistant.modules.practicereview.PracticeDtos.*;
import com.ican.assistant.modules.practicereview.PracticeTemplates;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Conservative checks for the documented deployment template; never executes evidence. */
@Component
public class MockPracticeAiProvider {
    public List<Suggestion> suggestions() {
        return PracticeTemplates.network().criteria().stream().map(item -> new Suggestion(
                item.title(), item.standard(), item.expectedEvidence(), item.required())).toList();
    }

    public List<Finding> review(Project project) {
        return project.criteria().stream().map(criterion -> evaluate(project, criterion)).toList();
    }

    private Finding evaluate(Project project, Criterion criterion) {
        var templateItem = PracticeTemplates.network().criteria().stream()
                .filter(item -> item.standard().equals(criterion.standard())).findFirst();
        if (templateItem.isEmpty()) return missing(criterion, "当前确定性检查无法核验这项自定义标准。请提供可观察的材料，或使用 AI 检查。");
        List<Evidence> evidence = project.evidence().stream().filter(item -> item.criterionIds().isEmpty()
                || item.criterionIds().contains(criterion.id())).toList();
        String key = templateItem.get().id();
        List<Citation> citations = new ArrayList<>();
        if (key.equals("https")) {
            List<Citation> failed = matches(evidence, "log", "(?i)(?:HTTPS|TLS|SSL).*?(?:failed|failure|error|失败|错误|证书过期)");
            if (!failed.isEmpty()) return new Finding(criterion.id(), "conflicting", "提交的日志包含 TLS/HTTPS 失败记录，当前材料与成功访问要求存在冲突。",
                    failed, "排查失败原因，并补交相同环境下的成功访问结果。");
            citations.addAll(matches(evidence, "config", "(?i)^\\s*listen\\s+443\\s+ssl(?:\\s|;|$)"));
            List<Citation> certificates = matches(evidence, "config", "(?i)^\\s*ssl_certificate\\s+\\S+");
            List<Citation> keys = matches(evidence, "config", "(?i)^\\s*ssl_certificate_key\\s+\\S+");
            List<Citation> visits = matches(evidence, "log", "(?i)^HTTPS\\s+(?:GET|POST|HEAD)\\s+https://\\S+\\s+->\\s+2\\d\\d\\b");
            if (citations.isEmpty() || certificates.isEmpty() || keys.isEmpty() || visits.isEmpty())
                return missing(criterion, "材料尚未同时包含 TLS 监听、证书配置和 HTTPS 成功访问结果。");
            citations.addAll(certificates); citations.addAll(keys); citations.addAll(visits);
        } else if (key.equals("http")) {
            citations.addAll(matches(evidence, "log", "(?i)^HTTP/(?:1\\.[01]|2(?:\\.0)?|3(?:\\.0)?)\\s+2\\d\\d\\b|\"(?:GET|POST|HEAD|PUT|DELETE)\\s+\\S+\\s+HTTP/[0-9.]+\"\\s+2\\d\\d\\b"));
            if (citations.isEmpty()) return missing(criterion, "未找到可识别的 HTTP 成功状态行或成功访问日志。");
        } else if (key.equals("logs")) {
            citations.addAll(matches(evidence, "log", "(?i)\\b(?:request_id|trace_id)=[A-Za-z0-9_-]+\\b"));
            if (citations.isEmpty()) return missing(criterion, "当前日志缺少可定位请求的 request_id 或 trace_id。");
        } else {
            List<Citation> environment = matches(evidence, "note", "(?i)^(?:环境|Environment)[:：].*\\d");
            List<Citation> start = matches(evidence, "note", "(?i)^(?:启动|Start)[:：].*\\S");
            List<Citation> verify = matches(evidence, "note", "(?i)^(?:验证|Verify)[:：].*\\S");
            if (environment.isEmpty() || start.isEmpty() || verify.isEmpty())
                return missing(criterion, "部署说明缺少明确的环境版本、启动步骤或验证步骤。");
            citations.addAll(environment); citations.addAll(start); citations.addAll(verify);
        }
        return new Finding(criterion.id(), "supported", "当前材料提供了该验收标准所需的具体证据；可点击引用核对原文。",
                List.copyOf(citations), null);
    }

    private Finding missing(Criterion criterion, String reason) {
        return new Finding(criterion.id(), "insufficient", reason, List.of(), "补充：" + criterion.expectedEvidence());
    }

    private List<Citation> matches(List<Evidence> evidence, String kind, String expression) {
        Pattern pattern = Pattern.compile(expression);
        // One exact reference per required signal keeps results small and deterministic.
        for (Evidence item : evidence) {
            if (!item.kind().equals(kind)) continue;
            String[] lines = item.content().split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                if (pattern.matcher(lines[i]).find()) return List.of(new Citation(item.id(), item.revision(), i + 1, i + 1, lines[i]));
            }
        }
        return List.of();
    }
}
