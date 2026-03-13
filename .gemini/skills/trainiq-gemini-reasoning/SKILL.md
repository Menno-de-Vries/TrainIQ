---
name: trainiq-gemini-reasoning
description: Expert implementation of Gemini 2.5 Flash in TrainIQ. Use when designing prompts, implementing hybrid reasoning (Thinking Budget), or building structured AI response systems.
---

# TrainIQ AI Reasoning Expert

This skill guides the implementation of advanced AI coaching logic using Gemini 2.5 Flash.

## Core Mandates

### 1. Hybrid Reasoning (Gemini 2.5 Flash)
- **Fast Mode (Thinking Disabled):** Use for tasks requiring instant response where the reasoning path is clear:
    - Meal Image Scanning (JSON extraction).
    - Workout Volume Summary.
- **Deep Mode (Thinking Enabled):** Use for complex coaching tasks. Set a `thinking_budget` of 500-1000 tokens:
    - Weekly Training Plan adjustments.
    - Personal Goal Analysis & Nutrition Advice.
    - Sleep vs. Training Performance correlation.

### 2. Structured Output & Safety
- **JSON Only:** Always request `response_mime_type: "application/json"`.
- **Schema Enforcement:** Provide a clear JSON schema in the prompt.
- **Fail-Safe Parsing:** Always use `runCatching` for parsing. Provide a deterministic fallback (e.g., hardcoded logic) if the AI fails or is offline.

### 3. Prompt Engineering (Persona)
- **Persona:** *"You are a world-class strength and nutrition coach. Be scientific, concise, and highly encouraging."*
- **Context Management:** Send only relevant deltas (e.g., "Weight changed -2kg in 7 days") instead of full history to minimize token costs.

## Prohibited Patterns
- ❌ Do NOT hardcode API keys. Use `AiUsageGate`.
- ❌ Do NOT use regex to extract JSON from AI text; use the structured output feature.
- ❌ Do NOT allow the AI to hallucinate units; explicitly specify metric system.

## Workflow
1. **Define Schema:** Check `DomainModels.kt` or `GeminiDtos.kt`.
2. **Select Mode:** Determine if `thinking_budget` is required.
3. **Draft Prompt:** Include System Instructions (Persona).
4. **Implement Service:** Call `GeminiApi` with proper parameters.
5. **Handle Failure:** Ensure fallback logic is robust.
