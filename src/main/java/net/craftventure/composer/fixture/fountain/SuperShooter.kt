package net.craftventure.composer.fixture.fountain

import net.craftventure.composer.extension.toOptional
import net.craftventure.composer.fixture.Location
import net.craftventure.composer.fixture.property.DoubleProperty
import net.craftventure.composer.timeline.KeyFrameEasing

class SuperShooter(
        name: String,
        location: Location
) : Fountain(
        name,
        location,
        "fountain:supershooter"
) {
    init {
        properties += DoubleProperty("shots", 0.0, min = 0.0, max = 0.0,
                inEasingOverride = KeyFrameEasing.PREVIOUS.toOptional(),
                outEasingOverride = KeyFrameEasing.PREVIOUS.toOptional())
        properties += DoubleProperty("pressure", 0.0, min = 0.0, max = 2.0)
        properties += DoubleProperty("height", 0.0, min = 0.0, max = 40.0)
    }

    override fun destroy() {}
}
