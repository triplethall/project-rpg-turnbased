package ru.triplethall.rpgturnbased

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3

class MainMenu(
    private val font: BitmapFont,
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val game: RPGTurnbased
) {
    private lateinit var newGame: Texture
    private lateinit var exitGame: Texture
    private var visible = true
    private var titleFont: BitmapFont = BitmapFont()
    private val titleScale = 2.5f
    private val buttonScale = 1.8f

    // Кнопки
    private data class Button(val rect: Rectangle, val texture: Texture, val action: () -> Unit)
    private val buttons = mutableListOf<Button>()

    // Камера для UI
    private val uiCamera = OrthographicCamera().apply {
        setToOrtho(false, screenWidth.toFloat(), screenHeight.toFloat())
    }

    init {
        titleFont.color = Color.GOLD
        titleFont.data.setScale(titleScale)
        loadTextures()
        initButtons()
    }
    private fun loadTextures()
    {
        newGame = Texture("menus/buttons/newgame.png")
        exitGame = Texture("menus/buttons/exit.png")
    }
    private fun initButtons() {
        val buttonWidth = 250f
        val buttonHeight = 100f
        val centerX = screenWidth / 2f - buttonWidth / 2f
        val startY = screenHeight / 2f

        // Кнопка "Новая игра"
        buttons.add(
            Button(
                rect = Rectangle(centerX, startY + 50f, buttonWidth, buttonHeight),
                texture = newGame,
                action = { startGame() }
            )
        )

        // Кнопка "Выход"
        buttons.add(
            Button(
                rect = Rectangle(centerX, startY - 60f, buttonWidth, buttonHeight),
                texture = exitGame,
                action = { exitGame() }
            )
        )
    }

    private fun startGame() {
        visible = false
        game.startGame()
    }

    private fun exitGame() {
        Gdx.app.exit()
    }

    fun handleInput(): Boolean {
        if (!visible) return false

        if (Gdx.input.justTouched()) {
            val touchPos = Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
            uiCamera.unproject(touchPos)

            buttons.forEach { button ->
                if (button.rect.contains(touchPos.x, touchPos.y)) {
                    button.action.invoke()
                    return true
                }
            }
        }
        return false
    }

    fun render(batch: SpriteBatch, shapeRenderer: ShapeRenderer) {
        if (!visible) return

        batch.projectionMatrix = uiCamera.combined
        batch.begin()

        // Заголовок игры
        val titleText = "RPG GAME"
        titleFont.draw(batch,
            titleText,
            screenWidth / 2.2f,
            screenHeight - 150f)

        // кнопочкиии
        buttons.forEach { button ->
            batch.draw(

                button.texture,
                button.rect.x,
                button.rect.y,
                button.rect.width,
                button.rect.height
            )
        }
        batch.end()
    }

    fun isVisible() = visible

    fun hide() {
        visible = false
    }

    fun show() {
        visible = true
    }

    fun dispose() {
        titleFont.dispose()
    }
}
