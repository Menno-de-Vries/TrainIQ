package com.trainiq.core.diagnostics

data class Breadcrumb(
    val message: String,
    val category: String = "app",
    val timestampMillis: Long,
)

class BreadcrumbRingBuffer(
    private val maxSize: Int = DefaultMaxSize,
) {
    private val breadcrumbs = ArrayDeque<Breadcrumb>(maxSize)

    init {
        require(maxSize > 0) { "maxSize must be positive" }
    }

    @Synchronized
    fun add(message: String, category: String = "app", timestampMillis: Long) {
        if (breadcrumbs.size == maxSize) {
            breadcrumbs.removeFirst()
        }
        breadcrumbs.addLast(
            Breadcrumb(
                message = message.trim().take(MaxMessageLength),
                category = category.trim().ifBlank { "app" }.take(MaxCategoryLength),
                timestampMillis = timestampMillis,
            ),
        )
    }

    @Synchronized
    fun snapshot(): List<Breadcrumb> = breadcrumbs.toList()

    @Synchronized
    fun clear() {
        breadcrumbs.clear()
    }

    companion object {
        const val DefaultMaxSize = 20
        private const val MaxMessageLength = 80
        private const val MaxCategoryLength = 24
    }
}
