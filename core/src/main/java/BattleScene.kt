package ru.triplethall.rpgturnbased

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.particles.influencers.RegionInfluencer
import com.badlogic.gdx.math.Rectangle
import kotlin.random.Random


class BattleScene(
    private val font: BitmapFont,
    private val screenWidth: Float,
    private val screenHeight: Float,
    private val gameMap: GameMap,
    private val BGArena: Texture,
    private val whitePixel: Texture
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
    private var isFleeing = false // if ESCAPE is clicked, it's true
    private var fleeTurnsLeft = 0 // how many turns have left for player to escape
    private val getDmgButtonRect = Rectangle()
    private val nextTurnButtonRect = Rectangle()
    private val skipButtonRect = Rectangle()
    private val fleeButtonRect = Rectangle()
    private var enemyX = 0
    private var enemyY = 0
    private var madeMoveThisTurn = false
    private var showVictoryScreen = false
    private var showDefeatScreen = false
    private val exitButton = Rectangle()
    // Бары игрока
    private lateinit var playerHealthBar: StatBar
    private lateinit var playerManaBar: StatBar
    // MESSAGES THAT ARE DISPLAYED IN BATTLE. USE 'messageSystem.addMessage("Hello world!", Color.WHITE)' TO ADD A MESSAGE
    private lateinit var messageSystem: BattleMessageSystem
    // Список баров врагов (синхронизируется с enemies)
    private val enemyBars = mutableListOf<StatBar>()
    fun startBattle(enemyCellX: Int, enemyCellY: Int, enemyCount: Int) {
        messageSystem = BattleMessageSystem(font, screenWidth,screenHeight, whitePixel)
        messageSystem.addMessage("start", Color.YELLOW)
        this.enemyX = enemyCellX
        this.enemyY = enemyCellY
        this.enemies = BattleEnemy.createRandomEnemies(enemyCount.coerceIn(1, 3))
        isActive = true
        madeMoveThisTurn = false
        enemyIndex = 0

        // Создаём бары игрока (они статичны)
        val playerBarX = 20f
        val playerHealthY = screenHeight * 0.9f
        val playerManaY = playerHealthY - squareSize - (padding * 2) - verticalGap

        playerHealthBar = StatBar(playerBarX, playerHealthY, 400f, 20f, Color.RED)
        playerManaBar = StatBar(playerBarX, playerManaY, 400f, 20f, Color.BLUE)

        // Создаём бары для врагов на основе текущих позиций
        updateEnemyBars()
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
    private fun updateEnemyBars() {
        enemyBars.clear()
        val rectHeight = 150f
        val rectWidth = 100f
        val space = screenWidth * 0.1f
        val enemyStartX = screenWidth - rectWidth - space
        val enemySpacing = 20f
        val rectY = (screenHeight - rectHeight) / 2

        enemies.forEachIndexed { index, enemy ->
            val barY = rectY + (index * (rectHeight + enemySpacing)) + rectHeight + 5f // под врагом
            enemyBars.add(StatBar(enemyStartX, barY, rectWidth, 10f, Color.RED))
        }
    }
    fun handleInput(player: Player): Boolean {
        if (!isActive) {
            return false
        }

        val touchX = Gdx.input.x.toFloat()
        val touchY = Gdx.input.y.toFloat()
        val yInverted = screenHeight - touchY


        if (showVictoryScreen || showDefeatScreen) // если экран победы/проигрыша активен
        {
            if (Gdx.input.justTouched() && exitButton.contains(touchX, yInverted))
            {
                showVictoryScreen = false
                showDefeatScreen = false
                endBattleAndClearEnemy()
                return true
            }
            return true // блокировать остальные клики
        }

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

            // SKIP TURN BUTTON
            if (!madeMoveThisTurn && skipButtonRect.contains(touchX, yInverted))
            {
                skipTurn()
                return true
            }

            // Кнопка получения урона
            if (getDmgButtonRect.contains(touchX, yInverted)) {
                getDmg(player, 1)
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
        showVictoryScreen = true
    }
    private fun defeatScreen()
    {
        println("defeat")
        val lostExp = (player.experience * 0.2).toInt().coerceAtLeast(1)
        println("lostExp = $lostExp")
        player.experience = (player.experience - lostExp).coerceAtLeast(0)
        println("player exp = ${player.experience}")
        player.corruption++
        println("player corr = ${player.corruption}")
        if (player.currentHealth <= 0)
        {
            player.currentHealth = 1
        }
        showDefeatScreen = true
    }
    // метод отрисовки победного экрана
    private fun drawVictoryScreen(batch: SpriteBatch, whitePixel: Texture)
    {
        // фон
        batch.color = Color(0f,0f,0f,0.5f)
        batch.draw(whitePixel, 0f, 0f, screenWidth, screenHeight)

        // текст победы
        font.color = Color.GOLD
        font.data.setScale(2f)
        font.draw(batch, "VICTORY", screenWidth / 2 - 65f, screenHeight / 2 + 100f)
        font.data.setScale(1f)

        // кнопка выхода
        val buttonWidth = 200f
        val buttonHeight = 60f
        val buttonX = screenWidth / 2 - buttonWidth / 2
        val buttonY = screenHeight / 2 - 50f

        exitButton.set(buttonX, buttonY, buttonWidth, buttonHeight)
        batch.color = Color.RED
        batch.draw(whitePixel, buttonX, buttonY, buttonWidth, buttonHeight)
        // tekst na knopke
        font.color = Color.WHITE
        font.draw(batch, "EXIT", buttonX + 75f, buttonY + 40f)

        batch.color = Color.WHITE
    }
    // метод отрисовки проигрышного экрана
    private fun drawDefeatScreen(batch: SpriteBatch, whitePixel: Texture) {
        // фон
        batch.color = Color(0f,0f,0f,0.5f)
        batch.draw(whitePixel, 0f, 0f, screenWidth, screenHeight)

        // текст проигрыша
        font.color = Color.ORANGE
        font.data.setScale(2f)
        font.draw(batch, "DEFEAT", screenWidth / 2 - 65f, screenHeight / 2 + 100f)
        font.data.setScale(1f)

        // кнопка выхода
        val buttonWidth = 200f
        val buttonHeight = 60f
        val buttonX = screenWidth / 2 - buttonWidth / 2
        val buttonY = screenHeight / 2 - 50f

        exitButton.set(buttonX, buttonY, buttonWidth, buttonHeight)
        batch.color = Color.RED
        batch.draw(whitePixel, buttonX, buttonY, buttonWidth, buttonHeight)
        // tekst na knopke
        font.color = Color.WHITE
        font.draw(batch, "EXIT", buttonX + 75f, buttonY + 40f)

        batch.color = Color.WHITE
    }
    fun isShowingEndScreen(): Boolean = showVictoryScreen || showDefeatScreen
    private fun performAttack()
    {
        if (isFleeing)
        {
            messageSystem.addMessage("trying to escape! can't attack!", Color.RED)
            return
        }
        val target = enemies[enemyIndex]

        // Простой расчет урона без типов
        val baseDamage = player.damage
        val randomMultiplier = 0.8 + Random.nextDouble() * 0.4
        val totalDamage = (baseDamage * randomMultiplier).toInt()

        // Учитываем защиту врага
        val dmgWithDef = (totalDamage * (1 - target.defense)).toInt()
        target.takeDamage(dmgWithDef)
        messageSystem.addMessage("dealt $dmgWithDef dmg to ${target.name}", Color.GREEN)
        if (!target.isAlive())
        {
            // IF ENEMY DEFEATED THEN REMOVE HIM FROM THE LIST
            messageSystem.addMessage("${target.name} is ded", Color.ORANGE)
            // ...BUT BEFORE THAT, GIVE PLAYER EXP FROM HIM
            val gainExp = (target.maxHealth / 5 + target.damage / 10).coerceAtLeast(10)
            println("gainExp = $gainExp")
            player.addExperience(gainExp)
            println("player exp = ${player.experience}")
            messageSystem.addMessage("got $gainExp from ${target.name}", Color.GOLD)
            enemies.removeAt(enemyIndex)
            updateEnemyBars()
            if (enemies.isEmpty())
            {
                // IF NO ENEMY LEFT THEN GGEZ
                messageSystem.addMessage("victory🕺")
                victoryScreen()
                return
            }
            else
            {
                messageSystem.addMessage("${enemies.size} enemies left")
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
            isFleeing = false
            defeatScreen()
            return
        }
        messageSystem.addMessage("enemy turn", Color.ORANGE)
        enemies.forEach { enemy ->
            if (enemy.isAlive() && player.currentHealth > 0)
            {
                if (enemy.canHit())
                {
                    val damage = enemy.calculateDamage()
                    player.currentHealth = (player.currentHealth - damage)
                    messageSystem.addMessage("${enemy.name} dealt you $damage dmg", Color.RED)
                }
                else
                {
                    messageSystem.addMessage("${enemy.name} missed", Color.YELLOW)
                }
            }
        }
        if (player.currentHealth <= 0)
        {
            messageSystem.addMessage("бро тебе нужно больше тренироваться", Color.RED)
            defeatScreen()
            return
        }

        // Проверка побега ПОСЛЕ хода врагов
        if (isFleeing)
        {
            fleeTurnsLeft--
            if (fleeTurnsLeft <= 0)
            {
                endBattleAndClearEnemy()
                isFleeing = false
                return
            }
            else
            {
                messageSystem.addMessage("$fleeTurnsLeft more turns until escape", Color.CYAN)
            }
        }
    }

    private fun nextTurn()
    {
        madeMoveThisTurn = false
        if (isActive && !enemies.isEmpty() && player.currentHealth > 0) {
            enemyTurn()
        }
    }
    private fun flee()
    {
        if (isFleeing)
        {
            // cancel escape, turn is wasted :troll:
            isFleeing = false
            fleeTurnsLeft = 0
            madeMoveThisTurn = true
            messageSystem.addMessage("canceling attempt of escaping. TURN IS WASTED BTWWWW", Color.CORAL)
            return
        }
        isFleeing = true
        fleeTurnsLeft = 2
        madeMoveThisTurn = true
        messageSystem.addMessage("player is escaping! $fleeTurnsLeft turns left till escape!", Color.CYAN)
    }
    private fun skipTurn()
    {
        madeMoveThisTurn = true
        messageSystem.addMessage("skipped a turn", Color.FIREBRICK)
    }
    fun update(delta: Float)
    {
        if (!isActive) return
        if (::messageSystem.isInitialized)
        {
            messageSystem.update(delta)
        }
    }
    fun render(batch: SpriteBatch, whitePixel: Texture, player: Player)
    {
        if (showVictoryScreen)
        {
            drawVictoryScreen(batch, whitePixel)
            return
        }
        if (showDefeatScreen)
        {
            drawDefeatScreen(batch, whitePixel)
            return
        }
        if (!isActive)
        {
            return
        }
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

        val totalWidth = buttonWidth * 4 + buttonSpacing * 3
        // !! IF U ADD MORE BUTTONS, CHANGE THIS ^ TO: !!
        // !! buttonWidth * (BUTTON COUNT) + buttonSpacing * (BUTTON COUNT - 1) !!
        val startX = (screenWidth - totalWidth) / 2
        val attackX = startX
        val turnX = startX + buttonWidth + buttonSpacing
        val skipX = startX + (buttonWidth + buttonSpacing) * 2
        val fleeX = startX + (buttonWidth + buttonSpacing) * 3

        attackButtonRect.set(attackX, buttonY, buttonWidth, buttonHeight)
        nextTurnButtonRect.set(turnX, buttonY, buttonWidth, buttonHeight)
        skipButtonRect.set(skipX, buttonY, buttonWidth, buttonHeight)
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
//            font.draw(batch, enemy.name, enemyStartX + 30f, enemyY - 20f)
//            font.draw(batch, "${enemy.currentHealth}/${enemy.maxHealth}",
//                enemyStartX + 30f, enemyY + rectHeight - 50f)
//
//            // Маркер текущей цели (если этот враг выбран для атаки)
//            if (index == enemyIndex && enemy.isAlive()) {
//                batch.color = Color.YELLOW
//                batch.draw(whitePixel, enemyStartX - 5f, enemyY - 5f, rectWidth + 10f, rectHeight + 10f)
//            }
            // Бары игрока
            playerHealthBar.render(batch, whitePixel, player.currentHealth, player.maxHealth)
            playerManaBar.render(batch, whitePixel, player.currentMana, player.maxMana)

            // Бары врагов
            enemies.forEachIndexed { index, enemy ->
                if (index < enemyBars.size) {
                    enemyBars[index].render(batch, whitePixel, enemy.currentHealth, enemy.maxHealth)
                }
            }
        }

        // BUTTONS
        // ATTACK BUTTON
        batch.color = if (!madeMoveThisTurn) Color.GREEN else Color.DARK_GRAY
        batch.draw(whitePixel, attackX, buttonY, buttonWidth, buttonHeight)
        font.color = Color.WHITE
        font.draw(batch, "ATTACK", attackX + 35f, buttonY + 42f)

        // NEXT TURN BUTTON
        batch.color = if (madeMoveThisTurn) Color.ORANGE else Color.DARK_GRAY
        batch.draw(whitePixel, turnX, buttonY, buttonWidth, buttonHeight)
        font.draw(batch, "NEXT TURN", turnX + 15f, buttonY + 42f)

        // SKIP TURN BUTTON
        batch.color = if (!madeMoveThisTurn) Color.GRAY else Color.DARK_GRAY
        batch.draw(whitePixel, skipX, buttonY, buttonWidth, buttonHeight)
        font.draw(batch, "SKIP TURN", skipX + 17f, buttonY + 42f)

        // ESCAPE/CANCEL BUTTON
        batch.color = when
        {
            madeMoveThisTurn -> Color.DARK_GRAY
            isFleeing -> Color.YELLOW
            else -> Color.RED
        }
        batch.draw(whitePixel, fleeX, buttonY, buttonWidth, buttonHeight)
        font.draw(batch, if (!isFleeing) "ESCAPE" else "CANCEL", fleeX + 35f, buttonY + 42f)

        // getDamageBtn
        val l_btnX = screenWidth / 2 - 100f
        val l_btnY = 300f // leave button


        batch.color = Color.GRAY // leave button

        getDmgButtonRect.set(l_btnX, l_btnY, 200f, 60f)
        batch.color = Color.GRAY // dmg button

        batch.draw(whitePixel, l_btnX, l_btnY, 200f, 60f)
        font.color = Color.WHITE // text color

        font.draw(batch, "getDmg", l_btnX + 30f, l_btnY + 35f)
        batch.color = Color.WHITE // button text

        if (::messageSystem.isInitialized)
        {
            messageSystem.render(batch)
        }

    }
    // --- НАСТРОЙКИ (поменяй здесь одну цифру, и всё изменится) ---
    private val squareSize = 24 * 2f       // Размер одного квадратика
//    private val spacing = 4 * 2f          // Расстояние между ними
    private val padding = 3 * 2f          // Внутренний отступ фона (рамка)
//    private val totalBlocks = 10      // Сколько всего квадратиков

    private val verticalGap = 15f // Отступ между барами
    // -------------------------------------------------------------

    private fun getDmg(player: Player, hp: Int = 5) {
        // Отнимаем 10 хп, но не даем упасть ниже 0
        player.currentHealth = (player.currentHealth - hp).coerceAtLeast(0)

        // Для теста можно выводить в консоль
        println("Упс! У игрока осталось ${player.currentHealth} HP")
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

        // Рисуем имя врага с типом в скобках
        font.color = Color.WHITE
        val typeShort = when (enemy.enemyType) {
            EnemyType.NO_TYPE -> ""
            EnemyType.FIRE -> " [Fire]"
            EnemyType.WATER -> " [Water]"
            EnemyType.WIND -> " [Wind]"
            EnemyType.EARTH -> " [Earth]"
            EnemyType.ICE -> " [Ice]"
            EnemyType.CRYSTAL -> " [Crystal]"
            EnemyType.CURSED -> " [Cursed]"
            EnemyType.ELECTRIC -> " [Electric]"
            EnemyType.POISON -> " [Poison]"
            EnemyType.HOLY -> " [Holy]"
        }
        font.draw(batch, "${enemy.name}$typeShort", x + 20f, y - 20f)

        font.color = Color.WHITE
        font.draw(batch, enemy.name, x + 20f, y - 20f)
        font.draw(batch, "${enemy.currentHealth}/${enemy.maxHealth}", x + 20f, y + height - 50f)
    }
    fun endBattleAndClearEnemy() {
        if (enemyX in 0 until gameMap.width && enemyY in 0 until gameMap.height) {
            gameMap.setTerrain(enemyX, enemyY, TerrainType.LAND)
        }
        isActive = false
        madeMoveThisTurn = false
        isFleeing = false
        fleeTurnsLeft = 0
        enemies.clear()
    }

}

