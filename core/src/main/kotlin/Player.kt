package ru.triplethall.rpgturnbased

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import kotlin.math.exp
import kotlin.math.pow
import kotlin.random.Random

class Player(
    var x: Int = 0,
    var y: Int = 0,
    var playerClass : PlayerClasses = PlayerClasses.ADVENTURIST // класс по умолчанию
) {
    // Базовые характеристики
    var damage: Int = 20                    // Урон
    var mageDamage: Int = 10                // урон магии
    var defense: Double = 0.0               // Защита (процентная)
    var attackSpeed: Double = 1.0           // Скорость (атаки)
    var accuracy: Double = 0.8              // Точность (шанс попадания по врагу)
    var will: Double = 0.5                  // Воля (сопротивление дебафам)
    var corruption: Int = 0                 // Скверна
    var luck: Double = 0.00                 // Удача
    var critChance: Double = 0.01           // крит шанс
    var level: Int = 1                      // Уровень
    var experience: Int = 0                 // Опыт

    // Максимальные значения
    var maxMana: Int = 50
    var maxHealth: Int = 100
    var currentHealth: Int = 100
    var currentMana: Int = 50

    val equipment = PlayerEquipment()

    // формула для расчета опыта
    companion object {
        private const val BASE_EXP = 100
        private const val EXP_GROWTH_FACTOR = 1.5
    }


    // Расчет необходимого опыта для некст левела
    fun getExpForNextLevel(): Int {
        return (BASE_EXP * (EXP_GROWTH_FACTOR.pow(level - 1))).toInt()
    }


    // Получение прогресса опыта в процентах (полоска опыта)
    fun getExpProgress(): Float {
        val expNeeded = getExpForNextLevel()
        return experience.toFloat() / expNeeded.toFloat()
    }


    // Добавление опыта + проверка на повышение уровня
    fun addExperience(amount: Int) {
        experience += amount

        // Проверка достаточно ли опыта для нескольких уровней
        while (experience >= getExpForNextLevel()) {
            levelUp()
        }
    }


    // Повышение уровня
    private fun levelUp() {
        val expNeeded = getExpForNextLevel()
        experience -= expNeeded
        level++

        // Stats up за уровень
        damage += 2
        defense += 0.02
        maxMana += 5
        currentMana = maxMana  // восстановление маны за ур
        maxHealth += 10
        currentHealth = maxHealth
        attackSpeed += 0.05
        accuracy += 0.02
        will = 0.02
        mageDamage += 4
        luck += 0.02
        critChance += 0.01
    }


    // Скверна модификаторы
    fun getCorruptionHealthModifier(): Double {
        return 1.0 - (corruption * 0.09)
    }
    fun getCorruptionDamageModifier(): Double {
        return 1.0 + (corruption * 0.07)
    }
    fun getCorruptionMageDamageModifier(): Double {
        return 1.0 + (corruption * 0.04)
    }


    // скверна при смерти
    fun applyCorruptionOnDeath() {
        corruption++
    }


    // Проверка хватает ли опыта
    fun canLevelUp(): Boolean {
        return experience >= getExpForNextLevel()
    }


    // Сброс скверны (например после какого-то события)
    fun removeCorruption(amount: Int) {
        corruption = maxOf(0, corruption - amount)
    }


    fun spawnOnShore(gameMap: GameMap) {
        val random = Random
        val candidates = mutableListOf<Pair<Int, Int>>()

        for (x in 0 until gameMap.width) {
            for (y in 0 until gameMap.height) {
                if (!gameMap.isWalkable(x, y)) continue

                var hasWaterNeighbor = false
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        if (dx == 0 && dy == 0) continue
                        val nx = x + dx
                        val ny = y + dy
                        if (nx !in 0 until gameMap.width || ny !in 0 until gameMap.height) {
                            hasWaterNeighbor = true
                        } else if (!gameMap.isWalkable(nx, ny)) {
                            hasWaterNeighbor = true
                        }
                    }
                }

                if (!hasWaterNeighbor) continue

                val isOnEdge = (x == 0 || x == gameMap.width - 1 || y == 0 || y == gameMap.height - 1)
                if (isOnEdge) {
                    candidates.add(Pair(x, y))
                }
            }
        }

        if (candidates.isNotEmpty()) {
            val (sx, sy) = candidates.random(random)
            this.x = sx
            this.y = sy
        } else {
            this.x = gameMap.width / 2
            this.y = gameMap.height / 2
        }
    }

    fun isAdjacentCardinal(targetX: Int, targetY: Int): Boolean {
        val dx = kotlin.math.abs(targetX - x)
        val dy = kotlin.math.abs(targetY - y)
        return (dx == 1 && dy == 0) || (dx == 0 && dy == 1)
    }

    fun tryMoveTo(targetX: Int, targetY: Int, gameMap: GameMap): Boolean {
        if (!isAdjacentCardinal(targetX, targetY)) {
            return false
        }

        if (gameMap.isWalkable(targetX, targetY)) {
            x = targetX
            y = targetY
            // Проверяем, был ли на этой клетке сундук, и убираем его
            if (gameMap.collectChest(targetX, targetY)) {
                // Здесь можно добавить логику награды (например, увеличить счёт, выдать предмет)

            }
            return true
        }
        return false
    }

    fun changeClass(newClass: PlayerClasses) {
        playerClass = newClass
        newClass.applyToPlayer(this)
    }


    fun render(batch: SpriteBatch, font: BitmapFont, cellSize: Int, cellGap: Int) {
        val posX = x * (cellSize + cellGap)
        val posY = y * (cellSize + cellGap)

        font.color = Color.YELLOW
        font.draw(batch, "P", posX + 10f, posY + cellSize - 5f)
    }
}
