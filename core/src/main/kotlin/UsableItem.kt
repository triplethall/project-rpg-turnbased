package ru.triplethall.rpgturnbased
import com.badlogic.gdx.graphics.Color
import kotlin.math.min
enum class ConsumableType {// Типы расходных предметов
    HEALTH_POTION_SMALL,    // Малое зелье здоровья (+15% HP)
    MANA_POTION_SMALL,      // Малое зелье маны (+15% MP)
    ANTIDOTE,               // Противоядие (снимает отравление)
    BANDAGE,                // Пластырь (снимает кровотечение)
    SPONGE,                 // Губка (снимает мокроту + 5% HP)
    HOLY_WATER              // Святая вода (снимает горение + проклятие)
}
data class ConsumableItem(// Класс расходного предмета
    val name: String,
    val description: String,
    val type: ConsumableType,
    val icon: String = "",          // Имя текстуры (для будущего UI)
    var quantity: Int = 1
) {
    fun useOutsideBattle(player: Player): Boolean {// Использование предмета вне боя (в инвентаре)
        if (quantity <= 0) return false
        val success = when (type) {
            ConsumableType.HEALTH_POTION_SMALL -> {
                val healAmount = (player.maxHealth * 0.15).toInt().coerceAtLeast(1)
                val oldHealth = player.currentHealth
                player.currentHealth = min(player.currentHealth + healAmount, player.maxHealth)
                println("Used: $name. Restored ${player.currentHealth - oldHealth} HP")
                true
            }
            ConsumableType.MANA_POTION_SMALL -> {
                val manaAmount = (player.maxMana * 0.15).toInt().coerceAtLeast(1)
                val oldMana = player.currentMana
                player.currentMana = min(player.currentMana + manaAmount, player.maxMana)
                println("Used: $name. Restored ${player.currentMana - oldMana} MP")
                true
            }
            ConsumableType.ANTIDOTE -> {
                if (player.debuffManager.hasDebuff(DebuffType.POISON)) {
                    player.debuffManager.removeDebuff(DebuffType.POISON)
                    println("Used: $name. Poisoning is removed!")
                    true
                } else {
                    println("$name does not work: there is no poisoning")
                    false
                }
            }
            ConsumableType.BANDAGE -> {
                if (player.debuffManager.hasDebuff(DebuffType.BLEED)) {
                    player.debuffManager.removeDebuff(DebuffType.BLEED)
                    println("Used: $name. The bleeding has stopped!")
                    true
                } else {
                    println("$name does not work: no bleeding")
                    false
                }
            }
            ConsumableType.SPONGE -> {
                val healAmount = (player.maxHealth * 0.05).toInt().coerceAtLeast(1)
                val oldHealth = player.currentHealth
                player.currentHealth = min(player.currentHealth + healAmount, player.maxHealth)
                if (player.debuffManager.hasDebuff(DebuffType.WET)) {
                    player.debuffManager.removeDebuff(DebuffType.WET)
                }
                println("Used: $name. Restored ${player.currentHealth - oldHealth} HP, Phlegm is removed")
                true
            }
            ConsumableType.HOLY_WATER -> {
                var anyRemoved = false
                if (player.debuffManager.hasDebuff(DebuffType.BURN)) {
                    player.debuffManager.removeDebuff(DebuffType.BURN)
                    anyRemoved = true
                }
                if (player.debuffManager.hasDebuff(DebuffType.CURSE)) {
                    player.debuffManager.removeDebuff(DebuffType.CURSE)
                    anyRemoved = true
                }
                if (anyRemoved) {
                    println("Использовано: $name. Сняты проклятия и горение!")
                    true
                } else {
                    println("$name не действует: нет проклятия или горения")
                    false
                }
            }
        }
        if (success) {
            quantity--
        }
        return success
    }
    fun useInBattle(player: Player, battleMessage: ((String, Color) -> Unit)? = null): Boolean {    // Использование предмета в бою (тратит ход)
        if (quantity <= 0) return false
        val log = { msg: String, color: Color ->
            println(msg)
            battleMessage?.invoke(msg, color)
        }
        val success = when (type) {
            ConsumableType.HEALTH_POTION_SMALL -> {
                val healAmount = (player.maxHealth * 0.15).toInt().coerceAtLeast(1)
                val oldHealth = player.currentHealth
                player.currentHealth = min(player.currentHealth + healAmount, player.maxHealth)
                log("🍺 $name restored ${player.currentHealth - oldHealth} HP!", Color.GREEN)
                true
            }
            ConsumableType.MANA_POTION_SMALL -> {
                val manaAmount = (player.maxMana * 0.15).toInt().coerceAtLeast(1)
                val oldMana = player.currentMana
                player.currentMana = min(player.currentMana + manaAmount, player.maxMana)
                log("🔮 $name restored ${player.currentMana - oldMana} MP!", Color.CYAN)
                true
            }
            ConsumableType.ANTIDOTE -> {
                if (player.debuffManager.hasDebuff(DebuffType.POISON)) {
                    player.debuffManager.removeDebuff(DebuffType.POISON)
                    log("💚 $name removed the poisoning!", Color.GREEN)
                    true
                } else {
                    log("⚠️ $name does not work: there is no poisoning!", Color.RED)
                    false
                }
            }
            ConsumableType.BANDAGE -> {
                if (player.debuffManager.hasDebuff(DebuffType.BLEED)) {
                    player.debuffManager.removeDebuff(DebuffType.BLEED)
                    log("🩹 $name stopped the bleeding!", Color.GREEN)
                    true
                } else {
                    log("⚠️ $name does not work: no bleeding!", Color.RED)
                    false
                }
            }
            ConsumableType.SPONGE -> {
                val healAmount = (player.maxHealth * 0.05).toInt().coerceAtLeast(1)
                val oldHealth = player.currentHealth
                player.currentHealth = min(player.currentHealth + healAmount, player.maxHealth)
                if (player.debuffManager.hasDebuff(DebuffType.WET)) {
                    player.debuffManager.removeDebuff(DebuffType.WET)
                }
                log("🧽 $name restored ${player.currentHealth - oldHealth} HP and he took off his phlegm!", Color.GREEN)
                true
            }
            ConsumableType.HOLY_WATER -> {
                var anyRemoved = false
                if (player.debuffManager.hasDebuff(DebuffType.BURN)) {
                    player.debuffManager.removeDebuff(DebuffType.BURN)
                    anyRemoved = true
                }
                if (player.debuffManager.hasDebuff(DebuffType.CURSE)) {
                    player.debuffManager.removeDebuff(DebuffType.CURSE)
                    anyRemoved = true
                }
                if (anyRemoved) {
                    log("💧 $name Lifted the curse and burn!", Color.PURPLE)
                    true
                } else {
                    log("⚠️ $name not effective: no curse or burning!", Color.RED)
                    false
                }
            }
        }
        if (success) {
            quantity--
        }
        return success
    }
    fun copyWithQuantity(newQuantity: Int): ConsumableItem {    // Создать копию с заданным количеством
        return copy(quantity = newQuantity)
    }
    companion object {
        fun healthPotionSmall(quantity: Int = 1) = ConsumableItem(// методы для создания предметов
            name = "A small bottle of health",
            description = "Restores 15% of health",
            type = ConsumableType.HEALTH_POTION_SMALL,
            quantity = quantity
        )
        fun manaPotionSmall(quantity: Int = 1) = ConsumableItem(
            name = "A small bottle of mana",
            description = "Restores 15% of mana",
            type = ConsumableType.MANA_POTION_SMALL,
            quantity = quantity
        )
        fun antidote(quantity: Int = 1) = ConsumableItem(
            name = "Antidote",
            description = "Relieves poisoning",
            type = ConsumableType.ANTIDOTE,
            quantity = quantity
        )
        fun bandage(quantity: Int = 1) = ConsumableItem(
            name = "Bandage",
            description = "Stops the bleeding",
            type = ConsumableType.BANDAGE,
            quantity = quantity
        )
        fun sponge(quantity: Int = 1) = ConsumableItem(
            name = "Sponge",
            description = "Removes phlegm and restores 5% health",
            type = ConsumableType.SPONGE,
            quantity = quantity
        )
        fun holyWater(quantity: Int = 1) = ConsumableItem(
            name = "Holy Water",
            description = "Relieves burn and curse",
            type = ConsumableType.HOLY_WATER,
            quantity = quantity

        )
        fun getStartingItems(): List<ConsumableItem> = listOf(        // Получить список всех базовых расходников для старта
            healthPotionSmall(3),
            manaPotionSmall(2),
            antidote(1),
            bandage(1)
        )
    }
}
