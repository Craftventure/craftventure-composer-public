package net.craftventure.composer.view

import javafx.animation.AnimationTimer
import javafx.event.EventHandler
import javafx.scene.*
import javafx.scene.input.KeyCode
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.Box
import javafx.scene.shape.Sphere
import net.craftventure.composer.App
import net.craftventure.composer.extension.orElse
import net.craftventure.composer.fixture.Fixture
import net.craftventure.composer.fixture.Location
import net.craftventure.composer.fixture.fountain.*
import net.craftventure.composer.threedee.Vector
import net.craftventure.composer.threedee.Xform
import tornadofx.addChildIfPossible

class PreviewView private constructor(root: Parent) : Scene(
        root,
        1024.0,
        768.0,
        true,
        SceneAntialiasing.BALANCED
) {
    private val CAMERA_INITIAL_DISTANCE = -450.0
    private val CAMERA_INITIAL_X_ANGLE = 70.0
    private val CAMERA_INITIAL_Y_ANGLE = 320.0
    private val CAMERA_NEAR_CLIP = 0.1
    private val CAMERA_FAR_CLIP = 10000.0
    private val CONTROL_MULTIPLIER = 0.1
    private val SHIFT_MULTIPLIER = 10.0
    private val MOUSE_SPEED = 0.1
    private val ROTATION_SPEED = 2.0
    private val TRACK_SPEED = 0.3

    private val root = Group()
    private val world = Xform()
    private val camera = PerspectiveCamera(true)
    private val cameraXform = Xform()
    private val cameraXform2 = Xform()
    private val cameraXform3 = Xform()

    private var mousePosX: Double = 0.0
    private var mousePosY: Double = 0.0
    private var mouseOldX: Double = 0.0
    private var mouseOldY: Double = 0.0
    private var mouseDeltaX: Double = 0.0
    private var mouseDeltaY: Double = 0.0

    private val fixtureGroup = Xform()
    private val waterparticlesGroup = Xform()
    private var animationTimer: AnimationTimer
    private var lastUpdate = System.currentTimeMillis()

    private var waterParticles = mutableListOf<WaterParticle>()

    private fun buildCamera() {
        root.children.add(cameraXform)
        cameraXform.children.add(cameraXform2)
        cameraXform2.children.add(cameraXform3)
        cameraXform3.children.add(camera)
        cameraXform3.setRotateZ(180.0)

        camera.nearClip = CAMERA_NEAR_CLIP
        camera.farClip = CAMERA_FAR_CLIP
        camera.translateZ = CAMERA_INITIAL_DISTANCE
        cameraXform.ry.angle = CAMERA_INITIAL_Y_ANGLE
        cameraXform.rx.angle = CAMERA_INITIAL_X_ANGLE
    }

    private fun handleMouse(scene: Scene, root: Node) {
        scene.onMousePressed = EventHandler { me ->
            mousePosX = me.sceneX
            mousePosY = me.sceneY
            mouseOldX = me.sceneX
            mouseOldY = me.sceneY
        }
        scene.onMouseDragged = EventHandler { me ->
            mouseOldX = mousePosX
            mouseOldY = mousePosY
            mousePosX = me.sceneX
            mousePosY = me.sceneY
            mouseDeltaX = mousePosX - mouseOldX
            mouseDeltaY = mousePosY - mouseOldY

            var modifier = 1.0

            if (me.isControlDown) {
                modifier = CONTROL_MULTIPLIER
            }
            if (me.isShiftDown) {
                modifier = SHIFT_MULTIPLIER
            }
            if (me.isPrimaryButtonDown) {
                cameraXform.ry.angle = cameraXform.ry.angle - mouseDeltaX * MOUSE_SPEED * modifier * ROTATION_SPEED
                cameraXform.rx.angle = cameraXform.rx.angle + mouseDeltaY * MOUSE_SPEED * modifier * ROTATION_SPEED
            } else if (me.isSecondaryButtonDown) {
                val z = camera.translateZ
                val newZ = z + mouseDeltaX * MOUSE_SPEED * modifier
                camera.translateZ = newZ
            } else if (me.isMiddleButtonDown) {
                cameraXform2.t.x = cameraXform2.t.x + mouseDeltaX * MOUSE_SPEED * modifier * TRACK_SPEED
                cameraXform2.t.y = cameraXform2.t.y + mouseDeltaY * MOUSE_SPEED * modifier * TRACK_SPEED
            }
        }
    }

    private fun handleKeyboard(scene: Scene, root: Node) {
        scene.onKeyPressed = EventHandler { event ->
            when (event.code) {
                KeyCode.Z -> {
                    cameraXform2.t.x = 0.0
                    cameraXform2.t.y = 0.0
                    camera.translateZ = CAMERA_INITIAL_DISTANCE
                    cameraXform.ry.angle = CAMERA_INITIAL_Y_ANGLE
                    cameraXform.rx.angle = CAMERA_INITIAL_X_ANGLE
                }
                KeyCode.V -> fixtureGroup.isVisible = !fixtureGroup.isVisible
            }
        }
    }

    private fun buildFixtures(fixtures: List<Fixture>) {
        fixtureGroup.children.clear()
        for (fixture in fixtures) {
            val fixtureXform = Xform()

            val fixtureSphere = Sphere(0.5)
            fixtureSphere.translateX = fixture.location.x
            fixtureSphere.translateY = fixture.location.y
            fixtureSphere.translateZ = fixture.location.z
            fixtureSphere.material = fixtureMaterial

            fixtureXform.children.add(fixtureSphere)

            fixtureGroup.children.add(fixtureXform)
        }

        val fixtureXform = Xform()
        val water = Box(1000.0, 0.01, 1000.0)
        water.translateY = App.mainScene.value!!.settings.value.orElse()?.waterLevel ?: 0.0
        water.material = waterOverlay
        fixtureXform.children.add(water)
        fixtureGroup.children.add(fixtureXform)

        world.children.remove(fixtureGroup)
        world.children.addAll(fixtureGroup)
    }

    private fun update() {
        val time = App.playbackController.time()
        while (System.currentTimeMillis() > lastUpdate + 50) {
            App.mainScene.value!!.fixtures.forEach { fixture ->
                val playingTimeline = fixture.getTimeline("play")
                if (playingTimeline != null) {
                    if (playingTimeline.valueAt(time) < 1.0 && fixture !is SuperShooter) {
                        return@forEach
                    }
                }
                when (fixture) {
                    is Shooter -> {
                        val pressureTimeline = fixture.getTimeline("pressure")

                        val pressure = pressureTimeline?.valueAt(time) ?: 0.0
                        if (pressure > 0) {
                            val particle = WaterParticle(fixture.location.y, Location(fixture.location.x, fixture.location.y, fixture.location.z), Vector(0.0, pressure, 0.0))
                            waterParticles.add(particle)
                            waterparticlesGroup.children.add(particle.model)
                        }
                    }
                    is SuperShooter -> {
                        val shootTime = fixture.getTimeline("shots")?.getFrameTimeBetween(time - 0.05, time)

                        if (shootTime != null) {
                            val pressureTimeline = fixture.getTimeline("pressure")
                            val pressure = pressureTimeline?.valueAt(time) ?: 0.0
                            if (pressure > 0) {
                                val height = fixture.getTimeline("height")?.valueAt(shootTime / 1000.0)
                                if (height != null) {
                                    for (i in 0 until height.toInt()) {
                                        val particle = WaterParticle(
                                                fixture.location.y, Location(fixture.location.x, fixture.location.y + i, fixture.location.z),
                                                Vector(0.0, pressure, 0.0)
                                        )
                                        waterParticles.add(particle)
                                        waterparticlesGroup.children.add(particle.model)
                                    }
                                }
                            }
                        }
                    }
                    is OarsmanJet -> {
                        val pressureTimeline = fixture.getTimeline("pressure")

                        val pressure = pressureTimeline?.valueAt(time) ?: 0.0
                        if (pressure > 0) {
                            val yaw = fixture.getTimeline("heading")?.valueAt(time) ?: 0.0
                            val pitch = fixture.getTimeline("pitch")?.valueAt(time) ?: 0.0

                            val motion = Vector().setYawPitchDegrees(yaw, pitch).multiply(pressure)

                            val particle = WaterParticle(fixture.location.y, Location(fixture.location.x, fixture.location.y, fixture.location.z), motion)
                            waterParticles.add(particle)
                            waterparticlesGroup.children.add(particle.model)
                        }
                    }
                    is LillyJet -> {
                        val pressureTimeline = fixture.getTimeline("pressure")

                        val pressure = pressureTimeline?.valueAt(time) ?: 0.0
                        if (pressure > 0) {
                            val rays = fixture.getTimeline("rays")?.valueAt(time) ?: 0.0
                            val pitch = fixture.getTimeline("pitch")?.valueAt(time) ?: 0.0

                            val angleOffset = (360.0 / rays) * 10
                            var currentYaw = angleOffset * time
                            val yawIncreasePerFountain = 360 / rays

                            for (ray in 0 until Math.ceil(rays).toInt()) {
                                val motion = Vector().setYawPitchDegrees(currentYaw, pitch).multiply(pressure)
                                val particle = WaterParticle(fixture.location.y, Location(fixture.location.x, fixture.location.y, fixture.location.z), motion)
                                waterParticles.add(particle)
                                waterparticlesGroup.children.add(particle.model)

                                currentYaw += yawIncreasePerFountain
                            }
                        }
                    }
                    is Bloom -> {
                        val pressureTimeline = fixture.getTimeline("pressure")

                        val pressure = pressureTimeline?.valueAt(time) ?: 0.0
                        if (pressure > 0) {
                            val rays = fixture.getTimeline("rays")?.valueAt(time) ?: 0.0
                            val pitch = fixture.getTimeline("pitch")?.valueAt(time) ?: 0.0

                            var yaw = 0.0
                            val yawIncreasePerFountain = 360 / rays

                            for (ray in 0 until Math.ceil(rays).toInt()) {
                                val motion = Vector().setYawPitchDegrees(yaw, pitch).multiply(pressure)
                                val particle = WaterParticle(fixture.location.y, Location(fixture.location.x, fixture.location.y, fixture.location.z), motion)
                                waterParticles.add(particle)
                                waterparticlesGroup.children.add(particle.model)

                                yaw += yawIncreasePerFountain
                            }
                        }
                    }
                }
            }
            lastUpdate += 50
        }

        waterParticles.forEach {
            it.update()
            if (it.shouldRemove()) {
                waterparticlesGroup.children.remove(it.model)
            }
        }
        waterParticles.removeAll { it.shouldRemove() }
    }

    init {
        animationTimer = object : AnimationTimer() {
            override fun handle(now: Long) {
                try {
                    update()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        animationTimer.start()
        root.addChildIfPossible(world)
        root.depthTest = DepthTest.ENABLE

        buildCamera()
//        buildMolecule()

        fill = Color.BLACK
        handleKeyboard(this, world)
        handleMouse(this, world)

        setCamera(camera)

        world.children.add(waterparticlesGroup)
        App.mainScene.map { it.fixtures }.subscribe({
            buildFixtures(it)
        })
    }

    companion object {
        fun newInstance() = PreviewView(Group())

        protected val waterMaterial = PhongMaterial().apply {
            diffuseColor = Color.BLUE
            specularColor = Color.LIGHTBLUE
        }

        protected val fixtureMaterial = PhongMaterial().apply {
            diffuseColor = Color.DARKGREY
            specularColor = Color.GREY
        }

        protected val waterOverlay = PhongMaterial().apply {
            diffuseColor = Color(0.0, 0.0, 0.0, 0.95)
            specularColor = Color.BLACK
        }
    }

    private class WaterParticle(val startY: Double, val location: Location, val motion: Vector) {
        private var lastUpdate = System.currentTimeMillis()
        var model: Box
            private set

        init {
            model = Box(1.0, 1.0, 1.0)
            model.translateX = location.x
            model.translateY = location.y
            model.translateZ = location.z
            model.material = waterMaterial
        }

        fun shouldRemove() = location.y < startY

        fun update() {
//            val percentage = ((System.currentTimeMillis() - lastUpdate) / 50.0).clamp(0.0, 1.0)
            while (System.currentTimeMillis() > lastUpdate + 50) {
                location.x += motion.x
                location.y += motion.y
                location.z += motion.z

//                motion.x *= 0.699999988079071
//                motion.y *= 0.699999988079071
//                motion.z *= -0.5

                motion.x *= 0.9800000190734863
                motion.y *= 0.9800000190734863
                motion.z *= 0.9800000190734863

                motion.y -= 0.04

                lastUpdate += 50
            }

            model.translateX = location.x
            model.translateY = location.y
            model.translateZ = location.z
        }
    }
}