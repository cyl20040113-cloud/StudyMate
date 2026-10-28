-- 手工执行的 MySQL 8 数据完整性迁移。
-- 执行 ALTER 前必须先处理下面检查语句返回的重复数据。
-- 当前仓库不使用 Flyway，因此该文件不会随应用启动自动执行。

SELECT userAccount, COUNT(*) AS duplicateCount
FROM user
WHERE isDelete = 0
GROUP BY userAccount
HAVING COUNT(*) > 1;

SELECT planetCode, COUNT(*) AS duplicateCount
FROM user
WHERE isDelete = 0 AND planetCode IS NOT NULL
GROUP BY planetCode
HAVING COUNT(*) > 1;

SELECT userId, teamId, COUNT(*) AS duplicateCount
FROM user_team
WHERE isDelete = 0
GROUP BY userId, teamId
HAVING COUNT(*) > 1;

-- 删除状态使用 NULL 作为唯一索引标记，使多条历史逻辑删除记录可以共存。
ALTER TABLE user
    ADD COLUMN activeUniqueMarker TINYINT
        GENERATED ALWAYS AS (CASE WHEN isDelete = 0 THEN 1 ELSE NULL END) STORED,
    ADD UNIQUE KEY uk_user_account_active (userAccount, activeUniqueMarker),
    ADD UNIQUE KEY uk_planet_code_active (planetCode, activeUniqueMarker);

ALTER TABLE user_team
    ADD COLUMN activeUniqueMarker TINYINT
        GENERATED ALWAYS AS (CASE WHEN isDelete = 0 THEN 1 ELSE NULL END) STORED,
    ADD UNIQUE KEY uk_user_team_active (userId, teamId, activeUniqueMarker);
