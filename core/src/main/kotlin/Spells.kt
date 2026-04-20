package ru.triplethall.rpgturnbased

import kotlin.random.Random

// Базовый абстрактный класс для всех навыков
abstract class Skill(
    val id: String,
    val name: String,
    val description: String,
    val manaCost: Int,
    val cooldown: Int,          // В ходах
    val canTargetDead: Boolean = false
)
{
    var currentCooldown = 0
    fun isReady(): Boolean = currentCooldown == 0
    fun isOnCooldown(): Boolean = currentCooldown > 0

    fun startCooldown() {
        currentCooldown = cooldown
    }

    fun reduceCooldown() {
        if (currentCooldown > 0) currentCooldown--
    }

    // Основной метод выполнения навыка
    abstract fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult

    // Проверка, можно ли использовать навык
    open fun canUse(caster: Player, targets: List<BattleEnemy>): Boolean {
        if (!isReady()) return false
        if (caster.currentMana < manaCost) return false
        if (caster.shouldSkipTurn()) return false
        if (!caster.canUseMagic() && manaCost > 0) return false // Магия заблокирована сайленсом
        return true
    }
}

// Результат применения навыка
data class SkillResult(
    val success: Boolean,
    val message: String,
    val damageDealt: Int = 0,
    val targetsAffected: List<BattleEnemy> = emptyList()
)

// Интерфейс для навыков, требующих выбора цели (используется в UI)
interface TargetableSkill {
    fun getValidTargets(caster: Player, enemies: List<BattleEnemy>): List<BattleEnemy>
}

// --- БАЗОВЫЙ НАВЫК: Уклонение ---
class DodgeSkill : Skill("dodge", "Dodge", "Increases dodge chance by 80% for 3 turns ", 10, 4) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        // Уклонение реализовано через бафф (дебафф наоборот) или специальный флаг.
        // Проще всего сделать через специальный DebuffType.
        // Добавим новый тип в DebuffType.
        caster.applyDebuff(DebuffType.DODGE, 3, 1.0)
        battleLog.addMessage("$name: Dodge chance increased!", com.badlogic.gdx.graphics.Color.CYAN)
        startCooldown()
        return SkillResult(true, "$name activated")
    }
}

// --- НАВЫКИ РЫЦАРЯ (KNIGHT) ---
class SlashSkill : Skill("slash", "Slash", "Deals damage to all enemies", 15, 2) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        var totalDamage = 0
        val baseDamage = caster.damage + (caster.mageDamage / 2) // Зависит от урона

        targets.forEach { enemy ->
            if (enemy.isAlive()) {
                val damage = calculateDamage(baseDamage, caster, enemy, isMagic = false, multiplier = 0.8) // 80% от урона
                enemy.takeDamage(damage)
                battleLog.addMessage("Slash deal $damage damage by ${enemy.name}", com.badlogic.gdx.graphics.Color.ORANGE)
                totalDamage += damage
            }
        }
        startCooldown()
        return SkillResult(true, "$name deal $totalDamage damage", totalDamage, targets)
    }
}
class KnightValorSkill : Skill("valor", "The valor of a knight", "+15% defense for 3 turns", 20, 5) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        // Используем существующий DebuffManager для баффов (можно добавить тип BUFF_DEFENSE)
        caster.applyDebuff(DebuffType.BUFF_DEFENSE, 3, 1.15)
        battleLog.addMessage("$name: Defense increased by 3 moves!", com.badlogic.gdx.graphics.Color.CYAN)
        startCooldown()
        return SkillResult(true, "Defense increased")
    }
}

// --- НАВЫКИ МАГА (MAGE) ---
class FireArrowSkill : Skill("fire_arrow", "Fire Arrow", "Deals magic damage and sets things on fire", 25, 1) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val target = targets.firstOrNull { it.isAlive() } ?: return SkillResult(false, "There is no goal")
        val damage = calculateDamage(caster.mageDamage, caster, target, isMagic = true, multiplier = 1.5)

        target.takeDamage(damage)
        target.applyDebuff(DebuffType.BURN, 3, 1.0)
        battleLog.addMessage("$name deal $damage damages and sets it on fire ${target.name}", com.badlogic.gdx.graphics.Color.ORANGE)
        startCooldown()
        return SkillResult(true, "Arson!", damage, listOf(target))
    }
}
class WaterStrikeSkill : Skill("water_strike", "Water stroke", "Deals magic damage and applies Wet", 20, 1) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val target = targets.firstOrNull { it.isAlive() } ?: return SkillResult(false, "There is no goal")
        val damage = calculateDamage(caster.mageDamage, caster, target, isMagic = true, multiplier = 1.3)

        target.takeDamage(damage)
        target.applyDebuff(DebuffType.WET, 3, 1.0)
        battleLog.addMessage("$name deal $damage урона. ${target.name} промок!", com.badlogic.gdx.graphics.Color.CYAN)

        startCooldown()
        return SkillResult(true, "The enemy got wet", damage, listOf(target))
    }
}
class WindSlashSkill : Skill("wind_slash", "Wind slash", "Magic damage to all enemies", 30, 2) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        var totalDamage = 0
        val baseDamage = (caster.mageDamage * 0.7).toInt()

        targets.forEach { enemy ->
            if (enemy.isAlive()) {
                val damage = calculateDamage(baseDamage, caster, enemy, isMagic = true, multiplier = 1.0)
                enemy.takeDamage(damage)
                battleLog.addMessage("Wind deal $damage damage by ${enemy.name}", com.badlogic.gdx.graphics.Color.GREEN)
                totalDamage += damage
            }
        }

        startCooldown()
        return SkillResult(true, "Damage to all enemies: $totalDamage", totalDamage, targets)
    }
}
class StoneBulletSkill : Skill("stone_bullet", "Stone bullet", "Throws 3 bullets at random enemies", 25, 2) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val aliveEnemies = targets.filter { it.isAlive() }
        if (aliveEnemies.isEmpty()) return SkillResult(false, "There are no goals")

        var totalDamage = 0
        repeat(3) {
            val target = aliveEnemies.random()
            val damage = calculateDamage(caster.mageDamage, caster, target, isMagic = true, multiplier = 0.6)
            target.takeDamage(damage)
            battleLog.addMessage("Stone bullet deal $damage damage by ${target.name}", com.badlogic.gdx.graphics.Color.BROWN)
            totalDamage += damage
        }

        startCooldown()
        return SkillResult(true, "Three bullets struck $totalDamage damage", totalDamage)
    }
}
class IceSpikeSkill : Skill("ice_spike", "Ice spikes", "Throws 2 icicles, slowing down enemies", 30, 2) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val aliveEnemies = targets.filter { it.isAlive() }.shuffled().take(2)
        if (aliveEnemies.isEmpty()) return SkillResult(false, "There are no goals")
        var totalDamage = 0
        aliveEnemies.forEach { target ->
            val damage = calculateDamage(caster.mageDamage, caster, target, isMagic = true, multiplier = 1.1)
            target.takeDamage(damage)
            target.applyDebuff(DebuffType.SLOW, 2, 1.0)
            battleLog.addMessage("Ice spikes deal $damage and slow ${target.name}", com.badlogic.gdx.graphics.Color.CYAN)
            totalDamage += damage
        }
        startCooldown()
        return SkillResult(true, "Enemy slow", totalDamage, aliveEnemies)
    }
}
class LightningBoltSkill : Skill("lightning", "Lightning", "Magic damage to all, 20% chance of stunning", 35, 3) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        var totalDamage = 0
        val wetMultiplier = caster.getLightningDamageModifier()
        targets.forEach { enemy ->
            if (enemy.isAlive()) {
                val isWet = enemy.debuffManager.hasDebuff(DebuffType.WET)
                val damageMultiplier = if (isWet) 1.25 * wetMultiplier else 1.0

                val damage = calculateDamage(caster.mageDamage, caster, enemy, isMagic = true, damageMultiplier)
                enemy.takeDamage(damage)

                if (isWet) battleLog.addMessage("${enemy.name} I'm wet! Damage increased!", com.badlogic.gdx.graphics.Color.BLUE)

                if (Random.nextDouble() < 0.2) {
                    enemy.applyDebuff(DebuffType.STUN, 1, 1.0)
                    battleLog.addMessage("${enemy.name} Stunned by lightning!", com.badlogic.gdx.graphics.Color.YELLOW)
                }
                totalDamage += damage
            }
        }
        startCooldown()
        return SkillResult(true, "Lightning deal $totalDamage damage", totalDamage)
    }
}

// --- НАВЫКИ АССАСИНА (ASSASSIN) ---
class BackstabSkill : Skill("backstab", "Backstab", "X2 Damage and bleeding", 15, 4) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val target = targets.firstOrNull { it.isAlive() } ?: return SkillResult(false, "There are no goals!")
        val damage = calculateDamage(caster.damage, caster, target, isMagic = false, multiplier = 2.0)

        target.takeDamage(damage)
        target.applyDebuff(DebuffType.BLEED, 3, 1.0)
        battleLog.addMessage("A stab in the back causes $damage It causes damage and causes bleeding!", com.badlogic.gdx.graphics.Color.RED)

        startCooldown()
        return SkillResult(true, "The bleeding is applied", damage, listOf(target))
    }
}
class ShurikenThrowSkill : Skill("shuriken", "Shuriken Throw", "Deals 1.5 damage", 10, 2) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val target = targets.firstOrNull { it.isAlive() } ?: return SkillResult(false, "There is no goal")
        val damage = calculateDamage(caster.damage, caster, target, isMagic = false, multiplier = 1.5)

        target.takeDamage(damage)
        battleLog.addMessage("Shuriken deal $damage damage by ${target.name}", com.badlogic.gdx.graphics.Color.GRAY)

        startCooldown()
        return SkillResult(true, "Hit!", damage, listOf(target))
    }
}
class StealthSkill : Skill("stealth", "Stealth", "+10% crit chance and +9% speed", 20, 3) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        caster.applyDebuff(DebuffType.BUFF_CRIT, 3, 1.1) // 1.1 = +10%
        caster.applyDebuff(DebuffType.BUFF_SPEED, 3, 1.09) // +9%

        battleLog.addMessage("Stealth: Crit and Speed increased by 3 turns!", com.badlogic.gdx.graphics.Color.PURPLE)
        startCooldown()
        return SkillResult(true, "The buff is activated")
    }
}

// --- НАВЫКИ ЛУЧНИКА (ARCHER) ---
class ArrowRainSkill : Skill("arrow_rain", "Arrows rain", "Damage all enemies", 25, 2) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        var totalDamage = 0
        targets.forEach { enemy ->
            if (enemy.isAlive()) {
                val damage = calculateDamage(caster.damage, caster, enemy, isMagic = false, multiplier = 0.6)
                enemy.takeDamage(damage)
                totalDamage += damage
            }
        }
        battleLog.addMessage("Arrows rain deal $totalDamage Total damage!", com.badlogic.gdx.graphics.Color.GREEN)
        startCooldown()
        return SkillResult(true, "Area attack", totalDamage)
    }
}
class AimedShotSkill : Skill("aimed_shot", "Aiming", "Charges 1 turn, then 100% hit, +50% crit, x2 damage", 20, 4) {
    var isCharging = false
    override fun canUse(caster: Player, targets: List<BattleEnemy>): Boolean {
        if (isCharging) return true // Если заряжается, можно использовать
        return super.canUse(caster, targets)
    }
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        if (!isCharging) {
            isCharging = true
            battleLog.addMessage("$name: Aiming... The next move is to fire!", com.badlogic.gdx.graphics.Color.YELLOW)
            return SkillResult(true, "Charging...")
        } else {
            isCharging = false
            val target = targets.firstOrNull { it.isAlive() } ?: return SkillResult(false, "There is no goal")
            // Гарантированное попадание + шанс крита
            val isCrit = Random.nextDouble() < (caster.critChance + 0.5)
            val critMultiplier = if (isCrit) 2.0 else 1.0
            val totalMultiplier = 2.0 * critMultiplier * caster.getDamageMultiplier()

            val damage = (caster.damage * totalMultiplier).toInt()
            target.takeDamage(damage)
            battleLog.addMessage("A well-aimed shot! ${if(isCrit) "CRIT! " else ""}$damage damage by ${target.name}", com.badlogic.gdx.graphics.Color.GOLD)

            startCooldown()
            return SkillResult(true, "A powerful shot!", damage, listOf(target))
        }
    }
}

// --- НАВЫКИ ЖРЕЦА (PRIEST) ---
class HealSkill : Skill("heal", "Heal", "Restores 20% of health", 30, 3) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val healAmount = (caster.maxHealth * 0.2).toInt()
        caster.currentHealth = (caster.currentHealth + healAmount).coerceAtMost(caster.maxHealth)
        battleLog.addMessage("$name restores $healAmount HP!", com.badlogic.gdx.graphics.Color.GREEN)
        startCooldown()
        return SkillResult(true, "Heal +$healAmount HP", healAmount)
    }
}
class ResurrectionSkill : Skill("resurrection", "Resurrection", "Upon death, resurrects with 50% HP for 3 turns", 100, 10) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        caster.applyDebuff(DebuffType.RESURRECTION, 3, 1.0)
        caster.currentMana = 0 // Тратит всю ману
        battleLog.addMessage("$name: Resurrection is active for 3 turns!", com.badlogic.gdx.graphics.Color.GOLD)
        startCooldown()
        return SkillResult(true, "The blessing is active")
    }
}
class CleanseSkill : Skill("cleanse", "Cleansing", "Removes all debuffs", 20, 3) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        caster.clearDebuffs()
        battleLog.addMessage("$name: All debuffs removed!", com.badlogic.gdx.graphics.Color.WHITE)
        startCooldown()
        return SkillResult(true, "Purification is completed")
    }
}

// --- НАВЫКИ ДЖОКЕРА (JOKER) ---
class CoinTossSkill : Skill("coin", "Coin Flip", "50% chance to deal damage, depends on luck", 5, 0) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val target = targets.firstOrNull { it.isAlive() } ?: return SkillResult(false, "There is no goal")
        val luckFactor = 1.0 + caster.luck
        if (Random.nextDouble() < 0.5) {
            val damage = (caster.damage * 0.8 * luckFactor).toInt()
            target.takeDamage(damage)
            battleLog.addMessage("Eagle! $damage damage by ${target.name}", com.badlogic.gdx.graphics.Color.YELLOW)
            return SkillResult(true, "Success!", damage, listOf(target))
        } else {
            battleLog.addMessage("Tails... A miss!", com.badlogic.gdx.graphics.Color.GRAY)
            return SkillResult(true, "Failure")
        }
    }
}
class DiceRollSkill : Skill("dice", "The roll of the dice", "Chance of 1/6 to deal damage, depends on luck", 0, 0) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val target = targets.firstOrNull { it.isAlive() } ?: return SkillResult(false, "There is no goal")
        val roll = Random.nextInt(1, 7)
        val luckFactor = 1.0 + caster.luck * 2

        if (roll == 6) {
            val damage = (caster.damage * 1.5 * luckFactor).toInt()
            target.takeDamage(damage)
            battleLog.addMessage("Dice: $roll! CRIT! $damage damage by ${target.name}", com.badlogic.gdx.graphics.Color.GOLD)
            return SkillResult(true, "CRIT!", damage, listOf(target))
        } else {
            battleLog.addMessage("Dice: $roll. Bad luck...", com.badlogic.gdx.graphics.Color.GRAY)
            return SkillResult(true, "Fail")
        }
    }
}
class SlotMachineSkill : Skill("slot", "Slot machine", "3 numbers from 1 to 9. Three of the same - the effect!", 0, 0) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val a = Random.nextInt(1, 10)
        val b = Random.nextInt(1, 10)
        val c = Random.nextInt(1, 10)
        battleLog.addMessage("Slots: [$a] [$b] [$c]", com.badlogic.gdx.graphics.Color.WHITE)
        if (a != b || b != c) {
            battleLog.addMessage("Nothing happened...", com.badlogic.gdx.graphics.Color.GRAY)
            return SkillResult(true, "empty")
        }
        when (a) {
            1 -> targets.forEach { it.applyDebuff(DebuffType.BURN, 3, 1.0) }
            2 -> targets.firstOrNull()?.applyDebuff(DebuffType.STUN, 4, 1.0)
            3 -> caster.applyDebuff(DebuffType.BUFF_INVULNERABLE, 4, 1.0)
            4 -> caster.applyDebuff(DebuffType.BUFF_DEFENSE, 3, 1.5)
            5 -> caster.currentHealth = (caster.currentHealth + caster.maxHealth * 0.5).toInt().coerceAtMost(caster.maxHealth)
            6 -> {
                caster.currentHealth = 0
                battleLog.addMessage("FATAL ERROR! The player is dead!", com.badlogic.gdx.graphics.Color.RED)
                return SkillResult(true, "Death")
            }
            7 -> {
                targets.forEach { it.takeDamage(9999) }
                battleLog.addMessage("JACKPOT! All enemies have been destroyed!", com.badlogic.gdx.graphics.Color.GOLD)
            }
            8 -> caster.applyDebuff(DebuffType.BUFF_DAMAGE, 3, 2.0)
            9 -> caster.applyDebuff(DebuffType.BUFF_INFINITE_MANA, 5, 1.0)
        }
        battleLog.addMessage("Three identical ones! The effect worked $a!", com.badlogic.gdx.graphics.Color.PURPLE)
        startCooldown()
        return SkillResult(true, "Effect $a")
    }
}

// --- НАВЫКИ БЕРСЕРКА (BERSERK) ---
class BloodthirstSkill : Skill("bloodthirst", "Bloodlust", "Damage + Bleeding. More damage when HP is low", 20, 3) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val target = targets.firstOrNull { it.isAlive() } ?: return SkillResult(false, "There is no goal")
        val missingHealthPercent = 1.0 - (caster.currentHealth.toDouble() / caster.maxHealth)
        val damageMultiplier = 1.0 + missingHealthPercent

        val damage = calculateDamage(caster.damage, caster, target, isMagic = false, damageMultiplier)
        target.takeDamage(damage)
        target.applyDebuff(DebuffType.BLEED, 3, 1.0)
        battleLog.addMessage("Bloodlust! $damage damage and bleeding on ${target.name}", com.badlogic.gdx.graphics.Color.RED)

        startCooldown()
        return SkillResult(true, "The bleeding", damage, listOf(target))
    }
}
class SelfHarmSkill : Skill("selfharm", "Self hurting", "Spends 15% HP, +15% speed for 3 turns", 0, 4) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val hpCost = (caster.maxHealth * 0.15).toInt()
        caster.currentHealth -= hpCost
        caster.applyDebuff(DebuffType.BUFF_SPEED, 3, 1.15)
        battleLog.addMessage("$name: -$hpCost HP, +15% speed for 3 moves!", com.badlogic.gdx.graphics.Color.ORANGE)
        startCooldown()
        return SkillResult(true, "Speed increased")
    }
}

// --- НАВЫКИ ШАМАНА (SHAMAN) ---
class WeakCurseSkill : Skill("weak_curse", "Weak curse", "Random curse for 2 turns", 25, 2) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val target = targets.firstOrNull { it.isAlive() } ?: return SkillResult(false, "There is no goal")
        val hpCost = (caster.maxHealth * 0.05).toInt()
        caster.currentHealth -= hpCost

        val curses = listOf(DebuffType.WEAKNESS, DebuffType.CURSE, DebuffType.SLOW, DebuffType.SILENCE)
        val curse = curses.random()
        target.applyDebuff(curse, 2, 1.0)

        battleLog.addMessage("$name: -$hpCost HP. На ${target.name} imposed $curse!", com.badlogic.gdx.graphics.Color.PURPLE)
        startCooldown()
        return SkillResult(true, "The curse is cast")
    }
}
class AlterEgoSkill : Skill("alter_ego", "Alter ego", "Creates a clone (75% stats) at the cost of 75% HP", 70, 10) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        caster.currentHealth = (caster.currentHealth * 0.25).toInt()
        caster.applyDebuff(DebuffType.CLONE, 5, 0.75) // Клон как бафф
        battleLog.addMessage("Alter ego! A clone with 75% characteristics has been created!", com.badlogic.gdx.graphics.Color.DARK_GRAY)
        startCooldown()
        return SkillResult(true, "A clone has been created")
    }
}
class DarkSecretsSkill : Skill("dark_secrets", "Dark Mysteries", "-5% HP, +50% Magic Damage", 50, 3) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val target = targets.firstOrNull { it.isAlive() } ?: return SkillResult(false, "There is no goal")
        val hpCost = (caster.maxHealth * 0.05).toInt()
        caster.currentHealth -= hpCost

        val damage = calculateDamage(caster.mageDamage, caster, target, isMagic = true, multiplier = 1.5)
        target.takeDamage(damage)

        battleLog.addMessage("Dark Mysteries! -$hpCost HP, but $damage magic damage!", com.badlogic.gdx.graphics.Color.BLACK)
        startCooldown()
        return SkillResult(true, "Powerful magic", damage, listOf(target))
    }
}
class HarvestSkill : Skill("harvest", "The Harvest", "Steals 10% of the enemy's HP. Cooldown: 3 turns", 20, 3) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val target = targets.firstOrNull { it.isAlive() } ?: return SkillResult(false, "There is no goal")
        val stolen = (target.maxHealth * 0.1).toInt()
        target.takeDamage(stolen)
        caster.currentHealth = (caster.currentHealth + stolen).coerceAtMost(caster.maxHealth)

        battleLog.addMessage("Harvest! Stolen $stolen HP у ${target.name}", com.badlogic.gdx.graphics.Color.GREEN)
        startCooldown()
        return SkillResult(true, "HP stolen", stolen, listOf(target))
    }
}

// --- НАВЫКИ МАСТЕРА МЕЧА (SWORDMASTER) ---
class DoubleSlashSkill : Skill("double_slash", "Double slhash", "2 hits with a multiplier of 1.25", 25, 3) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val target = targets.firstOrNull { it.isAlive() } ?: return SkillResult(false, "There is no goal")
        var totalDamage = 0
        repeat(2) {
            val damage = calculateDamage(caster.damage, caster, target, isMagic = false, multiplier = 1.25)
            target.takeDamage(damage)
            totalDamage += damage
        }
        battleLog.addMessage("A double incision applies $totalDamage damage!", com.badlogic.gdx.graphics.Color.ORANGE)
        startCooldown()
        return SkillResult(true, "Two hits", totalDamage, listOf(target))
    }
}
class FlurrySkill : Skill("flurry", "A flurry of cuts", "Шквал порезов", 30, 7) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val target = targets.firstOrNull { it.isAlive() } ?: return SkillResult(false, "There is no goal")
        var totalDamage = 0
        repeat(9) {
            val damage = (caster.damage * 0.15 * caster.getDamageMultiplier()).toInt().coerceAtLeast(1)
            target.takeDamage(damage)
            totalDamage += damage
        }
        battleLog.addMessage("A barrage of slashes! 9 strikes on the $totalDamage damage!", com.badlogic.gdx.graphics.Color.RED)
        startCooldown()
        return SkillResult(true, "Flurry of cuts", totalDamage, listOf(target))
    }
}

// --- НАВЫКИ МОНАРХА (MONARCH) ---
class BattleStandardSkill : Skill("standard", "Battle flag", "Summons a standard-bearer (+15% damage/speed)", 40, 6) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        caster.applyDebuff(DebuffType.BANNER, 5, 1.15) // 5 ходов
        battleLog.addMessage("The battle flag is raised! +15% to damage and speed!", com.badlogic.gdx.graphics.Color.GOLD)
        startCooldown()
        return SkillResult(true, "The buff is activated")
    }
}
class SoulEmpowermentSkill : Skill("soul_emp", "Soul Enhancement", "+20% HP/Def/Will for 3 turns. CD 5", 35, 5) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        caster.applyDebuff(DebuffType.BUFF_HEALTH, 3, 1.2)
        caster.applyDebuff(DebuffType.BUFF_DEFENSE, 3, 1.2)
        caster.applyDebuff(DebuffType.BUFF_WILL, 3, 1.2)
        battleLog.addMessage("Soul Boost! +20% to Protection, Health, and Willpower!", com.badlogic.gdx.graphics.Color.CYAN)
        startCooldown()
        return SkillResult(true, "A powerful buff")
    }
}

// --- НАВЫКИ АЛХИМИКА (ALCHEMIST) ---
class PoisonVialSkill : Skill("poison_vial", "Poison Vial", "Throws a vial of poison, dealing magic damage and poisoning the target", 20, 1) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val target = targets.firstOrNull { it.isAlive() } ?: return SkillResult(false, "No target")
        val damage = calculateDamage(caster.mageDamage, caster, target, isMagic = true, multiplier = 1.2)
        target.takeDamage(damage)
        target.applyDebuff(DebuffType.POISON, 3, 1.0)
        battleLog.addMessage("$name deals $damage damage and poisons ${target.name}!", com.badlogic.gdx.graphics.Color.GREEN)
        startCooldown()
        return SkillResult(true, "Poisoned", damage, listOf(target))
    }
}
class HealingPotionSkill : Skill("healing_potion", "Healing Potion", "Restores 30% HP", 25, 4) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val heal = (caster.maxHealth * 0.3).toInt()
        caster.currentHealth = (caster.currentHealth + heal).coerceAtMost(caster.maxHealth)
        battleLog.addMessage("$name restores $heal HP!", com.badlogic.gdx.graphics.Color.GREEN)
        startCooldown()
        return SkillResult(true, "Healed $heal HP", heal)
    }
}
class ExplosiveMixtureSkill : Skill("explosive_mix", "Explosive Mixture", "Throws an unstable mixture that explodes, dealing damage to all enemies", 40, 5) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        var totalDamage = 0
        val aliveEnemies = targets.filter { it.isAlive() }

        aliveEnemies.forEach { enemy ->
            val damage = calculateDamage(caster.mageDamage, caster, enemy, isMagic = true, multiplier = 1.4)
            enemy.takeDamage(damage)
            battleLog.addMessage("Explosion hits ${enemy.name} for $damage damage!", com.badlogic.gdx.graphics.Color.ORANGE)
            totalDamage += damage
        }

        // 30% шанс поджечь каждого врага
        aliveEnemies.forEach { enemy ->
            if (Random.nextDouble() < 0.3) {
                enemy.applyDebuff(DebuffType.BURN, 2, 1.0)
                battleLog.addMessage("${enemy.name} caught fire from the explosion!", com.badlogic.gdx.graphics.Color.RED)
            }
        }

        startCooldown()
        return SkillResult(true, "Explosion dealt $totalDamage total damage", totalDamage, aliveEnemies)
    }
}
class AdrenalineVialSkill : Skill("adrenaline_vial", "Adrenaline Vial", "Increases attack speed by 30% and damage by 15% for 3 turns", 30, 4) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        caster.applyDebuff(DebuffType.BUFF_SPEED, 3, 1.3)
        caster.applyDebuff(DebuffType.BUFF_DAMAGE, 3, 1.15)
        battleLog.addMessage("$name: Attack speed +30% and damage +15% for 3 turns!", com.badlogic.gdx.graphics.Color.RED)
        startCooldown()
        return SkillResult(true, "Adrenaline rushing!")
    }
}
class SmokeBombSkill : Skill("smoke_bomb", "Smoke Bomb", "Creates a smoke screen, blinding enemies and increasing dodge chance", 25, 5) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val aliveEnemies = targets.filter { it.isAlive() }

        // Снижение точности врагов через WEAKNESS (снижение урона) или можно использовать PARALYSIS для шанса промаха
        aliveEnemies.forEach { enemy ->
            enemy.applyDebuff(DebuffType.WEAKNESS, 2, 0.5) // -50% урона (как аналог снижения точности)
            battleLog.addMessage("${enemy.name} is blinded by the smoke!", com.badlogic.gdx.graphics.Color.GRAY)
        }

        // Увеличение шанса уклонения для игрока
        caster.applyDebuff(DebuffType.DODGE, 3, 1.0)

        battleLog.addMessage("$name: Enemies blinded! Dodge chance increased!", com.badlogic.gdx.graphics.Color.LIGHT_GRAY)
        startCooldown()
        return SkillResult(true, "Smoke screen deployed", 0, aliveEnemies)
    }
}






// ==================== БЕСКЛАССОВЫЕ НАВЫКИ ====================
// Эти навыки можно изучить через предметы, свитки, учителей или особые события

// --- БАЗОВЫЕ АТАКУЮЩИЕ НАВЫКИ ---
class PowerStrikeSkill : Skill("power_strike", "Power Strike", "A powerful blow that deals 150% damage", 15, 2) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val target = targets.firstOrNull { it.isAlive() } ?: return SkillResult(false, "No target available")
        val damage = calculateDamage(caster.damage, caster, target, isMagic = false, multiplier = 1.5)
        target.takeDamage(damage)
        battleLog.addMessage("$name deals $damage damage to ${target.name}!", com.badlogic.gdx.graphics.Color.ORANGE)
        startCooldown()
        return SkillResult(true, "Powerful strike!", damage, listOf(target))
    }
}
class CleaveSkill : Skill("cleave", "Cleave", "Hits the target and adjacent enemies", 20, 3) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val aliveEnemies = targets.filter { it.isAlive() }
        if (aliveEnemies.isEmpty()) return SkillResult(false, "No targets available")

        val maxTargets = minOf(3, aliveEnemies.size)
        val hitTargets = aliveEnemies.take(maxTargets)
        var totalDamage = 0

        hitTargets.forEach { enemy ->
            val damage = calculateDamage(caster.damage, caster, enemy, isMagic = false, multiplier = 0.9)
            enemy.takeDamage(damage)
            battleLog.addMessage("Cleave hits ${enemy.name} for $damage damage!", com.badlogic.gdx.graphics.Color.ORANGE)
            totalDamage += damage
        }

        startCooldown()
        return SkillResult(true, "Cleave hit $maxTargets enemies", totalDamage, hitTargets)
    }
}

class PreciseStrikeSkill : Skill("precise_strike", "Precise Strike", "Guaranteed hit with increased crit chance", 20, 2) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val target = targets.firstOrNull { it.isAlive() } ?: return SkillResult(false, "No target available")
        // Временно повышаем крит шанс для этого удара
        val oldCrit = caster.critChance
        caster.critChance += 0.3
        val damage = calculateDamage(caster.damage, caster, target, isMagic = false, multiplier = 1.2)
        caster.critChance = oldCrit

        target.takeDamage(damage)
        battleLog.addMessage("Precise strike deals $damage damage to ${target.name}!", com.badlogic.gdx.graphics.Color.YELLOW)
        startCooldown()
        return SkillResult(true, "Precise hit!", damage, listOf(target))
    }
}

class RageStrikeSkill : Skill("rage_strike", "Rage Strike", "More damage the lower your health", 25, 3) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val target = targets.firstOrNull { it.isAlive() } ?: return SkillResult(false, "No target available")
        val missingHealthPercent = 1.0 - (caster.currentHealth.toDouble() / caster.maxHealth)
        val multiplier = 1.0 + missingHealthPercent * 1.5 // до +150% урона

        val damage = calculateDamage(caster.damage, caster, target, isMagic = false, multiplier = multiplier)
        target.takeDamage(damage)
        battleLog.addMessage("Rage fuels your strike! $damage damage to ${target.name}!", com.badlogic.gdx.graphics.Color.RED)
        startCooldown()
        return SkillResult(true, "Rage strike!", damage, listOf(target))
    }
}
class ThrustSkill : Skill("thrust", "Thrust", "Quick thrust that ignores 50% of enemy defense", 15, 1) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val target = targets.firstOrNull { it.isAlive() } ?: return SkillResult(false, "No target available")

        // Временно применяем дебафф снижения защиты вместо прямого изменения
        target.applyDebuff(DebuffType.WEAKNESS, 1, 0.5) // -50% защиты через WEAKNESS

        val damage = calculateDamage(caster.damage, caster, target, isMagic = false, multiplier = 1.1)
        target.takeDamage(damage)

        battleLog.addMessage("Thrust pierces armor! $damage damage to ${target.name}!", com.badlogic.gdx.graphics.Color.CYAN)
        startCooldown()
        return SkillResult(true, "Armor pierced!", damage, listOf(target))
    }
}

// --- МАГИЧЕСКИЕ АТАКУЮЩИЕ НАВЫКИ ---
class MagicMissileSkill : Skill("magic_missile", "Magic Missile", "Fires 3 missiles at random enemies", 20, 1) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val aliveEnemies = targets.filter { it.isAlive() }
        if (aliveEnemies.isEmpty()) return SkillResult(false, "No targets available")

        var totalDamage = 0
        repeat(3) {
            val target = aliveEnemies.random()
            val damage = calculateDamage(caster.mageDamage, caster, target, isMagic = true, multiplier = 0.7)
            target.takeDamage(damage)
            battleLog.addMessage("Magic Missile hits ${target.name} for $damage damage!", com.badlogic.gdx.graphics.Color.PURPLE)
            totalDamage += damage
        }

        startCooldown()
        return SkillResult(true, "Three missiles fired!", totalDamage)
    }
}
class ArcaneBoltSkill : Skill("arcane_bolt", "Arcane Bolt", "Pure arcane energy that deals magic damage", 15, 0) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val target = targets.firstOrNull { it.isAlive() } ?: return SkillResult(false, "No target available")
        val damage = calculateDamage(caster.mageDamage, caster, target, isMagic = true, multiplier = 1.4)
        target.takeDamage(damage)
        battleLog.addMessage("Arcane Bolt strikes ${target.name} for $damage damage!", com.badlogic.gdx.graphics.Color.PURPLE)
        startCooldown()
        return SkillResult(true, "Arcane power!", damage, listOf(target))
    }
}
class ShadowBoltSkill : Skill("shadow_bolt", "Shadow Bolt", "Deals dark damage and has chance to curse", 25, 2) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val target = targets.firstOrNull { it.isAlive() } ?: return SkillResult(false, "No target available")
        val damage = calculateDamage(caster.mageDamage, caster, target, isMagic = true, multiplier = 1.3)
        target.takeDamage(damage)

        if (Random.nextDouble() < 0.25) {
            target.applyDebuff(DebuffType.CURSE, 2, 1.0)
            battleLog.addMessage("${target.name} is cursed by the shadows!", com.badlogic.gdx.graphics.Color.DARK_GRAY)
        }

        battleLog.addMessage("Shadow Bolt deals $damage damage to ${target.name}!", com.badlogic.gdx.graphics.Color.DARK_GRAY)
        startCooldown()
        return SkillResult(true, "Shadow magic!", damage, listOf(target))
    }
}
class MindBlastSkill : Skill("mind_blast", "Mind Blast", "Psychic attack that ignores defense", 30, 3) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val target = targets.firstOrNull { it.isAlive() } ?: return SkillResult(false, "No target available")
        val damage = (caster.mageDamage * 1.6 * caster.getDamageMultiplier()).toInt()
        target.takeDamage(damage)
        target.applyDebuff(DebuffType.STUN, 1, 1.0)
        battleLog.addMessage("Mind Blast devastates ${target.name} for $damage damage and stuns them!", com.badlogic.gdx.graphics.Color.PINK)
        startCooldown()
        return SkillResult(true, "Mental assault!", damage, listOf(target))
    }
}
class VampiricTouchSkill : Skill("vampiric_touch", "Vampiric Touch", "Drains life from the enemy", 25, 3) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val target = targets.firstOrNull { it.isAlive() } ?: return SkillResult(false, "No target available")
        val damage = calculateDamage(caster.mageDamage, caster, target, isMagic = true, multiplier = 1.2)
        target.takeDamage(damage)

        val healAmount = (damage * 0.5).toInt()
        caster.currentHealth = (caster.currentHealth + healAmount).coerceAtMost(caster.maxHealth)

        battleLog.addMessage("Vampiric Touch drains $damage life from ${target.name} and heals for $healAmount!", com.badlogic.gdx.graphics.Color.MAROON)
        startCooldown()
        return SkillResult(true, "Life drained!", damage, listOf(target))
    }
}

// --- ЗАЩИТНЫЕ НАВЫКИ ---
class IronWillSkill : Skill("iron_will", "Iron Will", "Increases will and defense for 3 turns", 20, 4) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        caster.applyDebuff(DebuffType.BUFF_WILL, 3, 1.3)
        caster.applyDebuff(DebuffType.BUFF_DEFENSE, 3, 1.2)
        battleLog.addMessage("Iron Will activated! +30% Will and +20% Defense for 3 turns!", com.badlogic.gdx.graphics.Color.CYAN)
        startCooldown()
        return SkillResult(true, "Will strengthened!")
    }
}
class StoneSkinSkill : Skill("stone_skin", "Stone Skin", "Greatly increases defense but reduces speed", 30, 5) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        caster.applyDebuff(DebuffType.BUFF_DEFENSE, 3, 1.5)
        caster.applyDebuff(DebuffType.SLOW, 3, 0.7)
        battleLog.addMessage("Stone Skin: +50% Defense but -30% Speed for 3 turns!", com.badlogic.gdx.graphics.Color.GRAY)
        startCooldown()
        return SkillResult(true, "Skin hardened!")
    }
}
class MeditationSkill : Skill("meditation", "Meditation", "Restores mana and increases focus", 0, 5) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val manaRestored = (caster.maxMana * 0.25).toInt()
        caster.currentMana = (caster.currentMana + manaRestored).coerceAtMost(caster.maxMana)
        caster.applyDebuff(DebuffType.BUFF_WILL, 2, 1.2)
        battleLog.addMessage("Meditation restores $manaRestored mana and increases Will!", com.badlogic.gdx.graphics.Color.BLUE)
        startCooldown()
        return SkillResult(true, "Mind cleared", manaRestored)
    }
}
class LastStandSkill : Skill("last_stand", "Last Stand", "When below 30% HP, become invulnerable for 2 turns", 40, 8) {
    override fun canUse(caster: Player, targets: List<BattleEnemy>): Boolean {
        if (!super.canUse(caster, targets)) return false
        return caster.currentHealth < caster.maxHealth * 0.3
    }

    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        caster.applyDebuff(DebuffType.BUFF_INVULNERABLE, 2, 1.0)
        caster.applyDebuff(DebuffType.BUFF_DAMAGE, 2, 1.3)
        battleLog.addMessage("LAST STAND! You become invulnerable and deal 30% more damage!", com.badlogic.gdx.graphics.Color.GOLD)
        startCooldown()
        return SkillResult(true, "Standing strong!")
    }
}
class TauntSkill : Skill("taunt", "Taunt", "Forces enemies to attack you, increases defense", 15, 4) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        caster.applyDebuff(DebuffType.BUFF_DEFENSE, 2, 1.25)
        targets.forEach { enemy ->
            if (enemy.isAlive()) {
                enemy.applyDebuff(DebuffType.WEAKNESS, 2, 0.8)
            }
        }
        battleLog.addMessage("Taunt: Enemies are forced to attack you! Defense increased!", com.badlogic.gdx.graphics.Color.ORANGE)
        startCooldown()
        return SkillResult(true, "Enemies provoked!")
    }
}

// --- ИСЦЕЛЯЮЩИЕ НАВЫКИ ---
class BandageSkill : Skill("bandage", "Bandage", "Heals a small amount and stops bleeding", 10, 2) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val healAmount = (caster.maxHealth * 0.15).toInt()
        caster.currentHealth = (caster.currentHealth + healAmount).coerceAtMost(caster.maxHealth)
        caster.debuffManager.removeDebuff(DebuffType.BLEED)
        battleLog.addMessage("Bandage heals $healAmount HP and stops bleeding!", com.badlogic.gdx.graphics.Color.GREEN)
        startCooldown()
        return SkillResult(true, "Wounds treated", healAmount)
    }
}
class SecondWindSkill : Skill("second_wind", "Second Wind", "Heals based on missing health", 35, 6) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val missingHealth = caster.maxHealth - caster.currentHealth
        val healAmount = (missingHealth * 0.4).toInt()
        caster.currentHealth = (caster.currentHealth + healAmount).coerceAtMost(caster.maxHealth)
        battleLog.addMessage("Second Wind restores $healAmount HP!", com.badlogic.gdx.graphics.Color.GREEN)
        startCooldown()
        return SkillResult(true, "Second wind!", healAmount)
    }
}
class PurifySkill : Skill("purify", "Purify", "Removes all negative effects", 25, 4) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val negativeDebuffs = listOf(
            DebuffType.POISON, DebuffType.BURN, DebuffType.BLEED, DebuffType.CURSE,
            DebuffType.WEAKNESS, DebuffType.SLOW, DebuffType.STUN, DebuffType.SILENCE,
            DebuffType.PARALYSIS, DebuffType.FREEZE
        )
        negativeDebuffs.forEach { caster.debuffManager.removeDebuff(it) }
        battleLog.addMessage("Purify removes all negative effects!", com.badlogic.gdx.graphics.Color.WHITE)
        startCooldown()
        return SkillResult(true, "Purified!")
    }
}
class RegenerationSkill : Skill("regeneration", "Regeneration", "Heals over time for 4 turns", 30, 5) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        caster.applyDebuff(DebuffType.BUFF_HEALTH, 4, 1.0)
        battleLog.addMessage("Regeneration active! Healing over 4 turns!", com.badlogic.gdx.graphics.Color.GREEN)
        startCooldown()
        return SkillResult(true, "Regenerating")
    }
}

// --- НАВЫКИ КОНТРОЛЯ ---
class HamstringSkill : Skill("hamstring", "Hamstring", "Slows the enemy and causes bleeding", 20, 3) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val target = targets.firstOrNull { it.isAlive() } ?: return SkillResult(false, "No target available")
        val damage = calculateDamage(caster.damage, caster, target, isMagic = false, multiplier = 0.8)
        target.takeDamage(damage)
        target.applyDebuff(DebuffType.SLOW, 2, 1.0)
        target.applyDebuff(DebuffType.BLEED, 3, 1.0)
        battleLog.addMessage("Hamstring slows and causes bleeding to ${target.name}!", com.badlogic.gdx.graphics.Color.RED)
        startCooldown()
        return SkillResult(true, "Enemy crippled!", damage, listOf(target))
    }
}

class DisarmSkill : Skill("disarm", "Disarm", "Reduces enemy damage greatly for 2 turns", 25, 4) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val target = targets.firstOrNull { it.isAlive() } ?: return SkillResult(false, "No target available")
        target.applyDebuff(DebuffType.WEAKNESS, 2, 0.4) // -60% урона
        battleLog.addMessage("${target.name} is disarmed! Damage greatly reduced!", com.badlogic.gdx.graphics.Color.GRAY)
        startCooldown()
        return SkillResult(true, "Enemy disarmed!", 0, listOf(target))
    }
}
class IntimidateSkill : Skill("intimidate", "Intimidate", "Fears enemies, reducing their stats", 30, 5) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        targets.forEach { enemy ->
            if (enemy.isAlive() && Random.nextDouble() < 0.7) {
                enemy.applyDebuff(DebuffType.CURSE, 2, 0.7)
                battleLog.addMessage("${enemy.name} is intimidated!", com.badlogic.gdx.graphics.Color.PURPLE)
            }
        }
        startCooldown()
        return SkillResult(true, "Enemies intimidated!")
    }
}
class ConcussiveBlowSkill : Skill("concussive_blow", "Concussive Blow", "Chance to stun the enemy", 20, 3) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val target = targets.firstOrNull { it.isAlive() } ?: return SkillResult(false, "No target available")
        val damage = calculateDamage(caster.damage, caster, target, isMagic = false, multiplier = 1.1)
        target.takeDamage(damage)

        if (Random.nextDouble() < 0.4) {
            target.applyDebuff(DebuffType.STUN, 1, 1.0)
            battleLog.addMessage("${target.name} is stunned!", com.badlogic.gdx.graphics.Color.YELLOW)
        }

        battleLog.addMessage("Concussive blow deals $damage damage!", com.badlogic.gdx.graphics.Color.ORANGE)
        startCooldown()
        return SkillResult(true, "Concussive hit!", damage, listOf(target))
    }
}

// --- НАВЫКИ СКОРОСТИ И МОБИЛЬНОСТИ ---
class QuickStepSkill : Skill("quick_step", "Quick Step", "Increases speed and dodge for 3 turns", 15, 3) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        caster.applyDebuff(DebuffType.BUFF_SPEED, 3, 1.25)
        caster.applyDebuff(DebuffType.DODGE, 3, 1.0)
        battleLog.addMessage("Quick Step: Speed and Dodge increased for 3 turns!", com.badlogic.gdx.graphics.Color.CYAN)
        startCooldown()
        return SkillResult(true, "Quickened!")
    }
}
class AdrenalineRushSkill : Skill("adrenaline_rush", "Adrenaline Rush", "Massive speed boost for 2 turns", 20, 4) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        caster.applyDebuff(DebuffType.BUFF_SPEED, 2, 1.5)
        battleLog.addMessage("Adrenaline Rush! Speed +50% for 2 turns!", com.badlogic.gdx.graphics.Color.RED)
        startCooldown()
        return SkillResult(true, "Adrenaline pumping!")
    }
}
class TimeWarpSkill : Skill("time_warp", "Time Warp", "Manipulate time to gain an extra turn", 50, 10) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        // Эффект должен обрабатываться в боевой системе
        battleLog.addMessage("Time warps! You gain an extra turn!", com.badlogic.gdx.graphics.Color.PURPLE)
        startCooldown()
        return SkillResult(true, "Time manipulated!")
    }
}

// --- ОСОБЫЕ НАВЫКИ ---
class MirrorImageSkill : Skill("mirror_image", "Mirror Image", "Creates illusions that can absorb hits", 35, 6) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        caster.applyDebuff(DebuffType.DODGE, 3, 1.5)
        caster.applyDebuff(DebuffType.BUFF_SPEED, 3, 1.1)
        battleLog.addMessage("Mirror images surround you! Dodge greatly increased!", com.badlogic.gdx.graphics.Color.CYAN)
        startCooldown()
        return SkillResult(true, "Illusions created!")
    }
}
class ManaShieldSkill : Skill("mana_shield", "Mana Shield", "Absorbs damage using mana instead of health", 40, 5) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        caster.applyDebuff(DebuffType.BUFF_DEFENSE, 3, 1.4)
        battleLog.addMessage("Mana Shield activated! Damage will drain mana instead of health!", com.badlogic.gdx.graphics.Color.BLUE)
        startCooldown()
        return SkillResult(true, "Shielded by mana!")
    }
}
class BerserkerRageSkill : Skill("berserker_rage", "Berserker Rage", "Trade defense for massive damage boost", 30, 5) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        caster.applyDebuff(DebuffType.BUFF_DAMAGE, 3, 1.5)
        caster.applyDebuff(DebuffType.WEAKNESS, 3, 0.5) // Получает больше урона
        battleLog.addMessage("BERSERKER RAGE! +50% damage but you take more damage!", com.badlogic.gdx.graphics.Color.RED)
        startCooldown()
        return SkillResult(true, "Rage unleashed!")
    }
}
class FocusSkill : Skill("focus", "Focus", "Next attack is guaranteed to crit", 25, 4) {
    var focused = false

    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        focused = true
        caster.applyDebuff(DebuffType.BUFF_CRIT, 1, 2.0)
        battleLog.addMessage("Focus! Your next attack will critically hit!", com.badlogic.gdx.graphics.Color.YELLOW)
        startCooldown()
        return SkillResult(true, "Focused!")
    }
}
class RetributionSkill : Skill("retribution", "Retribution", "Returns a portion of damage taken", 25, 4) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        caster.applyDebuff(DebuffType.BUFF_DEFENSE, 3, 1.1)
        battleLog.addMessage("Retribution active! Enemies take damage when they hit you!", com.badlogic.gdx.graphics.Color.GOLD)
        startCooldown()
        return SkillResult(true, "Retribution ready!")
    }
}
class WarCrySkill : Skill("war_cry", "War Cry", "Increases damage and reduces enemy defense", 30, 5) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        caster.applyDebuff(DebuffType.BUFF_DAMAGE, 3, 1.2)
        targets.forEach { enemy ->
            if (enemy.isAlive()) {
                enemy.applyDebuff(DebuffType.WEAKNESS, 2, 0.85)
            }
        }
        battleLog.addMessage("WAR CRY! Your damage increases and enemies weaken!", com.badlogic.gdx.graphics.Color.ORANGE)
        startCooldown()
        return SkillResult(true, "War cry echoes!")
    }
}

// --- ПРОДВИНУТЫЕ/РЕДКИЕ НАВЫКИ ---
class PhoenixBlessingSkill : Skill("phoenix", "Phoenix Blessing", "Upon death, revive with full health once per battle", 100, 99) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        caster.applyDebuff(DebuffType.RESURRECTION, 999, 1.0)
        battleLog.addMessage("Phoenix Blessing! You will rise from death once!", com.badlogic.gdx.graphics.Color.GOLD)
        startCooldown()
        return SkillResult(true, "Blessed by phoenix!")
    }
}
class DragonBreathSkill : Skill("dragon_breath", "Dragon Breath", "Unleash dragon fire on all enemies", 60, 8) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        var totalDamage = 0
        targets.forEach { enemy ->
            if (enemy.isAlive()) {
                val damage = calculateDamage(caster.mageDamage, caster, enemy, isMagic = true, multiplier = 2.0)
                enemy.takeDamage(damage)
                enemy.applyDebuff(DebuffType.BURN, 3, 1.5)
                battleLog.addMessage("Dragon fire burns ${enemy.name} for $damage damage!", com.badlogic.gdx.graphics.Color.ORANGE)
                totalDamage += damage
            }
        }
        startCooldown()
        return SkillResult(true, "Dragon breath unleashed!", totalDamage, targets)
    }
}
class DivineInterventionSkill : Skill("divine", "Divine Intervention", "Fully heal and gain invulnerability for 1 turn", 80, 15) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        caster.currentHealth = caster.maxHealth
        caster.currentMana = caster.maxMana
        caster.applyDebuff(DebuffType.BUFF_INVULNERABLE, 1, 1.0)
        caster.clearDebuffs()
        battleLog.addMessage("DIVINE INTERVENTION! Fully healed and invulnerable!", com.badlogic.gdx.graphics.Color.GOLD)
        startCooldown()
        return SkillResult(true, "Divine power!", caster.maxHealth)
    }
}
class ApocalypseSkill : Skill("apocalypse", "Apocalypse", "Deal massive damage to all, but also damage yourself", 100, 20) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        var totalDamage = 0
        val selfDamage = (caster.maxHealth * 0.3).toInt()

        targets.forEach { enemy ->
            if (enemy.isAlive()) {
                val damage = calculateDamage(caster.mageDamage, caster, enemy, isMagic = true, multiplier = 3.0)
                enemy.takeDamage(damage)
                totalDamage += damage
            }
        }

        caster.currentHealth = (caster.currentHealth - selfDamage).coerceAtLeast(1)
        battleLog.addMessage("APOCALYPSE! $totalDamage damage to all enemies, but you take $selfDamage damage!", com.badlogic.gdx.graphics.Color.PURPLE)
        startCooldown()
        return SkillResult(true, "Apocalypse unleashed!", totalDamage, targets)
    }
}
class VoidWalkerSkill : Skill("void_walker", "Void Walker", "Become untargetable for 1 turn and deal bonus damage next turn", 45, 7) {
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        caster.applyDebuff(DebuffType.BUFF_INVULNERABLE, 1, 1.0)
        caster.applyDebuff(DebuffType.BUFF_DAMAGE, 2, 1.5)
        battleLog.addMessage("You step into the void! Invulnerable and next attack deals +50% damage!", com.badlogic.gdx.graphics.Color.DARK_GRAY)
        startCooldown()
        return SkillResult(true, "Void walking!")
    }
}




// Вспомогательная функция расчета урона с учетом крита
fun calculateDamage(base: Int, caster: Player, target: BattleEnemy, isMagic: Boolean, multiplier: Double = 1.0): Int {
    val isCrit = Random.nextDouble() < caster.critChance
    val critMult = if (isCrit) 2.0 else 1.0

    val damage = (base * multiplier * critMult * caster.getDamageMultiplier()).toInt()
    val defense = if (isMagic) 0.0 else target.defense * target.getDefenseMultiplier()

    return (damage * (1 - defense)).toInt().coerceAtLeast(1)
}
