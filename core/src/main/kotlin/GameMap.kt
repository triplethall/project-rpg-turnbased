package ru.triplethall.rpgturnbased

import kotlin.random.Random

// Типы местности
enum class TerrainType {
    WATER,
    LAND,
    MOUNTAIN,
    Chest,
    CITY,
    CITYANCHOR,
    ENEMY,
    TRAP,
    UPGRADE,
    OUTPOST,
    OpenedChest,
    FOREST

}

class GameMap(
    val width: Int = 100,
    val height: Int = 100,
    var chestMenu: ChestMenu? = null
) {
    private val originalTerrain = Array(width) { Array(height) { TerrainType.LAND } }

    private val terrain = Array(width) { Array(height) { TerrainType.WATER } }
    private val explored = Array(width) { BooleanArray(height) { false } }

    fun markExplored(x: Int, y: Int) {
        explored[x][y] = true
    }

    fun placeEnemyWithOriginal(x: Int, y: Int) {
        originalTerrain[x][y] = getTerrain(x, y) // сохраняем что было (LAND или FOREST)
        terrain[x][y] = TerrainType.ENEMY
    }

    fun restoreAfterBattle(x: Int, y: Int) {
        if (terrain[x][y] == TerrainType.ENEMY) {
            terrain[x][y] = originalTerrain[x][y] // восстанавливаем исходный тип
        }
    }


     // Возвращает список координат всех клеток с типом ENEMY
    fun getAllEnemyCells(): List<Pair<Int, Int>> {
        val cells = mutableListOf<Pair<Int, Int>>()
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (terrain[x][y] == TerrainType.ENEMY) {
                    cells.add(Pair(x, y))
                }
            }
        }
        return cells
    }

     //Возвращает список координат врагов в радиусе от центра

    fun getEnemiesNear(centerX: Int, centerY: Int, radius: Int): List<Pair<Int, Int>> {
        val cells = mutableListOf<Pair<Int, Int>>()
        for (dx in -radius..radius) {
            for (dy in -radius..radius) {
                val nx = centerX + dx
                val ny = centerY + dy
                if (nx in 0 until width && ny in 0 until height && terrain[nx][ny] == TerrainType.ENEMY) {
                    cells.add(Pair(nx, ny))
                }
            }
        }
        return cells
    }

      // Проверяет, есть ли хотя бы один враг на карте

    fun hasEnemies(): Boolean {
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (terrain[x][y] == TerrainType.ENEMY) return true
            }
        }
        return false
    }

     //Удаляет всех врагов с карты (превращает обратно в исходный тип местности)
    fun clearAllEnemies() {
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (terrain[x][y] == TerrainType.ENEMY) {
                    terrain[x][y] = originalTerrain[x][y] // восстанавливаем ландшафт
                }
            }
        }
    }


    fun isExplored(x: Int, y: Int): Boolean = explored[x][y]
    fun getTerrain(x: Int, y: Int): TerrainType {
        if (x !in 0 until width || y !in 0 until height) return TerrainType.WATER
        return terrain[x][y]
    }
    fun setTerrain(x: Int, y: Int, type: TerrainType)
    {
        if (x in 0 until width && y in 0 until height)
        {
            terrain[x][y] = type;
        }
    }

    fun isWalkable(x: Int, y: Int): Boolean {
        val t = getTerrain(x, y)

        return t == TerrainType.LAND ||
            t == TerrainType.ENEMY ||
            t == TerrainType.TRAP ||
            t == TerrainType.UPGRADE ||
            t == TerrainType.OUTPOST ||
            t == TerrainType.Chest ||
            t == TerrainType.OpenedChest ||
            t == TerrainType.FOREST
    }

    fun generate(playerStartX: Int = 1, playerStartY: Int = 1) {
        generateIslandShape()
        ensureSingleIsland()
        fillSmallLakes()
        placeMountains()
        ensureStartAreaIsWalkable()
        validateMountainPaths()
        placeForestGroups()
        placeChests()
        placeCity()
        placeEnemies(10, playerStartX, playerStartY)
        placeTraps(3, playerStartX, playerStartY)
        placeUpgrade()
        placeOutpost()
        ensureStartAreaIsLand(playerStartX, playerStartY)
    }

    // --- Логика генерации ---

    private fun generateIslandShape() {
        val random = Random

        for (x in 0 until width) {
            for (y in 0 until height) {
                terrain[x][y] = TerrainType.WATER
            }
        }

        val centerPoints = listOf(
            Pair(width / 2, height / 2),
            Pair(width / 2 + random.nextInt(-5, 6), height / 2 + random.nextInt(-5, 6)),
            Pair(width / 2 + random.nextInt(-5, 6), height / 2 + random.nextInt(-5, 6)),
            Pair(width / 2 + random.nextInt(-5, 6), height / 2 + random.nextInt(-5, 6)),
            Pair(width / 2 + random.nextInt(-5, 6), height / 2 + random.nextInt(-5, 6)),
            Pair(width / 2 + random.nextInt(-5, 6), height / 2 + random.nextInt(-5, 6)),
            Pair(width / 2 + random.nextInt(-5, 6), height / 2 + random.nextInt(-5, 6))
        )

        for ((cx, cy) in centerPoints) {
            growLand(cx, cy, random.nextInt(30, 35))
        }

        addRandomCapes(random)

        cleanNoise()
    }

    private fun growLand(startX: Int, startY: Int, maxSteps: Int) {
        val random = Random
        var x = startX
        var y = startY
        val directions = listOf(Pair(0, 1), Pair(0, -1), Pair(1, 0), Pair(-1, 0))

        for (i in 0 until maxSteps) {
            if (x in 0 until width && y in 0 until height) {
                terrain[x][y] = TerrainType.LAND
            }

            val (dx, dy) = directions.random(random)
            x += dx
            y += dy

            if (random.nextFloat() < 0.15f) {
                val (dx2, dy2) = directions.random(random)
                if (x + dx2 in 0 until width && y + dy2 in 0 until height) {
                    terrain[x + dx2][y + dy2] = TerrainType.LAND
                }
            }
        }
    }

    private fun addRandomCapes(random: Random) {
        val landCells = mutableListOf<Pair<Int, Int>>()

        for (x in 0 until width) {
            for (y in 0 until height) {
                if (terrain[x][y] == TerrainType.LAND) {
                    landCells.add(Pair(x, y))
                }
            }
        }

        val capeCount = random.nextInt(25, 40)
        for (i in 0 until capeCount) {
            val (sx, sy) = landCells.random(random)
            val length = random.nextInt(10, 15) // Длина мыса

            val dir = listOf(Pair(0, 1), Pair(0, -1), Pair(1, 0), Pair(-1, 0)).random(random)

            var cx = sx
            var cy = sy
            for (j in 0 until length) {
                cx += dir.first
                cy += dir.second
                if (cx in 0 until width && cy in 0 until height) {
                    terrain[cx][cy] = TerrainType.LAND
                } else {
                    break
                }
            }
        }
    }

    private fun cleanNoise() {
        val newTerrain = Array(width) { x ->
            Array(height) { y ->
                terrain[x][y]
            }
        }

        for (x in 0 until width) {
            for (y in 0 until height) {
                val neighbors = countLandNeighbors(x, y)
                if (terrain[x][y] == TerrainType.LAND && neighbors < 3) {
                    newTerrain[x][y] = TerrainType.WATER // Убираем одиночки
                }
                if (terrain[x][y] == TerrainType.WATER && neighbors > 6) {
                    newTerrain[x][y] = TerrainType.LAND // Убираем одиночки
                }
            }
        }

        for (x in 0 until width) {
            for (y in 0 until height) {
                terrain[x][y] = newTerrain[x][y]
            }
        }
    }

    private fun countLandNeighbors(x: Int, y: Int): Int {
        var count = 0
        for (dx in -1..1) {
            for (dy in -1..1) {
                if (dx == 0 && dy == 0) continue
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until width && ny in 0 until height && terrain[nx][ny] == TerrainType.LAND) {
                    count++
                }
            }
        }
        return count
    }

    private fun placeMountains() {
        val random = Random
        val landCells = mutableListOf<Pair<Int, Int>>()

        for (x in 0 until width) {
            for (y in 0 until height) {
                if (terrain[x][y] == TerrainType.LAND) {
                    landCells.add(Pair(x, y))
                }
            }
        }

        val mountainGroups = random.nextInt(3, 7) // 3-5 групп гор на карте

        for (g in 0 until mountainGroups) {
            val attempts = 20
            for (a in 0 until attempts) {
                val (sx, sy) = landCells.random(random)

                if (countLandNeighbors(sx, sy) < 5) continue
                if (hasDiagonalWater(sx, sy)) continue

                val groupSize = random.nextInt(1, 4) // 1, 2 или 3
                val placed = mutableListOf<Pair<Int, Int>>()
                placed.add(Pair(sx, sy))

                var valid = true

                for (i in 1 until groupSize) {
                    val last = placed.last()
                    val directions = listOf(Pair(0, 1), Pair(0, -1), Pair(1, 0), Pair(-1, 0))
                    val (dx, dy) = directions.random(random)
                    val nx = last.first + dx
                    val ny = last.second + dy

                    if (nx in 0 until width && ny in 0 until height &&
                        terrain[nx][ny] == TerrainType.LAND &&
                        !placed.contains(Pair(nx, ny))) {


                        val existingMountainsNearby = countAdjacentMountains(nx, ny)
                        if (existingMountainsNearby + placed.size < 3) {
                            placed.add(Pair(nx, ny))
                        } else {
                            valid = false
                            break
                        }
                    } else {
                        valid = false
                        break
                    }
                }

                if (valid) {
                    for ((px, py) in placed) {
                        terrain[px][py] = TerrainType.MOUNTAIN
                    }
                    break
                }
            }
        }
    }

    private fun placeChests() {
        val random = Random
        val possibleCells = mutableListOf<Pair<Int, Int>>()
        val centerX = width / 2
        val centerY = height / 2

        for (x in 0 until width) {
            for (y in 0 until height) {
                // Разрешаем ставить сундуки на LAND
                if (terrain[x][y] == TerrainType.LAND ) {
                    // Не ставим сундуки в стартовой зоне
                    if (kotlin.math.abs(x - centerX) > 3 || kotlin.math.abs(y - centerY) > 3) {
                        possibleCells.add(Pair(x, y))
                    }
                }
            }
        }

        if (possibleCells.isEmpty()) return

        val chestCount = random.nextInt(4, minOf(7, possibleCells.size / 10 + 5))
        repeat(chestCount) {
            val (cx, cy) = possibleCells.random(random)
            // Не проверяем на LAND, ставим на любую подходящую клетку
            terrain[cx][cy] = TerrainType.Chest
        }
    }
    fun collectChest(x: Int, y: Int): Boolean {
        if (x !in 0 until width || y !in 0 until height) return false

        if (terrain[x][y] == TerrainType.Chest) {
            terrain[x][y] = TerrainType.OpenedChest // Теперь он открыт
            chestMenu?.show()
            return true
        }
        return false
    }
    private fun countAdjacentMountains(x: Int, y: Int): Int {
        var count = 0
        for (dx in -1..1) {
            for (dy in -1..1) {
                if (dx == 0 && dy == 0) continue
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until width && ny in 0 until height && terrain[nx][ny] == TerrainType.MOUNTAIN) {
                    count++
                }
            }
        }
        return count
    }

    private fun ensureStartAreaIsWalkable() {
        val centerX = width / 2
        val centerY = height / 2
        for (x in (centerX - 2)..(centerX + 2)) {
            for (y in (centerY - 2)..(centerY + 2)) {
                if (x in 0 until width && y in 0 until height) {
                    terrain[x][y] = TerrainType.LAND // Стартовая зона всегда земля
                }
            }
        }
    }

    private fun ensureSingleIsland() {
        val visited = Array(width) { BooleanArray(height) { false } }
        val landCells = mutableListOf<Pair<Int, Int>>()


        for (x in 0 until width) {
            for (y in 0 until height) {
                if (terrain[x][y] == TerrainType.LAND) {
                    landCells.add(Pair(x, y))
                }
            }
        }

        if (landCells.isEmpty()) return


        val mainIsland = mutableSetOf<Pair<Int, Int>>()
        val queue = ArrayDeque<Pair<Int, Int>>()
        val center = landCells.minByOrNull {
            kotlin.math.abs(it.first - width/2) + kotlin.math.abs(it.second - height/2)
        } ?: return

        queue.add(center)
        mainIsland.add(center)
        visited[center.first][center.second] = true

        while (queue.isNotEmpty()) {
            val (x, y) = queue.removeFirst()
            for (dx in -1..1) {
                for (dy in -1..1) {
                    if (dx == 0 && dy == 0) continue
                    val nx = x + dx
                    val ny = y + dy
                    if (nx in 0 until width && ny in 0 until height &&
                        !visited[nx][ny] && terrain[nx][ny] == TerrainType.LAND) {
                        visited[nx][ny] = true
                        mainIsland.add(Pair(nx, ny))
                        queue.add(Pair(nx, ny))
                    }
                }
            }
        }


        for ((x, y) in landCells) {
            if (!mainIsland.contains(Pair(x, y))) {
                terrain[x][y] = TerrainType.WATER
            }
        }
    }

    private fun fillSmallLakes() {
        val visited = Array(width) { BooleanArray(height) { false } }
        val lakes = mutableListOf<MutableList<Pair<Int, Int>>>()

        for (x in 0 until width) {
            for (y in 0 until height) {
                if (terrain[x][y] == TerrainType.WATER && !visited[x][y]) {
                    val lake = findWaterArea(x, y, visited)
                    if (lake.isNotEmpty() && isEnclosedLake(lake)) {
                        lakes.add(lake)
                    }
                }
            }
        }

        lakes.sortByDescending { it.size }


        for ((index, lake) in lakes.withIndex()) {
            if (index >= 2 || lake.size <= 5) {
                for ((x, y) in lake) {
                    terrain[x][y] = TerrainType.LAND
                }
            }
        }
    }

    private fun findWaterArea(startX: Int, startY: Int, visited: Array<BooleanArray>): MutableList<Pair<Int, Int>> {
        val area = mutableListOf<Pair<Int, Int>>()
        val queue = ArrayDeque<Pair<Int, Int>>()

        queue.add(Pair(startX, startY))
        visited[startX][startY] = true

        while (queue.isNotEmpty()) {
            val (x, y) = queue.removeFirst()
            area.add(Pair(x, y))

            for (dx in -1..1) {
                for (dy in -1..1) {
                    if (dx == 0 && dy == 0) continue
                    if (kotlin.math.abs(dx) + kotlin.math.abs(dy) > 1) continue // Только 4 направления

                    val nx = x + dx
                    val ny = y + dy
                    if (nx in 0 until width && ny in 0 until height &&
                        !visited[nx][ny] && terrain[nx][ny] == TerrainType.WATER) {
                        visited[nx][ny] = true
                        queue.add(Pair(nx, ny))
                    }
                }
            }
        }

        return area
    }

    private fun isEnclosedLake(lake: List<Pair<Int, Int>>): Boolean {
        for ((x, y) in lake) {
            for (dx in -1..1) {
                for (dy in -1..1) {
                    if (dx == 0 && dy == 0) continue
                    if (kotlin.math.abs(dx) + kotlin.math.abs(dy) > 1) continue

                    val nx = x + dx
                    val ny = y + dy
                    if (nx !in 0 until width || ny !in 0 until height) {
                        return false // Касается края карты — не озеро
                    }
                    if (terrain[nx][ny] == TerrainType.WATER && !lake.contains(Pair(nx, ny))) {
                        return false // Связано с другой водой — не озеро
                    }
                }
            }
        }
        return true
    }

    private fun hasDiagonalWater(x: Int, y: Int): Boolean {
        val diagonalDirections = listOf(
            Pair(-1, -1), Pair(-1, 1),
            Pair(1, -1), Pair(1, 1)
        )

        for ((dx, dy) in diagonalDirections) {
            val nx = x + dx
            val ny = y + dy

            if (nx !in 0 until width || ny !in 0 until height) {
                return true
            }

            if (terrain[nx][ny] == TerrainType.WATER) {
                return true
            }
        }

        return false
    }

    private fun validateMountainPaths() {

        val allLandCells = mutableListOf<Pair<Int, Int>>()
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (terrain[x][y] == TerrainType.LAND || terrain[x][y] == TerrainType.MOUNTAIN) {
                    allLandCells.add(Pair(x, y))
                }
            }
        }

        if (allLandCells.isEmpty()) return

        val reachableCells = mutableListOf<Pair<Int, Int>>()
        val visited = mutableSetOf<Pair<Int, Int>>()
        val queue = ArrayDeque<Pair<Int, Int>>()

        val start = Pair(width / 2, height / 2)
        if (terrain[start.first][start.second] == TerrainType.LAND) {
            queue.add(start)
            visited.add(start)
            reachableCells.add(start)
        } else {
            val nearestLand = allLandCells.minByOrNull {
                kotlin.math.abs(it.first - width/2) + kotlin.math.abs(it.second - height/2)
            } ?: return

            queue.add(nearestLand)
            visited.add(nearestLand)
            reachableCells.add(nearestLand)
        }

        while (queue.isNotEmpty()) {
            val (x, y) = queue.removeFirst()

            val directions = listOf(Pair(0, 1), Pair(0, -1), Pair(1, 0), Pair(-1, 0))
            for ((dx, dy) in directions) {
                val nx = x + dx
                val ny = y + dy

                if (nx in 0 until width && ny in 0 until height &&
                    !visited.contains(Pair(nx, ny))) {

                    if (terrain[nx][ny] == TerrainType.LAND) {
                        visited.add(Pair(nx, ny))
                        reachableCells.add(Pair(nx, ny))
                        queue.add(Pair(nx, ny))
                    }

                }
            }
        }


        if (reachableCells.size == allLandCells.size) {
            return  // Всё ок, вся суша достижима
        }

        val unreachableCells = allLandCells.filter { !reachableCells.contains(it) }

        var fixed = false
        for ((ux, uy) in unreachableCells) {
            val directions = listOf(Pair(0, 1), Pair(0, -1), Pair(1, 0), Pair(-1, 0))
            for ((dx, dy) in directions) {
                val nx = ux + dx
                val ny = uy + dy

                if (nx in 0 until width && ny in 0 until height &&
                    reachableCells.contains(Pair(nx, ny)) &&
                    terrain[nx][ny] == TerrainType.MOUNTAIN) {
                    terrain[nx][ny] = TerrainType.LAND
                    fixed = true
                }
            }
        }

        if (fixed) {
            validateMountainPaths()
        }
    }
    private fun canPlaceCity(x: Int, y: Int): Boolean
    {
        for (dx in 0..1)
        {
            for (dy in 0..1)
            {
                val nx = x + dx
                val ny = y + dy
                if (nx !in 0 until width || ny !in 0 until height)
                {
                    return false
                }
                if (terrain[nx][ny] != TerrainType.LAND)
                {
                    return false
                }
            }
        }
        return true
    }
    private fun goodCityPosition(x: Int, y: Int): Boolean
    {
        var landCount = 0
        for (dx in -2..3)
        {
            for (dy in -2..3)
            {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until width && ny in 0 until height)
                {
                    if (terrain[nx][ny] == TerrainType.LAND)
                    {
                        landCount++
                    }
                }
            }
        }
        return landCount > 20
    }
    private fun placeCity()
    {
        val random = Random
        var attemps = 0
        while (attemps < 1000)
        {
            attemps++
            val x = random.nextInt(1, width - 2)
            val y = random.nextInt(1, height - 2)
            if (canPlaceCity(x, y) && goodCityPosition(x,y))
            {
                for (dx in 0..1)
                {
                    for (dy in 0..1)
                    {
                        if (dx == 0 && dy == 0) {terrain[x + dx][y + dy] = TerrainType.CITYANCHOR} else {
                        terrain[x + dx][y + dy] = TerrainType.CITY}
                    }
                }
                return
            }
        }
        val cx = width / 2
        val cy = height / 2
        for (dx in 0..1)
        {
            for (dy in 0..1)
            {
                terrain[cx+dx][cy+dy] = TerrainType.CITY
            }
        }
    }
    // метод, чтобы вычислять спавн игрока
    private fun isPlayerStartPosition(x: Int, y: Int, playerStartX: Int, playerStartY: Int, minDistance: Int = 3): Boolean
    {
        val dx = kotlin.math.abs(x - playerStartX)
        val dy = kotlin.math.abs(y - playerStartY)
        val distance = kotlin.math.sqrt((dx * dx + dy * dy).toDouble())
        return distance < minDistance


    }
    private fun placeEnemies(count: Int = 10, playerStartX: Int, playerStartY: Int) {
        val random = Random
        var placed = 0

        // Сначала спавним врагов около сундуков
        val chestPositions = mutableListOf<Pair<Int, Int>>()
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (terrain[x][y] == TerrainType.Chest) {
                    chestPositions.add(Pair(x, y))
                }
            }
        }

        val usedPositions = mutableSetOf<Pair<Int, Int>>()

        // Для каждого сундука спавним 1-2 врага рядом
        for ((chestX, chestY) in chestPositions) {
            val enemiesPerChest = random.nextInt(1, 4) // 2 или 4 врага

            for (i in 0 until enemiesPerChest) {
                // Ищем подходящую клетку вокруг сундука (радиус 1)
                var found = false
                for (attempt in 0 until 20) { // 20 попыток найти место
                    val dx = random.nextInt(-1, 2) // -1, 0, 1
                    val dy = random.nextInt(-1, 2)

                    if (dx == 0 && dy == 0) continue // пропускаем сам сундук

                    val enemyX = chestX + dx
                    val enemyY = chestY + dy
                    val enemyPos = Pair(enemyX, enemyY)

                    // Проверяем, можно ли поставить врага
                    if (enemyX in 0 until width && enemyY in 0 until height &&
                        (terrain[enemyX][enemyY] == TerrainType.LAND || terrain[enemyX][enemyY] == TerrainType.FOREST) && // можно на землю и лес
                        !usedPositions.contains(enemyPos) &&
                        !isPlayerStartPosition(enemyX, enemyY, playerStartX, playerStartY, 5) &&
                        isWalkable(enemyX, enemyY) // проверка на проходимость
                    ) {
                        // СОХРАНЯЕМ исходный тип и ставим врага
                        originalTerrain[enemyX][enemyY] = terrain[enemyX][enemyY]
                        terrain[enemyX][enemyY] = TerrainType.ENEMY
                        usedPositions.add(enemyPos)
                        placed++
                        found = true
                        break
                    }
                }

                // Если не нашли место с первой попытки, пробуем радиус 2
                if (!found) {
                    for (attempt in 0 until 20) {
                        val dx = random.nextInt(-2, 3)
                        val dy = random.nextInt(-2, 3)

                        if (dx == 0 && dy == 0) continue

                        val enemyX = chestX + dx
                        val enemyY = chestY + dy
                        val enemyPos = Pair(enemyX, enemyY)

                        if (enemyX in 0 until width && enemyY in 0 until height &&
                            (terrain[enemyX][enemyY] == TerrainType.LAND || terrain[enemyX][enemyY] == TerrainType.FOREST) &&
                            !usedPositions.contains(enemyPos) &&
                            !isPlayerStartPosition(enemyX, enemyY, playerStartX, playerStartY, 5) &&
                            isWalkable(enemyX, enemyY)
                        ) {
                            // СОХРАНЯЕМ исходный тип и ставим врага
                            originalTerrain[enemyX][enemyY] = terrain[enemyX][enemyY]
                            terrain[enemyX][enemyY] = TerrainType.ENEMY
                            usedPositions.add(enemyPos)
                            placed++
                            break
                        }
                    }
                }
            }
        }

        // Если нужно больше врагов, добиваем рандомными
        if (placed < count) {
            // Собираем все возможные позиции
            val availablePositions = mutableListOf<Pair<Int, Int>>()
            for (x in 0 until width) {
                for (y in 0 until height) {
                    if (canPlaceEnemy(x, y, playerStartX, playerStartY) &&
                        !usedPositions.contains(Pair(x, y))) {
                        availablePositions.add(Pair(x, y))
                    }
                }
            }

            // Перемешиваем и берем нужное количество
            if (availablePositions.isNotEmpty()) {
                val shuffled = availablePositions.shuffled(random)
                val toPlace = minOf(count - placed, shuffled.size)

                for (i in 0 until toPlace) {
                    val (x, y) = shuffled[i]
                    originalTerrain[x][y] = terrain[x][y]
                    terrain[x][y] = TerrainType.ENEMY
                    placed++
                }
            }

            // Если всё ещё не хватает врагов, пробуем с увеличенным радиусом
            if (placed < count) {
                var relaxedAttempts = 0
                while (placed < count && relaxedAttempts < 1000) {
                    relaxedAttempts++
                    val x = random.nextInt(0, width)
                    val y = random.nextInt(0, height)
                    val pos = Pair(x, y)
                    if ((terrain[x][y] == TerrainType.LAND || terrain[x][y] == TerrainType.FOREST) &&
                        !isPlayerStartPosition(x, y, playerStartX, playerStartY, 3) &&
                        !usedPositions.contains(pos)) {
                        originalTerrain[x][y] = terrain[x][y]
                        terrain[x][y] = TerrainType.ENEMY
                        placed++
                    }
                }
            }
        }
    }

    private fun canPlaceEnemy(x: Int, y: Int, playerStartX: Int, playerStartY: Int): Boolean
    {
        val t = terrain[x][y]
        if (t != TerrainType.LAND)
        {
            return false
        }
        // проверка расстояния до стартовой позиции игрока
        if (isPlayerStartPosition(x, y, playerStartX, playerStartY, 5))
        {
            return false
        }
        for (dx in -2..2)
        {
            for (dy in -2..2)
            {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until width && ny in 0 until height)
                {
                    if (terrain[nx][ny] == TerrainType.CITY ||
                        terrain[nx][ny] == TerrainType.OUTPOST ||
                        terrain[nx][ny] == TerrainType.UPGRADE ||
                        terrain[nx][ny] == TerrainType.TRAP ||
                        terrain[nx][ny] == TerrainType.ENEMY)
                    {
                        return false
                    }
                }
            }
        }
        return true
    }
    private fun placeTraps(count: Int = 3, playerStartX: Int, playerStartY: Int)
    {
        val random = Random
        var placed = 0
        // Собираем доступные позиции
        val availablePositions = mutableListOf<Pair<Int, Int>>()
        for (x in 0 until width)
        {
            for (y in 0 until height)
            {
                if (canPlaceTraps(x, y, playerStartX, playerStartY))
                {
                    availablePositions.add(Pair(x, y))
                }
            }
        }

        // Перемешиваем и берем нужное количество
        if (availablePositions.isNotEmpty())
        {
            val shuffled = availablePositions.shuffled(random)
            val toPlace = minOf(count, shuffled.size)

            for (i in 0 until toPlace)
            {
                val (x, y) = shuffled[i]
                terrain[x][y] = TerrainType.TRAP
                placed++
            }
        }
        if (placed == 0) {
            for (x in 0 until width)
            {
                for (y in 0 until height)
                {
                    if (terrain[x][y] == TerrainType.LAND && !isPlayerStartPosition(x, y, playerStartX, playerStartY, 2))
                    {
                        terrain[x][y] = TerrainType.TRAP
                        return
                    }
                }
            }
        }

    }
    private fun canPlaceTraps(x: Int, y: Int, playerStartX: Int, playerStartY: Int): Boolean
    {
        if (terrain[x][y] != TerrainType.LAND) {
            return false
        }
        // Проверка расстояния до стартовой позиции игрока
        if (isPlayerStartPosition(x, y, playerStartX, playerStartY, 4)) {
            return false
        }
        // Проверка, что не ставим ловушку на другие объекты
        for (dx in -1..1) {
            for (dy in -1..1) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until width && ny in 0 until height)
                {
                    if (terrain[nx][ny] == TerrainType.CITY ||
                        terrain[nx][ny] == TerrainType.ENEMY ||
                        terrain[nx][ny] == TerrainType.UPGRADE ||
                        terrain[nx][ny] == TerrainType.OUTPOST ||
                        terrain[nx][ny] == TerrainType.TRAP)
                    {
                        return false
                    }
                }
            }
        }
        return true
    }
    private fun placeUpgrade()
    {
        val random = Random
        if (random.nextFloat() > 0.5f)
        {
            return
        }
        var attemps = 0
        while (attemps < 1000)
        {
            attemps++
            val x = random.nextInt(0, width)
            val y = random.nextInt(0, height)
            if (canPlaceUpgrade(x, y))
            {
                terrain[x][y] = TerrainType.UPGRADE
                return
            }
        }
    }
    private fun canPlaceUpgrade(x: Int, y: Int): Boolean
    {
        if (terrain[x][y] != TerrainType.LAND)
        {
            return false
        }
        return true
    }
    private fun placeOutpost()
    {
        val random = Random
        if (random.nextFloat() > 0.25f)
        {
            return
        }
        var attempts = 0
        while (attempts < 1000)
        {
            attempts++
            val x = random.nextInt(0, width)
            val y = random.nextInt(0, height)
            val horizontal = random.nextBoolean()
            if (canPlaceOutpost(x, y, horizontal))
            {
                if (horizontal)
                {
                    terrain[x][y] = TerrainType.OUTPOST
                    terrain[x+1][y] = TerrainType.OUTPOST
                }
                else
                {
                    terrain[x][y] = TerrainType.OUTPOST
                    terrain[x][y+1] = TerrainType.OUTPOST
                }
                return
            }
        }
    }
    private fun canPlaceOutpost(x: Int, y: Int, horizontal: Boolean): Boolean
    {
        val positions = if (horizontal)
        {
            listOf( Pair(x, y) , Pair( x + 1, y))
        }
        else
        {
            listOf( Pair(x , y) , Pair(x , y + 1))
        }
        for ((nx, ny) in positions)
        {
            if (nx !in 0 until width || ny !in 0 until height)
            {
                return false
            }
            if (terrain[nx][ny] != TerrainType.LAND)
            {
                return false
            }
        }
        return true
    }


    private fun ensureStartAreaIsLand(playerStartX: Int, playerStartY: Int) {
        // Делаем область 3x3 вокруг игрока землёй
        for (x in (playerStartX - 1)..(playerStartX + 1)) {
            for (y in (playerStartY - 1)..(playerStartY + 1)) {
                if (x in 0 until width && y in 0 until height) {
                    // Если это не гора и не сундук (чтобы не затереть важные объекты)
                    if (terrain[x][y] != TerrainType.MOUNTAIN &&
                        terrain[x][y] != TerrainType.Chest &&
                        terrain[x][y] != TerrainType.ENEMY &&
                        terrain[x][y] != TerrainType.WATER) {
                        terrain[x][y] = TerrainType.LAND
                    }
                }
            }
        }

        // Дополнительно проверяем, что сама позиция игрока точно земля
        if (playerStartX in 0 until width && playerStartY in 0 until height) {
            terrain[playerStartX][playerStartY] = TerrainType.LAND
        }
    }

    private fun placeForestGroups() {
        val random = Random
        val groupsCount = random.nextInt(4, 7)
        var placedGroups = 0
        val maxAttempts = 30

        for (attempt in 0 until maxAttempts) {
            if (placedGroups >= groupsCount) break

            val groupSize = random.nextInt(3, 11)
            val startCell = findFreeLandCell()
            if (startCell == null) continue

            val group = growForestGroup(startCell.first, startCell.second, groupSize)

            if (group.size >= groupSize * 0.7) {
                // Размещаем лес
                for ((x, y) in group) {
                    if (terrain[x][y] != TerrainType.Chest) {
                        terrain[x][y] = TerrainType.FOREST
                    }
                }
                placedGroups++
            }
        }
    }

    // Поиск свободной клетки LAND
    private fun findFreeLandCell(): Pair<Int, Int>? {
        val random = Random
        for (attempt in 0 until 100) {
            val x = random.nextInt(0, width)
            val y = random.nextInt(0, height)

            if (terrain[x][y] == TerrainType.LAND &&
                !isNearSpecialObjects(x, y)) {
                return Pair(x, y)
            }
        }
        return null
    }

    // Проверка, что рядом нет важных объектов
    private fun isNearSpecialObjects(x: Int, y: Int): Boolean {
        for (dx in -2..2) {
            for (dy in -2..2) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until width && ny in 0 until height) {
                    when (terrain[nx][ny]) {
                        TerrainType.MOUNTAIN,
                        TerrainType.Chest,
                        TerrainType.ENEMY,
                        TerrainType.FOREST -> return true
                        else -> {}
                    }
                }
            }
        }
        return false
    }

    // Выращивание группы леса
    private fun growForestGroup(startX: Int, startY: Int, targetSize: Int): MutableList<Pair<Int, Int>> {
        val random = Random
        val group = mutableListOf(Pair(startX, startY))
        val frontier = mutableListOf(Pair(startX, startY))
        val visited = mutableSetOf(Pair(startX, startY))

        while (group.size < targetSize && frontier.isNotEmpty()) {
            val current = frontier.random(random)
            frontier.remove(current)

            val neighbors = getAdjacentLandCells(current.first, current.second)

            for (neighbor in neighbors) {
                if (!visited.contains(neighbor) && group.size < targetSize &&
                    !isNearSpecialObjects(neighbor.first, neighbor.second)) {
                    group.add(neighbor)
                    visited.add(neighbor)
                    frontier.add(neighbor)
                }
            }
        }

        // Гарантированный сундук в большой группе (9+ клеток)
        if (group.size >= 9) {
            var chestPlaced = false

            for ((x, y) in group) {
                val hasNorth = group.contains(Pair(x, y + 1))
                val hasSouth = group.contains(Pair(x, y - 1))
                val hasWest = group.contains(Pair(x - 1, y))
                val hasEast = group.contains(Pair(x + 1, y))

                if (hasNorth && hasSouth && hasWest && hasEast) {
                    terrain[x][y] = TerrainType.Chest
                    chestPlaced = true
                    break
                }
            }

            if (!chestPlaced && group.isNotEmpty()) {
                val (x, y) = group.first()
                terrain[x][y] = TerrainType.Chest
            }
        }

        return group
    }

    // Получение соседних клеток LAND (4 направления)
    private fun getAdjacentLandCells(x: Int, y: Int): List<Pair<Int, Int>> {
        val neighbors = mutableListOf<Pair<Int, Int>>()
        val directions = listOf(
            Pair(0, 1), Pair(0, -1), Pair(1, 0), Pair(-1, 0)
        )

        for ((dx, dy) in directions) {
            val nx = x + dx
            val ny = y + dy
            if (nx in 0 until width && ny in 0 until height &&
                terrain[nx][ny] == TerrainType.LAND) {
                neighbors.add(Pair(nx, ny))
            }
        }
        return neighbors
    }
}
