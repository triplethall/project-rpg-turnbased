package ru.triplethall.rpgturnbased

import kotlin.random.Random


// !!! ФАЙЛ ГДЕ ЕСТЬ МЕТОДЫ ЧТОБЫ ВРАГИ ПОЛУЧАЛИ УРОН И НАНОСИЛИ УРОН
class BattleEnemy(
    val type: Enemy,
    var currentHealth: Int,
    val maxHealth: Int,
    val damage: Int,
    val magicDamage: Int,
    val defense: Double,
    val accuracy: Double
)
{
    // NAME DISPLAY
    val name: String get() = type.displayEnemyName
    // CHECK FOR ALIVE
    fun isAlive(): Boolean = currentHealth > 0
    // METHOD FOR ENEMY TO TAKE DAMAGE
    fun takeDamage(amount: Int) {
        currentHealth = (currentHealth - amount).coerceAtLeast(0)
    }
    // METHOD FOR CALCULATING DAMAGE
    fun calculateDamage(): Int {
        val randomMultiplier = 0.8 + Random.nextDouble() * 0.4
        return (damage * randomMultiplier).toInt()
    }
    // METHOD TO CHECK IF ENEMY HIT
    fun canHit(): Boolean {
        return Random.nextDouble() < accuracy
    }

    companion object {
        fun fromType(enemyType: Enemy): BattleEnemy {
            return BattleEnemy(
                type = enemyType,
                currentHealth = enemyType.baseEnemyMaxHealth,
                maxHealth = enemyType.baseEnemyMaxHealth,
                damage = enemyType.baseEnemyDamage,
                magicDamage = enemyType.baseEnemyMageDamage,
                defense = enemyType.baseEnemyDefense,
                accuracy = enemyType.baseEnemyAccuracy
            )
        }

        fun createRandomEnemies(count: Int): MutableList<BattleEnemy> {
            val enemyTypes = listOf(
                Enemy.GREEN_SLIME,
                Enemy.RED_SLIME,
                Enemy.BLUE_SLIME
            )

            return (1..count).map {
                val randomType = enemyTypes.random()
                BattleEnemy.fromType(randomType)
            }.toMutableList()
        }
    }

}
