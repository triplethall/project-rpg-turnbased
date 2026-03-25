package ru.triplethall.rpgturnbased

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle
import kotlin.random.Random


class BattleScene(
    private val font: BitmapFont,
    private val screenWidth: Float,
    private val screenHeight: Float,
    private val gameMap: GameMap,
    private val BGArena: Texture
) {
    var isActive = false
        private set
    private lateinit var player: Player
    fun setPlayer(player: Player)
    {
        this.player = player
    }
    private var enemies: MutableList<BattleEnemy> = mutableListOf() // ENEMY COUNT IN BATTLE
    private var enemyIndex = 0 // WHICH ENEMY PLAYER IS ATTACKING
    private val attackButtonRect = Rectangle()
    private val nextTurnButtonRect = Rectangle()
    private val fleeButtonRect = Rectangle()
    private var enemyX = 0
    private var enemyY = 0
    private var madeMoveThisTurn = false
    fun startBattle(enemyCellX: Int, enemyCellY: Int, enemyCount: Int) {
        this.enemyX = enemyCellX
        this.enemyY = enemyCellY
        this.enemies = BattleEnemy.createRandomEnemies(enemyCount.coerceIn(1, 3))
        isActive = true
        madeMoveThisTurn = false
        enemyIndex = 0
    }
    // PEREGRUZKA METODA
    fun startBattle(enemyCellX: Int, enemyCellY: Int)
    {
        if (player.currentHealth <= 0)
        {
            println("player is DEAD LMAO")
            return
        }
        val randomCount = (1..3).random()
        startBattle(enemyCellX, enemyCellY, randomCount)
    }
    fun handleInput(player: Player): Boolean {
        if (!isActive) {
            return false
        }

        val touchX = Gdx.input.x.toFloat()
        val touchY = Gdx.input.y.toFloat()
        val yInverted = screenHeight - touchY

        if (Gdx.input.justTouched()) {
            // CLICKING ENEMY CHANGES TARGET TO HIM
            if (!madeMoveThisTurn)
            {
                val clickedIndex = getEnemyPos(touchX, yInverted)
                if (clickedIndex != -1 && enemies.getOrNull(clickedIndex)?.isAlive() == true)
                {
                    enemyIndex = clickedIndex
                    return true
                }
            }
            // Кнопка атаки
            if (!madeMoveThisTurn && attackButtonRect.contains(touchX, yInverted)) {
                performAttack()
                return true
            }

            // Кнопка перехода хода
            if (madeMoveThisTurn && nextTurnButtonRect.contains(touchX, yInverted)) {
                nextTurn()
                return true
            }

            // Кнопка побега
            if (!madeMoveThisTurn && fleeButtonRect.contains(touchX, yInverted)) {
                flee()
                return true
            }
        }
        return false
    }
    fun getEnemyPos(x: Float, y: Float): Int
    {
        val rectHeight = 150f
        val rectWidth = 100f
        val space = screenWidth * 0.1f
        val rectY = (screenHeight - rectHeight) / 2
        val rectX = screenWidth - rectWidth - space
        val enemyStartX = rectX - 400f

        when (enemies.size) {
            1 -> {
                val enemyY = rectY - 100f
                val rect = Rectangle(enemyStartX, enemyY, rectWidth, rectHeight)
                if (rect.contains(x, y))
                {
                    return 0
                }
            }
            2 -> {
                val offsetY = 90f
                val enemyY1 = rectY - 100f - offsetY  // Верхний
                val enemyY2 = rectY - 100f + offsetY  // Нижний
                val rect1 = Rectangle(enemyStartX, enemyY1, rectWidth, rectHeight)
                val rect2 = Rectangle(enemyStartX - 100f, enemyY2, rectWidth, rectHeight)
                if (rect1.contains(x,y))
                {
                    return 0
                }
                else if (rect2.contains(x, y))
                {
                    return 1
                }
            }
            3 -> {
                val offsetY = 100f
                val enemyY1 = rectY - 100f - offsetY  // Верхний
                val enemyY2 = rectY - 100f           // Центральный
                val enemyY3 = rectY - 100f + offsetY  // Нижний
                val rect1 = Rectangle(enemyStartX - 100f, enemyY1 - 50f, rectWidth, rectHeight)
                val rect2 = Rectangle(enemyStartX, enemyY2, rectWidth, rectHeight)
                val rect3 = Rectangle(enemyStartX - 85f, enemyY3 + 50f, rectWidth, rectHeight)
                if (rect1.contains(x,y))
                {
                    return 0
                }
                else if (rect2.contains(x,y))
                {
                    return 1
                }
                else if (rect3.contains(x,y))
                {
                    return 2
                }
            }
        }
        return -1
    }
    private fun victoryScreen()
    {
        println("victory")
        endBattleAndClearEnemy()
        // TODO: VICTORY SCREEN AND REWARD MOST LIKELY
    }
    private fun defeatScreen()
    {
        println("defeat")
        if (player.currentHealth <= 0)
        {
            player.currentHealth = 1
        }
        endBattleAndClearEnemy()
        // TODO: DEFEAT SCREEN + ADD CORRUPTION AND PROBABLY GO BACK TO SPAWN?
    }
    private fun performAttack()
    {
        if (enemies.isEmpty())
        {
            endBattleAndClearEnemy()
            return
        }


        val target = enemies[enemyIndex]

        val baseDamage = player.damage
        val randomMultiplier = 0.8 + Random.nextDouble() * 0.4
        val totalDamage = (baseDamage * randomMultiplier).toInt()

        val dmgWithDef = (totalDamage * (1 - target.defense)).toInt()
        target.takeDamage(dmgWithDef)
        println("dealt $dmgWithDef to ${target.name}")
        if (!target.isAlive())
        {
            // IF ENEMY DEFEATED THEN REMOVE HIM FROM THE LIST
            println("${target.name} is defeated")
            enemies.removeAt(enemyIndex)
            if (enemies.isEmpty())
            {
                // IF NO ENEMY LEFT THEN GGEZ
                println("no enemies left")
                victoryScreen()
                return
            }
            else
            {
                println("${enemies.size} enemies left")
            }
            // IF INDEX OUT OF RANGE THEN GO TO START
            if (enemyIndex >= enemies.size)
            {
                enemyIndex = 0
            }
        }
        madeMoveThisTurn = true
    }
    private fun enemyTurn()
    {
        if (enemies.isEmpty())
        {
            return
        }

        if (player.currentHealth <= 0)
        {
            defeatScreen()
            return
        }
        println("enemy turn")
        enemies.forEach { enemy ->
            if (enemy.isAlive() && player.currentHealth > 0)
            {
                if (enemy.canHit())
                {
                    val damage = enemy.calculateDamage()
                    player.currentHealth = (player.currentHealth - damage)
                    println("${enemy.name} dealt $damage . player health: ${player.currentHealth}/${player.maxHealth}")
                }
                else
                {
                    println("enemy missed")
                }
            }
        }
        if (player.currentHealth <= 0)
        {
            defeatScreen()
        }
    }
    private fun nextTurn()
    {
        madeMoveThisTurn = false
        if (isActive && !enemies.isEmpty() && player.currentHealth > 0)
        {
            enemyTurn()
        }
    }
    private fun flee()
    {
        madeMoveThisTurn = true
        // TODO: ESCAPE WITH 2 TURNS
    }
    fun render(batch: SpriteBatch, whitePixel: Texture, player: Player)
    {
        if (!isActive)
        {
            return
        }
        // TODO: BATTLE MESSAGES (PLAYER TURN, DAMAGE DEALT, ETC)
        // COORDINATES FOR RECTANGLES (PLAYER AND ENEMY)
        val rectHeight = 150f
        val rectWidth = 100f
        val space = screenWidth * 0.1f
        val rectY = (screenHeight - rectHeight) / 2
        val rectX = screenWidth - rectWidth - space
        batch.draw(BGArena, 0f, 0f, screenWidth, screenHeight)
        // BUTTON SIZES, SPACING AND COORDINATES
        val buttonWidth = 150f
        val buttonHeight = 70f
        val buttonSpacing = 30f
        val buttonY = 50f

        val totalWidth = buttonWidth * 3 + buttonSpacing * 2
        val startX = (screenWidth - totalWidth) / 2
        val attackX = startX
        val turnX = startX + buttonWidth + buttonSpacing
        val fleeX = startX + (buttonWidth + buttonSpacing) * 2

        attackButtonRect.set(attackX, buttonY, buttonWidth, buttonHeight)
        nextTurnButtonRect.set(turnX, buttonY, buttonWidth, buttonHeight)
        fleeButtonRect.set(fleeX, buttonY, buttonWidth, buttonHeight)
        batch.color = Color.BLUE
        // DRAWING PLAYER RECTANGLE
        batch.draw(whitePixel, space + 400f, rectY - 100f, rectWidth, rectHeight)

        font.color = Color.WHITE
        // SHOWING INFO ABOUT PLAYER
        font.draw(batch, "PLAYER", space + 430f, rectY - 120f)
        font.draw(batch, "${player.currentHealth}/${player.maxHealth}", space + 430f, rectY + rectHeight - 70f)
        if (enemies.isNotEmpty()) {
            val enemyStartX = rectX - 400f

            when (enemies.size) {
                1 -> {
                    // один враг
                    val enemyY = rectY - 100f
                    drawEnemy(batch, whitePixel, enemies[0], enemyStartX, enemyY, rectWidth, rectHeight, enemyIndex == 0)
                }
                2 -> {
                    // Два врага
                    val offsetY = 90f
                    val enemyY1 = rectY - 100f - offsetY  // Верхний
                    val enemyY2 = rectY - 100f + offsetY  // Нижний
                    drawEnemy(batch, whitePixel, enemies[0], enemyStartX, enemyY1, rectWidth, rectHeight, enemyIndex == 0)
                    drawEnemy(batch, whitePixel, enemies[1], enemyStartX - 100f, enemyY2, rectWidth, rectHeight, enemyIndex == 1)
                }
                3 -> {
                    // Три врага
                    val offsetY = 100f
                    val enemyY1 = rectY - 100f - offsetY  // Верхний
                    val enemyY2 = rectY - 100f           // Центральный
                    val enemyY3 = rectY - 100f + offsetY  // Нижний
                    drawEnemy(batch, whitePixel, enemies[0], enemyStartX - 100f, enemyY1 - 50f, rectWidth, rectHeight, enemyIndex == 0)
                    drawEnemy(batch, whitePixel, enemies[1], enemyStartX, enemyY2, rectWidth, rectHeight, enemyIndex == 1)
                    drawEnemy(batch, whitePixel, enemies[2], enemyStartX - 85f, enemyY3 + 50f, rectWidth, rectHeight, enemyIndex == 2)
                }
                else -> {
                    println("idk")
                    return
                }
            }

            // Отображаем количество врагов
            font.color = Color.WHITE
            font.draw(batch, "enemies: ${enemies.size}", screenWidth - 150f, screenHeight - 30f)
        }

        // BUTTONS
        // ATTACK BUTTON
        batch.color = if (!madeMoveThisTurn) Color.GREEN else Color.DARK_GRAY
        batch.draw(whitePixel, attackX, buttonY, buttonWidth, buttonHeight)
        font.color = Color.WHITE
        font.draw(batch, "ATTACK", attackX + 45f, buttonY + 42f)

        // NEXT TURN BUTTON
        batch.color = if (madeMoveThisTurn) Color.ORANGE else Color.DARK_GRAY
        batch.draw(whitePixel, turnX, buttonY, buttonWidth, buttonHeight)
        font.draw(batch, "NEXT TURN", turnX + 55f, buttonY + 42f)

        // ESCAPE BUTTON
        batch.color = if (!madeMoveThisTurn) Color.RED else Color.DARK_GRAY
        batch.draw(whitePixel, fleeX, buttonY, buttonWidth, buttonHeight)
        font.draw(batch, "ESCAPE", fleeX + 45f, buttonY + 42f)

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

    fun endBattleAndClearEnemy() {
        if (enemyX in 0 until gameMap.width && enemyY in 0 until gameMap.height) {
            gameMap.setTerrain(enemyX, enemyY, TerrainType.LAND)
        }
        isActive = false
        madeMoveThisTurn = false
        enemies.clear()
    }
    private fun drawEnemy(batch: SpriteBatch, whitePixel: Texture, enemy: BattleEnemy, x: Float, y: Float, width: Float, height: Float, isSelected: Boolean) {
        // Если враг мертв — рисуем серым
        batch.color = if (enemy.isAlive()) Color.RED else Color.DARK_GRAY
        batch.draw(whitePixel, x, y, width, height)

        // Если это выбранный враг — подсвечиваем желтой рамкой
        if (isSelected && enemy.isAlive()) {
            batch.color = Color.YELLOW
            batch.draw(whitePixel, x - 5f, y - 5f, width + 10f, height + 10f)
        }

        font.color = Color.WHITE
        font.draw(batch, enemy.name, x + 20f, y - 20f)
        font.draw(batch, "${enemy.currentHealth}/${enemy.maxHealth}", x + 20f, y + height - 50f)
    }


}
