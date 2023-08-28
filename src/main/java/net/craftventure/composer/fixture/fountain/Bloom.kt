package net.craftventure.composer.fixture.fountain

import net.craftventure.composer.fixture.Location
import net.craftventure.composer.fixture.property.DoubleProperty
import net.craftventure.composer.fixture.property.IntProperty

class Bloom(
        name: String,
        location: Location
) : Fountain(
        name,
        location,
        "fountain:bloom"
) {

    init {
        properties.add(DoubleProperty("pressure", 0.0, min = 0.0, max = 2.0))
        properties.add(DoubleProperty("pitch", -50.0))
        properties.add(IntProperty("rays", 0, 1, 10))
    }

    override fun destroy() {}
}
