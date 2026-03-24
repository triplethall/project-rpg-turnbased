package ru.triplethall.rpgturnbased

class VisibilityManager(private val gameMap: GameMap) {
    private var visibleTiles: Set<Pair<Int, Int>> = emptySet()
    private val viewRadius = 7.0 // Радиус обзора в клетках

    fun updateVisibility(playerPosition: Pair<Int, Int>) {
        val px = playerPosition.first
        val py = playerPosition.second

        // 1. Сначала берем квадрат с запасом (чуть больше радиуса)
        val range = viewRadius.toInt()
        val minX = 0.coerceAtLeast(px - range)
        val maxX = (gameMap.width - 1).coerceAtMost(px + range)
        val minY = 0.coerceAtLeast(py - range)
        val maxY = (gameMap.height - 1).coerceAtMost(py + range)

        // 2. Оставляем только те клетки, которые попадают в КРУГ
        visibleTiles = (minX..maxX).flatMap { x ->
            (minY..maxY).mapNotNull { y ->
                val dx = x - px
                val dy = y - py
                // Условие круга: x² + y² <= r²
                if (dx * dx + dy * dy <= viewRadius * viewRadius) {
                    Pair(x, y)
                } else null
            }
        }.toSet()

        // 3. Обновляем исследованные клетки
        visibleTiles.forEach { (x, y) -> gameMap.markExplored(x, y) }
    }

    fun isVisible(x: Int, y: Int): Boolean = visibleTiles.contains(Pair(x, y))
}
