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

    // Флаг для отслеживания первой атаки по кролику
    private var firstAttackDodged = false

    private var skipTurn = false
    // NAME DISPLAY
    val name: String get() = type.displayEnemyName
    // CHECK FOR ALIVE
    fun isAlive(): Boolean = currentHealth > 0

    // METHOD FOR ENEMY TO TAKE DAMAGE (с учетом типа врага)
    fun takeDamage(amount: Int, attackerType: EnemyType = EnemyType.NO_TYPE, isMagic: Boolean = false): Boolean {
        var finalDamage = amount

        // Особый эффект для кроликов - 50% шанс уклониться от первой атаки в бою
        if (this.enemyType == EnemyType.BUNNY && !firstAttackDodged) {
            if (Random.nextDouble() < 0.5) {
                firstAttackDodged = true
                println("${this.name} уклоняется от первой атаки!")
                return false // Атака не наносит урон
            }
            firstAttackDodged = true
        }

        // Применяем особые эффекты типа врага ПРИ ПОЛУЧЕНИИ УРОНА
        when (this.enemyType) {
            EnemyType.FIRE -> {
                // "При получении урона огнем - восстанавливает 10% здоровья"
                if (attackerType == EnemyType.FIRE) {
                    val healAmount = (maxHealth * 0.1).toInt()
                    currentHealth = (currentHealth + healAmount).coerceAtMost(maxHealth)
                    println("${this.name} (${this.enemyType.displayName}) восстанавливает $healAmount здоровья от огненной атаки!")
                }
            }
            EnemyType.WIND, EnemyType.BUNNY -> {
                // Кролики тоже имеют шанс уклонения как WIND
                // Эффект применяется в canHit()
            }

            EnemyType.EARTH -> {
                // "Имеет +30% к защите от физических атак"

                if (!isMagic) {
                    finalDamage = (finalDamage * 0.7).toInt()
                }
            }
            else -> {}
        }

        currentHealth = (currentHealth - finalDamage).coerceAtLeast(0)

        if (attackerType != EnemyType.NO_TYPE && this.enemyType != EnemyType.NO_TYPE) {
            println("${this.name} (${this.enemyType.displayName}) получает $finalDamage урона")
        }

        return true
    }
    // METHOD FOR ENEMY TO TAKE DAMAGE
    fun takeDamage(amount: Int) {
        currentHealth = (currentHealth - amount).coerceAtLeast(0)
    }
    // METHOD FOR CALCULATING DAMAGE
    fun calculateDamage(targetEnemyType: EnemyType? = null, isMagic: Boolean): Int {
        val randomMultiplier = 0.8 + Random.nextDouble() * 0.4
        val baseDamage = if (isMagic) magicDamage else damage

        // Кролики-берсерки наносят больше урона при низком здоровье
        var damageMultiplier = 1.0
        if (enemyType == EnemyType.BERSERK) {
            val healthPercent = currentHealth.toDouble() / maxHealth
            if (healthPercent < 0.5) {
                damageMultiplier = 1.3 // +30% урона при здоровье ниже 50%
            }
        }

        val damage = (baseDamage * randomMultiplier).toInt().coerceAtLeast(1)
        return (damage * getDamageMultiplier()).toInt()
    }

    // METHOD TO CHECK IF ENEMY HIT
    fun canHit(target: Player): Boolean {
        var hitChance = accuracy
        // Учитываем уклонение игрока
        if (target.debuffManager.hasDebuff(DebuffType.DODGE)) {
            val dodgeChance = DebuffApplier.getDodgeChance(
                target.debuffManager.getAllDebuff(),
                target.attackSpeed,
                target.luck
            )
            hitChance *= (1.0 - dodgeChance)
        }
        return Random.nextDouble() < hitChance
    }

    // Метод для проверки уклонения врага (для атак игрока)
    fun canDodge(isPhysical: Boolean = true): Boolean {
        return when (enemyType) {
            EnemyType.WIND -> {
                // 25% уклонение от физических атак
                isPhysical && Random.nextDouble() < 0.25
            }
            EnemyType.BUNNY -> {
                // 15% базовое уклонение + бонус от скорости
                val baseChance = 0.15
                val speedBonus = (type.baseEnemyAttackSpeed - 1.0) * 0.1
                Random.nextDouble() < (baseChance + speedBonus)
            }
            else -> false
        }
    }

    // Метод для воскрешения нежити
    fun tryResurrect(): Boolean {
        if (enemyType == EnemyType.UNDEAD && currentHealth <= 0) {
            if (Random.nextDouble() < 0.3) {
                currentHealth = 1
                println("${this.name} воскресает с 1 HP!")
                return true
            }
        }
        return false
    }

    // Метод для кражи жизни (DARK тип)
    fun tryLifeSteal(damageDealt: Int): Int {
        if (enemyType == EnemyType.DARK && damageDealt > 0) {
            val stealAmount = (damageDealt * 0.1).toInt()
            currentHealth = (currentHealth + stealAmount).coerceAtMost(maxHealth)
            return stealAmount
        }
        return 0
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
            val allEnemies = Enemy.values().toList()

            return (1..count).map {
                val randomType = allEnemies.random()
                BattleEnemy.fromType(randomType)
            }.toMutableList()
        }

        // Метод для создания врагов определенного типа (например, только скелеты)
        fun createEnemiesByCategory(count: Int, category: String): MutableList<BattleEnemy> {
            val filteredEnemies = when (category.lowercase()) {
                "slime" -> Enemy.values().filter { it.displayEnemyName.contains("Slime") }
                "skeleton" -> Enemy.values().filter { it.displayEnemyName.contains("Skeleton") }
                "goblin" -> Enemy.values().filter { it.displayEnemyName.contains("Goblin") }
                "spider" -> Enemy.values().filter { it.displayEnemyName.contains("Spider") }
                "rabbit" -> Enemy.values().filter {
                    it.displayEnemyName.contains("Rabbit") ||
                        it.displayEnemyName.contains("Bunny")
                }

                "wolf" -> Enemy.values().filter {
                    it.displayEnemyName.contains("Wolf") ||
                        it.displayEnemyName.contains("Fenrir")
                }

                "troll" -> Enemy.values().filter { it.displayEnemyName.contains("Troll") }
                "orc" -> Enemy.values().filter { it.displayEnemyName.contains("Orc") }
                "elemental" -> Enemy.values().filter {
                    it.displayEnemyName.contains("Elemental") ||
                        it.displayEnemyName.contains("Spirit") ||
                        it.displayEnemyName.contains("Golem")
                }
                "elite" -> Enemy.values().filter { it.isElite }
                "undead" -> Enemy.values().filter { it.enemyType == EnemyType.UNDEAD }
                "fire" -> Enemy.values().filter { it.enemyType == EnemyType.FIRE }
                "ice" -> Enemy.values().filter { it.enemyType == EnemyType.ICE }
                "poison" -> Enemy.values().filter { it.enemyType == EnemyType.POISON }
                "dark" -> Enemy.values().filter { it.enemyType == EnemyType.DARK }
                "holy" -> Enemy.values().filter { it.enemyType == EnemyType.HOLY }
                else -> Enemy.values().toList()
            }

            if (filteredEnemies.isEmpty()) {
                return createRandomEnemies(count)
            }

            return (1..count).map {
                val randomType = filteredEnemies.random()
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
