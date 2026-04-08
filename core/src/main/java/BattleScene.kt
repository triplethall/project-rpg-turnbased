package ru.triplethall.rpgturnbased

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import kotlin.random.Random

class BattleScene(
    private val font: BitmapFont,
    private val screenWidth: Float,
    private val screenHeight: Float,
    private val gameMap: GameMap,
    private val BGArena: Texture,
    private val whitePixel: Texture,
    private val barTexture : Texture
) {
    var isActive = false
        private set
    private lateinit var player: Player
    fun setPlayer(player: Player) {
        this.player = player
    }
    private var slimeAtlas: TextureAtlas? = null
    private var slimeIdleAnimation: Animation<TextureRegion>? = null
    private var slimeAttackAnimation: Animation<TextureRegion>? = null
    private var stateTime = 0f
    fun loadAssets() {
        try {
            val atlasPath = "npc/enemy/slime1/slime1-idle.atlas"
            val atlasFile = Gdx.files.internal(atlasPath)

            if (!atlasFile.exists()) {
                Gdx.app.error("BATTLE_DEBUG", "ФАЙЛ НЕ НАЙДЕН: $atlasPath")
                return
            }

            slimeAtlas = TextureAtlas(atlasFile)
            val frames = com.badlogic.gdx.utils.Array<TextureRegion>()

            // ЛОГИРОВАНИЕ: Выводим все имена, которые реально есть в атласе
            Gdx.app.log("BATTLE_DEBUG", "--- Список регионов в атласе ---")
            slimeAtlas?.regions?.forEach {
                Gdx.app.log("BATTLE_DEBUG", "Найдено имя: '${it.name}'")
            }

            // Попытка найти кадры (пробуем разные варианты имен)
            val namesToTry = arrayOf("idle1", "idle2", "idle_1", "idle_2", "idle")

            for (name in namesToTry) {
                val region = slimeAtlas?.findRegion(name)
                if (region != null) {
                    frames.add(region)
                    Gdx.app.log("BATTLE_DEBUG", "Добавлен кадр: $name")
                }
            }

            if (frames.size > 0) {
                slimeIdleAnimation = Animation(0.2f, frames, Animation.PlayMode.LOOP)
                Gdx.app.log("BATTLE_DEBUG", "АНИМАЦИЯ СОЗДАНА. Кадров: ${frames.size}")
            } else {
                Gdx.app.error("BATTLE_DEBUG", "ОШИБКА: Не удалось собрать ни одного кадра для анимации!")
            }

            val attackFrames = com.badlogic.gdx.utils.Array<TextureRegion>()
            attackFrames.add(slimeAtlas?.findRegion("attack1"))
            attackFrames.add(slimeAtlas?.findRegion("attack2"))
            attackFrames.add(slimeAtlas?.findRegion("attack3"))
// Animation.PlayMode.NORMAL (проиграть один раз и остановиться на последнем кадре)
            slimeAttackAnimation = Animation(0.1f, attackFrames, Animation.PlayMode.NORMAL)

        } catch (e: Exception) {
            Gdx.app.error("BATTLE_DEBUG", "КРАШ ПРИ ЗАГРУЗКЕ: ${e.message}")
            e.printStackTrace()
        }
    }
    private val layout = GlyphLayout()
    private var enemies: MutableList<BattleEnemy> = mutableListOf()
    private var enemyIndex = 0
    private val attackButtonRect = Rectangle()
    private var isFleeing = false
    private var fleeTurnsLeft = 0
    private val nextTurnButtonRect = Rectangle()
    private val skipButtonRect = Rectangle()
    private val fleeButtonRect = Rectangle()
    private var enemyX = 0
    private var enemyY = 0
    private var enemyCells: List<Pair<Int, Int>> = emptyList()
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

        playerHealthBar = StatBar(playerBarX, playerHealthY, 400f, 100f, Color.RED)
        playerManaBar = StatBar(playerBarX, playerManaY, 400f, 100f, Color.BLUE)

        updateEnemyBars()
    }

    // ===== НОВЫЙ МЕТОД ДЛЯ БОЯ СО СПИСКОМ ВРАГОВ =====
    fun startBattleWithEnemies(enemiesList: List<BattleEnemy>, cells: List<Pair<Int, Int>>) {
        if (player.currentHealth <= 0) {
            println("player is DEAD, cannot start battle")
            return
        }
        this.enemies = enemiesList.toMutableList()
        this.enemyCells = cells
        this.isActive = true
        this.madeMoveThisTurn = false
        this.isFleeing = false
        this.fleeTurnsLeft = 0
        this.showVictoryScreen = false
        this.showDefeatScreen = false

        messageSystem = BattleMessageSystem(font, screenWidth, screenHeight, whitePixel)
        messageSystem.addMessage("Бой начинается! Врагов: ${enemies.size}", Color.YELLOW)

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
        val rectWidth = 160f
        val space = screenWidth * 0.1f
        val rectY = (screenHeight - rectHeight) / 2
        val rectX = screenWidth - rectWidth - space
        val enemyStartX = rectX - 400f

        val barWidth = rectWidth
        val barHeight = 55f
        val gapAboveEnemy = 15f // Расстояние между врагом и полоской

        enemies.forEachIndexed { index, enemy ->
            var barX = enemyStartX
            var barY = 0f

            // Дублируем логику координат из getEnemyPos
            when (enemies.size) {
                1 -> {
                    barY = rectY - 100f + rectHeight + gapAboveEnemy
                }
                2 -> {
                    val offsetY = 90f
                    if (index == 0) {
                        barY = (rectY - 100f - offsetY) + rectHeight + gapAboveEnemy
                    } else {
                        barX = enemyStartX - 100f
                        barY = (rectY - 100f + offsetY) + rectHeight + gapAboveEnemy
                    }
                }
                3 -> {
                    val offsetY = 100f
                    when (index) {
                        0 -> {
                            barX = enemyStartX - 100f
                            barY = (rectY - 100f - offsetY - 50f) + rectHeight + gapAboveEnemy
                        }
                        1 -> {
                            barY = (rectY - 100f) + rectHeight + gapAboveEnemy
                        }
                        2 -> {
                            barX = enemyStartX - 85f
                            barY = (rectY - 100f + offsetY + 50f) + rectHeight + gapAboveEnemy
                        }
                    }
                }
            }

            enemyBars.add(StatBar(barX, barY, barWidth, barHeight, Color.RED))
        }
    }


    fun handleInput(player: Player): Boolean {
        if (!isActive) return false

        val touchX = Gdx.input.x.toFloat()
        val touchY = Gdx.input.y.toFloat()
        val yInverted = screenHeight - touchY

        if (showVictoryScreen || showDefeatScreen) {
            if (Gdx.input.justTouched() && exitButton.contains(touchX, yInverted)) {
                endBattleAndClearEnemy()
                showVictoryScreen = false
                showDefeatScreen = false
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
        val rectWidth = 160f
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
        SoundManager.playSound("sounds/atack.mp3") // звук атаки
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
        stateTime += Gdx.graphics.deltaTime // Обновляем время анимации


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


            drawBarWithText(batch, playerHealthBar, "${player.currentHealth}/${player.maxHealth}", 2f, barTexture)
            drawBarWithText(batch, playerManaBar, "${player.currentMana}/${player.maxMana}", 2f, barTexture)
// --- СТАТИСТИКА ИГРОКА ---
            font.data.setScale(1.4f) // Делаем текст чуть меньше для компактности
            val statsX = 20f
// Позиция под полоской маны (берем Y мана-бара и отступаем вниз)
            val statsY = playerManaBar.y - 15f

// Подготавливаем тексты (используем свойства из твоего класса Player)
            val damageText = "ATK: ${player.damage}"
            val defenseText = "DEF: ${(player.defense * 100).toInt()}%"
            val levelText = "LVL: ${player.level}"

            // Рисуем с тенью для лучшей видимости
            fun drawStatWithShadow(batch: SpriteBatch, text: String, x: Float, y: Float, color: Color) {
                font.color = Color.BLACK
                font.draw(batch, text, x + 1f, y - 1f) // Тень
                font.color = color
                font.draw(batch, text, x, y)
            }

// Рисуем в ряд или в колонку
            drawStatWithShadow(batch, levelText, statsX, statsY, Color.GOLD)
            drawStatWithShadow(batch, damageText, statsX + 120f, statsY, Color.ORANGE)
            drawStatWithShadow(batch, defenseText, statsX + 260f, statsY, Color.CYAN)

            font.data.setScale(1.0f) // Сброс масштаба




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
                if (enemy.isAlive()) {
                    enemyBars.getOrNull(index)?.let { bar ->
                        // Передаем текущие HP/MaxHP врага в функцию отрисовки бара
                        bar.render(batch, barTexture, whitePixel, enemy.currentHealth, enemy.maxHealth)
                        // Рисуем текст поверх
                        drawBarWithText(batch, bar, "${enemy.currentHealth}/${enemy.maxHealth}", 1.25f, barTexture, drawBar = false)
                    }
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
        font.data.setScale(1f)
        font.color = Color.WHITE
    }
    private fun drawBarWithText(
        batch: SpriteBatch,
        bar: StatBar,
        text: String,
        scale: Float,
        texture: Texture,
        drawBar: Boolean = true
    ) {
        // Рисуем сам бар, если это нужно (для игрока)
        if (drawBar) {
            // Здесь используем актуальные статы игрока в зависимости от цвета бара
            val current = if (bar.color == Color.RED) player.currentHealth else player.currentMana
            val max = if (bar.color == Color.RED) player.maxHealth else player.maxMana
            bar.render(batch, texture, whitePixel, current, max)
        }

        // Настраиваем шрифт
        font.data.setScale(scale)
        layout.setText(font, text)

        // Вычисляем центр
        val textX = bar.x + (bar.width - layout.width) / 2
        val textY = bar.y + (bar.height + layout.height) / 2

        // РИСУЕМ ТЕНЬ (черный текст со смещением на 2 пикселя)
        font.color = Color.BLACK
        font.draw(batch, text, textX + 2f, textY - 2f)

        // РИСУЕМ ОСНОВНОЙ ТЕКСТ (поверх тени)
        font.color = Color.WHITE
        font.draw(batch, text, textX, textY)

        // Сбрасываем масштаб и цвет для остального рендера
        font.data.setScale(1.0f)
        font.color = Color.WHITE
    }

    private val squareSize = 24 * 2f
    private val padding = 3 * 2f
    private val verticalGap = 15f



    private fun drawEnemy(batch: SpriteBatch, whitePixel: Texture, enemy: BattleEnemy, x: Float, y: Float, width: Float, height: Float, isSelected: Boolean) {
        // FIXME: когда бой заканчивался то шрифт становился все меньше и меньше.. оказывается это все было из-за того, что тут не сбрасывался размер шрифта
        // ^^ ЕСЛИ ЧТО НЕ ИСПРАВИЛ ^^
        val oldScaleX = font.data.scaleX
        val oldScaleY = font.data.scaleY
        // Если враг мертв — рисуем серым
        batch.color = if (enemy.isAlive()) Color.RED else Color.DARK_GRAY
        batch.draw(whitePixel, x, y, width, height)

    private fun drawEnemy(
        batch: SpriteBatch,
        whitePixel: Texture,
        enemy: BattleEnemy,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        isSelected: Boolean
    ) {
        if (!enemy.isAlive()) return

        // 1. Рисуем тень/выделение под врагом, если он выбран
        if (isSelected) {
            batch.color = Color(1f, 1f, 0f, 0.4f) // Желтая подсветка
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
        // 2. Получаем кадр анимации
        batch.color = Color.WHITE
        val currentFrame = slimeIdleAnimation?.getKeyFrame(stateTime)

        if (currentFrame != null) {
            // Рисуем спрайт слайма
            batch.draw(currentFrame, x, y, width, height)
        } else {
            // Резервный вариант, если атлас не подгрузился
            batch.color = Color.GREEN
            batch.draw(whitePixel, x, y, width, height)
        }
        font.draw(batch, "${enemy.name}$typeShort", x + 20f, y - 20f)
        font.draw(batch, "${enemy.currentHealth}/${enemy.maxHealth}", x + 20f, y + height - 50f)

        font.data.setScale(oldScaleX, oldScaleY)
        font.color = Color.WHITE
    }

    fun endBattleAndClearEnemy() {
        if (isActive && showVictoryScreen) {
            // Только победа удаляет врагов
            if (enemyCells.isNotEmpty()) {
                // Бой через сундук – удаляем всех врагов из списка
                for ((x, y) in enemyCells) {
                    if (x in 0 until gameMap.width && y in 0 until gameMap.height) {
                        gameMap.restoreAfterBattle(x, y)
                    }
                }
            } else {
                // Обычный бой с одним врагом – удаляем клетку, с которой начался бой
                if (enemyX in 0 until gameMap.width && enemyY in 0 until gameMap.height) {
                    gameMap.restoreAfterBattle(enemyX, enemyY)
                }
            }
        }
        // При побеге или поражении ничего не восстанавливаем – враг остаётся

        isActive = false
        madeMoveThisTurn = false
        isFleeing = false
        fleeTurnsLeft = 0
        enemies.clear()
        enemyCells = emptyList()

        SoundManager.stopMusic()
        SoundManager.resumePlaylist()
    }
}

