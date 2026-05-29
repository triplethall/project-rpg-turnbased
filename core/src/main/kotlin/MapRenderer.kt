package ru.triplethall.rpgturnbased

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import kotlin.math.sqrt


class MapRenderer (
    private val gameMap: GameMap,
    cellSize: Int = 32,
    cellGap: Int = 4,
    private val chestClosed: Texture,
    private val chestOpen: Texture,
    private val font: BitmapFont
){
    private val pixelTexture: Texture
    private lateinit var beachTextures: Array<TextureRegion>
    private val beachW = 32f
    private val beachH = 8f
    private val city1Texture: Texture
    private val upgradeTexture: Texture
    private val dirtTexture: Texture
    private val mtnTexture: Texture
    private val caveTexture: Texture
    private lateinit var beachCorner: TextureRegion
    private val cornerW = 32f
    private val cornerH = 32f

    private lateinit var cloudTextures: Array<TextureRegion>
    private val waterTextures = arrayOfNulls<Texture>(4)
    private var waterFrameIndex = 0
    private val forestTexture: Texture
    private var lastFrameTime = 0f
    private val frameDuration = 0.5f
    private val bgTileSize = 1024f

    private val lightCache = Array(gameMap.width) { FloatArray(gameMap.height) }
    private val distCache = Array(gameMap.width) { FloatArray(gameMap.height) }
    private lateinit var decoTextures: Array<TextureRegion>
    private data class DecoItem(val idx: Int, val mirror: Boolean, val w: Float, val h: Float, val offX: Float, val offY: Float)
    private val decoCache = Array(gameMap.width) {
        Array(gameMap.height) {
            Array(4) { mutableListOf<DecoItem>() }
        }
    }
    private val decoProbability = 0.12f
    private var lastPlayerX = -999
    private var lastPlayerY = -999
    private var cacheValid = false

    private val cellSize = cellSize.toFloat()
    private val cellGap = cellGap.toFloat()
    private val cloudIndices = Array(gameMap.width) {
        Array(gameMap.height) { (0..4).random() }
    }
    private val jitterRadius = 1f
    private val grassIndices = Array(gameMap.width) {
        Array(gameMap.height) { IntArray(4) { 0 } }
    }
    private val grassMirrors = Array(gameMap.width) {
        Array(gameMap.height) { BooleanArray(4) { false } }
    }
    private lateinit var grassSpoilers: Array<TextureRegion>

    private var showTeleportMenu = false
    private val menuWidth = 250f
    private val menuHeight = 150f

    init {
        pixelTexture = Texture(1, 1, Pixmap.Format.RGBA8888)
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.WHITE)
        pixmap.fill()
        pixelTexture.draw(pixmap, 0, 0)
        pixmap.dispose()
        mtnTexture = Texture("map_layers/rocks/rock1.png")
        beachCorner = TextureRegion(Texture("map_layers/beach/corner.png"))
        waterTextures[0] = Texture("bg/water_01_01.png")
        waterTextures[1] = Texture("bg/water_01_02.png")
        waterTextures[2] = Texture("bg/water_01_03.png")
        waterTextures[3] = Texture("bg/water_01_04.png")
        beachTextures = Array(7) { i ->
            TextureRegion(Texture("map_layers/beach/beach$i.png"))
        }
        upgradeTexture = Texture("upgradetile.png")
        dirtTexture = Texture("map_layers/dirt.png")
        city1Texture = Texture("towns/town1.png")
        forestTexture = Texture("map_layers/forest.png")
        cloudTextures = Array(5) { i ->
            TextureRegion(Texture("map_layers/clouds/clouds$i.png"))
        }
        decoTextures = Array(17) { i ->
            TextureRegion(Texture("map_decor/deco0-${i.toString().padStart(2, '0')}.png"))
        }
        generateDecoVariations()
        grassSpoilers = Array(10) { i ->
            TextureRegion(Texture("map_layers/grass_spoilers/light_grass$i.png"))
        }
        caveTexture = Texture("caves/cave0.png")
        generateGrassVariations()
    }

    fun dispose() {
        pixelTexture.dispose()
        for (t in waterTextures) {
            t?.dispose()
        }
        forestTexture.dispose()
        city1Texture.dispose()
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

    fun updateTeleport(playerX: Int, playerY: Int) {
        if (gameMap.getTerrain(playerX, playerY) == TerrainType.TELEPORT && !showTeleportMenu) {
            showTeleportMenu = true
            gameMap.setTerrain(playerX, playerY, TerrainType.LAND)
        }

        if (showTeleportMenu && Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            showTeleportMenu = false
        }
    }

    fun isTeleportMenuVisible(): Boolean = showTeleportMenu

    private fun isChestInForest(x: Int, y: Int): Boolean {
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
        rebuildLightCache(player)
        val mapWidthPx = gameMap.width * (cellSize + cellGap)
        val mapHeightPx = gameMap.height * (cellSize + cellGap)

        val currentWaterTex = waterTextures[waterFrameIndex] ?: return
        batch.color = Color.WHITE
        val cols = (mapWidthPx / bgTileSize).toInt() + 2
        val rows = (mapHeightPx / bgTileSize).toInt() + 2
        for (tx in -3 until cols) {
            for (ty in -3 until rows) {
                batch.draw(currentWaterTex, tx * bgTileSize, ty * bgTileSize, bgTileSize, bgTileSize)
            }
        }

        for (x in 0 until gameMap.width) {
            for (y in 0 until gameMap.height) {
                if (!gameMap.isExplored(x, y)) continue

                val terrain = gameMap.getTerrain(x, y)
                if (terrain == TerrainType.WATER) continue

                val posX = x * (cellSize + cellGap)
                val posY = y * (cellSize + cellGap)
                val light = if (cacheValid) lightCache[x][y] else 1.0f
                renderBeaches(batch, x, y, posX, posY, light)
                renderBeachCorners(batch, x, y, posX, posY, light)

                when (terrain) {
                    TerrainType.LAND, TerrainType.UPGRADE, TerrainType.CITY,
                    TerrainType.CITYANCHOR, TerrainType.MOUNTAIN, TerrainType.OPENEDCHEST,
                    TerrainType.CHEST, TerrainType.FOREST, TerrainType.ENEMY, TerrainType.TELEPORT -> {
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

        val gapColor = Color(0.15f, 0.35f, 0.15f, 1f)
        val inset = 1f
        val lineWider = cellGap + 2 * inset

        for (x in 0 until gameMap.width) {
            for (y in 0 until gameMap.height) {
                val light = if (cacheValid) lightCache[x][y] else 1.0f
                val terrain = gameMap.getTerrain(x, y)
                if (terrain == TerrainType.WATER) continue

                val posX = x * (cellSize + cellGap)
                val posY = y * (cellSize + cellGap)

                batch.color = gapColor.cpy().mul(light, light, light, 1f)
                batch.draw(pixelTexture, posX - cellGap - inset, posY - cellGap, lineWider, cellSize + 2 * cellGap)
                batch.draw(pixelTexture, posX + cellSize - inset, posY - cellGap, lineWider, cellSize + 2 * cellGap)
                batch.draw(pixelTexture, posX - cellGap - inset, posY - cellGap - inset, cellSize + 2 * cellGap + 2 * inset, lineWider)
                batch.draw(pixelTexture, posX - cellGap - inset, posY + cellSize - inset, cellSize + 2 * cellGap + 2 * inset, lineWider)
                renderGrassBorders(batch, x, y, posX, posY, light)

                batch.color = Color.WHITE.cpy().mul(light, light, light, 1f)
                val addition = 3f

                val side0TexIndex = grassIndices[x][y][0]
                drawGrassSpoiler(batch, grassSpoilers[side0TexIndex], posX + cellSize/2 - addition, posY + cellSize/2 - addition * 1.5f, cellSize + addition * 2, cellGap + addition, side = 0, mirror = true)

                val side1TexIndex = grassIndices[x][y][1]
                drawGrassSpoiler(batch, grassSpoilers[side1TexIndex], posX + cellSize/2 + addition, posY - cellSize/2 - addition * 1.5f, cellSize + addition * 2, cellGap + addition, side = 1, mirror = true)

                val side2TexIndex = grassIndices[x][y][2]
                val mirror2 = grassMirrors[x][y][2]
                drawGrassSpoiler(batch, grassSpoilers[side2TexIndex], posX - cellGap, posY + cellSize - cellGap, cellSize + addition * 2, cellGap + addition, side = 2, mirror = mirror2)

                val side3TexIndex = grassIndices[x][y][3]
                val mirror3 = grassMirrors[x][y][3]
                drawGrassSpoiler(batch, grassSpoilers[side3TexIndex], posX - cellGap, posY + cellGap, cellSize + addition * 2, cellGap + addition, side = 3, mirror = mirror3)
            }
        }

        for (x in 0 until gameMap.width) {
            for (y in 0 until gameMap.height) {
                if (gameMap.getTerrain(x, y) != TerrainType.WATER) {
                    val light = if (cacheValid) lightCache[x][y] else 1.0f
                    val posX = x * (cellSize + cellGap)
                    val posY = y * (cellSize + cellGap)
                    renderDecoBorders(batch, x, y, posX, posY, light)
                }
            }
        }

        for (x in 0 until gameMap.width) {
            for (y in gameMap.height - 1 downTo 0) {
                if (!gameMap.isExplored(x, y)) continue
                val distance = if (cacheValid) distCache[x][y] else 0f

                val terrain = gameMap.getTerrain(x, y)
                if (distance > 8 && terrain == TerrainType.ENEMY) continue
                val light = if (cacheValid) lightCache[x][y] else 1.0f

                when (terrain) {
                    TerrainType.WATER, TerrainType.LAND -> continue
                    else -> {
                        val posX = x * (cellSize + cellGap)
                        val posY = y * (cellSize + cellGap)

                        when (terrain) {
                            TerrainType.CHEST, TerrainType.OPENEDCHEST -> {
                                val shouldHideInForest = (terrain == TerrainType.CHEST && isChestInForest(x, y))
                                if (shouldHideInForest) {
                                    batch.color = Color(0.2f, 0.5f, 0.1f, 1f).mul(light, light, light, 1f)
                                    batch.draw(forestTexture, posX - cellSize * 0.2f, posY, cellSize * 1.4f, cellSize * 1.4f)
                                } else {
                                    val tex = if (terrain == TerrainType.CHEST) chestClosed else chestOpen
                                    batch.color = Color(light, light, light, 1f)
                                    batch.draw(tex, posX + 3f, posY + 3f, cellSize - 4f, cellSize - 4f)
                                }
                            }
                            TerrainType.TELEPORT -> {
                                batch.color = Color(0.8f, 0.2f, 0.8f, 1f).mul(light, light, light, 1f)
                                batch.draw(pixelTexture, posX, posY, cellSize, cellSize)
                                batch.color = Color.WHITE.cpy().mul(light, light, light, 1f)
                                font.draw(batch, "T", posX + cellSize/2 - 5f, posY + cellSize/2 + 5f)
                            }
                            TerrainType.UPGRADE -> {
                                batch.color = Color(0.5f, 0.5f, 0.5f, 1f).mul(light, light, light, 1f)
                                batch.draw(upgradeTexture, posX + 5f, posY + 5f, cellSize * 0.7f, cellSize * 0.7f)
                            }
                            TerrainType.CAVEENTRANCE -> {
                                batch.color = Color(1f, 1f, 1f, 1f).mul(light, light, light, 1f)
                                batch.draw(caveTexture, posX - cellSize * 0.2f, posY - cellSize * 0.2f, cellSize * 1.4f, cellSize * 1.4f)
                            }
                            TerrainType.CITYANCHOR -> {
                                batch.color = Color(0.5f, 0.5f, 0.5f, 1f).mul(light, light, light, 1f)
                                batch.draw(city1Texture, posX - cellSize * 0.3f, posY - 1f, cellSize * 2.65f, cellSize * 2.65f)
                            }
                            TerrainType.FOREST -> {
                                batch.color = Color(0.2f, 0.5f, 0.1f, 1f).mul(light, light, light, 1f)
                                batch.draw(forestTexture, posX - cellSize * 0.2f, posY, cellSize * 1.4f, cellSize * 1.4f)
                            }
                            TerrainType.MOUNTAIN -> {
                                batch.color = Color(1f, 1f, 1f, 1f).mul(light, light, light, 1f)
                                batch.draw(mtnTexture, posX - cellSize * 0.2f, posY - cellGap, cellSize * 1.4f, cellSize * 1.5f)
                            }
                            TerrainType.QUEST_GIVER -> {
                                batch.color = Color(0.3f, 0.5f, 1f, 1f).mul(light, light, light, 1f)
                                batch.draw(pixelTexture, posX, posY, cellSize, cellSize)
                                batch.color = Color.YELLOW
                                font.draw(batch, "!", posX + cellSize/2 - 5f, posY + cellSize - 10f)
                            }
                            TerrainType.ENEMY -> {
                                batch.color = Color.RED.cpy().mul(light, light, light, 1f)
                                batch.draw(pixelTexture, posX, posY, cellSize, cellSize)
                            }
                            TerrainType.TRAP -> {
                                batch.color = Color.GRAY.cpy().mul(light, light, light, 1f)
                                batch.draw(pixelTexture, posX, posY, cellSize, cellSize)
                            }
                            TerrainType.TRAP_TRIGGERED -> {
                                batch.color = Color.DARK_GRAY.cpy().mul(light, light, light, 1f)
                                batch.draw(pixelTexture, posX, posY, cellSize, cellSize)
                            }
                            TerrainType.OUTPOST -> {
                                batch.color = Color.CORAL.cpy().mul(light, light, light, 1f)
                                batch.draw(pixelTexture, posX, posY, cellSize, cellSize)
                            }
                            else -> {
                                batch.color = Color.WHITE.cpy().mul(light, light, light, 1f)
                                batch.draw(pixelTexture, posX, posY, cellSize, cellSize)
                            }
                        }
                    }
                }
            }
        }

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

        if (showTeleportMenu) {
            renderTeleportMenu(batch)
        }

        batch.color = Color.WHITE
    }

    private fun renderTeleportMenu(batch: SpriteBatch) {
        val menuX = (Gdx.graphics.width / 2f) - menuWidth / 2
        val menuY = (Gdx.graphics.height / 2f) - menuHeight / 2

        batch.end()

        val shapeRenderer = ShapeRenderer()
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0.1f, 0.1f, 0.1f, 0.95f)
        shapeRenderer.rect(menuX, menuY, menuWidth, menuHeight)
        shapeRenderer.color = Color(0.5f, 0.2f, 0.5f, 1f)
        shapeRenderer.rect(menuX + 5, menuY + 5, menuWidth - 10, menuHeight - 10)
        shapeRenderer.end()

        batch.begin()

        font.color = Color.WHITE
        font.draw(batch, "ТЕЛЕПОРТ", menuX + menuWidth/2 - 40f, menuY + menuHeight - 25f)
        font.draw(batch, "Вы нашли таинственный", menuX + 25f, menuY + menuHeight - 60f)
        font.draw(batch, "телепорт!", menuX + 65f, menuY + menuHeight - 85f)
        font.draw(batch, "Нажмите ESC", menuX + 70f, menuY + 30f)

        font.color = Color.YELLOW
        font.draw(batch, "[ESC]", menuX + 95f, menuY + 12f)
    }

    private fun drawGrassSpoiler(
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

    private fun generateGrassVariations() {
        for (x in 0 until gameMap.width) {
            for (y in 0 until gameMap.height) {
                for (side in 0..3) {
                    grassIndices[x][y][side] = (0..9).random()
                    grassMirrors[x][y][side] = (0..1).random() == 1
                }
            }
        }
    }

    private fun renderBeaches(batch: SpriteBatch, x: Int, y: Int, posX: Float, posY: Float, light: Float) {
        val neighbors = listOf(
            3 to Pair(0, 1),
            2 to Pair(0, -1),
            0 to Pair(-1, 0),
            1 to Pair(1, 0)
        )

        for ((side, offset) in neighbors) {
            val nx = x + offset.first
            val ny = y + offset.second

            if (nx in -1 until gameMap.width + 1 && ny in -1 until gameMap.height + 1 &&
                gameMap.getTerrain(nx, ny) == TerrainType.WATER) {

                val texIdx = kotlin.math.abs((x * 31 + y * 17) % 7)
                val tex = beachTextures[texIdx]

                batch.color = Color.WHITE.cpy().mul(light, light, light, 1f)

                when (side) {
                    3 -> {
                        batch.draw(tex, posX - cellGap - 1f, posY + cellSize + cellGap, beachW + 2 * cellGap + 2f, beachH + cellGap * 2)
                    }
                    2 -> {
                        batch.draw(tex, posX + 2 * cellGap - 3.1f,  posY - cellGap - cellSize/2 - 1f, 16f, 8f, beachW + 2 * cellGap + 2f, beachH + cellGap * 2, 1f, 1f, 180f)
                    }
                    0 -> {
                        batch.draw(tex, posX - 2 * cellGap + 3f, posY - cellGap - 1f, 0f, 0f, beachW + 2 * cellGap + 2f, beachH + cellGap * 2, 1f, 1f, 90f)
                    }
                    1 -> {
                        batch.draw(tex, posX + cellSize + 2 * cellGap + 4f, posY + cellSize/2 + 3 * cellGap + 1f, 0f, 8f, beachW + 2 * cellGap + 2f, beachH + cellGap * 2, 1f, 1f, -90f)
                    }
                }
            }
        }
    }

    private fun renderGrassBorders(batch: SpriteBatch, x: Int, y: Int, posX: Float, posY: Float, light: Float) {
        val neighbors = listOf(
            3 to Pair(0, 1),
            2 to Pair(0, -1),
            0 to Pair(-1, 0),
            1 to Pair(1, 0)
        )

        for ((side, offset) in neighbors) {
            val nx = x + offset.first
            val ny = y + offset.second

            if (nx in -1 until gameMap.width + 1 && ny in -1 until gameMap.height + 1 &&
                gameMap.getTerrain(nx, ny) == TerrainType.WATER) {

                val texIdx2 = kotlin.math.abs((x * 31 + y * 17) % 10)
                val tex2 = grassSpoilers[texIdx2]

                batch.color = Color.WHITE.cpy().mul(light, light, light, 1f)

                when (side) {
                    3 -> {
                        batch.draw(tex2, posX - cellGap - 1f, posY + cellSize + cellGap, beachW + 2 * cellGap + 2f, beachH)
                    }
                    2 -> {
                        batch.draw(tex2, posX + 2 * cellGap - 3.1f,  posY - cellGap - cellSize/2 - 1f, 16f, 8f, beachW + 2 * cellGap + 2f, beachH, 1f, 1f, 180f)
                    }
                    0 -> {
                        batch.draw(tex2, posX - 2 * cellGap + 3f, posY - cellGap - 1f, 0f, 0f, beachW + 2 * cellGap + 2f, beachH, 1f, 1f, 90f)
                    }
                    1 -> {
                        batch.draw(tex2, posX + cellSize + 2 * cellGap + 4f, posY + cellSize/2 + 3 * cellGap + 1f, 0f, 8f, beachW + 2 * cellGap + 2f, beachH, 1f, 1f, -90f)
                    }
                }
            }
        }
    }

    private fun rebuildLightCache(player: Player) {
        if (player.x == lastPlayerX && player.y == lastPlayerY) {
            cacheValid = true
            return
        }

        lastPlayerX = player.x
        lastPlayerY = player.y

        for (x in 0 until gameMap.width) {
            for (y in 0 until gameMap.height) {
                val dx = (x - player.x).toDouble()
                val dy = (y - player.y).toDouble()
                val dist = sqrt(dx * dx + dy * dy).toFloat()
                distCache[x][y] = dist

                lightCache[x][y] = when {
                    dist <= 4.0f -> 1.0f
                    dist >= 8.0f -> 0.4f
                    else -> {
                        val ratio = (dist - 4.0f) / 4.0f
                        1.0f - (ratio * 0.6f)
                    }
                }
            }
        }
        cacheValid = true
    }

    private fun renderBeachCorners(batch: SpriteBatch, x: Int, y: Int, posX: Float, posY: Float, light: Float) {
        val diagonals = listOf(
            Triple(1, 1, 0f),
            Triple(1, -1, -90f),
            Triple(-1, -1, 180f),
            Triple(-1, 1, 90f)
        )

        for ((dx, dy, rotation) in diagonals) {
            val nx = x + dx
            val ny = y + dy

            if (nx in -1 until gameMap.width + 1 && ny in -1 until gameMap.height + 1 &&
                gameMap.getTerrain(nx, ny) == TerrainType.WATER &&
                gameMap.getTerrain(x + dx, y) == TerrainType.WATER &&
                gameMap.getTerrain(x, y + dy) == TerrainType.WATER) {

                batch.color = Color.WHITE.cpy().mul(light, light, light, 1f)

                val cornerX = if (dx > 0) posX + cellSize else posX
                val cornerY = if (dy > 0) posY + cellSize else posY

                batch.draw(beachCorner, cornerX, cornerY, 0f, 0f, cornerW/1.5f, cornerH/1.5f, 1f, 1f, rotation)
            }
        }
    }

    private fun generateDecoVariations() {
        val rand = kotlin.random.Random
        for (x in 0 until gameMap.width) {
            for (y in gameMap.height - 1 downTo 0) {
                for (side in 0..3) {
                    if (!isValidDecoSide(x, y, side)) {
                        decoCache[x][y][side].clear()
                        continue
                    }
                    val count = if (rand.nextFloat() < decoProbability) rand.nextInt(1, 2) else 0
                    repeat(count) {
                        val idx = rand.nextInt(0, 17)
                        val tex = decoTextures[idx]
                        val origW = tex.regionWidth.toFloat()
                        val origH = tex.regionHeight.toFloat()
                        val target = rand.nextFloat() * 6f + 10f
                        val scale = target / maxOf(origW, origH)

                        val offX = when (side) {
                            0 -> -rand.nextFloat() * cellGap - cellGap
                            1 -> rand.nextFloat() * cellGap - 2 * cellGap
                            else -> rand.nextFloat() * cellSize
                        }
                        val offY = when (side) {
                            2 -> -rand.nextFloat() * cellGap - cellGap
                            3 -> rand.nextFloat() * cellGap - 2 * cellGap
                            else -> rand.nextFloat() * cellSize
                        }

                        decoCache[x][y][side].add(
                            DecoItem(idx, rand.nextBoolean(), origW * scale, origH * scale, offX, offY)
                        )
                    }
                }
            }
        }
    }

    private fun renderDecoBorders(batch: SpriteBatch, x: Int, y: Int, posX: Float, posY: Float, light: Float) {
        for (side in 0..3) {
            for (deco in decoCache[x][y][side]) {
                val darkLight = (light - 0.2f).coerceAtLeast(0.2f)
                batch.color = Color.WHITE.cpy().mul(darkLight, darkLight, darkLight, 1f)

                val ox = deco.w / 2f
                val oy = deco.h / 2f
                batch.draw(
                    decoTextures[deco.idx],
                    posX + deco.offX, posY + deco.offY,
                    ox, oy,
                    deco.w, deco.h,
                    if (deco.mirror) -1f else 1f, 1f,
                    0f
                )
            }
        }
    }

    private fun isValidDecoSide(x: Int, y: Int, side: Int): Boolean {
        val (dx, dy) = when (side) {
            0 -> -1 to 0
            1 -> 1 to 0
            2 -> 0 to -1
            3 -> 0 to 1
            else -> return false
        }

        val nx = x + dx
        val ny = y + dy

        if (nx !in 0 until gameMap.width || ny !in 0 until gameMap.height) return false

        val t1 = gameMap.getTerrain(x, y)
        val t2 = gameMap.getTerrain(nx, ny)

        return (t1 == TerrainType.LAND || t1 == TerrainType.ENEMY) &&
            (t2 == TerrainType.LAND || t2 == TerrainType.ENEMY)
    }
}
