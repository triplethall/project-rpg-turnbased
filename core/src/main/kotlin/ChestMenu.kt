package ru.triplethall.rpgturnbased

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle

class ChestMenu(private val font: BitmapFont) {
    var isVisible = false

    // Сделаем меню побольше
    private val menuWidth = 400f
    private val menuHeight = 300f

    // Прямоугольники для отрисовки и кликов
    private val menuRect = Rectangle()
    private val exitBtnRect = Rectangle()

    fun show() {
        isVisible = true
        // Обновляем позиции при открытии, если окно изменило размер
        updateLayout()
    }

    fun hide() {
        isVisible = false
    }

    private fun updateLayout() {
        val sw = Gdx.graphics.width.toFloat()
        val sh = Gdx.graphics.height.toFloat()

        menuRect.set((sw - menuWidth) / 2, (sh - menuHeight) / 2, menuWidth, menuHeight)
        // Кнопка внизу по центру меню
        exitBtnRect.set(menuRect.x + (menuWidth - 120) / 2, menuRect.y + 40, 120f, 60f)
    }

    fun handleInput(): Boolean {
        if (!isVisible) return false

        if (Gdx.input.justTouched()) {
            val touchX = Gdx.input.x.toFloat()
            // Важно: в LibGDX Y тача идет сверху вниз, а в камере снизу вверх
            val touchY = Gdx.graphics.height - Gdx.input.y.toFloat()

            if (exitBtnRect.contains(touchX, touchY)) {
                isVisible = false
                return true
            }
        }
        return isVisible
    }

    fun render(batch: SpriteBatch, whitePixel: Texture) {
        if (!isVisible) return

        updateLayout() // Чтобы меню всегда было в центре экрана

        // Фон (темно-серый)
        batch.color = Color(0.15f, 0.15f, 0.15f, 0.9f)
        batch.draw(whitePixel, menuRect.x, menuRect.y, menuRect.width, menuRect.height)

        // Рамка (светло-серая)
        batch.color = Color.LIGHT_GRAY
        val thickness = 2f
        batch.draw(whitePixel, menuRect.x, menuRect.y, menuRect.width, thickness) // низ
        batch.draw(whitePixel, menuRect.x, menuRect.y + menuRect.height, menuRect.width, thickness) // верх
        batch.draw(whitePixel, menuRect.x, menuRect.y, thickness, menuRect.height) // лево
        batch.draw(whitePixel, menuRect.x + menuRect.width, menuRect.y, thickness, menuRect.height) // право

        // Кнопка Exit
        batch.color = Color.FIREBRICK
        batch.draw(whitePixel, exitBtnRect.x, exitBtnRect.y, exitBtnRect.width, exitBtnRect.height)

        // Текст
        batch.color = Color.WHITE
        font.draw(batch, "TREASURE CHEST", menuRect.x + 80, menuRect.y + menuRect.height - 40)
        font.draw(batch, "EXIT", exitBtnRect.x + 35, exitBtnRect.y + 40)
    }
}
