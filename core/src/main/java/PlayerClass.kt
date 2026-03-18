package ru.triplethall.rpgturnbased

import ru.triplethall.rpgturnbased.Player
import kotlin.Double


enum class PlayerClasses(
    val displayName: String,                     // название класса
    val description: String,                     // описание класса
    val baseDamage: Int = 15,                    // Урон
    val baseMaxHealth: Int = 100,                // хп
    val baseDefense: Double = 0.0,               // Защита (процентная)
    val baseMaxMana: Int = 50,                   // Мана
    val baseAttackSpeed: Double = 1.0,           // Скорость (атаки)
    val baseAccuracy: Double = 0.8,              // Точность (шанс попадания по врагу)
    val baseWill: Double = 0.0,                  // Воля (сопротивление дебафам)
    val baseMageDamage: Int = 10,                // урон магии
    val baseLuck: Double = 0.01,                 // Удача
    val baseCritChance: Double = 0.01            // крит шанс
) {
    ADVENTURIST(
        displayName = "Авантюрист",
        description = "Новичок который только начал своё приключение",
        baseDamage = 15,
        baseMaxHealth = 100,
        baseDefense = 0.0,
        baseMaxMana = 50,
        baseAttackSpeed = 1.0,
        baseAccuracy = 0.8,
        baseWill = 0.0,
        baseMageDamage = 10,
        baseLuck = 0.01,
        baseCritChance = 0.01
    ),
    KNIGHT(
        displayName = "Рыцарь",
        description = "Рассудительный, с повышенной живучесть и урон",
        baseDamage = 20,
        baseDefense = 0.08,
        baseMaxHealth = 150,
        baseAttackSpeed = 0.9,
        baseWill = 0.3,
        baseAccuracy = 0.86
    ),

    MAGE(
        displayName = "Волшебник",
        description = "Умнейший с повышенным уроном магии",
        baseDamage = 5,
        baseMageDamage = 20,
        baseMaxMana = 150,
        baseDefense = 0.00,
        baseMaxHealth = 80,
        baseAttackSpeed = 1.0,
        baseWill = 0.5,
        baseAccuracy = 0.88
    ),

    ASSASSIN(
        displayName = "Ассасин",
        description = "Мастер убийств, Тренировался скрытности",
        baseDamage = 18,
        baseDefense = 0.00,
        baseMaxHealth = 110,
        baseAttackSpeed = 1.4,
        baseWill = 0.07,
        baseAccuracy = 0.94,
        baseCritChance = 0.11
    ),

    ARCHER(
        displayName = "Лучник",
        description = "Зоркий глаз, хорошее владение луком",
        baseDamage = 16,
        baseMaxHealth = 95,
        baseDefense = 0.01,
        baseMaxMana = 60,
        baseAttackSpeed = 1.2,
        baseAccuracy = 0.72,
        baseWill = 0.09,
        baseMageDamage = 5,
        baseLuck = 0.04,
        baseCritChance = 0.09
    ),

    PRIEST(
        displayName = "Священник",
        description = "Великий святой, умеет искусно залечивать раны",
        baseDamage = 7,
        baseMaxHealth = 105,
        baseDefense = 0.0,
        baseMaxMana = 140,
        baseAttackSpeed = 1.0,
        baseAccuracy = 0.83,
        baseWill = 0.76,
        baseMageDamage = 22,
        baseLuck = 0.01,
        baseCritChance = 0.01
    ),

    JOKER(
        displayName = "Джокер",
        description = "Глупец, что вечно полагается на удачу",
        baseDamage = 13,
        baseMaxHealth = 110,
        baseDefense = 0.01,
        baseMaxMana = 70,
        baseAttackSpeed = 1.0,
        baseAccuracy = 0.85,
        baseWill = 0.01,
        baseMageDamage = 15,
        baseLuck = 0.1,
        baseCritChance = 0.05
    ),

    BERSERK(
        displayName = "Берсерк",
        description = "Жажда крови, Лишь на грани смерти он чувствует запах крови",
        baseDamage = 23,
        baseMaxHealth = 200,
        baseDefense = 0.04,
        baseMaxMana = 25,
        baseAttackSpeed = 1.3,
        baseAccuracy = 0.87,
        baseWill = 0.04,
        baseMageDamage = 16,
        baseLuck = 0.01,
        baseCritChance = 0.07
    ),

    SHAMAN(
        displayName = "Шаман",
        description = "Мастер проклятий, изучал тёмные таинства",
        baseDamage = 7,
        baseMaxHealth = 180,
        baseDefense = 0.00,
        baseMaxMana = 145,
        baseAttackSpeed = 1.0,
        baseAccuracy = 0.85,
        baseWill = 0.07,
        baseMageDamage = 21,
        baseLuck = 0.01,
        baseCritChance = 0.02
    );


    //  Применение бонусов класса к игроку:
    fun applyToPlayer(player: Player) {
        player.damage = baseDamage
        player.defense = baseDefense
        player.maxMana = baseMaxMana
        player.currentMana = baseMaxMana
        player.maxHealth = baseMaxHealth
        player.currentHealth = baseMaxHealth
        player.attackSpeed = baseAttackSpeed
        player.accuracy = baseAccuracy
        player.will = baseWill
        player.mageDamage = baseMageDamage
        player.luck = baseLuck
        player.critChance = baseCritChance
    }


    // описание бонусов класса:
    @Suppress("DefaultLocale")
    fun getStatsDescription(): String {
        return """
        $displayName - $description
        |----------------------
        | Урон: $baseDamage ${getDiff(baseDamage, 15)}
        | Защита: ${(baseDefense * 100).toInt()}% ${getDiff(baseDefense, 0.0)}
        | Здоровье: $baseMaxHealth ${getDiff(baseMaxHealth, 100)}
        | Мана: $baseMaxMana ${getDiff(baseMaxMana, 50)}
        | Скорость: ${String.format("%.1f", baseAttackSpeed)} ${getDiff(baseAttackSpeed, 1.0)}
        | Точность: ${(baseAccuracy * 100).toInt()}% ${getDiff(baseAccuracy, 0.8)}
        | Воля: ${(baseWill * 100).toInt()}% ${getDiff(baseWill, 0.5)}
        | Маг. урон: $baseMageDamage ${getDiff(baseMageDamage, 10)}
        | Удача: ${(baseLuck * 100).toInt()}% ${getDiff(baseLuck, 0.01)}
        | Крит шанс: ${(baseCritChance * 100).toInt()}% ${getDiff(baseCritChance, 0.01)}
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

class ClassSelector {
    private var selectedClass: PlayerClasses? = null

    fun getCurrentClass(): PlayerClasses? = selectedClass

    fun selectClass(playerClass: PlayerClasses, player: Player) {
        selectedClass = playerClass
        playerClass.applyToPlayer(player)
        println("Выбран класс: ${playerClass.displayName}")
        println(playerClass.getStatsDescription())
    }

    fun showAllClasses() {
        println("====== ДОСТУПНЫЕ КЛАССЫ =====")
        PlayerClasses.values().forEachIndexed { index, playerClass ->
            println("\n${index + 1}. ${playerClass.displayName}")
            println("   ${playerClass.description}")
        }
    }
}
