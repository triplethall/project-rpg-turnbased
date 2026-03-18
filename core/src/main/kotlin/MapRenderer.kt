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
val visibilityManager = VisibilityManager(gameMap)
    fun render(batch: SpriteBatch, player: Player) {
        visibilityManager.updateVisibility(Pair(player.x, player.y))

        for (x in 0 until gameMap.width) {
            for (y in 0 until gameMap.height) {
                val posX = (x * (cellSize + cellGap))
                val posY = (y * (cellSize + cellGap))

                if (!gameMap.isExplored(x, y)) {
                    batch.color = Color.DARK_GRAY // Незнакомые клетки остаются тёмными
                    batch.draw(pixelTexture, posX, posY, cellSize, cellSize)
                    continue
                }

                batch.color = when (gameMap.getTerrain(x, y)) {
                    TerrainType.WATER -> Color.TEAL     // Вода (фон)
                    TerrainType.LAND -> Color.GREEN      // Земля
                    TerrainType.MOUNTAIN -> Color.BLACK  // Горы
                    TerrainType.CITY -> Color.BROWN     // Город
                    TerrainType.ENEMY -> Color.RED      // Враг
                    TerrainType.TRAP -> Color.GRAY      // Ловушки
                    TerrainType.UPGRADE -> Color.ORANGE // Улучшения
                    TerrainType.OUTPOST -> Color.CORAL  // Аванпосты
                }

                batch.draw(pixelTexture, posX, posY, cellSize, cellSize)
            }
        }
        batch.setColor(Color.WHITE)
    }

}
