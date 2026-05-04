-- 默认模板和快捷操作数据
-- 运行此脚本后，用户ID需要替换为实际用户ID

-- 默认分类
INSERT INTO template_category (user_id, name, sort_order) VALUES
(1, '学习笔记', 1),
(1, '社交媒体', 2),
(1, '工作汇报', 3);

-- 默认模板
INSERT INTO template (user_id, category_id, name, style, length, output_types, custom_prompt_ids) VALUES
-- 学习笔记模板
(1, 1, '详细学习笔记', 'detailed', 'long', '["summary", "article", "card"]', '[]'),
-- 小红书模板
(1, 2, '小红书爆款文案', 'creative', 'standard', '["xiaohongshu", "card"]', '[]'),
-- 工作汇报模板
(1, 3, '简洁工作汇报', 'concise', 'short', '["summary", "article"]', '[]'),
-- 通用模板
(1, NULL, '快速总结', 'concise', 'standard', '["summary"]', '[]');

-- 默认快捷操作
INSERT INTO quick_action (user_id, name, steps, apply_scope) VALUES
-- 复制当前内容
(1, '复制当前内容', '[{"action":"copy","params":{}}]', 'single'),
-- 导出 Markdown
(1, '导出 Markdown', '[{"action":"export","params":{}}]', 'single'),
-- 复制并导出
(1, '复制并导出', '[{"action":"copy","params":{}},{"action":"export","params":{}}]', 'single'),
-- 重新生成
(1, '重新生成当前内容', '[{"action":"regenerate","params":{}}]', 'single');
