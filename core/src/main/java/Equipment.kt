package ru.triplethall.rpgturnbased

import kotlin.collections.joinToString

enum class EquipmentType
{
    HELMET,
    CHESTPLATE,
    BOOTS,
    SHIELD,
    WEAPON,
    RUNE
}

enum class WeaponType
{
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

    val description: String,                // Описание
    val level: Int = 1                      // Требуемый уровень
)
{
    fun canBeUsedBy(playerClass: PlayerClasses): Boolean =
        playerClass in allowedClasses || allowedClasses.isEmpty()

    fun getStatsDescription(): String {
        val bonuses = mutableListOf<String>()
        if (damageBonus != 0)
        {
            val sign = if (damageBonus > 0) "+" else ""
            bonuses.add("Урон: $sign$damageBonus")
        }
        if (mageDamageBonus != 0)
        {
            val sign = if (mageDamageBonus > 0) "+" else ""
            bonuses.add("Маг. урон: $sign$mageDamageBonus")
        }
        if (healthBonus != 0)
        {
            val sign = if (healthBonus > 0) "+" else ""
            bonuses.add("Здоровье: $sign$healthBonus")
        }
        if (manaBonus != 0)
        {
            val sign = if (manaBonus > 0) "+" else ""
            bonuses.add("Мана: $sign$manaBonus")
        }
        if (defenseBonus != 0.0)
        {
            val percent = (defenseBonus * 100).toInt()
            val sign = if (percent > 0) "+" else ""
            bonuses.add("Защита: $sign$percent%")
        }
        if (attackSpeedBonus != 0.0)
        {
            val percent = (attackSpeedBonus * 100).toInt()
            val sign = if (percent > 0) "+" else ""
            bonuses.add("Скорость атаки: $sign$percent%")
        }
        if (accuracyBonus != 0.0)
        {
            val percent = (accuracyBonus * 100).toInt()
            val sign = if (percent > 0) "+" else ""
            bonuses.add("Точность: $sign$percent%")
        }
        if (willBonus != 0.0)
        {
            val percent = (willBonus * 100).toInt()
            val sign = if (percent > 0) "+" else ""
            bonuses.add("Воля: $sign$percent%")
        }
        if (luckBonus != 0.0)
        {
            val percent = (luckBonus * 100).toInt()
            val sign = if (percent > 0) "+" else ""
            bonuses.add("Удача: $sign$percent%")
        }
        if (critChanceBonus != 0.0)
        {
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
class PlayerEquipment
{
    private val runes = arrayOfNulls<EquipmentItem>(2)
    fun getRune(slot: Int): EquipmentItem? = runes.getOrNull(slot)

    private val equipped = mutableMapOf<EquipmentType, EquipmentItem>()
    // получить данные о предмете по типу
    fun getEquipped(type: EquipmentType): EquipmentItem? = equipped[type]
    // получить данные о всех предметах
    fun getAllEquipped(): Map<EquipmentType, EquipmentItem> = equipped.toMap()
    // экипировка предмета
    fun equip(item: EquipmentItem, player: Player, playerClass: PlayerClasses):Boolean
    {
        if (!item.canBeUsedBy(playerClass))
        {
            return false
        }
        if (item.level > player.level)
        {
            return false
        }
        if (equipped.containsKey(item.type))
        {
            return false
        }
        equipped[item.type] = item
        applyItemBonuses(item, player, true)
        return true
    }
    fun unequip(type: EquipmentType, player: Player? = null): Boolean
    {
        val item = equipped.remove(type) ?: return false
        player?.let { applyItemBonuses(item, it, false) }
        return true
    }


    fun equipRune(rune: EquipmentItem, slot: Int, player: Player): Boolean {
        if (slot !in 0..1) return false
        if (runes[slot] != null) return false
        runes[slot] = rune
        applyItemBonuses(rune, player, true)
        return true
    }

    fun unequipRune(slot: Int, player: Player): Boolean {
        val rune = runes[slot] ?: return false
        runes[slot] = null
        applyItemBonuses(rune, player, false)
        return true
    }


    private fun applyItemBonuses(item: EquipmentItem, player: Player, apply: Boolean)
    {
        val multiplier = if (apply) 1 else -1

        player.damage += item.damageBonus * multiplier
        player.mageDamage += item.mageDamageBonus * multiplier
        player.maxHealth += item.healthBonus * multiplier
        player.maxMana += item.manaBonus * multiplier
        player.defense += item.defenseBonus * multiplier
        player.attackSpeed += item.attackSpeedBonus * multiplier
        player.accuracy += item.accuracyBonus * multiplier
        player.will += item.willBonus * multiplier
        player.luck += item.luckBonus * multiplier
        player.critChance += item.critChanceBonus * multiplier

        if (apply) {
            player.currentHealth += item.healthBonus
            player.currentMana += item.manaBonus
        }
    }
}

object EquipmentDatabase
{
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
        //bonus
        description = "Wrought from durable darksteel.",
        level = 10
    )
    val RAILBLADE = EquipmentItem(
        id = "greatsword_02",
        name = "Railblade",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.GREATSWORD,
        allowedClasses = listOf(PlayerClasses.KNIGHT),
        //bonus
        description = "This ancient blade holds the power of demonslayers.",
        level = 70
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
    // Spears
    val IRON_SPEAR = EquipmentItem(
        id = "spear_01",
        name = "Iron spear",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.SPEAR,
        allowedClasses = listOf(PlayerClasses.KNIGHT),
        //bonus
        description = "A spear, perfect for poking at a long distances.",
        level = 10
    )
    val TRIDENT_SPEAR = EquipmentItem(
        id = "spear_02",
        name = "Trident spear",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.SPEAR,
        allowedClasses = listOf(PlayerClasses.KNIGHT),
        //bonus
        description = "Was used by fisherman previously.",
        level = 25
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
    // ===== ASSASSIN =====
    // Knifes
    val TOY_KNIFE = EquipmentItem(
        id = "knife_01",
        name = "Toy knife",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.KNIFE,
        allowedClasses = listOf(PlayerClasses.ASSASSIN),
        //bonus
        description = "Attacks do not cause harm.",
        level = 1
    )
    val BUTCHER_KNIFE = EquipmentItem(
        id = "knife_02",
        name = "Butcher's knife",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.KNIFE,
        allowedClasses = listOf(PlayerClasses.ASSASSIN),
        //bonus
        description = "GRAAAH FRESH MEAT!",
        level = 40
    )
    val BLOODY_MACHETE = EquipmentItem(
        id = "knife_03",
        name = "Bloody machete",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.KNIFE,
        allowedClasses = listOf(PlayerClasses.ASSASSIN),
        //bonus
        description = "Ki, ki.. ma, ma..",
        level = 50
    )
    // Daggers
    val RUSTY_DAGGER = EquipmentItem(
        id = "dagger_01",
        name = "Rusty dagger",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.DAGGER,
        allowedClasses = listOf(PlayerClasses.ASSASSIN),
        //bonus
        description = "Attacks do not cause tetanus.",
        level = 1
    )
    val CENTRAL_DIRK = EquipmentItem(
        id = "dagger_02",
        name = "Central dirk",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.DAGGER,
        allowedClasses = listOf(PlayerClasses.ASSASSIN),
        //bonus
        description = "No, it's not \"central d#ck\"",
        level = 20
    )
    val CHAMPION_DAGGER = EquipmentItem(
        id = "dagger_03",
        name = "Champion's dagger",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.DAGGER,
        allowedClasses = listOf(PlayerClasses.ASSASSIN),
        //bonus
        description = "No, it's not rip-off, I swear!",
        level = 40
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
        description = "",
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
        description = "Old dusty book you found from your attic",
        level = 1
    )
    val SLIMY_TOME = EquipmentItem(
        id = "book_02",
        name = "Slimy tome",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.BOOK,
        allowedClasses = listOf(PlayerClasses.MAGE, PlayerClasses.SHAMAN),
        //bonus
        description = "Book dropped from a slime. It's sticky...",
        level = 10
    )
    val NECRONOMICON = EquipmentItem(
        id = "book_03",
        name = "Necronomicon",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.BOOK,
        allowedClasses = listOf(PlayerClasses.SHAMAN),
        //bonus
        description = "A dark aura radiates from this book..",
        level = 20
    )
    val GLACIAL_SCRIPTURES = EquipmentItem(
        id = "book_04",
        name = "Glacial scriptures",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.BOOK,
        allowedClasses = listOf(PlayerClasses.MAGE, PlayerClasses.SHAMAN),
        //bonus
        description = "You feel cold chill running down your spine while holding it..",
        level = 35
    )
    val HELLFIRE_GLYPHS = EquipmentItem(
        id = "book_05",
        name = "Hellfire glyphs",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.BOOK,
        allowedClasses = listOf(PlayerClasses.MAGE, PlayerClasses.SHAMAN),
        //bonus
        description = "You smell something fried..",
        level = 45
    )
    val VOID_GUIDE = EquipmentItem(
        id = "book_06",
        name = "Void guide",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.BOOK,
        allowedClasses = listOf(PlayerClasses.SHAMAN),
        //bonus
        description = "Imaginary technique: Hollow Purple.",
        level = 60
    )
    val MAGNUM_OPUS = EquipmentItem(
        id = "book_07",
        name = "Magnum opus",
        type = EquipmentType.WEAPON,
        weaponType = WeaponType.BOOK,
        allowedClasses = listOf(PlayerClasses.MAGE, PlayerClasses.SHAMAN),
        //bonus
        description = "The ultimate tome of magic.",
        level = 80
    )
    // ===== ARMOR SETS =====
    // General armor
    val LEATHER_BOOTS = EquipmentItem(
        id = "boots_01",
        name = "Leather boots",
        type = EquipmentType.BOOTS,
        allowedClasses = listOf(PlayerClasses.ADVENTURIST, PlayerClasses.KNIGHT, PlayerClasses.MAGE, PlayerClasses.ASSASSIN, PlayerClasses.ARCHER, PlayerClasses.PRIEST, PlayerClasses.JOKER, PlayerClasses.BERSERK, PlayerClasses.SHAMAN),
        defenseBonus = 0.01,
        description = "You don't feel protected wearing these.",
        level = 1
    )
    val LEATHER_CHESTPLATE = EquipmentItem(
        id = "chestplate_01",
        name = "Leather chestplate",
        type = EquipmentType.CHESTPLATE,
        allowedClasses = listOf(PlayerClasses.ADVENTURIST, PlayerClasses.KNIGHT, PlayerClasses.MAGE, PlayerClasses.ASSASSIN, PlayerClasses.ARCHER, PlayerClasses.PRIEST, PlayerClasses.JOKER, PlayerClasses.BERSERK, PlayerClasses.SHAMAN),
        defenseBonus = 0.01,
        description = "You don't feel protected wearing these.",
        level = 1
    )
    val LEATHER_HELMET = EquipmentItem(
        id = "helmet_01",
        name = "Leather helmet",
        type = EquipmentType.HELMET,
        allowedClasses = listOf(PlayerClasses.ADVENTURIST, PlayerClasses.KNIGHT, PlayerClasses.MAGE, PlayerClasses.ASSASSIN, PlayerClasses.ARCHER, PlayerClasses.PRIEST, PlayerClasses.JOKER, PlayerClasses.BERSERK, PlayerClasses.SHAMAN),
        defenseBonus = 0.01,
        description = "You don't feel protected wearing these.",
        level = 1
    )
    val IRON_HELMET = EquipmentItem(
        id = "helmet_02",
        name = "Iron helmet",
        type = EquipmentType.HELMET,
        allowedClasses = listOf(PlayerClasses.ADVENTURIST, PlayerClasses.KNIGHT, PlayerClasses.ASSASSIN, PlayerClasses.ARCHER, PlayerClasses.BERSERK),
        defenseBonus = 0.10,
        accuracyBonus = -0.03,
        description = "A sturdy armor, able to maintain user's survivability.",
        level = 20
    )
    val IRON_CHESTPLATE = EquipmentItem(
        id = "chestplate_02",
        name = "Iron chestplate",
        type = EquipmentType.CHESTPLATE,
        allowedClasses = listOf(PlayerClasses.ADVENTURIST, PlayerClasses.KNIGHT, PlayerClasses.ASSASSIN, PlayerClasses.ARCHER, PlayerClasses.BERSERK),
        defenseBonus = 0.10,
        attackSpeedBonus = -0.05,
        description = "A sturdy armor, able to maintain user's survivability.",
        level = 20
    )
    val IRON_BOOTS = EquipmentItem(
        id = "boots_02",
        name = "Iron boots",
        type = EquipmentType.BOOTS,
        allowedClasses = listOf(PlayerClasses.ADVENTURIST, PlayerClasses.KNIGHT, PlayerClasses.ASSASSIN, PlayerClasses.ARCHER, PlayerClasses.BERSERK),
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
        description = "",
        level = 45
    )
    val ARCHER_CHESTPLATE = EquipmentItem(
        id = "chestplate_04",
        name = "Archer's coat",
        type = EquipmentType.HELMET,
        allowedClasses = listOf(PlayerClasses.ARCHER),
        accuracyBonus = 0.05,
        defenseBonus = -0.10,
        attackSpeedBonus = 0.05,
        description = "",
        level = 45
    )
    val ARCHER_BOOTS = EquipmentItem(
        id = "boots_04",
        name = "Archer's boots",
        type = EquipmentType.HELMET,
        allowedClasses = listOf(PlayerClasses.ARCHER),
        defenseBonus = -0.05,
        attackSpeedBonus = 0.15,
        description = "",
        level = 45
    )
    // Shields
    val WOODEN_SHIELD = EquipmentItem(
        id = "shield_01",
        name = "Wooden shield",
        type = EquipmentType.SHIELD,
        allowedClasses = listOf(PlayerClasses.ADVENTURIST, PlayerClasses.KNIGHT, PlayerClasses.MAGE, PlayerClasses.ASSASSIN, PlayerClasses.ARCHER, PlayerClasses.PRIEST, PlayerClasses.JOKER, PlayerClasses.BERSERK, PlayerClasses.SHAMAN),
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
        level = 1
    )
    val PATH_DEFENDER = EquipmentItem(
        id = "shield_03",
        name = "The Path's Defender",
        type = EquipmentType.SHIELD,
        allowedClasses = listOf(PlayerClasses.KNIGHT),
        defenseBonus = 0.5,
        description = "",
        level = 75
    )



}

