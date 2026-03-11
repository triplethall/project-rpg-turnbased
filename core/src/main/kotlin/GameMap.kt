package ru.triplethall.rpgturnbased

import kotlin.random.Random  //для рандома

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
        initializeRandomMap()

        for (iteration in 1..5) {
            applyCellularAutomaton()
        }

        ensureStartAreaIsWalkable()
    }

    private fun initializeRandomMap() {
        val random = Random

        for (x in 0 until width) {
            for (y in 0 until height) {
                walkable[x][y] = random.nextFloat() >= 0.25f //25% что будет вода
            }
        }
    }

    private fun applyCellularAutomaton() {
        val newWalkable = Array(width) { x ->
            BooleanArray(height) { y -> walkable[x][y] }
        }

        val random = kotlin.random.Random

        for (x in 0 until width) {
            for (y in 0 until height) {
                val wallCount = countAdjacentWalls(x, y)

                if (walkable[x][y]) { // Сейчас земля
                    // Основное правило
                    newWalkable[x][y] = wallCount < 4

                    // Дополнительное правило: если земля окружена 8 землёй, есть шанс стать водой
                    if (wallCount == 0 && random.nextFloat() < 0.05f) {
                        newWalkable[x][y] = false  // Становится водой
                    }
                } else { // Сейчас вода
                    newWalkable[x][y] = wallCount < 3
                }
            }
        }

        // Применяем изменения
        for (x in 0 until width) {
            for (y in 0 until height) {
                walkable[x][y] = newWalkable[x][y]
            }
        }
    }

    //считает стенки для applyCellularAutomaton
    private fun countAdjacentWalls(x: Int, y: Int): Int {
        var count = 0

        for (dx in -1..1) {
            for (dy in -1..1) {
                if (dx == 0 && dy == 0) continue

                val nx = x + dx
                val ny = y + dy

                if (nx !in 0 until width || ny !in 0 until height) {
                    count++
                } else if (!walkable[nx][ny]) {
                    count++
                }
            }
        }

        return count
    }

    //центр будет всегда землёй
    private fun ensureStartAreaIsWalkable() {
        val centerX = width / 2
        val centerY = height / 2

        for (x in (centerX - 2)..(centerX + 2)) {
            for (y in (centerY - 2)..(centerY + 2)) {
                if (x in 0 until width && y in 0 until height) {
                    walkable[x][y] = true
                }
            }
        }
    }
}
