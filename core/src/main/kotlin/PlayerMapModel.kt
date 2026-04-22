package ru.triplethall.rpgturnbased

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import kotlin.math.abs
import kotlin.math.sin

class PlayerMapModel(
    private val bodyDown: TextureRegion,
    private val bodyUp: TextureRegion,
    private val bodyLeft: TextureRegion,
    private val bodyRight: TextureRegion,
    private val legRegion: TextureRegion,
    private val armorRegion: TextureRegion? = null
) {
    enum class State { IDLE, MOVING }
    private fun getBodyRegion(direction: Int): TextureRegion {
        return when (direction) {
            1 -> bodyUp      // UP
            2 -> bodyDown    // DOWN
            3 -> bodyLeft    // LEFT
            4 -> bodyRight   // RIGHT
            else -> bodyDown
        }
    }
    fun render(
        batch: SpriteBatch,
        player: Player,
        x: Float,
        y: Float,
        direction: Int, // 1=UP, 2=DOWN, 3=LEFT, 4=RIGHT
        state: State,
        stateTime: Float
    ) {
        val currentBody = getBodyRegion(direction)
        // Увеличиваем скорость анимации в 3 раза → 3 шага за проход клетки
        val baseSpeed = 9f
        val speed = if (state == State.MOVING) baseSpeed * 3f else baseSpeed
        val t = stateTime * speed
        val wave = sin(t)
        val isVertical = direction == 1 || direction == 2

        // Размеры (под твои ассеты)
        val bodyW = 14f; val bodyH = 20f
        val legW = 6f;   val legH = 17f

        // Точка крепления ног (таз)
        val hipX = x + bodyW / 2f + 5f
        val hipY = y + bodyH * 0.65f + 10f

        // === 1. Ноги ===
        val legOriginY = legH // вращение от верха (таз)
        val legdelta: Float
        if (direction == 1 || direction == 2) {
            legdelta = 0f
        } else {
            legdelta = 1f
        }
        // Левая нога
        val leftRot = if (!isVertical) wave * 30f else 0f
        val leftScaleY = if (isVertical) 1f + wave * 0.10f else 1f
        batch.draw(
            legRegion,
            hipX - legW/2f + 2.5f + legdelta, hipY - legH+ 2f,  // позиция
            legW/2f, legOriginY,                // origin (точка вращения)
            legW - 1.0f, legH+2f,                         // размер
            1f, leftScaleY,                     // scale
            leftRot                             // вращение
        )

        // Правая нога (ПРОТИВОФАЗА: -wave)


        val rightRot = if (!isVertical) -wave * 30f else 0f
        val rightScaleY = if (isVertical) 1f - wave * 0.15f else 1f // тоже в противофазе
        batch.draw(
            legRegion,
            hipX + legW/2f + 1.5f - legdelta, hipY - legH+2f,
            legW/2f, legOriginY,
            legW - 1.0f, legH+2f,
            1f, rightScaleY,
            rightRot
        )

        // === 2. Тело (подпрыгивание при ходьбе) ===
        val bob = if (state == State.MOVING) abs(wave) * 3f else 0f
        batch.draw(currentBody, x + 5.3f, y + bob + 21f, bodyW*1.7f, bodyH*1.5f)

        // === 3. Броня ===
        if (armorRegion != null) {
            batch.draw(armorRegion, x + 6f, y + bob + 24f, bodyW + 4f, bodyH * 0.6f)
        }
    }
}
