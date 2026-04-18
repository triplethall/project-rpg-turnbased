package ru.triplethall.rpgturnbased

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle
enum class ChestAction
{
    NONE,
    IGNORE,
    OPEN,
    ATTACK
}
class ChestMenu(private val font: BitmapFont) {
    var isVisible = false
    private var action: ChestAction = ChestAction.NONE

    // Сделаем меню побольше
    private val menuWidth = 500f
    private val menuHeight = 300f

    // Прямоугольники для отрисовки и кликов
    private val menuRect = Rectangle()
    private val ignoreBtnRect = Rectangle()
    private val openBtnRect = Rectangle()
    private val attackBtnRect = Rectangle()

    fun show() {
        isVisible = true
        action = ChestAction.NONE
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

        val buttonWidth = 120f
        val buttonHeight = 60f
        val spacing = 20f
        val totalWidth = 3 * buttonWidth + 2 * spacing
        val startX = menuRect.x + (menuWidth - totalWidth) / 2
        val buttonY = menuRect.y + 40

        ignoreBtnRect.set(startX, buttonY, buttonWidth, buttonHeight)
        openBtnRect.set(startX + buttonWidth + spacing, buttonY, buttonWidth, buttonHeight)
        attackBtnRect.set(startX + 2 * (buttonWidth + spacing), buttonY, buttonWidth, buttonHeight)
    }

    fun handleInput(): ChestAction {
        if (!isVisible) return ChestAction.NONE

        if (Gdx.input.justTouched()) {
            val touchX = Gdx.input.x.toFloat()
            // Важно: в LibGDX Y тача идет сверху вниз, а в камере снизу вверх
            val touchY = Gdx.graphics.height - Gdx.input.y.toFloat()

            when
            {
                ignoreBtnRect.contains(touchX, touchY) -> {
                    action = ChestAction.IGNORE
                    isVisible = false
                }
                openBtnRect.contains(touchX, touchY) -> {
                    action = ChestAction.OPEN
                    isVisible = false
                }
                attackBtnRect.contains(touchX, touchY) -> {
                    action = ChestAction.ATTACK
                    isVisible = false
                }
                else -> action = ChestAction.NONE
            }
        }
        return action
    }

    fun render(batch: SpriteBatch, whitePixel: Texture) {
        if (!isVisible) return

        updateLayout() // Чтобы меню всегда было в центре экрана

        // Фон (темно-серый)
        batch.color = Color(0.15f, 0.15f, 0.15f, 0.9f)
        batch.draw(whitePixel, menuRect.x, menuRect.y, menuRect.width - 100f, menuRect.height)

        // Рамка (светло-серая)
        batch.color = Color.LIGHT_GRAY
        val thickness = 2f
        batch.draw(whitePixel, menuRect.x, menuRect.y, menuRect.width, thickness) // низ
        batch.draw(whitePixel, menuRect.x, menuRect.y + menuRect.height, menuRect.width, thickness) // верх
        batch.draw(whitePixel, menuRect.x, menuRect.y, thickness, menuRect.height) // лево
        batch.draw(whitePixel, menuRect.x + menuRect.width, menuRect.y, thickness, menuRect.height) // право

        // Кнопки
        // IGNORE
        batch.color = Color.DARK_GRAY
        batch.draw(whitePixel, ignoreBtnRect.x, ignoreBtnRect.y, ignoreBtnRect.width, ignoreBtnRect.height)
        // OPEN
        batch.color = Color.YELLOW
        batch.draw(whitePixel, openBtnRect.x, openBtnRect.y, openBtnRect.width, openBtnRect.height)
        // ATTACK
        batch.color = Color.RED
        batch.draw(whitePixel, attackBtnRect.x, attackBtnRect.y, attackBtnRect.width, attackBtnRect.height)

        // Текст
        batch.color = Color.WHITE
        font.draw(batch, "TREASURE CHEST", menuRect.x + 80, menuRect.y + menuRect.height - 40)
        font.draw(batch, "IGNORE", ignoreBtnRect.x + 35, ignoreBtnRect.y + 40)
        font.draw(batch, "OPEN", openBtnRect.x + 35, openBtnRect.y + 40)
        font.draw(batch, "ATTACK", attackBtnRect.x + 35, attackBtnRect.y + 40)
    }
}
