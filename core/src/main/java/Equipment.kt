package ru.triplethall.rpgturnbased

import kotlin.collections.joinToString

enum class EquipmentType {
    HELMET,
    CHESTPLATE,
    BOOTS,
    SHIELD,
    WEAPON,
    RUNE
}

enum class WeaponType {
    // Knight
    SWORD,
    AXE,
    GREATSWORD,
    SPEAR,

    // Mage
    STAFF,
    ORB,
    BOOK,

    // Assassin
    DAGGER,
    KNIFE,

    // Archer
    BOW,
    CROSSBOW,

    // Priest
    // he uses staffs

    // Joker
    CARDS,

    // Berserk
    // he uses axes

    // Shaman
    // he uses books

    // Monarch
    GLOVES,

    // Adventurer
//    SWORD,

    // Swordmaster
    KATANA,
    DUAL_SWORDS,

    // Alchemist
    FLASK,
}

data class EquipmentItem(
    val id: String,                          // Уникальный ID
    val name: String,                        // Название
    val type: EquipmentType,                 // Тип снаряжения
    val weaponType: WeaponType? = null,      // Тип оружия (если это оружие)
    val allowedClasses: List<PlayerClasses>, // Классы, которые могут использовать

    val damageBonus: Int = 0,
    val mageDamageBonus: Int = 0,
    val healthBonus: Int = 0,
    val manaBonus: Int = 0,
    val defenseBonus: Double = 0.0,          // self-explanatory
    val attackSpeedBonus: Double = 0.0,
    val accuracyBonus: Double = 0.0,
    val willBonus: Double = 0.0,
    val luckBonus: Double = 0.0,
    val critChanceBonus: Double = 0.0,

    val description: String,                 // Описание
    val level: Int = 1                       // Требуемый уровень
) {
    fun canBeUsedBy(playerClass: PlayerClasses): Boolean =
        playerClass in allowedClasses || allowedClasses.isEmpty()

    fun getStatsDescription(): String {
        val bonuses = mutableListOf<String>()
        if (damageBonus != 0) {
            val sign = if (damageBonus > 0) "+" else ""
            bonuses.add("Урон: $sign$damageBonus")
        }
        if (mageDamageBonus != 0) {
            val sign = if (mageDamageBonus > 0) "+" else ""
            bonuses.add("Маг. урон: $sign$mageDamageBonus")
        }
        if (healthBonus != 0) {
            val sign = if (healthBonus > 0) "+" else ""
            bonuses.add("Здоровье: $sign$healthBonus")
        }
        if (manaBonus != 0) {
            val sign = if (manaBonus > 0) "+" else ""
            bonuses.add("Мана: $sign$manaBonus")
        }
        if (defenseBonus != 0.0) {
            val percent = (defenseBonus * 100).toInt()
            val sign = if (percent > 0) "+" else ""
            bonuses.add("Защита: $sign$percent%")
        }
        if (attackSpeedBonus != 0.0) {
            val percent = (attackSpeedBonus * 100).toInt()
            val sign = if (percent > 0) "+" else ""
            bonuses.add("Скорость атаки: $sign$percent%")
        }
        if (accuracyBonus != 0.0) {
            val percent = (accuracyBonus * 100).toInt()
            val sign = if (percent > 0) "+" else ""
            bonuses.add("Точность: $sign$percent%")
        }
        if (willBonus != 0.0) {
            val percent = (willBonus * 100).toInt()
            val sign = if (percent > 0) "+" else ""
            bonuses.add("Воля: $sign$percent%")
        }
        if (luckBonus != 0.0) {
            val percent = (luckBonus * 100).toInt()
            val sign = if (percent > 0) "+" else ""
            bonuses.add("Удача: $sign$percent%")
        }
        if (critChanceBonus != 0.0) {
            val percent = (critChanceBonus * 100).toInt()
            val sign = if (percent > 0) "+" else ""
            bonuses.add("Шанс крита: $sign$percent%")
        }

        return """
        |$name (Ур. $level)
        |${description}
        |Классы: ${allowedClasses.joinToString { it.displayName }}
        |${if (bonuses.isNotEmpty()) bonuses.joinToString("\n|") else "Нет бонусов"}
        """.trimMargin()
    }
}

class PlayerEquipment {
    private val runes = arrayOfNulls<EquipmentItem>(2)
    fun getRune(slot: Int): EquipmentItem? = runes.getOrNull(slot)

    private val equipped = mutableMapOf<EquipmentType, EquipmentItem>()
    // получить данные о предмете по типу
    fun getEquipped(type: EquipmentType): EquipmentItem? = equipped[type]
    // получить данные о всех предметах
    fun getAllEquipped(): Map<EquipmentType, EquipmentItem> = equipped.toMap()
    // экипировка предмета
    fun equip(item: EquipmentItem, player: Player, playerClass: PlayerClasses): Boolean {
        if (!item.canBeUsedBy(playerClass)) {
            return false
        }
        if (item.level > player.level) {
            return false
        }
        if (equipped.containsKey(item.type)) {
            return false
        }
        equipped[item.type] = item
        player.recalculateStats()
        return true
    }

    fun unequip(type: EquipmentType, player: Player? = null): Boolean {
        val item = equipped.remove(type) ?: return false
        player?.recalculateStats()
        return true
    }

    fun equipRune(rune: EquipmentItem, slot: Int, player: Player): Boolean {
        if (slot !in 0..1) return false
        if (runes[slot] != null) return false
        runes[slot] = rune
        player.recalculateStats()
        return true
    }

    fun unequipRune(slot: Int, player: Player): Boolean {
        val rune = runes[slot] ?: return false
        runes[slot] = null
        player.recalculateStats()
        return true
    }
}

object EquipmentDatabase {

    // ===== KNIGHT / BERSERK =====
    // Swords
    val WOODEN_SWORD = EquipmentItem(
        id = "sword_01",
        name = "Wooden Sword",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.SWORD,
        allowedClasses = listOf(PlayerClasses.KNIGHT, PlayerClasses.ADVENTURIST),
        damageBonus = 1,
        description = "The beginning of Journey",
        level = 1
    )
    val CHAMPION_SWORD = EquipmentItem(
        id = "sword_02",
        name = "Champion's Sword",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.SWORD,
        allowedClasses = listOf(PlayerClasses.KNIGHT, PlayerClasses.ADVENTURIST),
        damageBonus = 25,
        defenseBonus = 0.05,
        description = "It's not a rip-off, I swear!",
        level = 40
    )
    val ANGEL_BOJII = EquipmentItem(
        id = "sword_03",
        name = "Ангелы Божии",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.SWORD,
        allowedClasses = listOf(PlayerClasses.KNIGHT),
        damageBonus = 10,
        description = "The blade, forged by an angels themselves.",
        level = 100
    )
    val HERO_SWORD = EquipmentItem(
        id = "sword_04",
        name = "Hero's Sword",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.SWORD,
        allowedClasses = listOf(PlayerClasses.KNIGHT, PlayerClasses.ADVENTURIST),
        damageBonus = 55,
        defenseBonus = 0.10,
        willBonus = 0.15,
        description = "A blade worthy of a true hero.",
        level = 75
    )

    // Greatswords
    val DEEPSEA_ANCHOR = EquipmentItem(
        id = "greatsword_20",
        name = "Deepsea anchor",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.GREATSWORD,
        allowedClasses = listOf(PlayerClasses.KNIGHT),
        damageBonus = 100,
        critChanceBonus = -1.0,
        attackSpeedBonus = -0.30,
        description = "Anchor, that holds the power of the deepsea creatures.",
        level = 95
    )
    val DARKSTEEL_GREATSWORD = EquipmentItem(
        id = "greatsword_01",
        name = "Darksteel greatsword",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.GREATSWORD,
        allowedClasses = listOf(PlayerClasses.KNIGHT),
        damageBonus = 18,
        attackSpeedBonus = -0.15,
        defenseBonus = 0.05,
        description = "Wrought from durable darksteel.",
        level = 10
    )
    val RAILBLADE = EquipmentItem(
        id = "greatsword_02",
        name = "Railblade",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.GREATSWORD,
        allowedClasses = listOf(PlayerClasses.KNIGHT),
        damageBonus = 75,
        attackSpeedBonus = -0.10,
        critChanceBonus = 0.15,
        description = "This ancient blade holds the power of demonslayers.",
        level = 70
    )
    val COLOSSUS_BLADE = EquipmentItem(
        id = "greatsword_03",
        name = "Colossus Blade",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.GREATSWORD,
        allowedClasses = listOf(PlayerClasses.KNIGHT),
        damageBonus = 120,
        attackSpeedBonus = -0.35,
        healthBonus = 150,
        description = "So heavy only the strongest can lift it.",
        level = 120
    )

    // Axes
    val LUMBER_AXE = EquipmentItem(
        id = "axe_01",
        name = "Old lumber axe",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.AXE,
        allowedClasses = listOf(PlayerClasses.KNIGHT, PlayerClasses.ADVENTURIST),
        damageBonus = 10,
        attackSpeedBonus = -0.05,
        description = "Old and rusty axe from your grand grandpa.",
        level = 1
    )
    val BERSERKER_CHOPPA = EquipmentItem(
        id = "axe_02",
        name = "Berserker's Chopper",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.AXE,
        allowedClasses = listOf(PlayerClasses.BERSERK),
        damageBonus = 45,
        critChanceBonus = 0.15,
        healthBonus = -25,
        description = "A massive axe that craves blood as much as it's wielder",
        level = 35
    )
    val GRAN_SUDARUSKA = EquipmentItem(
        id = "axe_15",
        name = "Gran Sudaruska",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.AXE,
        allowedClasses = listOf(PlayerClasses.KNIGHT, PlayerClasses.MAGE),
        damageBonus = 25,
        mageDamageBonus = 25,
        attackSpeedBonus = -0.25,
        description = "In her dying breaths, Sudaruska infused herself into this weapon, cutting through hundreds of enemies. Now that her mortal shell is no more, she has become Gran Sudaruska",
        level = 125
    )
    val BLOOD_MOON_AXE = EquipmentItem(
        id = "axe_03",
        name = "Blood Moon Axe",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.AXE,
        allowedClasses = listOf(PlayerClasses.BERSERK),
        damageBonus = 80,
        critChanceBonus = 0.25,
        attackSpeedBonus = -0.10,
        healthBonus = -75,
        description = "Under the blood moon, this axe becomes unstoppable.",
        level = 80
    )
    val FROSTBITE_AXE = EquipmentItem(
        id = "axe_04",
        name = "Frostbite Axe",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.AXE,
        allowedClasses = listOf(PlayerClasses.KNIGHT, PlayerClasses.BERSERK),
        damageBonus = 50,
        mageDamageBonus = 20,
        attackSpeedBonus = -0.05,
        description = "Cold as the northern winds.",
        level = 55
    )

    // Spears
    val IRON_SPEAR = EquipmentItem(
        id = "spear_01",
        name = "Iron spear",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.SPEAR,
        allowedClasses = listOf(PlayerClasses.KNIGHT),
        damageBonus = 12,
        accuracyBonus = 0.05,
        description = "A spear, perfect for poking at a long distances.",
        level = 10
    )
    val TRIDENT_SPEAR = EquipmentItem(
        id = "spear_02",
        name = "Trident spear",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.SPEAR,
        allowedClasses = listOf(PlayerClasses.KNIGHT),
        damageBonus = 28,
        critChanceBonus = 0.10,
        description = "Was used by fisherman previously.",
        level = 25
    )
    val DRAGOON_SPEAR = EquipmentItem(
        id = "spear_03",
        name = "Dragoon Spear",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.SPEAR,
        allowedClasses = listOf(PlayerClasses.KNIGHT),
        damageBonus = 65,
        critChanceBonus = 0.20,
        attackSpeedBonus = -0.10,
        description = "Legendary spear of the dragon knights.",
        level = 80
    )
    val HOLY_LANCE = EquipmentItem(
        id = "spear_04",
        name = "Holy Lance",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.SPEAR,
        allowedClasses = listOf(PlayerClasses.KNIGHT, PlayerClasses.PRIEST),
        damageBonus = 45,
        mageDamageBonus = 20,
        willBonus = 0.15,
        description = "Blessed by the highest order of priests.",
        level = 65
    )

    // ===== ARCHER =====
    // Bows
    val SHORT_BOW = EquipmentItem(
        id = "bow_01",
        name = "Short bow",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.BOW,
        allowedClasses = listOf(PlayerClasses.ADVENTURIST, PlayerClasses.ARCHER),
        damageBonus = 8,
        attackSpeedBonus = 0.10,
        description = "Good for beginners. Not so good for everything else.",
        level = 1
    )
    val LONG_BOW = EquipmentItem(
        id = "bow_02",
        name = "Long bow",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.BOW,
        allowedClasses = listOf(PlayerClasses.ARCHER),
        damageBonus = 15,
        attackSpeedBonus = -0.05,
        accuracyBonus = 0.10,
        description = "Slow, but precise and powerful.",
        level = 25
    )
    val ETERNAL_GALE = EquipmentItem(
        id = "bow_03",
        name = "Eternal Gale",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.BOW,
        allowedClasses = listOf(PlayerClasses.ARCHER),
        damageBonus = 25,
        attackSpeedBonus = 0.30,
        critChanceBonus = 0.15,
        description = "The wind itself guides these arrows.",
        level = 65
    )
    val RECURVE_BOW = EquipmentItem(
        id = "bow_04",
        name = "Recurve Bow",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.BOW,
        allowedClasses = listOf(PlayerClasses.ARCHER),
        damageBonus = 42,
        attackSpeedBonus = 0.15,
        accuracyBonus = 0.15,
        description = "Perfect balance of power and speed.",
        level = 50
    )
    val SOULSTRING_BOW = EquipmentItem(
        id = "bow_05",
        name = "Soulstring Bow",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.BOW,
        allowedClasses = listOf(PlayerClasses.ARCHER),
        damageBonus = 80,
        attackSpeedBonus = 0.25,
        critChanceBonus = 0.25,
        healthBonus = -50,
        description = "Uses your life force to empower arrows.",
        level = 110
    )

    // ===== ASSASSIN =====
    // Knifes
    val TOY_KNIFE = EquipmentItem(
        id = "knife_01",
        name = "Toy knife",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.KNIFE,
        allowedClasses = listOf(PlayerClasses.ASSASSIN),
        damageBonus = 3,
        attackSpeedBonus = 0.20,
        description = "Attacks do not cause harm.",
        level = 1
    )
    val BUTCHER_KNIFE = EquipmentItem(
        id = "knife_02",
        name = "Butcher's knife",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.KNIFE,
        allowedClasses = listOf(PlayerClasses.ASSASSIN),
        damageBonus = 45,
        critChanceBonus = 0.20,
        attackSpeedBonus = -0.05,
        description = "GRAAAH FRESH MEAT!",
        level = 40
    )
    val BLOODY_MACHETE = EquipmentItem(
        id = "knife_03",
        name = "Bloody machete",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.KNIFE,
        allowedClasses = listOf(PlayerClasses.ASSASSIN),
        damageBonus = 55,
        critChanceBonus = 0.15,
        attackSpeedBonus = 0.10,
        description = "Ki, ki.. ma, ma..",
        level = 50
    )
    val RITUAL_KNIFE = EquipmentItem(
        id = "knife_04",
        name = "Ritual Knife",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.KNIFE,
        allowedClasses = listOf(PlayerClasses.ASSASSIN),
        damageBonus = 70,
        critChanceBonus = 0.30,
        mageDamageBonus = 15,
        description = "Used in dark ceremonies. Still wet with... something.",
        level = 85
    )

    // Daggers
    val RUSTY_DAGGER = EquipmentItem(
        id = "dagger_01",
        name = "Rusty dagger",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.DAGGER,
        allowedClasses = listOf(PlayerClasses.ASSASSIN),
        damageBonus = 5,
        attackSpeedBonus = 0.25,
        description = "Attacks do not cause tetanus.",
        level = 1
    )
    val CENTRAL_DIRK = EquipmentItem(
        id = "dagger_02",
        name = "Central dirk",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.DAGGER,
        allowedClasses = listOf(PlayerClasses.ASSASSIN),
        damageBonus = 22,
        attackSpeedBonus = 0.20,
        accuracyBonus = 0.05,
        description = "No, it's not \"central d#ck\"",
        level = 20
    )
    val CHAMPION_DAGGER = EquipmentItem(
        id = "dagger_03",
        name = "Champion's dagger",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.DAGGER,
        allowedClasses = listOf(PlayerClasses.ASSASSIN),
        damageBonus = 42,
        attackSpeedBonus = 0.15,
        critChanceBonus = 0.10,
        description = "No, it's not rip-off, I swear!",
        level = 40
    )
    val SHADOWFANG = EquipmentItem(
        id = "dagger_04",
        name = "Shadowfang",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.DAGGER,
        allowedClasses = listOf(PlayerClasses.ASSASSIN),
        damageBonus = 68,
        attackSpeedBonus = 0.35,
        critChanceBonus = 0.25,
        description = "Strikes from the shadows are always fatal.",
        level = 75
    )

    // ===== MAGE / PRIEST / SHAMAN =====
    // Staffs
    val WOODEN_STAFF = EquipmentItem(
        id = "staff_01",
        name = "Wooden staff",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.STAFF,
        allowedClasses = listOf(PlayerClasses.MAGE, PlayerClasses.PRIEST, PlayerClasses.SHAMAN),
        mageDamageBonus = 5,
        manaBonus = 15,
        description = "Very basic staff for newbie mages.",
        level = 1
    )
    val CRYSTAL_STAFF = EquipmentItem(
        id = "staff_02",
        name = "Crystal staff",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.STAFF,
        allowedClasses = listOf(PlayerClasses.MAGE, PlayerClasses.PRIEST),
        mageDamageBonus = 20,
        manaBonus = 50,
        description = "Crystals makes this staff cast stable and frequent",
        level = 10
    )
    val SOUL_EATER_STAFF = EquipmentItem(
        id = "staff_03",
        name = "Souleater staff",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.STAFF,
        allowedClasses = listOf(PlayerClasses.MAGE, PlayerClasses.SHAMAN),
        mageDamageBonus = 70,
        manaBonus = 120,
        willBonus = 0.25,
        description = "It whispers to you. It craves for enemy's souls.",
        level = 75
    )
    val SERPENT_STAFF = EquipmentItem(
        id = "staff_04",
        name = "Serpent Staff",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.STAFF,
        allowedClasses = listOf(PlayerClasses.MAGE, PlayerClasses.SHAMAN),
        mageDamageBonus = 55,
        manaBonus = 90,
        luckBonus = 0.10,
        description = "Coiled with the power of an ancient serpent.",
        level = 60
    )

    // Orbs
    val GLASS_ORB = EquipmentItem(
        id = "orb_01",
        name = "Glass orb",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.ORB,
        allowedClasses = listOf(PlayerClasses.MAGE, PlayerClasses.PRIEST),
        luckBonus = 0.01,
        mageDamageBonus = 5,
        manaBonus = 10,
        description = "Careful not to break it!",
        level = 5
    )
    val APPRENTICE_ORB = EquipmentItem(
        id = "orb_02",
        name = "Apprentice orb",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.ORB,
        allowedClasses = listOf(PlayerClasses.MAGE, PlayerClasses.PRIEST),
        luckBonus = 0.03,
        mageDamageBonus = 15,
        manaBonus = 20,
        description = "A simple orb for practicing magic.",
        level = 10
    )
    val MAGE_ORB = EquipmentItem(
        id = "orb_03",
        name = "Mage orb",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.ORB,
        allowedClasses = listOf(PlayerClasses.MAGE, PlayerClasses.PRIEST),
        luckBonus = 0.05,
        mageDamageBonus = 25,
        manaBonus = 30,
        description = "This orb radiates powerful magic.",
        level = 15
    )
    val MASTER_ORB = EquipmentItem(
        id = "orb_04",
        name = "Master orb",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.ORB,
        allowedClasses = listOf(PlayerClasses.MAGE, PlayerClasses.PRIEST),
        luckBonus = 0.07,
        mageDamageBonus = 30,
        manaBonus = 30,
        description = "You feel a surge of power, holding that orb.",
        level = 25
    )
    val ARCHMAGE_ORB = EquipmentItem(
        id = "orb_05",
        name = "Archmage orb",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.ORB,
        allowedClasses = listOf(PlayerClasses.MAGE, PlayerClasses.PRIEST),
        luckBonus = 0.10,
        mageDamageBonus = 50,
        manaBonus = 75,
        description = "t m opera o mambo mambo",
        level = 50
    )

    // Books
    val MOLDY_BOOK = EquipmentItem(
        id = "book_01",
        name = "Moldy book",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.BOOK,
        allowedClasses = listOf(PlayerClasses.MAGE, PlayerClasses.SHAMAN),
        mageDamageBonus = 5,
        description = "Old dusty book you found from your attic",
        level = 1
    )
    val SLIMY_TOME = EquipmentItem(
        id = "book_02",
        name = "Slimy tome",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.BOOK,
        allowedClasses = listOf(PlayerClasses.MAGE, PlayerClasses.SHAMAN),
        mageDamageBonus = 12,
        manaBonus = 25,
        description = "Book dropped from a slime. It's sticky...",
        level = 10
    )
    val NECRONOMICON = EquipmentItem(
        id = "book_03",
        name = "Necronomicon",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.BOOK,
        allowedClasses = listOf(PlayerClasses.SHAMAN),
        mageDamageBonus = 35,
        willBonus = 0.20,
        healthBonus = -30,
        description = "A dark aura radiates from this book..",
        level = 20
    )
    val GLACIAL_SCRIPTURES = EquipmentItem(
        id = "book_04",
        name = "Glacial scriptures",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.BOOK,
        allowedClasses = listOf(PlayerClasses.MAGE, PlayerClasses.SHAMAN),
        mageDamageBonus = 45,
        manaBonus = 60,
        defenseBonus = 0.05,
        description = "You feel cold chill running down your spine while holding it..",
        level = 35
    )
    val HELLFIRE_GLYPHS = EquipmentItem(
        id = "book_05",
        name = "Hellfire glyphs",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.BOOK,
        allowedClasses = listOf(PlayerClasses.MAGE, PlayerClasses.SHAMAN),
        mageDamageBonus = 65,
        critChanceBonus = 0.15,
        description = "You smell something fried..",
        level = 45
    )
    val VOID_GUIDE = EquipmentItem(
        id = "book_06",
        name = "Void guide",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.BOOK,
        allowedClasses = listOf(PlayerClasses.SHAMAN),
        mageDamageBonus = 85,
        willBonus = 0.30,
        manaBonus = 100,
        description = "Imaginary technique: Hollow Purple.",
        level = 60
    )
    val MAGNUM_OPUS = EquipmentItem(
        id = "book_07",
        name = "Magnum opus",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.BOOK,
        allowedClasses = listOf(PlayerClasses.MAGE, PlayerClasses.SHAMAN),
        mageDamageBonus = 120,
        manaBonus = 200,
        willBonus = 0.35,
        luckBonus = 0.15,
        description = "The ultimate tome of magic.",
        level = 80
    )

    // ===== SWORDMASTER =====
    // Katanas
    val IRON_KATANA = EquipmentItem(
        id = "katana_01",
        name = "Iron Katana",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.KATANA,
        allowedClasses = listOf(PlayerClasses.SWORDMASTER),
        damageBonus = 18,
        attackSpeedBonus = 0.15,
        critChanceBonus = 0.05,
        description = "A swift blade forged in the eastern lands.",
        level = 15
    )
    val SHADOW_KATANA = EquipmentItem(
        id = "katana_02",
        name = "Shadow Katana",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.KATANA,
        allowedClasses = listOf(PlayerClasses.SWORDMASTER),
        damageBonus = 35,
        attackSpeedBonus = 0.25,
        critChanceBonus = 0.15,
        description = "Its edge is so sharp it cuts the very shadows.",
        level = 45
    )
    val MURAMASA = EquipmentItem(
        id = "katana_03",
        name = "Muramasa",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.KATANA,
        allowedClasses = listOf(PlayerClasses.SWORDMASTER),
        damageBonus = 65,
        attackSpeedBonus = 0.35,
        critChanceBonus = 0.25,
        healthBonus = -100,
        description = "A cursed blade that thirsts for the blood of its wielder.",
        level = 85
    )
    val YAMI_NO_KEN = EquipmentItem(
        id = "katana_04",
        name = "Yami no Ken",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.KATANA,
        allowedClasses = listOf(PlayerClasses.SWORDMASTER),
        damageBonus = 120,
        attackSpeedBonus = 0.40,
        critChanceBonus = 0.30,
        accuracyBonus = 0.20,
        description = "The legendary Sword of Darkness.",
        level = 150
    )

    // Dual Swords
    val TWIN_FALCHIONS = EquipmentItem(
        id = "dual_01",
        name = "Twin Falchions",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.DUAL_SWORDS,
        allowedClasses = listOf(PlayerClasses.SWORDMASTER),
        damageBonus = 15,
        attackSpeedBonus = 0.30,
        defenseBonus = -0.10,
        description = "Two swords are better than one, but you sacrifice defense.",
        level = 20
    )
    val BLADE_DANCERS = EquipmentItem(
        id = "dual_02",
        name = "Blade Dancers",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.DUAL_SWORDS,
        allowedClasses = listOf(PlayerClasses.SWORDMASTER),
        damageBonus = 30,
        attackSpeedBonus = 0.45,
        critChanceBonus = 0.10,
        description = "Light as a feather, deadly as a viper.",
        level = 50
    )
    val WINDS_OF_CHANGE = EquipmentItem(
        id = "dual_03",
        name = "Winds of Change",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.DUAL_SWORDS,
        allowedClasses = listOf(PlayerClasses.SWORDMASTER),
        damageBonus = 55,
        attackSpeedBonus = 0.50,
        critChanceBonus = 0.15,
        accuracyBonus = 0.10,
        description = "You can barely see the swings, only the aftermath.",
        level = 90
    )
    val SOUL_RENDER = EquipmentItem(
        id = "dual_04",
        name = "Soul Render",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.DUAL_SWORDS,
        allowedClasses = listOf(PlayerClasses.SWORDMASTER),
        damageBonus = 90,
        attackSpeedBonus = 0.60,
        critChanceBonus = 0.25,
        willBonus = 0.20,
        description = "Cuts not only flesh, but the very soul of the enemy.",
        level = 140
    )

    // ===== MONARCH =====
    // Gloves
    val LEATHER_GLOVES = EquipmentItem(
        id = "gloves_01",
        name = "Leather Gloves",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.GLOVES,
        allowedClasses = listOf(PlayerClasses.MONARCH),
        damageBonus = 5,
        attackSpeedBonus = 0.10,
        description = "Just regular leather gloves.",
        level = 1
    )
    val SPIKED_GAUNTLETS = EquipmentItem(
        id = "gloves_02",
        name = "Spiked Gauntlets",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.GLOVES,
        allowedClasses = listOf(PlayerClasses.MONARCH),
        damageBonus = 22,
        critChanceBonus = 0.10,
        description = "Brass knuckles? No, steel knuckles.",
        level = 25
    )
    val ROYAL_TOUCH = EquipmentItem(
        id = "gloves_03",
        name = "Royal Touch",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.GLOVES,
        allowedClasses = listOf(PlayerClasses.MONARCH),
        damageBonus = 45,
        willBonus = 0.25,
        luckBonus = 0.15,
        description = "Gloves infused with the authority of a true ruler.",
        level = 60
    )
    val FIST_OF_THE_TITAN = EquipmentItem(
        id = "gloves_04",
        name = "Fist of the Titan",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.GLOVES,
        allowedClasses = listOf(PlayerClasses.MONARCH),
        damageBonus = 85,
        attackSpeedBonus = -0.20,
        critChanceBonus = 0.40,
        healthBonus = 200,
        description = "One punch is all it takes.",
        level = 110
    )

    // ===== JOKER =====
    // Cards
    val TORN_CARDS = EquipmentItem(
        id = "cards_01",
        name = "Torn Cards",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.CARDS,
        allowedClasses = listOf(PlayerClasses.JOKER),
        mageDamageBonus = 8,
        luckBonus = 0.05,
        description = "A deck of old, torn playing cards.",
        level = 1
    )
    val TRICKSTER_DECK = EquipmentItem(
        id = "cards_02",
        name = "Trickster's Deck",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.CARDS,
        allowedClasses = listOf(PlayerClasses.JOKER),
        mageDamageBonus = 28,
        luckBonus = 0.15,
        critChanceBonus = 0.20,
        description = "Is this your card? No? Too bad, you're dead.",
        level = 35
    )
    val DEATH_TAROT = EquipmentItem(
        id = "cards_03",
        name = "Death Tarot",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.CARDS,
        allowedClasses = listOf(PlayerClasses.JOKER),
        mageDamageBonus = 60,
        luckBonus = 0.30,
        critChanceBonus = 0.35,
        willBonus = 0.15,
        description = "The thirteenth card. It seals your fate.",
        level = 80
    )
    val JOKERS_WILD = EquipmentItem(
        id = "cards_04",
        name = "Joker's Wild",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.CARDS,
        allowedClasses = listOf(PlayerClasses.JOKER),
        mageDamageBonus = 110,
        luckBonus = 0.50,
        critChanceBonus = 0.50,
        description = "The ultimate deck. Expect the unexpected.",
        level = 130
    )

    // ===== ALCHEMIST =====
    // Flasks
    val TEST_TUBE = EquipmentItem(
        id = "flask_01",
        name = "Test Tube",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.FLASK,
        allowedClasses = listOf(PlayerClasses.ALCHEMIST),
        mageDamageBonus = 4,
        description = "Contains a mysterious bubbling liquid.",
        level = 1
    )
    val ACID_FLASK = EquipmentItem(
        id = "flask_02",
        name = "Acid Flask",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.FLASK,
        allowedClasses = listOf(PlayerClasses.ALCHEMIST),
        mageDamageBonus = 18,
        defenseBonus = -0.05,
        description = "Melts armor and faces alike.",
        level = 20
    )
    val PHILOSOPHER_STONE_FLASK = EquipmentItem(
        id = "flask_03",
        name = "Philosopher's Flask",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.FLASK,
        allowedClasses = listOf(PlayerClasses.ALCHEMIST),
        mageDamageBonus = 42,
        manaBonus = 75,
        willBonus = 0.20,
        description = "Turns mana into pure destructive energy.",
        level = 55
    )
    val ELIXIR_OF_DOOM = EquipmentItem(
        id = "flask_04",
        name = "Elixir of Doom",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.FLASK,
        allowedClasses = listOf(PlayerClasses.ALCHEMIST),
        mageDamageBonus = 95,
        critChanceBonus = 0.20,
        description = "The final concoction. Drink it? Throw it!",
        level = 105
    )

    // ===== ARMOR SETS =====
    // General armor
    val LEATHER_BOOTS = EquipmentItem(
        id = "boots_01",
        name = "Leather boots",
        type = EquipmentType.BOOTS,
        allowedClasses = listOf(PlayerClasses.ADVENTURIST, PlayerClasses.KNIGHT, PlayerClasses.MAGE, PlayerClasses.ASSASSIN, PlayerClasses.ARCHER, PlayerClasses.PRIEST, PlayerClasses.JOKER, PlayerClasses.BERSERK, PlayerClasses.SHAMAN, PlayerClasses.MONARCH, PlayerClasses.SWORDMASTER, PlayerClasses.ALCHEMIST),
        defenseBonus = 0.01,
        description = "You don't feel protected wearing these.",
        level = 1
    )
    val LEATHER_CHESTPLATE = EquipmentItem(
        id = "chestplate_01",
        name = "Leather chestplate",
        type = EquipmentType.CHESTPLATE,
        allowedClasses = listOf(PlayerClasses.ADVENTURIST, PlayerClasses.KNIGHT, PlayerClasses.MAGE, PlayerClasses.ASSASSIN, PlayerClasses.ARCHER, PlayerClasses.PRIEST, PlayerClasses.JOKER, PlayerClasses.BERSERK, PlayerClasses.SHAMAN, PlayerClasses.MONARCH, PlayerClasses.SWORDMASTER, PlayerClasses.ALCHEMIST),
        defenseBonus = 0.01,
        description = "You don't feel protected wearing these.",
        level = 1
    )
    val LEATHER_HELMET = EquipmentItem(
        id = "helmet_01",
        name = "Leather helmet",
        type = EquipmentType.HELMET,
        allowedClasses = listOf(PlayerClasses.ADVENTURIST, PlayerClasses.KNIGHT, PlayerClasses.MAGE, PlayerClasses.ASSASSIN, PlayerClasses.ARCHER, PlayerClasses.PRIEST, PlayerClasses.JOKER, PlayerClasses.BERSERK, PlayerClasses.SHAMAN, PlayerClasses.MONARCH, PlayerClasses.SWORDMASTER, PlayerClasses.ALCHEMIST),
        defenseBonus = 0.01,
        description = "You don't feel protected wearing these.",
        level = 1
    )

    val IRON_HELMET = EquipmentItem(
        id = "helmet_02",
        name = "Iron helmet",
        type = EquipmentType.HELMET,
        allowedClasses = listOf(PlayerClasses.ADVENTURIST, PlayerClasses.KNIGHT, PlayerClasses.ASSASSIN, PlayerClasses.ARCHER, PlayerClasses.BERSERK, PlayerClasses.SWORDMASTER),
        defenseBonus = 0.10,
        accuracyBonus = -0.03,
        description = "A sturdy armor, able to maintain user's survivability.",
        level = 20
    )
    val IRON_CHESTPLATE = EquipmentItem(
        id = "chestplate_02",
        name = "Iron chestplate",
        type = EquipmentType.CHESTPLATE,
        allowedClasses = listOf(PlayerClasses.ADVENTURIST, PlayerClasses.KNIGHT, PlayerClasses.ASSASSIN, PlayerClasses.ARCHER, PlayerClasses.BERSERK, PlayerClasses.SWORDMASTER),
        defenseBonus = 0.10,
        attackSpeedBonus = -0.05,
        description = "A sturdy armor, able to maintain user's survivability.",
        level = 20
    )
    val IRON_BOOTS = EquipmentItem(
        id = "boots_02",
        name = "Iron boots",
        type = EquipmentType.BOOTS,
        allowedClasses = listOf(PlayerClasses.ADVENTURIST, PlayerClasses.KNIGHT, PlayerClasses.ASSASSIN, PlayerClasses.ARCHER, PlayerClasses.BERSERK, PlayerClasses.SWORDMASTER),
        defenseBonus = 0.10,
        attackSpeedBonus = -0.05,
        description = "A sturdy armor, able to maintain user's survivability.",
        level = 20
    )

    // Knight set
    val KNIGHT_HELMET = EquipmentItem(
        id = "helmet_03",
        name = "Knight's helmet",
        type = EquipmentType.HELMET,
        allowedClasses = listOf(PlayerClasses.KNIGHT),
        defenseBonus = 0.25,
        accuracyBonus = -0.05,
        attackSpeedBonus = -0.15,
        description = "A piece of armor, used by true knights.",
        level = 45
    )
    val KNIGHT_CHESTPLATE = EquipmentItem(
        id = "chestplate_03",
        name = "Knight's chestplate",
        type = EquipmentType.CHESTPLATE,
        allowedClasses = listOf(PlayerClasses.KNIGHT),
        defenseBonus = 0.25,
        accuracyBonus = -0.05,
        attackSpeedBonus = -0.15,
        description = "A piece of armor, user by true knights.",
        level = 45
    )
    val KNIGHT_BOOTS = EquipmentItem(
        id = "boots_03",
        name = "Knight's boots",
        type = EquipmentType.BOOTS,
        allowedClasses = listOf(PlayerClasses.KNIGHT),
        defenseBonus = 0.25,
        accuracyBonus = -0.05,
        attackSpeedBonus = -0.15,
        description = "A piece of armor, user by true knights.",
        level = 45
    )

    // Archer set
    val ARCHER_HELMET = EquipmentItem(
        id = "helmet_04",
        name = "Archer's hood",
        type = EquipmentType.HELMET,
        allowedClasses = listOf(PlayerClasses.ARCHER),
        accuracyBonus = 0.10,
        defenseBonus = -0.05,
        description = "A light hood that doesn't obstruct vision.",
        level = 45
    )
    val ARCHER_CHESTPLATE = EquipmentItem(
        id = "chestplate_04",
        name = "Archer's coat",
        type = EquipmentType.CHESTPLATE,
        allowedClasses = listOf(PlayerClasses.ARCHER),
        accuracyBonus = 0.05,
        defenseBonus = -0.10,
        attackSpeedBonus = 0.05,
        description = "A flexible coat for maximum mobility.",
        level = 45
    )
    val ARCHER_BOOTS = EquipmentItem(
        id = "boots_04",
        name = "Archer's boots",
        type = EquipmentType.BOOTS,
        allowedClasses = listOf(PlayerClasses.ARCHER),
        defenseBonus = -0.05,
        attackSpeedBonus = 0.15,
        description = "Soft boots for silent movement.",
        level = 45
    )

    // Mage armor
    val ROBE_OF_APPRENTICE = EquipmentItem(
        id = "chestplate_05",
        name = "Robe of Apprentice",
        type = EquipmentType.CHESTPLATE,
        allowedClasses = listOf(PlayerClasses.MAGE, PlayerClasses.PRIEST, PlayerClasses.SHAMAN),
        defenseBonus = 0.02,
        manaBonus = 25,
        mageDamageBonus = 5,
        description = "A simple robe for learning magic.",
        level = 5
    )
    val ARCANE_VESTMENTS = EquipmentItem(
        id = "chestplate_06",
        name = "Arcane Vestments",
        type = EquipmentType.CHESTPLATE,
        allowedClasses = listOf(PlayerClasses.MAGE, PlayerClasses.PRIEST, PlayerClasses.SHAMAN),
        defenseBonus = 0.05,
        manaBonus = 75,
        mageDamageBonus = 15,
        willBonus = 0.10,
        description = "Woven with threads of pure mana.",
        level = 30
    )
    val ROBE_OF_THE_VOID = EquipmentItem(
        id = "chestplate_07",
        name = "Robe of the Void",
        type = EquipmentType.CHESTPLATE,
        allowedClasses = listOf(PlayerClasses.MAGE, PlayerClasses.SHAMAN),
        defenseBonus = 0.10,
        manaBonus = 150,
        mageDamageBonus = 35,
        willBonus = 0.20,
        description = "The void gazes also into you.",
        level = 70
    )
    val MAGE_HOOD = EquipmentItem(
        id = "helmet_06",
        name = "Mage's Hood",
        type = EquipmentType.HELMET,
        allowedClasses = listOf(PlayerClasses.MAGE, PlayerClasses.PRIEST, PlayerClasses.SHAMAN),
        defenseBonus = 0.03,
        manaBonus = 40,
        willBonus = 0.05,
        description = "A hood that helps focus magical energies.",
        level = 20
    )
    val MAGE_BOOTS = EquipmentItem(
        id = "boots_06",
        name = "Mage's Boots",
        type = EquipmentType.BOOTS,
        allowedClasses = listOf(PlayerClasses.MAGE, PlayerClasses.PRIEST, PlayerClasses.SHAMAN),
        defenseBonus = 0.02,
        manaBonus = 20,
        attackSpeedBonus = 0.05,
        description = "Comfortable boots for long study sessions.",
        level = 15
    )

    // Assassin armor
    val ASSASSIN_HOOD = EquipmentItem(
        id = "helmet_05",
        name = "Assassin's Hood",
        type = EquipmentType.HELMET,
        allowedClasses = listOf(PlayerClasses.ASSASSIN),
        defenseBonus = 0.05,
        critChanceBonus = 0.10,
        accuracyBonus = 0.05,
        description = "Silence is golden.",
        level = 30
    )
    val ASSASSIN_GARB = EquipmentItem(
        id = "chestplate_08",
        name = "Assassin's Garb",
        type = EquipmentType.CHESTPLATE,
        allowedClasses = listOf(PlayerClasses.ASSASSIN),
        defenseBonus = 0.10,
        critChanceBonus = 0.15,
        attackSpeedBonus = 0.10,
        description = "Light as a shadow, dark as night.",
        level = 35
    )
    val ASSASSIN_BOOTS = EquipmentItem(
        id = "boots_05",
        name = "Assassin's Boots",
        type = EquipmentType.BOOTS,
        allowedClasses = listOf(PlayerClasses.ASSASSIN),
        defenseBonus = 0.05,
        attackSpeedBonus = 0.15,
        critChanceBonus = 0.05,
        description = "Makes no sound on any surface.",
        level = 30
    )

    // Shields
    val WOODEN_SHIELD = EquipmentItem(
        id = "shield_01",
        name = "Wooden shield",
        type = EquipmentType.SHIELD,
        allowedClasses = listOf(PlayerClasses.ADVENTURIST, PlayerClasses.KNIGHT, PlayerClasses.MAGE, PlayerClasses.ASSASSIN, PlayerClasses.ARCHER, PlayerClasses.PRIEST, PlayerClasses.JOKER, PlayerClasses.BERSERK, PlayerClasses.SHAMAN, PlayerClasses.MONARCH, PlayerClasses.SWORDMASTER, PlayerClasses.ALCHEMIST),
        defenseBonus = 0.05,
        description = "Shield that barely defends you.",
        level = 1
    )
    val PALADIN_SHIELD = EquipmentItem(
        id = "shield_02",
        name = "Paladin's shield",
        type = EquipmentType.SHIELD,
        allowedClasses = listOf(PlayerClasses.KNIGHT),
        defenseBonus = 0.20,
        attackSpeedBonus = -0.10,
        willBonus = 0.10,
        healthBonus = 50,
        description = "A shield, that only worthy could wield.",
        level = 40
    )
    val PATH_DEFENDER = EquipmentItem(
        id = "shield_03",
        name = "The Path's Defender",
        type = EquipmentType.SHIELD,
        allowedClasses = listOf(PlayerClasses.KNIGHT),
        defenseBonus = 0.50,
        healthBonus = 100,
        willBonus = 0.15,
        description = "The ultimate shield. Nothing gets past it.",
        level = 75
    )
    val BUCKLER = EquipmentItem(
        id = "shield_04",
        name = "Buckler",
        type = EquipmentType.SHIELD,
        allowedClasses = listOf(PlayerClasses.ADVENTURIST, PlayerClasses.ASSASSIN, PlayerClasses.SWORDMASTER),
        defenseBonus = 0.10,
        attackSpeedBonus = 0.05,
        description = "Small but effective for parrying.",
        level = 15
    )
    val TOWER_SHIELD = EquipmentItem(
        id = "shield_05",
        name = "Tower Shield",
        type = EquipmentType.SHIELD,
        allowedClasses = listOf(PlayerClasses.KNIGHT),
        defenseBonus = 0.35,
        attackSpeedBonus = -0.25,
        accuracyBonus = -0.10,
        healthBonus = 150,
        description = "You are a walking fortress.",
        level = 60
    )

    // Runes
    val DEFENCE_RUNE = EquipmentItem(
        id = "rune_01",
        name = "Defence rune",
        type = EquipmentType.RUNE,
        allowedClasses = emptyList(),
        defenseBonus = 0.20,
        healthBonus = -50,
        description = "This rune's magic shields you in exchange for your vitality.",
        level = 20
    )
    val RUNE_OF_POWER = EquipmentItem(
        id = "rune_02",
        name = "Rune of Power",
        type = EquipmentType.RUNE,
        allowedClasses = emptyList(),
        damageBonus = 15,
        mageDamageBonus = 15,
        defenseBonus = -0.10,
        description = "Increases power at the cost of defense.",
        level = 30
    )
    val RUNE_OF_VITALITY = EquipmentItem(
        id = "rune_03",
        name = "Rune of Vitality",
        type = EquipmentType.RUNE,
        allowedClasses = emptyList(),
        healthBonus = 150,
        manaBonus = 50,
        description = "Strengthens both body and mind.",
        level = 40
    )
    val RUNE_OF_HASTE = EquipmentItem(
        id = "rune_04",
        name = "Rune of Haste",
        type = EquipmentType.RUNE,
        allowedClasses = emptyList(),
        attackSpeedBonus = 0.25,
        accuracyBonus = 0.10,
        defenseBonus = -0.15,
        description = "Speed is key. Sacrifice defense for unmatched agility.",
        level = 50
    )
    val RUNE_OF_CRITICAL = EquipmentItem(
        id = "rune_05",
        name = "Rune of Critical",
        type = EquipmentType.RUNE,
        allowedClasses = emptyList(),
        critChanceBonus = 0.30,
        luckBonus = 0.15,
        healthBonus = -75,
        description = "Risk it all for the big numbers.",
        level = 60
    )
    val RUNE_OF_WILL = EquipmentItem(
        id = "rune_06",
        name = "Rune of Will",
        type = EquipmentType.RUNE,
        allowedClasses = emptyList(),
        willBonus = 0.25,
        manaBonus = 100,
        description = "Strengthens your mental fortitude.",
        level = 45
    )

    // СПИСОК ВСЕХ ОРУЖИЙ (ВСЕХ КЛАССОВ)
    val AllWeapons = listOf(
        // Swords
        WOODEN_SWORD, CHAMPION_SWORD, ANGEL_BOJII, HERO_SWORD,
        // Greatswords
        DEEPSEA_ANCHOR, DARKSTEEL_GREATSWORD, RAILBLADE, COLOSSUS_BLADE,
        // Axes
        LUMBER_AXE, BERSERKER_CHOPPA, GRAN_SUDARUSKA, BLOOD_MOON_AXE, FROSTBITE_AXE,
        // Spears
        IRON_SPEAR, TRIDENT_SPEAR, DRAGOON_SPEAR, HOLY_LANCE,
        // Bows
        SHORT_BOW, LONG_BOW, ETERNAL_GALE, RECURVE_BOW, SOULSTRING_BOW,
        // Knifes
        TOY_KNIFE, BUTCHER_KNIFE, BLOODY_MACHETE, RITUAL_KNIFE,
        // Daggers
        RUSTY_DAGGER, CENTRAL_DIRK, CHAMPION_DAGGER, SHADOWFANG,
        // Staffs
        WOODEN_STAFF, CRYSTAL_STAFF, SOUL_EATER_STAFF, SERPENT_STAFF,
        // Orbs
        GLASS_ORB, APPRENTICE_ORB, MAGE_ORB, MASTER_ORB, ARCHMAGE_ORB,
        // Books
        MOLDY_BOOK, SLIMY_TOME, NECRONOMICON, GLACIAL_SCRIPTURES, HELLFIRE_GLYPHS, VOID_GUIDE, MAGNUM_OPUS,
        // Swordmaster - Katanas
        IRON_KATANA, SHADOW_KATANA, MURAMASA, YAMI_NO_KEN,
        // Swordmaster - Dual Swords
        TWIN_FALCHIONS, BLADE_DANCERS, WINDS_OF_CHANGE, SOUL_RENDER,
        // Monarch - Gloves
        LEATHER_GLOVES, SPIKED_GAUNTLETS, ROYAL_TOUCH, FIST_OF_THE_TITAN,
        // Joker - Cards
        TORN_CARDS, TRICKSTER_DECK, DEATH_TAROT, JOKERS_WILD,
        // Alchemist - Flasks
        TEST_TUBE, ACID_FLASK, PHILOSOPHER_STONE_FLASK, ELIXIR_OF_DOOM
    )

    // СПИСОК ВСЕХ БРОНЬ (ВСЕХ КЛАССОВ)
    val AllArmor = listOf(
        // Leather set
        LEATHER_BOOTS, LEATHER_CHESTPLATE, LEATHER_HELMET,
        // Iron set
        IRON_HELMET, IRON_CHESTPLATE, IRON_BOOTS,
        // Knight set
        KNIGHT_HELMET, KNIGHT_CHESTPLATE, KNIGHT_BOOTS,
        // Archer set
        ARCHER_HELMET, ARCHER_CHESTPLATE, ARCHER_BOOTS,
        // Mage set
        ROBE_OF_APPRENTICE, ARCANE_VESTMENTS, ROBE_OF_THE_VOID, MAGE_HOOD, MAGE_BOOTS,
        // Assassin set
        ASSASSIN_HOOD, ASSASSIN_GARB, ASSASSIN_BOOTS
    )

    // СПИСОК ВСЕХ ЩИТОВ (ВСЕХ КЛАССОВ)
    val AllShields = listOf(
        WOODEN_SHIELD,
        PALADIN_SHIELD,
        PATH_DEFENDER,
        BUCKLER,
        TOWER_SHIELD
    )

    // СПИСОК ВСЕХ РУН
    val AllRunes = listOf(
        DEFENCE_RUNE,
        RUNE_OF_POWER,
        RUNE_OF_VITALITY,
        RUNE_OF_HASTE,
        RUNE_OF_CRITICAL,
        RUNE_OF_WILL
    )

    // СПИСОК ВООБЩЕ ВСЕГО (ОРУЖИЯ, БРОНЯ, ЩИТЫ, и т.д.)
    val AllItems = AllWeapons + AllArmor + AllShields + AllRunes
}
