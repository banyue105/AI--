package com.ican.assistant.api;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class PlatformController {
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/modules")
    public List<Map<String, String>> modules() {
        return List.of(
                Map.of("id", "ability-growth", "title", "能力成长", "description", "建立能力结构和成长路径", "route", "/ability", "status", "prototype"),
                Map.of("id", "decision-sandbox", "title", "决策沙盒", "description", "编辑现实条件并比较不同方案", "route", "/decision", "status", "ready"),
                Map.of("id", "module3", "title", "第三模块", "description", "方向待确定", "route", "", "status", "pending"));
    }
}
