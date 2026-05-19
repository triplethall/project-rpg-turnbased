package ru.triplethall.rpgturnbased

import com.badlogic.gdx.Gdx

enum class UpgradeType
{
    LEVELUP,
    ADDSPELL,
    SPELLUPGRADE
}
object UpgradeSystem
{
    fun collectUpgrade(player: Player, gameMap: GameMap, x: Int, y: Int, type: UpgradeType = UpgradeType.LEVELUP): Boolean {
        if (gameMap.getTerrain(x, y) != TerrainType.UPGRADE) return false

        when (type) {
            UpgradeType.LEVELUP -> {
                player.addExperience(player.getExpForNextLevel()) // повышает уровень на 1
                Gdx.app.log("UPGRADE_DEBUG", "player leveled up!")
            }
            UpgradeType.ADDSPELL -> {
                Gdx.app.log("UPGRADE_DEBUG", "player got new spell!.. not") // пока у всех при старте есть навыки, так что пока оставлю так (прост как-то нелогично будет что у рыцаря огненный выстрел)
            }
            UpgradeType.SPELLUPGRADE -> {
                Gdx.app.log("UPGRADE_DEBUG", "player upgraded his spell!.. not") // у навыков нет пока уровней или какой-то прокачки
            }
        }

        gameMap.setTerrain(x, y, TerrainType.LAND) // убираем апгрейд с карты, чтобы не собирался повторно
        return true
    }

}
