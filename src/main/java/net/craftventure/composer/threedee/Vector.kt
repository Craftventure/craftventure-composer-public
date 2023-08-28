package net.craftventure.composer.threedee

import net.craftventure.composer.extension.toRadians

class Vector(var x: Double = 0.0, var y: Double = 0.0, var z: Double = 0.0) {
    fun setYawPitchDegrees(yaw: Double, pitch: Double): Vector {
        return this.setYawPitchRadians(yaw.toRadians(), pitch.toRadians())
    }

    fun setYawPitchRadians(yaw: Double, pitch: Double): Vector {
        val xz = Math.cos(pitch)
        x = (-xz * Math.sin(yaw))
        y = (-Math.sin(pitch))
        z = (xz * Math.cos(yaw))
        return this
    }

    fun multiply(multiplier: Double): Vector {
        x *= multiplier
        y *= multiplier
        z *= multiplier
        return this
    }

    operator fun timesAssign(multiplier: Double) {
        multiply(multiplier)
    }
}