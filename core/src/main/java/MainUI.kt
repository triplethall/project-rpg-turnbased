package ru.triplethall.rpgturnbased

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle

class MainUI(
    private val font: BitmapFont,
    private val screenWidth: Float,
    private val screenHeight: Float,
    private val whitePixel: Texture,
    private val barTexture: Texture,
    private val pauseButtonTexture: Texture,
    private val inventoryButtonTexture: Texture,
    private val statsButtonTexture: Texture,
    private val pauseMenu: PauseMenu,
    private val inventory: Inventory,
    private var player: Player
) {
    // Области для кнопок (прямоугольники для обработки ввода)
    private val pauseButtonRect = Rectangle()
    private val inventoryButtonRect = Rectangle()
    private val statsButtonRect = Rectangle()

    // Размеры и отступы
    private val buttonSize = 120f // размер кнопок
    private val buttonSpacing = 20f // расстояние между кнопками по гор-ли
    private val barsX = 20f
    private val barsY = screenHeight - 60f
    private val barsWidth = 400f
    private val barsHeight = 80f
    private val gapBetweenBars = 0f

    // Отступ от баров до кнопок
    private val buttonsYOffset = 100f

    init {
        updateButtonRects()
    }

    private fun updateButtonRects() {
        // Располагаем кнопки по горизонтали, сразу под полосками баров
        val startX = barsX
        val baseY = barsY - barsHeight - buttonSpacing - buttonsYOffset

        pauseButtonRect.set(startX, baseY, buttonSize, buttonSize)
        inventoryButtonRect.set(startX + buttonSize + buttonSpacing, baseY, buttonSize, buttonSize)
        statsButtonRect.set(startX + (buttonSize + buttonSpacing) * 2, baseY, buttonSize, buttonSize)
    }

    fun updatePlayer(newPlayer: Player) {
        player = newPlayer
    }

    /**
     * Обрабатывает касания по кнопкам UI.
     * @return true, если касание было обработано (клик по одной из кнопок)
     */
    fun handleInput(touchX: Float, touchY: Float): Boolean {
        // Реагируем только на начало касания (один раз за клик)
        if (!Gdx.input.justTouched()) return false

        val invertedY = screenHeight - touchY

        if (pauseButtonRect.contains(touchX, invertedY)) {
            pauseMenu.toggle()
            return true
        }
        if (inventoryButtonRect.contains(touchX, invertedY)) {
            inventory.toggle()
            return true
        }
        if (statsButtonRect.contains(touchX, invertedY)) {
            pauseMenu.toggleStats()
            return true
        }
        return false
    }

    fun render(batch: SpriteBatch) {
        // Рисуем бары здоровья и маны
        drawPlayerBars(batch)

        // Рисуем кнопки
        batch.draw(pauseButtonTexture, pauseButtonRect.x, pauseButtonRect.y, buttonSize, buttonSize)
        batch.draw(inventoryButtonTexture, inventoryButtonRect.x, inventoryButtonRect.y, buttonSize, buttonSize)
        batch.draw(statsButtonTexture, statsButtonRect.x, statsButtonRect.y, buttonSize, buttonSize)
    }

    private fun drawPlayerBars(batch: SpriteBatch) {
        // Цвета баров (оригинальные из BattleScene)
        val hpColor = Color(0.5f, 0.15f, 0.1f, 1f)
        val mpColor = Color(0.129f, 0.216f, 0.471f, 1f)

        // Расчёт общей подложки
        val bgY = barsY - gapBetweenBars - barsHeight
        val bgHeight = barsHeight * 2 + gapBetweenBars
        batch.draw(barTexture, barsX - 85f, bgY - 40f, barsWidth * 1.8f, bgHeight * 1.4f)

        // Отступы для заливки
        val hpFillYOffset = barsHeight * 0.0f
        val mpFillYOffset = barsHeight * 0.45f
        val fillXOffset = barsWidth * 0.0875f
        val fillHeight = barsHeight / 2.4f

        val hpRatio = (player.currentHealth.toFloat() / player.maxHealth).coerceIn(0f, 1f)
        val mpRatio = (player.currentMana.toFloat() / player.maxMana).coerceIn(0f, 1f)

        val hpFillX = barsX + fillXOffset
        val hpFillY = barsY + hpFillYOffset
        val hpFullW = barsWidth + 85f
        val hpFillW = barsWidth * hpRatio + 85f

        val mpFillX = barsX + fillXOffset
        val mpFillY = (barsY - gapBetweenBars - barsHeight) + mpFillYOffset
        val mpFullW = barsWidth + 85f
        val mpFillW = barsWidth * mpRatio + 85f

        // Заливка HP
        batch.color = hpColor
        batch.draw(whitePixel, hpFillX, hpFillY, hpFillW, fillHeight)
        // Заливка MP
        batch.color = mpColor
        batch.draw(whitePixel, mpFillX, mpFillY, mpFillW, fillHeight)
        batch.color = Color.WHITE

        // Эффекты объёма
        addVolume(batch, hpFillX, hpFillY, hpFillW, fillHeight)
        addVolume(batch, mpFillX, mpFillY, mpFillW, fillHeight)

        // Переполнение (если статы выше максимума)
        drawOverflow(batch, hpFillX, hpFillY, fillHeight, hpFullW, player.currentHealth, player.maxHealth)
        drawOverflow(batch, mpFillX, mpFillY, fillHeight, mpFullW, player.currentMana, player.maxMana)

        // Текст с тенью
        val oldScale = font.data.scaleX
        font.data.setScale(3f)
        drawCenteredText(batch, "${player.currentHealth}/${player.maxHealth}", hpFillX, hpFillY, hpFullW, fillHeight)
        drawCenteredText(batch, "${player.currentMana}/${player.maxMana}", mpFillX, mpFillY, mpFullW, fillHeight)
        font.data.setScale(oldScale)
    }

    private fun addVolume(batch: SpriteBatch, fillX: Float, fillY: Float, fillW: Float, fillH: Float) {
        batch.color = Color(0f, 0f, 0f, 0.2f)
        batch.draw(whitePixel, fillX, fillY, fillW, fillH / 2f)
        batch.color = Color(1f, 1f, 1f, 0.25f)
        batch.draw(whitePixel, fillX, fillY + fillH * 0.75f, fillW, fillH / 4f)
        batch.color = Color.WHITE
    }

    private fun drawOverflow(batch: SpriteBatch, fillX: Float, fillY: Float, fillH: Float, fullW: Float, current: Int, max: Int) {
        if (current > max) {
            val overflowRatio = (current - max).toFloat() / max
            val overflowW = (fullW * overflowRatio).coerceAtMost(fullW)
            batch.color = Color(1f, 1f, 1f, 0.4f)
            batch.draw(whitePixel, fillX, fillY, overflowW, fillH)
            batch.color = Color.WHITE
        }
    }

    private fun drawCenteredText(batch: SpriteBatch, text: String, fillX: Float, fillY: Float, fullW: Float, fillH: Float) {
        val layout = GlyphLayout(font, text)
        val cx = fillX + (fullW - layout.width) / 2
        val cy = fillY + (fillH + layout.height) / 2 + 2f
        font.color = Color.BLACK
        font.draw(batch, text, cx + 2f, cy - 2f)
        font.color = Color.WHITE
        font.draw(batch, text, cx, cy)
    }
}
