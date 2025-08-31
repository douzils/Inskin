package com.inskin.app

import java.util.Locale
import kotlin.math.min

fun ByteArray.toHex(max: Int = Int.MAX_VALUE): String {
    val n = min(size, max)
    if (n <= 0) return ""
    val sb = StringBuilder(n * 2)
    for (i in 0 until n) sb.append(String.format(Locale.US, "%02X", this[i].toInt() and 0xFF))
    return sb.toString()
}

@Suppress("unused")
fun ByteArray?.toHexOrEmpty(max: Int = Int.MAX_VALUE): String = this?.toHex(max) ?: ""

