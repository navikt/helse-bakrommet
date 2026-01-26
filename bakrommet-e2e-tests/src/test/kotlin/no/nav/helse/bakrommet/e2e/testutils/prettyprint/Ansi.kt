package no.nav.helse.bakrommet.e2e.testutils.prettyprint

fun String.with(style: Style) = ANSIString(this, foreground = style.foreground, background = style.background, style = style.style).toString()

data class Style(
    val foreground: ANSI.ColorFG? = null,
    val background: ANSI.ColorBG? = null,
    val style: ANSI.Style? = null,
) {
    companion object {
        val RED = Style(foreground = ANSI.ColorFG.RED)
        val GREEN = Style(foreground = ANSI.ColorFG.GREEN)
        val CYAN = Style(foreground = ANSI.ColorFG.CYAN)
        val YELLOW = Style(foreground = ANSI.ColorFG.YELLOW)
        val BOLD_WHITE = Style(foreground = ANSI.ColorFG.BRIGHT_WHITE, style = ANSI.Style.BOLD)
        val BRIGHT_YELLOW = Style(foreground = ANSI.ColorFG.BRIGHT_YELLOW)
    }
}

data class ANSIString(
    val text: String,
    val foreground: ANSI.ColorFG? = null,
    val background: ANSI.ColorBG? = null,
    val style: ANSI.Style? = null,
) {
    override fun toString(): String {
        val codes = listOfNotNull(foreground?.code, background?.code, style?.code).joinToString("")
        return "$codes$text${ANSI.RESET}"
    }
}

object ANSI {
    const val RESET: String = "\u001B[0m"

    enum class ColorFG(
        val code: String,
    ) {
        BLACK("\u001B[30m"),
        RED("\u001B[31m"),
        GREEN("\u001B[32m"),
        YELLOW("\u001B[33m"),
        BLUE("\u001B[34m"),
        PURPLE("\u001B[35m"),
        CYAN("\u001B[36m"),
        BRIGHT_BLACK("\u001B[90m"),
        BRIGHT_RED("\u001B[91m"),
        BRIGHT_GREEN("\u001B[92m"),
        BRIGHT_YELLOW("\u001B[93m"),
        BRIGHT_BLUE("\u001B[94m"),
        BRIGHT_PURPLE("\u001B[95m"),
        BRIGHT_CYAN("\u001B[96m"),
        BRIGHT_WHITE("\u001B[97m"),
    }

    enum class ColorBG(
        val code: String,
    ) {
        BLACK("\u001B[40m"),
        RED("\u001B[41m"),
        GREEN("\u001B[42m"),
        YELLOW("\u001B[43m"),
        BLUE("\u001B[44m"),
        PURPLE("\u001B[45m"),
        CYAN("\u001B[46m"),
        BRIGHT_YELLOW("\u001B[93m"),
        BRIGHT_PURPLE("\u001B[105m"),
        BRIGHT_WHITE("\u001B[107m"),
    }

    enum class Style(
        val code: String,
    ) {
        BOLD("\u001B[1m"),
        DIM("\u001B[2m"),
        ITALIC("\u001B[3m"),
        UNDERLINE("\u001B[4m"),
        BLINK("\u001B[5m"),
        REVERSE("\u001B[7m"),
        STRIKETHROUGH("\u001B[9m"),
    }
}
