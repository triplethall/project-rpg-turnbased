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
    private val pauseButtonTexture: Texture,
    private val statsBackgroundTexture: Texture,
    private val continueButtonTexture: Texture,
    private val exitButtonTexture: Texture,
    private val backgroundMenuTexture: Texture  // НОВОЕ: текстура фона меню паузы
) {
    var isVisible = false
        private set

    private val pauseButtonRect = Rectangle(0f, screenHeight - 120f, 120f, 120f)

    var isStatsVisible = false
        private set
    private var closeButtonRect = Rectangle()

    private var resumeRect = Rectangle()
    private var exitRect = Rectangle()

    // Размеры окна статистики (абсолютные)
    private val statsWindowWidth = 1000f
    private val statsWindowHeight = 800f

    // Отступы кнопок от краёв панели меню
    private val topMargin = 100f
    private val bottomMargin = 100f

    // Размеры панели меню (подложки)
    private val panelWidth = 900f
    private val panelHeight = 600f

    // Вычисление позиции панели меню
    private fun getPanelRect(): Rectangle {
        val pX = (screenWidth - panelWidth) / 2
        val pY = (screenHeight - panelHeight) / 2
        return Rectangle(pX, pY, panelWidth, panelHeight)
    }

    // Обновление координат кнопок на основе реальных размеров текстур
    private fun updateMenuRects() {
        val panel = getPanelRect()
        val pX = panel.x
        val pY = panel.y
        val panelH = panel.height

        val continueWidth = continueButtonTexture.width.toFloat()
        val continueHeight = continueButtonTexture.height.toFloat()
        val exitWidth = exitButtonTexture.width.toFloat()
        val exitHeight = exitButtonTexture.height.toFloat()

        val continueX = pX + (panelWidth - continueWidth) / 2
        val continueY = pY + panelH - topMargin - continueHeight
        resumeRect.set(continueX, continueY, continueWidth, continueHeight)

        val exitX = pX + (panelWidth - exitWidth) / 2
        val exitY = pY + bottomMargin
        exitRect.set(exitX, exitY, exitWidth, exitHeight)
    }

    // Обновление прямоугольника кнопки "CLOSE" в окне статистики
    private fun updateCloseButtonRect() {
        val statsRect = getStatsWindowRect()
        val closeBtnW = 100f
        val closeBtnH = 40f
        val closeBtnX = statsRect.x + statsRect.width - closeBtnW - 10f
        val closeBtnY = statsRect.y + 10f
        closeButtonRect.set(closeBtnX, closeBtnY, closeBtnW, closeBtnH)
    }

    private fun getStatsWindowRect(): Rectangle {
        val statsX = (screenWidth - statsWindowWidth) / 2
        val statsY = (screenHeight - statsWindowHeight) / 2
        return Rectangle(statsX, statsY, statsWindowWidth, statsWindowHeight)
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

        if (isStatsVisible) {
            updateCloseButtonRect()
            if (Gdx.input.justTouched() && closeButtonRect.contains(touchX, gameY)) {
                toggleStats()
                return true
            }
        }

        if (!isVisible) {
            if (Gdx.input.justTouched() && pauseButtonRect.contains(touchX, gameY)) {
                toggle()
                return true
            }
            return false
        }

        updateMenuRects()

        if (Gdx.input.justTouched()) {
            if (resumeRect.contains(touchX, gameY)) {
                toggle()
                return true
            }
            if (exitRect.contains(touchX, gameY)) {
                Gdx.app.exit()
                return true
            }
        }

        return false
    }

    fun render(batch: SpriteBatch, whitePixel: Texture, player: Player? = null) {
        // Кнопка паузы
        if (!isVisible) {
            batch.color = Color.WHITE
            batch.draw(pauseButtonTexture, pauseButtonRect.x, pauseButtonRect.y,
                pauseButtonRect.width, pauseButtonRect.height)
        }

        // Затемнение фона
        if (isVisible || isStatsVisible) {
            batch.color = Color(0f, 0f, 0f, 0.7f)
            batch.draw(whitePixel, 0f, 0f, screenWidth, screenHeight)
        }

        // Меню паузы
        if (isVisible) {
            updateMenuRects()
            val panel = getPanelRect()
            // Рисуем текстурную подложку вместо белого прямоугольника
            batch.color = Color.WHITE
            batch.draw(backgroundMenuTexture, panel.x, panel.y, panel.width, panel.height)

            // Кнопка CONTINUE
            batch.draw(continueButtonTexture, resumeRect.x, resumeRect.y,
                resumeRect.width, resumeRect.height)

            // Кнопка EXIT
            batch.draw(exitButtonTexture, exitRect.x, exitRect.y,
                exitRect.width, exitRect.height)
        }

        // Окно статистики
        if (isStatsVisible && player != null) {
            renderStats(batch, whitePixel, player)
        }

        batch.color = Color.WHITE
    }

    private fun renderStats(batch: SpriteBatch, whitePixel: Texture, player: Player) {
        updateCloseButtonRect()

        val statsRect = getStatsWindowRect()
        val statsX = statsRect.x
        val statsY = statsRect.y
        val statsW = statsRect.width
        val statsH = statsRect.height

        batch.color = Color.WHITE
        batch.draw(statsBackgroundTexture, statsX, statsY, statsW, statsH)

        font.color = Color.YELLOW
        font.data.setScale(1.8f)
        font.draw(batch, "Player Statistics", statsX + 30f, statsY + statsH - 40f)
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

        var yOffset = statsY + statsH - 100f
        for (stat in statsList) {
            font.color = if (stat.startsWith("  ")) Color.LIGHT_GRAY else Color.WHITE
            font.draw(batch, stat, statsX + 40f, yOffset)
            yOffset -= 35f
        }

        batch.color = Color.RED
        batch.draw(whitePixel, closeButtonRect.x, closeButtonRect.y,
            closeButtonRect.width, closeButtonRect.height)
        font.color = Color.WHITE
        font.data.setScale(1f)
        font.draw(batch, "CLOSE", closeButtonRect.x + 20f, closeButtonRect.y + 28f)
        font.data.setScale(1.2f)
    }
}
