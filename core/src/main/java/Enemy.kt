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
    ),

    DARK(
        displayName = "Темный",
        specialEffect = "Крадет 10% нанесенного урона в здоровье"
    ),

    BLOOD(
        displayName = "Кровавый",
        specialEffect = "Атаки накладывают 'Кровотечение' (3% урона за ход)"
    ),

    BERSERK(
        displayName = "Берсерк",
        specialEffect = "+30% урона при здоровье ниже 50%"
    ),

    UNDEAD(
        displayName = "Нежить",
        specialEffect = "При смерти имеет 30% шанс воскреснуть с 1 HP"
    ),

    BUNNY(
        displayName = "Кроличий",
        specialEffect = "Имеет 50% шанс уклониться от первой атаки в бою"
    );
}



// враги
enum class Enemy
    (
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
    // ===== СЛИЗНИ =====
    GREEN_SLIME(
        displayEnemyName = "Green Slime",
        baseEnemyDamage = 10,
        baseEnemyMageDamage = 10,
        baseEnemyMaxHealth = 50,
        baseEnemyDefense = 0.0,
        baseEnemyAttackSpeed = 1.0,
        baseEnemyAccuracy = 0.8,
        baseEnemyWill = 0.0,
        baseEnemyCritChance = 0.00,
        enemyType = EnemyType.POISON
    ),

    RED_SLIME(
        displayEnemyName = "Red Slime",
        baseEnemyDamage = 25,
        baseEnemyMageDamage = 10,
        baseEnemyMaxHealth = 40,
        baseEnemyDefense = 0.0,
        baseEnemyAttackSpeed = 1.0,
        baseEnemyAccuracy = 0.8,
        baseEnemyWill = 0.02,
        baseEnemyCritChance = 0.00,
        enemyType = EnemyType.FIRE,
    ),

    BLUE_SLIME(
        displayEnemyName = "Blue Slime",
        baseEnemyDamage = 10,
        baseEnemyMageDamage = 10,
        baseEnemyMaxHealth = 50,
        baseEnemyDefense = 0.1,
        baseEnemyAttackSpeed = 1.0,
        baseEnemyAccuracy = 0.8,
        baseEnemyWill = 0.0,
        baseEnemyCritChance = 0.01,
        enemyType = EnemyType.WATER
    ),

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
    ),

    // ===== ЭЛЕМЕНТАЛИ =====
    FIRE_ELEMENTAL(
        displayEnemyName = "Fire Elemental",
        baseEnemyDamage = 35,
        baseEnemyMageDamage = 45,
        baseEnemyMaxHealth = 180,
        baseEnemyDefense = 0.1,
        baseEnemyAttackSpeed = 1.1,
        baseEnemyAccuracy = 0.85,
        baseEnemyWill = 0.1,
        baseEnemyCritChance = 0.05,
        enemyType = EnemyType.FIRE,
        isElite = true
    ),

    WATER_SPIRIT(
        displayEnemyName = "Water Spirit",
        baseEnemyDamage = 20,
        baseEnemyMageDamage = 35,
        baseEnemyMaxHealth = 150,
        baseEnemyDefense = 0.15,
        baseEnemyAttackSpeed = 1.2,
        baseEnemyAccuracy = 0.9,
        baseEnemyWill = 0.15,
        baseEnemyCritChance = 0.05,
        enemyType = EnemyType.WATER
    ),

    STORM_HARPY(
        displayEnemyName = "Storm Harpy",
        baseEnemyDamage = 30,
        baseEnemyMageDamage = 20,
        baseEnemyMaxHealth = 130,
        baseEnemyDefense = 0.05,
        baseEnemyAttackSpeed = 1.5,
        baseEnemyAccuracy = 0.88,
        baseEnemyWill = 0.08,
        baseEnemyCritChance = 0.1,
        enemyType = EnemyType.WIND
    ),

    ROCK_GOLEM(
        displayEnemyName = "Rock Golem",
        baseEnemyDamage = 40,
        baseEnemyMageDamage = 10,
        baseEnemyMaxHealth = 300,
        baseEnemyDefense = 0.35,
        baseEnemyAttackSpeed = 0.6,
        baseEnemyAccuracy = 0.7,
        baseEnemyWill = 0.2,
        baseEnemyCritChance = 0.01,
        enemyType = EnemyType.EARTH,
        isElite = true
    ),

    FROST_WRAITH(
        displayEnemyName = "Frost Wraith",
        baseEnemyDamage = 28,
        baseEnemyMageDamage = 40,
        baseEnemyMaxHealth = 160,
        baseEnemyDefense = 0.15,
        baseEnemyAttackSpeed = 1.0,
        baseEnemyAccuracy = 0.9,
        baseEnemyWill = 0.12,
        baseEnemyCritChance = 0.08,
        enemyType = EnemyType.ICE
    ),

    SHADOW_WRAITH(
        displayEnemyName = "Shadow Wraith",
        baseEnemyDamage = 45,
        baseEnemyMageDamage = 45,
        baseEnemyMaxHealth = 140,
        baseEnemyDefense = 0.1,
        baseEnemyAttackSpeed = 1.3,
        baseEnemyAccuracy = 0.95,
        baseEnemyWill = 0.25,
        baseEnemyCritChance = 0.15,
        enemyType = EnemyType.CURSED,
        isElite = true
    ),

    STORM_SPIDER(
        displayEnemyName = "Storm Spider",
        baseEnemyDamage = 25,
        baseEnemyMageDamage = 30,
        baseEnemyMaxHealth = 120,
        baseEnemyDefense = 0.05,
        baseEnemyAttackSpeed = 1.4,
        baseEnemyAccuracy = 0.87,
        baseEnemyWill = 0.1,
        baseEnemyCritChance = 0.1,
        enemyType = EnemyType.ELECTRIC
    ),

    VENOM_DRAKE(
        displayEnemyName = "Venom Drake",
        baseEnemyDamage = 38,
        baseEnemyMageDamage = 25,
        baseEnemyMaxHealth = 200,
        baseEnemyDefense = 0.2,
        baseEnemyAttackSpeed = 1.0,
        baseEnemyAccuracy = 0.85,
        baseEnemyWill = 0.1,
        baseEnemyCritChance = 0.05,
        enemyType = EnemyType.POISON
    ),

    CELESTIAL_GUARDIAN(
        displayEnemyName = "Celestial Guardian",
        baseEnemyDamage = 30,
        baseEnemyMageDamage = 50,
        baseEnemyMaxHealth = 220,
        baseEnemyDefense = 0.25,
        baseEnemyAttackSpeed = 1.0,
        baseEnemyAccuracy = 0.95,
        baseEnemyWill = 0.3,
        baseEnemyCritChance = 0.1,
        enemyType = EnemyType.HOLY,
        isElite = true
    ),

    // ===== СКЕЛЕТЫ =====
    SKELETON_WARRIOR(
        displayEnemyName = "Skeleton Warrior",
        baseEnemyDamage = 22,
        baseEnemyMageDamage = 5,
        baseEnemyMaxHealth = 80,
        baseEnemyDefense = 0.1,
        baseEnemyAttackSpeed = 1.0,
        baseEnemyAccuracy = 0.82,
        baseEnemyWill = 0.05,
        baseEnemyCritChance = 0.03,
        enemyType = EnemyType.UNDEAD
    ),

    SKELETON_ARCHER(
        displayEnemyName = "Skeleton Archer",
        baseEnemyDamage = 28,
        baseEnemyMageDamage = 8,
        baseEnemyMaxHealth = 60,
        baseEnemyDefense = 0.0,
        baseEnemyAttackSpeed = 1.3,
        baseEnemyAccuracy = 0.9,
        baseEnemyWill = 0.03,
        baseEnemyCritChance = 0.08,
        enemyType = EnemyType.UNDEAD
    ),

    SKELETON_MAGE(
        displayEnemyName = "Skeleton Mage",
        baseEnemyDamage = 8,
        baseEnemyMageDamage = 35,
        baseEnemyMaxHealth = 55,
        baseEnemyDefense = 0.0,
        baseEnemyAttackSpeed = 1.1,
        baseEnemyAccuracy = 0.88,
        baseEnemyWill = 0.1,
        baseEnemyCritChance = 0.05,
        enemyType = EnemyType.CURSED
    ),

    SKELETON_KNIGHT(
        displayEnemyName = "Skeleton Knight",
        baseEnemyDamage = 35,
        baseEnemyMageDamage = 10,
        baseEnemyMaxHealth = 150,
        baseEnemyDefense = 0.25,
        baseEnemyAttackSpeed = 0.9,
        baseEnemyAccuracy = 0.85,
        baseEnemyWill = 0.15,
        baseEnemyCritChance = 0.05,
        enemyType = EnemyType.UNDEAD,
        isElite = true
    ),



    // ===== ГОБЛИНЫ =====
    GOBLIN_SCOUT(
        displayEnemyName = "Goblin Scout",
        baseEnemyDamage = 15,
        baseEnemyMageDamage = 5,
        baseEnemyMaxHealth = 45,
        baseEnemyDefense = 0.0,
        baseEnemyAttackSpeed = 1.5,
        baseEnemyAccuracy = 0.85,
        baseEnemyWill = 0.0,
        baseEnemyCritChance = 0.05,
        enemyType = EnemyType.WIND
    ),

    GOBLIN_WARRIOR(
        displayEnemyName = "Goblin Warrior",
        baseEnemyDamage = 20,
        baseEnemyMageDamage = 3,
        baseEnemyMaxHealth = 65,
        baseEnemyDefense = 0.05,
        baseEnemyAttackSpeed = 1.2,
        baseEnemyAccuracy = 0.8,
        baseEnemyWill = 0.02,
        baseEnemyCritChance = 0.03,
        enemyType = EnemyType.NO_TYPE
    ),

    GOBLIN_SHAMAN(
        displayEnemyName = "Goblin Shaman",
        baseEnemyDamage = 10,
        baseEnemyMageDamage = 30,
        baseEnemyMaxHealth = 50,
        baseEnemyDefense = 0.0,
        baseEnemyAttackSpeed = 1.1,
        baseEnemyAccuracy = 0.85,
        baseEnemyWill = 0.1,
        baseEnemyCritChance = 0.05,
        enemyType = EnemyType.POISON
    ),

    GOBLIN_BERSERKER(
        displayEnemyName = "Goblin Berserker",
        baseEnemyDamage = 35,
        baseEnemyMageDamage = 5,
        baseEnemyMaxHealth = 90,
        baseEnemyDefense = 0.0,
        baseEnemyAttackSpeed = 1.4,
        baseEnemyAccuracy = 0.75,
        baseEnemyWill = 0.05,
        baseEnemyCritChance = 0.1,
        enemyType = EnemyType.BERSERK
    ),

    GOBLIN_CHIEFTAIN(
        displayEnemyName = "Goblin Chieftain",
        baseEnemyDamage = 40,
        baseEnemyMageDamage = 15,
        baseEnemyMaxHealth = 200,
        baseEnemyDefense = 0.15,
        baseEnemyAttackSpeed = 1.1,
        baseEnemyAccuracy = 0.85,
        baseEnemyWill = 0.15,
        baseEnemyCritChance = 0.08,
        enemyType = EnemyType.BLOOD,
        isElite = true
    ),

    // ===== ПАУКИ =====
    SPIDER_HATCHLING(
        displayEnemyName = "Spider Hatchling",
        baseEnemyDamage = 12,
        baseEnemyMageDamage = 5,
        baseEnemyMaxHealth = 35,
        baseEnemyDefense = 0.0,
        baseEnemyAttackSpeed = 1.6,
        baseEnemyAccuracy = 0.85,
        baseEnemyWill = 0.0,
        baseEnemyCritChance = 0.05,
        enemyType = EnemyType.POISON
    ),

    VENOM_SPIDER(
        displayEnemyName = "Venom Spider",
        baseEnemyDamage = 20,
        baseEnemyMageDamage = 15,
        baseEnemyMaxHealth = 70,
        baseEnemyDefense = 0.05,
        baseEnemyAttackSpeed = 1.3,
        baseEnemyAccuracy = 0.88,
        baseEnemyWill = 0.05,
        baseEnemyCritChance = 0.08,
        enemyType = EnemyType.POISON
    ),

    WEB_SPIDER(
        displayEnemyName = "Web Spider",
        baseEnemyDamage = 15,
        baseEnemyMageDamage = 10,
        baseEnemyMaxHealth = 55,
        baseEnemyDefense = 0.0,
        baseEnemyAttackSpeed = 1.4,
        baseEnemyAccuracy = 0.9,
        baseEnemyWill = 0.08,
        baseEnemyCritChance = 0.05,
        enemyType = EnemyType.ICE  // Замедление как заморозка
    ),

    CRYSTAL_SPIDER(
        displayEnemyName = "Crystal Spider",
        baseEnemyDamage = 25,
        baseEnemyMageDamage = 20,
        baseEnemyMaxHealth = 100,
        baseEnemyDefense = 0.15,
        baseEnemyAttackSpeed = 1.2,
        baseEnemyAccuracy = 0.9,
        baseEnemyWill = 0.1,
        baseEnemyCritChance = 0.08,
        enemyType = EnemyType.EARTH
    ),

    SHADOW_SPIDER(
        displayEnemyName = "Shadow Spider",
        baseEnemyDamage = 30,
        baseEnemyMageDamage = 25,
        baseEnemyMaxHealth = 85,
        baseEnemyDefense = 0.1,
        baseEnemyAttackSpeed = 1.4,
        baseEnemyAccuracy = 0.92,
        baseEnemyWill = 0.12,
        baseEnemyCritChance = 0.1,
        enemyType = EnemyType.DARK
    ),

    // ===== ТРОЛЛИ =====
    FOREST_TROLL(
        displayEnemyName = "Forest Troll",
        baseEnemyDamage = 35,
        baseEnemyMageDamage = 10,
        baseEnemyMaxHealth = 180,
        baseEnemyDefense = 0.2,
        baseEnemyAttackSpeed = 0.8,
        baseEnemyAccuracy = 0.75,
        baseEnemyWill = 0.1,
        baseEnemyCritChance = 0.05,
        enemyType = EnemyType.EARTH
    ),

    CAVE_TROLL(
        displayEnemyName = "Cave Troll",
        baseEnemyDamage = 45,
        baseEnemyMageDamage = 15,
        baseEnemyMaxHealth = 250,
        baseEnemyDefense = 0.3,
        baseEnemyAttackSpeed = 0.7,
        baseEnemyAccuracy = 0.7,
        baseEnemyWill = 0.15,
        baseEnemyCritChance = 0.03,
        enemyType = EnemyType.EARTH,
        isElite = true
    ),

    // ===== ОРКИ =====
    ORC_GRUNT(
        displayEnemyName = "Orc Grunt",
        baseEnemyDamage = 30,
        baseEnemyMageDamage = 5,
        baseEnemyMaxHealth = 100,
        baseEnemyDefense = 0.1,
        baseEnemyAttackSpeed = 1.0,
        baseEnemyAccuracy = 0.8,
        baseEnemyWill = 0.05,
        baseEnemyCritChance = 0.05,
        enemyType = EnemyType.NO_TYPE
    ),

    ORC_BERSERKER(
        displayEnemyName = "Orc Berserker",
        baseEnemyDamage = 45,
        baseEnemyMageDamage = 5,
        baseEnemyMaxHealth = 120,
        baseEnemyDefense = 0.05,
        baseEnemyAttackSpeed = 1.3,
        baseEnemyAccuracy = 0.75,
        baseEnemyWill = 0.05,
        baseEnemyCritChance = 0.12,
        enemyType = EnemyType.BERSERK
    ),

    ORC_SHAMAN(
        displayEnemyName = "Orc Shaman",
        baseEnemyDamage = 20,
        baseEnemyMageDamage = 40,
        baseEnemyMaxHealth = 90,
        baseEnemyDefense = 0.05,
        baseEnemyAttackSpeed = 1.0,
        baseEnemyAccuracy = 0.88,
        baseEnemyWill = 0.15,
        baseEnemyCritChance = 0.08,
        enemyType = EnemyType.FIRE
    ),

    // ===== КРОЛИКИ =====
    RABBIT(
        displayEnemyName = "Wild Rabbit",
        baseEnemyDamage = 8,
        baseEnemyMageDamage = 0,
        baseEnemyMaxHealth = 25,
        baseEnemyDefense = 0.0,
        baseEnemyAttackSpeed = 1.8,
        baseEnemyAccuracy = 0.9,
        baseEnemyWill = 0.0,
        baseEnemyCritChance = 0.02,
        enemyType = EnemyType.BUNNY
    ),

    ANGRY_RABBIT(
        displayEnemyName = "Angry Rabbit",
        baseEnemyDamage = 18,
        baseEnemyMageDamage = 0,
        baseEnemyMaxHealth = 40,
        baseEnemyDefense = 0.0,
        baseEnemyAttackSpeed = 1.6,
        baseEnemyAccuracy = 0.88,
        baseEnemyWill = 0.02,
        baseEnemyCritChance = 0.05,
        enemyType = EnemyType.BERSERK
    ),

    SHADOW_RABBIT(
        displayEnemyName = "Shadow Rabbit",
        baseEnemyDamage = 25,
        baseEnemyMageDamage = 20,
        baseEnemyMaxHealth = 55,
        baseEnemyDefense = 0.08,
        baseEnemyAttackSpeed = 1.5,
        baseEnemyAccuracy = 0.9,
        baseEnemyWill = 0.1,
        baseEnemyCritChance = 0.08,
        enemyType = EnemyType.DARK
    ),

    GIANT_RABBIT(
        displayEnemyName = "Giant Rabbit",
        baseEnemyDamage = 35,
        baseEnemyMageDamage = 10,
        baseEnemyMaxHealth = 150,
        baseEnemyDefense = 0.15,
        baseEnemyAttackSpeed = 1.1,
        baseEnemyAccuracy = 0.82,
        baseEnemyWill = 0.1,
        baseEnemyCritChance = 0.05,
        enemyType = EnemyType.EARTH,
        isElite = true
    ),

    // ===== ВОЛКИ =====
    WOLF(
        displayEnemyName = "Wolf",
        baseEnemyDamage = 22,
        baseEnemyMageDamage = 0,
        baseEnemyMaxHealth = 65,
        baseEnemyDefense = 0.05,
        baseEnemyAttackSpeed = 1.3,
        baseEnemyAccuracy = 0.85,
        baseEnemyWill = 0.05,
        baseEnemyCritChance = 0.05,
        enemyType = EnemyType.NO_TYPE
    ),

    DIRE_WOLF(
        displayEnemyName = "Dire Wolf",
        baseEnemyDamage = 32,
        baseEnemyMageDamage = 0,
        baseEnemyMaxHealth = 95,
        baseEnemyDefense = 0.1,
        baseEnemyAttackSpeed = 1.2,
        baseEnemyAccuracy = 0.88,
        baseEnemyWill = 0.08,
        baseEnemyCritChance = 0.08,
        enemyType = EnemyType.NO_TYPE
    ),

    ALPHA_WOLF(
        displayEnemyName = "Alpha Wolf",
        baseEnemyDamage = 40,
        baseEnemyMageDamage = 0,
        baseEnemyMaxHealth = 130,
        baseEnemyDefense = 0.15,
        baseEnemyAttackSpeed = 1.4,
        baseEnemyAccuracy = 0.9,
        baseEnemyWill = 0.15,
        baseEnemyCritChance = 0.1,
        enemyType = EnemyType.NO_TYPE,
        isElite = true
    ),

    FROST_WOLF(
        displayEnemyName = "Frost Wolf",
        baseEnemyDamage = 28,
        baseEnemyMageDamage = 20,
        baseEnemyMaxHealth = 85,
        baseEnemyDefense = 0.1,
        baseEnemyAttackSpeed = 1.25,
        baseEnemyAccuracy = 0.87,
        baseEnemyWill = 0.1,
        baseEnemyCritChance = 0.07,
        enemyType = EnemyType.ICE
    ),

    FIRE_WOLF(
        displayEnemyName = "Fire Wolf",
        baseEnemyDamage = 35,
        baseEnemyMageDamage = 15,
        baseEnemyMaxHealth = 75,
        baseEnemyDefense = 0.05,
        baseEnemyAttackSpeed = 1.35,
        baseEnemyAccuracy = 0.85,
        baseEnemyWill = 0.08,
        baseEnemyCritChance = 0.08,
        enemyType = EnemyType.FIRE
    ),

    SHADOW_WOLF(
        displayEnemyName = "Shadow Wolf",
        baseEnemyDamage = 38,
        baseEnemyMageDamage = 25,
        baseEnemyMaxHealth = 90,
        baseEnemyDefense = 0.1,
        baseEnemyAttackSpeed = 1.4,
        baseEnemyAccuracy = 0.92,
        baseEnemyWill = 0.12,
        baseEnemyCritChance = 0.1,
        enemyType = EnemyType.DARK
    ),

    STORM_WOLF(
        displayEnemyName = "Storm Wolf",
        baseEnemyDamage = 30,
        baseEnemyMageDamage = 30,
        baseEnemyMaxHealth = 80,
        baseEnemyDefense = 0.08,
        baseEnemyAttackSpeed = 1.45,
        baseEnemyAccuracy = 0.88,
        baseEnemyWill = 0.1,
        baseEnemyCritChance = 0.09,
        enemyType = EnemyType.ELECTRIC
    ),

    WIND_WOLF(
        displayEnemyName = "Wind Wolf",
        baseEnemyDamage = 25,
        baseEnemyMageDamage = 15,
        baseEnemyMaxHealth = 70,
        baseEnemyDefense = 0.05,
        baseEnemyAttackSpeed = 1.6,
        baseEnemyAccuracy = 0.9,
        baseEnemyWill = 0.08,
        baseEnemyCritChance = 0.08,
        enemyType = EnemyType.WIND
    ),

    UNDEAD_WOLF(
        displayEnemyName = "Undead Wolf",
        baseEnemyDamage = 35,
        baseEnemyMageDamage = 15,
        baseEnemyMaxHealth = 88,
        baseEnemyDefense = 0.08,
        baseEnemyAttackSpeed = 1.2,
        baseEnemyAccuracy = 0.84,
        baseEnemyWill = 0.15,
        baseEnemyCritChance = 0.07,
        enemyType = EnemyType.UNDEAD
    ),

    BERSERK_WOLF(
        displayEnemyName = "Berserk Wolf",
        baseEnemyDamage = 50,
        baseEnemyMageDamage = 10,
        baseEnemyMaxHealth = 95,
        baseEnemyDefense = 0.05,
        baseEnemyAttackSpeed = 1.5,
        baseEnemyAccuracy = 0.82,
        baseEnemyWill = 0.1,
        baseEnemyCritChance = 0.12,
        enemyType = EnemyType.BERSERK
    ),
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


//// ===== БОССЫ =====
//FENRIR(
//    displayEnemyName = "Fenrir",
//    baseEnemyDamage = 70,
//    baseEnemyMageDamage = 40,
//    baseEnemyMaxHealth = 400,
//    baseEnemyDefense = 0.25,
//    baseEnemyAttackSpeed = 1.5,
//    baseEnemyAccuracy = 0.95,
//    baseEnemyWill = 0.3,
//    baseEnemyCritChance = 0.15,
//    enemyType = EnemyType.ICE,
//    isBoss = true
//),
//
//WOLF_PACK_LEADER(
//    displayEnemyName = "Wolf Pack Leader",
//    baseEnemyDamage = 60,
//    baseEnemyMageDamage = 30,
//    baseEnemyMaxHealth = 350,
//    baseEnemyDefense = 0.2,
//    baseEnemyAttackSpeed = 1.4,
//    baseEnemyAccuracy = 0.92,
//    baseEnemyWill = 0.25,
//    baseEnemyCritChance = 0.12,
//    enemyType = EnemyType.BLOOD,
//    isBoss = true
//),
//
//SPIRIT_WOLF(
//    displayEnemyName = "Spirit Wolf",
//    baseEnemyDamage = 50,
//    baseEnemyMageDamage = 60,
//    baseEnemyMaxHealth = 300,
//    baseEnemyDefense = 0.15,
//    baseEnemyAttackSpeed = 1.6,
//    baseEnemyAccuracy = 0.95,
//    baseEnemyWill = 0.35,
//    baseEnemyCritChance = 0.15,
//    enemyType = EnemyType.DARK,
//    isBoss = true
//),
//
//WHITE_WOLF(
//    displayEnemyName = "White Wolf",
//    baseEnemyDamage = 38,
//    baseEnemyMageDamage = 35,
//    baseEnemyMaxHealth = 140,
//    baseEnemyDefense = 0.15,
//    baseEnemyAttackSpeed = 1.3,
//    baseEnemyAccuracy = 0.93,
//    baseEnemyWill = 0.2,
//    baseEnemyCritChance = 0.1,
//    enemyType = EnemyType.HOLY,
//    isElite = true
//),
//
//BLACK_WOLF(
//    displayEnemyName = "Black Wolf",
//    baseEnemyDamage = 55,
//    baseEnemyMageDamage = 25,
//    baseEnemyMaxHealth = 120,
//    baseEnemyDefense = 0.12,
//    baseEnemyAttackSpeed = 1.45,
//    baseEnemyAccuracy = 0.9,
//    baseEnemyWill = 0.15,
//    baseEnemyCritChance = 0.12,
//    enemyType = EnemyType.DARK,
//    isElite = true
//),

//    BONE_LORD(
//        displayEnemyName = "Bone Lord",
//        baseEnemyDamage = 50,
//        baseEnemyMageDamage = 50,
//        baseEnemyMaxHealth = 350,
//        baseEnemyDefense = 0.3,
//        baseEnemyAttackSpeed = 1.0,
//        baseEnemyAccuracy = 0.9,
//        baseEnemyWill = 0.3,
//        baseEnemyCritChance = 0.1,
//        enemyType = EnemyType.DARK,
//        isBoss = true
//    ),
//
//    RABBIT_KING(
//        displayEnemyName = "Rabbit King",
//        baseEnemyDamage = 50,
//        baseEnemyMageDamage = 35,
//        baseEnemyMaxHealth = 300,
//        baseEnemyDefense = 0.2,
//        baseEnemyAttackSpeed = 1.4,
//        baseEnemyAccuracy = 0.92,
//        baseEnemyWill = 0.25,
//        baseEnemyCritChance = 0.1,
//        enemyType = EnemyType.HOLY,
//        isBoss = true
//    ),
//
//    GOBLIN_KING(
//        displayEnemyName = "Goblin King",
//        baseEnemyDamage = 55,
//        baseEnemyMageDamage = 30,
//        baseEnemyMaxHealth = 400,
//        baseEnemyDefense = 0.2,
//        baseEnemyAttackSpeed = 1.2,
//        baseEnemyAccuracy = 0.9,
//        baseEnemyWill = 0.25,
//        baseEnemyCritChance = 0.12,
//        enemyType = EnemyType.BERSERK,
//        isBoss = true
//    ),
//
//    SPIDER_QUEEN(
//        displayEnemyName = "Spider Queen",
//        baseEnemyDamage = 45,
//        baseEnemyMageDamage = 35,
//        baseEnemyMaxHealth = 300,
//        baseEnemyDefense = 0.2,
//        baseEnemyAttackSpeed = 1.2,
//        baseEnemyAccuracy = 0.92,
//        baseEnemyWill = 0.25,
//        baseEnemyCritChance = 0.1,
//        enemyType = EnemyType.BLOOD,
//        isBoss = true
//    ),
//
//    ORC_WARLORD(
//        displayEnemyName = "Orc Warlord",
//        baseEnemyDamage = 60,
//        baseEnemyMageDamage = 20,
//        baseEnemyMaxHealth = 350,
//        baseEnemyDefense = 0.25,
//        baseEnemyAttackSpeed = 1.1,
//        baseEnemyAccuracy = 0.85,
//        baseEnemyWill = 0.2,
//        baseEnemyCritChance = 0.1,
//        enemyType = EnemyType.BLOOD,
//        isBoss = true
//    );
