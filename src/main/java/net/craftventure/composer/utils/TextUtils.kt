package net.craftventure.composer.utils

import javafx.scene.text.Font
import java.awt.FontMetrics
import java.awt.Graphics

object TextUtils {

    fun computeStringWidth(text: String, font: Font, graphics: Graphics): Int =
            text.sumByDouble {
                graphics.getFontMetrics(font)
        0.0
//                Toolkit.getToolkit().fontLoader.getCharWidth(it, font).toDouble()
            }.toInt()
}