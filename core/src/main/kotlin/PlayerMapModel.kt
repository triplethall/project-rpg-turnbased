package ru.triplethall.rpgturnbased

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import kotlin.math.abs
import kotlin.math.sin

class PlayerMapModel(
    private val bodyRegion: TextureRegion,
    private val legRegion: TextureRegion,
    private val armorRegion: TextureRegion? = null
) {
    enum class State { IDLE, MOVING }

    fun render(
        batch: SpriteBatch,
        player: Player,
        x: Float,
        y: Float,
        direction: Int, // 1=UP, 2=DOWN, 3=LEFT, 4=RIGHT
        state: State,
        stateTime: Float
    ) {
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

        // Левая нога
        val leftRot = if (!isVertical) wave * 30f else 0f
        val leftScaleY = if (isVertical) 1f + wave * 0.10f else 1f
        batch.draw(
            legRegion,
            hipX - legW/2f - 1f, hipY - legH,  // позиция
            legW/2f, legOriginY,                // origin (точка вращения)
            legW, legH,                         // размер
            1f, leftScaleY,                     // scale
            leftRot                             // вращение
        )

        // Правая нога (ПРОТИВОФАЗА: -wave)
        val rightRot = if (!isVertical) -wave * 30f else 0f
        val rightScaleY = if (isVertical) 1f - wave * 0.15f else 1f // тоже в противофазе
        batch.draw(
            legRegion,
            hipX + legW/2f + 1f, hipY - legH,
            legW/2f, legOriginY,
            legW, legH,
            1f, rightScaleY,
            rightRot
        )

        // === 2. Тело (подпрыгивание при ходьбе) ===
        val bob = if (state == State.MOVING) abs(wave) * 3f else 0f
        batch.draw(bodyRegion, x + 8f, y + bob + 20f, bodyW, bodyH)

        // === 3. Броня ===
        if (armorRegion != null) {
            batch.draw(armorRegion, x + 6f, y + bob + 24f, bodyW + 4f, bodyH * 0.6f)
        }
    }
}
