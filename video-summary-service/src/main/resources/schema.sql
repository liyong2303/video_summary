CREATE DATABASE IF NOT EXISTS video_summary DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE video_summary;

CREATE TABLE IF NOT EXISTS task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID（Phase 1暂用0表示匿名）',
    bvid VARCHAR(20) NOT NULL COMMENT 'BV号',
    cid BIGINT DEFAULT NULL COMMENT '视频CID',
    video_title VARCHAR(200) DEFAULT NULL COMMENT '视频标题',
    video_duration INT DEFAULT NULL COMMENT '视频时长（秒）',
    cover_url VARCHAR(500) DEFAULT NULL COMMENT '封面URL',
    subtitle_storage_path VARCHAR(500) DEFAULT NULL COMMENT '字幕文件存储路径',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '任务状态: pending/processing/completed/partially_completed/failed/cancelled',
    error_message VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    completed_at DATETIME DEFAULT NULL COMMENT '完成时间',
    UNIQUE KEY uk_user_bvid (user_id, bvid) COMMENT '同用户同BV号去重',
    INDEX idx_status (status) COMMENT '状态索引',
    INDEX idx_created_at (created_at) COMMENT '创建时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务表';
