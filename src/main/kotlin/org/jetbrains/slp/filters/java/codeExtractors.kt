package org.jetbrains.slp.filters.java

// TODO ("remove it")
fun String.getCodeBetweenCodeDelimiters() =
  Regex("""(.*?)([;{}])""").findAll(this)