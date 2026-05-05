package ru.triplethall.rpgturnbased

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle

data class Item(
    val name: String,
    val description: String,
    var quantity: Int,
    val isEquippable: Boolean,
    var isEquipped: Boolean = false,
    val equipmentItem: EquipmentItem? = null
)

class Inventory(
    private val font: BitmapFont,
    private val screenWidth: Float,
    private val screenHeight: Float,
    private var invbutt: Texture
) {
    var isVisible = false
        private set

    // Слоты экипировки
    private val helmetSlotRect = Rectangle()
    private val chestplateSlotRect = Rectangle()
    private val bootsSlotRect = Rectangle()
    private val shieldSlotRect = Rectangle()
    private val weaponSlotRect = Rectangle()
    private val rune1SlotRect = Rectangle()
    private val rune2SlotRect = Rectangle()

    private var isItemDetailsVisible = false

    // Кнопка открытия инвентаря
    private val inventoryButtonRect = Rectangle(140f, screenHeight - 140f, 120f, 120f)

    // Элементы UI
    private val closeButtonRect = Rectangle()
    private val itemRects = mutableListOf<Rectangle>()
    private val selectedItemRect = Rectangle()
    private val equipButtonRect = Rectangle()
    private val useButtonRect = Rectangle()
    private val dropButtonRect = Rectangle()
    private val equipmentSlots = mutableMapOf<EquipmentType, Rectangle>()
    private val runeSlots = mutableListOf<Rectangle>()

    private val items = mutableListOf<Item>()
    private var selectedItem: Item? = null
    private var selectedItemIndex = -1

    private var isEquipPressed = false
    private var isUsePressed = false
    private var isDropPressed = false
    private var currentPlayer: Player? = null

    init {
        initTestItems()
        initEquipmentSlots()
    }

    private fun initEquipmentSlots() {
        equipmentSlots[EquipmentType.HELMET] = helmetSlotRect
        equipmentSlots[EquipmentType.CHESTPLATE] = chestplateSlotRect
        equipmentSlots[EquipmentType.BOOTS] = bootsSlotRect
        equipmentSlots[EquipmentType.SHIELD] = shieldSlotRect
        equipmentSlots[EquipmentType.WEAPON] = weaponSlotRect
        runeSlots.clear()
        runeSlots.addAll(listOf(rune1SlotRect, rune2SlotRect))
    }

    private fun initTestItems() {
        items.addAll(listOf(
            Item("Health Potion", "Restores 50 HP", 3, false),
            Item("Mana Potion", "Restores 30 MP", 2, false),
            Item("Wooden Sword", "A basic sword. Damage +10", 1, true, false, EquipmentDatabase.WOODEN_SWORD),
            Item("Leather Armor", "Basic armor. Defense +15%", 1, true, false, EquipmentDatabase.LEATHER_CHESTPLATE),
            Item("Iron Helmet", "Basic helmet. Defense +10%", 1, true, false, EquipmentDatabase.IRON_HELMET),
            Item("Iron Boots", "Basic boots. Defense +10%", 1, true, false, EquipmentDatabase.IRON_BOOTS),
            Item("Wooden Shield", "Basic shield. Defense +5%", 1, true, false, EquipmentDatabase.WOODEN_SHIELD),
            Item("Defence rune", "+20% armor in exchange for -20% max health", 1, true, false, EquipmentDatabase.DEFENCE_RUNE)
        ))
    }


    private fun updateAllInteractiveRects() {
        val panelW = 1200f
        val panelH = 700f
        val panelX = (screenWidth - panelW) / 2
        val panelY = (screenHeight - panelH) / 2

        val closeBtnW = 150f
        val closeBtnH = 90f
        val closeBtnX = panelX + panelW - closeBtnW - 20f
        val closeBtnY = panelY + 10f   // снизу панели
        closeButtonRect.set(closeBtnX, closeBtnY, closeBtnW, closeBtnH)

        updateItemRects(panelX, panelY, panelW, panelH)
        updateEquipmentSlotsRects(panelX, panelY, panelW, panelH)

        if (isItemDetailsVisible && selectedItem != null) {
            val detailsW = 400f
            val detailsH = 350f
            val detailsX = panelX + panelW - detailsW - 400f
            val detailsY = panelY + 100f
            val buttonY = detailsY + 50f
            val buttonW = 120f
            val buttonH = 50f

            if (selectedItem!!.isEquippable) {
                equipButtonRect.set(detailsX + 50f, buttonY, buttonW, buttonH)
            } else {
                useButtonRect.set(detailsX + 50f, buttonY, buttonW, buttonH)
            }
            dropButtonRect.set(detailsX + 200f, buttonY, buttonW, buttonH)
        }
    }

    private fun updateItemRects(panelX: Float, panelY: Float, panelW: Float, panelH: Float) {
        itemRects.clear()
        val startX = panelX + 50f
        val startY = panelY + panelH - 150f
        val itemSize = 80f
        val padding = 20f
        val itemsPerRow = 4

        items.forEachIndexed { index, _ ->
            val row = index / itemsPerRow
            val col = index % itemsPerRow
            val x = startX + col * (itemSize + padding)
            val y = startY - row * (itemSize + padding)
            itemRects.add(Rectangle(x, y - itemSize, itemSize, itemSize))
        }
    }

    private fun updateEquipmentSlotsRects(panelX: Float, panelY: Float, panelW: Float, panelH: Float) {
        val equipPanelX = panelX + panelW - 350f
        val equipPanelY = panelY + 50f
        val slotSize = 60f
        val slotStartX = equipPanelX + 50f
        var slotY = equipPanelY + 600f - 80f   // equipPanelH = 600f

        val mainSlots = listOf(
            EquipmentType.HELMET to helmetSlotRect,
            EquipmentType.CHESTPLATE to chestplateSlotRect,
            EquipmentType.BOOTS to bootsSlotRect,
            EquipmentType.SHIELD to shieldSlotRect,
            EquipmentType.WEAPON to weaponSlotRect
        )

        for ((type, rect) in mainSlots) {
            rect.set(slotStartX, slotY - slotSize, slotSize, slotSize)
            slotY -= (slotSize + 15f)
            if (type == EquipmentType.WEAPON) slotY -= 10f
        }

        for (i in runeSlots.indices) {
            val rect = runeSlots[i]
            rect.set(slotStartX, slotY - slotSize, slotSize, slotSize)
            slotY -= (slotSize + 15f)
        }
    }

    fun toggle() {
        isVisible = !isVisible
        if (!isVisible) {
            isItemDetailsVisible = false
            selectedItem = null
            selectedItemIndex = -1
        }
    }

    fun handleInput(player: Player) {
        val touchX = Gdx.input.x.toFloat()
        val touchY = Gdx.input.y.toFloat()
        val invertedY = screenHeight - touchY
        currentPlayer = player

        if (!isVisible) {
            if (Gdx.input.justTouched() && inventoryButtonRect.contains(touchX, invertedY)) {
                toggle()
            }
            return
        }

        updateAllInteractiveRects()

        // 1. Кнопка закрытия
        if (Gdx.input.justTouched() && closeButtonRect.contains(touchX, invertedY)) {
            toggle()
            return
        }

        // 2. Слоты экипировки
        for ((type, rect) in equipmentSlots) {
            if (rect.contains(touchX, invertedY)) {
                val equippedItem = player.equipment.getEquipped(type)
                if (equippedItem != null) {
                    unequipItemByType(type, player)
                }
                return
            }
        }

        // 3. Слоты рун
        for ((index, rect) in runeSlots.withIndex()) {
            if (rect.contains(touchX, invertedY)) {
                val rune = player.equipment.getRune(index)
                if (rune != null) {
                    unequipRune(index, player)
                }
                return
            }
        }

        // 4. Действия с выбранным предметом
        if (Gdx.input.justTouched()) {
            if (isItemDetailsVisible && selectedItem != null) {
                if (selectedItem!!.isEquippable) {
                    if (equipButtonRect.contains(touchX, invertedY)) {
                        isEquipPressed = true
                        handleEquipItem(selectedItem!!, player)
                        return
                    }
                } else {
                    if (useButtonRect.contains(touchX, invertedY)) {
                        isUsePressed = true
                        handleUseItem(selectedItem!!, player)
                        return
                    }
                }
                if (dropButtonRect.contains(touchX, invertedY)) {
                    isDropPressed = true
                    handleDropItem(selectedItem!!)
                    return
                }
            }

            // 5. Выбор предмета из списка
            for ((index, rect) in itemRects.withIndex()) {
                if (index < items.size && rect.contains(touchX, invertedY)) {
                    selectedItem = items[index]
                    selectedItemIndex = index
                    isItemDetailsVisible = true
                    selectedItemRect.set(rect)
                    return
                }
            }
        } else {
            isEquipPressed = false
            isUsePressed = false
            isDropPressed = false
        }
    }

    // ==================== ОТРИСОВКА ====================
    fun render(batch: SpriteBatch, whitePixel: Texture, player: Player) {
        currentPlayer = player
        if (!isVisible) return

        // Затемнение фона
        batch.color = Color(0f, 0f, 0f, 0.7f)
        batch.draw(whitePixel, 0f, 0f, screenWidth, screenHeight)

        val panelW = 1200f
        val panelH = 700f
        val panelX = (screenWidth - panelW) / 2
        val panelY = (screenHeight - panelH) / 2

        // Основная панель
        batch.color = Color.DARK_GRAY
        batch.draw(whitePixel, panelX, panelY, panelW, panelH)

        font.color = Color.YELLOW
        font.data.setScale(2f)
        font.draw(batch, "INVENTORY", panelX + 20f, panelY + panelH - 20f)

        renderItems(batch, whitePixel, panelX, panelY, panelW, panelH)
        renderEquipmentPanel(batch, whitePixel, panelX, panelY, panelW, panelH, player)

        // Кнопка закрытия (пересчёт и отрисовка – гарантированно последней)
        val closeBtnW = 150f
        val closeBtnH = 90f
        val closeBtnX = panelX + panelW - closeBtnW - 20f
        val closeBtnY = panelY + 10f
        closeButtonRect.set(closeBtnX, closeBtnY, closeBtnW, closeBtnH)

        batch.color = Color.RED
        batch.draw(whitePixel, closeBtnX, closeBtnY, closeBtnW, closeBtnH)
        font.color = Color.WHITE
        font.data.setScale(1.2f)
        font.draw(batch, "X", closeBtnX + closeBtnW / 2 - 15f, closeBtnY + closeBtnH / 2 + 10f)

        if (isItemDetailsVisible && selectedItem != null) {
            renderItemDetails(batch, whitePixel, panelX, panelY, panelW, panelH, selectedItem!!)
        }

        batch.color = Color.WHITE
        font.data.setScale(1f)
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================
    private fun unequipItemByType(type: EquipmentType, player: Player) {
        val item = player.equipment.getEquipped(type) ?: return
        if (player.equipment.unequip(type, player)) {
            val existingItem = items.find { it.equipmentItem?.id == item.id }
            if (existingItem != null) {
                existingItem.quantity++
                existingItem.isEquipped = false
            } else {
                items.add(Item(
                    name = item.name,
                    description = item.description,
                    quantity = 1,
                    isEquippable = true,
                    isEquipped = false,
                    equipmentItem = item
                ))
            }
        }
    }

    private fun unequipRune(slotIndex: Int, player: Player) {
        val rune = player.equipment.getRune(slotIndex) ?: return
        if (player.equipment.unequipRune(slotIndex, player)) {
            val existingItem = items.find { it.equipmentItem?.id == rune.id }
            if (existingItem != null) {
                existingItem.quantity++
                existingItem.isEquipped = false
            } else {
                items.add(Item(
                    name = rune.name,
                    description = rune.description,
                    quantity = 1,
                    isEquippable = true,
                    isEquipped = false,
                    equipmentItem = rune
                ))
            }
        }
    }

    private fun handleEquipItem(item: Item, player: Player) {
        if (!item.isEquippable) return
        val equipmentItem = item.equipmentItem
        if (equipmentItem == null) {
            // Руна
            if (item.isEquipped) {
                for (i in 0..1) {
                    if (player.equipment.getRune(i)?.name == item.name) {
                        player.equipment.unequipRune(i, player)
                        item.isEquipped = false
                        break
                    }
                }
            } else {
                for (i in 0..1) {
                    if (player.equipment.getRune(i) == null) {
                        val runeItem = item.equipmentItem ?: continue
                        if (player.equipment.equipRune(runeItem, i, player)) {
                            item.isEquipped = true
                            item.quantity--
                            if (item.quantity <= 0) {
                                items.remove(item)
                                isItemDetailsVisible = false
                                selectedItem = null
                                selectedItemIndex = -1
                            }
                        }
                        break
                    }
                }
            }
            return
        }

        // Обычное снаряжение
        if (!equipmentItem.canBeUsedBy(player.playerClass)) {
            println("You cannot equip this item with your class: ${player.playerClass.displayName}")
            return
        }
        if (equipmentItem.level > player.level) {
            println("Your level is too low to equip this item. Required: ${equipmentItem.level}")
            return
        }
        if (item.isEquipped) {
            player.equipment.unequip(equipmentItem.type, player)
            item.isEquipped = false
            println("Unequipped: ${item.name}")
            return
        }
        if (player.equipment.equip(equipmentItem, player, player.playerClass)) {
            item.isEquipped = true
            item.quantity--
            if (item.quantity <= 0) {
                items.remove(item)
                isItemDetailsVisible = false
                selectedItem = null
                selectedItemIndex = -1
            }
            println("Equipped: ${item.name}")
        } else {
            println("Failed to equip: ${item.name}. Slot may be occupied or item incompatible.")
        }
    }

    private fun handleUseItem(item: Item, player: Player) {
        if (!item.isEquippable && item.quantity > 0) {
            when (item.name) {
                "Health Potion" -> player.currentHealth = minOf(player.currentHealth + 50, player.maxHealth)
                "Mana Potion" -> player.currentMana = minOf(player.currentMana + 30, player.maxMana)
            }
            item.quantity--
            if (item.quantity <= 0) {
                items.remove(item)
                isItemDetailsVisible = false
                selectedItem = null
                selectedItemIndex = -1
            }
        }
    }

    private fun handleDropItem(item: Item) {
        items.remove(item)
        isItemDetailsVisible = false
        selectedItem = null
        selectedItemIndex = -1
    }

    private fun renderEquipmentPanel(batch: SpriteBatch, whitePixel: Texture, panelX: Float, panelY: Float, panelW: Float, panelH: Float, player: Player) {
        val equipPanelX = panelX + panelW - 350f
        val equipPanelY = panelY + 50f
        val equipPanelW = 320f
        val equipPanelH = 600f

        batch.color = Color(0.3f, 0.3f, 0.3f, 0.9f)
        batch.draw(whitePixel, equipPanelX, equipPanelY, equipPanelW, equipPanelH)

        font.color = Color.YELLOW
        font.data.setScale(1.2f)
        font.draw(batch, "EQUIPMENT", equipPanelX + 100f, equipPanelY + equipPanelH - 15f)

        val slotSize = 60f
        val slotStartX = equipPanelX + 50f
        var slotY = equipPanelY + equipPanelH - 80f

        val mainSlots = listOf(
            Triple(EquipmentType.HELMET, "HELMET", helmetSlotRect),
            Triple(EquipmentType.CHESTPLATE, "CHEST", chestplateSlotRect),
            Triple(EquipmentType.BOOTS, "BOOTS", bootsSlotRect),
            Triple(EquipmentType.SHIELD, "SHIELD", shieldSlotRect),
            Triple(EquipmentType.WEAPON, "WEAPON", weaponSlotRect)
        )

        for ((type, label, rect) in mainSlots) {
            val equippedItem = player.equipment.getEquipped(type)
            rect.set(slotStartX, slotY - slotSize, slotSize, slotSize)

            batch.color = Color(0.2f, 0.2f, 0.2f, 1f)
            batch.draw(whitePixel, rect.x, rect.y, rect.width, rect.height)

            batch.color = Color.GOLD
            val thickness = 2f
            batch.draw(whitePixel, rect.x, rect.y, rect.width, thickness)
            batch.draw(whitePixel, rect.x, rect.y + rect.height, rect.width, thickness)
            batch.draw(whitePixel, rect.x, rect.y, thickness, rect.height)
            batch.draw(whitePixel, rect.x + rect.width, rect.y, thickness, rect.height)

            if (equippedItem != null) {
                batch.color = Color.ORANGE
                batch.draw(whitePixel, rect.x + 10f, rect.y + 10f, rect.width - 20f, rect.height - 20f)
                font.color = Color.WHITE
                font.data.setScale(0.8f)
                font.draw(batch, equippedItem.name.take(2), rect.x + rect.width / 2 - 10f, rect.y + rect.height / 2 + 5f)
            }

            font.color = Color.LIGHT_GRAY
            font.data.setScale(0.8f)
            font.draw(batch, label, slotStartX + slotSize + 10f, slotY - slotSize / 2 + 10f)

            if (equippedItem != null) {
                font.color = Color.GREEN
                font.data.setScale(0.7f)
                val statText = if (equippedItem.damageBonus != 0) "ATK: +${equippedItem.damageBonus}"
                else if (equippedItem.defenseBonus != 0.0) "DEF: +${(equippedItem.defenseBonus * 100).toInt()}%"
                else ""
                if (statText.isNotEmpty()) {
                    font.draw(batch, statText, slotStartX + slotSize + 10f, slotY - slotSize / 2 - 10f)
                }
            }

            slotY -= (slotSize + 15f)
            if (type == EquipmentType.WEAPON) slotY -= 10f
        }

        for ((index, rect) in runeSlots.withIndex()) {
            val label = "RUNE ${index + 1}"
            val equippedItem = player.equipment.getRune(index)
            rect.set(slotStartX, slotY - slotSize, slotSize, slotSize)

            batch.color = Color(0.2f, 0.2f, 0.2f, 1f)
            batch.draw(whitePixel, rect.x, rect.y, rect.width, rect.height)

            batch.color = Color.GOLD
            val thickness = 2f
            batch.draw(whitePixel, rect.x, rect.y, rect.width, thickness)
            batch.draw(whitePixel, rect.x, rect.y + rect.height, rect.width, thickness)
            batch.draw(whitePixel, rect.x, rect.y, thickness, rect.height)
            batch.draw(whitePixel, rect.x + rect.width, rect.y, thickness, rect.height)

            if (equippedItem != null) {
                batch.color = Color.ORANGE
                batch.draw(whitePixel, rect.x + 10f, rect.y + 10f, rect.width - 20f, rect.height - 20f)
                font.color = Color.WHITE
                font.data.setScale(0.8f)
                font.draw(batch, equippedItem.name.take(2), rect.x + rect.width / 2 - 10f, rect.y + rect.height / 2 + 5f)
            }

            font.color = Color.LIGHT_GRAY
            font.data.setScale(0.8f)
            font.draw(batch, label, slotStartX + slotSize + 10f, slotY - slotSize / 2 + 10f)

            if (equippedItem != null) {
                font.color = Color.GREEN
                font.data.setScale(0.7f)
                val statText = if (equippedItem.damageBonus != 0) "ATK: +${equippedItem.damageBonus}"
                else if (equippedItem.defenseBonus != 0.0) "DEF: +${(equippedItem.defenseBonus * 100).toInt()}%"
                else ""
                if (statText.isNotEmpty()) {
                    font.draw(batch, statText, slotStartX + slotSize + 10f, slotY - slotSize / 2 - 10f)
                }
            }

            slotY -= (slotSize + 15f)
        }

        renderPlayerStats(batch, whitePixel, equipPanelX, equipPanelY, equipPanelW, equipPanelH, player)
    }

    private fun renderPlayerStats(batch: SpriteBatch, whitePixel: Texture, panelX: Float, panelY: Float, panelW: Float, panelH: Float, player: Player) {
        val statsX = panelX - 250f
        val statsY = panelY + 10f

        font.color = Color.CYAN
        font.data.setScale(0.9f)
        font.draw(batch, "STATS", statsX + 20f, statsY + 100f)
        font.color = Color.WHITE
        font.draw(batch, "HP: ${player.currentHealth}/${player.maxHealth}", statsX + 20f, statsY + 75f)
        font.draw(batch, "MP: ${player.currentMana}/${player.maxMana}", statsX + 20f, statsY + 55f)
        font.draw(batch, "ATK: ${player.damage}", statsX + 20f, statsY + 35f)
        font.draw(batch, "M.ATK: ${player.mageDamage}", statsX + 20f, statsY + 15f)
        font.draw(batch, "DEF: ${(player.defense * 100).toInt()}%", statsX + 150f, statsY + 75f)
        font.draw(batch, "CRIT: ${(player.critChance * 100).toInt()}%", statsX + 150f, statsY + 55f)
        font.draw(batch, "ACC: ${(player.accuracy * 100).toInt()}%", statsX + 150f, statsY + 35f)
        font.draw(batch, "SPD: ${(player.attackSpeed * 100).toInt()}%", statsX + 150f, statsY + 15f)
        font.draw(batch, "LUCK: ${(player.luck * 100).toInt()}%", statsX + 20f, statsY - 5f)
        font.draw(batch, "CORRUPT: ${player.corruption}", statsX + 150f, statsY - 5f)
    }

    private fun renderItems(batch: SpriteBatch, whitePixel: Texture, panelX: Float, panelY: Float, panelW: Float, panelH: Float) {
        val startX = panelX + 50f
        val startY = panelY + panelH - 150f
        val itemSize = 80f
        val padding = 20f
        val itemsPerRow = 4

        itemRects.clear()

        for ((index, item) in items.withIndex()) {
            val row = index / itemsPerRow
            val col = index % itemsPerRow
            val x = startX + col * (itemSize + padding)
            val y = startY - row * (itemSize + padding)
            val itemRect = Rectangle(x, y - itemSize, itemSize, itemSize)
            itemRects.add(itemRect)

            if (index == selectedItemIndex) {
                batch.color = Color.YELLOW
                batch.draw(whitePixel, x - 5f, y - itemSize - 5f, itemSize + 10f, itemSize + 10f)
            }

            batch.color = if (item.isEquipped) Color.GOLD else Color.GRAY
            batch.draw(whitePixel, x, y - itemSize, itemSize, itemSize)

            batch.color = when {
                item.isEquippable -> Color.ORANGE
                item.name.contains("Potion") -> Color.RED
                else -> Color.CYAN
            }
            batch.draw(whitePixel, x + 10f, y - itemSize + 10f, itemSize - 20f, itemSize - 20f)

            if (item.quantity > 1) {
                font.color = Color.WHITE
                font.data.setScale(1f)
                font.draw(batch, "x${item.quantity}", x + itemSize - 30f, y - 10f)
            }

            if (item.isEquipped) {
                font.color = Color.GREEN
                font.data.setScale(1f)
                font.draw(batch, "E", x + 5f, y - 15f)
            }

            font.color = Color.WHITE
            font.data.setScale(0.6f)
            val shortName = if (item.name.length > 12) item.name.take(10) + ".." else item.name
            font.draw(batch, shortName, x + 5f, y - itemSize + 15f)
        }
    }

    private fun renderItemDetails(batch: SpriteBatch, whitePixel: Texture, panelX: Float, panelY: Float, panelW: Float, panelH: Float, item: Item) {
        val detailsW = 400f
        val detailsH = 350f
        val detailsX = panelX + panelW - detailsW - 400f
        val detailsY = panelY + 100f

        batch.color = Color(0.2f, 0.2f, 0.2f, 0.95f)
        batch.draw(whitePixel, detailsX, detailsY, detailsW, detailsH)

        font.color = Color.YELLOW
        font.data.setScale(1.5f)
        font.draw(batch, item.name, detailsX + 20f, detailsY + detailsH - 20f)

        font.color = Color.WHITE
        font.data.setScale(1.2f)
        font.draw(batch, item.description, detailsX + 20f, detailsY + detailsH - 60f)

        val buttonY = detailsY + 50f
        val buttonW = 120f
        val buttonH = 50f

        if (item.isEquippable) {
            val equipX = detailsX + 50f
            equipButtonRect.set(equipX, buttonY, buttonW, buttonH)
            val isItemEquipped = item.isEquipped || (item.equipmentItem?.let { eqItem ->
                currentPlayer?.equipment?.getEquipped(eqItem.type)?.id == eqItem.id
            } == true)

            batch.color = if (isEquipPressed) Color.LIGHT_GRAY else Color(0.3f, 0.6f, 0.3f, 1f)
            batch.draw(whitePixel, equipX, buttonY, buttonW, buttonH)
            font.color = Color.WHITE
            font.draw(batch, if (isItemEquipped) "UNEQUIP" else "EQUIP", equipX + 15f, buttonY + 32f)
        } else {
            val useX = detailsX + 50f
            useButtonRect.set(useX, buttonY, buttonW, buttonH)
            batch.color = if (isUsePressed) Color.LIGHT_GRAY else Color(0.3f, 0.6f, 0.3f, 1f)
            batch.draw(whitePixel, useX, buttonY, buttonW, buttonH)
            font.color = Color.WHITE
            font.draw(batch, "USE", useX + 35f, buttonY + 32f)
        }

        val dropX = detailsX + 200f
        dropButtonRect.set(dropX, buttonY, buttonW, buttonH)
        batch.color = if (isDropPressed) Color.LIGHT_GRAY else Color(0.8f, 0.3f, 0.3f, 1f)
        batch.draw(whitePixel, dropX, buttonY, buttonW, buttonH)
        font.color = Color.WHITE
        font.draw(batch, "DROP", dropX + 30f, buttonY + 32f)

        font.data.setScale(1f)

        if (item.isEquippable && item.equipmentItem != null) {
            val eqItem = item.equipmentItem!!
            font.data.setScale(0.9f)
            font.color = Color.LIGHT_GRAY
            font.draw(batch, "Level required: ${eqItem.level}", detailsX + 20f, detailsY + detailsH - 95f)
            font.draw(batch, "Class: ${eqItem.allowedClasses.joinToString { it.displayName }}", detailsX + 20f, detailsY + detailsH - 120f)

            var bonusY = detailsY + detailsH - 150f
            font.color = Color.GREEN
            if (eqItem.damageBonus != 0) font.draw(batch, "Damage: +${eqItem.damageBonus}", detailsX + 20f, bonusY); bonusY -= 25f
            if (eqItem.mageDamageBonus != 0) font.draw(batch, "Magic Damage: +${eqItem.mageDamageBonus}", detailsX + 20f, bonusY); bonusY -= 25f
            if (eqItem.healthBonus != 0) font.draw(batch, "Health: +${eqItem.healthBonus}", detailsX + 20f, bonusY); bonusY -= 25f
            if (eqItem.manaBonus != 0) font.draw(batch, "Mana: +${eqItem.manaBonus}", detailsX + 20f, bonusY); bonusY -= 25f
            if (eqItem.defenseBonus != 0.0) font.draw(batch, "Defense: +${(eqItem.defenseBonus * 100).toInt()}%", detailsX + 20f, bonusY); bonusY -= 25f
            if (eqItem.attackSpeedBonus != 0.0) font.draw(batch, "Attack Speed: +${(eqItem.attackSpeedBonus * 100).toInt()}%", detailsX + 20f, bonusY); bonusY -= 25f
            if (eqItem.accuracyBonus != 0.0) font.draw(batch, "Accuracy: +${(eqItem.accuracyBonus * 100).toInt()}%", detailsX + 20f, bonusY); bonusY -= 25f
            if (eqItem.critChanceBonus != 0.0) font.draw(batch, "Crit Chance: +${(eqItem.critChanceBonus * 100).toInt()}%", detailsX + 20f, bonusY)
        }
        font.data.setScale(1f)
    }

    fun addEquipmentItem(equipmentItem: EquipmentItem, quantity: Int = 1) {
        val existingItem = items.find { it.equipmentItem?.id == equipmentItem.id }
        if (existingItem != null) {
            existingItem.quantity += quantity
        } else {
            items.add(Item(
                name = equipmentItem.name,
                description = equipmentItem.description,
                quantity = quantity,
                isEquippable = true,
                isEquipped = false,
                equipmentItem = equipmentItem
            ))
        }
    }

    fun addItem(item: Item) {
        println("=== ADDING ITEM TO INVENTORY ===")
        println("Item: ${item.name}, Quantity: ${item.quantity}")
        if (item.isEquippable && item.equipmentItem != null) {
            val existingItem = items.find {
                it.equipmentItem?.id == item.equipmentItem?.id && !it.isEquipped
            }
            if (existingItem != null) {
                existingItem.quantity += item.quantity
            } else {
                items.add(item.copy())
            }
        } else {
            val existingItem = items.find { it.name == item.name && !it.isEquippable }
            if (existingItem != null) {
                existingItem.quantity += item.quantity
            } else {
                items.add(item)
            }
        }
    }

    fun removeItem(itemName: String, quantity: Int = 1) {
        val item = items.find { it.name == itemName }
        item?.let {
            it.quantity -= quantity
            if (it.quantity <= 0) items.remove(it)
        }
    }

    fun getEquippableItems(): List<Item> = items.filter { it.isEquippable }
}
