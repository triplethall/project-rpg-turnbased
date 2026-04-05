package ru.triplethall.rpgturnbased

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle

class CityMenu(
    private val font: BitmapFont,
    private val screenWidth: Float,
    private val screenHeight: Float,
)
{
    var isVisible = false
        private set

    private val windowWidth = 600f
    private val windowHeight = 400f
    private var closeButtonRect = Rectangle()
    private fun getWindowRect(): Rectangle
    {
        val x = (screenWidth - windowWidth) / 2
        val y = (screenHeight - windowHeight) / 2
        return Rectangle(x, y, windowWidth, windowHeight)
    }
    private fun updCloseBtn()
    {
        val window = getWindowRect()
        val closeW = 120f
        val closeH = 50f
        val closeX = window.x + (window.width - closeW) / 2
        val closeY = window.y + 40f
        closeButtonRect.set(closeX, closeY, closeW, closeH)
    }
    fun show()
    {
        isVisible = true
    }
    fun hide()
    {
        isVisible = false
    }
    fun handleInput(): Boolean
    {
        if (!isVisible) return false
        val touchX = Gdx.input.x.toFloat()
        val touchY = Gdx.input.y.toFloat()
        val yInverted = screenHeight - touchY
        updCloseBtn()
        if (Gdx.input.justTouched() && closeButtonRect.contains(touchX, yInverted))
        {
            hide()
            return true
        }
        return false
    }
    fun render(batch: SpriteBatch, shapeRenderer: ShapeRenderer)
    {
        if (!isVisible) return
        batch.end()

        shapeRenderer.setAutoShapeType(true)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(0f,0f,0f,0.7f)
        shapeRenderer.rect(0f,0f,screenWidth,screenHeight)
        shapeRenderer.end()

        val window = getWindowRect()
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(0.2f,0.2f,0.2f,1f)
        shapeRenderer.rect(window.x, window.y, window.width, window.height)

        shapeRenderer.set(ShapeRenderer.ShapeType.Line)
        shapeRenderer.setColor(1f,0.5f,0f,1f)
        shapeRenderer.rect(window.x,window.y,window.width,window.height)
        shapeRenderer.end()
        batch.begin()
        font.color = Color.GOLD
        font.data.setScale(1.5f)
        font.draw(batch, "CITY MENU", window.x + 30f, window.y + window.height - 40f)
        updCloseBtn()
        batch.end()
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(1f,0.2f,0.2f,1f)
        shapeRenderer.rect(closeButtonRect.x, closeButtonRect.y, closeButtonRect.width, closeButtonRect.height)
        shapeRenderer.end()
        batch.begin()
        font.color = Color.WHITE
        font.data.setScale(1.3f)
        font.draw(batch, "EXIT", closeButtonRect.x + 28f, closeButtonRect.y + 28f)
        font.data.setScale(1.0f)
        batch.color = Color.WHITE
    }
}
