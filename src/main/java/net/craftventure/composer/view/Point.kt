package net.craftventure.composer.view

class Point(
        var x: Double,
        var y: Double
) {
    fun distanceTo(other: Point): Double {
        val dx = x - other.x
        val dy = y - other.y
        return Math.sqrt(dx * dx + dy * dy)
    }
}