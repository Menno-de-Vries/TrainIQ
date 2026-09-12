package com.trainiq.features.nutrition

/** Product rule for newly saved items; never a density conversion or historical backfill. */
internal fun mealHydrationMl(grams: Double, countsAsFluid: Boolean): Double {
    require(grams.isFinite() && grams > 0.0 && grams <= 100_000.0)
    return if (countsAsFluid) grams else 0.0
}
