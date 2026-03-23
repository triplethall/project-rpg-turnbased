package ru.triplethall.rpgturnbased

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle

class PauseMenu(
    private val font: BitmapFont,
    private val screenWidth: Float,
    private val screenHeight: Float,
    private val pauseButtonTexture: Texture // Добавленный параметр для текстуры кнопки паузы
) {
    var isVisible = false
        private set

    private val pauseButtonRect = Rectangle(0f, screenHeight - 120f, 120f, 120f)

    var isStatsVisible = false
        private set
    private var closeButtonRect = Rectangle()

    private var resumeRect = Rectangle()
    private var statsRect = Rectangle()
    private var exitRect = Rectangle()

    private var isResumePressed = false
    private var isExitPressed = false

    private fun updateMenuRects() {
        val panelW = 900f
        val panelH = 600f
        val pX = (screenWidth - panelW) / 2
        val pY = (screenHeight - panelH) / 2

        val btnW = 600f
        val btnH = 120f
        val cX = pX + (panelW - btnW) / 2

        resumeRect.set(cX, pY + 350f, btnW, btnH)
        statsRect.set(cX, pY + 200f, btnW, btnH)
        exitRect.set(cX, pY + 50f, btnW, btnH)
    }

    fun toggle() {
        isVisible = !isVisible
        if (!isVisible) {
            isStatsVisible = false
        }
    }

    fun toggleStats() {
        isStatsVisible = !isStatsVisible
    }

    fun handleInput(player: Player? = null): Boolean {
        val touchX = Gdx.input.x.toFloat()
        val touchY = Gdx.input.y.toFloat()
        val gameY = screenHeight - touchY

        if (!isVisible) {
            if (Gdx.input.justTouched() && pauseButtonRect.contains(touchX, gameY)) {
                toggle()
                return true
            }
            isResumePressed = false
            isExitPressed = false
            return false
        }

        updateMenuRects()

        if (Gdx.input.justTouched()) {
            if (isStatsVisible && closeButtonRect.contains(touchX, gameY)) {
                toggleStats()
                return true
            }

            if (resumeRect.contains(touchX, gameY)) {
                toggle()
                return true
            }
            if (statsRect.contains(touchX, gameY)) {
                toggleStats()
                return true
            }
            if (exitRect.contains(touchX, gameY)) {
                Gdx.app.exit()
                return true
            }
        }

        isResumePressed = false
        isExitPressed = false
        return false
    }

    fun render(batch: SpriteBatch, whitePixel: Texture, player: Player? = null) {
        // Рисуем кнопку паузы только когда меню скрыто
        if (!isVisible) {
            batch.color = Color.WHITE
            batch.draw(
                pauseButtonTexture,
                pauseButtonRect.x,
                pauseButtonRect.y,
                pauseButtonRect.width,
                pauseButtonRect.height
            )
        }

        if (!isVisible) return

        // Затемнение фона
        batch.color = Color(0f, 0f, 0f, 0.7f)
        batch.draw(whitePixel, 0f, 0f, screenWidth, screenHeight)

        updateMenuRects()

        // Панель меню
        batch.color = Color.DARK_GRAY
        val panelW = 900f
        val panelH = 600f
        val pX = (screenWidth - panelW) / 2
        val pY = (screenHeight - panelH) / 2
        batch.draw(whitePixel, pX, pY, panelW, panelH)

        // Кнопка CONTINUE
        batch.color = if (isResumePressed) Color.LIGHT_GRAY else Color.GRAY
        batch.draw(whitePixel, resumeRect.x, resumeRect.y, resumeRect.width, resumeRect.height)

        // Кнопка STATISTICS
        batch.color = Color.GRAY // пока без изменения цвета при нажатии
        batch.draw(whitePixel, statsRect.x, statsRect.y, statsRect.width, statsRect.height)

        // Кнопка EXIT
        batch.color = if (isExitPressed) Color.LIGHT_GRAY else Color.GRAY
        batch.draw(whitePixel, exitRect.x, exitRect.y, exitRect.width, exitRect.height)

        // Текст на кнопках
        font.color = Color.WHITE
        font.data.setScale(2f)
        font.draw(batch, "CONTINUE", resumeRect.x + 60f, resumeRect.y + 70f)
        font.draw(batch, "STATISTICS", statsRect.x + 40f, statsRect.y + 70f)
        font.draw(batch, "EXIT", exitRect.x + 90f, exitRect.y + 70f)
        font.data.setScale(1f)

        // Отображение статистики
        if (isStatsVisible && player != null) {
            renderStats(batch, whitePixel, player, pX, pY, panelW, panelH)
        }

        batch.color = Color.WHITE
    }

    private fun renderStats(batch: SpriteBatch, whitePixel: Texture, player: Player, pX: Float, pY: Float, panelW: Float, panelH: Float) {
        val statsW = panelW * 0.9f
        val statsH = panelH * 0.6f
        val statsX = pX + (panelW - statsW) / 2
        val statsY = pY + 250f

        batch.color = Color(0.1f, 0.1f, 0.1f, 0.9f)
        batch.draw(whitePixel, statsX, statsY, statsW, statsH)

        font.color = Color.YELLOW
        font.data.setScale(1.5f)
        font.draw(batch, "Player Statistics", statsX + 20f, statsY + statsH - 20f)
        font.data.setScale(1.2f)

        val healthMod = player.getCorruptionHealthModifier()
        val damageMod = player.getCorruptionDamageModifier()

        val statsList = listOf(
            "Level: ${player.level}",
            "Experience: ${player.experience}/${player.getExpForNextLevel()} (${(player.getExpProgress() * 100).toInt()}%)",
            "Health: ${player.currentHealth}/${player.maxHealth}",
            "Mana: ${player.currentMana}/${player.maxMana}",
            "Damage: ${player.damage}",
            "Defense: ${(player.defense * 100).toInt()}%",
            "Accuracy: ${(player.accuracy * 100).toInt()}%",
            "Attack speed: ${String.format("%.2f", player.attackSpeed)}",
            "Will: ${(player.will * 100).toInt()}%",
            "Corruption: ${player.corruption}",
            "  Health modifier: ${(healthMod * 100).toInt()}%",
            "  Damage modifier: ${(damageMod * 100).toInt()}%"
        )

        var yOffset = statsY + statsH - 60f
        for (stat in statsList) {
            if (stat.startsWith("  ")) {
                font.color = Color.LIGHT_GRAY
            } else {
                font.color = Color.WHITE
            }
            font.draw(batch, stat, statsX + 30f, yOffset)
            yOffset -= 30f
        }

        // Кнопка закрытия статистики
        val closeBtnW = 100f
        val closeBtnH = 40f
        val closeBtnX = statsX + statsW - closeBtnW - 10f
        val closeBtnY = statsY + 10f

        closeButtonRect.set(closeBtnX, closeBtnY, closeBtnW, closeBtnH)

        batch.color = Color.RED
        batch.draw(whitePixel, closeBtnX, closeBtnY, closeBtnW, closeBtnH)
        font.color = Color.WHITE
        font.data.setScale(1f)
        font.draw(batch, "CLOSE", closeBtnX + 20f, closeBtnY + 28f)
        font.data.setScale(1.2f)
    }
}
