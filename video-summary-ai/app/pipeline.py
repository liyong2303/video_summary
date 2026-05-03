"""
AI Pipeline engine.

Step 1: Summary (sequential, must complete first)
Step 2-4: Article, Cards, Xiaohongshu (parallel, depend on Step 1)
"""
import asyncio
import time
import logging
from dataclasses import dataclass, field
from enum import Enum
from typing import AsyncGenerator

from app.llm import stream_chat, chat
from app.prompts import (
    summary_prompt,
    article_prompt,
    card_prompt,
    xiaohongshu_prompt,
    compress_prompt,
)
from app.config import settings

logger = logging.getLogger(__name__)


class StepType(str, Enum):
    SUMMARY = "summary"
    ARTICLE = "article"
    CARD = "card"
    XIAOHONGSHU = "xiaohongshu"


class StepStatus(str, Enum):
    PENDING = "pending"
    RUNNING = "running"
    COMPLETED = "completed"
    FAILED = "failed"
    SKIPPED = "skipped"


@dataclass
class StepResult:
    step: StepType
    status: StepStatus = StepStatus.PENDING
    content: str = ""
    tokens_used: int = 0
    error: str = ""
    started_at: float = 0
    completed_at: float = 0


@dataclass
class PipelineResult:
    task_id: str
    steps: dict[StepType, StepResult] = field(default_factory=dict)
    started_at: float = 0
    completed_at: float = 0

    def __post_init__(self):
        for step_type in StepType:
            self.steps[step_type] = StepResult(step=step_type)


def estimate_tokens(text: str) -> int:
    """Rough estimate: 1 Chinese char ≈ 1.5 tokens, 1 English word ≈ 1.3 tokens."""
    chinese_chars = sum(1 for c in text if '\u4e00' <= c <= '\u9fff')
    other_chars = len(text) - chinese_chars
    return int(chinese_chars * 1.5 + other_chars * 0.25)


async def compress_subtitle(subtitle_text: str) -> str:
    """Compress long subtitles (>8K token) before pipeline."""
    tokens = estimate_tokens(subtitle_text)
    if tokens <= settings.subtitle_compress_threshold:
        return subtitle_text

    logger.info(f"Compressing subtitle: {tokens} tokens > {settings.subtitle_compress_threshold} threshold")
    try:
        system, prompt = compress_prompt(subtitle_text)
        compressed = await asyncio.wait_for(
            chat(prompt, system),
            timeout=settings.step_timeout_seconds,
        )
        logger.info(f"Subtitle compressed: {tokens} -> {estimate_tokens(compressed)} tokens")
        return compressed
    except asyncio.TimeoutError:
        logger.warning("Subtitle compression timed out, truncating instead")
        # Fallback: truncate to ~8K tokens (~16000 Chinese chars)
        return subtitle_text[:16000]
    except Exception as e:
        logger.warning(f"Subtitle compression failed: {e}, truncating instead")
        return subtitle_text[:16000]


async def _run_step(
    step_type: StepType,
    prompt_text: str,
    system_text: str,
    result: StepResult,
    on_chunk=None,
) -> StepResult:
    """Run a single pipeline step with streaming."""
    result.status = StepStatus.RUNNING
    result.started_at = time.time()

    try:
        content_parts = []
        async for chunk in stream_chat(prompt_text, system_text):
            content_parts.append(chunk)
            if on_chunk:
                await on_chunk(step_type, chunk)

        result.content = "".join(content_parts)
        result.status = StepStatus.COMPLETED
        result.tokens_used = estimate_tokens(result.content)
        logger.info(f"Step {step_type.value} completed: {len(result.content)} chars")

    except asyncio.TimeoutError:
        result.status = StepStatus.FAILED
        result.error = f"Step timed out after {settings.step_timeout_seconds}s"
        logger.error(f"Step {step_type.value} timed out")
    except Exception as e:
        result.status = StepStatus.FAILED
        result.error = str(e)
        logger.error(f"Step {step_type.value} failed: {e}")
    finally:
        result.completed_at = time.time()

    return result


async def execute_pipeline(
    task_id: str,
    subtitle_text: str,
    style: str = "concise",
    length: str = "standard",
    on_chunk=None,
    on_step_start=None,
    on_step_complete=None,
) -> PipelineResult:
    """
    Execute the full AI pipeline.

    Step 1: Summary (must complete first)
    Step 2-4: Article, Cards, Xiaohongshu (parallel)
    """
    pipeline = PipelineResult(task_id=task_id)
    pipeline.started_at = time.time()

    # Compress long subtitles
    subtitle_text = await compress_subtitle(subtitle_text)

    # Step 1: Generate summary (sequential)
    if on_step_start:
        await on_step_start(StepType.SUMMARY)

    system, prompt = summary_prompt(subtitle_text, style, length)
    await _run_step(
        StepType.SUMMARY, prompt, system,
        pipeline.steps[StepType.SUMMARY],
        on_chunk=on_chunk,
    )

    if on_step_complete:
        await on_step_complete(StepType.SUMMARY, pipeline.steps[StepType.SUMMARY])

    # If summary failed, skip remaining steps
    if pipeline.steps[StepType.SUMMARY].status != StepStatus.COMPLETED:
        for step_type in [StepType.ARTICLE, StepType.CARD, StepType.XIAOHONGSHU]:
            pipeline.steps[step_type].status = StepStatus.SKIPPED
            pipeline.steps[step_type].error = "Skipped: summary step failed"
        pipeline.completed_at = time.time()
        return pipeline

    summary_content = pipeline.steps[StepType.SUMMARY].content

    # Steps 2-4: Parallel execution
    parallel_steps = [
        (StepType.ARTICLE, article_prompt(summary_content, style, length)),
        (StepType.CARD, card_prompt(summary_content, style, length)),
        (StepType.XIAOHONGSHU, xiaohongshu_prompt(summary_content, style, length)),
    ]

    async def run_parallel_step(step_type, system_text, prompt_text):
        if on_step_start:
            await on_step_start(step_type)
        result = await _run_step(
            step_type, prompt_text, system_text,
            pipeline.steps[step_type],
            on_chunk=on_chunk,
        )
        if on_step_complete:
            await on_step_complete(step_type, result)
        return result

    await asyncio.gather(*[
        run_parallel_step(step_type, system_text, prompt_text)
        for step_type, (system_text, prompt_text) in parallel_steps
    ], return_exceptions=True)

    pipeline.completed_at = time.time()
    logger.info(
        f"Pipeline {task_id} completed in "
        f"{pipeline.completed_at - pipeline.started_at:.1f}s"
    )
    return pipeline


async def execute_single_step(
    task_id: str,
    subtitle_text: str,
    output_type: StepType,
    style: str = "concise",
    length: str = "standard",
) -> StepResult:
    """
    Execute a single pipeline step.
    For steps that depend on summary (article/card/xiaohongshu),
    summary is generated first as a prerequisite.
    """
    step_type = output_type

    subtitle_text = await compress_subtitle(subtitle_text)

    if step_type == StepType.SUMMARY:
        result = StepResult(step=step_type)
        system, prompt = summary_prompt(subtitle_text, style, length)
        try:
            await asyncio.wait_for(
                _run_step(step_type, prompt, system, result),
                timeout=settings.step_timeout_seconds,
            )
        except asyncio.TimeoutError:
            result.status = StepStatus.FAILED
            result.error = f"Step timed out after {settings.step_timeout_seconds}s"
            result.completed_at = time.time()
            logger.error(f"Step {step_type.value} timed out")
        return result

    # Dependent steps: generate summary first (silently)
    summary_result = StepResult(step=StepType.SUMMARY)
    system, prompt = summary_prompt(subtitle_text, style, length)
    try:
        await asyncio.wait_for(
            _run_step(StepType.SUMMARY, prompt, system, summary_result),
            timeout=settings.step_timeout_seconds,
        )
    except asyncio.TimeoutError:
        summary_result.status = StepStatus.FAILED
        summary_result.error = f"Step timed out after {settings.step_timeout_seconds}s"
        summary_result.completed_at = time.time()
        logger.error(f"Step {StepType.SUMMARY.value} timed out")

    if summary_result.status != StepStatus.COMPLETED:
        result = StepResult(step=step_type)
        result.status = StepStatus.FAILED
        result.error = f"Cannot generate {step_type.value}: summary step failed — {summary_result.error}"
        t = time.time()
        result.started_at = t
        result.completed_at = t
        return result

    prompt_fns = {
        StepType.ARTICLE: article_prompt,
        StepType.CARD: card_prompt,
        StepType.XIAOHONGSHU: xiaohongshu_prompt,
    }
    system, prompt = prompt_fns[step_type](summary_result.content, style, length)
    result = StepResult(step=step_type)
    try:
        await asyncio.wait_for(
            _run_step(step_type, prompt, system, result),
            timeout=settings.step_timeout_seconds,
        )
    except asyncio.TimeoutError:
        result.status = StepStatus.FAILED
        result.error = f"Step timed out after {settings.step_timeout_seconds}s"
        result.completed_at = time.time()
        logger.error(f"Step {step_type.value} timed out")
    return result
