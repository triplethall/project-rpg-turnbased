package ru.triplethall.rpgturnbased

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle
import kotlin.random.Random


class BattleScene(
    private val font: BitmapFont,
    private val screenWidth: Float,
    private val screenHeight: Float,
    private val gameMap: GameMap,
    private val BGArena: Texture
) {
    var isActive = false
        private set
    private lateinit var player: Player
    fun setPlayer(player: Player)
    {
        this.player = player
    }
    private val attackButtonRect = Rectangle()
    private val nextTurnButtonRect = Rectangle()
    private val fleeButtonRect = Rectangle()
    private var enemyX = 0
    private var enemyY = 0
    private var madeMoveThisTurn = false
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

        if (Gdx.input.justTouched()) {
            // Кнопка атаки
            if (!madeMoveThisTurn && attackButtonRect.contains(touchX, yInverted)) {
                performAttack()
                return true
            }

            // Кнопка перехода хода
            if (madeMoveThisTurn && nextTurnButtonRect.contains(touchX, yInverted)) {
                nextTurn()
                return true
            }

            // Кнопка побега
            if (!madeMoveThisTurn && fleeButtonRect.contains(touchX, yInverted)) {
                flee()
                return true
            }
        }
        return false
    }
    private fun performAttack()
    {
        val baseDamage = player.damage
        val randomMultiplier = 0.8 + Random.nextDouble() * 0.4
        val totalDamage = (baseDamage * randomMultiplier).toInt()
        madeMoveThisTurn = true
    }
    private fun nextTurn()
    {
        madeMoveThisTurn = false
    }
    private fun flee()
    {
        if (Random.nextInt(1, 5) <= 1)
        {
            endBattleAndClearEnemy()
        }
        madeMoveThisTurn = true
    }
    fun render(batch: SpriteBatch, whitePixel: Texture, player: Player)
    {
        if (!isActive)
        {
            return
        }
        val rectHeight = 200f
        val rectWidth = 150f
        val space = screenWidth * 0.1f
        val rectY = (screenHeight - rectHeight) / 2
        val rectX = screenWidth - rectWidth - space
        batch.draw(BGArena, 0f, 0f, screenWidth, screenHeight)
        // размеры кнопок
        val buttonWidth = 150f
        val buttonHeight = 70f
        val buttonSpacing = 30f
        val buttonY = 50f
        // позиции кнопок
        val totalWidth = buttonWidth * 3 + buttonSpacing * 2
        val startX = (screenWidth - totalWidth) / 2
        val attackX = startX
        val turnX = startX + buttonWidth + buttonSpacing
        val fleeX = startX + (buttonWidth + buttonSpacing) * 2
        //
        attackButtonRect.set(attackX, buttonY, buttonWidth, buttonHeight)
        nextTurnButtonRect.set(turnX, buttonY, buttonWidth, buttonHeight)
        fleeButtonRect.set(fleeX, buttonY, buttonWidth, buttonHeight)

        batch.color = Color.BLUE
        batch.draw(whitePixel, space + 400f, rectY - 100f, rectWidth, rectHeight)
        font.color = Color.WHITE
        font.draw(batch, "PLAYER", space + 430f, rectY - 120f)
        font.draw(batch, "${player.currentHealth}/${player.maxHealth}", space + 430f, rectY + rectHeight - 70f)
        batch.color = Color.RED
        batch.draw(whitePixel, rectX - 400f, rectY - 100f, rectWidth, rectHeight)
        font.color = Color.WHITE
        font.draw(batch, "ENEMY", rectX - 400f, rectY - 120f)
        font.draw(batch, "currentHealth/maxHealth", rectX - 400f, rectY + rectHeight - 70f)
        // Кнопки
        // Атака
        batch.color = if (!madeMoveThisTurn) Color.GREEN else Color.DARK_GRAY
        batch.draw(whitePixel, attackX, buttonY, buttonWidth, buttonHeight)
        font.color = Color.WHITE
        font.draw(batch, "ATTACK", attackX + 45f, buttonY + 42f)

        // Следующий ход
        batch.color = if (madeMoveThisTurn) Color.ORANGE else Color.DARK_GRAY
        batch.draw(whitePixel, turnX, buttonY, buttonWidth, buttonHeight)
        font.draw(batch, "NEXT TURN", turnX + 55f, buttonY + 42f)

        // Побег
        batch.color = if (!madeMoveThisTurn) Color.RED else Color.DARK_GRAY
        batch.draw(whitePixel, fleeX, buttonY, buttonWidth, buttonHeight)
        font.draw(batch, "ESCAPE", fleeX + 45f, buttonY + 42f)

    }
    fun endBattleAndClearEnemy() {
        if (enemyX in 0 until gameMap.width && enemyY in 0 until gameMap.height) {
            gameMap.setTerrain(enemyX, enemyY, TerrainType.LAND)
        }
        isActive = false
        madeMoveThisTurn = false
    }

}
