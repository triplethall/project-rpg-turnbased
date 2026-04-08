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
    val accuracy: Double,
    val enemyType: EnemyType
) : DamageReceiver
{
    val debuffManager = DebuffManager()
    private var skipTurn = false
    // NAME DISPLAY
    val name: String get() = type.displayEnemyName
    // CHECK FOR ALIVE
    fun isAlive(): Boolean = currentHealth > 0
    // METHOD FOR ENEMY TO TAKE DAMAGE (с учетом типа врага)
    fun takeDamage(amount: Int, attackerType: EnemyType = EnemyType.NO_TYPE, isMagic: Boolean = false) {
        currentHealth = (currentHealth - amount).coerceAtLeast(0)

        // Выводим информацию о типе врага (только для информации)
        if (attackerType != EnemyType.NO_TYPE && this.enemyType != EnemyType.NO_TYPE) {
            println("${this.name} (${this.enemyType.displayName}) получает $amount урона")
        }
    }
    // METHOD FOR ENEMY TO TAKE DAMAGE
    fun takeDamage(amount: Int) {
        currentHealth = (currentHealth - amount).coerceAtLeast(0)
    }
    // METHOD FOR CALCULATING DAMAGE
    fun calculateDamage(targetEnemyType: EnemyType? = null, isMagic: Boolean): Int {
        val randomMultiplier = 0.8 + Random.nextDouble() * 0.4
        val baseDamage = if (isMagic) magicDamage else damage
        val damage = (baseDamage * randomMultiplier).toInt().coerceAtLeast(1)
        return (damage * getDamageMultiplier()).toInt()
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
                accuracy = enemyType.baseEnemyAccuracy,
                enemyType = enemyType.enemyType
            )
        }

        fun createRandomEnemies(count: Int): MutableList<BattleEnemy> {
            val enemyTypes = listOf(
                Enemy.GREEN_SLIME,
                Enemy.RED_SLIME,
                Enemy.BLUE_SLIME,
                Enemy.WIND_SLIME,
                Enemy.EARTH_SLIME,
                Enemy.ICE_SLIME,
                Enemy.CURSED_SLIME,
                Enemy.ELECTRIC_SLIME,
                Enemy.HOLY_SLIME
            )

            return (1..count).map {
                val randomType = enemyTypes.random()
                BattleEnemy.fromType(randomType)
            }.toMutableList()
        }
    }


    override fun takeDebuffDamage(amount: Int) {
        currentHealth = (currentHealth - amount).coerceAtLeast(0)
        println("${name} получает $amount урона от дебаффа")
    }

    fun applyDebuff(type: DebuffType, duration: Int, intensity: Double = 1.0, stacks: Int = 1) {
        debuffManager.addDebuffs(type, duration, intensity, stacks)
    }

    fun processDebuffs(): Int {
        val debuffs = debuffManager.getAllDebuff()

        val damage = DebuffApplier.Companion.applyDamageDebuffs(this, debuffs, maxHealth)
        skipTurn = DebuffApplier.Companion.shouldSkipTurn(debuffs)
        debuffManager.tick()

        return damage
    }

    fun shouldSkipTurn(): Boolean = skipTurn

    fun getDamageMultiplier(): Double {
        val modifiers = DebuffApplier.Companion.getStatModifiers(debuffManager.getAllDebuff())
        return modifiers.damageMultiplier
    }

    fun getDefenseMultiplier(): Double {
        val modifiers = DebuffApplier.Companion.getStatModifiers(debuffManager.getAllDebuff())
        return modifiers.defenseMultiplier
    }

    fun tryApplyDebuffOnHit(debuffType: DebuffType, chance: Double, duration: Int, intensity: Double = 1.0) {
        if (Random.nextDouble() < chance) {
            // Применяем к игроку через колбэк (нужно передавать ссылку на игрока)
            println("${name} применяет ${debuffType.name}!")
        }
    }
}
