package ru.triplethall.rpgturnbased

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import kotlin.random.Random

enum class DebuffType {
    POISON,      // Отравление - урон каждый ход
    BURN,        // Горение - урон каждый ход
    FREEZE,      // Заморозка - замедление
    PARALYSIS,   // Паралич - пропуск хода
    CURSE,       // Проклятие - снижение всех характеристик
    WEAKNESS,    // Ослабление - снижение урона
    SLOW,        // Замедление - снижение скорости
    STUN,        // Оглушение - пропуск хода
    BLEED,       // Кровотечение - урон каждый ход
    SILENCE,     // Молчание - нельзя использовать магию
    WET,         // Мокрота - +25% урона от молний


    DODGE,              // Уклонение
    RESURRECTION,       // Воскрешение
    BUFF_DEFENSE,       // Бафф защиты
    BUFF_CRIT,          // Бафф крита
    BUFF_SPEED,         // Бафф скорости
    BUFF_DAMAGE,        // Бафф урона
    BUFF_INVULNERABLE,  // Неуязвимость
    BUFF_INFINITE_MANA, // Бесконечная мана
    BUFF_HEALTH,        // Бафф здоровья
    BUFF_WILL,          // Бафф воли
    CLONE,              // Клон
    BANNER              // Знамя
}
data class DebuffEffect(
    val type: DebuffType,
    val duration: Int,          // Оставшиеся ходы
    val maxDuration: Int,       // Максимальная длительность
    val intensity: Double = 1.0,        // интенсивность (1.0 = 100% эффекта)
    val stackCount: Int = 1     // Колво стаков
)
{
    fun tick(): DebuffEffect? {
        val newDuration = duration - 1
        return if(newDuration <=0) null else copy(duration = newDuration)
    }

    fun addStuck(additionalStacks: Int = 1, maxStack: Int = 5): DebuffEffect{
        val newStack = (stackCount + additionalStacks).coerceAtMost(maxStack)
        return copy(stackCount = newStack, duration = maxDuration)
    }
}

class DebuffManager {
    private val activeDebuffs = mutableMapOf<DebuffType, DebuffEffect>()

    fun addDebuffs(type: DebuffType, duration: Int, intensity: Double = 1.0, stacks: Int = 1)
    {
        val existing = activeDebuffs[type]
        if (existing != null)
        {
            // обновление существующего дебаффа
            val newStacks = (existing.stackCount + stacks).coerceAtMost(5)
            activeDebuffs[type] = existing.copy(
                duration = maxOf(existing.duration, duration),
                intensity = (existing.intensity + intensity).coerceAtMost(2.0),
                stackCount = newStacks
            )
        }
        else
        {
            activeDebuffs[type] = DebuffEffect(type, duration, duration, intensity, stacks)
        }
    }

    fun removeDebuff(type: DebuffType) {
        activeDebuffs.remove(type)
    }

    fun hasDebuff(type: DebuffType): Boolean = activeDebuffs.containsKey(type)

    fun getDebuff(type: DebuffType): DebuffEffect? = activeDebuffs[type]

    fun getAllDebuff(): List<DebuffEffect> = activeDebuffs.values.toList()

    fun tick(): List<DebuffEffect> {
        val expired = mutableListOf<DebuffType>()
        activeDebuffs.forEach { type, effect ->
            val newEffect = effect.tick()
            if (newEffect == null)
            {
                expired.add(type)
            }
            else
            {
                activeDebuffs[type] = newEffect
            }
        }
        expired.forEach { activeDebuffs.remove(it) }
        return activeDebuffs.values.toList()
    }

    fun clear() {
        activeDebuffs.clear()
    }

    fun isEmpty(): Boolean = activeDebuffs.isEmpty()
    fun size(): Int = activeDebuffs.size
}

data class StatModifiers(
    val damageMultiplier: Double,
    val speedMultiplier: Double,
    val defenseMultiplier: Double
)
// Интерфейс для получения урона от дебаффов
interface DamageReceiver {
    fun takeDebuffDamage(amount: Int)
}


class DebuffApplier {
    companion object{
        // Проверить, есть ли неуязвимость
        fun isInvulnerable(debuffs: List<DebuffEffect>): Boolean {
            return debuffs.any { it.type == DebuffType.BUFF_INVULNERABLE }
        }
        // Получить модификатор крит шанса
        fun getCritModifier(debuffs: List<DebuffEffect>): Double {
            var critMod = 1.0
            debuffs.forEach { debuff ->
                if (debuff.type == DebuffType.BUFF_CRIT) {
                    critMod *= debuff.intensity
                }
            }
            return critMod
        }
        // Получить модификатор максимального здоровья
        fun getHealthModifier(debuffs: List<DebuffEffect>): Double {
            var healthMod = 1.0
            debuffs.forEach { debuff ->
                if (debuff.type == DebuffType.BUFF_HEALTH) {
                    healthMod *= debuff.intensity
                }
            }
            return healthMod
        }
        // Проверить, бесконечная ли мана
        fun hasInfiniteMana(debuffs: List<DebuffEffect>): Boolean {
            return debuffs.any { it.type == DebuffType.BUFF_INFINITE_MANA }
        }
        // Проверить, активно ли воскрешение
        fun hasResurrection(debuffs: List<DebuffEffect>): Boolean {
            return debuffs.any { it.type == DebuffType.RESURRECTION }
        }
        // Получить шанс уклонения (максимум 80%)
        fun getDodgeChance(debuffs: List<DebuffEffect>, baseSpeed: Double, luck: Double): Double {
            val hasDodge = debuffs.any { it.type == DebuffType.DODGE }
            if (!hasDodge) return 0.0
            return (baseSpeed * 0.3 + luck * 0.5).coerceAtMost(0.8)
        }
        //применение урона от дебафов
        fun applyDamageDebuffs(target: DamageReceiver, debuffs: List<DebuffEffect>, maxHealth: Int): Int {
            var totalDamage = 0
            debuffs.forEach { debuffs ->
                val damage = when (debuffs.type)
                {
                    DebuffType.POISON -> (maxHealth * 0.05 * debuffs.intensity * debuffs.stackCount).toInt()
                    DebuffType.BURN -> (maxHealth * 0.08 * debuffs.intensity).toInt()
                    DebuffType.BLEED -> (maxHealth * 0.03 * debuffs.stackCount).toInt()
                    else -> 0
                }
                if (damage > 0)
                {
                    target.takeDebuffDamage(damage)
                    totalDamage += damage
                }
            }
            return totalDamage
        }
        // получать модификаторы характеристик от дебаффов
        fun getStatModifiers(debuffs: List<DebuffEffect>): StatModifiers {
            var damageMult = 1.0
            var speedMult = 1.0
            var defenseMult = 1.0


            debuffs.forEach { debuff ->
                when (debuff.type)
                {
                    // ===== БАФФЫ =====
                    DebuffType.BUFF_DAMAGE -> damageMult *= debuff.intensity
                    DebuffType.BUFF_SPEED -> speedMult *= debuff.intensity
                    DebuffType.BUFF_DEFENSE -> defenseMult *= debuff.intensity
                    DebuffType.BANNER -> {
                        damageMult *= debuff.intensity
                        speedMult *= debuff.intensity
                        speedMult *= debuff.intensity
                    }
                    DebuffType.CLONE -> {
                        damageMult *= debuff.intensity
                        speedMult *= debuff.intensity
                        defenseMult *= debuff.intensity
                    }

                    // ===== ДЕБАФФЫ =====
                    DebuffType.WET -> {}
                    DebuffType.WEAKNESS -> damageMult *= (1.0 - 0.25 * debuff.intensity)
                    DebuffType.CURSE -> {
                        damageMult *= (1.0 - 0.2 * debuff.intensity)
                        defenseMult *= (1.0 - 0.2 * debuff.intensity)
                    }
                    DebuffType.SLOW -> speedMult *= (1.0 - 0.4 * debuff.intensity)
                    DebuffType.PARALYSIS -> speedMult *= (1.0 - 0.3 * debuff.intensity)
                    else -> {}
                }
            }
            return StatModifiers(damageMult, speedMult, defenseMult)
        }
        // Проверить, пропускает ли цель ход
        fun shouldSkipTurn(debuffs: List<DebuffEffect>): Boolean {
            return debuffs.any {
                it.type == DebuffType.FREEZE ||
                    it.type == DebuffType.STUN ||
                    (it.type == DebuffType.PARALYSIS && Random.nextDouble() < 0.3) // 30% шанс
            }
        }
        // Проверить, может ли цель использовать магию
        fun canUseMagic(debuffs: List<DebuffEffect>): Boolean {
            return !debuffs.any { it.type == DebuffType.SILENCE }
        }
    }
}


// Класс для отображения дебаффов в битве
class DebuffRenderer(private val font: BitmapFont) {

    fun renderDebuffs(
        batch: SpriteBatch,
        debuffs: List<DebuffEffect>,
        x: Float,
        y: Float,
        isPlayer: Boolean = true
    ) {
        if (debuffs.isEmpty()) return

        val startX = if (isPlayer) x + 5f else x + 85f
        var currentY = y + 20f
        val iconSize = 24f
        val spacing = 28f

        debuffs.forEachIndexed { index, debuff ->
            val iconX = startX + (index * spacing)
            if (iconX < x + 95f) { // Не выходим за пределы
                drawDebuffIcon(batch, debuff, iconX, currentY, iconSize)
                // Рисуем количество стаков
                if (debuff.stackCount > 1) {
                    font.color = Color.WHITE
                    font.draw(batch, "${debuff.stackCount}", iconX + iconSize - 8f, currentY + iconSize - 4f)
                }
                // Рисуем длительность маленькими точками
                drawDurationDots(batch, debuff, iconX, currentY - 4f, iconSize)
            }
        }
    }

    private fun drawDebuffIcon(batch: SpriteBatch, debuff: DebuffEffect, x: Float, y: Float, size: Float) {
        // Цвет для разных типов
        batch.color = when (debuff.type) {
            // Дебаффы
            DebuffType.POISON -> Color.GREEN
            DebuffType.BURN -> Color.ORANGE
            DebuffType.FREEZE -> Color.CYAN
            DebuffType.PARALYSIS -> Color.YELLOW
            DebuffType.CURSE -> Color.PURPLE
            DebuffType.WEAKNESS -> Color.GRAY
            DebuffType.SLOW -> Color.LIGHT_GRAY
            DebuffType.STUN -> Color.GOLD
            DebuffType.BLEED -> Color.RED
            DebuffType.SILENCE -> Color.BROWN
            DebuffType.WET -> Color.BLUE

            // Баффы
            DebuffType.DODGE -> Color.CYAN
            DebuffType.RESURRECTION -> Color.GOLD
            DebuffType.BUFF_DEFENSE -> Color.BLUE
            DebuffType.BUFF_CRIT -> Color.ORANGE
            DebuffType.BUFF_SPEED -> Color.GREEN
            DebuffType.BUFF_DAMAGE -> Color.RED
            DebuffType.BUFF_INVULNERABLE -> Color.PINK
            DebuffType.BUFF_INFINITE_MANA -> Color.PURPLE
            DebuffType.BUFF_HEALTH -> Color.GREEN
            DebuffType.BUFF_WILL -> Color.WHITE
            DebuffType.CLONE -> Color.DARK_GRAY
            DebuffType.BANNER -> Color.GOLDENROD
        }

        font.color = batch.color
        val symbol = when (debuff.type) {
            // Дебаффы
            DebuffType.POISON -> "☠"
            DebuffType.BURN -> "🔥"
            DebuffType.FREEZE -> "❄"
            DebuffType.PARALYSIS -> "⚡"
            DebuffType.CURSE -> "👻"
            DebuffType.WEAKNESS -> "↓"
            DebuffType.SLOW -> "🐌"
            DebuffType.STUN -> "💫"
            DebuffType.BLEED -> "🩸"
            DebuffType.SILENCE -> "🔇"
            DebuffType.WET -> "💧"

            // Баффы
            DebuffType.DODGE -> "↗"
            DebuffType.RESURRECTION -> "✝"
            DebuffType.BUFF_DEFENSE -> "🛡"
            DebuffType.BUFF_CRIT -> "★"
            DebuffType.BUFF_SPEED -> "⚡"
            DebuffType.BUFF_DAMAGE -> "⚔"
            DebuffType.BUFF_INVULNERABLE -> "✨"
            DebuffType.BUFF_INFINITE_MANA -> "∞"
            DebuffType.BUFF_HEALTH -> "❤"
            DebuffType.BUFF_WILL -> "🧠"
            DebuffType.CLONE -> "👥"
            DebuffType.BANNER -> "🚩"
        }
        font.draw(batch, symbol, x + 4f, y + size - 6f)

        batch.color = Color.WHITE
    }

    private fun drawDurationDots(batch: SpriteBatch, debuff: DebuffEffect, x: Float, y: Float, size: Float) {
        val maxDots = 5
        val remaining = debuff.duration.coerceAtMost(maxDots)
        val dotSize = 3f
        val startX = x + 2f

        for (i in 0 until remaining) {
            batch.color = Color.YELLOW
            // В реальном проекте рисуем кружок
            val whitePixel = batch.color
            batch.color = Color.WHITE
        }
    }

    fun renderDebuffTooltip(batch: SpriteBatch, debuff: DebuffEffect, x: Float, y: Float) {
        val tooltipText = when (debuff.type) {
            // Дебаффы
            DebuffType.POISON -> "Отравление: ${(debuff.intensity * 5 * debuff.stackCount).toInt()}% урона/ход"
            DebuffType.BURN -> "Горение: ${(debuff.intensity * 8).toInt()}% урона/ход"
            DebuffType.FREEZE -> "Заморозка: пропуск хода"
            DebuffType.PARALYSIS -> "Паралич: 30% шанс пропуска хода"
            DebuffType.CURSE -> "Проклятие: -${(debuff.intensity * 20).toInt()}% ко всем статам"
            DebuffType.WEAKNESS -> "Ослабление: -${(debuff.intensity * 25).toInt()}% урона"
            DebuffType.SLOW -> "Замедление: -${(debuff.intensity * 40).toInt()}% скорости"
            DebuffType.STUN -> "Оглушение: пропуск хода"
            DebuffType.BLEED -> "Кровотечение: ${debuff.stackCount * 3}% урона/ход"
            DebuffType.SILENCE -> "Молчание: нельзя использовать магию"
            DebuffType.WET -> "Мокрый: +25% урона от молний"

            // Баффы
            DebuffType.DODGE -> "Уклонение: шанс до 80%"
            DebuffType.RESURRECTION -> "Воскрешение: возрождение с 50% HP"
            DebuffType.BUFF_DEFENSE -> "Защита: +${((debuff.intensity - 1) * 100).toInt()}%"
            DebuffType.BUFF_CRIT -> "Крит: +${((debuff.intensity - 1) * 100).toInt()}%"
            DebuffType.BUFF_SPEED -> "Скорость: +${((debuff.intensity - 1) * 100).toInt()}%"
            DebuffType.BUFF_DAMAGE -> "Урон: +${((debuff.intensity - 1) * 100).toInt()}%"
            DebuffType.BUFF_INVULNERABLE -> "Неуязвимость: иммунитет к урону"
            DebuffType.BUFF_INFINITE_MANA -> "Бесконечная мана"
            DebuffType.BUFF_HEALTH -> "Здоровье: +${((debuff.intensity - 1) * 100).toInt()}%"
            DebuffType.BUFF_WILL -> "Воля: +${((debuff.intensity - 1) * 100).toInt()}%"
            DebuffType.CLONE -> "Клон: ${(debuff.intensity * 100).toInt()}% характеристик"
            DebuffType.BANNER -> "Боевой стяг: +${((debuff.intensity - 1) * 100).toInt()}% урона и скорости"
        }
        font.color = Color.WHITE
        font.draw(batch, "$tooltipText (${debuff.duration} ходов)", x + 10f, y + 30f)
    }
}
