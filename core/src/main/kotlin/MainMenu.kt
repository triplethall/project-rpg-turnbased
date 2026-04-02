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
import ru.triplethall.rpgturnbased.SoundManager

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
    // Удаляем старую переменную audioEnabled, больше не нужна
    // private var audioEnabled = true
    private var titleFont: BitmapFont = BitmapFont()
    private val titleScale = 2.5f
    private var textFont: BitmapFont = BitmapFont()

    // Кнопки
    private data class Button(val rect: Rectangle, val texture: Texture, val action: () -> Unit)
    private val buttons = mutableListOf<Button>()

    // ДОБАВЛЕНО: прямоугольники для регулировки громкости
    private var musicDownRect = Rectangle()
    private var musicUpRect = Rectangle()
    private var soundDownRect = Rectangle()
    private var soundUpRect = Rectangle()
    private var closeSettingsRect = Rectangle()

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

    private fun loadTextures() {
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

        buttons.add(
            Button(
                rect = Rectangle(centerX, startY + spacing, buttonWidth, buttonHeight),
                texture = newGame,
                action = { startGame() }
            )
        )
        buttons.add(
            Button(
                rect = Rectangle(centerX, startY, buttonWidth, buttonHeight),
                texture = optionsMenu,
                action = { openSettings() }
            )
        )
        buttons.add(
            Button(
                rect = Rectangle(centerX, startY - spacing, buttonWidth, buttonHeight),
                texture = creditsMenu,
                action = { openCredits() }
            )
        )
        buttons.add(
            Button(
                rect = Rectangle(centerX, startY - spacing * 2, buttonWidth, buttonHeight),
                texture = exitBtn,
                action = { exitGame() }
            )
        )
    }

    // ИНИЦИАЛИЗАЦИЯ ПРЯМОУГОЛЬНИКОВ ДЛЯ НАСТРОЕК (исправлено)
    private fun initSettingsRects() {
        val menuRect = getMenuRect()
        val buttonSize = 60f          // размер кнопок +/-
        val labelX = menuRect.x + 50f // отступ слева для текста
        val valueX = menuRect.x + 250f // место для отображения процентов
        var y = menuRect.y + 280f      // вертикальная позиция для музыки

        // Музыка: кнопка "-" и "+"
        musicDownRect = Rectangle(labelX + 100f, y, buttonSize, buttonSize)
        musicUpRect = Rectangle(labelX + 180f, y, buttonSize, buttonSize)

        y -= 80f // позиция для звуков
        soundDownRect = Rectangle(labelX + 100f, y, buttonSize, buttonSize)
        soundUpRect = Rectangle(labelX + 180f, y, buttonSize, buttonSize)

        // Кнопка закрытия
        closeSettingsRect = getCloseRect()
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
        initSettingsRects() // теперь правильно инициализирует все кнопки
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

    // Удалён старый метод toggleAudio()

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
        val x = menuRect.x + menuRect.width - buttonWidth - 20f
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

    // ОБРАБОТКА НАСТРОЕК (добавлены кнопки + и -)
    private fun handleOptionsInput(): Boolean {
        if (Gdx.input.justTouched()) {
            val touchPos = Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
            uiCamera.unproject(touchPos)

            // Кнопка "Музыка -"
            if (musicDownRect.contains(touchPos.x, touchPos.y)) {
                SoundManager.musicVolume -= 0.1f
                return true
            }
            // Кнопка "Музыка +"
            if (musicUpRect.contains(touchPos.x, touchPos.y)) {
                SoundManager.musicVolume += 0.1f
                return true
            }
            // Кнопка "Звуки -"
            if (soundDownRect.contains(touchPos.x, touchPos.y)) {
                SoundManager.soundVolume -= 0.1f
                return true
            }
            // Кнопка "Звуки +"
            if (soundUpRect.contains(touchPos.x, touchPos.y)) {
                SoundManager.soundVolume += 0.1f
                return true
            }
            // Кнопка закрытия
            if (closeSettingsRect.contains(touchPos.x, touchPos.y)) {
                closeSettings()
                return true
            }
        }
        return false
    }

    private fun handleCreditsInput(): Boolean {
        if (Gdx.input.justTouched()) {
            val touchPos = Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
            uiCamera.unproject(touchPos)
            val closeRect = getCloseRect()
            if (closeRect.contains(touchPos.x, touchPos.y)) {
                closeCredits()
                return true
            }
        }
        return false
    }

    fun render(batch: SpriteBatch, shapeRenderer: ShapeRenderer) {
        if (!visible && !optionsVisible && !creditsVisible) return

        batch.projectionMatrix = uiCamera.combined
        if (visible) {
            batch.begin()
            val titleText = "RPG GAME"
            titleFont.draw(batch, titleText, screenWidth / 2.2f, screenHeight - 150f)
            buttons.forEach { button ->
                batch.draw(button.texture, button.rect.x, button.rect.y, button.rect.width, button.rect.height)
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

    // ОТРИСОВКА МЕНЮ НАСТРОЕК (добавлены ползунки громкости)
    private fun renderOptionsMenu(batch: SpriteBatch, shapeRenderer: ShapeRenderer) {
        val menuRect = getMenuRect()

        // Полупрозрачный фон
        shapeRenderer.projectionMatrix = uiCamera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(Color(0f, 0f, 0f, 0.5f))
        shapeRenderer.rect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat())
        shapeRenderer.end()

        // Фон меню
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(Color(0.2f, 0.2f, 0.2f, 1f))
        shapeRenderer.rect(menuRect.x, menuRect.y, menuRect.width, menuRect.height)
        shapeRenderer.end()

        batch.begin()

        // Заголовок
        val subTitleFont = BitmapFont()
        subTitleFont.color = Color.WHITE
        subTitleFont.data.setScale(2f)
        subTitleFont.draw(batch, "OPTIONS",
            menuRect.x + menuRect.width / 2f - 50f,
            menuRect.y + menuRect.height - 30f)

        // Текст "Music volume"
        textFont.color = Color.WHITE
        textFont.data.setScale(1.2f)
        textFont.draw(batch, "Music volume:", menuRect.x + 50f, menuRect.y + 300f)
        textFont.draw(batch, "${(SoundManager.musicVolume * 100).toInt()}%", menuRect.x + 260f, menuRect.y + 300f)

        // Текст "Sound volume"
        textFont.draw(batch, "Sound volume:", menuRect.x + 50f, menuRect.y + 220f)
        textFont.draw(batch, "${(SoundManager.soundVolume * 100).toInt()}%", menuRect.x + 260f, menuRect.y + 220f)

        batch.end()

        // Рисуем кнопки "-" и "+" для музыки
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(Color.LIGHT_GRAY)
        shapeRenderer.rect(musicDownRect.x, musicDownRect.y, musicDownRect.width, musicDownRect.height)
        shapeRenderer.rect(musicUpRect.x, musicUpRect.y, musicUpRect.width, musicUpRect.height)
        shapeRenderer.rect(soundDownRect.x, soundDownRect.y, soundDownRect.width, soundDownRect.height)
        shapeRenderer.rect(soundUpRect.x, soundUpRect.y, soundUpRect.width, soundUpRect.height)
        shapeRenderer.end()

        batch.begin()
        textFont.color = Color.BLACK
        textFont.data.setScale(1.5f)
        textFont.draw(batch, "-", musicDownRect.x + 20f, musicDownRect.y + 40f)
        textFont.draw(batch, "+", musicUpRect.x + 20f, musicUpRect.y + 40f)
        textFont.draw(batch, "-", soundDownRect.x + 20f, soundDownRect.y + 40f)
        textFont.draw(batch, "+", soundUpRect.x + 20f, soundUpRect.y + 40f)

        // Кнопка закрытия
        batch.draw(closeButton, closeSettingsRect.x, closeSettingsRect.y, closeSettingsRect.width, closeSettingsRect.height)
        batch.end()
    }

    private fun renderCreditsMenu(batch: SpriteBatch, shapeRenderer: ShapeRenderer) {
        val menuRect = getMenuRect()

        shapeRenderer.projectionMatrix = uiCamera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(Color(0f, 0f, 0f, 0.5f))
        shapeRenderer.rect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat())
        shapeRenderer.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(Color(0.2f, 0.2f, 0.2f, 1f))
        shapeRenderer.rect(menuRect.x, menuRect.y, menuRect.width, menuRect.height)
        shapeRenderer.end()

        batch.begin()

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
        textFont.draw(batch, "Main developer:", menuRect.x + 50f, yOffset)
        yOffset -= 35f

        textFont.color = Color.WHITE
        textFont.data.setScale(1.5f)
        textFont.draw(batch, "  triplethall", menuRect.x + 50f, yOffset)
        yOffset -= 45f

        textFont.color = Color.GOLD
        textFont.data.setScale(1.7f)
        textFont.draw(batch, "Contributors", menuRect.x + 50f, yOffset)
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
        for (c in contributors) {
            textFont.draw(batch, c, menuRect.x + 50f, yOffset)
            yOffset -= 35f
        }

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
