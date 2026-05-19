package ru.triplethall.rpgturnbased

data class QuestGiver(
    val x: Int,
    val y: Int,
    val name: String = "Quest giver",
    val health: Int = 80,
    val maxHealth: Int = 80,
    val mana: Int = 50,
    val maxMana: Int = 50,
    val attack: Int = 15,
    val defense: Int = 12,
    val level: Int = 5
) {
    fun getStatsDescription(): String {
        return "$name (LVL. $level) | HP: $health/$maxHealth | ATK: $attack | DEF: $defense"
    }
}
