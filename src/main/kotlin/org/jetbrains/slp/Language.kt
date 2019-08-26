package org.jetbrains.slp

enum class Language {
    JAVA {
        override val extensions = listOf("java")
    },
    PYTHON {
        override val extensions = listOf("py", "pyw", "py3")
    },
    UNKNOWN {
        override val extensions = listOf("")
    };

    abstract val extensions: List<String>
}