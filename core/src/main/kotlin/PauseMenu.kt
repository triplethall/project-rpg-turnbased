package ru.triplethall.rpgturnbased

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle

class PauseMenu(
    private val font: BitmapFont,
    private val screenWidth: Float,
    private val screenHeight: Float
) {
    var isVisible = false
        private set

    private val pauseButtonRect = Rectangle(0f, 0f, 120f, 120f)
    private var resumeRect = Rectangle()
    private var exitRect = Rectangle()

    fun toggle() {
        isVisible = !isVisible
    }


    fun handleInput(): Boolean {
        if (!isVisible) {
            if (Gdx.input.justTouched()) {
                val touchX = Gdx.input.x.toFloat()
                val touchY = Gdx.input.y.toFloat()

                if (pauseButtonRect.contains(touchX, touchY)) {
                    toggle()
                    return true
                }
            }
            return false
        }

        if (Gdx.input.justTouched()) {
            val touchX = Gdx.input.x.toFloat()
            val yInverted = screenHeight - Gdx.input.y.toFloat()

            if (resumeRect.contains(touchX, yInverted)) {
                toggle()
                return true
            }
            if (exitRect.contains(touchX, yInverted)) {
                Gdx.app.exit()
                return true
            }
        }
        return false
    }

    fun render(batch: SpriteBatch, whitePixel: Texture) {
        batch.color = Color.BLACK
        batch.draw(whitePixel, pauseButtonRect.x, pauseButtonRect.y, pauseButtonRect.width, pauseButtonRect.height)

        font.color = Color.WHITE
        font.data.setScale(2f)
        font.draw(batch, "||", pauseButtonRect.x + 12f, pauseButtonRect.y + 28f)
        font.data.setScale(1f)

        if (!isVisible) return

        batch.color = Color(0f, 0f, 0f, 0.7f)
        batch.draw(whitePixel, 0f, 0f, screenWidth, screenHeight)

        val panelW = 900f
        val panelH = 600f
        val pX = (screenWidth - panelW) / 2
        val pY = (screenHeight - panelH) / 2

        batch.color = Color.DARK_GRAY
        batch.draw(whitePixel, pX, pY, panelW, panelH)

        val btnW = 600f
        val btnH = 120f
        val cX = pX + (panelW - btnW) / 2

        resumeRect.set(cX, pY + 120f, btnW, btnH)
        exitRect.set(cX, pY + 60f, btnW, btnH)

        batch.color = Color.GRAY
        batch.draw(whitePixel, resumeRect.x, resumeRect.y, resumeRect.width, resumeRect.height)
        batch.draw(whitePixel, exitRect.x, exitRect.y, exitRect.width, exitRect.height)


        font.color = Color.WHITE
        font.data.setScale(1.2f)
        font.draw(batch, "CONTINUE", cX + 30f, resumeRect.y + 28f)
        font.draw(batch, "EXIT", cX + 70f, exitRect.y + 28f)
        font.data.setScale(1f)
        batch.color = Color.WHITE
    }
}
