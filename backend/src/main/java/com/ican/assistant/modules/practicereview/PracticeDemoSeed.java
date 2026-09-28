package com.ican.assistant.modules.practicereview;

import static com.ican.assistant.modules.practicereview.PracticeDtos.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class PracticeDemoSeed implements ApplicationRunner {
    private final PracticeProjectRepository projects;
    private final PracticeService service;
    private final boolean enabled;

    public PracticeDemoSeed(PracticeProjectRepository projects, PracticeService service, @Value("${app.seed-demo:true}") boolean enabled) {
        this.projects = projects; this.service = service; this.enabled = enabled;
    }

    @java.lang.Override
    public void run(ApplicationArguments args) {
        if (!enabled || projects.count() > 0) return;
        Project project = service.create(new CreateRequest("示例：网络服务部署", "独立完成一个可复现的网络服务部署", PracticeTemplates.NETWORK));
        project = service.update(project.id(), new UpdateRequest(project.inputRevision(), project.title(), project.goal(),
                List.of(new InputMetric("effort", "hour", "self", "effective_work",
                        new Expected(new BigDecimal("4"), new BigDecimal("6")), new BigDecimal("9"), "before_practice", Instant.now())),
                "合成演示样例：预期投入 4–6 小时，实际 9 小时；额外 3 小时用于排查环境依赖。所有材料与投入均为示例数据。"));
        project = service.saveEvidence(project.id(), null, new EvidenceRequest(project.inputRevision(), "示例访问日志", "log",
                "HTTP/1.1 200 OK\n2026-09-28T09:00:00Z request_id=demo-001 method=GET path=/health status=200\n以上为合成演示日志。", List.of()));
        project = service.saveEvidence(project.id(), null, new EvidenceRequest(project.inputRevision(), "示例部署说明（待补充）", "note",
                "启动：npm run start\n验证：curl http://localhost:8080/health\n说明：还未记录环境版本；以上为合成演示材料。", List.of()));
        service.review(project.id(), new ReviewRequest(project.inputRevision(), UUID.randomUUID().toString()));
    }
}
