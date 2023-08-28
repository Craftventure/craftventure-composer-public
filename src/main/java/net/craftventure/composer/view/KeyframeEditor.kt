package net.craftventure.composer.view

import com.sun.javafx.tk.Toolkit
import javafx.event.EventHandler
import javafx.scene.Cursor
import javafx.scene.canvas.Canvas
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import javafx.scene.transform.Affine
import net.craftventure.composer.App
import net.craftventure.composer.extension.*
import net.craftventure.composer.fixture.Fixture
import net.craftventure.composer.fixture.property.NumberProperty
import net.craftventure.composer.scene.ShowScene
import net.craftventure.composer.timeline.KeyFrame
import net.craftventure.composer.timeline.Timeline
import net.craftventure.composer.utils.TextUtils
import org.apache.commons.lang3.time.DurationFormatUtils


class KeyframeEditor : Canvas() {
    private val VALUE_ZOOM_WIDTH = 60.0
    private val TOPBAR_HEIGHT = 40.0
    private val BASE_BACKGROUND = Color.web("#232323")
    private val CONTROL_COLOR = Color.web("#b1b1b1")
    private val SEPARATOR_COLOR = Color.web("#313131")
    private val ACCENT_COLOR = Color.web("#2d8ceb")
    private val DEFAULT_FONT = graphicsContext2D.font
    private val PROPERTY_HEIGHT = 30.0
    private val PROPERTIES_WIDTH = 100.0
    private val MAX_TIMEZOOM = 400.0
    private val MAX_VALUEZOOM = 400.0

    private var mouse: Point? = null
    private var drag: Point? = null
    private var scrollY: Double = 0.0
    private var startTimeSeconds: Double = 0.0

    private var scene: ShowScene? = null
    private val transform = Affine()

    private var fixture: Fixture? = null
    private var timeZoom: Double = MAX_TIMEZOOM
    private var valueZoom: Double = MAX_VALUEZOOM
    private var startValue: Double = 0.0

    private var selectedKeyframe: KeyFrame? = null
    private var isDraggingTimeline = false

    private val timelines = mutableListOf<Timeline<NumberProperty<*>>>()
    private var selectedTimeline: Timeline<NumberProperty<*>>? = null

    private fun timelineColor(timeline: Timeline<*>) = timelineColor(timelines.indexOf(timeline))

    private fun timelineColor(index: Int): Color {
        return when (index % 10) {
            0 -> Color.RED
            1 -> Color.AQUA
            2 -> Color.BURLYWOOD
            3 -> Color.BLUE
            4 -> Color.DARKGREEN
            5 -> Color.DARKMAGENTA
            6 -> Color.GREENYELLOW
            7 -> Color.MAROON
            8 -> Color.GOLDENROD
            9 -> Color.MEDIUMVIOLETRED
            10 -> Color.ORANGERED
            else -> Color.CYAN
        }
    }

    init {
        App.playbackController.timeAsFlowable().subscribe({
            invalidate()
        })
        onMousePressed = EventHandler {
            //            println("Mouse pressed")
            updatePosition(it)

            if (it.x < PROPERTIES_WIDTH) {
                selectedKeyframe = null

                var y = scrollY + TOPBAR_HEIGHT
                fixture?.properties?.let { properties ->
                    for (property in properties) {
                        val timeline = fixture?.getTimeline(property) ?: continue
                        if (it.y > y && it.y < y + PROPERTY_HEIGHT) {
                            this.selectedTimeline = timeline
                            selectedKeyframe = null
                            break
                        }
                        y += PROPERTY_HEIGHT
                    }
                }
            } else if (it.y < TOPBAR_HEIGHT) {
                isDraggingTimeline = true
                val timeAtX = timeAtX(it.x)
                App.playbackController.sync(timeAtX)
            } else if (it.x > PROPERTIES_WIDTH + VALUE_ZOOM_WIDTH && it.y > TOPBAR_HEIGHT) {
                val keyframe = getKeyframeAtCursor()
                if (keyframe != null && it.isShiftDown) {
                    App.instance.openKeyFrameEditor(keyframe)
                } else if (it.isControlDown) {
                    val keyFrame = KeyFrame((timeAtX(it.x) * 1000).toLong(), valueAtY(it.y))
                    selectedTimeline?.addKeyframe(keyFrame)
                }
                selectedKeyframe = keyframe

                if (it.isSecondaryButtonDown && selectedKeyframe != null) {
                    selectedTimeline?.removeKeyFrame(selectedKeyframe!!)
                    selectedKeyframe = null
                }
            }

            invalidate()
            it.consume()
        }
        onMouseDragged = EventHandler {
            //            println("Mouse dragged ${it.isControlDown}")
            updatePosition(it)

            drag = Point(it.x, it.y)

            if (isDraggingTimeline) {
                val timeAtX = timeAtX(it.x)
                App.playbackController.sync(timeAtX)
            } else {
                selectedKeyframe?.let { selectedKeyframe ->
                    selectedKeyframe.keyValue = valueAtY(drag!!.y)
                    selectedKeyframe.at = (timeAtX(drag!!.x) * 1000).toLong()
                }
                timelines.forEach {
                    it.clampAll()
                }
            }

            invalidate()
            it.consume()
        }
        onMouseDragReleased = EventHandler {
            //            println("Mouse drag released ${it.isControlDown}")

            drag = null
            isDraggingTimeline = false
            timelines.forEach {
                it.resortIfUnsorted()
                it.clampAll()
            }

            updatePosition(it)
            invalidate()
            it.consume()
        }
        onMouseReleased = EventHandler {
            //            println("Mouse released")
            updatePosition(it)

            drag = null
            isDraggingTimeline = false

            timelines.forEach {
                it.resortIfUnsorted()
                it.clampAll()
            }

            invalidate()
            it.consume()
        }
        onMouseMoved = EventHandler {
            updatePosition(it)

            if (cursor != Cursor.DEFAULT)
                cursor = Cursor.DEFAULT

            val keyframe = getKeyframeAtCursor()
            if (keyframe != null) {
                cursor = Cursor.MOVE
            }

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

            if (it.x <= PROPERTIES_WIDTH) {
                val yChange = (it.deltaY / 3.0)
                scrollY += yChange
                if (scrollY > 0)
                    scrollY = 0.0
            } else if (it.x > PROPERTIES_WIDTH + VALUE_ZOOM_WIDTH) {
                val yChange = (it.deltaY / 20.0).let { if (it > 0) 1.1 else if (it < 0) 0.8 else 1.0 }
                timeZoom *= yChange
                if (timeZoom < 1)
                    timeZoom = 1.0
                if (timeZoom > MAX_TIMEZOOM)
                    timeZoom = MAX_TIMEZOOM

                val xChange = ((it.deltaX / 3.0) / timeZoom)
                startTimeSeconds -= xChange
                if (startTimeSeconds < 0)
                    startTimeSeconds = 0.0
            } else if (it.x > PROPERTIES_WIDTH) {
                val yChange = (it.deltaY / 20.0).let { if (it > 0) 1.1 else if (it < 0) 0.8 else 1.0 }
                valueZoom *= yChange
                if (valueZoom < 1)
                    valueZoom = 1.0
                if (valueZoom > MAX_VALUEZOOM)
                    valueZoom = MAX_VALUEZOOM

                val xChange = ((it.deltaX / 3.0) / valueZoom)
                startValue -= xChange
            }

            invalidate()
            it.consume()
        }
    }

    private fun getKeyframeAtCursor(): KeyFrame? {
        mouse?.let { mouse ->
            val point = Point(0.0, 0.0)
            selectedTimeline?.let { timeline ->
                for (keyframe in timeline.keyframes) {
                    val x = timeToX(keyframe.at / 1000.0)
                    val y = valueToY(keyframe.keyValue.toDouble())
                    point.x = x
                    point.y = y

                    if (mouse.distanceTo(point) <= 3) {
                        return keyframe
                    }
                }
            }
        }
        return null
    }

    fun setFixture(fixture: Fixture?) {
        this.fixture = fixture

        timelines.clear()
        fixture?.let {
            for (property in fixture.properties) {
                val timeline = fixture.getTimeline(property)
                if (timeline != null) {
                    timelines.add(timeline)
                }
            }
        }
        selectedTimeline = timelines.firstOrNull()
        selectedKeyframe = null
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

        invalidate()
    }

    private fun invalidate() {
        draw()
    }

    private fun draw() {
        // Clear canvas
        val g = graphicsContext2D
        g.clearRect(0.0, 0.0, width, height)

        // Draw properties background
        g.fill = BASE_BACKGROUND
        g.fillRect(PROPERTIES_WIDTH, 0.0, width - PROPERTIES_WIDTH, height)

        drawPropertyGraphs()
        drawTimelineIndicators()
        drawValueZoomIndicators()

        // Draw separator between properties/keyframes
        g.fill = BASE_BACKGROUND
        g.fillRect(0.0, 0.0, PROPERTIES_WIDTH, height)

        drawProperties()

        // Draw properties topbar
        g.fill = CONTROL_COLOR
        g.fillText(fixture?.name ?: "", 3.0, 12.0, PROPERTIES_WIDTH)
        g.fillText(fixture?.javaClass?.simpleName ?: "", 3.0, 25.0, PROPERTIES_WIDTH)

        // Draw debug data
//        g.fillText("$scrollY $startTimeSeconds $timeZoom", 3.0, 20.0, PROPERTIES_WIDTH.toDouble())

        drawMouseIndicator()
        drawPlaybackIndicator()

        mouse?.let {
            val keyFrame = getKeyframeAtCursor() ?: return@let
            g.fill = ACCENT_COLOR
            g.fillText("${keyFrame.keyValue.toDouble().format(3)}@${(keyFrame.at / 1000.0).format(3)}", it.x, it.y - 5.0)
        }
    }

    private fun drawPlaybackIndicator() {
        val g = graphicsContext2D
        g.fill = ACCENT_COLOR
        g.stroke = ACCENT_COLOR
        val x = timeToX(App.playbackController.time())
        if (x < PROPERTIES_WIDTH + VALUE_ZOOM_WIDTH) return
        val y = TOPBAR_HEIGHT
        g.strokeLine(x, TOPBAR_HEIGHT, x, height)
        g.fillPolygon(doubleArrayOf(x, x + 5, x - 5), doubleArrayOf(y, y - 5, y - 5), 3)
        g.fillRect(x + 5 - 10.0, y - 5 - 10.0, 10.0, 10.0)
    }

    private fun drawPropertyGraphs() {
        for (timeline in timelines) {
            if (timeline == selectedTimeline) continue
            drawTimelineGraph(timeline)
        }
        selectedTimeline?.let { timeline ->
            drawTimelineGraph(timeline)
            drawTimelineKeyframes(timeline)
        }
    }

    private fun drawTimelineGraph(timeline: Timeline<*>, alpha: Double = 1.0) {
        val g = graphicsContext2D
        timeline.resortIfUnsorted()
        val color = timelineColor(timeline).deriveColor(1.0, 1.0, 1.0, alpha)
//            val handleColor = color.deriveColor(1.0, 1.0, 1.5, 1.0)
        g.fill = color
        g.stroke = color

        g.lineWidth = 1.0
        g.beginPath()
        var moved = false
        for (i in (PROPERTIES_WIDTH + VALUE_ZOOM_WIDTH).floor().toInt()..width.ceil().toInt()) {
            val atTime = timeAtX(i.toDouble())
            val valueAtTime = timeline.valueAt(atTime)
            val atY = valueToY(valueAtTime)

            if (!moved) {
                moved = true
                g.moveTo(i.toDouble(), atY)
            } else {
                g.lineTo(i.toDouble(), atY)
            }
        }
        g.stroke()
        g.closePath()
    }

    private fun drawTimelineKeyframes(timeline: Timeline<*>) {
        val handleRadius = 3.0
        val g = graphicsContext2D
        timeline.resortIfUnsorted()
        val color = timelineColor(timeline)
//            val handleColor = color.deriveColor(1.0, 1.0, 1.5, 1.0)
        g.fill = color
        g.stroke = color

        g.lineWidth = 1.0

        for (keyframe in timeline.keyframes) {
            val x = timeToX(keyframe.at / 1000.0)
            val y = valueToY(keyframe.keyValue.toDouble())

            g.lineWidth = 1.0
            g.stroke = ACCENT_COLOR
            g.fill = ACCENT_COLOR

            g.stroke = color
            g.fill = color
            if (x >= PROPERTIES_WIDTH + VALUE_ZOOM_WIDTH && x <= width && y >= 0 && y <= height) {
                g.fillOval(x - handleRadius, y - handleRadius, handleRadius * 2, handleRadius * 2)
            }
        }
    }

    private fun drawProperties() {
        val time = App.playbackController.time()
        var y = scrollY + TOPBAR_HEIGHT

        val g = graphicsContext2D
        g.fill = CONTROL_COLOR
        g.stroke = CONTROL_COLOR
        g.lineWidth = 1.0
        val properties = fixture?.properties ?: return
        for (property in properties) {
            fixture?.getTimeline(property)?.let { timeline ->
                g.fill = if (timeline == selectedTimeline) ACCENT_COLOR else BASE_BACKGROUND
                g.fillRect(0.0, y, PROPERTIES_WIDTH, PROPERTY_HEIGHT)

                g.strokeLine(0.0, y, PROPERTIES_WIDTH, y)
                g.fill = timelineColor(timeline)
                g.fillText(timeline.property.name, 3.0, y + (g.font.size + 2), PROPERTIES_WIDTH)
                g.fillText(timeline.valueAt(time).format(3), 3.0, y + 25, PROPERTIES_WIDTH)
                y += PROPERTY_HEIGHT
            }
        }
    }

    private fun currentTimePerPixel() = 1 / timeZoom
    private fun currentValuePerPixel() = 1 / valueZoom

    private fun calculateTimeLabelKeys() = (MAX_TIMEZOOM / timeZoom).toInt().toDouble().clamp(0.5, 100.0)
    private fun calculateValueLabelKeys() = (MAX_VALUEZOOM / valueZoom).toInt().toDouble().clamp(0.5, 100.0)

    private fun valueAtY(y: Double) = startValue - (currentValuePerPixel() * TOPBAR_HEIGHT) + (currentValuePerPixel() * y)
    private fun valueToY(value: Double) = -(startValue / currentValuePerPixel()) + (value / currentValuePerPixel()) + TOPBAR_HEIGHT
    private fun timeAtX(x: Double) = startTimeSeconds - (currentTimePerPixel() * (PROPERTIES_WIDTH + VALUE_ZOOM_WIDTH)) + (currentTimePerPixel() * x)
    private fun timeToX(timeInSeconds: Double) = -(startTimeSeconds / currentTimePerPixel()) + (timeInSeconds / currentTimePerPixel()) + (PROPERTIES_WIDTH + VALUE_ZOOM_WIDTH)

    private fun drawValueZoomIndicators() {
        val g = graphicsContext2D

        g.fill = BASE_BACKGROUND
        g.fillRect(PROPERTIES_WIDTH, 0.0, VALUE_ZOOM_WIDTH, height)

        g.stroke = CONTROL_COLOR
        g.fill = CONTROL_COLOR

        val labelAt = calculateValueLabelKeys()
        val labelCount = ((valueAtY(height) - valueAtY(0.0)) / labelAt).abs()

        val offset = (valueAtY(0.0) % labelAt) / labelAt
        val spaceInbetween = height / labelCount

        g.lineWidth = 1.0
        for (l in 0..Math.ceil(labelCount + 1).toInt()) {
            val y = (l * spaceInbetween) - (offset * spaceInbetween)
            if (y < -g.font.size || y > height + g.font.size) continue
            val valueAtY = valueAtY(y).round().toDouble()
            g.strokeLine(PROPERTIES_WIDTH + VALUE_ZOOM_WIDTH - 20, y, PROPERTIES_WIDTH + VALUE_ZOOM_WIDTH, y)

            val label = "$valueAtY"
            val width = TextUtils.computeStringWidth(label, g.font)
            g.fillText(label, PROPERTIES_WIDTH + VALUE_ZOOM_WIDTH - 25.0 - width, y + (g.font.size / 2.0), VALUE_ZOOM_WIDTH - 25)
        }
        for (l in 0..Math.ceil(labelCount + 1).toInt() * 10) {
            val y = ((l / 10.0) * spaceInbetween) - (offset * spaceInbetween)
            if (y < -g.font.size || y > height + g.font.size) continue
            g.strokeLine(PROPERTIES_WIDTH + VALUE_ZOOM_WIDTH - 5.0, y, PROPERTIES_WIDTH + VALUE_ZOOM_WIDTH, y)
        }
    }

    private fun drawTimelineIndicators() {
        val framesStart = PROPERTIES_WIDTH + VALUE_ZOOM_WIDTH
        val g = graphicsContext2D
        g.fill = BASE_BACKGROUND
        g.fillRect(PROPERTIES_WIDTH, 0.0, width - PROPERTIES_WIDTH, TOPBAR_HEIGHT)

        g.stroke = CONTROL_COLOR
        g.fill = CONTROL_COLOR

        val labelAt = calculateTimeLabelKeys()

        val keyframeViewpartWidth = (width - framesStart).toInt()
        val endTimeDisplayed = timeAtX(width)//startTimeSeconds + (keyframeViewpartWidth / timeZoom)
        val labelCount = ((endTimeDisplayed - startTimeSeconds) / labelAt).abs()

        val offset = (startTimeSeconds % labelAt) / labelAt
        val spaceInbetween = keyframeViewpartWidth / labelCount

        g.lineWidth = 1.0
        for (l in 0..Math.ceil(labelCount + 1).toInt()) {
            val x = framesStart + (l * spaceInbetween) - (offset * spaceInbetween)
            if (x < framesStart || x > width) continue
            val timeAtX = timeAtX(x)//Math.floor(startTimeSeconds - (startTimeSeconds % labelAt)) + (l * labelAt)
            g.strokeLine(x, TOPBAR_HEIGHT - 10, x, TOPBAR_HEIGHT)

            val label = formatSeconds(timeAtX)
            val width = TextUtils.computeStringWidth(label, g.font)
            g.fillText(label, x - (width / 2.0), TOPBAR_HEIGHT - 13, PROPERTIES_WIDTH)
        }

        g.lineWidth = 1.0
        for (l in 0..Math.ceil(labelCount + 1).toInt() * 10) {
            val x = framesStart + ((l / 10.0) * spaceInbetween) - (offset * spaceInbetween)
            if (x < framesStart || x > width) continue
            g.strokeLine(x, TOPBAR_HEIGHT - 5, x, TOPBAR_HEIGHT)
        }
    }

    private fun drawMouseIndicator() {
        val g = graphicsContext2D

        mouse?.let {
            g.lineWidth = 1.0
            if (it.x >= PROPERTIES_WIDTH + VALUE_ZOOM_WIDTH) {
                g.fill = CONTROL_COLOR
                g.stroke = CONTROL_COLOR
                g.strokeLine(it.x, TOPBAR_HEIGHT - 20, it.x, TOPBAR_HEIGHT)

                val label = formatSecondsWithMillis(timeAtX(it.x))
                val width =TextUtils.computeStringWidth(label, g.font)
                g.fillText(label, it.x - (width / 2.0), TOPBAR_HEIGHT - 25, PROPERTIES_WIDTH)
            }
            g.lineWidth = 1.0
            if (it.x >= PROPERTIES_WIDTH) {
                g.fill = CONTROL_COLOR
                g.stroke = CONTROL_COLOR
                g.strokeLine(PROPERTIES_WIDTH + VALUE_ZOOM_WIDTH - 20, it.y, PROPERTIES_WIDTH + VALUE_ZOOM_WIDTH, it.y)

                val label = valueAtY(it.y).format(2)
                val width = TextUtils.computeStringWidth(label, g.font)
                g.fillText(label, PROPERTIES_WIDTH + -width, it.y + (g.font.size / 2.0))
            }
        }
    }

    private fun formatSecondsWithMillis(seconds: Double) = DurationFormatUtils.formatDuration((seconds * 1000).toLong(), "mm:ss.SSS")
    private fun formatSeconds(seconds: Double) = DurationFormatUtils.formatDuration((seconds * 1000).toLong(), "mm:ss")
}