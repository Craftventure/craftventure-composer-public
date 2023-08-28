package net.craftventure.composer.extension

import java.util.*

fun <T> Optional<T>?.orElse() = this?.orElse(null)

fun <T> T?.toOptional() = Optional.ofNullable(this)