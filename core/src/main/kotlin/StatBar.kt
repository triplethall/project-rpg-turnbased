package ru.triplethall.rpgturnbased

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch

class StatBar(
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float,
    val color: Color
) {
    fun render(batch: SpriteBatch, frameTexture: Texture, whitePixel: Texture, current: Int, max: Int) {
        batch.color = Color.WHITE
        batch.draw(frameTexture, x, y, width, height)

        val paddingX = width * 0.165f
        val paddingY = height * 0.35f
        val innerWidth = width - (paddingX * 2)
        val innerHeight = height - (paddingY * 2)

        val percent = Math.max(0f, current.toFloat() / max.toFloat())
        val filledWidth = innerWidth * percent

        batch.color = Color.BLACK
        batch.draw(whitePixel, x + paddingX, y + paddingY, innerWidth, innerHeight)

        batch.color = color
        batch.draw(whitePixel, x + paddingX, y + paddingY, filledWidth, innerHeight)

        batch.color = Color(0f, 0f, 0f, 0.3f)
        batch.draw(whitePixel, x + paddingX, y + paddingY, filledWidth, innerHeight * 0.4f)

        batch.color = Color(1f, 1f, 1f, 0.2f)
        batch.draw(whitePixel, x + paddingX, y + paddingY + (innerHeight * 0.7f), filledWidth, innerHeight * 0.15f)

        batch.color = Color.WHITE
    }
}
