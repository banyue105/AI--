package com.ican.assistant.api;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ModuleRegistry {
    public record ModuleManifest(String id, String title, String description, String route,
                                 String status, String updatedAt) {}

    public List<ModuleManifest> modules() {
        return List.of(
                new ModuleManifest("ability-growth", "能力成长", "把经验转成能力图谱，找到下一步", "/ability", "ready", "已接入数据库"),
                new ModuleManifest("decision-sandbox", "决策沙盒", "改变现实条件，比较不同选择的代价", "/decision", "ready", "已接入数据库"),
                new ModuleManifest("module3", "实践验证与复盘", "核对成果证据，记录实际投入与补验变化", "/module3", "prototype", "已可演示"),
                new ModuleManifest("module4", "职业规划", "匹配岗位、分析 JD、规划能力提升", "/module4/match", "prototype", "演示岗位库"));
    }
}
