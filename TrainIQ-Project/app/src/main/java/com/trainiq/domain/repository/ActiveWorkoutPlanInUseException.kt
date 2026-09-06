package com.trainiq.domain.repository

/** Deleting the plan must never implicitly discard a workout that the user is logging. */
class ActiveWorkoutPlanInUseException : IllegalStateException(
    "Rond de actieve training eerst af of verwijder die via de actieve training. Daarna kun je de routine of sessie verwijderen.",
)
