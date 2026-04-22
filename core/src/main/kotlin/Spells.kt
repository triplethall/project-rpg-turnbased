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
class DodgeSkill : Skill("dodge", "Dodge", "Increases dodge chance by 80% for 3 turns ", 10, 4) { // NEW: Cooldown increased 1 -> 4
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
class StoneBulletSkill : Skill("stone_bullet", "Stone bullet", "Throws 3 bullets at random enemies", 25, 2) { // NEW: Cooldown increased 1 -> 2
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
class BackstabSkill : Skill("backstab", "Backstab", "X2 Damage and bleeding", 15, 4) { // NEW: Cooldown increased 2 -> 4
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
class ShurikenThrowSkill : Skill("shuriken", "Shuriken Throw", "Deals 1.5 damage", 10, 2) { // NEW: Cooldown increased 0 -> 2
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        val target = targets.firstOrNull { it.isAlive() } ?: return SkillResult(false, "There is no goal")
        val damage = calculateDamage(caster.damage, caster, target, isMagic = false, multiplier = 1.5)

        target.takeDamage(damage)
        battleLog.addMessage("Shuriken deal $damage damage by ${target.name}", com.badlogic.gdx.graphics.Color.GRAY)

        startCooldown()
        return SkillResult(true, "Hit!", damage, listOf(target))
    }
}
class StealthSkill : Skill("stealth", "Stealth", "+10% crit chance and +9% speed", 20, 3) { // NEW: Cooldown decreased 5 -> 3
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
class ResurrectionSkill : Skill("resurrection", "Resurrection", "Upon death, resurrects with 50% HP for 3 turns", 100, 10) { // NEW: ManaCost decreased 999 -> 100
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
class DiceRollSkill : Skill("dice", "The roll of the dice", "Chance of 1/6 to deal damage, depends on luck", 0, 0) { // NEW: ManaCost decreased 10 -> 0
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
class SlotMachineSkill : Skill("slot", "Slot machine", "3 numbers from 1 to 9. Three of the same - the effect!", 0, 0) { // NEW: ManaCost decreased 30 -> 0; Cooldown decreased 5 -> 0
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
            2 -> targets.firstOrNull()?.applyDebuff(DebuffType.FREEZE, 2, 1.0)
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
class AlterEgoSkill : Skill("alter_ego", "Alter ego", "Creates a clone (75% stats) at the cost of 75% HP", 70, 10) { // NEW: ManaCost increased 50 -> 70
    override fun execute(caster: Player, targets: List<BattleEnemy>, battleLog: BattleMessageSystem): SkillResult {
        caster.currentHealth = (caster.currentHealth * 0.25).toInt()
        caster.applyDebuff(DebuffType.CLONE, 5, 0.75) // Клон как бафф
        battleLog.addMessage("Alter ego! A clone with 75% characteristics has been created!", com.badlogic.gdx.graphics.Color.DARK_GRAY)
        startCooldown()
        return SkillResult(true, "A clone has been created")
    }
}
class DarkSecretsSkill : Skill("dark_secrets", "Dark Mysteries", "-5% HP, +50% Magic Damage", 50, 3) { // NEW: ManaCost increased 30 -> 50
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
class DoubleSlashSkill : Skill("double_slash", "Double slhash", "2 hits with a multiplier of 1.25", 25, 3) { // NEW: Fixed typo in name (slhash instead of slash)
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
class FlurrySkill : Skill("flurry", "A flurry of cuts", "Шквал порезов", 30, 7) { // NEW: Cooldown increased 4 -> 7
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

// Вспомогательная функция расчета урона с учетом крита
fun calculateDamage(base: Int, caster: Player, target: BattleEnemy, isMagic: Boolean, multiplier: Double = 1.0): Int {
    val isCrit = Random.nextDouble() < caster.critChance
    val critMult = if (isCrit) 2.0 else 1.0

    val damage = (base * multiplier * critMult * caster.getDamageMultiplier()).toInt()
    val defense = if (isMagic) 0.0 else target.defense * target.getDefenseMultiplier()

    return (damage * (1 - defense)).toInt().coerceAtLeast(1)
}
