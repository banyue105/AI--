package com.ican.assistant.modules.careerplanning;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ican.assistant.core.auth.CurrentUser;
import com.ican.assistant.modules.careerplanning.CareerDtos.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CareerService {
    private static final Map<String, List<String>> ALIASES = Map.ofEntries(
            Map.entry("Python", List.of("python")), Map.entry("Linux", List.of("linux")),
            Map.entry("TCP/IP", List.of("tcp/ip", "tcpip")), Map.entry("Git", List.of("git")),
            Map.entry("HTTP", List.of("http", "http 服务")), Map.entry("Shell", List.of("shell", "shell 自动化")),
            Map.entry("Docker", List.of("docker", "容器化")), Map.entry("监控", List.of("监控", "监控诊断", "monitor")),
            Map.entry("安全", List.of("服务安全", "security", "网络安全")),
            Map.entry("SQL", List.of("sql", "数据库")), Map.entry("Vue", List.of("vue")),
            Map.entry("TypeScript", List.of("typescript", "ts")), Map.entry("JavaScript", List.of("javascript", "js")),
            Map.entry("HTML", List.of("html")), Map.entry("CSS", List.of("css")),
            Map.entry("数据分析", List.of("数据分析")), Map.entry("可视化", List.of("可视化")),
            Map.entry("沟通", List.of("沟通", "表达")), Map.entry("产品设计", List.of("产品设计")),
            Map.entry("项目管理", List.of("项目管理")), Map.entry("测试", List.of("测试", "自动化测试")));
    private static final Map<String, String> PRACTICE_ACTIONS = Map.ofEntries(
            Map.entry("Docker", "把一个已有服务写成 Dockerfile 并完成本地部署，记录配置与验证结果。"),
            Map.entry("HTTP", "为已有服务实现并测试两个 HTTP 接口，记录状态码和错误处理。"),
            Map.entry("SQL", "设计一张业务表，完成查询与索引练习，并保存 SQL 和结果。"),
            Map.entry("Linux", "在 Linux 环境独立部署一个小服务，记录操作步骤与排错过程。"),
            Map.entry("Shell", "编写可重复运行的部署脚本，并记录运行日志与失败处理。"),
            Map.entry("监控", "为一个服务配置日志和基础监控，记录一次问题定位过程。"),
            Map.entry("安全", "为已有服务检查密钥、权限与输入校验，并记录修复前后的验证。"),
            Map.entry("Vue", "用 Vue 完成一个可交互页面，并整理组件拆分与状态管理过程。"),
            Map.entry("数据分析", "选一组公开数据完成清洗、分析和结论说明，保留可复现的过程。"));

    private final JdbcTemplate jdbc;
    private final CurrentUser currentUser;
    private final ObjectMapper json;

    public CareerService(JdbcTemplate jdbc, CurrentUser currentUser, ObjectMapper json) {
        this.jdbc = jdbc;
        this.currentUser = currentUser;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public List<Job> jobs(String keyword, String category, String city, String jobType) {
        String query = safe(keyword).toLowerCase(Locale.ROOT);
        return allJobs().stream()
                .filter(job -> query.isEmpty() || Stream.of(job.title(), job.company(), job.description(), job.requirements())
                        .anyMatch(value -> value.toLowerCase(Locale.ROOT).contains(query)))
                .filter(job -> safe(category).isEmpty() || job.category().equals(category))
                .filter(job -> safe(city).isEmpty() || job.city().equals(city))
                .filter(job -> safe(jobType).isEmpty() || job.jobType().equals(jobType))
                .toList();
    }

    @Transactional(readOnly = true)
    public Preference preference() {
        return jdbc.query("SELECT category, city, target_job_id FROM career_preference WHERE user_id = ?",
                (rs, row) -> new Preference(rs.getString(1), rs.getString(2), rs.getString(3)), currentUser.id())
                .stream().findFirst().orElse(new Preference("", "", null));
    }

    @Transactional
    public Preference savePreference(PreferenceInput input) {
        String category = safe(input.category());
        String city = safe(input.city());
        Preference before = preference();
        save(new Preference(category, city, before.targetJobId()));
        return preference();
    }

    @Transactional
    public Preference setTarget(String jobId) {
        findJob(jobId).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "演示岗位不存在"));
        Preference before = preference();
        save(new Preference(before.category(), before.city(), jobId));
        return preference();
    }

    @Transactional(readOnly = true)
    public Job target() {
        String id = preference().targetJobId();
        return id == null ? null : findJob(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<JobMatch> matches() {
        Preference pref = preference();
        List<Skill> skills = skills();
        return allJobs().stream().map(job -> new JobMatch(job, compare(job.skillTags(), skills, pref, job)))
                .sorted(Comparator.comparingInt((JobMatch item) -> item.match().score()).reversed()
                        .thenComparing(item -> item.job().id()))
                .toList();
    }

    @Transactional
    public Report createReport(ReportInput input) {
        Job job = input.jobId() == null || input.jobId().isBlank() ? null :
                findJob(input.jobId()).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "演示岗位不存在"));
        String jd = input.jdText().strip();
        String title = job == null ? safe(input.jobTitle()) : job.title();
        if (title.isBlank()) title = "自定义 JD";
        List<Skill> currentSkills = skills();
        LinkedHashSet<String> recognized = new LinkedHashSet<>(ALIASES.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(alias -> containsTerm(jd, alias)))
                .map(Map.Entry::getKey).sorted().toList());
        currentSkills.stream().map(Skill::name).filter(name -> containsTerm(jd, name))
                .filter(name -> recognized.stream().noneMatch(tag -> aliases(tag).stream()
                        .anyMatch(alias -> alias.equalsIgnoreCase(name))))
                .forEach(recognized::add);
        List<String> tags = List.copyOf(recognized);
        Match match = compare(tags, currentSkills, new Preference("", "", null), null);
        List<String> actions = new ArrayList<>();
        if (tags.isEmpty()) actions.add("JD 中未识别到可比对的明确技能。补充技术、工具或职责要求后重新分析。");
        match.gaps().stream().limit(5).forEach(gap -> {
            String tag = gap.split("：", 2)[0];
            actions.add(PRACTICE_ACTIONS.getOrDefault(tag,
                    "围绕“" + tag + "”完成一个可验证的小项目，并把成果记录到能力图谱。"));
        });
        if (!match.matched().isEmpty()) actions.add("将已有匹配能力对应的实践证据整理成可展示的项目案例。");
        if (actions.isEmpty()) actions.add("继续用真实项目验证当前能力，并定期更新能力图谱。");
        Report report = new Report(UUID.randomUUID().toString(), job == null ? null : job.id(), title, jd,
                match.score(), match.matched(), match.gaps(), List.copyOf(actions), tags, Instant.now());
        try {
            jdbc.update("INSERT INTO career_report(id,user_id,job_id,job_title,jd_text,report_json,created_at) VALUES(?,?,?,?,?,?,?)",
                    report.id(), currentUser.id(), report.jobId(), report.jobTitle(), report.jdText(),
                    json.writeValueAsString(report), Timestamp.from(report.createdAt()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("报告保存失败", e);
        }
        return report;
    }

    @Transactional(readOnly = true)
    public List<Report> reports() {
        return jdbc.query("SELECT report_json FROM career_report WHERE user_id = ? ORDER BY created_at DESC, id DESC",
                (rs, row) -> decode(rs.getString(1)), currentUser.id());
    }

    @Transactional(readOnly = true)
    public Report report(String id) {
        return jdbc.query("SELECT report_json FROM career_report WHERE id = ? AND user_id = ?",
                (rs, row) -> decode(rs.getString(1)), id, currentUser.id())
                .stream().findFirst().orElseThrow(() -> error(HttpStatus.NOT_FOUND, "报告不存在"));
    }

    private Report decode(String value) {
        try { return json.readValue(value, Report.class); }
        catch (JsonProcessingException e) { throw new IllegalStateException("报告读取失败", e); }
    }

    private List<Job> allJobs() {
        return jdbc.query("SELECT id,title,company,category,city,job_type,salary,description,requirements,skill_tags FROM career_job ORDER BY sort_order",
                (rs, row) -> job(rs));
    }

    private Optional<Job> findJob(String id) {
        return jdbc.query("SELECT id,title,company,category,city,job_type,salary,description,requirements,skill_tags FROM career_job WHERE id = ?",
                (rs, row) -> job(rs), id).stream().findFirst();
    }

    private Job job(ResultSet rs) throws SQLException {
        return new Job(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9), List.of(rs.getString(10).split("\\|")));
    }

    private List<Skill> skills() {
        return jdbc.query("SELECT id,name,skill_level,status FROM ability_skill WHERE user_id = ?",
                (rs, row) -> new Skill(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getString(4)), currentUser.id());
    }

    private Match compare(List<String> tags, List<Skill> skills, Preference pref, Job job) {
        List<String> matched = new ArrayList<>();
        List<String> gaps = new ArrayList<>();
        double total = 0;
        for (String tag : tags) {
            Skill skill = skills.stream().filter(item -> aliases(tag).stream()
                    .anyMatch(alias -> alias.equalsIgnoreCase(item.id()) || alias.equalsIgnoreCase(item.name()))).findFirst().orElse(null);
            if (skill != null && !skill.status().equals("gap") && !skill.status().equals("target")) {
                total += Math.min(1, skill.level() / 4.0);
                if (skill.level() >= 2) matched.add(tag + "（能力等级 " + skill.level() + "/4）");
                else gaps.add(tag + "：已有基础，建议补充实践证据");
            } else gaps.add(tag + "：能力图谱中尚无已掌握记录");
        }
        int score = tags.isEmpty() ? 0 : (int) Math.round(total / tags.size() * 100);
        if (job != null) {
            if (!pref.category().isBlank() && pref.category().equals(job.category())) score = Math.min(100, score + 5);
            if (!pref.city().isBlank() && pref.city().equals(job.city())) score = Math.min(100, score + 5);
        }
        return new Match(job == null ? null : job.id(), score, List.copyOf(matched), List.copyOf(gaps));
    }

    private List<String> aliases(String tag) {
        return Stream.concat(Stream.of(tag), ALIASES.getOrDefault(tag, List.of()).stream()).toList();
    }

    private boolean containsTerm(String text, String term) {
        String haystack = text.toLowerCase(Locale.ROOT);
        String needle = term.toLowerCase(Locale.ROOT);
        if (needle.matches("[a-z0-9+.#/-]{1,3}"))
            return Pattern.compile("(?<![a-z0-9])" + Pattern.quote(needle) + "(?![a-z0-9])").matcher(haystack).find();
        return haystack.contains(needle);
    }

    private void save(Preference pref) {
        if (jdbc.update("UPDATE career_preference SET category = ?, city = ?, target_job_id = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?",
                pref.category(), pref.city(), pref.targetJobId(), currentUser.id()) == 0) {
            jdbc.update("INSERT INTO career_preference(user_id,category,city,target_job_id) VALUES(?,?,?,?)",
                    currentUser.id(), pref.category(), pref.city(), pref.targetJobId());
        }
    }

    private String safe(String value) { return value == null ? "" : value.strip(); }
    private ResponseStatusException error(HttpStatus status, String message) { return new ResponseStatusException(status, message); }
    private record Skill(String id, String name, int level, String status) {}
}
