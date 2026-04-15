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
    private val getDmgButtonRect = Rectangle()
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
                Gdx.app.error(
                    "BATTLE_DEBUG",
                    "ОШИБКА: Не удалось собрать ни одного кадра для анимации!"
                )
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

    private var waitingForSkillTarget = false
    private var selectedSkill: Skill? = null
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
    private val skillButtons = mutableListOf<Rectangle>()
    private val skillsPerRow = 4
    private val skillButtonSize = 60f
    private lateinit var playerHealthBar: StatBar
    private lateinit var playerManaBar: StatBar
    private lateinit var messageSystem: BattleMessageSystem
    private val enemyBars = mutableListOf<StatBar>()
    private val debuffRenderer = DebuffRenderer(font)
    private var battleLog = mutableListOf<String>()
    private var lastDebuffDamage = 0

    // ===== РАДИАЛЬНОЕ МЕНЮ НАВЫКОВ =====
    private var showSkillWheel = false
    private val skillWheelButtons = mutableListOf<SkillWheelButton>()
    private val skillWheelRadius = 250f
    private val skillWheelCenterX: Float get() = screenWidth / 2
    private val skillWheelCenterY: Float get() = screenHeight / 2
    private val skillWheelButtonSize = 120f
    private var skillWheelSelectedIndex = -1

    // Кнопка для открытия меню навыков
    private val skillsMenuButtonRect = Rectangle()
    private val skillsMenuButtonSize = 80f

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

        playerHealthBar =
            StatBar(playerBarX, playerHealthY, 400f, 100f, Color(0.5f, 0.15f, 0.1f, 1f))
        playerManaBar =
            StatBar(playerBarX, playerManaY, 400f, 100f, Color(0.129f, 0.216f, 0.471f, 1f))

        updateEnemyBars()
        if (player.skills.isEmpty()) {
            player.learnSkillsForClass()
        }
        updateSkillButtons()
    }

    fun updateSkillButtons() {
        skillButtons.clear()
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
        playerHealthBar =
            StatBar(playerBarX, playerHealthY, 400f, 20f, Color(0.478f, 0.220f, 0.008f, 1f))
        playerManaBar =
            StatBar(playerBarX, playerManaY, 400f, 20f, Color(0.129f, 0.216f, 0.471f, 1f))
        updateEnemyBars()

        if (player.skills.isEmpty()) {
            player.learnSkillsForClass()
        }
        updateSkillButtons()
    }

    fun startBattle(enemyCellX: Int, enemyCellY: Int) {
        println("DEBUG: Player class = ${player.playerClass}")
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

    private fun useSkill(skill: Skill) {
        if (!skill.canUse(player, enemies)) {
            messageSystem.addMessage("${skill.name} cant use!", Color.GRAY)
            return
        }
        val result = skill.execute(player, enemies, messageSystem)
        if (result.success) {
            player.currentMana -= skill.manaCost
            madeMoveThisTurn = true
            updateEnemyBars()
            if (enemies.all { !it.isAlive() }) {
                victoryScreen()
            }
            messageSystem.addMessage("${skill.name} used!", Color.CYAN)
        }
    }

    fun handleInput(player: Player): Boolean {
        if (!isActive) return false

        val touchX = Gdx.input.x.toFloat()
        val touchY = Gdx.input.y.toFloat()
        val yInverted = screenHeight - touchY

        // Обновляем ховер для радиального меню
        if (showSkillWheel) {
            updateSkillWheelHover(touchX, yInverted)
        }

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
            // ===== 1. РАДИАЛЬНОЕ МЕНЮ НАВЫКОВ =====
            if (showSkillWheel) {
                return handleSkillWheelInput(touchX, yInverted)
            }

            // ===== 2. КНОПКА ОТКРЫТИЯ МЕНЮ НАВЫКОВ =====
            if (!madeMoveThisTurn && skillsMenuButtonRect.contains(touchX, yInverted)) {
                showSkillWheel = true
                buildSkillWheel()
                return true
            }

            // ===== 3. ВЫБОР ЦЕЛИ ДЛЯ НАВЫКА =====
            if (waitingForSkillTarget && selectedSkill != null) {
                val clickedIndex = getEnemyPos(touchX, yInverted)
                if (clickedIndex != -1 && enemies.getOrNull(clickedIndex)?.isAlive() == true) {
                    val previousIndex = enemyIndex
                    enemyIndex = clickedIndex
                    useSkill(selectedSkill!!)
                    if (enemies.getOrNull(previousIndex)?.isAlive() == true) {
                        enemyIndex = previousIndex
                    }
                    waitingForSkillTarget = false
                    selectedSkill = null
                    return true
                }
            }

            // ===== 4. ВЫБОР ВРАГА =====
            if (!madeMoveThisTurn && !waitingForSkillTarget) {
                val clickedIndex = getEnemyPos(touchX, yInverted)
                if (clickedIndex != -1 && enemies.getOrNull(clickedIndex)?.isAlive() == true) {
                    enemyIndex = clickedIndex
                    return true
                }
            }

            // ===== 5. КНОПКА АТАКИ =====
            if (!madeMoveThisTurn && attackButtonRect.contains(touchX, yInverted)) {
                performAttack()
                return true
            }

            // ===== 6. КНОПКА СЛЕДУЮЩИЙ ХОД =====
            if (madeMoveThisTurn && nextTurnButtonRect.contains(touchX, yInverted)) {
                nextTurn()
                return true
            }

            // ===== 7. КНОПКА ПРОПУСКА ХОДА =====
            if (!madeMoveThisTurn && skipButtonRect.contains(touchX, yInverted)) {
                skipTurn()
                return true
            }
            // ===== 8. КНОПКА ПОБЕГА =====
            if (!madeMoveThisTurn && fleeButtonRect.contains(touchX, yInverted)) {
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
        val dmgWithDef =
            (totalDamage * (1 - target.defense * target.getDefenseMultiplier())).toInt()

        target.takeDamage(dmgWithDef)
        SoundManager.playSound("sounds/atack.mp3") // звук атаки
        messageSystem.addMessage("dealt $dmgWithDef dmg to ${target.name}", Color.GREEN)

        if (!target.isAlive()) {
            messageSystem.addMessage("${target.name} is ded", Color.ORANGE)
            val gainExp = (target.maxHealth * 50 + target.damage * 100).coerceAtLeast(10)
            println("gainExp = $gainExp")
            player.addExperience(gainExp)
            println("player exp = ${player.experience}")
            messageSystem.addMessage("got $gainExp from ${target.name}", Color.GOLD)
            enemies.removeAt(enemyIndex)

            // Проклятый враг накладывает проклятие при смерти
            if (target.enemyType == EnemyType.CURSED) {
                player.applyDebuff(DebuffType.CURSE, 3, 0.7)
                messageSystem.addMessage("${target.name} cursed you!", Color.PURPLE)
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
                        val newHealth =
                            (ally.currentHealth + healAmount).coerceAtMost(ally.maxHealth)
                        val healed = newHealth - ally.currentHealth
                        if (healed > 0) {
                            ally.currentHealth = newHealth
                            messageSystem.addMessage(
                                "${ally.name} restored $healed HP From ${enemy.name}",
                                Color.GREEN
                            )
                        }
                    }
                }
                if (enemy.canHit(player)) {
                    val damage =
                        (enemy.calculateDamage(null, true) * enemy.getDamageMultiplier()).toInt()
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
        if (debuffDmg > 0) {
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
        player.skills.forEach { it.reduceCooldown() } // Уменьшаем кулдауны
        player.currentMana += 10

        if (isActive && enemies.isNotEmpty() && player.currentHealth > 0) {
            enemyTurn()
        }
    }

    private fun flee() {
        if (isFleeing) {
            isFleeing = false
            fleeTurnsLeft = 0
            madeMoveThisTurn = true
            messageSystem.addMessage(
                "canceling attempt of escaping. TURN IS WASTED BTWWWW",
                Color.CORAL
            )
            return
        }
        isFleeing = true
        fleeTurnsLeft = 2
        madeMoveThisTurn = true
        messageSystem.addMessage(
            "player is escaping! $fleeTurnsLeft turns left till escape!",
            Color.CYAN
        )
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
                    drawEnemy(
                        batch,
                        whitePixel,
                        enemies[0],
                        enemyStartX,
                        enemyY,
                        rectWidth,
                        rectHeight,
                        enemyIndex == 0
                    )
                }

                2 -> {
                    val offsetY = 90f
                    val enemyY1 = rectY - 100f - offsetY
                    val enemyY2 = rectY - 100f + offsetY
                    drawEnemy(
                        batch,
                        whitePixel,
                        enemies[0],
                        enemyStartX,
                        enemyY1,
                        rectWidth,
                        rectHeight,
                        enemyIndex == 0
                    )
                    drawEnemy(
                        batch,
                        whitePixel,
                        enemies[1],
                        enemyStartX - 100f,
                        enemyY2,
                        rectWidth,
                        rectHeight,
                        enemyIndex == 1
                    )
                }

                3 -> {
                    val offsetY = 100f
                    val enemyY1 = rectY - 100f - offsetY
                    val enemyY2 = rectY - 100f
                    val enemyY3 = rectY - 100f + offsetY
                    drawEnemy(
                        batch,
                        whitePixel,
                        enemies[0],
                        enemyStartX - 100f,
                        enemyY1 - 50f,
                        rectWidth,
                        rectHeight,
                        enemyIndex == 0
                    )
                    drawEnemy(
                        batch,
                        whitePixel,
                        enemies[1],
                        enemyStartX,
                        enemyY2,
                        rectWidth,
                        rectHeight,
                        enemyIndex == 1
                    )
                    drawEnemy(
                        batch,
                        whitePixel,
                        enemies[2],
                        enemyStartX - 85f,
                        enemyY3 + 50f,
                        rectWidth,
                        rectHeight,
                        enemyIndex == 2
                    )
                }

                else -> {
                    println("idk")
                    return
                }
            }


            drawPlayerBars(batch, playerHealthBar, playerManaBar, barTexture, gap = 1f)
// --- СТАТИСТИКА ИГРОКА ---
            font.data.setScale(1.4f) // Делаем текст чуть меньше для компактности
            val statsX = 20f
            val statsY = playerManaBar.y - 15f

            val damageText = "ATK: ${player.damage}"
            val defenseText = "DEF: ${(player.defense * 100).toInt()}%"
            val levelText = "LVL: ${player.level}"

            fun drawStatWithShadow(
                batch: SpriteBatch,
                text: String,
                x: Float,
                y: Float,
                color: Color
            ) {
                font.color = Color.BLACK
                font.draw(batch, text, x + 1f, y - 1f)
                font.color = color
                font.draw(batch, text, x, y)
            }

            drawStatWithShadow(batch, levelText, statsX, statsY, Color.GOLD)
            drawStatWithShadow(batch, damageText, statsX + 120f, statsY, Color.ORANGE)
            drawStatWithShadow(batch, defenseText, statsX + 260f, statsY, Color.CYAN)

            font.data.setScale(1.0f)

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
                        bar.render(
                            batch,
                            barTexture,
                            whitePixel,
                            enemy.currentHealth,
                            enemy.maxHealth
                        )
                        drawBarWithText(
                            batch,
                            bar,
                            "${enemy.currentHealth}/${enemy.maxHealth}",
                            1.25f,
                            barTexture,
                            drawBar = false
                        )
                    }
                }
            }
        }

        // ===== КНОПКА ОТКРЫТИЯ МЕНЮ НАВЫКОВ =====
        val skillsBtnX = screenWidth - skillsMenuButtonSize - 20f
        val skillsBtnY = screenHeight - skillsMenuButtonSize - 20f
        skillsMenuButtonRect.set(skillsBtnX, skillsBtnY, skillsMenuButtonSize, skillsMenuButtonSize)

        batch.color = Color.PURPLE
        batch.draw(whitePixel, skillsBtnX, skillsBtnY, skillsMenuButtonSize, skillsMenuButtonSize)

        // Белая рамка
        batch.color = Color.WHITE
        batch.draw(whitePixel, skillsBtnX - 2f, skillsBtnY - 2f, skillsMenuButtonSize + 4f, 2f)
        batch.draw(
            whitePixel,
            skillsBtnX - 2f,
            skillsBtnY + skillsMenuButtonSize,
            skillsMenuButtonSize + 4f,
            2f
        )
        batch.draw(whitePixel, skillsBtnX - 2f, skillsBtnY, 2f, skillsMenuButtonSize)
        batch.draw(
            whitePixel,
            skillsBtnX + skillsMenuButtonSize,
            skillsBtnY,
            2f,
            skillsMenuButtonSize
        )

        font.color = Color.WHITE
        font.data.setScale(1.5f)
        layout.setText(font, "⚔")
        font.draw(
            batch, "⚔",
            skillsBtnX + skillsMenuButtonSize / 2 - layout.width / 2,
            skillsBtnY + skillsMenuButtonSize / 2 + layout.height / 2
        )
        font.data.setScale(1f)

        // ===== ОСНОВНЫЕ КНОПКИ =====
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

        // ===== КНОПКА getDmg (отладочная) =====
        val l_btnX = screenWidth / 2 - 100f
        val l_btnY = 300f
        getDmgButtonRect.set(l_btnX, l_btnY, 200f, 60f)
        batch.color = Color.GRAY
        batch.draw(whitePixel, l_btnX, l_btnY, 200f, 60f)
        font.color = Color.WHITE
        font.draw(batch, "getDmg", l_btnX + 30f, l_btnY + 35f)

        // ===== ДЕБАФФЫ ИГРОКА =====
        fun renderDebuffs(batch: SpriteBatch, player: Player) {
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

        // ===== ИНДИКАТОР ОЖИДАНИЯ ЦЕЛИ =====
        if (waitingForSkillTarget && selectedSkill != null) {
            font.color = Color.YELLOW
            font.data.setScale(1.2f)
            val text = ">>> Выбери цель для ${selectedSkill!!.name} <<<"
            layout.setText(font, text)

            // Фон для текста
            batch.color = Color(0f, 0f, 0f, 0.5f)
            batch.draw(
                whitePixel,
                screenWidth / 2 - layout.width / 2 - 10f,
                screenHeight - 60f,
                layout.width + 20f,
                40f
            )

            // Текст
            batch.color = Color.WHITE
            font.color = Color.YELLOW
            font.draw(
                batch, text,
                screenWidth / 2 - layout.width / 2,
                screenHeight - 30f
            )
            font.data.setScale(1f)
        }

        // ===== СООБЩЕНИЯ БОЯ =====
        if (::messageSystem.isInitialized) {
            messageSystem.render(batch)
        }

        // ===== РАДИАЛЬНОЕ МЕНЮ (поверх всего) =====
        drawSkillWheel(batch, whitePixel)

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

    fun drawPlayerBars(
        batch: SpriteBatch,
        hpBar: StatBar,
        mpBar: StatBar,
        bgTexture: Texture,
        gap: Float = 1f
    ) {
        // Общий фон
        val bgY = mpBar.y
        val bgHeight = hpBar.height + mpBar.height + gap
        batch.draw(bgTexture, hpBar.x - 85f, bgY - 40f, hpBar.width * 1.8f, bgHeight * 1.4f)

        // Динамические отступы (в процентах от размеров бара)
        val hpFillYOffset = hpBar.height * 0.23f   // 23% от высоты (было 23f при высоте 100)
        val mpFillYOffset = mpBar.height * 0.45f   // 45% от высоты (было 45f)
        val fillXOffset = hpBar.width * 0.0875f    // 8.75% от ширины (было 35f при ширине 400)
        val fillHeight = hpBar.height / 2.4f       // сохраняем пропорцию

        val hpRatio = (player.currentHealth.toFloat() / player.maxHealth).coerceIn(0f, 1f)
        val mpRatio = (player.currentMana.toFloat() / player.maxMana).coerceIn(0f, 1f)

        val hpFillX = hpBar.x + fillXOffset
        val hpFillY = hpBar.y + hpFillYOffset
        val hpFullW = hpBar.width + 85f
        val hpFillW = hpBar.width * hpRatio + 85f

        val mpFillX = mpBar.x + fillXOffset
        val mpFillY = mpBar.y + mpFillYOffset
        val mpFullW = mpBar.width + 85f
        val mpFillW = mpBar.width * mpRatio + 85f

        batch.color = hpBar.color
        batch.draw(whitePixel, hpFillX, hpFillY, hpFillW, fillHeight)
        batch.color = mpBar.color
        batch.draw(whitePixel, mpFillX, mpFillY, mpFillW, fillHeight)
        batch.color = Color.WHITE

        // Эффекты объёма
        fun addVolume(fillX: Float, fillY: Float, fillW: Float, fillH: Float) {
            batch.color = Color(0f, 0f, 0f, 0.2f)
            batch.draw(whitePixel, fillX, fillY, fillW, fillH / 2f)
            batch.color = Color(1f, 1f, 1f, 0.25f)
            batch.draw(whitePixel, fillX, fillY + fillH * 0.75f, fillW, fillH / 4f)
            batch.color = Color.WHITE
        }
        addVolume(hpFillX, hpFillY, hpFillW, fillHeight)
        addVolume(mpFillX, mpFillY, mpFillW, fillHeight)

        // Переполнение (если статы выше максимума)
        fun drawOverflow(
            fillX: Float,
            fillY: Float,
            fillH: Float,
            fullW: Float,
            current: Int,
            max: Int
        ) {
            if (current > max) {
                val overflowRatio = (current - max).toFloat() / max
                val overflowW = (fullW * overflowRatio).coerceAtMost(fullW)
                batch.color = Color(1f, 1f, 1f, 0.4f)
                batch.draw(whitePixel, fillX, fillY, overflowW, fillH)
                batch.color = Color.WHITE
            }
        }
        drawOverflow(hpFillX, hpFillY, fillHeight, hpFullW, player.currentHealth, player.maxHealth)
        drawOverflow(mpFillX, mpFillY, fillHeight, mpFullW, player.currentMana, player.maxMana)

        // Текст
        font.data.setScale(3f)
        fun drawCentered(text: String, fillX: Float, fillY: Float, fullW: Float, fillH: Float) {
            layout.setText(font, text)
            val cx = fillX + (fullW - layout.width) / 2
            val cy = fillY + (fillH + layout.height) / 2 + 2f
            font.color = Color.BLACK
            font.draw(batch, text, cx + 2f, cy - 2f)
            font.color = Color.WHITE
            font.draw(batch, text, cx, cy)
        }
        drawCentered(
            "${player.currentHealth}/${player.maxHealth}",
            hpFillX,
            hpFillY,
            hpFullW,
            fillHeight
        )
        drawCentered(
            "${player.currentMana}/${player.maxMana}",
            mpFillX,
            mpFillY,
            mpFullW,
            fillHeight
        )

        font.data.setScale(1f)
        font.color = Color.WHITE
    }

    private val squareSize = 24 * 2f
    private val padding = 3 * 2f
    private val verticalGap = 15f

    private fun buildSkillWheel() {
        skillWheelButtons.clear()

        val skills = player.skills
        if (skills.isEmpty()) return

        val angleStep = 360.0 / skills.size

        skills.forEachIndexed { index, skill ->
            val angle = Math.toRadians(index * angleStep - 90.0) // -90 чтобы первый был сверху

            val buttonX = (skillWheelCenterX + skillWheelRadius * Math.cos(angle)).toFloat() - skillWheelButtonSize / 2
            val buttonY = (skillWheelCenterY + skillWheelRadius * Math.sin(angle)).toFloat() - skillWheelButtonSize / 2

            val rect = Rectangle(buttonX, buttonY, skillWheelButtonSize, skillWheelButtonSize)
            skillWheelButtons.add(SkillWheelButton(skill, rect, angle))
        }
    }

    private fun drawSkillWheel(batch: SpriteBatch, whitePixel: Texture) {
        if (!showSkillWheel) return

        // Затемнённый фон
        batch.color = Color(0f, 0f, 0f, 0.7f)
        batch.draw(whitePixel, 0f, 0f, screenWidth, screenHeight)

        // Центральный круг
        batch.color = Color.DARK_GRAY
        batch.draw(whitePixel, skillWheelCenterX - 60f, skillWheelCenterY - 60f, 120f, 120f)

        // Линии от центра к кнопкам
        batch.color = Color.GRAY
        skillWheelButtons.forEach { button ->
            val endX = button.rect.x + skillWheelButtonSize / 2
            val endY = button.rect.y + skillWheelButtonSize / 2
        }

        // Кнопки навыков
        skillWheelButtons.forEachIndexed { index, button ->
            val skill = button.skill

            // Цвет кнопки в зависимости от состояния
            val color = when {
                !skill.canUse(player, enemies) -> Color(0.3f, 0.3f, 0.3f, 1f)
                skill.isOnCooldown() -> Color(0.5f, 0.5f, 0.5f, 1f)
                index == skillWheelSelectedIndex -> Color.GOLD
                else -> Color.SKY
            }

            batch.color = color
            batch.draw(whitePixel, button.rect.x, button.rect.y, button.rect.width, button.rect.height)

            // Рамка кнопки
            batch.color = Color.WHITE
            // Здесь можно нарисовать рамку

            // Текст на кнопке
            font.data.setScale(1.5f)
            font.color = Color.WHITE

            val text = when {
                skill.isOnCooldown() -> "${skill.currentCooldown}"
                skill.manaCost > 0 -> "${skill.manaCost} MP"
                else -> skill.name.take(3)
            }

            layout.setText(font, text)
            val textX = button.rect.x + (button.rect.width - layout.width) / 2
            val textY = button.rect.y + (button.rect.height + layout.height) / 2

            // Тень текста
            font.color = Color.BLACK
            font.draw(batch, text, textX + 2f, textY - 2f)
            font.color = Color.WHITE
            font.draw(batch, text, textX, textY)

            // Название навыка под кнопкой
            font.data.setScale(1.3f)
            layout.setText(font, skill.name)
            val nameX = button.rect.x + (button.rect.width - layout.width) / 2
            val nameY = button.rect.y - 12f
            font.color = Color.BLACK
            font.draw(batch, skill.name, nameX + 1f, nameY - 1f)
            font.color = Color.YELLOW
            font.draw(batch, skill.name, nameX, nameY)
        }

        // Кнопка закрытия в центре
        batch.color = Color.RED
        batch.draw(whitePixel, skillWheelCenterX - 30f, skillWheelCenterY - 30f, 60f, 60f)
        font.data.setScale(0.8f)
        font.color = Color.WHITE
        val closeText = "X"
        layout.setText(font, closeText)
        font.draw(batch, closeText,
            skillWheelCenterX - layout.width / 2,
            skillWheelCenterY + layout.height / 2)

        font.data.setScale(1f)
        batch.color = Color.WHITE
    }

    private fun handleSkillWheelInput(touchX: Float, touchY: Float): Boolean {
        if (!showSkillWheel) return false

        // Проверяем кнопку закрытия в центре
        val centerRect = Rectangle(skillWheelCenterX - 45f, skillWheelCenterY - 45f, 90f, 90f)
        if (centerRect.contains(touchX, touchY)) {
            showSkillWheel = false
            skillWheelSelectedIndex = -1
            return true
        }

        // Проверяем кнопки навыков
        skillWheelButtons.forEachIndexed { index, button ->
            if (button.rect.contains(touchX, touchY)) {
                val skill = button.skill

                if (skill.canUse(player, enemies)) {
                    // Если навык требует цели, ждём выбора врага
                    if (skill is TargetableSkill) {
                        selectedSkill = skill
                        waitingForSkillTarget = true
                        showSkillWheel = false
                        messageSystem.addMessage("Choose target for ${skill.name}", Color.YELLOW)
                    } else {
                        // Иначе сразу применяем
                        useSkill(skill)
                        showSkillWheel = false
                    }
                } else {
                    val reason = when {
                        skill.isOnCooldown() -> "On Cooldown for (${skill.currentCooldown} turns)"
                        player.currentMana < skill.manaCost -> "not enough mana (${skill.manaCost})"
                        player.shouldSkipTurn() -> "You stunned"
                        else -> "cant use"
                    }
                    messageSystem.addMessage("${skill.name}: $reason!", Color.GRAY)
                }
                return true
            }
        }
        // Клик вне меню - закрываем
        showSkillWheel = false
        skillWheelSelectedIndex = -1
        return true
    }

    private fun updateSkillWheelHover(mouseX: Float, mouseY: Float) {
        skillWheelSelectedIndex = -1
        skillWheelButtons.forEachIndexed { index, button ->
            if (button.rect.contains(mouseX, mouseY)) {
                skillWheelSelectedIndex = index
            }
        }
    }

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
        }
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

        //font.data.setScale(oldScaleX, oldScaleY)
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

        // сброс кд навыков
        player.skills.forEach { skill ->
            skill.currentCooldown = 0
        }

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

data class SkillWheelButton(
    val skill: Skill,
    val rect: Rectangle,
    val angle: Double
)
