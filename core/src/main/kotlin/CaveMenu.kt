package ru.triplethall.rpgturnbased

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle

class CaveMenu(
    private val font: BitmapFont,
    private var screenHeight: Float,
    private var screenWidth: Float
) {
    var isVisible = false
        private set
    private val windowWidth = 600f
    private val windowHeight = 450f
    private var noButtonRect = Rectangle()
    private var yesButtonRect = Rectangle()

    private fun updateScreenSize() {
        screenWidth = Gdx.graphics.width.toFloat()
        screenHeight = Gdx.graphics.height.toFloat()
    }

    private fun getWindowRect(): Rectangle {
        updateScreenSize()
        val x = (screenWidth - windowWidth) / 2
        val y = (screenHeight - windowHeight) / 2
        return Rectangle(x, y, windowWidth, windowHeight)
    }
    private fun updateLayout() {
        val window = getWindowRect()
        val buttonHeight = 80f
        val buttonWidth = 120f
        val buttonSpacing = 40f
        val totalWidth = buttonWidth * 2 + buttonSpacing
        val startX = window.x + (window.width - totalWidth) / 2
        val buttonY = window.y + 50f

        yesButtonRect.set(startX, buttonY, buttonWidth, buttonHeight)
        noButtonRect.set(startX + buttonWidth + buttonSpacing, buttonY, buttonWidth, buttonHeight)
    }

    fun show() {
        isVisible = true
        updateLayout()
    }

    fun hide() {
        isVisible = false
    }

    fun handleInput(): Boolean {
        if (!isVisible) return false

        updateLayout()

        val touchX = Gdx.input.x.toFloat()
        val touchY = Gdx.input.y.toFloat()
        val yInverted = screenHeight - touchY

        if (Gdx.input.justTouched()) {
            if (noButtonRect.contains(touchX, yInverted)) {
                hide()
                return true
            }
            if (yesButtonRect.contains(touchX, yInverted)) {
                hide()
                return true
            }
        }
        return false
    }

    fun render(batch: SpriteBatch, shapeRenderer: ShapeRenderer) {
        if (!isVisible) return

        updateLayout()

        val window = getWindowRect()

        if (batch.isDrawing) batch.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1f)
        shapeRenderer.rect(window.x, window.y, window.width, window.height)
        shapeRenderer.setColor(0.8f, 0.2f, 0.2f, 1f)
        shapeRenderer.rect(noButtonRect.x, noButtonRect.y, noButtonRect.width, noButtonRect.height)
        shapeRenderer.setColor(0.2f, 0.8f, 0.2f, 1f)
        shapeRenderer.rect(yesButtonRect.x, yesButtonRect.y, yesButtonRect.width, yesButtonRect.height)
        shapeRenderer.end()

        batch.begin()
        val textX = window.x + 175f
        val textY = window.y + window.height - 40f
        font.data.setScale(1.5f)
        font.color = Color.WHITE
        font.draw(batch, "Do you want to proceed?", textX, textY)

        val yesTextX = yesButtonRect.x + (yesButtonRect.width - 50) / 2
        val yesTextY = yesButtonRect.y + (yesButtonRect.height + font.capHeight) / 2
        font.draw(batch, "YES", yesTextX, yesTextY)

        val noTextX = noButtonRect.x + (noButtonRect.width - 30) / 2
        val noTextY = noButtonRect.y + (noButtonRect.height + font.capHeight) / 2
        font.draw(batch, "NO", noTextX, noTextY)
    }
}
