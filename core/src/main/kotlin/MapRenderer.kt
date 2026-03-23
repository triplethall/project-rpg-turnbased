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

    //фон
    private val waterTextures = arrayOfNulls<Texture>(4)
    private var waterFrameIndex = 0
    private var lastFrameTime = 0f
    private val frameDuration = 0.5f //частота смены кадров фона
    private val bgTileSize = 1024f
    private val cellSize = cellSize.toFloat()
    private val cellGap = cellGap.toFloat()



    init{
        pixelTexture = Texture(1,1, Pixmap.Format.RGBA8888)
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.WHITE)
        pixmap.fill()
        pixelTexture.draw(pixmap, 0, 0)
        pixmap.dispose()

        waterTextures[0] = Texture("bg/water_01_01.png")
        waterTextures[1] = Texture("bg/water_01_02.png")
        waterTextures[2] = Texture("bg/water_01_03.png")
        waterTextures[3] = Texture("bg/water_01_04.png")
    }

    fun dispose() {
        pixelTexture.dispose()
        for (t in waterTextures) {
            t?.dispose()
        }
    }

    fun update(delta: Float) {
        lastFrameTime += delta
        if (lastFrameTime >= frameDuration) {
            lastFrameTime -= frameDuration
            waterFrameIndex = (waterFrameIndex + 1) % 4
        }
    }




val visibilityManager = VisibilityManager(gameMap)
    fun render(batch: SpriteBatch, player: Player) {

        val currentWaterTex = waterTextures[waterFrameIndex] ?: return
        batch.color = Color.WHITE

        val mapWidthPx = gameMap.width * (cellSize + cellGap)
        val mapHeightPx = gameMap.height * (cellSize + cellGap)

        val cols = (mapWidthPx / bgTileSize).toInt() + 2
        val rows = (mapHeightPx / bgTileSize).toInt() + 2

        for (x in -3 until cols) {
            for (y in -3 until rows) {
                val posX = x * bgTileSize
                val posY = y * bgTileSize
                batch.draw(currentWaterTex, posX, posY, bgTileSize, bgTileSize)
            }
        }



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
                    TerrainType.WATER -> continue     // Вода (фон)
                    TerrainType.LAND -> Color.GREEN      // Земля
                    TerrainType.MOUNTAIN -> Color.BLACK  // Горы
                    TerrainType.Chest -> Color.GOLD // сундук
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
