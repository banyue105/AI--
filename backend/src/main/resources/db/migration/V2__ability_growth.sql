CREATE TABLE ability_graph_state (
    user_id VARCHAR(64) PRIMARY KEY,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_ability_graph_user FOREIGN KEY (user_id) REFERENCES user_profile(id)
);

CREATE TABLE ability_skill (
    user_id VARCHAR(64) NOT NULL,
    id VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    normalized_name VARCHAR(120) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    skill_level INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    x DOUBLE NOT NULL,
    y DOUBLE NOT NULL,
    sort_order INTEGER NOT NULL,
    PRIMARY KEY (user_id, id),
    CONSTRAINT uk_ability_skill_name UNIQUE (user_id, normalized_name),
    CONSTRAINT fk_ability_skill_user FOREIGN KEY (user_id) REFERENCES user_profile(id),
    CONSTRAINT ck_ability_skill_level CHECK (skill_level BETWEEN 0 AND 4),
    CONSTRAINT ck_ability_skill_status CHECK (status IN ('mastered', 'developing', 'gap', 'target'))
);

CREATE TABLE ability_relation (
    user_id VARCHAR(64) NOT NULL,
    from_id VARCHAR(64) NOT NULL,
    to_id VARCHAR(64) NOT NULL,
    relation_type VARCHAR(20) NOT NULL,
    confidence DOUBLE NOT NULL,
    sort_order INTEGER NOT NULL,
    PRIMARY KEY (user_id, from_id, to_id, relation_type),
    CONSTRAINT fk_ability_relation_from FOREIGN KEY (user_id, from_id) REFERENCES ability_skill(user_id, id),
    CONSTRAINT fk_ability_relation_to FOREIGN KEY (user_id, to_id) REFERENCES ability_skill(user_id, id),
    CONSTRAINT ck_ability_relation_confidence CHECK (confidence >= 0 AND confidence <= 1),
    CONSTRAINT ck_ability_relation_type CHECK (relation_type IN ('prerequisite', 'related')),
    CONSTRAINT ck_ability_relation_self CHECK (from_id <> to_id)
);

CREATE TABLE ability_evidence (
    user_id VARCHAR(64) NOT NULL,
    id VARCHAR(64) NOT NULL,
    title VARCHAR(200) NOT NULL,
    note VARCHAR(4000) NOT NULL,
    created_at DATE NOT NULL,
    PRIMARY KEY (user_id, id),
    CONSTRAINT fk_ability_evidence_user FOREIGN KEY (user_id) REFERENCES user_profile(id)
);

CREATE TABLE ability_skill_evidence (
    user_id VARCHAR(64) NOT NULL,
    skill_id VARCHAR(64) NOT NULL,
    evidence_id VARCHAR(64) NOT NULL,
    PRIMARY KEY (user_id, skill_id, evidence_id),
    CONSTRAINT fk_ability_link_skill FOREIGN KEY (user_id, skill_id) REFERENCES ability_skill(user_id, id),
    CONSTRAINT fk_ability_link_evidence FOREIGN KEY (user_id, evidence_id) REFERENCES ability_evidence(user_id, id)
);

INSERT INTO ability_graph_state (user_id, updated_at) VALUES ('demo-user', '2026-09-14 13:32:00');

INSERT INTO ability_skill (user_id,id,name,normalized_name,description,skill_level,status,x,y,sort_order) VALUES
('demo-user','python','Python','python','脚本与后端开发基础',3,'mastered',48,72,0),
('demo-user','linux','Linux','linux','常用命令与系统管理',2,'mastered',48,202,1),
('demo-user','tcpip','TCP/IP','tcp/ip','网络分层与传输协议',2,'developing',248,72,2),
('demo-user','git','Git 协作','git 协作','版本控制与团队工作流',2,'mastered',248,202,3),
('demo-user','http','HTTP 服务','http 服务','请求、响应与接口设计',1,'developing',450,72,4),
('demo-user','shell','Shell 自动化','shell 自动化','部署脚本与环境配置',1,'developing',450,202,5),
('demo-user','docker','容器化','容器化','镜像、容器与 Compose',0,'gap',650,202,6),
('demo-user','security','服务安全','服务安全','权限、密钥与攻击面',0,'gap',650,332,7),
('demo-user','monitor','监控诊断','监控诊断','日志、指标与故障定位',0,'gap',850,72,8),
('demo-user','deploy','网络服务部署','网络服务部署','独立完成可复现的服务上线',0,'target',850,202,9);

INSERT INTO ability_relation (user_id,from_id,to_id,relation_type,confidence,sort_order) VALUES
('demo-user','python','http','prerequisite',0.95,0),
('demo-user','tcpip','http','prerequisite',0.90,1),
('demo-user','linux','shell','prerequisite',0.92,2),
('demo-user','git','shell','related',0.72,3),
('demo-user','shell','docker','prerequisite',0.88,4),
('demo-user','tcpip','security','prerequisite',0.84,5),
('demo-user','http','monitor','prerequisite',0.85,6),
('demo-user','docker','deploy','prerequisite',0.95,7),
('demo-user','security','deploy','prerequisite',0.86,8),
('demo-user','monitor','deploy','prerequisite',0.90,9);

INSERT INTO ability_evidence (user_id,id,title,note,created_at) VALUES
('demo-user','ev-python','课程项目','使用 FastAPI 完成数据接口','2026-09-10'),
('demo-user','ev-linux','实践记录','独立配置 Linux 开发环境','2026-09-07');

INSERT INTO ability_skill_evidence (user_id,skill_id,evidence_id) VALUES
('demo-user','python','ev-python'),
('demo-user','linux','ev-linux');
