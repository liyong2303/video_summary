import json
import logging
from typing import Optional

from fastapi import FastAPI, Header, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from pydantic import BaseModel

from app.config import settings
from app.pipeline import (
    StepType,
    StepStatus,
    execute_pipeline,
)

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="VideoSum AI Pipeline", version="0.1.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


def verify_secret(x_internal_secret: Optional[str] = Header(None)):
    if x_internal_secret != settings.internal_secret:
        raise HTTPException(status_code=401, detail="Invalid internal secret")


@app.get("/health")
async def health():
    return {"status": "ok", "service": "video-summary-ai"}


class PipelineRequest(BaseModel):
    task_id: str
    subtitle_text: str


@app.post("/pipeline/execute")
async def pipeline_execute(
    request: PipelineRequest,
    x_internal_secret: Optional[str] = Header(None),
):
    """Execute pipeline and return results (non-streaming, for simple integration)."""
    verify_secret(x_internal_secret)

    result = await execute_pipeline(
        task_id=request.task_id,
        subtitle_text=request.subtitle_text,
    )

    return {
        "task_id": result.task_id,
        "steps": {
            step.value: {
                "status": step_result.status.value,
                "content": step_result.content,
                "tokens_used": step_result.tokens_used,
                "error": step_result.error,
                "duration": step_result.completed_at - step_result.started_at if step_result.completed_at else 0,
            }
            for step, step_result in result.steps.items()
        },
        "total_duration": result.completed_at - result.started_at if result.completed_at else 0,
    }


@app.post("/pipeline/execute/stream")
async def pipeline_execute_stream(
    request: PipelineRequest,
    x_internal_secret: Optional[str] = Header(None),
):
    """Execute pipeline with SSE streaming output."""
    verify_secret(x_internal_secret)

    async def event_generator():
        step_chunk_buffers = {}

        async def on_step_start(step_type: StepType):
            event = {"type": "step_start", "step": step_type.value}
            yield f"data: {json.dumps(event, ensure_ascii=False)}\n\n"

        async def on_chunk(step_type: StepType, chunk: str):
            # Buffer chunks for step_complete
            if step_type not in step_chunk_buffers:
                step_chunk_buffers[step_type] = []
            step_chunk_buffers[step_type].append(chunk)

            event = {
                "type": "chunk",
                "step": step_type.value,
                "content": chunk,
            }
            yield f"data: {json.dumps(event, ensure_ascii=False)}\n\n"

        async def on_step_complete(step_type: StepType, step_result):
            event = {
                "type": "step_complete",
                "step": step_type.value,
                "tokens_used": step_result.tokens_used,
                "status": step_result.status.value,
            }
            yield f"data: {json.dumps(event, ensure_ascii=False)}\n\n"

        result = await execute_pipeline(
            task_id=request.task_id,
            subtitle_text=request.subtitle_text,
            on_step_start=on_step_start,
            on_chunk=on_chunk,
            on_step_complete=on_step_complete,
        )

        # Pipeline complete event
        total_tokens = sum(s.tokens_used for s in result.steps.values())
        event = {
            "type": "pipeline_complete",
            "total_tokens": total_tokens,
            "duration": result.completed_at - result.started_at if result.completed_at else 0,
        }
        yield f"data: {json.dumps(event, ensure_ascii=False)}\n\n"

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )


@app.get("/pipeline/{task_id}/status")
async def pipeline_status(task_id: str, x_internal_secret: Optional[str] = Header(None)):
    verify_secret(x_internal_secret)
    # Phase 2: In-memory only. Phase 3 will add Redis-backed status.
    return {"task_id": task_id, "status": "unknown"}


@app.post("/pipeline/{task_id}/cancel")
async def pipeline_cancel(task_id: str, x_internal_secret: Optional[str] = Header(None)):
    verify_secret(x_internal_secret)
    # Phase 2: Not implemented yet. Need task tracking with cancellation support.
    return {"task_id": task_id, "cancelled": False}
