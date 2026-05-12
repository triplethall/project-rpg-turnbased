package ru.triplethall.rpgturnbased

import com.badlogic.gdx.Gdx
import kotlin.random.Random

enum class TrapType // типы ловушек
{
    HEALTH_LOSS,
    POISON_TRAP,
    MANA_BURN,
    SLOW_TRAP,
    WEAK_TRAP
}
object TrapManager
{
    fun randomTrapType(): TrapType = TrapType.entries.random() // случайный тип ловушек при генерации

    // применить эффект ловушки на игрока
    fun applyTrap(player: Player, trapType: TrapType)
    {
        when (trapType)
        {
            TrapType.HEALTH_LOSS -> {
                val maxHP = player.maxHealth
                val prcnt = Random.nextDouble(0.3, 0.7)
                val dmg = (maxHP * prcnt).toInt().coerceAtLeast(1)
                player.currentHealth = (player.currentHealth - dmg).coerceAtLeast(0)
                Gdx.app.log("TRAP_DEBUG", "trap dealt $dmg damage")
            }
            TrapType.POISON_TRAP -> {
                player.applyDebuff(DebuffType.POISON, 5)
                Gdx.app.log("TRAP_DEBUG", "palyer got poisoned")
            }
            TrapType.MANA_BURN -> {
                val burnedManaPrcnt = Random.nextDouble(0.3, 0.6)
                player.currentMana = (player.currentMana * burnedManaPrcnt).toInt().coerceAtLeast(0)
                Gdx.app.log("TRAP_DEBUG", "mana burned for $burnedManaPrcnt")
            }
            TrapType.SLOW_TRAP -> {
                player.applyDebuff(DebuffType.SLOW, 7)
                Gdx.app.log("TRAP_DEBUG","player got slowed")
            }
            TrapType.WEAK_TRAP -> {
                player.applyDebuff(DebuffType.WEAKNESS, 5)
                Gdx.app.log("TRAP_DEBUG", "player got weak")
            }
        }
    }
    // шанс избежать ловушки, чтобы задействовать удачу игрока
    fun evadeTrap(player: Player): Boolean
    {
        return Random.nextDouble() < (player.luck * 0.5)
    }
}
