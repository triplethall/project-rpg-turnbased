package ru.triplethall.rpgturnbased

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

    fun generate() {
        for (x in 0 until width) {
            for (y in 0 until height) {
                walkable[x][y] = true
            }
        }

        val random = kotlin.random.Random
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (random.nextFloat() < 0.2f) {
                    walkable[x][y] = false
                }
            }
        }

        val centerX = width / 2
        val centerY = height / 2
        for (x in centerX - 2..centerX + 2) {
            for (y in centerY - 2..centerY + 2) {
                if (x in 0 until width && y in 0 until height) {
                    walkable[x][y] = true
                }
            }
        }
    }
}
