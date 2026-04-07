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
        // === ПРЕ-РАСЧЁТЫ ===
        visibilityManager.updateVisibility(Pair(player.x, player.y))
        val mapWidthPx = gameMap.width * (cellSize + cellGap)
        val mapHeightPx = gameMap.height * (cellSize + cellGap)

        // === PASS 0: ФОН (вода) ===
        val currentWaterTex = waterTextures[waterFrameIndex] ?: return
        batch.color = Color.WHITE
        val cols = (mapWidthPx / bgTileSize).toInt() + 2
        val rows = (mapHeightPx / bgTileSize).toInt() + 2
        for (tx in -3 until cols) {
            for (ty in -3 until rows) {
                batch.draw(currentWaterTex, tx * bgTileSize, ty * bgTileSize, bgTileSize, bgTileSize)
            }
        }

        // === PASS 1: БАЗОВЫЙ РЕЛЬЕФ (земля/песок для всех тайлов) ===
        for (x in 0 until gameMap.width) {
            for (y in 0 until gameMap.height) {
                if (!gameMap.isExplored(x, y)) continue

                val terrain = gameMap.getTerrain(x, y)
                if (terrain == TerrainType.WATER) continue

                val posX = x * (cellSize + cellGap)
                val posY = y * (cellSize + cellGap)
                val light = calculateLight(x, y, player) // вынеси расчёт в отдельную функцию

                // Рисуем подложку
                when (terrain) {
                    TerrainType.LAND, TerrainType.OpenedChest, TerrainType.Chest -> {
                        batch.color = Color.WHITE.cpy().mul(light, light, light, 1f)
                        batch.draw(dirtTexture, posX, posY, cellSize, cellSize)
                    }

                    else -> {
                        batch.color = Color.WHITE.cpy().mul(light, light, light, 1f)
                        batch.draw(pixelTexture, posX, posY, cellSize, cellSize)
                    }
                }
            }
        }

        // === PASS 2: ДЕКОР И ОБЪЕКТЫ (горы, города, враги, сундуки) ===
        for (x in 0 until gameMap.width) {
            for (y in 0 until gameMap.height) {
                if (!gameMap.isExplored(x, y)) continue
                val terrain = gameMap.getTerrain(x, y)
                if (terrain == TerrainType.WATER || terrain == TerrainType.LAND) continue

                val posX = x * (cellSize + cellGap)
                val posY = y * (cellSize + cellGap)
                val light = calculateLight(x, y, player)

                when (terrain) {
                    TerrainType.Chest, TerrainType.OpenedChest -> {
                        val tex = if (terrain == TerrainType.Chest) chestClosed else chestOpen
                        batch.color = Color(light, light, light, 1f)
                        batch.draw(tex, posX - 4f, posY - 2f, cellSize + 8f, cellSize + 8f)
                    }
                    else -> {
                        val color = when (terrain) {
                            TerrainType.MOUNTAIN -> Color.BLACK
                            TerrainType.CITY -> Color.BROWN
                            TerrainType.ENEMY -> Color.RED
                            TerrainType.TRAP -> Color.GRAY
                            TerrainType.UPGRADE -> Color.ORANGE
                            TerrainType.OUTPOST -> Color.CORAL
                            else -> Color.WHITE
                        }
                        batch.color = color.cpy().mul(light, light, light, 1f)
                        batch.draw(pixelTexture, posX, posY, cellSize, cellSize)
                    }
                }
            }
        }

        // === PASS 3: СЕТКА / ОТСТУПЫ (рисуются поверх всех тайлов) ===
        val gapColor = Color(0.15f, 0.35f, 0.15f, 1f)
        val gapThickness = cellGap * 1.2f  // ~12px вместо 4px
        val inset = 2f                   // небольшой отступ от края тайла
        val insetModifier = 1f
        for (x in 0 until gameMap.width) {
            for (y in 0 until gameMap.height) {
                val terrain = gameMap.getTerrain(x, y)
                if (terrain == TerrainType.WATER) continue  // пропускаем только воду

                val posX = x * (cellSize + cellGap)
                val posY = y * (cellSize + cellGap)

                batch.color = gapColor

                // Левая граница
                batch.draw(pixelTexture, posX - inset, posY - gapThickness/2 + inset/2, gapThickness, cellSize - inset * insetModifier + gapThickness*1f)
                // Правая граница
                batch.draw(pixelTexture, posX + cellSize - gapThickness + inset, posY + inset, gapThickness, cellSize - inset * insetModifier)
                // Нижняя граница
                batch.draw(pixelTexture, posX + inset, posY - inset, cellSize - inset * insetModifier, gapThickness)
                // Верхняя граница
                batch.draw(pixelTexture, posX + inset, posY + cellSize - gapThickness + inset, cellSize - inset * insetModifier, gapThickness)
            }
        }

        // === PASS 4: ТУМАН ВОЙНЫ (неисследованное) ===
        for (x in 0 until gameMap.width) {
            for (y in 0 until gameMap.height) {
                if (gameMap.isExplored(x, y) || gameMap.getTerrain(x, y) == TerrainType.WATER) continue

                val posX = x * (cellSize + cellGap)
                val posY = y * (cellSize + cellGap)
                val (jX, jY) = getTileJitter(x, y, lastFrameTime)

                batch.color = Color.WHITE
                batch.draw(cloud1Texture, posX - 10f + jX, posY - 10f + jY, cellSize + cellGap * 5, cellSize + cellGap * 5)
            }
        }

        batch.color = Color.WHITE // сброс цвета
    }

    // Вынесенный расчёт освещения для чистоты кода
    private fun calculateLight(x: Int, y: Int, player: Player): Float {
        val dx = (x - player.x).toDouble()
        val dy = (y - player.y).toDouble()
        val distance = sqrt(dx * dx + dy * dy).toFloat()

        return when {
            distance <= 4.0f -> 1.0f
            distance >= 8.0f -> 0.4f
            else -> {
                val ratio = (distance - 4.0f) / 4.0f
                1.0f - (ratio * 0.6f)
            }
        }
    }


}
