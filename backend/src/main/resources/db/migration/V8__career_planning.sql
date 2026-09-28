CREATE TABLE career_job (
    id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    company VARCHAR(160) NOT NULL,
    category VARCHAR(80) NOT NULL,
    city VARCHAR(80) NOT NULL,
    job_type VARCHAR(40) NOT NULL,
    salary VARCHAR(80) NOT NULL,
    description TEXT NOT NULL,
    requirements TEXT NOT NULL,
    skill_tags VARCHAR(1000) NOT NULL,
    sort_order INT NOT NULL
);

CREATE TABLE career_preference (
    user_id VARCHAR(64) PRIMARY KEY,
    category VARCHAR(80) NOT NULL DEFAULT '',
    city VARCHAR(80) NOT NULL DEFAULT '',
    target_job_id VARCHAR(64),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_career_target_job FOREIGN KEY (target_job_id) REFERENCES career_job(id)
);

CREATE TABLE career_report (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    job_id VARCHAR(64),
    job_title VARCHAR(160) NOT NULL,
    jd_text TEXT NOT NULL,
    report_json LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_career_report_job FOREIGN KEY (job_id) REFERENCES career_job(id)
);
CREATE INDEX idx_career_report_user_time ON career_report(user_id, created_at);

INSERT INTO career_job (id,title,company,category,city,job_type,salary,description,requirements,skill_tags,sort_order) VALUES
('job-backend-1','后端开发工程师','星桥科技（演示）','后端开发','上海','全职','18–28K','参与业务接口设计、服务开发与线上问题排查。','熟悉 Python、HTTP、SQL、Git；了解 Docker 与 Linux。','Python|HTTP|SQL|Git|Docker|Linux',1),
('job-ops-1','云平台运维工程师','澄海网络（演示）','运维与云计算','杭州','全职','16–25K','维护云上服务，改进部署流程与监控告警。','具备 Linux、Shell、Docker、监控诊断、服务安全基础。','Linux|Shell|Docker|监控|安全',2),
('job-devops-1','DevOps 工程师','路标软件（演示）','运维与云计算','上海','全职','20–32K','建设持续交付与服务发布体系。','熟悉 Git、Linux、Shell、Docker、HTTP 和监控。','Git|Linux|Shell|Docker|HTTP|监控',3),
('job-frontend-1','前端开发工程师','远帆互动（演示）','前端开发','北京','全职','18–30K','开发面向用户的 Web 产品和组件。','熟悉 HTML、CSS、JavaScript、TypeScript、Vue、Git。','HTML|CSS|JavaScript|TypeScript|Vue|Git',4),
('job-fullstack-1','全栈开发工程师','知行工坊（演示）','全栈开发','深圳','全职','20–34K','负责前后端功能的设计、开发和部署。','熟悉 Vue、TypeScript、Python、SQL、HTTP、Docker。','Vue|TypeScript|Python|SQL|HTTP|Docker',5),
('job-data-1','数据分析师','数原研究（演示）','数据分析','上海','全职','15–24K','建立指标分析和业务洞察报告。','熟悉 Python、SQL、数据分析、可视化和沟通表达。','Python|SQL|数据分析|可视化|沟通',6),
('job-security-1','安全工程师','云盾实验室（演示）','安全','北京','全职','19–31K','开展服务安全评估、漏洞排查与加固。','理解 TCP/IP、Linux、服务安全、Python、HTTP。','TCP/IP|Linux|安全|Python|HTTP',7),
('job-product-1','产品经理','白帆产品（演示）','产品与设计','广州','全职','16–26K','梳理需求、设计方案并推动跨团队交付。','需要产品设计、数据分析、沟通、项目管理能力。','产品设计|数据分析|沟通|项目管理',8),
('job-test-1','测试开发工程师','晨星质量（演示）','测试','成都','全职','15–24K','建立接口测试与自动化质量保障流程。','熟悉 Python、HTTP、SQL、Git、自动化测试。','Python|HTTP|SQL|Git|测试',9),
('job-intern-1','后端开发实习生','微光实验室（演示）','后端开发','远程','实习','200–300/天','协助编写接口、测试和开发文档。','了解 Python、Git、HTTP 和 SQL。','Python|Git|HTTP|SQL',10),
('job-intern-2','前端开发实习生','棱镜工作室（演示）','前端开发','杭州','实习','180–260/天','参与页面与组件开发。','了解 HTML、CSS、JavaScript、Vue、Git。','HTML|CSS|JavaScript|Vue|Git',11),
('job-network-1','网络服务工程师','拓路系统（演示）','运维与云计算','远程','全职','17–27K','部署和维护网络服务，处理连接和可用性问题。','熟悉 TCP/IP、HTTP、Linux、Shell、Docker、监控。','TCP/IP|HTTP|Linux|Shell|Docker|监控',12);
