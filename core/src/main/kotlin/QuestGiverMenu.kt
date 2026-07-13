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
    private val screenHeight: Int,
    private val dialogueManager: DialogueManager
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
    private val nextButton = Rectangle(
        (screenWidth - 200f) / 2,
        180f,
        200f,
        60f
    )
    private val choiceButtons = ArrayList<Rectangle>()

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
            val node = dialogueManager.currentNode
            if (node == null)
            {
                if (closeButton.contains(x, y)) {
                    hide()
                    return true
                }
                return false
            }
            val choices = node.choices
            if (choices != null && choices.size > 0) { // Используем .size вместо .isNotEmpty()
                for (i in 0 until choices.size) { // Итерируемся по размеру Array
                    if (i < choiceButtons.size && choiceButtons[i].contains(x, y)) {
                        dialogueManager.selectNode(choices[i].nextId)
                        return true
                    }
                }
            }
            else
            {
                if (nextButton.contains(x,y))
                {
                    dialogueManager.selectNode(node.nextId)
                    return true
                }
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
        val node = dialogueManager.currentNode
        batch.begin()

        // Заголовок
        font.color = Color.YELLOW
        val titleText = node?.speaker ?: "Quest giver"
        font.draw(batch, titleText, (screenWidth - 200f) / 2, screenHeight - 200f)

        // Информация о существе (статы)
        currentQuestGiver?.let { qg ->
            font.color = Color.WHITE
            font.draw(batch, qg.getStatsDescription(), (screenWidth - 300f) / 2, screenHeight - 280f)
        }

        if (node != null)
        {
            font.color = Color.WHITE
            font.draw(batch, node.text, 130f, screenHeight - 240f, screenWidth - 260f, com.badlogic.gdx.utils.Align.left, true)
            val choices = node.choices
            if (choices != null && choices.size > 0) {
                choiceButtons.clear()
                var startY = screenHeight - 400f

                for (i in 0 until choices.size) {
                    val choice = choices[i]
                    val rect = Rectangle(130f, startY, screenWidth - 260f, 45f)
                    choiceButtons.add(rect)

                    batch.setColor(0.2f, 0.2f, 0.2f, 0.6f)
                    batch.draw(whitePixel, rect.x, rect.y, rect.width, rect.height)
                    batch.setColor(Color.WHITE)

                    font.color = Color.CYAN
                    font.draw(batch, "${i + 1}. ${choice.text}", rect.x + 15f, rect.y + 30f)

                    startY -= 55f
                }
            }
            else
            {
                batch.draw(whitePixel, nextButton.x, nextButton.y, nextButton.width, nextButton.height)
                font.color = Color.BLACK
                font.draw(batch, "Next", nextButton.x + 65f, nextButton.y + 40f)
            }
        }
        else
        {
            font.color = Color.LIGHT_GRAY
            font.draw(batch, "Dialogue ended.", 130f, screenHeight - 240f)

            // Кнопка "Выход"
            batch.draw(whitePixel, closeButton.x, closeButton.y, closeButton.width, closeButton.height)
            font.color = Color.BLACK
            font.draw(batch, "Close UI", closeButton.x + 45f, closeButton.y + 40f)
        }

        batch.end()
    }
}
