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

CREATE TABLE IF NOT EXISTS task_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    output_type VARCHAR(20) NOT NULL COMMENT '输出类型: summary/article/card/xiaohongshu',
    content LONGTEXT COMMENT '生成内容',
    model_used VARCHAR(50) DEFAULT NULL COMMENT '使用的模型',
    input_tokens INT DEFAULT NULL COMMENT '输入token数',
    output_tokens INT DEFAULT NULL COMMENT '输出token数',
    status VARCHAR(20) NOT NULL DEFAULT 'completed' COMMENT '状态: completed/failed/skipped',
    INDEX idx_task_id (task_id) COMMENT '任务ID索引',
    INDEX idx_output_type (output_type) COMMENT '输出类型索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务结果表';

CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    password_hash VARCHAR(200) NOT NULL COMMENT '密码哈希',
    role VARCHAR(20) NOT NULL DEFAULT 'free' COMMENT '角色: free/paid/admin',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username) COMMENT '用户名唯一',
    UNIQUE KEY uk_email (email) COMMENT '邮箱唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS daily_usage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    usage_date DATE NOT NULL COMMENT '使用日期',
    count INT NOT NULL DEFAULT 0 COMMENT '使用次数',
    UNIQUE KEY uk_user_date (user_id, usage_date) COMMENT '每用户每天唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日用量表';

CREATE TABLE IF NOT EXISTS task_result_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    task_result_id BIGINT NOT NULL COMMENT '关联的 task_result.id',
    task_id BIGINT NOT NULL COMMENT '任务ID（冗余字段，方便查询）',
    output_type VARCHAR(20) NOT NULL COMMENT '输出类型（冗余字段）',
    content LONGTEXT COMMENT '内容快照',
    version INT NOT NULL COMMENT '版本号（从1开始递增）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_task_result_id (task_result_id),
    INDEX idx_task_id_output_type (task_id, output_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务结果编辑历史表';

CREATE TABLE IF NOT EXISTS user_preference (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    style VARCHAR(20) NOT NULL DEFAULT 'concise' COMMENT '默认风格: academic/casual/concise',
    length VARCHAR(20) NOT NULL DEFAULT 'standard' COMMENT '默认字数: short/standard/long',
    output_types JSON COMMENT '默认输出格式: ["summary", "article", "card", "xiaohongshu"]',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户偏好表';

CREATE TABLE IF NOT EXISTS custom_prompt (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    name VARCHAR(100) NOT NULL COMMENT 'Prompt名称',
    output_type VARCHAR(20) NOT NULL COMMENT '输出类型: summary/article/card/xiaohongshu',
    system_prompt TEXT COMMENT 'System prompt',
    user_prompt TEXT COMMENT 'User prompt模板',
    is_default BOOLEAN DEFAULT FALSE COMMENT '是否为默认Prompt',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_output_type (output_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户自定义Prompt表';
