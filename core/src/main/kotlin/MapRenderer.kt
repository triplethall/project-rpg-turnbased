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
    private lateinit var cloudTextures: Array<TextureRegion>
    private val waterTextures = arrayOfNulls<Texture>(4)
    private var waterFrameIndex = 0
    private val forestTexture: Texture
    private var lastFrameTime = 0f
    private val frameDuration = 0.5f //частота смены кадров фона
    private val bgTileSize = 1024f
    private val cellSize = cellSize.toFloat()
    private val cellGap = cellGap.toFloat()
    private val cloudIndices = Array(gameMap.width) {
        Array(gameMap.height) { (0..4).random() } // сразу генерируем 0-4
    }
    private val jitterRadius = 1f
    private val grassIndices = Array(gameMap.width) {
        Array(gameMap.height) { IntArray(4) { 0 } }
    }
    private val grassMirrors = Array(gameMap.width) {
        Array(gameMap.height) { BooleanArray(4) { false } }
    }
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
        forestTexture = Texture("map_layers/forest.png")
        sandTexture = Texture("map_layers/sand_back_tile.png")
        cloudTextures = Array(5) { i ->
            TextureRegion(Texture("map_layers/clouds/clouds$i.png"))
        }
        grassSpoilers = Array(10) { i ->
            TextureRegion(Texture("map_layers/grass_spoilers/light_grass$i.png"))
        }
        generateGrassVariations()
    }

    fun dispose() {
        pixelTexture.dispose()
        for (t in waterTextures) {
            t?.dispose()
        }
        forestTexture.dispose()
        sandTexture.dispose()
        dirtTexture.dispose()
        for (region in grassSpoilers) {
            region.texture.dispose()
        }
        for (region in cloudTextures) region.texture.dispose()
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

    private fun getCloudJitter(x: Int, y: Int, time: Float): Pair<Float, Float> {
        val steppedTime = (time / 0.2f).toInt()
        val seed = x * 73856093 + y * 19349663 + steppedTime * 374761393
        val rng = java.util.Random(seed.toLong())

        return Pair(
            (rng.nextFloat() * 2 - 1) * jitterRadius,
            (rng.nextFloat() * 2 - 1) * jitterRadius
        )
    }

    fun render(batch: SpriteBatch, player: Player) {

        visibilityManager.updateVisibility(Pair(player.x, player.y))
        val mapWidthPx = gameMap.width * (cellSize + cellGap)
        val mapHeightPx = gameMap.height * (cellSize + cellGap)

        // 0 слой - вода
        val currentWaterTex = waterTextures[waterFrameIndex] ?: return
        batch.color = Color.WHITE
        val cols = (mapWidthPx / bgTileSize).toInt() + 2
        val rows = (mapHeightPx / bgTileSize).toInt() + 2
        for (tx in -3 until cols) {
            for (ty in -3 until rows) {
                batch.draw(currentWaterTex, tx * bgTileSize, ty * bgTileSize, bgTileSize, bgTileSize)
            }
        }

        // слой 1 - базовая подложка, земля
        for (x in 0 until gameMap.width) {
            for (y in 0 until gameMap.height) {
                if (!gameMap.isExplored(x, y)) continue

                val terrain = gameMap.getTerrain(x, y)
                if (terrain == TerrainType.WATER) continue

                val posX = x * (cellSize + cellGap)
                val posY = y * (cellSize + cellGap)
                val light = calculateLight(x, y, player)

                when (terrain) {
                    TerrainType.LAND, TerrainType.OpenedChest, TerrainType.Chest, TerrainType.FOREST -> {
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



        // слой 2 - сетка, ассеты травы и оставшийся декор
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

                // базовая зеленая сетка
                batch.color = gapColor.cpy().mul(light, light, light, 1f)
                batch.draw(pixelTexture, posX - cellGap - inset, posY - cellGap, lineWider, cellSize + 2*cellGap) // left
                batch.draw(pixelTexture, posX + cellSize - inset, posY - cellGap, lineWider, cellSize + 2*cellGap)  // right
                batch.draw(pixelTexture, posX - cellGap - inset, posY - cellGap - inset, cellSize + 2*cellGap + 2*inset, lineWider) // bottom
                batch.draw(pixelTexture, posX - cellGap - inset, posY + cellSize - inset, cellSize + 2*cellGap + 2*inset, lineWider) // top

                // текстуры травы в клетках
                batch.color = Color.WHITE.cpy().mul(light, light, light, 1f)
                val addition = 3f
                // левая сторона
                run {
                    val side = 0
                    val texIndex = grassIndices[x][y][side]

                    drawGrassSpoiler(batch, grassSpoilers[texIndex], posX + cellSize/2 - addition * 1f, posY + cellSize/2 - addition * 1.5f, cellSize+addition*2, cellGap+addition, side = 0, mirror = true)
                }
                // правая сторона
                run {
                    val side = 1
                    val texIndex = grassIndices[x][y][side]

                    drawGrassSpoiler(batch, grassSpoilers[texIndex], posX + cellSize/2 + addition * 1f, posY - cellSize/2- addition * 1.5f, cellSize+addition*2, cellGap+addition, side = 1, mirror = true)
                }
                // нижняя сторона
                run {
                    val side = 2
                    val texIndex = grassIndices[x][y][side]
                    val mirror = grassMirrors[x][y][side]
                    drawGrassSpoiler(batch, grassSpoilers[texIndex], posX - cellGap, posY + cellSize - cellGap, cellSize+addition*2, cellGap+addition, side = 2, mirror = mirror)
                }
                // верхняя сторона
                run {
                    val side = 3
                    val texIndex = grassIndices[x][y][side]
                    val mirror = grassMirrors[x][y][side]
                    drawGrassSpoiler(batch, grassSpoilers[texIndex], posX - cellGap, posY + cellGap, cellSize+addition*2, cellGap+addition, side = 3, mirror = mirror)
                }
            }
        }

        // слой 3 - объекты
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
                        val shouldHideInForest = (terrain == TerrainType.Chest && isChestInForest(x, y))
                        if (shouldHideInForest) {
                            // Скрытый сундук в лесу - рисуем просто лес (тёмно-зелёный)
                            batch.color = Color(0.2f, 0.5f, 0.1f, 1f).mul(light, light, light, 1f)
                            batch.draw(forestTexture, posX - cellSize*0.2f, posY, cellSize*1.4f, cellSize*1.4f)
                        } else {

                            val tex = if (terrain == TerrainType.Chest) chestClosed else chestOpen
                            batch.color = Color(light, light, light, 1f)
                            batch.draw(tex, posX+3f, posY+3f, cellSize - 4f, cellSize - 4f)
                        }
                    }

                    TerrainType.FOREST -> {
                        batch.color = Color(0.2f, 0.5f, 0.1f, 1f).mul(light, light, light, 1f)
                        batch.draw(forestTexture, posX - cellSize*0.2f, posY, cellSize*1.4f, cellSize*1.4f)
                    }
                    else -> {
                        val color = when (terrain) {
                            TerrainType.MOUNTAIN -> Color.BLACK
                            TerrainType.CITY -> Color.BROWN
                            TerrainType.ENEMY -> Color.RED
                            TerrainType.TRAP -> Color.GRAY
                            TerrainType.UPGRADE -> Color.ORANGE
                            TerrainType.OUTPOST -> Color.CORAL
                            TerrainType.FOREST -> continue
                            else -> Color.WHITE
                        }
                        batch.color = color.cpy().mul(light, light, light, 1f)
                        batch.draw(pixelTexture, posX, posY, cellSize, cellSize)
                    }
                }
            }
        }

        // слой 4 - туман войны
        for (x in 0 until gameMap.width) {
            for (y in 0 until gameMap.height) {
                if (gameMap.isExplored(x, y) || gameMap.getTerrain(x, y) == TerrainType.WATER) continue

                val posX = x * (cellSize + cellGap)
                val posY = y * (cellSize + cellGap)


                batch.color = Color.WHITE
                val cloudIdx = cloudIndices[x][y]
                val (jX, jY) = getCloudJitter(x, y, lastFrameTime)
                batch.draw(cloudTextures[cloudIdx], posX - 10f + jX, posY - 10f + jY, cellSize + cellGap * 5, cellSize + cellGap * 5)
            }
        }

        batch.color = Color.WHITE
    }


    fun calculateLight(x: Int, y: Int, player: Player): Float {
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

    fun drawGrassSpoiler(
        batch: SpriteBatch,
        region: TextureRegion,
        posX: Float, posY: Float,
        cellSize: Float, cellGap: Float,
        side: Int,
        mirror: Boolean
    ) {
        val rotation = when (side) {
            0 -> 90f
            1 -> -90f
            2 -> 180f
            else -> 0f
        }

        val scaleX = if (mirror) -1f else 1f
        val scaleY = 1f

        when (side) {
            0, 1 -> {
                batch.draw(region,
                    posX - cellGap/2, posY,
                    cellGap/2, cellSize/2,
                    cellSize, cellGap,
                    scaleX, scaleY,
                    rotation
                )
            }
            2, 3 -> {
                batch.draw(region,
                    posX, posY - cellGap/2,
                    cellSize/2, cellGap/2,
                    cellSize, cellGap,
                    scaleX, scaleY,
                    rotation
                )
            }
        }
    }

    fun generateGrassVariations() {
        for (x in 0 until gameMap.width) {
            for (y in 0 until gameMap.height) {
                for (side in 0..3) {
                    grassIndices[x][y][side] = (0..9).random()
                    grassMirrors[x][y][side] = (0..1).random() == 1
                }
            }
        }
    }
}
