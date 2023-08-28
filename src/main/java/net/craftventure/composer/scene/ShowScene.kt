package net.craftventure.composer.scene

import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import io.reactivex.subjects.BehaviorSubject
import javafx.collections.FXCollections
import net.craftventure.composer.App
import net.craftventure.composer.extension.orElse
import net.craftventure.composer.extension.toOptional
import net.craftventure.composer.fixture.Fixture
import net.craftventure.composer.fixture.Location
import net.craftventure.composer.fixture.fountain.*
import net.craftventure.composer.timeline.KeyFrame
import net.craftventure.composer.timeline.KeyFrameEasing
import java.io.File
import java.io.FileReader
import java.io.PrintWriter
import java.util.*

class ShowScene {
    val fixtures = FXCollections.observableArrayList<Fixture>()
    val selectedFixture = BehaviorSubject.createDefault(Optional.empty<Fixture>())
    val settings = BehaviorSubject.createDefault(Optional.empty<Settings>())

    fun addFixture(item: Fixture) {
        if (fixtures.firstOrNull { it.name == item.name } != null)
            throw IllegalStateException("An item with name '${item.name}' is already in included within the current scene")
        if (selectedFixture.value.orElse() == null)
            selectedFixture.onNext(Optional.of(item))
        fixtures.add(item)
    }

    fun save(file: File): Boolean {
        val json = SceneJson()
        json.settings = settings.value?.get()
//        println("Saving with settings ${settings.value}")

        for (fixture in fixtures) {
            val jsonFixture = JsonFixture().apply {
                name = fixture.name
                kind = fixture.kind
                location = JsonLocation().apply {
                    x = fixture.location.x
                    y = fixture.location.y
                    z = fixture.location.z
                }

                for (property in fixture.properties) {
                    val jsonProperty = JsonProperty()
                    jsonProperty.name = property.name

                    val timeline = fixture.getTimeline(property.name)!!
                    for (frame in timeline.keyframes) {
                        val jsonKeyFrame = JsonKeyFrame().apply {
                            at = frame.at.toDouble() / 1000.0
                            value = frame.keyValue.toDouble()
                            inEasing = frame.inEasing
                            outEasing = frame.outEasing
                        }
                        jsonProperty.keyframes.add(jsonKeyFrame)
                    }

                    properties.add(jsonProperty)
                }
            }
            json.fixtures.add(jsonFixture)
        }

        val writer = PrintWriter(file)
        writer.print(App.gson.toJson(json))
        writer.close()

        return false
    }

    fun load(file: File): ShowScene {
        val reader = JsonReader(FileReader(file))
        val data = App.gson.fromJson<SceneJson>(reader, object : TypeToken<SceneJson>() {}.type)
        settings.onNext(data.settings.toOptional())
//        println(App.gson.toJson(data))

        val newFixtures = mutableListOf<Fixture>()
        for (jsonFixture in data.fixtures) {
//            println(jsonFixture.kind)
            when (jsonFixture.kind) {
                "fountain:shooter" -> {
                    val shooter = Shooter(jsonFixture.name, jsonFixture.location.toLocation())
                    newFixtures.add(shooter)

                    for (jsonProperty in jsonFixture.properties) {
                        val timeline = shooter.getTimeline(jsonProperty.name)!!
                        for (jsonKeyframe in jsonProperty.keyframes) {
                            timeline.addKeyframe(KeyFrame(
                                    (jsonKeyframe.at * 1000).toLong(),
                                    jsonKeyframe.value,
                                    inEasing = jsonKeyframe.inEasing ?: KeyFrameEasing.NONE,
                                    outEasing = jsonKeyframe.outEasing ?: KeyFrameEasing.NONE
                            ))
                        }
                    }
                }
                "fountain:supershooter" -> {
                    val shooter = SuperShooter(jsonFixture.name, jsonFixture.location.toLocation())
                    newFixtures.add(shooter)

                    for (jsonProperty in jsonFixture.properties) {
                        val timeline = shooter.getTimeline(jsonProperty.name)!!
                        for (jsonKeyframe in jsonProperty.keyframes) {
                            timeline.addKeyframe(KeyFrame(
                                    (jsonKeyframe.at * 1000).toLong(),
                                    jsonKeyframe.value,
                                    inEasing = jsonKeyframe.inEasing ?: KeyFrameEasing.NONE,
                                    outEasing = jsonKeyframe.outEasing ?: KeyFrameEasing.NONE
                            ))
                        }
                    }
                }
                "fountain:oarsmanjet" -> {
                    val shooter = OarsmanJet(jsonFixture.name, jsonFixture.location.toLocation())
                    newFixtures.add(shooter)

                    for (jsonProperty in jsonFixture.properties) {
                        val timeline = shooter.getTimeline(jsonProperty.name)!!
                        for (jsonKeyframe in jsonProperty.keyframes) {
                            timeline.addKeyframe(KeyFrame(
                                    (jsonKeyframe.at * 1000).toLong(),
                                    jsonKeyframe.value,
                                    inEasing = jsonKeyframe.inEasing ?: KeyFrameEasing.NONE,
                                    outEasing = jsonKeyframe.outEasing ?: KeyFrameEasing.NONE
                            ))
                        }
                    }
                }
                "fountain:lillyjet" -> {
                    val shooter = LillyJet(jsonFixture.name, jsonFixture.location.toLocation())
                    newFixtures.add(shooter)

                    for (jsonProperty in jsonFixture.properties) {
                        val timeline = shooter.getTimeline(jsonProperty.name)!!
                        for (jsonKeyframe in jsonProperty.keyframes) {
                            timeline.addKeyframe(KeyFrame(
                                    (jsonKeyframe.at * 1000).toLong(),
                                    jsonKeyframe.value,
                                    inEasing = jsonKeyframe.inEasing ?: KeyFrameEasing.NONE,
                                    outEasing = jsonKeyframe.outEasing ?: KeyFrameEasing.NONE
                            ))
                        }
                    }
                }
                "fountain:bloom" -> {
                    val shooter = Bloom(jsonFixture.name, jsonFixture.location.toLocation())
                    newFixtures.add(shooter)

                    for (jsonProperty in jsonFixture.properties) {
                        val timeline = shooter.getTimeline(jsonProperty.name)!!
                        for (jsonKeyframe in jsonProperty.keyframes) {
                            timeline.addKeyframe(KeyFrame(
                                    (jsonKeyframe.at * 1000).toLong(),
                                    jsonKeyframe.value,
                                    inEasing = jsonKeyframe.inEasing ?: KeyFrameEasing.NONE,
                                    outEasing = jsonKeyframe.outEasing ?: KeyFrameEasing.NONE
                            ))
                        }
                    }
                }
                else -> throw IllegalStateException("Unknown kind ${jsonFixture.kind}")
            }
        }

        fixtures.clear()
        fixtures.addAll(newFixtures)

        return this
    }

    private class SceneJson {
        var settings: Settings? = null
        val fixtures = mutableListOf<JsonFixture>()
    }

//    private class JsonSettings {
//        var x: Double? = 0.0
//        var y: Double? = 0.0
//        var z: Double? = 0.0
//        var waterLevel: Double? = 0.0
//    }

    private class JsonFixture {
        lateinit var name: String
        lateinit var kind: String
        lateinit var location: JsonLocation
        var properties = mutableListOf<JsonProperty>()
    }

    private class JsonLocation {
        var x: Double? = 0.0
        var y: Double? = 0.0
        var z: Double? = 0.0

        fun toLocation() = Location(x!!, y!!, z!!)
    }

    private class JsonProperty {
        lateinit var name: String
        var keyframes = mutableListOf<JsonKeyFrame>()
    }

    private class JsonKeyFrame {
        var at: Double = 0.0
        var value: Number = 0.0
        var inEasing: KeyFrameEasing? = null
        var outEasing: KeyFrameEasing? = null
    }
}