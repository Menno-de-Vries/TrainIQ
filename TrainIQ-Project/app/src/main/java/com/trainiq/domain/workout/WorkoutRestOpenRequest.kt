package com.trainiq.domain.workout

/** A notification can resume this exact session/generation; it never authorizes starting a draft. */
data class WorkoutRestOpenRequest(val dayId: Long, val sessionId: Long, val generation: String)
