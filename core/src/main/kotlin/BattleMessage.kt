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

    fun render(batch: SpriteBatch, font: BitmapFont, whitePixel: Texture)
    {
        val alpha = if (lifetime >= fadeStart) 1f else (lifetime / fadeStart).coerceIn(0f, 1f).pow(2)
        layout.setText(font, text)
        val textWidth = layout.width
        val textHeight = layout.height
        val oldBatch = batch.color.cpy()
        batch.color = Color(0f, 0f, 0f, 0.7f * alpha)
        batch.draw(whitePixel, x - 5f, y - textHeight + 5f, textWidth + 10f, textHeight + 5f)
        batch.color = oldBatch
        val oldColor = font.color.cpy()
        font.color = Color(color.r, color.g, color.b, alpha)
        font.draw(batch, text, x, y)
        font.color = oldColor
    }
}
class BattleMessageSystem(
    private val font: BitmapFont,
    private val screenWidth: Float,
    private val screenHeight: Float,
    private val whitePixel: Texture
)
{
    private val messages = mutableListOf<BattleMessage>()
    private val maxMsg = 6
    private val space = 25f
    private val startX = 20f
    private var startY = screenHeight * 0.35f

    fun addMessage(text: String, color: Color = Color.WHITE, duration: Float = 5f, fadeStart: Float = 2f)
    {
        messages.add(BattleMessage(text, color, duration, fadeStart))
        while (messages.size > maxMsg)
        {
            messages.removeAt(0)
        }
        updatePos()
    }
    private fun updatePos()
    {
        val startYpos = startY - (messages.size - 1) * space
        messages.forEachIndexed { index, message ->
            message.x = startX
            message.y = startYpos + index * space
        }
    }
    fun update(delta: Float)
    {
        val it = messages.iterator()
        while (it.hasNext()) {
            val msg = it.next()
            if (msg.update(delta)) {
                it.remove()
            }
        }
        updatePos()
    }
    fun render(batch: SpriteBatch)
    {
        messages.forEach {it.render(batch,font,whitePixel)}
    }
    fun clear()
    {
        messages.clear()
    }
}
