package com.trainiq.ai.prompts

import com.google.gson.GsonBuilder
import com.trainiq.domain.model.ExerciseEffortSummary

/** Explicit JSON nulls keep unknown effort distinct from omitted or empty fields. */
internal fun exerciseEffortJson(exercises: List<ExerciseEffortSummary>): String =
    GsonBuilder().serializeNulls().create().toJson(exercises)
