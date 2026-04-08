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
    private val backgroundMenuTexture: Texture,
    private val settingsButtonTexture: Texture   // НОВАЯ текстура для кнопки настроек
) {
    var isVisible = false
        private set

    private val pauseButtonRect = Rectangle(0f, screenHeight - 120f, 120f, 120f)

    var isStatsVisible = false
        private set
    private var closeButtonRect = Rectangle()

    // Прямоугольники для кнопок меню паузы (увеличенные)
    private var resumeRect = Rectangle()
    private var settingsRect = Rectangle()
    private var exitRect = Rectangle()

    // Прямоугольники для элементов окна настроек
    private var settingsWindowRect = Rectangle()
    private var closeSettingsRect = Rectangle()
    private var musicDownRect = Rectangle()
    private var musicUpRect = Rectangle()
    private var soundDownRect = Rectangle()
    private var soundUpRect = Rectangle()

    private var isSettingsVisible = false

    // Размеры увеличенных кнопок
    private val buttonWidth = 300f
    private val buttonHeight = 120f
    private val buttonSpacing = 30f

    // Размеры панели меню (подложки) – чуть шире, чтобы вместить кнопки
    private val panelWidth = 700f
    private val panelHeight = 550f

    // Размеры окна настроек
    private val settingsWindowWidth = 600f
    private val settingsWindowHeight = 400f

    // Вычисление позиции панели меню
    private fun getPanelRect(): Rectangle {
        val pX = (screenWidth - panelWidth) / 2
        val pY = (screenHeight - panelHeight) / 2
        return Rectangle(pX, pY, panelWidth, panelHeight)
    }

    // Обновление координат кнопок на основе панели
    private fun updateMenuRects() {
        val panel = getPanelRect()
        val pX = panel.x
        val pY = panel.y
        val panelH = panel.height

        // Вертикальное расположение: CONTINUE сверху, SETTINGS посередине, EXIT снизу
        val totalHeight = buttonHeight * 3 + buttonSpacing * 2
        val startY = pY + (panelH - totalHeight) / 2

        resumeRect.set(pX + (panelWidth - buttonWidth) / 2, startY + buttonHeight * 2 + buttonSpacing, buttonWidth, buttonHeight)
        settingsRect.set(pX + (panelWidth - buttonWidth) / 2, startY + buttonHeight + buttonSpacing, buttonWidth, buttonHeight)
        exitRect.set(pX + (panelWidth - buttonWidth) / 2, startY, buttonWidth, buttonHeight)
    }

    private fun getSettingsWindowRect(): Rectangle {
        val x = (screenWidth - settingsWindowWidth) / 2
        val y = (screenHeight - settingsWindowHeight) / 2
        return Rectangle(x, y, settingsWindowWidth, settingsWindowHeight)
    }

    private fun updateSettingsRects() {
        settingsWindowRect = getSettingsWindowRect()
        val winX = settingsWindowRect.x
        val winY = settingsWindowRect.y
        val winW = settingsWindowRect.width
        val winH = settingsWindowRect.height

        // Кнопка закрытия (крестик) в правом верхнем углу
        val closeSize = 50f
        closeSettingsRect.set(winX + winW - closeSize - 10f, winY + winH - closeSize - 10f, closeSize, closeSize)

        // Кнопки + и - для музыки
        val buttonSize = 60f
        val labelX = winX + 60f
        val valueX = winX + 300f
        var y = winY + winH - 120f

        musicDownRect.set(valueX + 20f, y, buttonSize, buttonSize)
        musicUpRect.set(valueX + 100f, y, buttonSize, buttonSize)

        y -= 80f
        soundDownRect.set(valueX + 20f, y, buttonSize, buttonSize)
        soundUpRect.set(valueX + 100f, y, buttonSize, buttonSize)
    }

    fun toggle() {
        isVisible = !isVisible
        if (!isVisible) {
            isStatsVisible = false
            isSettingsVisible = false
        }
    }

    fun toggleStats() {
        isStatsVisible = !isStatsVisible
        if (isStatsVisible) isSettingsVisible = false
    }

    private fun openSettings() {
        isSettingsVisible = true
        isStatsVisible = false
        updateSettingsRects()
    }

    private fun closeSettings() {
        isSettingsVisible = false
    }

    fun handleInput(player: Player? = null): Boolean {
        val touchX = Gdx.input.x.toFloat()
        val touchY = Gdx.input.y.toFloat()
        val gameY = screenHeight - touchY

        // Обработка окна статистики (если открыто)
        if (isStatsVisible) {
            updateCloseButtonRect()
            if (Gdx.input.justTouched() && closeButtonRect.contains(touchX, gameY)) {
                toggleStats()
                return true
            }
        }

        // Обработка окна настроек (если открыто)
        if (isSettingsVisible) {
            updateSettingsRects()
            if (Gdx.input.justTouched()) {
                // Кнопка закрытия настроек
                if (closeSettingsRect.contains(touchX, gameY)) {
                    closeSettings()
                    return true
                }
                // Кнопки регулировки громкости
                if (musicDownRect.contains(touchX, gameY)) {
                    SoundManager.musicVolume -= 0.1f
                    return true
                }
                if (musicUpRect.contains(touchX, gameY)) {
                    SoundManager.musicVolume += 0.1f
                    return true
                }
                if (soundDownRect.contains(touchX, gameY)) {
                    SoundManager.soundVolume -= 0.1f
                    return true
                }
                if (soundUpRect.contains(touchX, gameY)) {
                    SoundManager.soundVolume += 0.1f
                    return true
                }
            }
        }

        // Обработка самой паузы
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
                SoundManager.playSound("sounds/mainBtnSound.mp3")
                toggle()
                return true
            }
            if (settingsRect.contains(touchX, gameY)) {
                SoundManager.playSound("sounds/mainBtnSound.mp3")
                openSettings()
                return true
            }
            if (exitRect.contains(touchX, gameY)) {
                SoundManager.playSound("sounds/mainBtnSound.mp3")
                Gdx.app.exit()
                return true
            }
        }
        return false
    }

    private fun updateCloseButtonRect() {
        val statsRect = getStatsWindowRect()
        val closeBtnW = 100f
        val closeBtnH = 40f
        val closeBtnX = statsRect.x + statsRect.width - closeBtnW - 10f
        val closeBtnY = statsRect.y + 10f
        closeButtonRect.set(closeBtnX, closeBtnY, closeBtnW, closeBtnH)
    }

    private fun getStatsWindowRect(): Rectangle {
        val statsX = (screenWidth - 1000f) / 2
        val statsY = (screenHeight - 800f) / 2
        return Rectangle(statsX, statsY, 1000f, 800f)
    }

    fun render(batch: SpriteBatch, whitePixel: Texture, player: Player? = null) {
        // Кнопка паузы
        if (!isVisible) {
            batch.color = Color.WHITE
            batch.draw(pauseButtonTexture, pauseButtonRect.x, pauseButtonRect.y,
                pauseButtonRect.width, pauseButtonRect.height)
        }

        // Затемнение фона
        if (isVisible || isStatsVisible || isSettingsVisible) {
            batch.color = Color(0f, 0f, 0f, 0.7f)
            batch.draw(whitePixel, 0f, 0f, screenWidth, screenHeight)
        }

        // Меню паузы
        if (isVisible) {
            updateMenuRects()
            val panel = getPanelRect()
            batch.color = Color.WHITE
            batch.draw(backgroundMenuTexture, panel.x, panel.y, panel.width, panel.height)

            // Кнопка CONTINUE
            batch.draw(continueButtonTexture, resumeRect.x, resumeRect.y, resumeRect.width, resumeRect.height)
            // Кнопка SETTINGS
            batch.draw(settingsButtonTexture, settingsRect.x, settingsRect.y, settingsRect.width, settingsRect.height)
            // Кнопка EXIT
            batch.draw(exitButtonTexture, exitRect.x, exitRect.y, exitRect.width, exitRect.height)
        }

        // Окно статистики
        if (isStatsVisible && player != null) {
            renderStats(batch, whitePixel, player)
        }

        // Окно настроек
        if (isSettingsVisible) {
            renderSettings(batch, whitePixel)
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

    private fun renderSettings(batch: SpriteBatch, whitePixel: Texture) {
        updateSettingsRects()
        val win = settingsWindowRect
        // Фон окна
        batch.color = Color(0.2f, 0.2f, 0.2f, 1f)
        batch.draw(whitePixel, win.x, win.y, win.width, win.height)

        // Заголовок
        font.color = Color.WHITE
        font.data.setScale(1.8f)
        font.draw(batch, "SETTINGS", win.x + win.width / 2 - 70f, win.y + win.height - 40f)
        font.data.setScale(1.2f)

        // Текст "Music volume"
        font.draw(batch, "Music volume:", win.x + 60f, win.y + win.height - 120f)
        font.draw(batch, "${(SoundManager.musicVolume * 100).toInt()}%", win.x + 300f, win.y + win.height - 120f)

        // Текст "Sound volume"
        font.draw(batch, "Sound volume:", win.x + 60f, win.y + win.height - 200f)
        font.draw(batch, "${(SoundManager.soundVolume * 100).toInt()}%", win.x + 300f, win.y + win.height - 200f)

        // Кнопки + и -
        batch.color = Color.LIGHT_GRAY
        batch.draw(whitePixel, musicDownRect.x, musicDownRect.y, musicDownRect.width, musicDownRect.height)
        batch.draw(whitePixel, musicUpRect.x, musicUpRect.y, musicUpRect.width, musicUpRect.height)
        batch.draw(whitePixel, soundDownRect.x, soundDownRect.y, soundDownRect.width, soundDownRect.height)
        batch.draw(whitePixel, soundUpRect.x, soundUpRect.y, soundUpRect.width, soundUpRect.height)

        font.color = Color.BLACK
        font.data.setScale(1.5f)
        font.draw(batch, "-", musicDownRect.x + 20f, musicDownRect.y + 40f)
        font.draw(batch, "+", musicUpRect.x + 20f, musicUpRect.y + 40f)
        font.draw(batch, "-", soundDownRect.x + 20f, soundDownRect.y + 40f)
        font.draw(batch, "+", soundUpRect.x + 20f, soundUpRect.y + 40f)
        font.data.setScale(1.2f)

        // Кнопка закрытия (крестик)
        batch.color = Color.RED
        batch.draw(whitePixel, closeSettingsRect.x, closeSettingsRect.y, closeSettingsRect.width, closeSettingsRect.height)
        font.color = Color.WHITE
        font.data.setScale(1.2f)
        font.draw(batch, "X", closeSettingsRect.x + 15f, closeSettingsRect.y + 35f)
        font.data.setScale(1.0f)

        batch.color = Color.WHITE
    }
}
