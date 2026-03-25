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
    fun handleInput(player: Player): Boolean {
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
        font.color = Color.WHITE // player info

        font.draw(batch, "PLAYER", space + 430f, rectY - 120f)
        font.draw(batch, "${player.currentHealth}/${player.maxHealth}", space + 430f, rectY + rectHeight - 70f)
        batch.color = Color.RED // enemy rect

        batch.draw(whitePixel, rectX - 400f, rectY - 100f, rectWidth, rectHeight)
        font.color = Color.WHITE // enemy info

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

        val btnX = screenWidth / 2 - 100f
        val btnY = 100f // leave button


        batch.color = Color.GRAY // leave button

        batch.draw(whitePixel, btnX, btnY, 200f, 60f)
        font.color = Color.WHITE // text color

        font.draw(batch, "LEAVE", btnX + 30f, btnY + 35f)
        batch.color = Color.WHITE // button text
        // getDamageBtn
        val l_btnX = screenWidth / 2 - 100f
        val l_btnY = 300f // leave button


        batch.color = Color.GRAY // leave button

        batch.draw(whitePixel, l_btnX, l_btnY, 200f, 60f)
        font.color = Color.WHITE // text color

        font.draw(batch, "getDmg", l_btnX + 30f, l_btnY + 35f)
        batch.color = Color.WHITE // button text


        // Указываем стартовую позицию для верхнего бара
        val startY = 600f
        // Первый бар (Здоровье)
        drawStatBar(whitePixel, batch, player.currentHealth, player.maxHealth, 20f, startY, Color.RED)

        // Второй бар (Мана) — рисуем ниже на (высоту квадрата + отступы + наш gap)
        val secondBarY = startY - squareSize - (padding * 2) - verticalGap
        drawStatBar(whitePixel, batch, player.currentMana, player.maxMana, 20f, secondBarY, Color.BLUE)

    }
    // --- НАСТРОЙКИ (поменяй здесь одну цифру, и всё изменится) ---
    private val squareSize = 24 * 2f       // Размер одного квадратика
    private val spacing = 4 * 2f          // Расстояние между ними
    private val padding = 3 * 2f          // Внутренний отступ фона (рамка)
    private val totalBlocks = 10      // Сколько всего квадратиков

    private val verticalGap = 15f // Отступ между барами
    // -------------------------------------------------------------

    private fun drawStatBar(whitePixel: Texture, batch: SpriteBatch ,current: Int, max: Int, startX: Float, y: Float, baseColor: Color) {
        // Вычисляем общую ширину всей полоски автоматически
        val step = squareSize + spacing
        val totalBarWidth = (step * totalBlocks) - spacing // Убираем лишний отступ в конце


        // 1. Рисуем сплошной фон (подложку)
        batch.color = Color.BLACK // Черная рамка
        batch.draw(whitePixel,
            startX - padding,
            y - padding,
            totalBarWidth + (padding * 2),
            squareSize + (padding * 2)
        )

        val totalPercent = (current.toFloat() / max.toFloat()) * 100f

        // 2. Рисуем квадратики
        for (i in 0 until totalBlocks) {
            val lowBound = i * (100f / totalBlocks)
            val highBound = (i + 1) * (100f / totalBlocks)

            // Логика яркости/прозрачности
            val factor = when {
                totalPercent >= highBound -> 1.0f
                totalPercent <= lowBound -> 0.0f
                else -> (totalPercent - lowBound) / (100f / totalBlocks)
            }

            if (factor > 0) {
                batch.color = baseColor.cpy().mul(factor, factor, factor, 1f)
                // Координата X теперь зависит от настроек выше
                batch.draw(whitePixel, startX + (i * step), y, squareSize, squareSize)
            }
        }
        batch.color = Color.WHITE
    }

    private fun getDmg(player: Player) {
        // Отнимаем 10 хп, но не даем упасть ниже 0
        player.currentHealth = (player.currentHealth - 10).coerceAtLeast(0)

        // Для теста можно выводить в консоль
        println("Упс! У игрока осталось ${player.currentHealth} HP")
    }
    fun endBattleAndClearEnemy() {
        if (enemyX in 0 until gameMap.width && enemyY in 0 until gameMap.height) {
            gameMap.setTerrain(enemyX, enemyY, TerrainType.LAND)
        }
        isActive = false
        madeMoveThisTurn = false
    }

}
