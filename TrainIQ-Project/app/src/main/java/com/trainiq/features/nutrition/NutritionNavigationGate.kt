package com.trainiq.features.nutrition

import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.runtime.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.BackHandler

data class AiNavigationState(val busy: Boolean = false, val hasReview: Boolean = false)

/** A composition lease prevents an old owner disposal from clearing a newer entry's protection. */
class NutritionNavigationGate {
    private data class Registration(val lease: String, val owner: String, val route: String,
        val state: AiNavigationState = AiNavigationState(), val discard: () -> Unit = {}, val notify: (String) -> Unit = {},
        val busyCheck: () -> Boolean = { false })
    private var registration: Registration? = null
    private var pendingAction: (() -> Unit)? = null
    private var pendingLease: String? = null
    private val _confirmLeave = MutableStateFlow(false)
    val confirmLeave = _confirmLeave.asStateFlow()

    fun register(owner: String, route: String, busyCheck: () -> Boolean = { false }): String {
        val lease = UUID.randomUUID().toString()
        registration = Registration(lease, owner, route, busyCheck = busyCheck)
        stay()
        return lease
    }

    fun update(lease: String, state: AiNavigationState, discard: () -> Unit, notify: (String) -> Unit) {
        if (registration?.lease == lease) registration = registration!!.copy(state = state, discard = discard, notify = notify)
    }

    fun unregister(lease: String) {
        if (registration?.lease != lease) return
        registration = null
        stay()
    }

    fun request(destination: String, action: () -> Unit): Boolean {
        val current = registration
        if (current == null || current.route == destination) { action(); return true }
        if (current.state.busy || current.busyCheck()) {
            current.notify("AI-items worden nog verwerkt. Wacht tot het opslaan en toevoegen klaar is.")
            return false
        }
        if (current.state.hasReview) {
            pendingAction = action; pendingLease = current.lease; _confirmLeave.value = true
            return false
        }
        action()
        return true
    }

    fun stay() { pendingAction = null; pendingLease = null; _confirmLeave.value = false }

    fun leave(): Boolean {
        val current = registration ?: return false
        if (current.lease != pendingLease || current.state.busy || current.busyCheck()) return false
        val action = pendingAction ?: return false
        stay()
        current.discard()
        action()
        return true
    }
}

@Composable
internal fun rememberNutritionNavigationLease(ownerId: String, route: String, gate: NutritionNavigationGate,
    busyCheck: () -> Boolean = { false }): String {
    val lease = remember(ownerId, gate) { gate.register(ownerId, route, busyCheck) }
    DisposableEffect(lease) { onDispose { gate.unregister(lease) } }
    return lease
}

@Composable
internal fun NutritionLeaveReviewDialog(gate: NutritionNavigationGate) {
    val confirm by gate.confirmLeave.collectAsState()
    if (confirm) AlertDialog(onDismissRequest = gate::stay,
        title = { Text("AI-controle verlaten?") },
        text = { Text("Nog niet toegevoegde AI-items vervallen. Opgeslagen producten blijven bewaard.") },
        confirmButton = { TextButton(onClick = { gate.leave() }) { Text("AI-controle verlaten") } },
        dismissButton = { TextButton(onClick = gate::stay) { Text("Blijven controleren") } })
}

@Composable
internal fun NutritionOwnerBackHandler(state: AiNavigationState, onBack: () -> Unit) {
    BackHandler(enabled = state.busy || state.hasReview, onBack = onBack)
}
