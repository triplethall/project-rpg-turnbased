package ru.triplethall.rpgturnbased

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch

class MapRenderer (
    private val gameMap: GameMap,
    cellSize: Int = 32,
    cellGap: Int = 4
){
    private val pixelTexture: Texture
    private val cellSize = cellSize.toFloat()
    private val cellGap = cellGap.toFloat()



    init{
        pixelTexture = Texture(1,1, Pixmap.Format.RGBA8888)
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.WHITE)
        pixmap.fill()
        pixelTexture.draw(pixmap, 0, 0)
        pixmap.dispose()
    }

    fun dispose() {
        pixelTexture.dispose()
    }


    fun render(batch: SpriteBatch) {
        for (x in 0 until gameMap.width) {
            for (y in 0 until gameMap.height) {
                batch.color = when (gameMap.getTerrain(x, y)) {
                    TerrainType.WATER -> continue     // Вода (фон)
                    TerrainType.LAND -> Color.GREEN      // Земля
                    TerrainType.MOUNTAIN -> Color.BLACK  // Горы
                    TerrainType.CITY -> Color.BROWN     // Город
                    TerrainType.ENEMY -> Color.RED      // Враг
                }

                val posX = (x * (cellSize + cellGap))
                val posY = (y * (cellSize + cellGap))

                batch.draw(pixelTexture, posX, posY, cellSize, cellSize)
            }
        }
        batch.setColor(Color.WHITE)
    }

}
