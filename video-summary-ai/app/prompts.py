"""Prompt templates for each pipeline step."""


def summary_prompt(subtitle_text: str) -> tuple[str, str]:
    """Step 1: Generate summary from subtitle text."""
    system = "你是一个专业的视频内容总结助手。你需要从视频字幕中提取核心观点，生成结构化的总结。"
    prompt = f"""请根据以下视频字幕内容，生成一份结构化的视频总结。

要求：
1. 列出3-5个核心观点，每个观点附带时间戳（如果字幕中有时间信息）
2. 最后用一段话总结视频的核心思想
3. 语言简洁有力，避免冗余
4. 保留关键数据和专有名词

字幕内容：
{subtitle_text}"""
    return system, prompt


def article_prompt(summary: str) -> tuple[str, str]:
    """Step 2: Generate article from summary."""
    system = "你是一个专业的内容创作者，擅长将视频总结扩展为结构清晰的文章。"
    prompt = f"""请根据以下视频总结，扩展为一篇结构清晰的文章。

要求：
1. 标题吸引人，体现核心观点
2. 开头用2-3句话概述视频内容，引起读者兴趣
3. 正文每个核心观点单独成段，补充必要的解释和背景
4. 结尾总结核心价值，给出行动建议或思考
5. 文章总长度800-1200字
6. 语气专业但不枯燥，适合阅读

视频总结：
{summary}"""
    return system, prompt


def card_prompt(summary: str) -> tuple[str, str]:
    """Step 3: Generate learning cards from summary."""
    system = "你是一个学习教育专家，擅长将知识提炼为便于记忆和复习的学习卡片。"
    prompt = f"""请根据以下视频总结，生成5-8张学习卡片。

要求：
每张卡片格式：
📌 [主题]
❓ 问题：[一个检验理解的问题]
💡 要点：[核心知识点，1-2句话]
🔗 关联：[与其他卡片的联系]

注意：
- 问题要有启发性，不是简单的事实回忆
- 要点精炼，突出关键词
- 卡片之间有逻辑递进关系

视频总结：
{summary}"""
    return system, prompt


def xiaohongshu_prompt(summary: str) -> tuple[str, str]:
    """Step 4: Generate Xiaohongshu (小红书) post from summary."""
    system = "你是一个小红书内容创作专家，擅长写出吸引眼球、易于传播的笔记。"
    prompt = f"""请根据以下视频总结，生成一篇小红书笔记。

要求：
1. 标题用emoji开头，15字以内，吸引点击
2. 正文结构：
   - 开头：用1-2句话抛出痛点或悬念
   - 中间：3-5个要点，每点一行，用emoji标注
   - 结尾：一句话总结+互动引导
3. 适当使用emoji增加趣味性
4. 总长度300-500字
5. 语气轻松活泼，像跟朋友分享
6. 加上3-5个相关话题标签

视频总结：
{summary}"""
    return system, prompt


def compress_prompt(subtitle_text: str) -> tuple[str, str]:
    """Compress long subtitles before main pipeline."""
    system = "你是一个文本压缩助手。你需要保留视频字幕的核心信息，去除重复和冗余内容。"
    prompt = f"""以下是一段过长的视频字幕。请压缩保留核心信息：

要求：
1. 保留所有关键观点和论据
2. 去除重复表述和口语化填充
3. 保持时间顺序逻辑
4. 压缩后长度为原文的50%左右

字幕内容：
{subtitle_text}"""
    return system, prompt
