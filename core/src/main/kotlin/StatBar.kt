// StatBar.kt
package ru.triplethall.rpgturnbased

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch

/**
 * Класс для отрисовки полоски здоровья/маны.
 * @param x координата левого нижнего угла
 * @param y координата левого нижнего угла
 * @param width ширина полоски
 * @param height высота полоски
 * @param color цвет заполнения
 */
class StatBar(
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float,
    val color: Color
) {
    fun render(batch: SpriteBatch, whitePixel: Texture, current: Int, max: Int) {
        val percent = current.toFloat() / max.toFloat()
        val filledWidth = width * percent

        // Фон
        batch.color = Color.DARK_GRAY
        batch.draw(whitePixel, x, y, width, height)

        // Заполненная часть
        batch.color = color
        batch.draw(whitePixel, x, y, filledWidth, height)

        // Сброс цвета
        batch.color = Color.WHITE
    }
}
