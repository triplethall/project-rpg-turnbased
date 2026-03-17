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
    private val screenHeight: Float
) {
    var isVisible = false
        private set

    private val pauseButtonRect = Rectangle(0f, screenHeight - 120f, 120f, 120f)

    // Кнопки внутри меню
    private var resumeRect = Rectangle()
    private var exitRect = Rectangle()

    // Флаги для визуального отклика
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

        resumeRect.set(cX, pY + 120f, btnW, btnH)
        exitRect.set(cX, pY + 60f, btnW, btnH)
    }

    fun toggle() {
        isVisible = !isVisible
    }

    fun handleInput(): Boolean {
        val touchX = Gdx.input.x.toFloat()
        val touchY = Gdx.input.y.toFloat()

        // Проверяем только на экране, никаких преобразований координат!
        if (!isVisible) {
            if (Gdx.input.justTouched() && pauseButtonRect.contains(touchX, touchY)) {
                toggle()
                return true
            }

            // Всегда сбрасывать состояние флагов
            isResumePressed = false
            isExitPressed = false
            return false
        }

        // Меню открыто, обработаем нажатия на внутренние кнопки
        updateMenuRects()

        if (Gdx.input.justTouched()) {
            isResumePressed = resumeRect.contains(touchX, touchY)
            isExitPressed = exitRect.contains(touchX, touchY)

            if (isResumePressed) {
                toggle()
                return true
            }
            if (isExitPressed) {
                Gdx.app.exit()
                return true
            }
        } else {
            // Сброс состояния нажатий
            isResumePressed = false
            isExitPressed = false
        }
        return false
    }

    fun render(batch: SpriteBatch, whitePixel: Texture) {
        // Отображаем кнопку паузы только когда меню скрыто
        if (!isVisible) {
            batch.color = Color.BLACK
            batch.draw(whitePixel, pauseButtonRect.x, pauseButtonRect.y, pauseButtonRect.width, pauseButtonRect.height)

            font.color = Color.WHITE
            font.data.setScale(2f)
            font.draw(batch, "||", pauseButtonRect.x + 12f, pauseButtonRect.y + 28f)
            font.data.setScale(1f)
        }

        // Если меню не видно, ничего больше не рисуем
        if (!isVisible) return

        batch.color = Color(0f, 0f, 0f, 0.7f)
        batch.draw(whitePixel, 0f, 0f, screenWidth, screenHeight)

        updateMenuRects()

        // Рисуем панель меню
        batch.color = Color.DARK_GRAY
        val panelW = 900f
        val panelH = 600f
        val pX = (screenWidth - panelW) / 2
        val pY = (screenHeight - panelH) / 2
        batch.draw(whitePixel, pX, pY, panelW, panelH)

        // Продолжить игру (меняется цвет при нажатии)
        batch.color = if (isResumePressed) Color.LIGHT_GRAY else Color.GRAY
        batch.draw(whitePixel, resumeRect.x, resumeRect.y, resumeRect.width, resumeRect.height)

        // Выход из игры
        batch.color = if (isExitPressed) Color.LIGHT_GRAY else Color.GRAY
        batch.draw(whitePixel, exitRect.x, exitRect.y, exitRect.width, exitRect.height)

        // Надписи на кнопках
        font.color = Color.WHITE
        font.data.setScale(1.2f)
        font.draw(batch, "CONTINUE", resumeRect.x + 30f, resumeRect.y + 28f)
        font.draw(batch, "EXIT", exitRect.x + 70f, exitRect.y + 28f)
        font.data.setScale(1f)

        // Восстанавливаем исходный цвет
        batch.color = Color.WHITE
    }
}
