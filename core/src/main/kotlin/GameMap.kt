package ru.triplethall.rpgturnbased

import kotlin.random.Random

// Типы местности
enum class TerrainType {
    WATER,
    LAND,
    MOUNTAIN,
    Chest,
    CITY,
    ENEMY,
    TRAP,
    UPGRADE,
    OUTPOST,
}

class GameMap(
    val width: Int = 100,
    val height: Int = 100
) {

    private val terrain = Array(width) { Array(height) { TerrainType.WATER } }
    private val explored = Array(width) { BooleanArray(height) { false } }

    fun markExplored(x: Int, y: Int) {
        explored[x][y] = true
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
            t == TerrainType.CITY ||
            t == TerrainType.ENEMY ||
            t == TerrainType.TRAP ||
            t == TerrainType.UPGRADE ||
            t == TerrainType.OUTPOST ||
            t == TerrainType.Chest
    }

    fun generate(playerStartX: Int = 1, playerStartY: Int = 1) {
        generateIslandShape()
        ensureSingleIsland()
        fillSmallLakes()
        placeMountains()
        ensureStartAreaIsWalkable()
        validateMountainPaths()
        placeChests()
        placeCity()
        placeEnemies(10, playerStartX, playerStartY)
        placeTraps(3, playerStartX, playerStartY)
        placeUpgrade()
        placeOutpost()
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
        val landCells = mutableListOf<Pair<Int, Int>>()
        val centerX = width / 2
        val centerY = height / 2
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (terrain[x][y] == TerrainType.LAND) {
                    if (kotlin.math.abs(x - centerX) > 3 || kotlin.math.abs(y - centerY) > 3) {
                        landCells.add(Pair(x, y))
                    }
                }
            }
        }
        if (landCells.isEmpty()) return
        val chestCount = random.nextInt(5, minOf(12, landCells.size / 10 + 5))
        repeat(chestCount) {
            val (cx, cy) = landCells.random(random)
            if (terrain[cx][cy] == TerrainType.LAND) {
                terrain[cx][cy] = TerrainType.Chest
            }
        }
    }

    fun collectChest(x: Int, y: Int): Boolean {
        if (x !in 0 until width || y !in 0 until height) return false
        if (terrain[x][y] == TerrainType.Chest) {
            terrain[x][y] = TerrainType.LAND
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
                        terrain[x + dx][y + dy] = TerrainType.CITY
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
    private fun placeEnemies(count: Int = 10, playerStartX: Int, playerStartY: Int)
    {
        val random = Random
        var placed = 0
        // Сначала собираем все возможные позиции
        val availablePositions = mutableListOf<Pair<Int, Int>>()
        for (x in 0 until width)
        {
            for (y in 0 until height)
            {
                if (canPlaceEnemy(x, y, playerStartX, playerStartY))
                {
                    availablePositions.add(Pair(x, y))
                }
            }
        }

        // Перемешиваем и берем нужное количество
        if (availablePositions.isNotEmpty()) {
            val shuffled = availablePositions.shuffled(random)
            val toPlace = minOf(count, shuffled.size)

            for (i in 0 until toPlace)
            {
                val (x, y) = shuffled[i]
                terrain[x][y] = TerrainType.ENEMY
                placed++
            }
        }
        // если все еще не хватает врагов, пробуем с увеличенным радиусом
        if (placed < count)
        {
            var relaxedAttempts = 0
            while (placed < count && relaxedAttempts < 1000)
            {
                relaxedAttempts++
                val x = random.nextInt(0, width)
                val y = random.nextInt(0, height)
                if (terrain[x][y] == TerrainType.LAND && !isPlayerStartPosition(x, y, playerStartX, playerStartY, 3))
                {
                    terrain[x][y] = TerrainType.ENEMY
                    placed++
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





}
