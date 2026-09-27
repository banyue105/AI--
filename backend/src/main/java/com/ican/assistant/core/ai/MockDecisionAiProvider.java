package com.ican.assistant.core.ai;

import com.ican.assistant.modules.decisionsandbox.DecisionDtos.ParseResult;
import com.ican.assistant.modules.decisionsandbox.DecisionDtos.Suggestion;
import com.ican.assistant.modules.decisionsandbox.DecisionDtos.SimulationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Deterministic demo adapter. It extracts candidates, never calculates estimates. */
@Component
public class MockDecisionAiProvider {
    private static final Pattern PEOPLE = Pattern.compile("(\\d+)\\s*(?:人|名)");
    private static final Pattern MONEY = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(万|元)");
    private static final Pattern DAYS = Pattern.compile("(\\d+)\\s*天");

    public ParseResult parse(String input) {
        List<Suggestion> candidates = new ArrayList<>();
        find(PEOPLE, input, match -> candidates.add(new Suggestion("people", "参与人数", match.group(1), 0.95,
                "按原文人数提取，需用户确认", "mock")));
        find(MONEY, input, match -> {
            double value = Double.parseDouble(match.group(1)) * ("万".equals(match.group(2)) ? 10000 : 1);
            candidates.add(new Suggestion("budget", "预算（元）", String.valueOf((long) value), 0.95,
                    "将‘万’换算为元，需用户确认", "mock"));
        });
        find(DAYS, input, match -> candidates.add(new Suggestion("time", "期限（天）", match.group(1), 0.95,
                "按原文天数提取，需用户确认", "mock")));
        if (input.contains("没有服务器") || input.contains("无服务器")) {
            candidates.add(new Suggestion("server", "已有服务器", "false", 0.9, "按原文资源提取，需用户确认", "mock"));
        } else if (input.contains("已有服务器") || input.contains("现有服务器") || input.contains("有服务器")) {
            candidates.add(new Suggestion("server", "已有服务器", "true", 0.9, "按原文资源提取，需用户确认", "mock"));
        }
        Matcher feature = Pattern.compile("(?:增加|新增|加入|开发)([^，。；;]+?)(?:[，。；;]|$)").matcher(input);
        if (feature.find()) candidates.add(new Suggestion("change", "功能变更", "增加" + feature.group(1).trim(), 0.82,
                "按原文动作提取；工作量由规则单独计算", "mock"));
        return new ParseResult(candidates.isEmpty() ? "未识别到可确认的人员、预算、期限、服务器或功能变更条件。" :
                "提取到 " + candidates.size() + " 项候选条件；确认后才会写入场景。", candidates,
                List.of("未写明的条件保持原值。", "提取结果是候选项，不是自动决策。"), "mock");
    }

    public String explain(SimulationResult baseline, SimulationResult changed) {
        return "规则估算显示：基准工期 " + baseline.timeRange().min() + "–" + baseline.timeRange().max()
                + " 天，变更后为 " + changed.timeRange().min() + "–" + changed.timeRange().max()
                + " 天；变更方案风险为" + switch (changed.riskLevel()) {
                    case "high" -> "高";
                    case "medium" -> "中";
                    default -> "低";
                } + "。区间来自人日、有效产能、成本及资源准备规则，仍需按实际任务校准。";
    }

    private void find(Pattern pattern, String input, java.util.function.Consumer<Matcher> consumer) {
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) consumer.accept(matcher);
    }
}
