package ru.triplethall.rpgturnbased

import kotlin.math.sqrt
import kotlin.jvm.JvmOverloads

class GameMap(
    val width: Int = 21,
    val height: Int = 21
) {
    private val walkable = Array(width) { BooleanArray(height) { true } }

    fun isWalkable(x: Int, y: Int): Boolean {
        if (x < 0 || x >= width || y < 0 || y >= height) return false
        return walkable[x][y]
    }

    fun setBlocked(x: Int, y: Int, blocked: Boolean) {
        if (x in 0 until width && y in 0 until height) {
            walkable[x][y] = !blocked
        }
    }

    @JvmOverloads
    fun generate(seed: Int = 42) {
        val perlin = PerlinNoise(seed)

        val centerX = width / 2.0
        val centerY = height / 2.0
        val maxDist = minOf(centerX, centerY) // радиус острова
        val noiseScale = 0.2                         // масштаб шума
        val threshold = 1.0                           // порог

        for (x in 0 until width) {
            for (y in 0 until height) {
                val dx = (x - centerX) / maxDist
                val dy = (y - centerY) / maxDist
                val distance = sqrt(dx * dx + dy * dy)
                val noise = perlin.noise(x * noiseScale, y * noiseScale)
                val value = distance + noise * 0.3
                walkable[x][y] = value < threshold
            }
        }

        // Гарантируем, что центральная клетка – суша
        walkable[centerX.toInt()][centerY.toInt()] = true
    }
}

// Вспомогательный класс для шума Перлина
class PerlinNoise(seed: Int) {
    private val p = IntArray(512)
    private val permutation = IntArray(256) { (it + seed) % 256 }

    init {
        for (i in 0 until 256) {
            p[i] = permutation[i]
            p[i + 256] = permutation[i]
        }
    }

    fun noise(x: Double, y: Double): Double {
        val xi = x.toInt() and 255
        val yi = y.toInt() and 255
        val xf = x - x.toInt()
        val yf = y - y.toInt()
        val u = fade(xf)
        val v = fade(yf)
        val a = p[xi] + yi
        val b = p[xi + 1] + yi
        return lerp(v,
            lerp(u, grad(p[a], xf, yf), grad(p[b], xf - 1, yf)),
            lerp(u, grad(p[a + 1], xf, yf - 1), grad(p[b + 1], xf - 1, yf - 1))
        )
    }

    private fun fade(t: Double): Double = t * t * t * (t * (t * 6 - 15) + 10)
    private fun lerp(t: Double, a: Double, b: Double): Double = a + t * (b - a)
    private fun grad(hash: Int, x: Double, y: Double): Double {
        val h = hash and 3
        return when (h) {
            0 -> x + y
            1 -> -x + y
            2 -> x - y
            else -> -x - y
        }
    }
}
