package com.trainiq.core.security

/** SharedPreferences removal is successful only after its synchronous durable commit. */
internal fun commitEncryptedKeyRemoval(commit: () -> Boolean) {
    check(commit()) { "AI-sleutel kon niet worden verwijderd. Probeer opnieuw." }
}
