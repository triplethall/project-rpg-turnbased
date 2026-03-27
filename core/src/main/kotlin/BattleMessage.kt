package ru.triplethall.rpgturnbased

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.Texture

class BattleMessage(
    val text: String,
    val color: Color = Color.WHITE,
    val duration: Float = 2f,
    var x: Float = 0f,
    var y: Float = 0f
)
{
    var lifetime: Float = duration

    fun update(delta: Float): Boolean
    {
        lifetime -= delta
        return lifetime <= 0
    }

    fun render(batch: SpriteBatch, font: BitmapFont, whitePixel: Texture)
    {
        val alpha = lifetime / duration
        val textWidth = font.getRegion().regionWidth.toFloat() * 0.6f
        val textHeight = font.getLineHeight()
        batch.color = Color(0f, 0f, 0f, 0.7f * alpha)
        batch.draw(whitePixel, x - 5f, y - textHeight + 5f, textWidth + 10f, textHeight + 5f)
        font.color = color.cpy().mul(0f,0f,0f,alpha)
        font.draw(batch, text, x, y)
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

    fun addMessage(text: String, color: Color = Color.WHITE, duration: Float = 2f)
    {
        messages.add(BattleMessage(text, color, duration))
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
