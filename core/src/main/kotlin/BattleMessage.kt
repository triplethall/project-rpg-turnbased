package ru.triplethall.rpgturnbased

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import kotlin.math.pow

class BattleMessage(
    val text: String,
    val color: Color = Color.WHITE,
    val duration: Float = 5f,
    val fadeStart: Float = 2f,
    var x: Float = 0f,
    var y: Float = 0f
)
{
    private val layout = GlyphLayout()
    var lifetime: Float = duration

    fun update(delta: Float): Boolean
    {
        lifetime -= delta
        return lifetime <= 0
    }

    fun render(batch: SpriteBatch, font: BitmapFont, whitePixel: Texture) {
        val alpha = if (lifetime >= fadeStart) 1f else (lifetime / fadeStart).coerceIn(0f, 1f).pow(2)
        layout.setText(font, text)

        val textWidth = layout.width
        val textHeight = layout.height

        // Настраиваем внутренние отступы (padding) для рамки
        val paddingX = 15f
        val paddingY = 10f

        // Рассчитываем размеры и положение фона
        // Теперь фон центрируется относительно Y текста
        val bgWidth = textWidth + (paddingX * 2)
        val bgHeight = textHeight + (paddingY * 2)
        val bgX = x - paddingX
        val bgY = y - textHeight - paddingY // Опускаем фон ниже базовой линии текста

        val oldBatchColor = batch.color.cpy()

        // Рисуем фон
        batch.color = Color(0f, 0f, 0f, 0.7f * alpha)
        batch.draw(whitePixel, bgX, bgY, bgWidth, bgHeight)

        // Рисуем текст (он остается в точке x, y)
        batch.color = oldBatchColor
        val oldFontColor = font.color.cpy()
        font.color = Color(color.r, color.g, color.b, alpha)
        font.draw(batch, text, x, y)
        font.color = oldFontColor
    }
}
class BattleMessageSystem(
    private val font: BitmapFont,
    private val screenWidth: Float,
    private val screenHeight: Float,
    private val whitePixel: Texture
) {
    private val messages = mutableListOf<BattleMessage>()

    // --- НАСТРОЙКИ РАЗМЕРА ---
    private val maxMsg = 10          // Увеличили количество строк (было 6)
    private val fontScale = 1.67f    // Увеличиваем сам текст (1.0f — стандарт)
    private val space = 40f         // Увеличили отступ между строками (было 25f), чтобы текст не налезал друг на друга
    private val startX = 30f        // Чуть отодвинули от края
    private var startY = screenHeight * 0.45f // Подняли лог чуть выше, чтобы влезло больше строк
    // -------------------------

    fun addMessage(text: String, color: Color = Color.WHITE, duration: Float = 5f, fadeStart: Float = 2f) {
        messages.add(BattleMessage(text, color, duration, fadeStart))
        if (messages.size > maxMsg) {
            messages.removeAt(0)
        }
        updatePos()
    }

    private fun updatePos() {
        // Чтобы новые сообщения появлялись снизу, а старые уходили вверх:
        messages.forEachIndexed { index, message ->
            message.x = startX
            message.y = startY + (index * space)
        }
    }

    fun update(delta: Float) {
        val it = messages.iterator()
        while (it.hasNext()) {
            if (it.next().update(delta)) it.remove()
        }
    }

    fun render(batch: SpriteBatch) {
        val oldScaleX = font.data.scaleX
        val oldScaleY = font.data.scaleY

        // Применяем увеличенный масштаб перед отрисовкой всех сообщений
        font.data.setScale(fontScale)

        messages.forEach { it.render(batch, font, whitePixel) }

        // Возвращаем масштаб в исходное состояние, чтобы не испортить другие тексты
        font.data.setScale(oldScaleX, oldScaleY)
    }

    fun clear() {
        messages.clear()
    }
}
