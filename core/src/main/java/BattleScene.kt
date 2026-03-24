package ru.triplethall.rpgturnbased

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle


class BattleScene(
    private val font: BitmapFont,
    private val screenWidth: Float,
    private val screenHeight: Float,
    private val gameMap: GameMap
) {
    var isActive = false
        private set
    private val exitButtonRect = Rectangle()
    private val getDmgButtonRect = Rectangle()
    private var enemyX = 0
    private var enemyY = 0
    fun startBattle(enemyCellX: Int, enemyCellY: Int) {
        this.enemyX = enemyCellX
        this.enemyY = enemyCellY
        isActive = true
    }
    fun handleInput(player: Player): Boolean {
        if (!isActive) {
            return false
        }

        val touchX = Gdx.input.x.toFloat()
        val touchY = Gdx.input.y.toFloat()
        val yInverted = screenHeight - touchY

        if (Gdx.input.justTouched() && exitButtonRect.contains(touchX, yInverted)) {
            endBattleAndClearEnemy()  // вызываем очистку
            return true
        }
        if (Gdx.input.justTouched() && getDmgButtonRect.contains(touchX, yInverted)) {
            getDmg(player)// получаем урон
            return true
        }

        return false
    }

    fun render(batch: SpriteBatch, whitePixel: Texture, player: Player)
    {
        if (!isActive)
        {
            return
        }

        batch.color = Color(0f, 0f, 0f, 0.8f)
        batch.draw(whitePixel, 0f, 0f, screenWidth, screenHeight)
        batch.color = Color.BLUE // player rect

        batch.draw(whitePixel, 200f, 300f, 150f, 200f)
        font.color = Color.WHITE // player info

        font.draw(batch, "PLAYER", 230f, 450f)
        font.draw(batch, "${player.currentHealth}/${player.maxHealth}", 220f, 380f)
        batch.color = Color.RED // enemy rect

        batch.draw(whitePixel, 550f, 300f, 150f, 200f)
        font.color = Color.WHITE // enemy info

        font.draw(batch, "ENEMY", 590f, 450f)
        font.draw(batch, "currentHealth/maxHealth", 600f, 380f)

        val btnX = screenWidth / 2 - 100f
        val btnY = 100f // leave button

        exitButtonRect.set(btnX, btnY, 200f, 60f)
        batch.color = Color.GRAY // leave button

        batch.draw(whitePixel, btnX, btnY, 200f, 60f)
        font.color = Color.WHITE // text color

        font.draw(batch, "LEAVE", btnX + 30f, btnY + 35f)
        batch.color = Color.WHITE // button text
        // getDamageBtn
        val l_btnX = screenWidth / 2 - 100f
        val l_btnY = 300f // leave button

        getDmgButtonRect.set(l_btnX, l_btnY, 200f, 60f)
        batch.color = Color.GRAY // leave button

        batch.draw(whitePixel, l_btnX, l_btnY, 200f, 60f)
        font.color = Color.WHITE // text color

        font.draw(batch, "getDmg", l_btnX + 30f, l_btnY + 35f)
        batch.color = Color.WHITE // button text


        // Указываем стартовую позицию для верхнего бара
        val startY = 600f
        // Первый бар (Здоровье)
        drawStatBar(whitePixel, batch, player.currentHealth, player.maxHealth, 20f, startY, Color.RED)

        // Второй бар (Мана) — рисуем ниже на (высоту квадрата + отступы + наш gap)
        val secondBarY = startY - squareSize - (padding * 2) - verticalGap
        drawStatBar(whitePixel, batch, player.currentMana, player.maxMana, 20f, secondBarY, Color.BLUE)

    }
    // --- НАСТРОЙКИ (поменяй здесь одну цифру, и всё изменится) ---
    private val squareSize = 24 * 2f       // Размер одного квадратика
    private val spacing = 4 * 2f          // Расстояние между ними
    private val padding = 3 * 2f          // Внутренний отступ фона (рамка)
    private val totalBlocks = 10      // Сколько всего квадратиков

    private val verticalGap = 15f // Отступ между барами
    // -------------------------------------------------------------

    private fun drawStatBar(whitePixel: Texture, batch: SpriteBatch ,current: Int, max: Int, startX: Float, y: Float, baseColor: Color) {
        // Вычисляем общую ширину всей полоски автоматически
        val step = squareSize + spacing
        val totalBarWidth = (step * totalBlocks) - spacing // Убираем лишний отступ в конце


        // 1. Рисуем сплошной фон (подложку)
        batch.color = Color.BLACK // Черная рамка
        batch.draw(whitePixel,
            startX - padding,
            y - padding,
            totalBarWidth + (padding * 2),
            squareSize + (padding * 2)
        )

        val totalPercent = (current.toFloat() / max.toFloat()) * 100f

        // 2. Рисуем квадратики
        for (i in 0 until totalBlocks) {
            val lowBound = i * (100f / totalBlocks)
            val highBound = (i + 1) * (100f / totalBlocks)

            // Логика яркости/прозрачности
            val factor = when {
                totalPercent >= highBound -> 1.0f
                totalPercent <= lowBound -> 0.0f
                else -> (totalPercent - lowBound) / (100f / totalBlocks)
            }

            if (factor > 0) {
                batch.color = baseColor.cpy().mul(factor, factor, factor, 1f)
                // Координата X теперь зависит от настроек выше
                batch.draw(whitePixel, startX + (i * step), y, squareSize, squareSize)
            }
        }
        batch.color = Color.WHITE
    }

    private fun getDmg(player: Player) {
        // Отнимаем 10 хп, но не даем упасть ниже 0
        player.currentHealth = (player.currentHealth - 10).coerceAtLeast(0)

        // Для теста можно выводить в консоль
        println("Упс! У игрока осталось ${player.currentHealth} HP")
    }
    fun endBattleAndClearEnemy() {
        if (enemyX in 0 until gameMap.width && enemyY in 0 until gameMap.height) {
            gameMap.setTerrain(enemyX, enemyY, TerrainType.LAND)
        }
        isActive = false
    }

}
