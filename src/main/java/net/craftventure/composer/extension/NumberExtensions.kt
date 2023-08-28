package net.craftventure.composer.extension

fun Double.format(digits: Int) = java.lang.String.format("%.${digits}f", this)
fun Double.orIfNan(other: Double): Double {
    if (isNaN()) {
        return other
    }
    return this
}