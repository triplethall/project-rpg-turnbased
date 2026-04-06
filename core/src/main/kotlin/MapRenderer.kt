package ru.triplethall.rpgturnbased

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.actions.Actions.color
import kotlin.math.pow
import kotlin.math.sqrt



class MapRenderer (
    private val gameMap: GameMap,
    cellSize: Int = 32,
    cellGap: Int = 4,
    private val chestClosed: Texture,
    private val chestOpen: Texture
){
    private val pixelTexture: Texture
    private val sandTexture: Texture
    private val dirtTexture: Texture
    private val grassTexture: Texture
    private val cloud1Texture: Texture

    //фон
    private val waterTextures = arrayOfNulls<Texture>(4)
    private var waterFrameIndex = 0
    private var lastFrameTime = 0f
    private val frameDuration = 0.5f //частота смены кадров фона
    private val bgTileSize = 1024f
    private val cellSize = cellSize.toFloat()
    private val cellGap = cellGap.toFloat()
    private var jitterOffset = Pair(0f, 0f)
    private var lastJitterTime = 0f
    private val jitterInterval = 0.25f // смена направления 4 раза в секунду
    private val jitterRadius = 1f


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

        dirtTexture = Texture("map_layers/dirt.png")
        grassTexture = Texture("map_layers/light_grass_tile.png")
        sandTexture = Texture("map_layers/sand_back_tile.png")
        cloud1Texture = Texture("map_layers/clouds1.png")
    }

    fun dispose() {
        pixelTexture.dispose()
        for (t in waterTextures) {
            t?.dispose()
        }
        sandTexture.dispose()
        dirtTexture.dispose()
        grassTexture.dispose()
        cloud1Texture.dispose()
    }

    fun update(delta: Float) {
        lastFrameTime += delta
        if (lastFrameTime >= frameDuration) {
            lastFrameTime -= frameDuration
            waterFrameIndex = (waterFrameIndex + 1) % 4
        }
    }




    val visibilityManager = VisibilityManager(gameMap)

    private fun getTileJitter(x: Int, y: Int, time: Float): Pair<Float, Float> {
        val jitterX = (Math.sin(x * 0.7 + time * 12) + Math.cos(y * 0.4 + time * 9)) * jitterRadius * 0.35f
        val jitterY = (Math.cos(x * 0.4 + time * 10) + Math.sin(y * 0.7 + time * 7)) * jitterRadius * 0.35f
        return Pair(jitterX.toFloat(), jitterY.toFloat())
    }

    fun render(batch: SpriteBatch, player: Player) {
        //фон
        val currentWaterTex = waterTextures[waterFrameIndex] ?: return
        batch.color = Color.WHITE

        val mapWidthPx = gameMap.width * (cellSize + cellGap)
        val mapHeightPx = gameMap.height * (cellSize + cellGap)

        val cols = (mapWidthPx / bgTileSize).toInt() + 2
        val rows = (mapHeightPx / bgTileSize).toInt() + 2

        for (tx in -3 until cols) {
            for (ty in -3 until rows) {
                val pX = tx * bgTileSize
                val pY = ty * bgTileSize
                batch.draw(currentWaterTex, pX, pY, bgTileSize, bgTileSize)
            }
        }

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
                val fullLightRadius = 4.0f  // До 4-й клетки всё горит ярко
                val maxVisibleRadius = 8.0f // На 8-й клетке наступает тьма
                val light = when {
                    distance <= fullLightRadius -> 1.0f // В центре яркость максимальна
                    distance >= maxVisibleRadius -> 0.4f // Дальше лимита — минимальная яркость
                    else -> {
                        // Плавно уменьшаем от 1.0 до 0.2 в промежутке между 4 и 8 клетками
                        val ratio = (distance - fullLightRadius) / (maxVisibleRadius - fullLightRadius)
                        1.0f - (ratio * (1.0f - 0.4f))
                    }
                }

                val terrain = gameMap.getTerrain(x, y)



                if (terrain == TerrainType.Chest || terrain == TerrainType.OpenedChest) {
                    // сначала рисуем подложку земли (зеленый квадрат)
                    batch.color = Color.GREEN.cpy().mul(light, light, light, 1f) // Применяем то же освещение
                    batch.draw(pixelTexture, posX, posY, cellSize, cellSize)

                    // рисуем саму текстуру сундука
                    val tex = if (terrain == TerrainType.Chest) chestClosed else chestOpen
                    batch.color = Color(light, light, light, 1f)
                    batch.draw(tex, posX - 4f, posY - 2f, cellSize + 8f, cellSize + 8f)

                    continue
                }


                batch.color = Color.WHITE
                if (gameMap.isExplored(x, y) && terrain != TerrainType.WATER) {
                    val c = batch.color
                    batch.setColor(c.r * light, c.g * light, c.b * light, 1f)
                    batch.draw(
                        grassTexture,
                        posX - 8f,
                        posY - 8f,
                        cellSize + cellGap * 4,
                        cellSize + cellGap * 4
                    )
                    if (terrain == TerrainType.LAND) {
                        //batch.draw(sandTexture, posX - 2f, posY - 2f, cellSize + cellGap*3, cellSize + cellGap*3)



                        batch.draw(dirtTexture, posX, posY, cellSize, cellSize)

                    }
                }


                // Цвет тайла + освещение
                batch.color = when (terrain) {
                    TerrainType.WATER -> continue
                    TerrainType.LAND -> Color.GREEN
                    TerrainType.MOUNTAIN -> Color.BLACK
                    TerrainType.CITY -> Color.BROWN
                    TerrainType.ENEMY -> Color.RED
                    TerrainType.TRAP -> Color.GRAY
                    TerrainType.UPGRADE -> Color.ORANGE
                    TerrainType.OUTPOST -> Color.CORAL
                    else -> Color.WHITE
                }
                val c = batch.color
                batch.setColor(c.r * light, c.g * light, c.b * light, 1f)

                if (terrain != TerrainType.LAND) {
                    batch.draw(pixelTexture, posX, posY, cellSize, cellSize)
                }
                if (!gameMap.isExplored(x, y) && terrain != TerrainType.WATER) {
                    batch.color = Color.LIGHT_GRAY
                    val (jX, jY) = getTileJitter(x, y, lastFrameTime)
                    batch.draw(cloud1Texture, posX - 10f + jX, posY - 10f + jY, cellSize + cellGap*5, cellSize + cellGap*5)

                    continue
                }
            }
        }
        batch.setColor(Color.WHITE)
    }

}
