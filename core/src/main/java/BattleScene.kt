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
    private val barTexture: Texture,
    // НОВЫЕ ТЕКСТУРЫ для кнопок
    private val attackTexture: Texture,
    private val nextTurnTexture: Texture,
    private val fleeTexture: Texture,
    private val logsTexture: Texture
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

            Gdx.app.log("BATTLE_DEBUG", "--- Список регионов в атласе ---")
            slimeAtlas?.regions?.forEach {
                Gdx.app.log("BATTLE_DEBUG", "Найдено имя: '${it.name}'")
            }

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
    private val nextTurnButtonRect = Rectangle()
    private val fleeButtonRect = Rectangle()
    private val logsButtonRect = Rectangle()
    private var isFleeing = false
    private var fleeTurnsLeft = 0
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
    private val battleLog = mutableListOf<String>()
    private var showLogs = false
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

        playerHealthBar = StatBar(playerBarX, playerHealthY, 400f, 100f, Color(0.5f, 0.15f, 0.1f, 1f))
        playerManaBar = StatBar(playerBarX, playerManaY, 400f, 100f, Color(0.129f, 0.216f, 0.471f, 1f))

        updateEnemyBars()
        if (player.skills.isEmpty()) {
            player.learnSkillsForClass()
        }
        updateSkillButtons()

        // Очищаем лог при старте боя
        battleLog.clear()
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
        val msg = "Battle begin! Enemy count: ${enemies.size}"
        messageSystem.addMessage(msg, Color.YELLOW)
        addToBattleLog(msg)

        SoundManager.pausePlaylist()
        SoundManager.playMusic("music/battle.mp3", true)

        val playerBarX = 20f
        val playerHealthY = screenHeight * 0.9f
        val playerManaY = playerHealthY - squareSize - (padding * 2) - verticalGap
        playerHealthBar = StatBar(playerBarX, playerHealthY, 400f, 100f, Color(0.5f, 0.15f, 0.1f, 1f))
        playerManaBar = StatBar(playerBarX, playerManaY, 400f, 100f, Color(0.129f, 0.216f, 0.471f, 1f))

        updateEnemyBars()
        if (player.skills.isEmpty()) {
            player.learnSkillsForClass()
        }
        updateSkillButtons()

        battleLog.clear()
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

    fun startMimicBattle(x: Int, y: Int, mimicSize: Int) {
        val mimicType = when (mimicSize) {
            2 -> Enemy.MEDIUM_MIMIC
            3 -> Enemy.LARGE_MIMIC
            else -> Enemy.SMALL_MIMIC
        }
        val mimic = BattleEnemy.fromType(mimicType)
        startBattleWithEnemies(listOf(mimic), listOf(Pair(x, y)))
        messageSystem.addMessage("MIMIC ATTACKED YOU!", Color.FIREBRICK)
        addToBattleLog("MIMIC ATTACKED YOU!")
    }

    private fun addToBattleLog(msg: String) {
        battleLog.add(msg)
        if (battleLog.size > 20) battleLog.removeAt(0)
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
        val gapAboveEnemy = 15f

        enemies.forEachIndexed { index, enemy ->
            var barX = enemyStartX
            var barY = 0f

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
            addToBattleLog("${skill.name} cant use!")
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
            addToBattleLog("${skill.name} used!")
        }
    }

    fun handleInput(player: Player): Boolean {
        if (!isActive) return false

        val touchX = Gdx.input.x.toFloat()
        val touchY = Gdx.input.y.toFloat()
        val yInverted = screenHeight - touchY

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
            // 1. Радиальное меню
            if (showSkillWheel) {
                return handleSkillWheelInput(touchX, yInverted)
            }

            // 2. Кнопка открытия меню навыков
            if (!madeMoveThisTurn && skillsMenuButtonRect.contains(touchX, yInverted)) {
                showSkillWheel = true
                buildSkillWheel()
                return true
            }

            // 3. Выбор цели для навыка
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

            // 4. Выбор врага
            if (!madeMoveThisTurn && !waitingForSkillTarget) {
                val clickedIndex = getEnemyPos(touchX, yInverted)
                if (clickedIndex != -1 && enemies.getOrNull(clickedIndex)?.isAlive() == true) {
                    enemyIndex = clickedIndex
                    return true
                }
            }

            // 5. Кнопка АТАКИ
            if (!madeMoveThisTurn && attackButtonRect.contains(touchX, yInverted)) {
                performAttack()
                return true
            }

            // 6. Кнопка СЛЕДУЮЩИЙ ХОД
            if (madeMoveThisTurn && nextTurnButtonRect.contains(touchX, yInverted)) {
                nextTurn()
                return true
            }

            // 7. Кнопка ПОБЕГА (flee)
            if (!madeMoveThisTurn && fleeButtonRect.contains(touchX, yInverted)) {
                flee()
                return true
            }

            // 8. Кнопка ЛОГОВ
            if (!madeMoveThisTurn && logsButtonRect.contains(touchX, yInverted)) {
                showLogs = !showLogs
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
        SoundManager.playSound("sounds/victorySound.mp3")
        showVictoryScreen = true
        addToBattleLog("VICTORY!")
    }

    private fun defeatScreen() {
        println("defeat")
        SoundManager.playSound("sounds/battleFail.mp3")
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
        addToBattleLog("DEFEAT...")
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
            addToBattleLog("Can't attack while escaping!")
            return
        }
        val target = enemies[enemyIndex]

        // Уклонение
        if (target.canDodge(isPhysical = true)) {
            messageSystem.addMessage("${target.name} уклонился от атаки!", Color.YELLOW)
            addToBattleLog("${target.name} dodged attack!")
            SoundManager.playSound("sounds/miss.mp3")
            madeMoveThisTurn = true
            return
        }

        if (target.enemyType == EnemyType.WIND) {
            val dodgeChance = 0.25
            if (Random.nextDouble() < dodgeChance) {
                messageSystem.addMessage("${target.name} уклонился от атаки!", Color.YELLOW)
                addToBattleLog("${target.name} dodged attack!")
                madeMoveThisTurn = true
                return
            }
        }

        val baseDamage = (player.damage * player.getDamageMultiplier()).toInt()
        val randomMultiplier = 0.8 + Random.nextDouble() * 0.4
        val totalDamage = (baseDamage * randomMultiplier).toInt()
        var targetDefense = target.defense
        val dmgWithDef = (totalDamage * (1 - targetDefense.coerceAtMost(0.8))).toInt()

        if (target.enemyType == EnemyType.EARTH) {
            targetDefense += 0.3
        }

        val attackHit = target.takeDamage(dmgWithDef, EnemyType.NO_TYPE, false)

        if (!attackHit) {
            messageSystem.addMessage("${target.name} уклонился от атаки!", Color.YELLOW)
            addToBattleLog("${target.name} dodged!")
            madeMoveThisTurn = true
            return
        }

        target.takeDamage(dmgWithDef)
        SoundManager.playSound("sounds/atack.mp3")
        val dmgMsg = "dealt $dmgWithDef dmg to ${target.name}"
        messageSystem.addMessage(dmgMsg, Color.GREEN)
        addToBattleLog(dmgMsg)

        if (!target.isAlive()) {
            if (target.tryResurrect()) {
                messageSystem.addMessage("${target.name} воскрес!", Color.PURPLE)
                addToBattleLog("${target.name} resurrected!")
                return
            }

            messageSystem.addMessage("${target.name} is ded", Color.ORANGE)
            addToBattleLog("${target.name} died")
            val gainExp = (target.maxHealth * 50 + target.damage * 100).coerceAtLeast(10)
            player.addExperience(gainExp)
            messageSystem.addMessage("got $gainExp from ${target.name}", Color.GOLD)
            addToBattleLog("+$gainExp XP")
            enemies.removeAt(enemyIndex)

            if (target.enemyType == EnemyType.CURSED) {
                player.applyDebuff(DebuffType.CURSE, 3, 0.7)
                messageSystem.addMessage("${target.name} cursed you!", Color.PURPLE)
                addToBattleLog("You are cursed!")
            }

            updateEnemyBars()
            if (enemies.isEmpty()) {
                messageSystem.addMessage("victory🕺")
                addToBattleLog("Victory!")
                victoryScreen()
                return
            } else {
                messageSystem.addMessage("${enemies.size} enemies left")
                addToBattleLog("${enemies.size} enemies remain")
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
        addToBattleLog("--- Enemy turn ---")

        enemies.forEach { enemy ->
            if (enemy.isAlive() && player.currentHealth > 0) {
                val skipEnemyTurn = enemy.shouldSkipTurn()

                if (skipEnemyTurn) {
                    messageSystem.addMessage("${enemy.name} skips turn! (debuff)")
                    addToBattleLog("${enemy.name} skips turn")
                    enemy.processDebuffs()
                    return@forEach
                }

                // Лечение HOLY
                if (enemy.enemyType == EnemyType.HOLY && enemy.isAlive()) {
                    val healAmount = (enemy.maxHealth * 0.05).toInt()
                    enemies.filter { it.isAlive() && it != enemy }.forEach { ally ->
                        val newHealth = (ally.currentHealth + healAmount).coerceAtMost(ally.maxHealth)
                        val healed = newHealth - ally.currentHealth
                        if (healed > 0) {
                            ally.currentHealth = newHealth
                            messageSystem.addMessage("${ally.name} restored $healed HP From ${enemy.name}", Color.GREEN)
                            addToBattleLog("${ally.name} healed +$healed HP")
                        }
                    }
                }

                if (enemy.canHit(player)) {
                    val damage = (enemy.calculateDamage(null, true) * enemy.getDamageMultiplier()).toInt()
                    val dmgWithDef = (damage * (1 - player.defense)).toInt()
                    player.currentHealth = (player.currentHealth - dmgWithDef)
                    val dmgMsg = "${enemy.name} dealt you $dmgWithDef dmg"
                    messageSystem.addMessage(dmgMsg, Color.RED)
                    addToBattleLog(dmgMsg)

                    if (enemy.enemyType == EnemyType.DARK) {
                        val stolen = enemy.tryLifeSteal(dmgWithDef)
                        if (stolen > 0) {
                            messageSystem.addMessage("${enemy.name} крадет $stolen здоровья!", Color.PURPLE)
                            addToBattleLog("${enemy.name} steals $stolen HP")
                        }
                    }

                    applyEnemyDebuff(enemy)
                } else {
                    messageSystem.addMessage("${enemy.name} missed", Color.YELLOW)
                    addToBattleLog("${enemy.name} missed")
                }

                enemy.processDebuffs()
            }
        }

        val debuffDmg = player.processDebuffs()
        if (debuffDmg > 0) {
            messageSystem.addMessage("player took $debuffDmg damage!", Color.FIREBRICK)
            addToBattleLog("Debuff damage: -$debuffDmg HP")
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
                addToBattleLog("You escaped!")
                return
            } else {
                messageSystem.addMessage("$fleeTurnsLeft more turns until escape", Color.CYAN)
                addToBattleLog("$fleeTurnsLeft turns to escape")
            }
        }
    }

    private fun applyEnemyDebuff(enemy: BattleEnemy) {
        val debuffChance = when (enemy.enemyType) {
            EnemyType.WATER -> 0.20
            EnemyType.ICE -> 0.25
            EnemyType.ELECTRIC -> 0.20
            EnemyType.CURSED -> 0.15
            EnemyType.POISON -> 0.25
            EnemyType.BLOOD -> 0.30
            EnemyType.FIRE -> 0.25
            else -> 0.0
        }

        val finalChance = debuffChance * (1.0 - player.will)
        if (Random.nextDouble() >= finalChance) return

        when (enemy.enemyType) {
            EnemyType.POISON -> {
                player.applyDebuff(DebuffType.POISON, 3, 1.0, 1)
                messageSystem.addMessage("Игрок отравлен!", Color.GREEN)
                addToBattleLog("Poisoned!")
            }
            EnemyType.FIRE -> {
                player.applyDebuff(DebuffType.BURN, 3, 1.0)
                messageSystem.addMessage("Игрок горит!", Color.FIREBRICK)
                addToBattleLog("Burning!")
            }
            EnemyType.ICE -> {
                player.applyDebuff(DebuffType.FREEZE, 1, 1.0)
                messageSystem.addMessage("Игрок заморожен!", Color.CYAN)
                addToBattleLog("Frozen!")
            }
            EnemyType.ELECTRIC -> {
                player.applyDebuff(DebuffType.PARALYSIS, 2, 0.8)
                messageSystem.addMessage("Игрок парализован!", Color.YELLOW)
                addToBattleLog("Paralyzed!")
            }
            EnemyType.CURSED -> {
                player.applyDebuff(DebuffType.CURSE, 3, 0.7)
                messageSystem.addMessage("Проклятие падает на игрока!", Color.PURPLE)
                addToBattleLog("Cursed!")
            }
            EnemyType.WATER -> {
                player.applyDebuff(DebuffType.WET, 3, 1.0)
                messageSystem.addMessage("Игрок промок! (+25% урон от молний)", Color.CYAN)
                addToBattleLog("Wet!")
            }
            EnemyType.BLOOD -> {
                player.applyDebuff(DebuffType.BLEED, 3, 1.0, 1)
                messageSystem.addMessage("Игрок истекает кровью!", Color.RED)
                addToBattleLog("Bleeding!")
            }
            else -> {}
        }
    }

    private fun nextTurn() {
        madeMoveThisTurn = false
        player.skills.forEach { it.reduceCooldown() }
        var manaRegen = 10

        if (DebuffApplier.hasInfiniteMana(player.debuffManager.getAllDebuff())) {
            manaRegen = player.maxMana
        }

        player.currentMana = (player.currentMana + manaRegen).coerceAtMost(player.maxMana)

        if (manaRegen > 0 && player.currentMana < player.maxMana) {
            messageSystem.addMessage("Восстановлено $manaRegen маны", Color.CYAN)
            addToBattleLog("+$manaRegen MP")
        }

        if (isActive && enemies.isNotEmpty() && player.currentHealth > 0) {
            enemyTurn()
        }
    }

    private fun flee() {
        if (isFleeing) {
            isFleeing = false
            fleeTurnsLeft = 0
            madeMoveThisTurn = true
            messageSystem.addMessage("canceling attempt of escaping. TURN IS WASTED BTWWWW", Color.CORAL)
            addToBattleLog("Escape canceled! Turn wasted.")
            return
        }
        isFleeing = true
        fleeTurnsLeft = 2
        madeMoveThisTurn = true
        messageSystem.addMessage("player is escaping! $fleeTurnsLeft turns left till escape!", Color.CYAN)
        addToBattleLog("Attempting to escape...")
    }

    fun update(delta: Float) {
        if (!isActive) return
        if (::messageSystem.isInitialized) {
            messageSystem.update(delta)
        }
    }

    fun render(batch: SpriteBatch, whitePixel: Texture, player: Player) {
        stateTime += Gdx.graphics.deltaTime

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

        // НОВЫЕ КНОПКИ с текстурами 64x64
        // НОВЫЕ КНОПКИ с текстурами 150x150, расположенные вертикально справа
        val buttonSize = 150f
        val buttonSpacing = 30f
        val rightMargin = 20f
        val bottomMargin = 50f

// Координата X фиксирована – прижата к правому краю
        val startX = screenWidth - buttonSize - rightMargin

        // Размещаем кнопки снизу вверх
        val attackY = bottomMargin
        val nextTurnY = attackY + buttonSize + buttonSpacing
        val fleeY = nextTurnY + buttonSize + buttonSpacing
        val logsY = fleeY + buttonSize + buttonSpacing

        attackButtonRect.set(startX, attackY, buttonSize, buttonSize)
        nextTurnButtonRect.set(startX, nextTurnY, buttonSize, buttonSize)
        fleeButtonRect.set(startX, fleeY, buttonSize, buttonSize)
        logsButtonRect.set(startX, logsY, buttonSize, buttonSize)

        batch.color = Color.WHITE
        batch.draw(attackTexture, attackButtonRect.x, attackButtonRect.y, buttonSize, buttonSize)
        batch.draw(nextTurnTexture, nextTurnButtonRect.x, nextTurnButtonRect.y, buttonSize, buttonSize)
        batch.draw(fleeTexture, fleeButtonRect.x, fleeButtonRect.y, buttonSize, buttonSize)
        batch.draw(logsTexture, logsButtonRect.x, logsButtonRect.y, buttonSize, buttonSize)

        // Далее старый код отрисовки врагов и т.д. (без изменений)
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
                else -> return
            }

            drawPlayerBars(batch, playerHealthBar, playerManaBar, barTexture, gap = 1f)

            // Статистика игрока
            font.data.setScale(1.4f)
            val statsX = 20f
            val statsY = playerManaBar.y - 15f
            val damageText = "ATK: ${player.damage}"
            val defenseText = "DEF: ${(player.defense * 100).toInt()}%"
            val levelText = "LVL: ${player.level}"

            fun drawStatWithShadow(batch: SpriteBatch, text: String, x: Float, y: Float, color: Color) {
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
                        bar.render(batch, barTexture, whitePixel, enemy.currentHealth, enemy.maxHealth)
                        drawBarWithText(batch, bar, "${enemy.currentHealth}/${enemy.maxHealth}", 1.25f, barTexture, drawBar = false)
                    }
                }
            }
        }

        // Кнопка открытия меню навыков
        val skillsBtnX = screenWidth - skillsMenuButtonSize - 20f
        val skillsBtnY = screenHeight - skillsMenuButtonSize - 20f
        skillsMenuButtonRect.set(skillsBtnX, skillsBtnY, skillsMenuButtonSize, skillsMenuButtonSize)

        batch.color = Color.PURPLE
        batch.draw(whitePixel, skillsBtnX, skillsBtnY, skillsMenuButtonSize, skillsMenuButtonSize)
        batch.color = Color.WHITE
        batch.draw(whitePixel, skillsBtnX - 2f, skillsBtnY - 2f, skillsMenuButtonSize + 4f, 2f)
        batch.draw(whitePixel, skillsBtnX - 2f, skillsBtnY + skillsMenuButtonSize, skillsMenuButtonSize + 4f, 2f)
        batch.draw(whitePixel, skillsBtnX - 2f, skillsBtnY, 2f, skillsMenuButtonSize)
        batch.draw(whitePixel, skillsBtnX + skillsMenuButtonSize, skillsBtnY, 2f, skillsMenuButtonSize)
        font.color = Color.WHITE
        font.data.setScale(1.5f)
        layout.setText(font, "⚔")
        font.draw(batch, "⚔",
            skillsBtnX + skillsMenuButtonSize/2 - layout.width/2,
            skillsBtnY + skillsMenuButtonSize/2 + layout.height/2)
        font.data.setScale(1f)

        // --- Отладочная кнопка урона (можно оставить) ---
        val l_btnX = screenWidth / 2 - 100f
        val l_btnY = 300f
        getDmgButtonRect.set(l_btnX, l_btnY, 200f, 60f)
        batch.color = Color.GRAY
        batch.draw(whitePixel, l_btnX, l_btnY, 200f, 60f)
        font.color = Color.WHITE
        font.draw(batch, "getDmg", l_btnX + 30f, l_btnY + 35f)

        // Дебаффы игрока
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

        // Индикатор выбора цели
        if (waitingForSkillTarget && selectedSkill != null) {
            font.color = Color.YELLOW
            font.data.setScale(1.2f)
            val text = ">>> Выбери цель для ${selectedSkill!!.name} <<<"
            layout.setText(font, text)
            batch.color = Color(0f, 0f, 0f, 0.5f)
            batch.draw(whitePixel,
                screenWidth/2 - layout.width/2 - 10f,
                screenHeight - 60f,
                layout.width + 20f,
                40f)
            batch.color = Color.WHITE
            font.color = Color.YELLOW
            font.draw(batch, text,
                screenWidth/2 - layout.width/2,
                screenHeight - 30f)
            font.data.setScale(1f)
        }

        // Система сообщений
        if (::messageSystem.isInitialized) {
            messageSystem.render(batch)
        }

        // Радиальное меню навыков
        drawSkillWheel(batch, whitePixel)

        // Окно логов (если открыто)
        if (showLogs) {
            drawLogWindow(batch, whitePixel)
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
        if (drawBar) {
            val current = if (bar.color == Color.RED) player.currentHealth else player.currentMana
            val max = if (bar.color == Color.RED) player.maxHealth else player.maxMana
            bar.render(batch, texture, whitePixel, current, max)
        }
        font.data.setScale(scale)
        layout.setText(font, text)
        val textX = bar.x + (bar.width - layout.width) / 2
        val textY = bar.y + (bar.height + layout.height) / 2
        font.color = Color.BLACK
        font.draw(batch, text, textX + 2f, textY - 2f)
        font.color = Color.WHITE
        font.draw(batch, text, textX, textY)
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
        val bgY = mpBar.y
        val bgHeight = hpBar.height + mpBar.height + gap
        batch.draw(bgTexture, hpBar.x - 85f, bgY - 40f, hpBar.width * 1.8f, bgHeight * 1.4f)

        val hpFillYOffset = hpBar.height * 0.23f
        val mpFillYOffset = mpBar.height * 0.45f
        val fillXOffset = hpBar.width * 0.0875f
        val fillHeight = hpBar.height / 2.4f

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

        fun addVolume(fillX: Float, fillY: Float, fillW: Float, fillH: Float) {
            batch.color = Color(0f, 0f, 0f, 0.2f)
            batch.draw(whitePixel, fillX, fillY, fillW, fillH / 2f)
            batch.color = Color(1f, 1f, 1f, 0.25f)
            batch.draw(whitePixel, fillX, fillY + fillH * 0.75f, fillW, fillH / 4f)
            batch.color = Color.WHITE
        }
        addVolume(hpFillX, hpFillY, hpFillW, fillHeight)
        addVolume(mpFillX, mpFillY, mpFillW, fillHeight)

        fun drawOverflow(fillX: Float, fillY: Float, fillH: Float, fullW: Float, current: Int, max: Int) {
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
        drawCentered("${player.currentHealth}/${player.maxHealth}", hpFillX, hpFillY, hpFullW, fillHeight)
        drawCentered("${player.currentMana}/${player.maxMana}", mpFillX, mpFillY, mpFullW, fillHeight)
        font.data.setScale(1f)
        font.color = Color.WHITE
    }

    private fun drawLogWindow(batch: SpriteBatch, whitePixel: Texture) {
        val winWidth = 500f
        val winHeight = 400f
        val winX = screenWidth - winWidth - 20f
        val winY = 80f

        batch.color = Color(0f, 0f, 0f, 0.85f)
        batch.draw(whitePixel, winX, winY, winWidth, winHeight)

        font.color = Color.YELLOW
        font.data.setScale(1.2f)
        font.draw(batch, "Battle Log", winX + 10f, winY + winHeight - 20f)

        font.data.setScale(0.9f)
        font.color = Color.WHITE
        val startY = winY + winHeight - 50f
        battleLog.reversed().take(15).forEachIndexed { i, msg ->
            if (i > 14) return@forEachIndexed
            font.draw(batch, msg, winX + 10f, startY - i * 25f)
        }
        font.data.setScale(1f)

        // Кнопка закрытия окна
        val closeSize = 30f
        val closeX = winX + winWidth - closeSize - 5f
        val closeY = winY + winHeight - closeSize - 5f
        batch.color = Color.RED
        batch.draw(whitePixel, closeX, closeY, closeSize, closeSize)
        font.color = Color.WHITE
        font.data.setScale(0.8f)
        font.draw(batch, "X", closeX + 10f, closeY + 20f)
        font.data.setScale(1f)
        batch.color = Color.WHITE
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
            val angle = Math.toRadians(index * angleStep - 90.0)
            val buttonX = (skillWheelCenterX + skillWheelRadius * Math.cos(angle)).toFloat() - skillWheelButtonSize / 2
            val buttonY = (skillWheelCenterY + skillWheelRadius * Math.sin(angle)).toFloat() - skillWheelButtonSize / 2
            val rect = Rectangle(buttonX, buttonY, skillWheelButtonSize, skillWheelButtonSize)
            skillWheelButtons.add(SkillWheelButton(skill, rect, angle))
        }
    }

    private fun drawSkillWheel(batch: SpriteBatch, whitePixel: Texture) {
        if (!showSkillWheel) return
        batch.color = Color(0f, 0f, 0f, 0.7f)
        batch.draw(whitePixel, 0f, 0f, screenWidth, screenHeight)
        batch.color = Color.DARK_GRAY
        batch.draw(whitePixel, skillWheelCenterX - 60f, skillWheelCenterY - 60f, 120f, 120f)
        batch.color = Color.GRAY
        skillWheelButtons.forEach { button ->
            val endX = button.rect.x + skillWheelButtonSize / 2
            val endY = button.rect.y + skillWheelButtonSize / 2
        }
        skillWheelButtons.forEachIndexed { index, button ->
            val skill = button.skill
            val color = when {
                !skill.canUse(player, enemies) -> Color(0.3f, 0.3f, 0.3f, 1f)
                skill.isOnCooldown() -> Color(0.5f, 0.5f, 0.5f, 1f)
                index == skillWheelSelectedIndex -> Color.GOLD
                else -> Color.SKY
            }
            batch.color = color
            batch.draw(whitePixel, button.rect.x, button.rect.y, button.rect.width, button.rect.height)
            batch.color = Color.WHITE
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
            font.color = Color.BLACK
            font.draw(batch, text, textX + 2f, textY - 2f)
            font.color = Color.WHITE
            font.draw(batch, text, textX, textY)
            font.data.setScale(1.3f)
            layout.setText(font, skill.name)
            val nameX = button.rect.x + (button.rect.width - layout.width) / 2
            val nameY = button.rect.y - 12f
            font.color = Color.BLACK
            font.draw(batch, skill.name, nameX + 1f, nameY - 1f)
            font.color = Color.YELLOW
            font.draw(batch, skill.name, nameX, nameY)
        }
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
        val centerRect = Rectangle(skillWheelCenterX - 45f, skillWheelCenterY - 45f, 90f, 90f)
        if (centerRect.contains(touchX, touchY)) {
            showSkillWheel = false
            skillWheelSelectedIndex = -1
            return true
        }
        skillWheelButtons.forEachIndexed { index, button ->
            if (button.rect.contains(touchX, touchY)) {
                val skill = button.skill
                if (skill.canUse(player, enemies)) {
                    if (skill is TargetableSkill) {
                        selectedSkill = skill
                        waitingForSkillTarget = true
                        showSkillWheel = false
                        messageSystem.addMessage("Choose target for ${skill.name}", Color.YELLOW)
                    } else {
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
        if (isSelected) {
            batch.color = Color(1f, 1f, 0f, 0.4f)
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
            EnemyType.DARK -> " [Dark]"
            EnemyType.BLOOD -> " [Blood]"
            EnemyType.BERSERK -> " [Berserk]"
            EnemyType.UNDEAD -> " [Undead]"
            EnemyType.BUNNY -> " [Bunny]"
        }
        batch.color = Color.WHITE
        val currentFrame = slimeIdleAnimation?.getKeyFrame(stateTime)
        if (currentFrame != null) {
            batch.draw(currentFrame, x, y, width, height)
        } else {
            batch.color = Color.GREEN
            batch.draw(whitePixel, x, y, width, height)
        }
        font.draw(batch, "${enemy.name}$typeShort", x + 20f, y - 20f)
        font.draw(batch, "${enemy.currentHealth}/${enemy.maxHealth}", x + 20f, y + height - 50f)
        font.color = Color.WHITE
    }

    fun endBattleAndClearEnemy() {
        if (isActive && showVictoryScreen) {
            if (enemyCells.isNotEmpty()) {
                for ((x, y) in enemyCells) {
                    if (x in 0 until gameMap.width && y in 0 until gameMap.height) {
                        gameMap.restoreAfterBattle(x, y)
                    }
                }
            } else {
                if (enemyX in 0 until gameMap.width && enemyY in 0 until gameMap.height) {
                    gameMap.restoreAfterBattle(enemyX, enemyY)
                }
            }
        }
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
