package com.trainiq.core.theme

enum class ThemeMode(val storageValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromStorageValue(value: String): ThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: SYSTEM
    }
}
