package ru.triplethall.rpgturnbased

import kotlin.random.Random

enum class CaveTile {
    FLOOR,
    WALL,
    EXIT
}

class CaveMap(
    val width: Int = 30,
    val height: Int = 30
) {
    private val tiles = Array(width) { Array(height) { CaveTile.WALL } }
    var exitX: Int = 0
    var exitY: Int = 0
    var playerStartX: Int = 0
    var playerStartY: Int = 0

    init {
        generate()
    }

    fun getTile(x: Int, y: Int): CaveTile {
        if (x !in 0 until width || y !in 0 until height) return CaveTile.WALL
        return tiles[x][y]
    }

    fun isWalkable(x: Int, y: Int): Boolean {
        val t = getTile(x, y)
        return t == CaveTile.FLOOR || t == CaveTile.EXIT
    }

    private fun generate() {
        val random = Random
        // 1. Заполняем стенами
        for (x in 0 until width) {
            for (y in 0 until height) {
                tiles[x][y] = CaveTile.WALL
            }
        }

        // 2. Комнаты
        val rooms = mutableListOf<Pair<Pair<Int, Int>, Pair<Int, Int>>>()
        val roomCount = random.nextInt(4, 8)

        repeat(roomCount) {
            var placed = false
            repeat(20) {
                val w = random.nextInt(4, 8)
                val h = random.nextInt(4, 8)
                val x = random.nextInt(1, width - w - 1)
                val y = random.nextInt(1, height - h - 1)

                var overlap = false
                for ((r, _) in rooms) {
                    if (x < r.first + w + 2 && x + w + 2 > r.first &&
                        y < r.second + h + 2 && y + h + 2 > r.second) {
                        overlap = true
                        break
                    }
                }
                if (!overlap) {
                    rooms.add(Pair(Pair(x, y), Pair(w, h)))
                    placed = true
                    return@repeat
                }
            }
            if (!placed) {
                val w = random.nextInt(4, 8)
                val h = random.nextInt(4, 8)
                val x = random.nextInt(1, width - w - 1)
                val y = random.nextInt(1, height - h - 1)
                rooms.add(Pair(Pair(x, y), Pair(w, h)))
            }
        }

        // Рисуем комнаты
        for ((pos, size) in rooms) {
            val (x, y) = pos
            val (w, h) = size
            for (dx in 0 until w) {
                for (dy in 0 until h) {
                    if (x + dx in 0 until width && y + dy in 0 until height) {
                        tiles[x + dx][y + dy] = CaveTile.FLOOR
                    }
                }
            }
        }

        // Соединяем комнаты коридорами
        for (i in 1 until rooms.size) {
            val (prevPos, _) = rooms[i - 1]
            val (curPos, _) = rooms[i]
            val (px, py) = prevPos
            val (cx, cy) = curPos
            val startX = px + rooms[i-1].second.first / 2
            val startY = py + rooms[i-1].second.second / 2
            val endX = cx + rooms[i].second.first / 2
            val endY = cy + rooms[i].second.second / 2

            var x = startX
            var y = startY
            while (x != endX) {
                if (x in 0 until width && y in 0 until height) tiles[x][y] = CaveTile.FLOOR
                x += if (endX > x) 1 else -1
            }
            while (y != endY) {
                if (x in 0 until width && y in 0 until height) tiles[x][y] = CaveTile.FLOOR
                y += if (endY > y) 1 else -1
            }
        }

        // Выход в углу
        val corners = listOf(
            Pair(1, 1),
            Pair(1, height - 2),
            Pair(width - 2, 1),
            Pair(width - 2, height - 2)
        )
        val exitCorner = corners.random(random)
        exitX = exitCorner.first
        exitY = exitCorner.second
        tiles[exitX][exitY] = CaveTile.EXIT

        // Точка входа игрока – центр первой комнаты
        val firstRoom = rooms.first()
        val (rx, ry) = firstRoom.first
        val (rw, rh) = firstRoom.second
        playerStartX = rx + rw / 2
        playerStartY = ry + rh / 2
        tiles[playerStartX][playerStartY] = CaveTile.FLOOR
    }
}
