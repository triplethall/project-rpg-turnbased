package ru.triplethall.rpgturnbased

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle


class BattleScene(
    private val font: BitmapFont,
    private val screenWidth: Float,
    private val screenHeight: Float,
    private val gameMap: GameMap
) {
    var isActive = false
        private set
    private val exitButtonRect = Rectangle()
    private var enemyX = 0
    private var enemyY = 0
    fun startBattle(enemyCellX: Int, enemyCellY: Int) {
        this.enemyX = enemyCellX
        this.enemyY = enemyCellY
        isActive = true
    }
    fun handleInput(): Boolean {
        if (!isActive) {
            return false
        }

        val touchX = Gdx.input.x.toFloat()
        val touchY = Gdx.input.y.toFloat()
        val yInverted = screenHeight - touchY

        if (Gdx.input.justTouched() && exitButtonRect.contains(touchX, yInverted)) {
            endBattleAndClearEnemy()  // вызываем очистку
            return true
        }

        return false
    }

    fun render(batch: SpriteBatch, whitePixel: Texture, player: Player)
    {
        if (!isActive)
        {
            return
        }

        batch.color = Color(0f, 0f, 0f, 0.8f)
        batch.draw(whitePixel, 0f, 0f, screenWidth, screenHeight)
        batch.color = Color.BLUE
        batch.draw(whitePixel, 200f, 300f, 150f, 200f)
        font.color = Color.WHITE
        font.draw(batch, "PLAYER", 230f, 450f)
        font.draw(batch, "${player.currentHealth}/${player.maxHealth}", 220f, 380f)
        batch.color = Color.RED
        batch.draw(whitePixel, 550f, 300f, 150f, 200f)
        font.color = Color.WHITE
        font.draw(batch, "ENEMY", 590f, 450f)
        font.draw(batch, "currentHealth/maxHealth", 600f, 380f)
        val btnX = screenWidth / 2 - 100f
        val btnY = 100f
        exitButtonRect.set(btnX, btnY, 200f, 60f)
        batch.color = Color.GRAY
        batch.draw(whitePixel, btnX, btnY, 200f, 60f)
        font.color = Color.WHITE
        font.draw(batch, "LEAVE", btnX + 30f, btnY + 35f)
        batch.color = Color.WHITE
    }
    fun endBattleAndClearEnemy() {
        if (enemyX in 0 until gameMap.width && enemyY in 0 until gameMap.height) {
            gameMap.setTerrain(enemyX, enemyY, TerrainType.LAND)
        }
        isActive = false
    }

}
