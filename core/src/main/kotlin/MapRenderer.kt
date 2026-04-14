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
    private lateinit var beachTextures: Array<TextureRegion>
    private val BEACH_W = 32f
    private val BEACH_H = 8f
    private val sandTexture: Texture
    private val city1Texture: Texture
    private val upgradeTexture: Texture
    private val dirtTexture: Texture
    private val mtnTexture: Texture
    private lateinit var beachCorner: TextureRegion
    private val CORNER_W = 32f
    private val CORNER_H = 32f
    private lateinit var cloudTextures: Array<TextureRegion>
    private val waterTextures = arrayOfNulls<Texture>(4)
    private var waterFrameIndex = 0
    private val forestTexture: Texture
    private var lastFrameTime = 0f
    private val frameDuration = 0.5f //частота смены кадров фона
    private val bgTileSize = 1024f

    private val lightCache = Array(gameMap.width) { FloatArray(gameMap.height) }
    private val distCache = Array(gameMap.width) { FloatArray(gameMap.height) }
    // Отслеживание последней позиции игрока
    private lateinit var decoTextures: Array<TextureRegion>
    private data class DecoItem(val idx: Int, val mirror: Boolean, val w: Float, val h: Float, val offX: Float, val offY: Float)
    // Кэш: [x][y][side] -> 0..2 декорации в зазоре
    private val decoCache = Array(gameMap.width) {
        Array(gameMap.height) {
            Array(4) { mutableListOf<DecoItem>() }
        }
    }
    private val DECO_PROBABILITY = 0.12f
    private var lastPlayerX = -999
    private var lastPlayerY = -999
    private var cacheValid = false

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
        sandTexture = Texture("map_layers/sand_back_tile.png")
        cloudTextures = Array(5) { i ->
            TextureRegion(Texture("map_layers/clouds/clouds$i.png"))
        }
        decoTextures = Array(13) { i ->
            TextureRegion(Texture("map_decor/deco0-${i.toString().padStart(2, '0')}.png"))
        }
        generateDecoVariations()
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
        rebuildLightCache(player)
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
                val light = if (cacheValid) lightCache[x][y] else 1.0f
                if (terrain != TerrainType.WATER) {
                    renderBeaches(batch, x, y, posX, posY, light)
                    renderBeachCorners(batch, x, y, posX, posY, light)
                }
                when (terrain) {
                    TerrainType.LAND, TerrainType.UPGRADE,TerrainType.CITY, TerrainType.CITYANCHOR, TerrainType.MOUNTAIN, TerrainType.OpenedChest, TerrainType.Chest, TerrainType.FOREST, TerrainType.ENEMY -> {
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
                val light = if (cacheValid) lightCache[x][y] else 1.0f
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
                renderGrassBorders(batch, x, y, posX, posY, light)
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
        // слой 3 - объекты
        for (x in 0 until gameMap.width) {
            for (y in gameMap.height - 1 downTo 0) {
                if (!gameMap.isExplored(x, y)) continue
                val distance = if (cacheValid) distCache[x][y] else 0f

                val terrain = gameMap.getTerrain(x, y)
                if (distance > 8 && terrain == TerrainType.ENEMY) continue
                if (terrain == TerrainType.WATER || terrain == TerrainType.LAND) continue

                val posX = x * (cellSize + cellGap)
                val posY = y * (cellSize + cellGap)
                val light = if (cacheValid) lightCache[x][y] else 1.0f


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
                    TerrainType.UPGRADE -> {
                        batch.color = Color(0.5f, 0.5f, 0.5f, 1f).mul(light, light, light, 1f)
                        batch.draw(upgradeTexture, posX + 5.0f, posY+5f, cellSize*0.7f, cellSize*0.7f)
                    }
                    TerrainType.CITYANCHOR -> {
                        batch.color = Color(0.5f, 0.5f, 0.5f, 1f).mul(light, light, light, 1f)
                        batch.draw(city1Texture, posX - cellSize*0.3f, posY - 1f, cellSize*2.65f, cellSize*2.65f)
                    }
                    TerrainType.FOREST -> {
                        batch.color = Color(0.2f, 0.5f, 0.1f, 1f).mul(light, light, light, 1f)
                        batch.draw(forestTexture, posX - cellSize*0.2f, posY, cellSize*1.4f, cellSize*1.4f)
                    }
                    TerrainType.MOUNTAIN -> {
                        batch.color = Color(1f, 1f, 1f, 1f).mul(light, light, light, 1f)
                        batch.draw(mtnTexture, posX - cellSize*0.2f, posY-cellGap, cellSize*1.4f, cellSize*1.5f)
                    }
                    else -> {
                        val color = when (terrain) {
                            TerrainType.MOUNTAIN -> continue
                            TerrainType.CITY -> continue
                            TerrainType.ENEMY -> Color.RED
                            TerrainType.TRAP -> Color.GRAY
                            TerrainType.UPGRADE -> continue
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
    private fun renderBeaches(batch: SpriteBatch, x: Int, y: Int, posX: Float, posY: Float, light: Float) {
        // Проверяем 4 стороны
        val neighbors = listOf(
            3 to Pair(0, 1),  // Top (y+1)
            2 to Pair(0, -1), // Bottom (y-1)
            0 to Pair(-1, 0), // Left (x-1)
            1 to Pair(1, 0)   // Right (x+1)
        )

        for ((side, offset) in neighbors) {
            val nx = x + offset.first
            val ny = y + offset.second

            // Если сосед валиден и это вода — рисуем пляж
            if (nx in -1 until gameMap.width +1 && ny in -1 until gameMap.height+1 &&
                gameMap.getTerrain(nx, ny) == TerrainType.WATER) {

                // Детерминированный выбор текстуры (чтобы не мерцала при каждом кадре)
                val texIdx = kotlin.math.abs((x * 31 + y * 17) % 7)
                val tex = beachTextures[texIdx]

                batch.color = Color.WHITE.cpy().mul(light, light, light, 1f)

                when (side) {
                    3 -> { // вода сверху. Рисуем над тайлом.
                        batch.draw(tex, posX - cellGap - 1f, posY + cellSize+cellGap, BEACH_W + 2*cellGap+2f, BEACH_H + cellGap*2)

                    }
                    2 -> { // Bottom: вода снизу. Рисуем под тайлом, поворот 180.
                        // pivot: Top-Center (16, 8) -> вешаем на нижнюю границу тайла
                        batch.draw(tex, posX + 2*cellGap - 3.1f,  posY - cellGap - cellSize/2 - 1f, 16f, 8f, BEACH_W + 2*cellGap + 2f, BEACH_H + cellGap*2, 1f, 1f, 180f)
                    }
                    0 -> { // Left: вода слева. Рисуем слева, поворот +90 (CCW).
                        // pivot: Bottom-Left (0, 0) -> вешаем на левый нижний угол тайла
                        batch.draw(tex, posX - 2*cellGap + 3f, posY - cellGap - 1f, 0f, 0f, BEACH_W + 2* cellGap + 2f, BEACH_H + cellGap*2, 1f, 1f, 90f)
                    }
                    1 -> { // Right: вода справа. Рисуем справа, поворот -90 (CW).
                        // pivot: Top-Left (0, 8) -> вешаем на правый верхний угол тайла
                        batch.draw(tex, posX + cellSize + 2*cellGap +4f, posY + cellSize/2 + 3*cellGap + 1f, 0f, 8f, BEACH_W+ 2* cellGap + 2f, BEACH_H+ cellGap*2, 1f, 1f, -90f)
                    }
                }
            }
        }
    }
    private fun renderGrassBorders(batch: SpriteBatch, x: Int, y: Int, posX: Float, posY: Float, light: Float) {
        // Проверяем 4 стороны
        val neighbors = listOf(
            3 to Pair(0, 1),  // Top (y+1)
            2 to Pair(0, -1), // Bottom (y-1)
            0 to Pair(-1, 0), // Left (x-1)
            1 to Pair(1, 0)   // Right (x+1)
        )

        for ((side, offset) in neighbors) {
            val nx = x + offset.first
            val ny = y + offset.second

            // Если сосед валиден и это вода — рисуем пляж
            if (nx in -1 until gameMap.width +1 && ny in -1 until gameMap.height+1 &&
                gameMap.getTerrain(nx, ny) == TerrainType.WATER) {

                // Детерминированный выбор текстуры (чтобы не мерцала при каждом кадре)

                val texIdx2 = kotlin.math.abs((x * 31 + y * 17) % 10)

                val tex2 = grassSpoilers[texIdx2]

                batch.color = Color.WHITE.cpy().mul(light, light, light, 1f)

                when (side) {
                    3 -> { // вода сверху. Рисуем над тайлом.

                        batch.draw(tex2, posX - cellGap - 1f, posY + cellSize+cellGap, BEACH_W + 2*cellGap+2f, BEACH_H)
                    }
                    2 -> { // Bottom: вода снизу. Рисуем под тайлом, поворот 180.
                        // pivot: Top-Center (16, 8) -> вешаем на нижнюю границу тайла
                        batch.draw(tex2, posX + 2*cellGap - 3.1f,  posY - cellGap - cellSize/2 - 1f, 16f, 8f, BEACH_W + 2*cellGap + 2f, BEACH_H, 1f, 1f, 180f)
                    }
                    0 -> { // Left: вода слева. Рисуем слева, поворот +90 (CCW).
                        // pivot: Bottom-Left (0, 0) -> вешаем на левый нижний угол тайла
                        batch.draw(tex2, posX - 2*cellGap + 3f, posY - cellGap - 1f, 0f, 0f, BEACH_W + 2* cellGap + 2f, BEACH_H, 1f, 1f, 90f)
                    }
                    1 -> { // Right: вода справа. Рисуем справа, поворот -90 (CW).
                        // pivot: Top-Left (0, 8) -> вешаем на правый верхний угол тайла
                        batch.draw(tex2, posX + cellSize + 2*cellGap +4f, posY + cellSize/2 + 3*cellGap + 1f, 0f, 8f, BEACH_W+ 2* cellGap + 2f, BEACH_H, 1f, 1f, -90f)
                    }
                }
            }
        }
    }
    private fun rebuildLightCache(player: Player) {
        // Если игрок не сдвинулся — выходим
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
                // Считаем дистанцию один раз
                val dist = sqrt(dx * dx + dy * dy).toFloat()
                distCache[x][y] = dist

                // Сразу считаем свет
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
        // Диагонали: (dx, dy, rotation)
        // corner.png ориентирован как верх-правый угол (вода по диагонали TR)
        val diagonals = listOf(
            Triple(1, 1, 0f),      // Top-Right: вода справа+сверху → 0°
            Triple(1, -1, -90f),   // Bottom-Right: вода справа+снизу → -90° (по часовой)
            Triple(-1, -1, 180f),  // Bottom-Left: вода слева+снизу → 180°
            Triple(-1, 1, 90f)     // Top-Left: вода слева+сверху → +90° (против часовой)
        )

        for ((dx, dy, rotation) in diagonals) {
            val nx = x + dx
            val ny = y + dy

            // Угол нужен, если диагональный сосед и оба прямых соседа — вода
            if (nx in -1 until gameMap.width + 1 && ny in -1 until gameMap.height + 1 &&
                gameMap.getTerrain(nx, ny) == TerrainType.WATER &&
                gameMap.getTerrain(x + dx, y) == TerrainType.WATER &&
                gameMap.getTerrain(x, y + dy) == TerrainType.WATER) {

                batch.color = Color.WHITE.cpy().mul(light, light, light, 1f)

                // Позиция: внешний угол тайла
                val cornerX = if (dx > 0) posX + cellSize else posX
                val cornerY = if (dy > 0) posY + cellSize else posY

                // pivot: 0,0 (левый нижний угол текстуры)
                batch.draw(beachCorner, cornerX, cornerY, 0f, 0f, CORNER_W/1.5f, CORNER_H/1.5f, 1f, 1f, rotation)
            }
        }
    }
    fun generateDecoVariations() {
        val rand = kotlin.random.Random
        for (x in 0 until gameMap.width) {
            for (y in gameMap.height - 1 downTo 0) {
                for (side in 0..3) {
                    // 0..2 декорации в одном зазоре
                    if (!isValidDecoSide(x, y, side)) {
                        decoCache[x][y][side].clear()
                        continue
                    }
                    val count = if (rand.nextFloat() < DECO_PROBABILITY) rand.nextInt(1, 2) else 0
                    for (n in 0 until count) {
                        val idx = rand.nextInt(0, 13)
                        val tex = decoTextures[idx]
                        val origW = tex.regionWidth.toFloat()
                        val origH = tex.regionHeight.toFloat()
                        val target = rand.nextFloat() * 6f + 10f // 10..16f
                        val scale = target / maxOf(origW, origH)

                        // Рандомное смещение внутри прямоугольника зазора
                        val offX = when (side) {
                            0 -> -rand.nextFloat() * cellGap - cellGap
                            1 -> rand.nextFloat() * cellGap - 2*cellGap
                            else -> rand.nextFloat() * cellSize
                        }
                        val offY = when (side) {
                            2 -> -rand.nextFloat() * cellGap - cellGap
                            3 -> rand.nextFloat() * cellGap - 2*cellGap
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
                // Затемнение: сдвиг на -0.2f, но не ниже 0.2f (чтобы не уходило в черный)
                val darkLight = (light - 0.2f).coerceAtLeast(0.2f)
                batch.color = Color.WHITE.cpy().mul(darkLight, darkLight, darkLight, 1f)

                // Rotation = 0, origin в центре для корректного зеркала
                val ox = deco.w / 2f
                val oy = deco.h / 2f
                batch.draw(
                    decoTextures[deco.idx],
                    posX + deco.offX, posY + deco.offY, // позиция внутри зазора
                    ox, oy,                              // origin
                    deco.w, deco.h,                      // размеры
                    if (deco.mirror) -1f else 1f, 1f,    // scale
                    0f                                   // rotation
                )
            }
        }
    }
    private fun isValidDecoSide(x: Int, y: Int, side: Int): Boolean {
        val (dx, dy) = when (side) {
            0 -> -1 to 0  // Left
            1 -> 1 to 0   // Right
            2 -> 0 to -1  // Bottom
            3 -> 0 to 1   // Top
            else -> return false
        }

        val nx = x + dx
        val ny = y + dy

        // Если сосед за границей карты — нельзя ставить декор
        if (nx !in 0 until gameMap.width || ny !in 0 until gameMap.height) return false

        val t1 = gameMap.getTerrain(x, y)
        val t2 = gameMap.getTerrain(nx, ny)

        // Разрешаем только если ОБА тайла — LAND или ENEMY
        return (t1 == TerrainType.LAND || t1 == TerrainType.ENEMY) &&
            (t2 == TerrainType.LAND || t2 == TerrainType.ENEMY)
}}
