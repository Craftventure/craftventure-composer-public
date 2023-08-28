package net.craftventure.composer.view

import javafx.animation.AnimationTimer
import javafx.event.EventHandler
import javafx.scene.canvas.Canvas
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import javafx.scene.transform.Affine
import net.craftventure.composer.App
import net.craftventure.composer.extension.format
import net.craftventure.composer.extension.orElse
import net.craftventure.composer.extension.toOptional
import net.craftventure.composer.fixture.Fixture
import net.craftventure.composer.scene.ShowScene


class SceneView : Canvas() {
    private val BASE_BACKGROUND = Color.web("#232323")

    private var mouse: Point? = null
    private var drag: Point? = null
    private var delta: Point = Point(0.0, 0.0)//80.0, -600.0)

    private var zoom = 1.0

    private var scene: ShowScene? = null
    private val transform = Affine()
    private var pressAt: Point? = null

    init {
        onMousePressed = EventHandler {
            //            println("Mouse pressed")
            updatePosition(it)

            drag = Point(it.x, it.y)

            pressAt = Point(it.x, it.y)

            invalidate()
            it.consume()
        }
        onMouseDragged = EventHandler {
            //            println("Mouse dragged ${it.isControlDown}")
            updatePosition(it)

            if (drag != null) {
                delta.x += ((it.x - drag!!.x) / zoom)
                delta.y += ((it.y - drag!!.y) / zoom)
            }

            drag = Point(it.x, it.y)

            invalidate()
            it.consume()
        }
        onMouseDragReleased = EventHandler {
            //            println("Mouse drag released ${it.isControlDown}")

            drag = null

            updatePosition(it)
            invalidate()
            it.consume()
        }
        onMouseReleased = EventHandler {
            //            println("Mouse released")
            updatePosition(it)

            drag = null

            if (pressAt?.distanceTo(Point(it.x, it.y)) ?: 0.0 <= 10) {
                scene?.let {
                    for (fixture in it.fixtures) {
                        val mouseOver = isMouseOverFixture(fixture)
                        if (mouseOver) {
                            it.selectedFixture.onNext(fixture.toOptional())
                            return@let
                        }
                    }
                }
            }

            pressAt = null

            invalidate()
            it.consume()
//            println("released ${mouse?.x} ${mouse?.y} ${delta.x} ${delta.y}")
        }
        onMouseMoved = EventHandler {
            updatePosition(it)

            invalidate()
            it.consume()
        }
        onMouseExited = EventHandler {
            //            println("Mouse exit")
            updatePosition(null)

            invalidate()
            it.consume()
        }
        onScroll = EventHandler {
            val change = (it.deltaY / 20.0) * 0.5
            if ((zoom >= 20 && change > 0) || (zoom <= 1 && change < 0)) return@EventHandler

            zoom += change
            if (zoom < 1)
                zoom = 1.0
            if (zoom > 20)
                zoom = 20.0

//            println("zoom=$zoom change=$change")
            invalidate()
            it.consume()
            //            println("Scroll ${it.deltaX} ${it.deltaY}")
        }

        val animationTimer = object : AnimationTimer() {
            var lastTime = 0.0
            override fun handle(now: Long) {
                try {
                    if (App.playbackController.isPlaying() || App.playbackController.time() != lastTime) {
                        draw()
                        lastTime = App.playbackController.time()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        animationTimer.start()
    }

    fun updateSelectedFixture() {
        invalidate()
    }

    fun updateFixtures() {
        invalidate()
    }

    fun updateScene(scene: ShowScene?) {
        this.scene = scene
        invalidate()
    }

    private fun updatePosition(mouseEvent: MouseEvent?) {
        mouse = if (mouseEvent != null)
            Point(mouseEvent.x, mouseEvent.y)
        else
            null
    }

    override fun minHeight(width: Double): Double {
        return 64.0
    }

    override fun maxHeight(width: Double): Double {
        return 1000.0
    }

    override fun prefHeight(width: Double): Double {
        return minHeight(width)
    }

    override fun minWidth(height: Double): Double {
        return 0.0
    }

    override fun maxWidth(height: Double): Double {
        return 10000.0
    }

    override fun isResizable(): Boolean {
        return true
    }

    override fun resize(width: Double, height: Double) {
        super.resize(width, height)

        super.setWidth(width)
        super.setHeight(height)

        zoom = 1.0

        invalidate()
    }

    private fun invalidate() {
        draw()
    }

    private fun draw() {
//        println("[${System.currentTimeMillis()}] Drawing")

        transform.setToIdentity()
        transform.appendTranslation(delta.x * zoom, delta.y * zoom)
        transform.appendScale(zoom, zoom, width / 2.0, height / 2.0)

        val g = graphicsContext2D
        g.clearRect(0.0, 0.0, width, height)

//        g.fill = Color.BLACK
//        g.fillText("${delta.x} ${delta.y} $zoom"/*${mouse?.x} ${mouse?.y}"*/, 3.0, 12.0)

        g.stroke = Color(0.0, 0.0, 0.0, 0.15)
        if (mouse != null) {
            g.strokeLine(mouse!!.x, 0.0, mouse!!.x, height)
            g.strokeLine(0.0, mouse!!.y, width, mouse!!.y)
        }

        g.stroke = Color(1.0, 1.0, 1.0, 1.0)
        scene?.let {
            for (fixture in it.fixtures) {
                val selected = App.mainScene.value!!.selectedFixture.value.orElse() == fixture
                val mouseOver = isMouseOverFixture(fixture)
                val playing = fixture.getTimeline("play")!!.valueAt(App.playbackController.time()) >= 1.0

                g.fill = when {
                    mouseOver -> Color.RED
                    selected -> Color.GREEN
                    playing -> Color.BLUE
                    else -> Color.BLACK
                }

                val location = transform.transform(fixture.location.x, fixture.location.z)
                g.fillRect(location.x, location.y, scaledValue(1.0), scaledValue(1.0))

                if (mouseOver) {
                    g.fill = Color.BLACK
                    g.strokeText(fixture.name, mouse!!.x, mouse!!.y - 12)
                    g.fillText(fixture.name, mouse!!.x, mouse!!.y - 12)
                }
            }
        }

        g.stroke = Color(1.0, 1.0, 1.0, 1.0)
        if (mouse != null) {
            g.strokeText(
                    "z=${unscaledValue(mouse!!.x - delta.x).format(2)} " +
                            "y=${unscaledValue(mouse!!.y - delta.y).format(2)}",
                    mouse!!.x,
                    mouse!!.y
            )
            g.fillText(
                    "z=${unscaledValue(mouse!!.x - delta.x).format(2)} " +
                            "y=${unscaledValue(mouse!!.y - delta.y).format(2)}",
                    mouse!!.x,
                    mouse!!.y
            )
        }
    }

    private fun isMouseOverFixture(fixture: Fixture, transform: Affine = this.transform): Boolean {
        if (this.mouse == null) return false

        val location = transform.transform(fixture.location.x, fixture.location.z)
        val size = scaledValue(1.0)
        val x = mouse!!.x
        val y = mouse!!.y

        return location.x <= x && location.x + size >= x && location.y <= y && location.y + size >= y
    }

    private fun scaledValue(input: Double) = input * zoom
    private fun unscaledValue(input: Double) = input / zoom
}