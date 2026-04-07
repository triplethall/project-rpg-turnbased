package ru.triplethall.rpgturnbased

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
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
    private val mapSeed = (Math.random() * Int.MAX_VALUE).toInt()
    private val cloud1Texture: Texture

    //фон
    private val waterTextures = arrayOfNulls<Texture>(4)
    private var waterFrameIndex = 0
    private var lastFrameTime = 0f
    private val frameDuration = 0.5f //частота смены кадров фона
    private val bgTileSize = 1024f
    private val cellSize = cellSize.toFloat()
    private val cellGap = cellGap.toFloat()

    private val jitterRadius = 1f
    private lateinit var grassSpoilers: Array<TextureRegion>


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

        sandTexture = Texture("map_layers/sand_back_tile.png")
        cloud1Texture = Texture("map_layers/clouds1.png")

        grassSpoilers = Array(10) { i ->
            TextureRegion(Texture("map_layers/grass_spoilers/light_grass$i.png"))
        }
    }

    fun dispose() {
        pixelTexture.dispose()
        for (t in waterTextures) {
            t?.dispose()
        }
        sandTexture.dispose()
        dirtTexture.dispose()
        for (region in grassSpoilers) {
            region.texture.dispose()
        }
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
        val jitterX = (Math.sin(x * 0.7 + time/2 * 12) + Math.cos(y * 0.4 + time/2 * 9)) * jitterRadius * 0.35f
        val jitterY = (Math.cos(x * 0.4 + time/2 * 10) + Math.sin(y * 0.7 + time/2 * 7)) * jitterRadius * 0.35f
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



        // === PASS 3: СЕТКА / ОТСТУПЫ (рисуются поверх всех тайлов) ===
        val gapColor = Color(0.15f, 0.35f, 0.15f, 1f)
        val inset = 1f
        val lineWider = cellGap + 2*inset

        for (x in 0 until gameMap.width) {
            for (y in 0 until gameMap.height) {
                val light = calculateLight(x, y, player)
                val terrain = gameMap.getTerrain(x, y)
                if (terrain == TerrainType.WATER) continue

                val posX = x * (cellSize + cellGap)
                val posY = y * (cellSize + cellGap)

                // 1. Рисуем базовую зелёную подложку (как было)

                batch.color = gapColor.cpy().mul(light, light, light, 1f)
                batch.draw(pixelTexture, posX - cellGap - inset, posY - cellGap, lineWider, cellSize + 2*cellGap) // left
                batch.draw(pixelTexture, posX + cellSize - inset, posY - cellGap, lineWider, cellSize + 2*cellGap)  // right
                batch.draw(pixelTexture, posX - cellGap - inset, posY - cellGap - inset, cellSize + 2*cellGap + 2*inset, lineWider) // bottom
                batch.draw(pixelTexture, posX - cellGap - inset, posY + cellSize - inset, cellSize + 2*cellGap + 2*inset, lineWider) // top

                // 2. Рисуем траву-оверлей поверх (случайная текстура + зеркало)
                batch.color = Color.WHITE.cpy().mul(light, light, light, 1f)
                val addition = 3f
                // Левая сторона (side = 0)
                run {
                    val side = 0
                    val texIndex = (seededRandom(x, y, side) * grassSpoilers.size).toInt()
                    val mirror = seededRandom(x, y, side + 100) < 0.5f
                    drawGrassSpoiler(batch, grassSpoilers[texIndex], posX + cellSize/2 - addition * 1f, posY + cellSize/2 - addition * 1.5f, cellSize+addition*2, cellGap+addition, side = 0, mirror = mirror)
                }
                // Правая сторона (side = 1)
                run {
                    val side = 1
                    val texIndex = (seededRandom(x, y, side) * grassSpoilers.size).toInt()
                    val mirror = seededRandom(x, y, side + 100) < 0.5f
                    drawGrassSpoiler(batch, grassSpoilers[texIndex], posX + cellSize/2 + addition * 1f, posY - cellSize/2- addition * 1.5f, cellSize+addition*2, cellGap+addition, side = 1, mirror = mirror)
                }
                // Нижняя сторона (side = 2)
                run {
                    val side = 2
                    val texIndex = (seededRandom(x, y, side) * grassSpoilers.size).toInt()
                    val mirror = seededRandom(x, y, side + 100) < 0.5f
                    drawGrassSpoiler(batch, grassSpoilers[texIndex], posX - cellGap, posY + cellSize - cellGap, cellSize+addition*2, cellGap+addition, side = 2, mirror = mirror)
                }
                // Верхняя сторона (side = 3)
                run {
                    val side = 3
                    val texIndex = (seededRandom(x, y, side) * grassSpoilers.size).toInt()
                    val mirror = seededRandom(x, y, side + 100) < 0.5f
                    drawGrassSpoiler(batch, grassSpoilers[texIndex], posX - cellGap, posY + cellGap, cellSize+addition*2, cellGap+addition, side = 3, mirror = mirror)
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
                        batch.draw(tex, posX, posY, cellSize - 4f, cellSize - 4f)
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
    // side: 0=left, 1=right, 2=bottom, 3=top
    private fun drawGrassSpoiler(
        batch: SpriteBatch,
        region: TextureRegion,
        posX: Float, posY: Float,
        cellSize: Float, cellGap: Float,
        side: Int,
        mirror: Boolean
    ) {
        val rotation = when (side) {
            0 -> 90f    // left
            1 -> -90f   // right
            2 -> 180f   // bottom
            else -> 0f  // top
        }

        val scaleX = if (mirror) -1f else 1f
        val scaleY = 1f

        when (side) {
            0, 1 -> { // Вертикальные стороны (левая/правая)
                // Позиция: центр зазора по X, начало тайла по Y
                // Origin: центр полоски для вращения
                // Size: ширина зазора, высота тайла
                batch.draw(region,
                    posX - cellGap/2, posY,           // x, y
                    cellGap/2, cellSize/2,            // originX, originY
                    cellSize, cellGap,                // width, height ✅
                    scaleX, scaleY,                   // scale
                    rotation
                )
            }
            2, 3 -> { // Горизонтальные стороны (низ/верх)
                batch.draw(region,
                    posX, posY - cellGap/2,           // x, y
                    cellSize/2, cellGap/2,            // originX, originY
                    cellSize, cellGap,                // width, height ✅
                    scaleX, scaleY,                   // scale
                    rotation
                )
            }
        }
    }
    private fun seededRandom(x: Int, y: Int, seed: Int = 0): Float {
        // Сумма с разными простыми множителями: каждый параметр сильно влияет на итог
        val h = x * 73856093 + y * 19349663 + seed * 374761393 + mapSeed * 668265263
        return (h * 2.3283064e-10f).coerceIn(0f, 1f)
    }
}
