package net.craftventure.composer.fixture.fountain

import net.craftventure.composer.fixture.Location
import net.craftventure.composer.fixture.property.DoubleProperty

class Shooter(
        name: String,
        location: Location
) : Fountain(
        name,
        location,
        "fountain:shooter"
) {
    init {
        properties += DoubleProperty("pressure", 0.0, min = 0.0, max = 2.0)
    }

    override fun destroy() {}
}
