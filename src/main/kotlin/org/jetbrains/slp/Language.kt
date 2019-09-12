package org.jetbrains.slp

enum class Language {
    JAVA {
        override val extensions = listOf("java")
    },
    PYTHON {
        override val extensions = listOf("py", "pyw", "py3")
    },
    KOTLIN {
        override val extensions = listOf("kt", "kts")
    },
    UNKNOWN {
        override val extensions = listOf("")
    };

    abstract val extensions: List<String>

    companion object {
        fun getLanguage(extension: String): Language {
            for (language in values()) {
                if (extension in language.extensions)
                    return language
            }
            return UNKNOWN
        }
    }
}