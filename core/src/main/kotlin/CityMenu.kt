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
    private val windowHeight = 450f
    private var closeButtonRect = Rectangle()
    private var shopButtonRect = Rectangle()
    private val shopButtonTexture: Texture = Texture("menus/buttons/shop.png")
    private fun getWindowRect(): Rectangle
    {
        val x = (screenWidth - windowWidth) / 2
        val y = (screenHeight - windowHeight) / 2
        return Rectangle(x, y, windowWidth, windowHeight)
    }
    // метод обновления (всех)кнопок
    private fun updButtons()
    {
        // кнопка close
        val window = getWindowRect()
        val closeW = 120f
        val closeH = 50f
        val closeX = window.x + (window.width - closeW) / 2
        val closeY = window.y + 40f
        closeButtonRect.set(closeX, closeY, closeW, closeH)

        // кнопка shop
        val shopW = 200f
        val shopH = 60f
        val shopX = window.x + (window.width - shopW) / 2
        val shopY = window.y + window.height - 150f
        shopButtonRect.set(shopX, shopY, shopW, shopH)
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
        updButtons()
        if (Gdx.input.justTouched())
        {
            if (closeButtonRect.contains(touchX, yInverted))
            {
                hide()
                return true
            }
            if (shopButtonRect.contains(touchX, yInverted))
            {
                return true
            }
        }
        return false
    }
    fun isShopClicked(): Boolean
    {
        if (!isVisible) return false
        val touchX = Gdx.input.x.toFloat()
        val touchY = Gdx.input.y.toFloat()
        val yInverted = screenHeight - touchY
        updButtons()
        if (Gdx.input.justTouched() && shopButtonRect.contains(touchX, yInverted))
        {
            return true
        }
        return false
    }
    fun render(batch: SpriteBatch, shapeRenderer: ShapeRenderer)
    {
        if (!isVisible) return
        if (batch.isDrawing) batch.end()

        shapeRenderer.setAutoShapeType(true)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(0f,0f,0f,0.7f)
        shapeRenderer.rect(0f,0f,screenWidth,screenHeight)
        shapeRenderer.end()

        val window = getWindowRect()
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(0.2f,0.2f,0.2f,1f)
        shapeRenderer.rect(window.x, window.y, window.width, window.height)
        shapeRenderer.end()

        batch.begin()
        font.color = Color.WHITE
        font.data.setScale(1.5f)
        val textX = window.x + (window.width / 2) - 70f
        val textY = window.y + window.height - 40f
        font.draw(batch, "CITY", textX, textY)

        updButtons()

        batch.end()
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(1f,0.2f,0.2f,1f)
        shapeRenderer.rect(closeButtonRect.x, closeButtonRect.y, closeButtonRect.width, closeButtonRect.height)
        shapeRenderer.end()

        batch.begin()
        font.color = Color.WHITE
        font.data.setScale(1.3f)
        font.draw(batch, "EXIT", closeButtonRect.x + 28f, closeButtonRect.y + 28f)
        // SHOP BUTTON
        batch.draw(
            shopButtonTexture,
            shopButtonRect.x,
            shopButtonRect.y,
            shopButtonRect.width,
            shopButtonRect.height + 15f
        )

        font.data.setScale(1.0f)
        batch.color = Color.WHITE
    }
    fun dispose()
    {
        shopButtonTexture.dispose()
    }
}
