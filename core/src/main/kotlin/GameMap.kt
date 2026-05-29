package ru.triplethall.rpgturnbased

import kotlin.math.sqrt
import kotlin.random.Random

enum class TerrainType {
    LAND,
    WATER,
    FOREST,
    MOUNTAIN,
    CITY,
    CITYANCHOR,
    CHEST,
    OPENEDCHEST,
    UPGRADE,
    ENEMY,
    TRAP,
    TRAP_TRIGGERED,
    OUTPOST,
    CAVEENTRANCE,
    QUEST_GIVER,
    TELEPORT
}

class GameMap(
    val width: Int = 100,
    val height: Int = 100,
    var chestMenu: ChestMenu? = null
) {
    private val originalTerrain = Array(width) { Array(height) { TerrainType.LAND } }
    private val questGivers = mutableMapOf<Pair<Int, Int>, QuestGiver>()
    private val terrain = Array(width) { Array(height) { TerrainType.WATER } }
    private val explored = Array(width) { BooleanArray(height) { false } }
    private val mimicSizes = mutableMapOf<Pair<Int, Int>, Int>()
    private val trapTypes = mutableMapOf<Pair<Int, Int>, TrapType>()

    fun setTrapType(x: Int, y: Int, type: TrapType) {
        trapTypes[Pair(x, y)] = type
    }

    fun getQuestGiver(x: Int, y: Int): QuestGiver? = questGivers[Pair(x, y)]

    fun getTrapType(x: Int, y: Int): TrapType? = trapTypes[Pair(x, y)]

    fun triggerTrap(x: Int, y: Int) {
        if (terrain[x][y] == TerrainType.TRAP) {
            terrain[x][y] = TerrainType.TRAP_TRIGGERED
            trapTypes.remove(Pair(x, y))
        }
    }

    fun markExplored(x: Int, y: Int) {
        explored[x][y] = true
    }

    fun placeEnemyWithOriginal(x: Int, y: Int) {
        originalTerrain[x][y] = getTerrain(x, y)
        terrain[x][y] = TerrainType.ENEMY
    }

    fun restoreAfterBattle(x: Int, y: Int) {
        if (terrain[x][y] == TerrainType.ENEMY) {
            terrain[x][y] = originalTerrain[x][y]
        }
    }

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

    fun hasEnemies(): Boolean {
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (terrain[x][y] == TerrainType.ENEMY) return true
            }
        }
        return false
    }

    fun clearAllEnemies() {
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (terrain[x][y] == TerrainType.ENEMY) {
                    terrain[x][y] = originalTerrain[x][y]
                }
            }
        }
    }

    fun isExplored(x: Int, y: Int): Boolean = explored[x][y]

    fun getTerrain(x: Int, y: Int): TerrainType {
        if (x !in 0 until width || y !in 0 until height) return TerrainType.WATER
        return terrain[x][y]
    }

    fun isMimicChest(x: Int, y: Int): Boolean = mimicSizes.containsKey(Pair(x, y))

    fun getMimicSize(x: Int, y: Int): Int = mimicSizes[Pair(x, y)] ?: 0

    fun setMimicSize(x: Int, y: Int, size: Int) {
        if (size > 0) {
            mimicSizes[Pair(x, y)] = size
        } else {
            mimicSizes.remove(Pair(x, y))
        }
    }

    fun setTerrain(x: Int, y: Int, type: TerrainType) {
        if (x in 0 until width && y in 0 until height) {
            terrain[x][y] = type
        }
    }

    fun isWalkable(x: Int, y: Int): Boolean {
        val t = getTerrain(x, y)
        return t == TerrainType.LAND ||
            t == TerrainType.ENEMY ||
            t == TerrainType.TRAP ||
            t == TerrainType.TRAP_TRIGGERED ||
            t == TerrainType.UPGRADE ||
            t == TerrainType.OUTPOST ||
            t == TerrainType.CHEST ||
            t == TerrainType.OPENEDCHEST ||
            t == TerrainType.FOREST ||
            t == TerrainType.QUEST_GIVER
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
        placeCaveEntrance(playerStartX, playerStartY)
        placeEnemies(10, playerStartX, playerStartY)
        placeTraps(3, playerStartX, playerStartY)
        placeUpgrade()
        placeOutpost()
        ensureStartAreaIsLand(playerStartX, playerStartY)
        placeQuestGivers(playerStartX, playerStartY)
    }

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
        for (idx in 0 until maxSteps) {
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
        repeat(capeCount) {
            val (sx, sy) = landCells.random(random)
            val length = random.nextInt(10, 15)
            val dir = listOf(Pair(0, 1), Pair(0, -1), Pair(1, 0), Pair(-1, 0)).random(random)
            var cx = sx
            var cy = sy
            for (idx in 0 until length) {
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
                    newTerrain[x][y] = TerrainType.WATER
                }
                if (terrain[x][y] == TerrainType.WATER && neighbors > 6) {
                    newTerrain[x][y] = TerrainType.LAND
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
        val mountainGroups = random.nextInt(3, 7)
        repeat(mountainGroups) { _ ->
            var placed = false
            repeat(20) { _ ->
                if (placed) return@repeat
                val (sx, sy) = landCells.random(random)
                if (countLandNeighbors(sx, sy) < 5) return@repeat
                if (hasDiagonalWater(sx, sy)) return@repeat
                val groupSize = random.nextInt(1, 4)
                val placedCells = mutableListOf(Pair(sx, sy))
                var valid = true
                for (j in 1 until groupSize) {
                    val last = placedCells.last()
                    val directions = listOf(Pair(0, 1), Pair(0, -1), Pair(1, 0), Pair(-1, 0))
                    val (dx, dy) = directions.random(random)
                    val nx = last.first + dx
                    val ny = last.second + dy
                    if (nx in 0 until width && ny in 0 until height &&
                        terrain[nx][ny] == TerrainType.LAND &&
                        !placedCells.contains(Pair(nx, ny))) {
                        val existingMountainsNearby = countAdjacentMountains(nx, ny)
                        if (existingMountainsNearby + placedCells.size < 3) {
                            placedCells.add(Pair(nx, ny))
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
                    for ((px, py) in placedCells) {
                        terrain[px][py] = TerrainType.MOUNTAIN
                    }
                    placed = true
                }
            }
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
                for ((x, y) in group) {
                    if (terrain[x][y] != TerrainType.CHEST) {
                        terrain[x][y] = TerrainType.FOREST
                    }
                }
                placedGroups++
            }
        }
    }

    private fun findFreeLandCell(): Pair<Int, Int>? {
        val random = Random
        for (attempt in 0 until 100) {
            val x = random.nextInt(0, width)
            val y = random.nextInt(0, height)
            if (terrain[x][y] == TerrainType.LAND && !isNearSpecialObjects(x, y)) {
                return Pair(x, y)
            }
        }
        return null
    }

    private fun isNearSpecialObjects(x: Int, y: Int): Boolean {
        for (dx in -2..2) {
            for (dy in -2..2) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until width && ny in 0 until height) {
                    when (terrain[nx][ny]) {
                        TerrainType.MOUNTAIN,
                        TerrainType.CHEST,
                        TerrainType.ENEMY,
                        TerrainType.FOREST -> return true
                        else -> {}
                    }
                }
            }
        }
        return false
    }

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

        if (group.size >= 9) {
            for ((x, y) in group) {
                val hasNorth = group.contains(Pair(x, y + 1))
                val hasSouth = group.contains(Pair(x, y - 1))
                val hasWest = group.contains(Pair(x - 1, y))
                val hasEast = group.contains(Pair(x + 1, y))
                if (hasNorth && hasSouth && hasWest && hasEast) {
                    terrain[x][y] = TerrainType.CHEST
                    if (random.nextDouble() < 0.3) {
                        val size = when {
                            random.nextDouble() < 0.6 -> 1
                            random.nextDouble() < 0.9 -> 2
                            else -> 3
                        }
                        setMimicSize(x, y, size)
                    }
                    break
                }
            }
        }
        return group
    }

    private fun getAdjacentLandCells(x: Int, y: Int): List<Pair<Int, Int>> {
        val neighbors = mutableListOf<Pair<Int, Int>>()
        val directions = listOf(Pair(0, 1), Pair(0, -1), Pair(1, 0), Pair(-1, 0))
        for ((dx, dy) in directions) {
            val nx = x + dx
            val ny = y + dy
            if (nx in 0 until width && ny in 0 until height && terrain[nx][ny] == TerrainType.LAND) {
                neighbors.add(Pair(nx, ny))
            }
        }
        return neighbors
    }

    private fun placeChests() {
        val random = Random
        val possibleCells = mutableListOf<Pair<Int, Int>>()
        val centerX = width / 2
        val centerY = height / 2
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (terrain[x][y] == TerrainType.LAND) {
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
            terrain[cx][cy] = TerrainType.CHEST
            if (random.nextDouble() < 0.3) {
                val size = when {
                    random.nextDouble() < 0.6 -> 1
                    random.nextDouble() < 0.9 -> 2
                    else -> 3
                }
                setMimicSize(cx, cy, size)
            }
        }
    }

    fun collectChest(x: Int, y: Int): Boolean {
        if (x !in 0 until width || y !in 0 until height) return false
        if (terrain[x][y] == TerrainType.CHEST) {
            terrain[x][y] = TerrainType.OPENEDCHEST
            mimicSizes.remove(Pair(x, y))
            return true
        }
        return false
    }

    fun checkDeleteOpenChest(x: Int, y: Int, px: Int, py: Int): Boolean {
        if (x !in 0 until width || y !in 0 until height) return false
        val distanceToPlayer = sqrt(((x - px) * (x - px) + (y - py) * (y - py)).toDouble()).toInt()
        if (terrain[x][y] == TerrainType.OPENEDCHEST && distanceToPlayer > 4) {
            terrain[x][y] = TerrainType.LAND
            mimicSizes.remove(Pair(x, y))
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
                    terrain[x][y] = TerrainType.LAND
                }
            }
        }
    }

    private fun ensureStartAreaIsLand(playerStartX: Int, playerStartY: Int) {
        for (x in (playerStartX - 1)..(playerStartX + 1)) {
            for (y in (playerStartY - 1)..(playerStartY + 1)) {
                if (x in 0 until width && y in 0 until height) {
                    if (terrain[x][y] != TerrainType.MOUNTAIN &&
                        terrain[x][y] != TerrainType.CHEST &&
                        terrain[x][y] != TerrainType.ENEMY &&
                        terrain[x][y] != TerrainType.WATER) {
                        terrain[x][y] = TerrainType.LAND
                    }
                }
            }
        }
        if (playerStartX in 0 until width && playerStartY in 0 until height) {
            terrain[playerStartX][playerStartY] = TerrainType.LAND
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
            kotlin.math.abs(it.first - width / 2) + kotlin.math.abs(it.second - height / 2)
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
                for ((lx, ly) in lake) {
                    terrain[lx][ly] = TerrainType.LAND
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
                    if (kotlin.math.abs(dx) + kotlin.math.abs(dy) > 1) continue
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
                        return false
                    }
                    if (terrain[nx][ny] == TerrainType.WATER && !lake.contains(Pair(nx, ny))) {
                        return false
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
                kotlin.math.abs(it.first - width / 2) + kotlin.math.abs(it.second - height / 2)
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
            return
        }
        val unreachableCells = allLandCells.filter { !reachableCells.contains(it) }
        for ((ux, uy) in unreachableCells) {
            val directions = listOf(Pair(0, 1), Pair(0, -1), Pair(1, 0), Pair(-1, 0))
            for ((dx, dy) in directions) {
                val nx = ux + dx
                val ny = uy + dy
                if (nx in 0 until width && ny in 0 until height &&
                    reachableCells.contains(Pair(nx, ny)) &&
                    terrain[nx][ny] == TerrainType.MOUNTAIN) {
                    terrain[nx][ny] = TerrainType.LAND
                }
            }
        }
    }

    private fun canPlaceCity(x: Int, y: Int): Boolean {
        for (dx in 0..1) {
            for (dy in 0..1) {
                val nx = x + dx
                val ny = y + dy
                if (nx !in 0 until width || ny !in 0 until height) {
                    return false
                }
                if (terrain[nx][ny] != TerrainType.LAND) {
                    return false
                }
            }
        }
        return true
    }

    private fun goodCityPosition(x: Int, y: Int): Boolean {
        var landCount = 0
        for (dx in -2..3) {
            for (dy in -2..3) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until width && ny in 0 until height) {
                    if (terrain[nx][ny] == TerrainType.LAND) {
                        landCount++
                    }
                }
            }
        }
        return landCount > 20
    }

    private fun placeCity() {
        val random = Random
        var attempts = 0
        while (attempts < 1000) {
            attempts++
            val x = random.nextInt(1, width - 2)
            val y = random.nextInt(1, height - 2)
            if (canPlaceCity(x, y) && goodCityPosition(x, y)) {
                for (dx in 0..1) {
                    for (dy in 0..1) {
                        if (dx == 0 && dy == 0) {
                            terrain[x + dx][y + dy] = TerrainType.CITYANCHOR
                        } else {
                            terrain[x + dx][y + dy] = TerrainType.CITY
                        }
                    }
                }
                return
            }
        }
        val cx = width / 2
        val cy = height / 2
        for (dx in 0..1) {
            for (dy in 0..1) {
                terrain[cx + dx][cy + dy] = TerrainType.CITY
            }
        }
    }

    private fun isPlayerStartPosition(x: Int, y: Int, playerStartX: Int, playerStartY: Int, minDistance: Int = 3): Boolean {
        val dx = kotlin.math.abs(x - playerStartX)
        val dy = kotlin.math.abs(y - playerStartY)
        val distance = kotlin.math.sqrt((dx * dx + dy * dy).toDouble())
        return distance < minDistance
    }

    private fun canPlaceQuestGiver(x: Int, y: Int, playerStartX: Int, playerStartY: Int): Boolean {
        if (terrain[x][y] != TerrainType.LAND) return false
        if (isPlayerStartPosition(x, y, playerStartX, playerStartY, 6)) return false
        for (dx in -1..1) {
            for (dy in -1..1) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until width && ny in 0 until height) {
                    when (terrain[nx][ny]) {
                        TerrainType.CITY, TerrainType.CITYANCHOR, TerrainType.OUTPOST,
                        TerrainType.UPGRADE, TerrainType.CHEST, TerrainType.ENEMY,
                        TerrainType.MOUNTAIN, TerrainType.TRAP, TerrainType.WATER,
                        TerrainType.CAVEENTRANCE -> return false
                        else -> {}
                    }
                }
            }
        }
        return true
    }

    private fun placeQuestGivers(playerStartX: Int, playerStartY: Int) {
        val random = Random
        val count = random.nextInt(2, 5)
        var placed = 0
        var attempts = 0
        val maxAttempts = 1000
        while (placed < count && attempts < maxAttempts) {
            attempts++
            val x = random.nextInt(0, width)
            val y = random.nextInt(0, height)
            if (canPlaceQuestGiver(x, y, playerStartX, playerStartY)) {
                terrain[x][y] = TerrainType.QUEST_GIVER
                questGivers[Pair(x, y)] = QuestGiver(x, y)
                placed++
            }
        }
    }

    private fun placeCaveEntrance(startX: Int, startY: Int) {
        val random = Random
        val count = random.nextInt(1, 3)
        var placed = 0
        var attempts = 0
        val maxAttempts = 500
        while (placed < count && attempts < maxAttempts) {
            attempts++
            val x = random.nextInt(0, width)
            val y = random.nextInt(0, height)
            if (canPlaceCaveEntrance(x, y, startX, startY)) {
                terrain[x][y] = TerrainType.CAVEENTRANCE
                placed++
            }
        }
    }

    private fun canPlaceCaveEntrance(x: Int, y: Int, startX: Int, startY: Int): Boolean {
        if (terrain[x][y] != TerrainType.LAND) return false
        if (isPlayerStartPosition(x, y, startX, startY, 10)) return false
        for (dx in -1..1) {
            for (dy in -1..1) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until width && ny in 0 until height) {
                    when (terrain[nx][ny]) {
                        TerrainType.CITY, TerrainType.CITYANCHOR, TerrainType.OUTPOST,
                        TerrainType.UPGRADE, TerrainType.CHEST, TerrainType.ENEMY,
                        TerrainType.MOUNTAIN, TerrainType.TRAP, TerrainType.WATER,
                        TerrainType.FOREST -> return false
                        else -> {}
                    }
                }
            }
        }
        return true
    }

    private fun placeEnemies(count: Int = 10, playerStartX: Int, playerStartY: Int) {
        val random = Random
        var placed = 0
        val chestPositions = mutableListOf<Pair<Int, Int>>()
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (terrain[x][y] == TerrainType.CHEST) {
                    chestPositions.add(Pair(x, y))
                }
            }
        }
        val usedPositions = mutableSetOf<Pair<Int, Int>>()
        for ((chestX, chestY) in chestPositions) {
            val enemiesPerChest = random.nextInt(1, 4)
            repeat(enemiesPerChest) {
                var found = false
                repeat(20) { _ ->
                    if (found) return@repeat
                    val dx = random.nextInt(-1, 2)
                    val dy = random.nextInt(-1, 2)
                    if (dx == 0 && dy == 0) return@repeat
                    val enemyX = chestX + dx
                    val enemyY = chestY + dy
                    val enemyPos = Pair(enemyX, enemyY)
                    if (enemyX in 0 until width && enemyY in 0 until height &&
                        (terrain[enemyX][enemyY] == TerrainType.LAND || terrain[enemyX][enemyY] == TerrainType.FOREST) &&
                        !usedPositions.contains(enemyPos) &&
                        !isPlayerStartPosition(enemyX, enemyY, playerStartX, playerStartY, 5) &&
                        isWalkable(enemyX, enemyY)) {
                        originalTerrain[enemyX][enemyY] = terrain[enemyX][enemyY]
                        terrain[enemyX][enemyY] = TerrainType.ENEMY
                        usedPositions.add(enemyPos)
                        placed++
                        found = true
                    }
                }
                if (!found) {
                    repeat(20) { _ ->
                        val dx = random.nextInt(-2, 3)
                        val dy = random.nextInt(-2, 3)
                        if (dx == 0 && dy == 0) return@repeat
                        val enemyX = chestX + dx
                        val enemyY = chestY + dy
                        val enemyPos = Pair(enemyX, enemyY)
                        if (enemyX in 0 until width && enemyY in 0 until height &&
                            (terrain[enemyX][enemyY] == TerrainType.LAND || terrain[enemyX][enemyY] == TerrainType.FOREST) &&
                            !usedPositions.contains(enemyPos) &&
                            !isPlayerStartPosition(enemyX, enemyY, playerStartX, playerStartY, 5) &&
                            isWalkable(enemyX, enemyY)) {
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
        if (placed < count) {
            val availablePositions = mutableListOf<Pair<Int, Int>>()
            for (x in 0 until width) {
                for (y in 0 until height) {
                    if (canPlaceEnemy(x, y, playerStartX, playerStartY) &&
                        !usedPositions.contains(Pair(x, y))) {
                        availablePositions.add(Pair(x, y))
                    }
                }
            }
            if (availablePositions.isNotEmpty()) {
                val shuffled = availablePositions.shuffled(random)
                val toPlace = minOf(count - placed, shuffled.size)
                for (idx in 0 until toPlace) {
                    val (x, y) = shuffled[idx]
                    originalTerrain[x][y] = terrain[x][y]
                    terrain[x][y] = TerrainType.ENEMY
                    placed++
                }
            }
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

    private fun canPlaceEnemy(x: Int, y: Int, playerStartX: Int, playerStartY: Int): Boolean {
        val t = terrain[x][y]
        if (t != TerrainType.LAND) return false
        if (isPlayerStartPosition(x, y, playerStartX, playerStartY, 5)) return false
        for (dx in -2..2) {
            for (dy in -2..2) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until width && ny in 0 until height) {
                    if (terrain[nx][ny] == TerrainType.CITY ||
                        terrain[nx][ny] == TerrainType.OUTPOST ||
                        terrain[nx][ny] == TerrainType.UPGRADE ||
                        terrain[nx][ny] == TerrainType.TRAP ||
                        terrain[nx][ny] == TerrainType.ENEMY) {
                        return false
                    }
                }
            }
        }
        return true
    }

    private fun placeTraps(count: Int = 3, playerStartX: Int, playerStartY: Int) {
        val random = Random
        var placed = 0
        val availablePositions = mutableListOf<Pair<Int, Int>>()
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (canPlaceTraps(x, y, playerStartX, playerStartY)) {
                    availablePositions.add(Pair(x, y))
                }
            }
        }
        if (availablePositions.isNotEmpty()) {
            val shuffled = availablePositions.shuffled(random)
            val toPlace = minOf(count, shuffled.size)
            for (idx in 0 until toPlace) {
                val (x, y) = shuffled[idx]
                terrain[x][y] = TerrainType.TRAP
                setTrapType(x, y, TrapManager.randomTrapType())
                placed++
            }
        }
        if (placed == 0) {
            for (x in 0 until width) {
                for (y in 0 until height) {
                    if (terrain[x][y] == TerrainType.LAND && !isPlayerStartPosition(x, y, playerStartX, playerStartY, 2)) {
                        terrain[x][y] = TerrainType.TRAP
                        return
                    }
                }
            }
        }
    }

    private fun canPlaceTraps(x: Int, y: Int, playerStartX: Int, playerStartY: Int): Boolean {
        if (terrain[x][y] != TerrainType.LAND) return false
        if (isPlayerStartPosition(x, y, playerStartX, playerStartY, 4)) return false
        for (dx in -1..1) {
            for (dy in -1..1) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until width && ny in 0 until height) {
                    if (terrain[nx][ny] == TerrainType.CITY ||
                        terrain[nx][ny] == TerrainType.ENEMY ||
                        terrain[nx][ny] == TerrainType.UPGRADE ||
                        terrain[nx][ny] == TerrainType.OUTPOST ||
                        terrain[nx][ny] == TerrainType.TRAP) {
                        return false
                    }
                }
            }
        }
        return true
    }

    private fun placeUpgrade() {
        val random = Random
        val min = 2
        val max = 5
        var placed = 0
        val lands = mutableListOf<Pair<Int, Int>>()
        for (x in 0 until width) {
            for (y in 0 until height) {
                if (canPlaceUpgrade(x, y)) {
                    lands.add(Pair(x, y))
                }
            }
        }
        val shuffled = lands.shuffled(random)
        for ((x, y) in shuffled) {
            if (placed >= min) break
            terrain[x][y] = TerrainType.UPGRADE
            placed++
        }
        while (placed < max && random.nextDouble() < 0.5 && placed < shuffled.size) {
            val (x, y) = shuffled[placed]
            if (terrain[x][y] == TerrainType.LAND) {
                terrain[x][y] = TerrainType.UPGRADE
                placed++
            }
        }
    }

    private fun canPlaceUpgrade(x: Int, y: Int): Boolean {
        return terrain[x][y] == TerrainType.LAND
    }

    private fun placeOutpost() {
        val random = Random
        if (random.nextFloat() > 0.25f) return
        var attempts = 0
        while (attempts < 1000) {
            attempts++
            val x = random.nextInt(0, width)
            val y = random.nextInt(0, height)
            val horizontal = random.nextBoolean()
            if (canPlaceOutpost(x, y, horizontal)) {
                if (horizontal) {
                    terrain[x][y] = TerrainType.OUTPOST
                    if (x + 1 in 0 until width) terrain[x + 1][y] = TerrainType.OUTPOST
                } else {
                    terrain[x][y] = TerrainType.OUTPOST
                    if (y + 1 in 0 until height) terrain[x][y + 1] = TerrainType.OUTPOST
                }
                return
            }
        }
    }

    private fun canPlaceOutpost(x: Int, y: Int, horizontal: Boolean): Boolean {
        val positions = if (horizontal) {
            listOf(Pair(x, y), Pair(x + 1, y))
        } else {
            listOf(Pair(x, y), Pair(x, y + 1))
        }
        for ((nx, ny) in positions) {
            if (nx !in 0 until width || ny !in 0 until height) return false
            if (terrain[nx][ny] != TerrainType.LAND) return false
        }
        return true
    }
}
