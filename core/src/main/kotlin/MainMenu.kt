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
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val game: RPGTurnbased
) {
    private lateinit var newGame: Texture
    private lateinit var exitBtn: Texture
    private lateinit var optionsMenu: Texture
    private lateinit var creditsMenu: Texture
    private lateinit var closeButton: Texture
    private var optionsVisible = false
    private var creditsVisible = false
    private var visible = true
    private var titleFont: BitmapFont = BitmapFont()
    private val titleScale = 2.5f
    private var textFont: BitmapFont = BitmapFont()

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
        textFont.color = Color.WHITE
        textFont.data.setScale(1.5f)
        loadTextures()
        initButtons()
    }
    private fun loadTextures()
    {
        newGame = Texture("menus/buttons/newgame.png")
        exitBtn = Texture("menus/buttons/exit.png")
        optionsMenu = Texture("menus/buttons/options.png")
        creditsMenu = Texture("menus/buttons/credits.png")
        closeButton = Texture("menus/buttons/back.png")


    }
    private fun initButtons() {
        val buttonWidth = 250f
        val buttonHeight = 100f
        val centerX = screenWidth / 2f - buttonWidth / 2f
        val startY = screenHeight / 2f
        val spacing = 120f

        // Кнопка "Новая игра"
        buttons.add(
            Button(
                rect = Rectangle(centerX, startY + spacing, buttonWidth, buttonHeight),
                texture = newGame,
                action = { startGame() }
            )
        )

        // Кнопка "Настройки"
        buttons.add(
            Button(
                rect = Rectangle(centerX, startY, buttonWidth, buttonHeight),
                texture = optionsMenu,
                action = { openSettings() }
            )
        )
        // Кнопка "Кредиты"
        buttons.add(
            Button(
                rect = Rectangle(centerX, startY - spacing, buttonWidth, buttonHeight),
                texture = creditsMenu,
                action = { openCredits() }
            )
        )
        // Кнопка "Выход"
        buttons.add(
            Button(
                rect = Rectangle(centerX, startY - spacing * 2, buttonWidth, buttonHeight),
                texture = exitBtn,
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
    private fun openSettings() {
        optionsVisible = true
    }

    private fun openCredits() {
        creditsVisible = true
    }

    private fun closeSettings() {
        optionsVisible = false
    }

    private fun closeCredits() {
        creditsVisible = false
    }

    fun handleInput(): Boolean {
        if (optionsVisible) return handleOptionsInput()
        if (creditsVisible) return handleCreditsInput()
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

    private fun getCloseRect(menuRect: Rectangle = getMenuRect()): Rectangle {
        val buttonWidth = 70f
        val buttonHeight = 70f
        val x = menuRect.x + menuRect.width - buttonWidth - 20f  // правый верхний угол
        val y = menuRect.y + menuRect.height - buttonHeight - 20f
        return Rectangle(x, y, buttonWidth, buttonHeight)
    }

    private fun getMenuRect(): Rectangle {
        val menuWidth = 600f
        val menuHeight = 400f
        val x = screenWidth / 2f - menuWidth / 2f
        val y = screenHeight / 2f - menuHeight / 2f
        return Rectangle(x, y, menuWidth, menuHeight)
    }

    private fun handleOptionsInput(): Boolean
    {
        if (Gdx.input.justTouched())
        {
            val touchPos = Vector3(Gdx.input.x.toFloat(),Gdx.input.y.toFloat(),0f)
            uiCamera.unproject(touchPos)

            val closeRect = getCloseRect()
            if (closeRect.contains(touchPos.x, touchPos.y))
            {
                closeSettings()
                return true
            }
        }
        return false
    }

    private fun handleCreditsInput(): Boolean
    {
        if (Gdx.input.justTouched())
        {
            val touchPos = Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
            uiCamera.unproject(touchPos)
            val closeRect = getCloseRect()
            if (closeRect.contains(touchPos.x, touchPos.y))
            {
                closeCredits()
                return true
            }
        }
        return false
    }
    fun render(batch: SpriteBatch, shapeRenderer: ShapeRenderer) {
        if (!visible && !optionsVisible && !creditsVisible) return

        batch.projectionMatrix = uiCamera.combined
        if (visible)
        {
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
        if (optionsVisible) {
            renderOptionsMenu(batch, shapeRenderer)
        }

        if (creditsVisible) {
            renderCreditsMenu(batch, shapeRenderer)
        }


    }
    private fun renderOptionsMenu(batch: SpriteBatch, shapeRenderer: ShapeRenderer) {
        val menuRect = getMenuRect()

        // полупрозрачный фон
        shapeRenderer.projectionMatrix = uiCamera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(Color(0f, 0f, 0f, 0.5f))
        shapeRenderer.rect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat())
        shapeRenderer.end()

        // фон меню
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(Color(0.2f, 0.2f, 0.2f, 1f))
        shapeRenderer.rect(menuRect.x, menuRect.y, menuRect.width, menuRect.height)
        shapeRenderer.end()

        batch.begin()

        // заголовок
        val subTitleFont = BitmapFont()
        subTitleFont.color = Color.WHITE
        subTitleFont.data.setScale(2f)
        subTitleFont.draw(batch, "OPTIONS",
            menuRect.x + menuRect.width / 2f - 50f,
            menuRect.y + menuRect.height - 30f)

        // TODO: ADD OPTION TO DISABLE/ENABLE AUDIO

        // Кнопка закрытия
        val closeRect = getCloseRect()
        batch.draw(closeButton, closeRect.x, closeRect.y, closeRect.width, closeRect.height)

        batch.end()
    }
    private fun renderCreditsMenu(batch: SpriteBatch, shapeRenderer: ShapeRenderer) {
        val menuRect = getMenuRect()

        // полупрозрачный фон
        shapeRenderer.projectionMatrix = uiCamera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(Color(0f, 0f, 0f, 0.5f))
        shapeRenderer.rect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat())
        shapeRenderer.end()

        // фон меню
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(Color(0.2f, 0.2f, 0.2f, 1f))
        shapeRenderer.rect(menuRect.x, menuRect.y, menuRect.width, menuRect.height)
        shapeRenderer.end()

        batch.begin()

        // заголовок
        val subTitleFont = BitmapFont()
        subTitleFont.color = Color.WHITE
        subTitleFont.data.setScale(2f)
        subTitleFont.draw(batch, "CREDITS",
            menuRect.x + menuRect.width / 2f - 50f,
            menuRect.y + menuRect.height - 30f)

        textFont.color = Color.WHITE
        textFont.data.setScale(1.5f)

        var yOffset = menuRect.y + menuRect.height - 90f
        textFont.color = Color.GOLD
        textFont.data.setScale(1.7f)
        textFont.draw(batch,
            "Main developer:",
            menuRect.x + 50f,
            yOffset)
        yOffset -= 35f

        textFont.color = Color.WHITE
        textFont.data.setScale(1.5f)

        textFont.draw(batch,
            "  triplethall",
            menuRect.x + 50f,
            yOffset)
        yOffset -= 45f

        textFont.color = Color.GOLD
        textFont.data.setScale(1.7f)

        textFont.draw(batch,
            "Contributors",
            menuRect.x + 50f,
            yOffset)
        yOffset -= 35f

        textFont.color = Color.WHITE
        textFont.data.setScale(1.5f)

        val contributors = listOf(
            "kamyshek1232",
            "DIAMOND00732",
            "EliteTea",
            "MishaBashkirov",
            "R4ckstarMade"
        )
        for (c in contributors)
        {
            textFont.draw(batch,
                c,
                menuRect.x + 50f,
                yOffset
            )
            yOffset -= 35f
        }
        // Кнопка закрытия
        val closeRect = getCloseRect()
        batch.draw(closeButton, closeRect.x, closeRect.y, closeRect.width, closeRect.height)

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
        if (::newGame.isInitialized) newGame.dispose()
        if (::exitBtn.isInitialized) exitBtn.dispose()
        if (::optionsMenu.isInitialized) optionsMenu.dispose()
        if (::creditsMenu.isInitialized) creditsMenu.dispose()
        if (::closeButton.isInitialized) closeButton.dispose()
    }
}
