package com.trainiq.core.health

import com.trainiq.domain.model.HealthConnectStatus

internal fun healthConnectStepSourceLabel(status: HealthConnectStatus): String {
    val diagnostic = status.stepDiagnostic ?: return "Health Connect"
    val displaysDirectSamsung = diagnostic.samsungHealthDirectStepsToday != null &&
        diagnostic.displaySteps == diagnostic.samsungHealthDirectStepsToday
    val displaysSamsungVisible = diagnostic.samsungHealthStepsToday != null &&
        diagnostic.displaySteps == diagnostic.samsungHealthStepsToday
    return if (displaysDirectSamsung || displaysSamsungVisible) {
        "Samsung Health"
    } else {
        "Health Connect"
    }
}
