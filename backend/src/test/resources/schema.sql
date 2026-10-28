DROP TABLE IF EXISTS user_team;
DROP TABLE IF EXISTS team;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(256),
    userAccount VARCHAR(256) NOT NULL,
    avatarUrl VARCHAR(1024),
    gender TINYINT,
    profile VARCHAR(512),
    userPassword VARCHAR(512) NOT NULL,
    phone VARCHAR(128),
    email VARCHAR(512),
    tags VARCHAR(1024),
    userStatus INT NOT NULL DEFAULT 0,
    createTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updateTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    isDelete TINYINT NOT NULL DEFAULT 0,
    userRole INT NOT NULL DEFAULT 0,
    planetCode VARCHAR(512),
    CONSTRAINT uk_test_user_account UNIQUE (userAccount),
    CONSTRAINT uk_test_planet_code UNIQUE (planetCode)
);

CREATE TABLE team (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(256) NOT NULL,
    description VARCHAR(1024),
    maxNum INT NOT NULL DEFAULT 1,
    expireTime TIMESTAMP,
    userId BIGINT,
    status INT NOT NULL DEFAULT 0,
    password VARCHAR(512),
    createTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updateTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    isDelete TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE user_team (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    userId BIGINT,
    teamId BIGINT,
    joinTime TIMESTAMP,
    createTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updateTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    isDelete TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_test_user_team UNIQUE (userId, teamId)
);
