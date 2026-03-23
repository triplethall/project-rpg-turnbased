package ru.triplethall.rpgturnbased

class VisibilityManager(private val gameMap: GameMap) {
    private lateinit var visibleTiles: Set<Pair<Int, Int>>

    fun updateVisibility(playerPosition: Pair<Int, Int>) {
        val x = playerPosition.first
        val y = playerPosition.second

        // Зона видимости игрока — квадрат 5x5
        val minX = 0.coerceAtLeast(x - 2) // Диапазон увеличился
        val maxX = (gameMap.width - 1).coerceAtMost(x + 2)
        val minY = 0.coerceAtLeast(y - 2)
        val maxY = (gameMap.height - 1).coerceAtMost(y + 2)

        visibleTiles = (minX..maxX).flatMap { row ->
            (minY..maxY).map { col -> Pair(row, col) }
        }.toSet()

        // Обновляем список исследованных клеток
        visibleTiles.forEach { (row, col) -> gameMap.markExplored(row, col) }
    }

    fun isVisible(x: Int, y: Int): Boolean = visibleTiles.contains(Pair(x, y))
}
