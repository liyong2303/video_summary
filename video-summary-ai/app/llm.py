from openai import AsyncOpenAI
from app.config import settings

client = AsyncOpenAI(
    api_key=settings.deepseek_api_key,
    base_url=settings.deepseek_base_url,
)


async def stream_chat(prompt: str, system: str = "你是一个专业的内容编辑助手。"):
    """Stream chat completion, yielding content chunks."""
    stream = await client.chat.completions.create(
        model=settings.deepseek_model,
        messages=[
            {"role": "system", "content": system},
            {"role": "user", "content": prompt},
        ],
        stream=True,
        temperature=0.7,
        max_tokens=2000,
    )
    async for chunk in stream:
        delta = chunk.choices[0].delta
        if delta.content:
            yield delta.content


async def chat(prompt: str, system: str = "你是一个专业的内容编辑助手。") -> str:
    """Non-streaming chat completion, returns full content."""
    response = await client.chat.completions.create(
        model=settings.deepseek_model,
        messages=[
            {"role": "system", "content": system},
            {"role": "user", "content": prompt},
        ],
        temperature=0.7,
        max_tokens=2000,
    )
    return response.choices[0].message.content or ""
