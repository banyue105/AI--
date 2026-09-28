package com.ican.assistant.modules.practicereview;

import static com.ican.assistant.modules.practicereview.PracticeDtos.*;
import java.util.List;

public final class PracticeTemplates {
    public static final String NETWORK = "network-service-v1";
    private PracticeTemplates() {}

    public static Template network() {
        return new Template(NETWORK, "网络服务部署", "用配置、访问日志和部署说明检查成果材料。", List.of(
                new Criterion("http", "基础服务访问", "提供一次 HTTP 请求成功的访问结果。",
                        "HTTP 状态行或包含请求路径与状态码的访问日志。", true, 0),
                new Criterion("https", "HTTPS 配置与访问", "提供 TLS 配置及对应的 HTTPS 请求成功结果。",
                        "TLS 监听、证书配置与 HTTPS 成功访问日志。", true, 1),
                new Criterion("logs", "日志可追溯", "提供带 request_id 或 trace_id 的请求日志。",
                        "含请求标识的原始日志，能定位一次请求。", true, 2),
                new Criterion("reproduce", "部署可复现", "部署说明包含环境版本、启动步骤和验证步骤。",
                        "明确写出环境版本、启动命令和验证命令的说明。", true, 3)));
    }

    public static List<Template> all() { return List.of(network()); }

    public static Template require(String id) {
        return all().stream().filter(template -> template.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("请选择已提供的实践模板。"));
    }
}
