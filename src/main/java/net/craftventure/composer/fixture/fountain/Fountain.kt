package net.craftventure.composer.fixture.fountain

import net.craftventure.composer.extension.toOptional
import net.craftventure.composer.fixture.Fixture
import net.craftventure.composer.fixture.Location
import net.craftventure.composer.fixture.property.BooleanProperty
import net.craftventure.composer.timeline.KeyFrameEasing

abstract class Fountain(
        name: String,
        location: Location,
        kind: String
) : Fixture(
        name,
        location,
        kind
) {
    init {
        properties.add(BooleanProperty(
                "play",
                false,
                inEasingOverride = KeyFrameEasing.PREVIOUS.toOptional(),
                outEasingOverride = KeyFrameEasing.PREVIOUS.toOptional()
        ))
    }
}