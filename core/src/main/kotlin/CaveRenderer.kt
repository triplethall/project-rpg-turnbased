package ru.triplethall.rpgturnbased

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch

class CaveRenderer(
    private val caveMap: CaveMap,
    private val cellSize: Float = 32f,
    private val cellGap: Float = 4f,
    private val whitePixel: Texture
) {
    fun render(batch: SpriteBatch) {
        val width = caveMap.width
        val height = caveMap.height

        for (x in 0 until width) {
            for (y in 0 until height) {
                val tile = caveMap.getTile(x, y)
                val posX = x * (cellSize + cellGap)
                val posY = y * (cellSize + cellGap)

                when (tile) {
                    CaveTile.FLOOR -> {
                        batch.color = Color.WHITE
                        batch.draw(whitePixel, posX, posY, cellSize, cellSize)
                    }
                    CaveTile.WALL -> {
                        batch.color = Color.DARK_GRAY
                        batch.draw(whitePixel, posX, posY, cellSize, cellSize)
                    }
                    CaveTile.EXIT -> {
                        batch.color = Color.GREEN
                        batch.draw(whitePixel, posX, posY, cellSize, cellSize)
                    }
                }
            }
        }
        // Рисуем зазоры
        batch.color = Color.WHITE
    }
}
