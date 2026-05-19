package ru.triplethall.rpgturnbased

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle

class QuestGiverMenu(
    private val font: BitmapFont,
    private val screenWidth: Int,
    private val screenHeight: Int
) {
    private var visible = false
    private var currentQuestGiver: QuestGiver? = null

    // Область кнопки "Выход"
    private val closeButton = Rectangle(
        (screenWidth - 200f) / 2,
        100f,
        200f,
        60f
    )

    fun show(questGiver: QuestGiver) {
        currentQuestGiver = questGiver
        visible = true
    }

    fun hide() {
        visible = false
        currentQuestGiver = null
    }

    fun isVisible(): Boolean = visible

    fun handleInput(): Boolean {
        if (!visible) return false
        if (Gdx.input.justTouched()) {
            val x = Gdx.input.x.toFloat()
            val y = (screenHeight - Gdx.input.y).toFloat()
            if (closeButton.contains(x, y)) {
                hide()
                return true
            }
        }
        return false
    }

    fun render(batch: SpriteBatch, shapeRenderer: ShapeRenderer, whitePixel: com.badlogic.gdx.graphics.Texture) {
        if (!visible) return
        if (batch.isDrawing) batch.end()
        // Полупрозрачный фон
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(0f, 0f, 0f, 0.85f)
        shapeRenderer.rect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat())
        shapeRenderer.end()

        // Белая рамка
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        shapeRenderer.setColor(Color.WHITE)
        shapeRenderer.rect(100f, 150f, screenWidth - 200f, screenHeight - 300f)
        shapeRenderer.end()

        batch.begin()

        // Заголовок
        font.color = Color.YELLOW
        font.draw(batch, "Quest giver", (screenWidth - 200f) / 2, screenHeight - 200f)

        // Информация о существе (статы)
        currentQuestGiver?.let { qg ->
            font.color = Color.WHITE
            font.draw(batch, qg.getStatsDescription(), (screenWidth - 300f) / 2, screenHeight - 280f)
            font.color = Color.LIGHT_GRAY
            font.draw(batch, "I don't have any assignments yet....", (screenWidth - 220f) / 2, screenHeight - 360f)
        }

        // Кнопка "Выход"
        batch.draw(whitePixel, closeButton.x, closeButton.y, closeButton.width, closeButton.height)
        font.color = Color.BLACK
        font.draw(batch, "Exit", closeButton.x + 70f, closeButton.y + 40f)

        batch.end()
    }
}
