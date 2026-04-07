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

    private fun isChestInForest(x: Int, y: Int): Boolean {
        // Проверяем, что сундук окружён лесом со всех 4 сторон
        val directions = listOf(
            Pair(0, 1), Pair(0, -1), Pair(1, 0), Pair(-1, 0)
        )
        for ((dx, dy) in directions) {
            val nx = x + dx
            val ny = y + dy
            if (nx in 0 until gameMap.width && ny in 0 until gameMap.height) {
                if (gameMap.getTerrain(nx, ny) != TerrainType.FOREST) {
                    return false
                }
            } else {
                return false
            }
        }
        return true
    }


    val visibilityManager = VisibilityManager(gameMap)
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

                if (!gameMap.isExplored(x, y) && terrain != TerrainType.WATER) {
                    batch.color = Color.DARK_GRAY // Незнакомые клетки остаются тёмными

                    batch.draw(pixelTexture, posX, posY, cellSize, cellSize)
                    continue
                }

                if (terrain == TerrainType.Chest || terrain == TerrainType.OpenedChest) {
                    // Проверяем, нужно ли скрыть сундук в лесу
                    val shouldHideInForest = (terrain == TerrainType.Chest && isChestInForest(x, y))

                    if (shouldHideInForest) {
                        // Скрытый сундук в лесу - рисуем просто лес (тёмно-зелёный)
                        batch.color = Color(0.2f, 0.5f, 0.1f, 1f).mul(light, light, light, 1f)
                        batch.draw(pixelTexture, posX, posY, cellSize, cellSize)
                    } else {
                        // Обычный сундук или открытый - рисуем подложку и текстуру
                        batch.color = Color.GREEN.cpy().mul(light, light, light, 1f)
                        batch.draw(pixelTexture, posX, posY, cellSize, cellSize)

                        val tex = if (terrain == TerrainType.Chest) chestClosed else chestOpen
                        batch.color = Color(light, light, light, 1f)
                        batch.draw(tex, posX - 4f, posY - 2f, cellSize + 8f, cellSize + 8f)
                    }

                    continue
                }


                batch.color = when (terrain) {
                    TerrainType.WATER -> continue     // Вода (фон)
                    TerrainType.LAND -> Color.GREEN      // Земля
                    TerrainType.MOUNTAIN -> Color.BLACK  // Горы
                    TerrainType.CITY -> Color.BROWN     // Город
                    TerrainType.ENEMY -> Color.RED      // Враг
                    TerrainType.TRAP -> Color.GRAY      // Ловушки
                    TerrainType.UPGRADE -> Color.ORANGE // Улучшения
                    TerrainType.OUTPOST -> Color.CORAL  // Аванпосты
                    TerrainType.FOREST -> Color.FOREST   // Лес
                    else -> Color.WHITE
                }

                val c = batch.color
                batch.setColor(c.r * light, c.g * light, c.b * light, 1f)

                batch.draw(pixelTexture, posX, posY, cellSize, cellSize)
            }
        }
        batch.setColor(Color.WHITE)
    }

}
