package ru.triplethall.rpgturnbased

import ru.triplethall.rpgturnbased.Player
import kotlin.Double


enum class PlayerClasses(
    val displayName: String,                     // название класса
    val description: String,                     // описание класса
    val baseDamage: Int = 25,                    // Урон
    val baseMageDamage: Int = 10,                // урон магии
    val baseMaxHealth: Int = 200,                // хп
    val baseDefense: Double = 0.0,               // Защита (процентная)
    val baseMaxMana: Int = 50,                   // Мана
    val baseAttackSpeed: Double = 1.0,           // Скорость (атаки)
    val baseAccuracy: Double = 0.8,              // Точность (шанс попадания по врагу)
    val baseWill: Double = 0.0,                  // Воля (сопротивление дебафам)
    val baseLuck: Double = 0.00,                 // Удача
    val baseCritChance: Double = 0.01            // крит шанс
)
{
    ADVENTURIST(
        displayName = "Adventurer",
        description = "A beginner who has just started their adventure",
        baseDamage = 25,
        baseMaxHealth = 200,
        baseDefense = 0.0,
        baseMaxMana = 50,
        baseAttackSpeed = 1.0,
        baseAccuracy = 0.8,
        baseWill = 0.0,
        baseMageDamage = 10,
        baseLuck = 0.00,
        baseCritChance = 0.01
    ),
    KNIGHT(
        displayName = "Knight",
        description = "Prudent, with increased survivability and damage",
        baseDamage = 30,
        baseDefense = 0.15,
        baseMaxHealth = 250,
        baseAttackSpeed = 0.85,
        baseAccuracy = 0.86,
        baseMaxMana = 40,
        baseMageDamage = 5,
        baseCritChance = 0.03,
        baseWill = 0.25
    ),

    MAGE(
        displayName = "Wizard",
        description = "Wise, with increased magic damage",
        baseDamage = 13,
        baseMageDamage = 28,
        baseMaxMana = 150,
        baseDefense = 0.02,
        baseMaxHealth = 180,
        baseAttackSpeed = 1.0,
        baseWill = 0.35,
        baseAccuracy = 0.85,
        baseCritChance = 0.04
    ),

    ASSASSIN(
        displayName = "Assassin",
        description = "Master of assassinations, trained in stealth",
        baseDamage = 29,
        baseDefense = 0.05,
        baseMaxHealth = 195,
        baseAttackSpeed = 1.5,
        baseWill = 0.10,
        baseAccuracy = 0.92,
        baseCritChance = 0.15,
        baseMaxMana = 45,
        baseMageDamage = 0
    ),

    ARCHER(
        displayName = "Archer",
        description = "Keen eye, good archery skills",
        baseDamage = 28,
        baseMaxHealth = 195,
        baseDefense = 0.03,
        baseMaxMana = 55,
        baseAttackSpeed = 1.3,
        baseAccuracy = 0.88,
        baseWill = 0.09,
        baseMageDamage = 5,
        baseLuck = 0.00,
        baseCritChance = 0.12
    ),

    PRIEST(
        displayName = "Priest",
        description = "Great saint, skilled at healing wounds",
        baseDamage = 17,
        baseMaxHealth = 200,
        baseDefense = 0.05,
        baseMaxMana = 140,
        baseAttackSpeed = 0.95,
        baseAccuracy = 0.85,
        baseWill = 0.50,
        baseMageDamage = 24,
        baseLuck = 0.00,
        baseCritChance = 0.01
    ),

    JOKER(
        displayName = "Joker",
        description = "Fool who always relies on luck",
        baseDamage = 23,
        baseMaxHealth = 205,
        baseDefense = 0.02,
        baseMaxMana = 70,
        baseAttackSpeed = 1.1,
        baseAccuracy = 0.82,
        baseWill = 0.05,
        baseMageDamage = 14,
        baseLuck = 0.15,
        baseCritChance = 0.08
    ),

    BERSERK(
        displayName = "Berserker",
        description = "Bloodthirsty, only on the verge of death does he smell blood",
        baseDamage = 33,
        baseMaxHealth = 280,
        baseDefense = 0.08,
        baseMaxMana = 25,
        baseAttackSpeed = 1.2,
        baseAccuracy = 0.82,
        baseWill = 0.08,
        baseMageDamage = 8,
        baseLuck = 0.00,
        baseCritChance = 0.07
    ),

    SHAMAN(
        displayName = "Shaman",
        description = "Master of curses, studied dark mysteries",
        baseDamage = 19,
        baseMaxHealth = 210,
        baseDefense = 0.04,
        baseMaxMana = 145,
        baseAttackSpeed = 1.0,
        baseAccuracy = 0.84,
        baseWill = 0.20,
        baseMageDamage = 23,
        baseLuck = 0.00,
        baseCritChance = 0.05
    ),

    SWORDMASTER(
        displayName = "Swordmaster",
        description = "Lord of swords, strong and fast",
        baseDamage = 31,
        baseMaxHealth = 225,
        baseDefense = 0.08,
        baseMaxMana = 45,
        baseAttackSpeed = 1.6,
        baseAccuracy = 0.90,
        baseWill = 0.4,
        baseMageDamage = 8,
        baseLuck = 0.0,
        baseCritChance = 0.10
    ),

    MONARCH(
        displayName = "Monk",
        description = "Strong in spirit, strong in body",
        baseDamage = 29,
        baseMaxHealth = 260,
        baseDefense = 0.12,
        baseMaxMana = 55,
        baseAttackSpeed = 1.25,
        baseAccuracy = 0.88,
        baseWill = 0.22,
        baseMageDamage = 20,
        baseLuck = 0.0,
        baseCritChance = 0.06
    );


    //  Применение бонусов класса к игроку:
    fun applyToPlayer(player: Player) {
        val level = player.level
        val levelBonus = level - 1

        player.damage = baseDamage + (levelBonus * 2)
        player.mageDamage = baseMageDamage + (levelBonus * 2)
        player.maxHealth = baseMaxHealth + (levelBonus * 8)
        player.maxMana = baseMaxMana + (levelBonus * 5)
        player.defense = baseDefense + (levelBonus * 0.01)
        player.attackSpeed = baseAttackSpeed + (levelBonus * 0.02)
        player.accuracy = baseAccuracy + (levelBonus * 0.01)
        player.will = baseWill + (levelBonus * 0.02)
        player.luck = baseLuck + (levelBonus * 0.01)
        player.critChance = baseCritChance + (levelBonus * 0.005)
    }


    // описание бонусов класса:
    @Suppress("DefaultLocale")
    fun getStatsDescription(): String {
        return """
        $displayName - $description
        |----------------------
        | Damage: $baseDamage ${getDiff(baseDamage, 15)}
        | Defense: ${(baseDefense * 100).toInt()}% ${getDiff(baseDefense, 0.0)}
        | Health: $baseMaxHealth ${getDiff(baseMaxHealth, 100)}
        | Mana: $baseMaxMana ${getDiff(baseMaxMana, 50)}
        | Speed: ${String.format("%.1f", baseAttackSpeed)} ${getDiff(baseAttackSpeed, 1.0)}
        | Accuracy: ${(baseAccuracy * 100).toInt()}% ${getDiff(baseAccuracy, 0.8)}
        | Will: ${(baseWill * 100).toInt()}% ${getDiff(baseWill, 0.5)}
        | Magic Damage: $baseMageDamage ${getDiff(baseMageDamage, 10)}
        | Luck: ${(baseLuck * 100).toInt()}% ${getDiff(baseLuck, 0.01)}
        | Crit Chance: ${(baseCritChance * 100).toInt()}% ${getDiff(baseCritChance, 0.01)}
        """.trimMargin()
    }
    private fun getDiff(value: Number, base: Number): String {
        val diff = when {
            value is Int && base is Int -> value - base
            value is Double && base is Double -> value - base
            else -> 0.0
        }
        return when {
            diff is Int && diff > 0 -> "(+$diff)"
            diff is Int && diff < 0 -> "($diff)"
            diff is Double && diff > 0 -> "(+${String.format("%.1f", diff)})"
            diff is Double && diff < 0 -> "(${String.format("%.1f", diff)})"
            else -> ""
        }
    }
}
