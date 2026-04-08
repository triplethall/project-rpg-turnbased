@file:Suppress("unused")

package ru.triplethall.rpgturnbased
import com.badlogic.gdx.Gdx
import kotlin.random.Random

//enum class EnemyState { IDLE, ATTACKING }
enum class EnemyType(
    val displayName: String,
    val specialEffect: String? = null   // специальный эффект типа
)
{
    NO_TYPE(
        displayName = "Обычный",
    ),

    FIRE(
        displayName = "Огненный",
        specialEffect = "При получении урона огнем - восстанавливает 10% здоровья"
    ),

    WATER(
        displayName = "Водный",
        specialEffect = "Атаки имеют шанс 20% наложить 'Мокрота' (+25% урон от молний)"
    ),

    WIND(
        displayName = "Ветряной",
        specialEffect = "Имеет 25% уклонение от физических атак"
    ),

    EARTH(
        displayName = "Земляной",
        specialEffect = "Имеет +30% к защите от физических атак"
    ),

    ICE(
        displayName = "Ледяной",
        specialEffect = "Атаки имеют шанс 25% заморозить цель (пропуск хода)"
    ),

    CURSED(
        displayName = "Проклятый",
        specialEffect = "При смерти накладывает 'Проклятие' на убийцу (-20% ко всем характеристикам на 3 хода)"
    ),

    ELECTRIC(
        displayName = "Электрический",
        specialEffect = "Атаки имеют шанс 20% парализовать цель (-30% скорости на 2 хода)"
    ),

    POISON(
        displayName = "Ядовитый",
        specialEffect = "Все атаки накладывают 'Отравление' (5% урона от макс. здоровья за ход)"
    ),

    HOLY(
        displayName = "Святой",
        specialEffect = "Лечит союзников на 5% здоровья каждый ход"
    );
}



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
    val isElite: Boolean = false,                     // пока не работает
    val enemyType: EnemyType = EnemyType.NO_TYPE,
    val spritePath: String? = null
) {
    GREEN_SLIME(
        displayEnemyName = "Green Slime",
        baseEnemyDamage = 10,                    // Урон
        baseEnemyMageDamage = 10,                // урон магии
        baseEnemyMaxHealth = 50,                 // хп
        baseEnemyDefense = 0.0,                  // Защита (процентная)
        baseEnemyAttackSpeed = 1.0,              // Скорость (атаки)
        baseEnemyAccuracy = 0.8,                 // Точность (шанс попадания по персонажу)
        baseEnemyWill = 0.0,                     // Воля (сопротивление дебафам)
        baseEnemyCritChance = 0.00,              // крит шанс
        enemyType = EnemyType.POISON
    ),

    RED_SLIME(
        displayEnemyName = "Red Slime",
        baseEnemyDamage = 25,                    // Урон
        baseEnemyMageDamage = 10,                // урон магии
        baseEnemyMaxHealth = 40,                 // хп
        baseEnemyDefense = 0.0,                  // Защита (процентная)
        baseEnemyAttackSpeed = 1.0,              // Скорость (атаки)
        baseEnemyAccuracy = 0.8,                 // Точность (шанс попадания по персонажу)
        baseEnemyWill = 0.02,                    // Воля (сопротивление дебафам)
        baseEnemyCritChance = 0.00,              // крит шанс
        enemyType = EnemyType.FIRE,
    ),

    BLUE_SLIME(
        displayEnemyName = "Blue Slime",
        baseEnemyDamage = 10,                    // Урон
        baseEnemyMageDamage = 10,                // урон магии
        baseEnemyMaxHealth = 50,                 // хп
        baseEnemyDefense = 0.1,                  // Защита (процентная)
        baseEnemyAttackSpeed = 1.0,              // Скорость (атаки)
        baseEnemyAccuracy = 0.8,                 // Точность (шанс попадания по персонажу)
        baseEnemyWill = 0.0,                     // Воля (сопротивление дебафам)
        baseEnemyCritChance = 0.01,              // крит шанс
        enemyType = EnemyType.WATER
    ),

    // WIND тип - Воздушный слизень
    WIND_SLIME(
        displayEnemyName = "Wind Slime",
        baseEnemyDamage = 18,
        baseEnemyMageDamage = 22,
        baseEnemyMaxHealth = 55,
        baseEnemyDefense = 0.05,
        baseEnemyAttackSpeed = 1.3,
        baseEnemyAccuracy = 0.85,
        baseEnemyWill = 0.05,
        baseEnemyCritChance = 0.08,
        enemyType = EnemyType.WIND
    ),

    // EARTH тип - Земляной слизень
    EARTH_SLIME(
        displayEnemyName = "Earth Slime",
        baseEnemyDamage = 28,
        baseEnemyMageDamage = 8,
        baseEnemyMaxHealth = 110,
        baseEnemyDefense = 0.25,
        baseEnemyAttackSpeed = 0.7,
        baseEnemyAccuracy = 0.75,
        baseEnemyWill = 0.1,
        baseEnemyCritChance = 0.02,
        enemyType = EnemyType.EARTH
    ),

    // ICE тип - Ледяной слизень
    ICE_SLIME(
        displayEnemyName = "Ice Slime",
        baseEnemyDamage = 15,
        baseEnemyMageDamage = 25,
        baseEnemyMaxHealth = 65,
        baseEnemyDefense = 0.1,
        baseEnemyAttackSpeed = 0.9,
        baseEnemyAccuracy = 0.82,
        baseEnemyWill = 0.08,
        baseEnemyCritChance = 0.05,
        enemyType = EnemyType.ICE
    ),

    // CURSED тип - Проклятый слизень
    CURSED_SLIME(
        displayEnemyName = "Cursed Slime",
        baseEnemyDamage = 26,
        baseEnemyMageDamage = 26,
        baseEnemyMaxHealth = 75,
        baseEnemyDefense = 0.02,
        baseEnemyAttackSpeed = 1.1,
        baseEnemyAccuracy = 0.9,
        baseEnemyWill = 0.15,
        baseEnemyCritChance = 0.12,
        enemyType = EnemyType.CURSED
    ),

    // ELECTRIC тип - Электрический слизень
    ELECTRIC_SLIME(
        displayEnemyName = "Electric Slime",
        baseEnemyDamage = 20,
        baseEnemyMageDamage = 28,
        baseEnemyMaxHealth = 60,
        baseEnemyDefense = 0.0,
        baseEnemyAttackSpeed = 1.2,
        baseEnemyAccuracy = 0.86,
        baseEnemyWill = 0.06,
        baseEnemyCritChance = 0.09,
        enemyType = EnemyType.ELECTRIC
    ),

    // HOLY тип - Святой слизень
    HOLY_SLIME(
        displayEnemyName = "Holy Slime",
        baseEnemyDamage = 18,
        baseEnemyMageDamage = 32,
        baseEnemyMaxHealth = 95,
        baseEnemyDefense = 0.2,
        baseEnemyAttackSpeed = 1.0,
        baseEnemyAccuracy = 0.92,
        baseEnemyWill = 0.2,
        baseEnemyCritChance = 0.07,
        enemyType = EnemyType.HOLY
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

