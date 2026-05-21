package ru.triplethall.rpgturnbased

object Boss
{
    fun createBossEnemy() : BattleEnemy = BattleEnemy.fromType(Enemy.DRAGON_BOSS)
}
