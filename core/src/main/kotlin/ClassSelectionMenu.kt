package ru.triplethall.rpgturnbased

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3

interface ClassSelectionListener {
    fun onClassSelected(playerClass: PlayerClasses?)
}

class ClassSelectionMenu(
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val listener: ClassSelectionListener
) {
    private var visible = true
    private var selectedClass: PlayerClasses? = null
    private var currentPage = 0
    private val classesPerPage = 3

    private val allClasses = PlayerClasses.values().toList()
    private val totalPages = (allClasses.size + classesPerPage - 1) / classesPerPage

    // Текстуры для кнопок (используем реальные текстуры, если есть)
    private var selectButtonTexture: Texture? = null
    private var backButtonTexture: Texture? = null

    private lateinit var titleFont: BitmapFont
    private lateinit var classFont: BitmapFont
    private lateinit var descFont: BitmapFont

    private val uiCamera = OrthographicCamera().apply {
        setToOrtho(false, screenWidth.toFloat(), screenHeight.toFloat())
    }

    private data class Button(val rect: Rectangle, val text: String, val action: () -> Unit)
    private val buttons = mutableListOf<Button>()
    private val classRects = mutableListOf<Rectangle>()

    init {
        loadTextures()
        initFonts()
        initClassRects()
        updateButtons()
    }

    private fun loadTextures() {
        // Пытаемся загрузить реальные текстуры, если их нет - используем null
        try {
            selectButtonTexture = Texture("menus/buttons/select.png")
        } catch (e: Exception) {
            selectButtonTexture = null
        }
        try {
            backButtonTexture = Texture("menus/buttons/back.png")
        } catch (e: Exception) {
            backButtonTexture = null
        }
    }

    private fun initFonts() {
        titleFont = BitmapFont()
        titleFont.color = Color.GOLD
        titleFont.data.setScale(4f)

        classFont = BitmapFont()
        classFont.color = Color.WHITE
        classFont.data.setScale(2.5f)

        descFont = BitmapFont()
        descFont.color = Color.LIGHT_GRAY
        descFont.data.setScale(1.8f)
    }

    private fun initClassRects() {
        val startX = screenWidth / 2f - 200f
        var startY = screenHeight - 200f
        val width = 400f
        val height = 120f
        val spacing = 20f

        classRects.clear()
        for (i in 0 until classesPerPage) {
            classRects.add(Rectangle(startX, startY - i * (height + spacing), width, height))
        }
    }

    private fun updateButtons() {
        buttons.clear()

        val buttonWidth = 150f
        val buttonHeight = 50f
        val centerX = screenWidth / 2f

        buttons.add(Button(
            rect = Rectangle(centerX - buttonWidth / 2f, 150f, buttonWidth, buttonHeight),
            text = "SELECT",
            action = { selectCurrentClass() }
        ))

        buttons.add(Button(
            rect = Rectangle(50f, 50f, 100f, 50f),
            text = "BACK",
            action = { cancel() }
        ))

        if (currentPage > 0) {
            buttons.add(Button(
                rect = Rectangle(centerX - 150f, 80f, 80f, 40f),
                text = "←",
                action = { previousPage() }
            ))
        }

        if (currentPage < totalPages - 1) {
            buttons.add(Button(
                rect = Rectangle(centerX + 70f, 80f, 80f, 40f),
                text = "→",
                action = { nextPage() }
            ))
        }
    }

    private fun getCurrentPageClasses(): List<PlayerClasses> {
        val start = currentPage * classesPerPage
        val end = minOf(start + classesPerPage, allClasses.size)
        return allClasses.subList(start, end)
    }

    private fun selectCurrentClass() {
        selectedClass?.let {
            visible = false
            listener.onClassSelected(it)
        }
    }

    private fun cancel() {
        visible = false
        listener.onClassSelected(null)
    }

    private fun nextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++
            selectedClass = null
            updateButtons()
        }
    }

    private fun previousPage() {
        if (currentPage > 0) {
            currentPage--
            selectedClass = null
            updateButtons()
        }
    }

    fun handleInput(): Boolean {
        if (!visible) return false

        if (Gdx.input.justTouched()) {
            val touchPos = Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
            uiCamera.unproject(touchPos)

            val currentClasses = getCurrentPageClasses()
            for (i in currentClasses.indices) {
                val rect = classRects[i]
                if (rect.contains(touchPos.x, touchPos.y)) {
                    selectedClass = currentClasses[i]
                    return true
                }
            }

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
        shapeRenderer.projectionMatrix = uiCamera.combined
        // 1. Темный фон
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(Color(0f, 0f, 0f, 0.8f))
        shapeRenderer.rect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat())
        shapeRenderer.end()

        // 2. Фон меню
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(Color(0.15f, 0.15f, 0.2f, 1f))
        shapeRenderer.rect(50f, 50f, screenWidth - 100f, screenHeight - 100f)
        shapeRenderer.end()

        // 3. Подсветка выбранного класса (прямоугольники)
        val currentClasses = getCurrentPageClasses()
        for (i in currentClasses.indices) {
            val rect = classRects[i]
            if (selectedClass == currentClasses[i]) {
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
                shapeRenderer.setColor(Color(0.3f, 0.5f, 0.8f, 0.5f))
                shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height)
                shapeRenderer.end()
            }
        }

        // 4. Рамки классов (линии)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        shapeRenderer.setColor(Color.WHITE)
        for (i in currentClasses.indices) {
            val rect = classRects[i]
            shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height)
        }
        shapeRenderer.end()

        // 5. Фон кнопок
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(Color(0.2f, 0.2f, 0.3f, 1f))
        for (button in buttons) {
            shapeRenderer.rect(button.rect.x, button.rect.y, button.rect.width, button.rect.height)
        }
        shapeRenderer.end()
        batch.projectionMatrix = uiCamera.combined
        batch.begin()

        // Заголовок
        titleFont.draw(batch, "CHOOSE YOUR CLASS",
            screenWidth / 2f - 150f, screenHeight - 80f)

        // Информация о странице
        if (totalPages > 1) {
            classFont.color = Color.GRAY
            classFont.draw(batch, "Page ${currentPage + 1}/$totalPages",
                screenWidth / 2f - 40f, 120f)
        }

        // Названия и описания классов
        for (i in currentClasses.indices) {
            val playerClass = currentClasses[i]
            val rect = classRects[i]

            // Название класса
            classFont.color = Color.GOLD
            classFont.draw(batch, playerClass.displayName,
                rect.x + 10f, rect.y + rect.height - 15f)

            // Описание
            descFont.color = Color.WHITE
            descFont.draw(batch, playerClass.description,
                rect.x + 10f, rect.y + rect.height - 45f)

            // Характеристики
            descFont.color = Color.LIGHT_GRAY
            val stats = "❤️ ${playerClass.baseMaxHealth}  ⚔️ ${playerClass.baseDamage}  🛡️ ${(playerClass.baseDefense * 100).toInt()}%"
            descFont.draw(batch, stats,
                rect.x + 10f, rect.y + 30f)
        }

        // Текст на кнопках
        for (button in buttons) {
            classFont.color = Color.WHITE
            classFont.data.setScale(1.0f)
            classFont.draw(batch, button.text,
                button.rect.x + button.rect.width / 2f - button.text.length * 5f,
                button.rect.y + button.rect.height / 2f + 10f)
        }

        batch.end()
    }

    fun isVisible() = visible

    fun show() {
        visible = true
        selectedClass = null
        currentPage = 0
        updateButtons()
    }

    fun hide() {
        visible = false
    }

    fun dispose() {
        titleFont.dispose()
        classFont.dispose()
        descFont.dispose()
        selectButtonTexture?.dispose()
        backButtonTexture?.dispose()
    }
}
