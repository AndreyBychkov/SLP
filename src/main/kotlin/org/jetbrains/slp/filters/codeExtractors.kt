package org.jetbrains.slp.filters

fun String.getCodeBetweenCodeDelimiters() =
  Regex("""(.*?)([;{}])""").findAll(this)