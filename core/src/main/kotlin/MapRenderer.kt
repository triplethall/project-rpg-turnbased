package ru.triplethall.rpgturnbased

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.actions.Actions.color
import kotlin.math.pow
import kotlin.math.sqrt


const val MAX_RADIUS = 5.0f // Радиус видимости

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

                // 1. Считаем дистанцию до игрока
                val dx = (x - player.x).toDouble()
                val dy = (y - player.y).toDouble()
                val distance = sqrt(dx * dx + dy * dy).toFloat()

                // 2. Считаем яркость: 1.0 (рядом) -> 0.2 (на границе видимости)
                // Используем 0.2f как минимальную яркость для уже исследованных клеток
                val light = (1.0f - (distance / MAX_RADIUS)).coerceIn(0.2f, 1.0f)

                val terrain = gameMap.getTerrain(x, y)
                if (!gameMap.isExplored(x, y) && terrain != TerrainType.WATER) {
                    batch.color = Color.DARK_GRAY // Незнакомые клетки остаются тёмными

                    batch.draw(pixelTexture, posX, posY, cellSize, cellSize)
                    continue
                }

                batch.color = when (gameMap.getTerrain(x, y)) {
                    TerrainType.WATER -> Color.CYAN     // Вода (фон)
                    TerrainType.LAND -> Color.GREEN      // Земля
                    TerrainType.MOUNTAIN -> Color.BLACK  // Горы
                    TerrainType.CITY -> Color.BROWN     // Город
                    TerrainType.ENEMY -> Color.RED      // Враг
                    TerrainType.TRAP -> Color.GRAY      // Ловушки
                    TerrainType.UPGRADE -> Color.ORANGE // Улучшения
                    TerrainType.OUTPOST -> Color.CORAL  // Аванпосты
                }
                val c = batch.color
                batch.setColor(c.r * light, c.g * light, c.b * light, 1f)

                batch.draw(pixelTexture, posX, posY, cellSize, cellSize)
            }
        }
        batch.setColor(Color.WHITE)
    }

}
