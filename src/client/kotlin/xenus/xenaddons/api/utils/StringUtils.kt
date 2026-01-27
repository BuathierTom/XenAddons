package xenus.xenaddons.api.utils

import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import java.text.NumberFormat
import java.util.*

object StringUtils {
    private val removeCodesRegex = "[\\u00a7&][0-9a-fk-or]".toRegex(RegexOption.IGNORE_CASE)
    private val romanValues = mapOf(
        'I' to 1,
        'V' to 5,
        'X' to 10,
        'L' to 50,
        'C' to 100,
        'D' to 500,
        'M' to 1000,
    )
    private val colorToFormat = ChatFormatting.entries.mapNotNull { format ->
        TextColor.fromLegacyFormat(format)?.let { it to format }
    }.toMap()
    private val escapedAmp = "&{2}".toRegex()
    private val ampToSection = "&(?=[0-9a-fklmnor])".toRegex()

    fun String.clearCodes(): String = this.replace(removeCodesRegex, "")

    fun String.replaceCodes(): String = this
        .split(escapedAmp)
        .joinToString("&") {
            it.replace(ampToSection, "§")
        }

    fun parseRoman(roman: String): Int {
        var lastValue = 0
        var total = 0
        roman.toCharArray().forEach {
            val value = romanValues[it] ?: return@forEach
            if (lastValue < value) total -= lastValue
            else total += lastValue
            lastValue = value
        }
        return total + lastValue
    }

    private val romanNums = listOf("", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX").let { r0to9 ->
        listOf("", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC").flatMap { pre ->
            r0to9.map { pre + it }
        }
    }.toTypedArray()

    fun formatRoman(num: Int): String {
        if (num < 0) return '-' + formatRoman(-num)
        if (num < romanNums.size) return romanNums[num]
        return buildString {
            append("M".repeat(num / 1000))
            var num = num % 1000
            if (num >= 900) {
                num -= 900
                append("CM")
            }
            if (num >= 500) {
                num -= 500
                append("D")
            }
            if (num >= 400) {
                num -= 400
                append("CD")
            }
            while (num >= 100) {
                num -= 100
                append("C")
            }
            append(romanNums[num])
        }
    }

    private fun parseStyle(style: Style): String = buildString {
        append("§r")

        style.color?.let(colorToFormat::get)?.run(::append)

        when {
            style.isBold -> append("§l")
            style.isItalic -> append("§o")
            style.isUnderlined -> append("§n")
            style.isStrikethrough -> append("§m")
            style.isObfuscated -> append("§k")
        }
    }

    private fun parseFormat(_text: Component): String {
        var str = ""

        _text.contents.visit({ style, text ->
            val styleFormat = parseStyle(style)
            str += "${styleFormat}$text"
            Optional.empty<Any>()
        }, _text.style)

        return str
    }

    fun Component.colorCodes(): String {
        var str = parseFormat(this)

        str += this.siblings.joinToString("", transform = ::parseFormat)

        return str
    }
}
