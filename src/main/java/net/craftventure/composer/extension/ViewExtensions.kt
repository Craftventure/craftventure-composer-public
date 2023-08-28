package net.craftventure.composer.extension

import javafx.scene.control.TextField
import javafx.scene.control.TextFormatter
import javafx.util.converter.DoubleStringConverter
import java.util.regex.Pattern


fun TextField.doubleFormatting() {
    val validDoubleText = Pattern.compile("-?((\\d*)|(\\d+\\.\\d*))")

    val textFormatter = TextFormatter<Double>(DoubleStringConverter(), 0.0) { change ->
        val newText = change.controlNewText
        return@TextFormatter if (validDoubleText.matcher(newText).matches()) {
            change
        } else
            null
    }

    setTextFormatter(textFormatter)
}