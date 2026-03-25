@file:Suppress("unused")

package ru.triplethall.rpgturnbased


// враги
enum class Enemy(
    // Базовые характеристики врага
    val displayEnemyName: String,                     // Имя врага
    val baseEnemyDamage: Int = 20,                    // Урон
    val baseEnemyMageDamage: Int = 10,                // урон магии
    val baseEnemyMaxHealth: Int = 200,                // хп
    val baseEnemyDefense: Double = 0.0,               // Защита (процентная)
    val baseEnemyAttackSpeed: Double = 1.0,           // Скорость (атаки)
    val baseEnemyAccuracy: Double = 0.8,              // Точность (шанс попадания по персонажу)
    val baseEnemyWill: Double = 0.0,                  // Воля (сопротивление дебафам)
    val baseEnemyCritChance: Double = 0.01,           // крит шанс
) {
    GREEN_SLIME(
        displayEnemyName = "Green Slime",
        baseEnemyDamage = 10,                    // Урон
        baseEnemyMageDamage = 10,                // урон магии
        baseEnemyMaxHealth = 70,                // хп
        baseEnemyDefense = 0.0,                  // Защита (процентная)
        baseEnemyAttackSpeed = 1.0,              // Скорость (атаки)
        baseEnemyAccuracy = 0.8,                 // Точность (шанс попадания по персонажу)
        baseEnemyWill = 0.0,                     // Воля (сопротивление дебафам)
        baseEnemyCritChance = 0.00,              // крит шанс
    ),

    RED_SLIME(
        displayEnemyName = "Red Slime",
        baseEnemyDamage = 25,                    // Урон
        baseEnemyMageDamage = 10,                // урон магии
        baseEnemyMaxHealth = 40,                // хп
        baseEnemyDefense = 0.0,                  // Защита (процентная)
        baseEnemyAttackSpeed = 1.0,              // Скорость (атаки)
        baseEnemyAccuracy = 0.8,                 // Точность (шанс попадания по персонажу)
        baseEnemyWill = 0.02,                    // Воля (сопротивление дебафам)
        baseEnemyCritChance = 0.00               // крит шанс
    ),

    BLUE_SLIME(
        displayEnemyName = "Blue Slime",
        baseEnemyDamage = 10,                    // Урон
        baseEnemyMageDamage = 10,                // урон магии
        baseEnemyMaxHealth = 50,                // хп
        baseEnemyDefense = 0.1,                  // Защита (процентная)
        baseEnemyAttackSpeed = 1.0,              // Скорость (атаки)
        baseEnemyAccuracy = 0.8,                 // Точность (шанс попадания по персонажу)
        baseEnemyWill = 0.0,                     // Воля (сопротивление дебафам)
        baseEnemyCritChance = 0.01               // крит шанс
    );
}





// дружелюбные сущности
enum class Ally(
    // Базовые характеристики существ
    val displayAllyName: String,                      // Имя существ
    val baseAllyDamage: Int = 100,                    // Урон
    val baseAllyMageDamage: Int = 10,                 // урон магии
    val baseAllyMaxHealth: Int = 100,                 // хп
    val baseAllyDefense: Double = 0.15,               // Защита (процентная)
    val baseAllyAttackSpeed: Double = 1.0,            // Скорость (атаки)
    val baseAllyAccuracy: Double = 1.0,               // Точность (шанс попадания по персонажу)
    val baseAllyWill: Double = 0.05,                  // Воля (сопротивление дебафам)
    val baseAllyCritChance: Double = 0.05,            // крит шанс
) {
    GUIDE(
        displayAllyName = "Guider",                  // Имя дружелюбных существ
        baseAllyDamage = 100,                        // Урон
        baseAllyMageDamage = 50,                     // урон магии
        baseAllyMaxHealth = 1000,                    // хп
        baseAllyDefense = 0.15,                      // Защита (процентная)
        baseAllyAttackSpeed = 1.0,                   // Скорость (атаки)
        baseAllyAccuracy = 1.0,                      // Точность (шанс попадания по персонажу)
        baseAllyWill = 0.05,                         // Воля (сопротивление дебафам)
        baseAllyCritChance = 0.05,                   // крит шанс
    );
}




// нейтральные сущности
enum class Neutral(
    // Базовые характеристики нейтральных существ
    val displayNeutralName: String,                     // Имя нейтральных существ
    val baseNeutralDamage: Int = 100,                   // Урон
    val baseNeutralMageDamage: Int = 50,                // урон магии
    val baseNeutralMaxHealth: Int = 1000,               // хп
    val baseNeutralDefense: Double = 0.15,              // Защита (процентная)
    val baseNeutralAttackSpeed: Double = 1.0,           // Скорость (атаки)
    val baseNeutralAccuracy: Double = 1.0,              // Точность (шанс попадания по персонажу)
    val baseNeutralWill: Double = 0.05,                 // Воля (сопротивление дебафам)
    val baseNeutralCritChance: Double = 0.05,           // крит шанс
) {
    TRADER(
        displayNeutralName = "Trader",
        baseNeutralDamage = 100,
        baseNeutralMageDamage = 40,
        baseNeutralMaxHealth = 1000,
        baseNeutralDefense = 0.15,
        baseNeutralAttackSpeed = 1.0,
        baseNeutralAccuracy = 1.0,
        baseNeutralWill = 0.05,
        baseNeutralCritChance = 0.05
    ),

    PRIEST(
        displayNeutralName = "Priest",
        baseNeutralDamage = 50,
        baseNeutralMageDamage = 150,
        baseNeutralMaxHealth = 600,
        baseNeutralDefense = 0.05,
        baseNeutralAttackSpeed = 1.0,
        baseNeutralAccuracy = 1.0,
        baseNeutralWill = 0.15,
        baseNeutralCritChance = 0.05
    );
}
