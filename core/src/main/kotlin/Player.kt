package ru.triplethall.rpgturnbased

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import kotlin.random.Random

class Player(
    var x: Int = 0,
    var y: Int = 0
) {
    fun spawnOnShore(gameMap: GameMap) {
        val random = Random
        val candidates = mutableListOf<Pair<Int, Int>>()

        for (x in 0 until gameMap.width) {
            for (y in 0 until gameMap.height) {
                if (!gameMap.isWalkable(x, y)) continue

                var hasWaterNeighbor = false
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        if (dx == 0 && dy == 0) continue
                        val nx = x + dx
                        val ny = y + dy
                        if (nx !in 0 until gameMap.width || ny !in 0 until gameMap.height) {
                            hasWaterNeighbor = true
                        } else if (!gameMap.isWalkable(nx, ny)) {
                            hasWaterNeighbor = true
                        }
                    }
                }

                if (!hasWaterNeighbor) continue

                val isOnEdge = (x == 0 || x == gameMap.width - 1 || y == 0 || y == gameMap.height - 1)
                if (isOnEdge) {
                    candidates.add(Pair(x, y))
                }
            }
        }

        if (candidates.isNotEmpty()) {
            val (sx, sy) = candidates.random(random)
            this.x = sx
            this.y = sy
        } else {
            this.x = gameMap.width / 2
            this.y = gameMap.height / 2
        }
    }

    fun isAdjacentCardinal(targetX: Int, targetY: Int): Boolean {
        val dx = kotlin.math.abs(targetX - x)
        val dy = kotlin.math.abs(targetY - y)
        return (dx == 1 && dy == 0) || (dx == 0 && dy == 1)
    }

    fun tryMoveTo(targetX: Int, targetY: Int, gameMap: GameMap): Boolean {
        if (!isAdjacentCardinal(targetX, targetY)) {
            return false
        }

        if (gameMap.isWalkable(targetX, targetY)) {
            x = targetX
            y = targetY
            // Проверяем, был ли на этой клетке сундук, и убираем его
            if (gameMap.collectChest(targetX, targetY)) {
                // Здесь можно добавить логику награды (например, увеличить счёт, выдать предмет)

            }
            return true
        }
        return false
    }

    fun render(batch: SpriteBatch, font: BitmapFont, cellSize: Int, cellGap: Int) {
        val posX = x * (cellSize + cellGap)
        val posY = y * (cellSize + cellGap)

        font.color = Color.YELLOW
        font.draw(batch, "P", posX + 10f, posY + cellSize - 5f)
    }
}
