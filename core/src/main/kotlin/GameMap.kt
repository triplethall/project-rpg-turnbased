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
        // Шаг 1: Заполняем карту случайными значениями
        initializeRandomMap()

        // Шаг 2: Применяем клеточный автомат для сглаживания (создания островов)
        for (iteration in 1..5) {
            applyCellularAutomaton()
        }

        // Шаг 3: Убеждаемся, что центр карты проходим (стартовая зона)
        ensureStartAreaIsWalkable()
    }

    private fun initializeRandomMap() {
        val random = kotlin.random.Random

        for (x in 0 until width) {
            for (y in 0 until height) {
                // 45% вероятность того, что клетка будет непроходимой (водой/преградой)
                walkable[x][y] = random.nextFloat() >= 0.2f
            }
        }
    }

    private fun applyCellularAutomaton() {
        // Создаем копию текущего состояния
        val newWalkable = Array(width) { x ->
            BooleanArray(height) { y -> walkable[x][y] }
        }

        for (x in 0 until width) {
            for (y in 0 until height) {
                val wallCount = countAdjacentWalls(x, y)

                // Правила клеточного автомата:
                // Если у клетки 4 или более соседей - стена/вода, она становится непроходимой
                // Если меньше 2 соседей - она становится проходимой (чтобы убрать одиночные клетки)
                if (walkable[x][y]) { // Сейчас проходимо (земля)
                    // Земля становится водой, если у нее слишком много соседей-воды
                    newWalkable[x][y] = wallCount < 4
                } else { // Сейчас непроходимо (вода)
                    // Вода становится землей, если у нее мало соседей-воды
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

    private fun countAdjacentWalls(x: Int, y: Int): Int {
        var count = 0

        for (dx in -1..1) {
            for (dy in -1..1) {
                // Пропускаем саму клетку
                if (dx == 0 && dy == 0) continue

                val nx = x + dx
                val ny = y + dy

                // Считаем клетки за границами как непроходимые (вода/стены)
                if (nx !in 0 until width || ny !in 0 until height) {
                    count++
                } else if (!walkable[nx][ny]) {
                    count++
                }
            }
        }

        return count
    }

    private fun ensureStartAreaIsWalkable() {
        val centerX = width / 2
        val centerY = height / 2

        // Делаем центр и его окрестности проходимыми
        for (x in (centerX - 2)..(centerX + 2)) {
            for (y in (centerY - 2)..(centerY + 2)) {
                if (x in 0 until width && y in 0 until height) {
                    walkable[x][y] = true
                }
            }
        }
    }
}
