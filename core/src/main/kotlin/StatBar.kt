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
//class StatBar(
//    var x: Float,
//    var y: Float,
//    val width: Float,
//    val height: Float,
//    val color: Color
//) {
//    fun render(batch: SpriteBatch, whitePixel: Texture, current: Int, max: Int) {
//        val percent = current.toFloat() / max.toFloat()
//        val filledWidth = width * percent
//
//        // Фон
//        batch.color = Color.DARK_GRAY
//        batch.draw(whitePixel, x, y, width, height)
//
//        // Заполненная часть
//        batch.color = color
//        batch.draw(whitePixel, x, y, filledWidth, height)
//
//        // Сброс цвета
//        batch.color = Color.WHITE
//    }
//}

class StatBar(
    var x: Float,
    var y: Float,
    val width: Float,   // Общая ширина рамки
    val height: Float,  // Общая высота рамки
    val color: Color
) {
    fun render(batch: SpriteBatch, frameTexture: Texture, whitePixel: Texture, current: Int, max: Int) {
        // 1. Рисуем каменную рамку
        batch.color = Color.WHITE
        batch.draw(frameTexture, x, y, width, height)

        // 2. Рассчитываем внутренние отступы (подберите значения под ваш спрайт)
        // Если ширина рамки 100, то отступ слева примерно 10% (10f)
        val paddingX = width * 0.165f
        val paddingY = height * 0.35f
        val innerWidth = width - (paddingX * 2)
        val innerHeight = height - (paddingY * 2)

        val percent = Math.max(0f, current.toFloat() / max.toFloat())
        val filledWidth = innerWidth * percent

        // 3. Рисуем фон полоски (внутри рамки)
        batch.color = Color.BLACK
        batch.draw(whitePixel, x + paddingX, y + paddingY, innerWidth, innerHeight)

        // 4. Рисуем заполнение
        batch.color = color
        batch.draw(whitePixel, x + paddingX, y + paddingY, filledWidth, innerHeight)

        // 4. Нижняя тень (затемняем нижнюю половину полоски)
        batch.color = Color(0f, 0f, 0f, 0.3f) // Черный с прозрачностью 30%
        batch.draw(whitePixel, x + paddingX, y + paddingY, filledWidth, innerHeight * 0.4f)

        // 5. Верхний блик (осветляем верхнюю часть для глянца)
        batch.color = Color(1f, 1f, 1f, 0.2f) // Белый с прозрачностью 20%
        batch.draw(whitePixel, x + paddingX, y + paddingY + (innerHeight * 0.7f), filledWidth, innerHeight * 0.15f)

        batch.color = Color.WHITE
    }
}
