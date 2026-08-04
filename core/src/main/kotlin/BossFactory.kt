package ru.triplethall.rpgturnbased

object BossFactory
{
    fun createBossEnemy() : BattleEnemy = BattleEnemy.fromType(Enemy.DRAGON_BOSS)
}
