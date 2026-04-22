package ru.triplethall.rpgturnbased

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import kotlin.math.pow
import kotlin.random.Random

class Player(
    var x: Int = 0,
    var y: Int = 0,
    var playerClass: PlayerClasses = PlayerClasses.ADVENTURIST // класс по умолчанию
) {
    // Базовые характеристики
    var damage: Int = 30                    // Урон
    var mageDamage: Int = 10                // урон магии
    var defense: Double = 0.0               // Защита (процентная)
    var attackSpeed: Double = 1.0           // Скорость (атаки)
    var accuracy: Double = 0.8              // Точность (шанс попадания по врагу)
    var will: Double = 0.1                  // Воля (сопротивление дебафам)
    var corruption: Int = 0                 // Скверна
    var luck: Double = 0.00                 // Удача
    var critChance: Double = 0.05           // крит шанс
    var level: Int = 1                      // Уровень
    var experience: Int = 0                 // Опыт
    val skills = mutableListOf<Skill>()
    var activeSkill: Skill? = null

    var direction: Int = 2          // 1=UP, 2=DOWN, 3=LEFT, 4=RIGHT


    private var currentPixelX: Float = 0f
    private var currentPixelY: Float = 0f
    private var targetPixelX: Float = 0f
    private var targetPixelY: Float = 0f
    private var moveProgress: Float = 1f
    private val moveDuration: Float = 0.25f // Время прохода клетки (сек). Меньше = быстрее
    var isMoving: Boolean = false
        private set

    fun syncRenderPos(cellSize: Int, cellGap: Int) {
        val step = (cellSize + cellGap).toFloat()
        currentPixelX = x * step; currentPixelY = y * step
        targetPixelX = currentPixelX; targetPixelY = currentPixelY
    }

    fun updateMovement(delta: Float) {
        if (!isMoving) return
        moveProgress += delta / moveDuration
        if (moveProgress >= 1f) {
            moveProgress = 1f; isMoving = false
            currentPixelX = targetPixelX; currentPixelY = targetPixelY
        }
    }

    private fun getRenderPosition(cellSize: Int, cellGap: Int): Pair<Float, Float> {
        val step = (cellSize + cellGap).toFloat()
        if (!isMoving) return Pair(x * step, y * step)
        val posX = currentPixelX + (targetPixelX - currentPixelX) * moveProgress
        val posY = currentPixelY + (targetPixelY - currentPixelY) * moveProgress
        return Pair(posX, posY)
    }
    private var moveStateTime: Float = 0f
    private lateinit var playerModel: PlayerMapModel

    fun initMapModel(
        bodyDown: Texture,
        bodyUp: Texture,
        bodyLeft: Texture,
        bodyRight: Texture,
        legTex: Texture,
        armorTex: Texture? = null
    ) {
        playerModel = PlayerMapModel(
            bodyDown = TextureRegion(bodyDown),
            bodyUp = TextureRegion(bodyUp),
            bodyLeft = TextureRegion(bodyLeft),
            bodyRight = TextureRegion(bodyRight),
            legRegion = TextureRegion(legTex),
            armorRegion = armorTex?.let { TextureRegion(it) }
        )
    }
    fun getLightningDamageModifier(): Double {
        return if (debuffManager.hasDebuff(DebuffType.WET)) 1.25 else 1.0
    }

    init {
        playerClass.applyToPlayer(this)
    }

    val debuffManager = DebuffManager()
    private var skipTurn = false

    // Максимальные значения
    var maxMana: Int = 50
    var maxHealth: Int = 200
    var currentHealth: Int = 200
    var currentMana: Int = 50

    val equipment = PlayerEquipment()

    companion object {
        private const val BASE_EXP = 100
        private const val EXP_GROWTH_FACTOR = 1.1
    }

    fun learnSkillsForClass() {
        skills.clear()
        skills.add(DodgeSkill()) // Базовый навык для всех (1)

        when (playerClass) {
            PlayerClasses.KNIGHT -> {
                skills.add(SlashSkill())
                skills.add(KnightValorSkill())
            }
            PlayerClasses.MAGE -> {
                skills.add(FireArrowSkill())
                skills.add(WaterStrikeSkill())
                skills.add(WindSlashSkill())
                skills.add(StoneBulletSkill())
                skills.add(IceSpikeSkill())
                skills.add(LightningBoltSkill())
            }
            PlayerClasses.ASSASSIN -> {
                skills.add(BackstabSkill())
                skills.add(ShurikenThrowSkill())
                skills.add(StealthSkill())
            }
            PlayerClasses.ARCHER -> {
                skills.add(ArrowRainSkill())
                skills.add(AimedShotSkill())
            }
            PlayerClasses.PRIEST -> {
                skills.add(HealSkill())
                skills.add(ResurrectionSkill())
                skills.add(CleanseSkill())
            }
            PlayerClasses.JOKER -> {
                skills.add(CoinTossSkill())
                skills.add(DiceRollSkill())
                skills.add(SlotMachineSkill())
            }
            PlayerClasses.BERSERK -> {
                skills.add(BloodthirstSkill())
                skills.add(SelfHarmSkill())
            }
            PlayerClasses.SHAMAN -> {
                skills.add(WeakCurseSkill())
                skills.add(AlterEgoSkill())
                skills.add(DarkSecretsSkill())
                skills.add(HarvestSkill())
            }
            PlayerClasses.SWORDMASTER -> {
                skills.add(DoubleSlashSkill())
                skills.add(FlurrySkill())
            }
            PlayerClasses.MONARCH -> {
                skills.add(BattleStandardSkill())
                skills.add(SoulEmpowermentSkill())
            }
            else -> {} // ADVENTURIST — только Dodge
        }

        println("DEBUG: Learned ${skills.size} skills for class $playerClass")
        skills.forEach { println("DEBUG: - ${it.name}") }
    }
    // Расчет необходимого опыта для некст левела
    fun getExpForNextLevel(): Int {
        return (BASE_EXP * (EXP_GROWTH_FACTOR.pow(level - 1))).toInt()
    }

    // Получение прогресса опыта в процентах (полоска опыта)
    fun getExpProgress(): Float {
        val expNeeded = getExpForNextLevel()
        return experience.toFloat() / expNeeded.toFloat()
    }

    // Добавление опыта + проверка на повышение уровня
    fun addExperience(amount: Int) {
        experience += amount

        // Проверка достаточно ли опыта для нескольких уровней
        while (experience >= getExpForNextLevel()) {
            levelUp()
        }
    }

    // Повышение уровня
    private fun levelUp() {
        val expNeeded = getExpForNextLevel()
        experience -= expNeeded
        level++
        println("!!!LEVEL UP!!!")
        // Stats up за уровень
        recalculateStats()
        currentMana = maxMana  // восстановление маны за ур
        currentHealth = maxHealth
    }

    // Скверна модификаторы
    fun getCorruptionHealthModifier(): Double {
        return (1.0 - (corruption * 0.05)).coerceAtLeast(0.5)  // Максимум -50% здоровья
    }

    fun getCorruptionDamageModifier(): Double {
        return (1.0 + (corruption * 0.05)).coerceAtMost(1.5)    // Максимум +50% урона
    }

    fun getCorruptionMageDamageModifier(): Double {
        return (1.0 + (corruption * 0.04)).coerceAtMost(1.4)    // Максимум +40% маг. урона
    }

    // скверна при смерти
    fun applyCorruptionOnDeath() {
        corruption++
        println("total crpt: $corruption")
    }

    // Проверка хватает ли опыта
    fun canLevelUp(): Boolean {
        return experience >= getExpForNextLevel()
    }

    // Сброс скверны (например после какого-то события)
    fun removeCorruption(amount: Int) {
        corruption = maxOf(0, corruption - amount)
    }

    fun spawnOnShore(gameMap: GameMap) {
        val random = Random
        val candidates = mutableListOf<Pair<Int, Int>>()

        for (x in 0 until gameMap.width) {
            for (y in 0 until gameMap.height) {
                if (!gameMap.isWalkable(x, y)) continue

                var hasWaterNeighbor = false
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        if (dx == 0 && dy == 0) continue
                        val nx = x + dx
                        val ny = y + dy
                        if (nx !in 0 until gameMap.width || ny !in 0 until gameMap.height) {
                            hasWaterNeighbor = true
                        } else if (!gameMap.isWalkable(nx, ny)) {
                            hasWaterNeighbor = true
                        }
                    }
                }

                if (!hasWaterNeighbor) continue

                val isOnEdge = (x == 0 || x == gameMap.width - 1 || y == 0 || y == gameMap.height - 1)
                if (isOnEdge) {
                    candidates.add(Pair(x, y))
                }
            }
        }

        if (candidates.isNotEmpty()) {
            val (sx, sy) = candidates.random(random)
            this.x = sx
            this.y = sy
        } else {
            this.x = gameMap.width / 2
            this.y = gameMap.height / 2
        }
    }

    fun isAdjacentCardinal(targetX: Int, targetY: Int): Boolean {
        val dx = kotlin.math.abs(targetX - x)
        val dy = kotlin.math.abs(targetY - y)
        return (dx == 1 && dy == 0) || (dx == 0 && dy == 1)
    }

    interface OnEnterForestListener {
        fun onEnterForest(x: Int, y: Int)
    }

    private var onEnterForestListener: OnEnterForestListener? = null

    fun setOnEnterForest(listener: OnEnterForestListener) {
        this.onEnterForestListener = listener
    }

    fun tryMoveTo(targetX: Int, targetY: Int, gameMap: GameMap, cellSize: Int, cellGap: Int): Boolean {
        if (isMoving) return false // Блокировка во время анимации
        if (!isAdjacentCardinal(targetX, targetY)) return false

        if (gameMap.isWalkable(targetX, targetY)) {
            val step = (cellSize + cellGap).toFloat()
            direction = when {
                targetY > y -> 1  // UP
                targetY < y -> 2  // DOWN
                targetX < x -> 3  // LEFT
                targetX > x -> 4  // RIGHT
                else -> direction
            }
            currentPixelX = x * step; currentPixelY = y * step
            targetPixelX = targetX * step; targetPixelY = targetY * step
            moveProgress = 0f; isMoving = true

            x = targetX; y = targetY // Логические координаты меняем сразу

            if (gameMap.collectChest(targetX, targetY)) { /* логика сундука */ }
            if (gameMap.getTerrain(targetX, targetY) == TerrainType.FOREST) {
                if (Random.nextFloat() < 0.1f) onEnterForestListener?.onEnterForest(targetX, targetY)
            }
            return true
        }
        return false
    }

    fun changeClass(newClass: PlayerClasses) {
        playerClass = newClass
        recalculateStats()
    }

    fun recalculateStats() {
        val hpPercent = if (maxHealth > 0) currentHealth.toFloat() / maxHealth else 1f
        val manaPercent = if (maxMana > 0) currentMana.toFloat() / maxMana else 1f

        playerClass.applyToPlayer(this)

        equipment.getAllEquipped().values.forEach { item ->
            damage += item.damageBonus
            mageDamage += item.mageDamageBonus
            maxHealth += item.healthBonus
            maxMana += item.manaBonus
            defense += item.defenseBonus
            attackSpeed += item.attackSpeedBonus
            accuracy += item.accuracyBonus
            will += item.willBonus
            luck += item.luckBonus
            critChance += item.critChanceBonus
        }
        equipment.getRune(0)?.let { rune ->
            damage += rune.damageBonus
            mageDamage += rune.mageDamageBonus
            maxHealth += rune.healthBonus
            maxMana += rune.manaBonus
            defense += rune.defenseBonus
            attackSpeed += rune.attackSpeedBonus
            accuracy += rune.accuracyBonus
            will += rune.willBonus
            luck += rune.luckBonus
            critChance += rune.critChanceBonus
        }
        equipment.getRune(1)?.let { rune ->
            damage += rune.damageBonus
            mageDamage += rune.mageDamageBonus
            maxHealth += rune.healthBonus
            maxMana += rune.manaBonus
            defense += rune.defenseBonus
            attackSpeed += rune.attackSpeedBonus
            accuracy += rune.accuracyBonus
            will += rune.willBonus
            luck += rune.luckBonus
            critChance += rune.critChanceBonus
        }
        currentHealth = (maxHealth * hpPercent).toInt().coerceAtLeast(1)
        currentMana = (maxMana * manaPercent).toInt().coerceAtLeast(0)
    }

    fun render(batch: SpriteBatch, font: BitmapFont, cellSize: Int, cellGap: Int) {
        val (posX, posY) = getRenderPosition(cellSize, cellGap)

        if (isMoving) moveStateTime += Gdx.graphics.deltaTime
        else moveStateTime = 0f

        val state = if (isMoving) PlayerMapModel.State.MOVING else PlayerMapModel.State.IDLE

        if (::playerModel.isInitialized) {
            playerModel.render(
                batch = batch, player = this,
                x = posX, y = posY, // <-- Плавающая позиция
                direction = direction, state = state, stateTime = moveStateTime
            )
        } else {
            font.color = Color.YELLOW
            font.draw(batch, "P", posX + 10f, posY + cellSize - 5f)
        }
    }


    fun applyDebuff(type: DebuffType, duration: Int, intensity: Double = 1.0, stacks: Int = 1) {
        debuffManager.addDebuffs(type, duration, intensity, stacks)
    }

    fun processDebuffs(): Int {
        val debuffs = debuffManager.getAllDebuff()

        // Применяем урон от дебаффов
        val damage = DebuffApplier.Companion.applyDamageDebuffs(
            object : DamageReceiver {
                override fun takeDebuffDamage(amount: Int) {
                    currentHealth = (currentHealth - amount).coerceAtLeast(0)
                }
            },
            debuffs,
            maxHealth
        )

        // Проверяем, нужно ли пропустить ход
        skipTurn = DebuffApplier.Companion.shouldSkipTurn(debuffs)

        // Обновляем длительность дебаффов
        debuffManager.tick()

        return damage
    }

    fun shouldSkipTurn(): Boolean = skipTurn

    fun clearDebuffs() {
        debuffManager.clear()
        skipTurn = false
    }

    fun getDamageMultiplier(): Double {
        val modifiers = DebuffApplier.Companion.getStatModifiers(debuffManager.getAllDebuff())
        return modifiers.damageMultiplier
    }

    fun getSpeedMultiplier(): Double {
        val modifiers = DebuffApplier.Companion.getStatModifiers(debuffManager.getAllDebuff())
        return modifiers.speedMultiplier
    }

    fun canUseMagic(): Boolean {
        return DebuffApplier.Companion.canUseMagic(debuffManager.getAllDebuff())
    }
    private val modelTextures = mutableListOf<Texture>()

    fun loadMapModel() {
        val bodyDown = Texture("player/maptex/upper_down.png")
        val bodyUp = Texture("player/maptex/upper_up.png")
        val bodyLeft = Texture("player/maptex/upper_left.png")
        val bodyRight = Texture("player/maptex/upper_right.png")
        val leg = Texture("player/maptex/leg.png")

        modelTextures.add(bodyDown)
        modelTextures.add(bodyUp)
        modelTextures.add(bodyLeft)
        modelTextures.add(bodyRight)
        modelTextures.add(leg)

        initMapModel(bodyDown, bodyUp, bodyLeft, bodyRight, leg)
    }
}
