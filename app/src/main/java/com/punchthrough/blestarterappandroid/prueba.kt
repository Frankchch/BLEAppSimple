package com.punchthrough.blestarterappandroid

import java.nio.ByteBuffer
import java.util.*
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

fun main() {
    val a:Double = -11.831865187310996

    val b = ByteBuffer.allocate(java.lang.Double.BYTES)
        .putDouble(a).array()

    val c = "c027a9ea3bdc5d00".hexToBytes()

    val d = "0".deDoubleAHex()

    val e = a.toLong().toString(16)

    val f = "01,-11 .  8   3, -75.62".formato().hexToBytes()

    val g = a.bytes()

    println("$b / " + String(b))
    println("$c / " + String(c))
    println(d)
    println(e)
    println("$f / " + String(f))
    println(g)
    println(true)
}

private fun String.hexToBytes() =
    this.chunked(2).map { it.toUpperCase(Locale.US).toInt(16).toByte() }.toByteArray()

private fun String.deDoubleAHex():String {
    if (this.toDouble() == 0.0) return "0000000000000000"
    val b = this.split(".")
    val exponente = floor(ln(abs(this.toDouble())) / ln(2.0))
    var auxi = 2.0.pow(exponente)
    var exponente_bi = (1023.0 + exponente).toString().split(".")[0].toInt().toString(2)
    while (exponente_bi.length < 11){
        exponente_bi = "0$exponente_bi"
    }

    var bits = if (this.toDouble() < 0){"1" + exponente_bi} else{"0"+exponente_bi}
    var ent_bytes = bits.chunked(4).map{it.toInt(2).toString(16)}.joinToString("")
    var mant = (abs(this.toDouble()) /auxi - 1)		//.toString().split(".")[1].chunked(1).toMutableList()
    var j = 0
    var mant_byte = ""
    while (j<13){
        mant *= 16
        var ent1 = mant.toString().split(".")[0].toInt()
        mant = ("0." + mant.toString().split(".")[1]).toDouble()
        mant_byte += ent1.toString(16)
        j += 1
    }

    var bytes = ent_bytes + mant_byte
    var reversed_bytes = bytes.chunked(2).reversed().joinToString("")
    return reversed_bytes
}

private fun String.formato():String {
    val arreglo = this.filter { !it.isWhitespace() }.split(",")
    val latitud_hex = arreglo[1].deDoubleAHex()
    val longitud_hex = arreglo[2].deDoubleAHex()
    val hex = arreglo[0] + latitud_hex + longitud_hex
    return hex
}

fun Double.bytes() =
    ByteBuffer.allocate(java.lang.Long.BYTES)
        .putLong(java.lang.Double.doubleToLongBits(this))

class prueba {
}