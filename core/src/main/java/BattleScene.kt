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
    private val BGArena: Texture,
    private val whitePixel: Texture
) {
    var isActive = false
        private set
    private lateinit var player: Player
    fun setPlayer(player: Player) {
        this.player = player
    }
    private var enemies: MutableList<BattleEnemy> = mutableListOf()
    private var enemyIndex = 0
    private val attackButtonRect = Rectangle()
    private var isFleeing = false
    private var fleeTurnsLeft = 0
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
    private lateinit var playerHealthBar: StatBar
    private lateinit var playerManaBar: StatBar
    private lateinit var messageSystem: BattleMessageSystem
    private val enemyBars = mutableListOf<StatBar>()

    private val debuffRenderer = DebuffRenderer(font)
    private var battleLog = mutableListOf<String>()
    private var lastDebuffDamage = 0

    fun startBattle(enemyCellX: Int, enemyCellY: Int, enemyCount: Int) {
        messageSystem = BattleMessageSystem(font, screenWidth, screenHeight, whitePixel)
        messageSystem.addMessage("start", Color.YELLOW)
        this.enemyX = enemyCellX
        this.enemyY = enemyCellY
        this.enemies = BattleEnemy.createRandomEnemies(enemyCount.coerceIn(1, 3))
        isActive = true
        madeMoveThisTurn = false
        enemyIndex = 0

        SoundManager.pausePlaylist()
        SoundManager.playMusic("music/battle.mp3", true)

        val playerBarX = 20f
        val playerHealthY = screenHeight * 0.9f
        val playerManaY = playerHealthY - squareSize - (padding * 2) - verticalGap

        playerHealthBar = StatBar(playerBarX, playerHealthY, 400f, 20f, Color.RED)
        playerManaBar = StatBar(playerBarX, playerManaY, 400f, 20f, Color.BLUE)

        updateEnemyBars()
    }

    fun startBattle(enemyCellX: Int, enemyCellY: Int) {
        if (player.currentHealth <= 0) {
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
            val barY = rectY + (index * (rectHeight + enemySpacing)) + rectHeight + 5f
            enemyBars.add(StatBar(enemyStartX, barY, rectWidth, 10f, Color.RED))
        }
    }

    fun handleInput(player: Player): Boolean {
        if (!isActive) return false

        val touchX = Gdx.input.x.toFloat()
        val touchY = Gdx.input.y.toFloat()
        val yInverted = screenHeight - touchY

        if (showVictoryScreen || showDefeatScreen) {
            if (Gdx.input.justTouched() && exitButton.contains(touchX, yInverted)) {
                showVictoryScreen = false
                showDefeatScreen = false
                endBattleAndClearEnemy()
                return true
            }
            return true
        }

        if (Gdx.input.justTouched()) {
            if (!madeMoveThisTurn) {
                val clickedIndex = getEnemyPos(touchX, yInverted)
                if (clickedIndex != -1 && enemies.getOrNull(clickedIndex)?.isAlive() == true) {
                    SoundManager.playSound("sounds/mainBtnSound.mp3")
                    enemyIndex = clickedIndex
                    return true
                }
            }

            if (!madeMoveThisTurn && attackButtonRect.contains(touchX, yInverted)) {
                SoundManager.playSound("sounds/mainBtnSound.mp3")
                performAttack()
                return true
            }

            if (madeMoveThisTurn && nextTurnButtonRect.contains(touchX, yInverted)) {
                SoundManager.playSound("sounds/mainBtnSound.mp3")
                nextTurn()
                return true
            }

            if (!madeMoveThisTurn && skipButtonRect.contains(touchX, yInverted)) {
                SoundManager.playSound("sounds/mainBtnSound.mp3")
                skipTurn()
                return true
            }

            if (getDmgButtonRect.contains(touchX, yInverted)) {
                SoundManager.playSound("sounds/mainBtnSound.mp3")
                getDmg(player, 1)
                return true
            }

            if (!madeMoveThisTurn && fleeButtonRect.contains(touchX, yInverted)) {
                SoundManager.playSound("sounds/mainBtnSound.mp3")
                flee()
                return true
            }
        }
        return false
    }

    fun getEnemyPos(x: Float, y: Float): Int {
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
                if (rect.contains(x, y)) return 0
            }
            2 -> {
                val offsetY = 90f
                val enemyY1 = rectY - 100f - offsetY
                val enemyY2 = rectY - 100f + offsetY
                val rect1 = Rectangle(enemyStartX, enemyY1, rectWidth, rectHeight)
                val rect2 = Rectangle(enemyStartX - 100f, enemyY2, rectWidth, rectHeight)
                if (rect1.contains(x, y)) return 0
                else if (rect2.contains(x, y)) return 1
            }
            3 -> {
                val offsetY = 100f
                val enemyY1 = rectY - 100f - offsetY
                val enemyY2 = rectY - 100f
                val enemyY3 = rectY - 100f + offsetY
                val rect1 = Rectangle(enemyStartX - 100f, enemyY1 - 50f, rectWidth, rectHeight)
                val rect2 = Rectangle(enemyStartX, enemyY2, rectWidth, rectHeight)
                val rect3 = Rectangle(enemyStartX - 85f, enemyY3 + 50f, rectWidth, rectHeight)
                if (rect1.contains(x, y)) return 0
                else if (rect2.contains(x, y)) return 1
                else if (rect3.contains(x, y)) return 2
            }
        }
        return -1
    }

    private fun victoryScreen() {
        println("victory")
        SoundManager.playSound("sounds/victorySound.mp3") // Звук победы
        showVictoryScreen = true
    }

    private fun defeatScreen() {
        println("defeat")
        SoundManager.playSound("sounds/battleFail.mp3")   // Звук поражения
        val lostExp = (player.experience * 0.2).toInt().coerceAtLeast(1)
        println("lostExp = $lostExp")
        player.experience = (player.experience - lostExp).coerceAtLeast(0)
        println("player exp = ${player.experience}")
        player.corruption++
        println("player corr = ${player.corruption}")
        if (player.currentHealth <= 0) {
            player.currentHealth = 1
        }
        showDefeatScreen = true
    }

    private fun drawVictoryScreen(batch: SpriteBatch, whitePixel: Texture) {
        batch.color = Color(0f, 0f, 0f, 0.5f)
        batch.draw(whitePixel, 0f, 0f, screenWidth, screenHeight)

        font.color = Color.GOLD
        font.data.setScale(2f)
        font.draw(batch, "VICTORY", screenWidth / 2 - 65f, screenHeight / 2 + 100f)
        font.data.setScale(1f)

        val buttonWidth = 200f
        val buttonHeight = 60f
        val buttonX = screenWidth / 2 - buttonWidth / 2
        val buttonY = screenHeight / 2 - 50f

        exitButton.set(buttonX, buttonY, buttonWidth, buttonHeight)
        batch.color = Color.RED
        batch.draw(whitePixel, buttonX, buttonY, buttonWidth, buttonHeight)
        font.color = Color.WHITE
        font.draw(batch, "EXIT", buttonX + 75f, buttonY + 40f)

        batch.color = Color.WHITE
    }

    private fun drawDefeatScreen(batch: SpriteBatch, whitePixel: Texture) {
        batch.color = Color(0f, 0f, 0f, 0.5f)
        batch.draw(whitePixel, 0f, 0f, screenWidth, screenHeight)

        font.color = Color.ORANGE
        font.data.setScale(2f)
        font.draw(batch, "DEFEAT", screenWidth / 2 - 65f, screenHeight / 2 + 100f)
        font.data.setScale(1f)

        val buttonWidth = 200f
        val buttonHeight = 60f
        val buttonX = screenWidth / 2 - buttonWidth / 2
        val buttonY = screenHeight / 2 - 50f

        exitButton.set(buttonX, buttonY, buttonWidth, buttonHeight)
        batch.color = Color.RED
        batch.draw(whitePixel, buttonX, buttonY, buttonWidth, buttonHeight)
        font.color = Color.WHITE
        font.draw(batch, "EXIT", buttonX + 75f, buttonY + 40f)

        batch.color = Color.WHITE
    }

    fun isShowingEndScreen(): Boolean = showVictoryScreen || showDefeatScreen

    private fun performAttack() {
        if (isFleeing) {
            messageSystem.addMessage("trying to escape! can't attack!", Color.RED)
            return
        }
        val target = enemies[enemyIndex]

        val baseDamage = (player.damage * player.getDamageMultiplier()).toInt()
        val randomMultiplier = 0.8 + Random.nextDouble() * 0.4
        val totalDamage = (baseDamage * randomMultiplier).toInt()
        val dmgWithDef = (totalDamage * (1 - target.defense * target.getDefenseMultiplier())).toInt()

        target.takeDamage(dmgWithDef)
        messageSystem.addMessage("dealt $dmgWithDef dmg to ${target.name}", Color.GREEN)

        if (!target.isAlive()) {
            messageSystem.addMessage("${target.name} is ded", Color.ORANGE)
            val gainExp = (target.maxHealth / 5 + target.damage / 10).coerceAtLeast(10)
            println("gainExp = $gainExp")
            player.addExperience(gainExp)
            println("player exp = ${player.experience}")
            messageSystem.addMessage("got $gainExp from ${target.name}", Color.GOLD)
            enemies.removeAt(enemyIndex)

            // Проклятый враг накладывает проклятие при смерти
            if (target.enemyType == EnemyType.CURSED) {
                player.applyDebuff(DebuffType.CURSE, 3, 0.7)
                messageSystem.addMessage("${target.name} проклинает тебя перед смертью!", Color.PURPLE)
            }

            updateEnemyBars()
            if (enemies.isEmpty()) {
                messageSystem.addMessage("victory🕺")
                victoryScreen()
                return
            } else {
                messageSystem.addMessage("${enemies.size} enemies left")
            }
            if (enemyIndex >= enemies.size) {
                enemyIndex = 0
            }
        }
        madeMoveThisTurn = true
    }

    private fun enemyTurn() {
        if (enemies.isEmpty()) return

        if (player.currentHealth <= 0) {
            isFleeing = false
            defeatScreen()
            return
        }

        messageSystem.addMessage("enemy turn", Color.ORANGE)

        enemies.forEach { enemy ->
            if (enemy.isAlive() && player.currentHealth > 0) {
                val skipEnemyTurn = enemy.shouldSkipTurn()

                if (skipEnemyTurn) {
                    messageSystem.addMessage("${enemy.name} skips turn! (debuff)")
                    enemy.processDebuffs()
                    return@forEach
                }
                if (enemy.enemyType == EnemyType.HOLY && enemy.isAlive()) {
                    val healAmount = (enemy.maxHealth * 0.05).toInt()
                    enemies.filter { it.isAlive() && it != enemy }.forEach { ally ->
                        val newHealth = (ally.currentHealth + healAmount).coerceAtMost(ally.maxHealth)
                        val healed = newHealth - ally.currentHealth
                        if (healed > 0) {
                            ally.currentHealth = newHealth
                            messageSystem.addMessage("${ally.name} восстановил $healed HP от ${enemy.name}", Color.GREEN)
                        }
                    }
                }
                if (enemy.canHit()) {
                    val damage = (enemy.calculateDamage(null, true) * enemy.getDamageMultiplier()).toInt()
                    val dmgWithDef = (damage * (1 - player.defense)).toInt()
                    player.currentHealth = (player.currentHealth - dmgWithDef)
                    messageSystem.addMessage("${enemy.name} dealt you $dmgWithDef dmg", Color.RED)
                    applyEnemyDebuff(enemy)
                } else {
                    messageSystem.addMessage("${enemy.name} missed", Color.YELLOW)
                }

                enemy.processDebuffs()
            }
        }
        val debuffDmg = player.processDebuffs()
        if (debuffDmg > 0)
        {
            messageSystem.addMessage("player took $debuffDmg damage!", Color.FIREBRICK)
        }
        if (player.currentHealth <= 0) {
            messageSystem.addMessage("бро тебе нужно больше тренироваться", Color.RED)
            defeatScreen()
            return
        }

        if (isFleeing) {
            fleeTurnsLeft--
            if (fleeTurnsLeft <= 0) {
                endBattleAndClearEnemy()
                isFleeing = false
                return
            } else {
                messageSystem.addMessage("$fleeTurnsLeft more turns until escape", Color.CYAN)
            }
        }
    }

    private fun applyEnemyDebuff(enemy: BattleEnemy) {
        val debuffChance = when (enemy.enemyType) {
            EnemyType.WATER -> 0.20   // 20% на мокроту
            EnemyType.ICE -> 0.25     // 25% на заморозку
            EnemyType.ELECTRIC -> 0.20 // 20% на паралич
            EnemyType.CURSED -> 0.15   // 15% на проклятие (при атаке, но основное при смерти)
            EnemyType.POISON -> 0.25   // 25% на отравление
            EnemyType.FIRE -> 0.25     // 25% на горение
            else -> 0.0
        }

        // Проверка на сопротивление (воля игрока)
        val finalChance = debuffChance * (1.0 - player.will)
        if (Random.nextDouble() >= finalChance) return

        when (enemy.enemyType) {
            EnemyType.POISON -> {
                player.applyDebuff(DebuffType.POISON, 3, 1.0, 1)
                messageSystem.addMessage("Игрок отравлен!", Color.GREEN)
            }
            EnemyType.FIRE -> {
                player.applyDebuff(DebuffType.BURN, 3, 1.0)
                messageSystem.addMessage("Игрок горит!", Color.FIREBRICK)
            }
            EnemyType.ICE -> {
                player.applyDebuff(DebuffType.FREEZE, 1, 1.0)
                messageSystem.addMessage("Игрок заморожен!", Color.CYAN)
            }
            EnemyType.ELECTRIC -> {
                player.applyDebuff(DebuffType.PARALYSIS, 2, 0.8)
                messageSystem.addMessage("Игрок парализован!", Color.YELLOW)
            }
            EnemyType.CURSED -> {
                player.applyDebuff(DebuffType.CURSE, 3, 0.7)
                messageSystem.addMessage("Проклятие падает на игрока!", Color.PURPLE)
            }
            EnemyType.WATER -> {
                player.applyDebuff(DebuffType.WET, 3, 1.0)
                messageSystem.addMessage("Игрок промок! (+25% урон от молний)", Color.CYAN)
            }
            else -> {}
        }
    }

    private fun nextTurn() {
        madeMoveThisTurn = false
        if (isActive && !enemies.isEmpty() && player.currentHealth > 0) {
            enemyTurn()
        }
    }

    private fun flee() {
        if (isFleeing) {
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

    private fun skipTurn() {
        madeMoveThisTurn = true
        messageSystem.addMessage("skipped a turn", Color.FIREBRICK)
    }

    fun update(delta: Float) {
        if (!isActive) return
        if (::messageSystem.isInitialized) {
            messageSystem.update(delta)
        }
    }

    fun render(batch: SpriteBatch, whitePixel: Texture, player: Player) {
        if (showVictoryScreen) {
            drawVictoryScreen(batch, whitePixel)
            return
        }
        if (showDefeatScreen) {
            drawDefeatScreen(batch, whitePixel)
            return
        }
        if (!isActive) return

        val rectHeight = 150f
        val rectWidth = 100f
        val space = screenWidth * 0.1f
        val rectY = (screenHeight - rectHeight) / 2
        val rectX = screenWidth - rectWidth - space
        batch.draw(BGArena, 0f, 0f, screenWidth, screenHeight)

        val buttonWidth = 150f
        val buttonHeight = 70f
        val buttonSpacing = 30f
        val buttonY = 50f

        val totalWidth = buttonWidth * 4 + buttonSpacing * 3
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
        batch.draw(whitePixel, space + 400f, rectY - 100f, rectWidth, rectHeight)

        font.color = Color.WHITE
        font.draw(batch, "PLAYER", space + 430f, rectY - 120f)
        font.draw(batch, "${player.currentHealth}/${player.maxHealth}", space + 430f, rectY + rectHeight - 70f)

        if (enemies.isNotEmpty()) {
            val enemyStartX = rectX - 400f

            when (enemies.size) {
                1 -> {
                    val enemyY = rectY - 100f
                    drawEnemy(batch, whitePixel, enemies[0], enemyStartX, enemyY, rectWidth, rectHeight, enemyIndex == 0)
                }
                2 -> {
                    val offsetY = 90f
                    val enemyY1 = rectY - 100f - offsetY
                    val enemyY2 = rectY - 100f + offsetY
                    drawEnemy(batch, whitePixel, enemies[0], enemyStartX, enemyY1, rectWidth, rectHeight, enemyIndex == 0)
                    drawEnemy(batch, whitePixel, enemies[1], enemyStartX - 100f, enemyY2, rectWidth, rectHeight, enemyIndex == 1)
                }
                3 -> {
                    val offsetY = 100f
                    val enemyY1 = rectY - 100f - offsetY
                    val enemyY2 = rectY - 100f
                    val enemyY3 = rectY - 100f + offsetY
                    drawEnemy(batch, whitePixel, enemies[0], enemyStartX - 100f, enemyY1 - 50f, rectWidth, rectHeight, enemyIndex == 0)
                    drawEnemy(batch, whitePixel, enemies[1], enemyStartX, enemyY2, rectWidth, rectHeight, enemyIndex == 1)
                    drawEnemy(batch, whitePixel, enemies[2], enemyStartX - 85f, enemyY3 + 50f, rectWidth, rectHeight, enemyIndex == 2)
                }
                else -> {
                    println("idk")
                    return
                }
            }

            font.color = Color.WHITE
            font.draw(batch, "enemies: ${enemies.size}", screenWidth - 150f, screenHeight - 30f)

            playerHealthBar.render(batch, whitePixel, player.currentHealth, player.maxHealth)
            playerManaBar.render(batch, whitePixel, player.currentMana, player.maxMana)



            // Дебаффы врагов
            enemies.forEachIndexed { index, enemy ->
                if (!enemy.debuffManager.isEmpty()) {
                    val rectHeight = 150f
                    val rectWidth = 100f
                    val space = screenWidth * 0.1f
                    val rectY = (screenHeight - rectHeight) / 2
                    val rectX = screenWidth - rectWidth - space
                    val enemyStartX = rectX - 400f
                    val enemyY = when (enemies.size) {
                        1 -> rectY - 100f
                        2 -> if (index == 0) rectY - 100f - 90f else rectY - 100f + 90f
                        else -> when (index) {
                            0 -> rectY - 100f - 100f - 50f
                            1 -> rectY - 100f
                            else -> rectY - 100f + 100f + 50f
                        }
                    }
                    debuffRenderer.renderDebuffs(
                        batch,
                        enemy.debuffManager.getAllDebuff(),
                        enemyStartX + (if (index == 1) -100f else 0f),
                        enemyY + 100f,
                        false
                    )
                }
            }

            enemies.forEachIndexed { index, enemy ->
                if (index < enemyBars.size) {
                    enemyBars[index].render(batch, whitePixel, enemy.currentHealth, enemy.maxHealth)
                }
            }
        }

        // BUTTONS
        batch.color = if (!madeMoveThisTurn && !isFleeing) Color.GREEN else Color.DARK_GRAY
        batch.draw(whitePixel, attackX, buttonY, buttonWidth, buttonHeight)
        font.color = Color.WHITE
        font.draw(batch, "ATTACK", attackX + 35f, buttonY + 42f)

        batch.color = if (madeMoveThisTurn) Color.ORANGE else Color.DARK_GRAY
        batch.draw(whitePixel, turnX, buttonY, buttonWidth, buttonHeight)
        font.draw(batch, "NEXT TURN", turnX + 15f, buttonY + 42f)

        batch.color = if (!madeMoveThisTurn) Color.GRAY else Color.DARK_GRAY
        batch.draw(whitePixel, skipX, buttonY, buttonWidth, buttonHeight)
        font.draw(batch, "SKIP TURN", skipX + 17f, buttonY + 42f)

        batch.color = when {
            madeMoveThisTurn -> Color.DARK_GRAY
            isFleeing -> Color.YELLOW
            else -> Color.RED
        }
        batch.draw(whitePixel, fleeX, buttonY, buttonWidth, buttonHeight)
        font.draw(batch, if (!isFleeing) "ESCAPE" else "CANCEL", fleeX + 35f, buttonY + 42f)

        val l_btnX = screenWidth / 2 - 100f
        val l_btnY = 300f
        getDmgButtonRect.set(l_btnX, l_btnY, 200f, 60f)
        batch.color = Color.GRAY
        batch.draw(whitePixel, l_btnX, l_btnY, 200f, 60f)
        font.color = Color.WHITE
        font.draw(batch, "getDmg", l_btnX + 30f, l_btnY + 35f)
        fun renderDebuffs(batch: SpriteBatch, player: Player) {
            // Дебаффы игрока
            if (!player.debuffManager.isEmpty()) {
                debuffRenderer.renderDebuffs(
                    batch,
                    player.debuffManager.getAllDebuff(),
                    20f,
                    screenHeight * 0.85f,
                    true
                )
            }
        }
        renderDebuffs(batch, player)

        if (::messageSystem.isInitialized) {
            messageSystem.render(batch)
        }
    }

    private val squareSize = 24 * 2f
    private val padding = 3 * 2f
    private val verticalGap = 15f

    private fun getDmg(player: Player, hp: Int = 5) {
        player.currentHealth = (player.currentHealth - hp).coerceAtLeast(0)
        println("Упс! У игрока осталось ${player.currentHealth} HP")
    }

    private fun drawEnemy(batch: SpriteBatch, whitePixel: Texture, enemy: BattleEnemy, x: Float, y: Float, width: Float, height: Float, isSelected: Boolean) {
        batch.color = if (enemy.isAlive()) Color.RED else Color.DARK_GRAY
        batch.draw(whitePixel, x, y, width, height)

        if (isSelected && enemy.isAlive()) {
            batch.color = Color.YELLOW
            batch.draw(whitePixel, x - 5f, y - 5f, width + 10f, height + 10f)
        }

        font.color = Color.WHITE
        val typeShort = when (enemy.enemyType) {
            EnemyType.NO_TYPE -> ""
            EnemyType.FIRE -> " [Fire]"
            EnemyType.WATER -> " [Water]"
            EnemyType.WIND -> " [Wind]"
            EnemyType.EARTH -> " [Earth]"
            EnemyType.ICE -> " [Ice]"
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
            gameMap.restoreAfterBattle(enemyX, enemyY)
        }
        isActive = false
        madeMoveThisTurn = false
        isFleeing = false
        fleeTurnsLeft = 0
        enemies.clear()

        SoundManager.stopMusic()
        SoundManager.resumePlaylist()
    }
}

