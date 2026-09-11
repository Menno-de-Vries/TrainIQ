package com.trainiq.domain.model

enum class FoodProviderMode(val label: String) {
    AUTOMATIC("Automatisch"), OPEN_FOOD_FACTS("Open Food Facts"), FATSECRET("FatSecret")
}
enum class FoodLookupFailure { INVALID_BARCODE, NETWORK, AUTH, NOT_CONFIGURED, INVALID_RESPONSE }
class FoodLookupException(val failure: FoodLookupFailure) : Exception(failure.name)

fun normalizedBarcode(value: String): String? {
    val clean = value.trim().replace("-", "").replace(" ", "")
    if (clean.length !in listOf(8, 12, 13, 14) || clean.any { it !in '0'..'9' }) return null
    val checksum = clean.dropLast(1).reversed().mapIndexed { index, c -> c.digitToInt() * if (index % 2 == 0) 3 else 1 }.sum()
    return clean.takeIf { (10 - checksum % 10) % 10 == clean.last().digitToInt() }
}

fun fatSecretGtin13(value: String): String? = normalizedBarcode(value)?.let {
    if (it.length == 14 && !it.startsWith('0')) null else it.takeLast(13).padStart(13, '0')
}
