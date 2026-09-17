CREATE TABLE user_profile (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    avatar VARCHAR(500),
    goal_title VARCHAR(300) NOT NULL,
    goal_deadline DATE NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE profile_goal (
    profile_id VARCHAR(64) NOT NULL,
    position_index INT NOT NULL,
    title VARCHAR(300) NOT NULL,
    PRIMARY KEY (profile_id, position_index),
    FOREIGN KEY (profile_id) REFERENCES user_profile(id)
);

INSERT INTO user_profile (id, name, avatar, goal_title, goal_deadline)
VALUES ('demo-user', '林澈', NULL, '独立完成一个可复现的网络服务部署', '2026-11-13');
INSERT INTO profile_goal (profile_id, position_index, title)
VALUES ('demo-user', 0, '独立完成一个可复现的网络服务部署');
