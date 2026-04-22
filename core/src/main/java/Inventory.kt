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

    // типы экипировки
    private val helmetSlotRect = Rectangle()
    private val chestplateSlotRect = Rectangle()
    private val bootsSlotRect = Rectangle()
    private val shieldSlotRect = Rectangle()
    private val weaponSlotRect = Rectangle()
    private val rune1SlotRect = Rectangle()
    private val rune2SlotRect = Rectangle()

    private var isItemDetailsVisible = false


    // Кнопка открытия инвентаря

    private val inventoryButtonRect = Rectangle(130f, screenHeight - 180, 120f, 120f)




    // Основное меню инвентаря
    private val closeButtonRect = Rectangle()
    private val itemRects = mutableListOf<Rectangle>()
    private val selectedItemRect = Rectangle()
    private val equipButtonRect = Rectangle()
    private val useButtonRect = Rectangle()
    private val dropButtonRect = Rectangle()
    private val equipmentSlots = mutableMapOf<EquipmentType, Rectangle>()

    private val runeSlots = mutableListOf<Rectangle>()

    // Список предметов
    private val items = mutableListOf<Item>()
    private var selectedItem: Item? = null
    private var selectedItemIndex = -1

    // Флаги для визуального отклика
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
        // Создаем тестовые предметы с привязкой к EquipmentDatabase
        items.addAll(listOf(
            Item("Health Potion", "Restores 50 HP", 3, false),
            Item("Mana Potion", "Restores 30 MP", 2, false),
            Item("Wooden Sword", "A basic sword. Damage +10", 1, true, false, EquipmentDatabase.WOODEN_SWORD),
            Item("Leather Armor", "Basic armor. Defense +15%", 1, true, false, EquipmentDatabase.LEATHER_CHESTPLATE),
            Item("Iron Helmet", "Basic helmet. Defense +10%", 1, true, false, EquipmentDatabase.IRON_HELMET),
            Item("Iron Boots", "Basic boots. Defense +10%", 1, true, false, EquipmentDatabase.IRON_BOOTS),
            Item("Wooden Shield", "Basic shield. Defense +5%", 1, true, false, EquipmentDatabase.WOODEN_SHIELD),
            Item("Defence rune", "+20% armor in exchange for -20% max health", 1, true, false,EquipmentDatabase.DEFENCE_RUNE)
        ))
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
            // Обработка кнопки открытия инвентаря
            if (Gdx.input.justTouched() && inventoryButtonRect.contains(touchX, invertedY)) {
                toggle()
            }
            return
        }

        // Проверяем клик по слотам экипировки
        var slotClicked = false
        equipmentSlots.forEach { (type, rect) ->
            if (rect.contains(touchX, invertedY)) {
                val equippedItem = player.equipment.getEquipped(type)
                if (equippedItem != null) {
                    unequipItemByType(type, player)
                }
                slotClicked = true
                return@forEach
            }
        }
        if (slotClicked) return

        // Проверяем клик по слотам рун
        var runeSlotClicked = false
        runeSlots.forEachIndexed { index, rect ->
            if (rect.contains(touchX, invertedY)) {
                val rune = player.equipment.getRune(index)
                if (rune != null) {
                    unequipRune(index, player)
                }
                runeSlotClicked = true
                return@forEachIndexed
            }
        }
        if (runeSlotClicked) return

        // Инвентарь открыт
        if (Gdx.input.justTouched()) {
            // Проверяем кнопку закрытия
            if (closeButtonRect.contains(touchX, invertedY)) {
                toggle()
                return
            }


            // Если детали предмета открыты
            if (isItemDetailsVisible && selectedItem != null) {
                // Проверяем кнопки действий с предметом
                if (equipButtonRect.contains(touchX, invertedY) && selectedItem!!.isEquippable) {
                    isEquipPressed = true
                    handleEquipItem(selectedItem!!, player)
                    return
                }
                if (useButtonRect.contains(touchX, invertedY) && !selectedItem!!.isEquippable) {
                    isUsePressed = true
                    handleUseItem(selectedItem!!, player)
                    return
                }
                if (dropButtonRect.contains(touchX, invertedY)) {
                    isDropPressed = true
                    handleDropItem(selectedItem!!)
                    return
                }
            }


            // Проверяем клик по предметам
            itemRects.forEachIndexed { index, rect ->
                if (index < items.size && rect.contains(touchX, invertedY)) {
                    selectedItem = items[index]
                    selectedItemIndex = index
                    isItemDetailsVisible = true

                    // Обновляем позицию прямоугольника выбранного предмета для подсветки
                    selectedItemRect.set(rect)
                }
            }
        } else {
            // Сброс состояний нажатий
            isEquipPressed = false
            isUsePressed = false
            isDropPressed = false
        }
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


    private fun handleEquipItem(item: Item, player: Player) {
        if (!item.isEquippable) return

        val equipmentItem = item.equipmentItem
        if (equipmentItem == null) {
            // Для рун - нужно экипировать в свободный слот
            if (item.isEquipped) {
                // Снимаем руну
                for (i in 0..1) {
                    if (player.equipment.getRune(i)?.name == item.name) {
                        player.equipment.unequipRune(i, player)
                        item.isEquipped = false
                        break
                    }
                }
            } else {
                // Экипируем руну в первый свободный слот
                for (i in 0..1) {
                    if (player.equipment.getRune(i) == null) {
                        // Создаем временный EquipmentItem для руны
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

        // Проверяем, может ли игрок использовать этот предмет
        if (!equipmentItem.canBeUsedBy(player.playerClass)) {
            println("You cannot equip this item with your class: ${player.playerClass.displayName}")
            return
        }

        if (equipmentItem.level > player.level) {
            println("Your level is too low to equip this item. Required: ${equipmentItem.level}")
            return
        }

        // Если предмет уже экипирован - снимаем
        if (item.isEquipped) {
            player.equipment.unequip(equipmentItem.type, player)
            item.isEquipped = false
            println("Unequipped: ${item.name}")
            return
        }

        // Пытаемся экипировать
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
            // Логика использования предмета
            when (item.name) {
                "Health Potion" -> {
                    player.currentHealth = minOf(player.currentHealth + 50, player.maxHealth)
                }
                "Mana Potion" -> {
                    player.currentMana = minOf(player.currentMana + 30, player.maxMana)
                }
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


    fun render(batch: SpriteBatch, whitePixel: Texture, player: Player) {
        currentPlayer = player


        // 1. Рисуем кнопку инвентаря (всегда видна)
        batch.draw(invbutt, inventoryButtonRect.x, inventoryButtonRect.y,
            inventoryButtonRect.width, inventoryButtonRect.height)

        if (!isVisible) return

        // 2. Затемняем фон (полупрозрачный черный слой)
        batch.color = Color(0f, 0f, 0f, 0.7f)
        batch.draw(whitePixel, 0f, 0f, screenWidth, screenHeight)

        // 3. Основная панель инвентаря (темно-серая)
        val panelW = 1200f
        val panelH = 700f
        val panelX = (screenWidth - panelW) / 2
        val panelY = (screenHeight - panelH) / 2

        batch.color = Color.DARK_GRAY
        batch.draw(whitePixel, panelX, panelY, panelW, panelH)

        // 4. Заголовок "INVENTORY"
        font.color = Color.YELLOW
        font.data.setScale(2f)
        font.draw(batch, "INVENTORY", panelX + 20f, panelY + panelH - 20f)

        // 5. Список предметов (слева)
        renderItems(batch, whitePixel, panelX, panelY, panelW, panelH)

        // 6. Кнопка закрытия (красная кнопка X)
        val closeBtnW = 100f
        val closeBtnH = 50f
        val closeBtnX = panelX + panelW - closeBtnW - 20f
        val closeBtnY = panelY + panelH - closeBtnH - 20f
        closeButtonRect.set(closeBtnX, closeBtnY, closeBtnW, closeBtnH)

        batch.color = Color.RED
        batch.draw(whitePixel, closeBtnX, closeBtnY, closeBtnW, closeBtnH)

        font.color = Color.WHITE
        font.data.setScale(1.2f)
        font.draw(batch, "X", closeBtnX + 40f, closeBtnY + 35f)

        // 7. Панель экипировки (справа) - рисуется ПОВЕРХ списка и кнопки закрытия
        renderEquipmentPanel(batch, whitePixel, panelX, panelY, panelW, panelH, player)

        // 8. Детали выбранного предмета (по центру) - рисуются ПОВЕРХ ВСЕГО
        if (isItemDetailsVisible && selectedItem != null) {
            renderItemDetails(batch, whitePixel, panelX, panelY, panelW, panelH, selectedItem!!)
        }

        // 9. Сбрасываем цвет и масштаб шрифта
        batch.color = Color.WHITE
        font.data.setScale(1f)
    }

    // Метод для снятия экипировки по типу
    private fun unequipItemByType(type: EquipmentType, player: Player) {
            val item = player.equipment.getEquipped(type) ?: return

            // Снимаем экипировку
            if (player.equipment.unequip(type, player)) {
                // Добавляем предмет обратно в инвентарь
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

    // Метод отрисовки панели экипировки
    private fun renderEquipmentPanel(batch: SpriteBatch, whitePixel: Texture, panelX: Float, panelY: Float, panelW: Float, panelH: Float, player: Player) {
            val equipPanelX = panelX + panelW - 350f
            val equipPanelY = panelY + 50f
            val equipPanelW = 320f
            val equipPanelH = 600f

            // Фон панели экипировки
            batch.color = Color(0.3f, 0.3f, 0.3f, 0.9f)
            batch.draw(whitePixel, equipPanelX, equipPanelY, equipPanelW, equipPanelH)

            // Заголовок панели
            font.color = Color.YELLOW
            font.data.setScale(1.2f)
            font.draw(batch, "EQUIPMENT", equipPanelX + 100f, equipPanelY + equipPanelH - 15f)

            // Параметры слотов
            val slotSize = 60f
            val slotStartX = equipPanelX + 50f
            var slotY = equipPanelY + equipPanelH - 80f

            // 1. Рисуем основные слоты (шлем, нагрудник, ботинки, щит, оружие)
            val mainSlots = listOf(
                Triple(EquipmentType.HELMET, "HELMET", helmetSlotRect),
                Triple(EquipmentType.CHESTPLATE, "CHEST", chestplateSlotRect),
                Triple(EquipmentType.BOOTS, "BOOTS", bootsSlotRect),
                Triple(EquipmentType.SHIELD, "SHIELD", shieldSlotRect),
                Triple(EquipmentType.WEAPON, "WEAPON", weaponSlotRect)
            )

            mainSlots.forEach { (type, label, rect) ->
                val equippedItem = player.equipment.getEquipped(type)

                // Прямоугольник слота
                rect.set(slotStartX, slotY - slotSize, slotSize, slotSize)

                // Фон слота
                batch.color = Color(0.2f, 0.2f, 0.2f, 1f)
                batch.draw(whitePixel, rect.x, rect.y, rect.width, rect.height)

                // Рамка слота
                batch.color = Color.GOLD
                val thickness = 2f
                batch.draw(whitePixel, rect.x, rect.y, rect.width, thickness)
                batch.draw(whitePixel, rect.x, rect.y + rect.height, rect.width, thickness)
                batch.draw(whitePixel, rect.x, rect.y, thickness, rect.height)
                batch.draw(whitePixel, rect.x + rect.width, rect.y, thickness, rect.height)

                // Если есть экипированный предмет - отображаем
                if (equippedItem != null) {
                    batch.color = Color.ORANGE
                    batch.draw(whitePixel, rect.x + 10f, rect.y + 10f, rect.width - 20f, rect.height - 20f)

                    // Первая буква названия
                    font.color = Color.WHITE
                    font.data.setScale(0.8f)
                    val firstLetter = equippedItem.name.take(2)
                    font.draw(batch, firstLetter, rect.x + rect.width / 2 - 10f, rect.y + rect.height / 2 + 5f)
                }

                // Название слота
                font.color = Color.LIGHT_GRAY
                font.data.setScale(0.8f)
                font.draw(batch, label, slotStartX + slotSize + 10f, slotY - slotSize / 2 + 10f)

                // Отображаем статы предмета если есть
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
                if (type == EquipmentType.WEAPON) {
                    slotY -= 10f  // Отступ перед рунами
                }
            }

            // 2. Рисуем слоты для рун
            runeSlots.forEachIndexed { index, rect ->
                val label = "RUNE ${index + 1}"
                val equippedItem = player.equipment.getRune(index)

                // Прямоугольник слота
                rect.set(slotStartX, slotY - slotSize, slotSize, slotSize)

                // Фон слота
                batch.color = Color(0.2f, 0.2f, 0.2f, 1f)
                batch.draw(whitePixel, rect.x, rect.y, rect.width, rect.height)

                // Рамка слота
                batch.color = Color.GOLD
                val thickness = 2f
                batch.draw(whitePixel, rect.x, rect.y, rect.width, thickness)
                batch.draw(whitePixel, rect.x, rect.y + rect.height, rect.width, thickness)
                batch.draw(whitePixel, rect.x, rect.y, thickness, rect.height)
                batch.draw(whitePixel, rect.x + rect.width, rect.y, thickness, rect.height)

                // Если есть экипированный предмет - отображаем
                if (equippedItem != null) {
                    batch.color = Color.ORANGE
                    batch.draw(whitePixel, rect.x + 10f, rect.y + 10f, rect.width - 20f, rect.height - 20f)

                    // Первая буква названия
                    font.color = Color.WHITE
                    font.data.setScale(0.8f)
                    val firstLetter = equippedItem.name.take(2)
                    font.draw(batch, firstLetter, rect.x + rect.width / 2 - 10f, rect.y + rect.height / 2 + 5f)
                }

                // Название слота
                font.color = Color.LIGHT_GRAY
                font.data.setScale(0.8f)
                font.draw(batch, label, slotStartX + slotSize + 10f, slotY - slotSize / 2 + 10f)

                // Отображаем статы предмета если есть
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

            // 3. Статистика персонажа
            renderPlayerStats(batch, whitePixel, equipPanelX, equipPanelY, equipPanelW, equipPanelH, player)
        }

    // Метод отрисовки статистики
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
            font.draw(batch, "LUCK: ${(player.luck * 100).toInt()}%",statsX+20f, statsY-5f)
            font.draw(batch, "CORRUPT: ${player.corruption}",statsX+150f, statsY-5f)
        }
    // FIXME: зачем такие пробелы делать 😭😭🙏
    private fun renderItems(batch: SpriteBatch, whitePixel: Texture, panelX: Float, panelY: Float, panelW: Float, panelH: Float) {
        val startX = panelX + 50f
        val startY = panelY + panelH - 150f
        val itemSize = 80f
        val padding = 20f
        val itemsPerRow = 4


        itemRects.clear()


        items.forEachIndexed { index, item ->
            val row = index / itemsPerRow
            val col = index % itemsPerRow
            val x = startX + col * (itemSize + padding)
            val y = startY - row * (itemSize + padding)


            val itemRect = Rectangle(x, y - itemSize, itemSize, itemSize)
            itemRects.add(itemRect)


            // Фон предмета
            if (index == selectedItemIndex) {
                batch.color = Color.YELLOW
                batch.draw(whitePixel, x - 5f, y - itemSize - 5f, itemSize + 10f, itemSize + 10f)
            }


            batch.color = if (item.isEquipped) Color.GOLD else Color.GRAY
            batch.draw(whitePixel, x, y - itemSize, itemSize, itemSize)


            // Иконка предмета
            batch.color = when {
                item.isEquippable -> Color.ORANGE
                item.name.contains("Potion") -> Color.RED
                else -> Color.CYAN
            }
            batch.draw(whitePixel, x + 10f, y - itemSize + 10f, itemSize - 20f, itemSize - 20f)


            // Количество
            if (item.quantity > 1) {
                font.color = Color.WHITE
                font.data.setScale(1f)
                font.draw(batch, "x${item.quantity}", x + itemSize - 30f, y - 10f)
            }


            // Маркер экипировки
            if (item.isEquipped) {
                font.color = Color.GREEN
                font.data.setScale(1f)
                font.draw(batch, "E", x + 5f, y - 15f)
            }

            // Название предмета (сокращенное)
            font.color = Color.WHITE
            font.data.setScale(0.6f)
            val shortName = if (item.name.length > 12) item.name.take(10) + ".." else item.name
            font.draw(batch, shortName, x + 5f, y - itemSize + 15f)
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

    private fun renderItemDetails(batch: SpriteBatch, whitePixel: Texture, panelX: Float, panelY: Float, panelW: Float, panelH: Float, item: Item) {
        val detailsW = 400f
        val detailsH = 350f
        val detailsX = panelX + panelW - detailsW - 400f
        val detailsY = panelY + 100f

        // Фон деталей
        batch.color = Color(0.2f, 0.2f, 0.2f, 0.95f)
        batch.draw(whitePixel, detailsX, detailsY, detailsW, detailsH)

        // Название предмета
        font.color = Color.YELLOW
        font.data.setScale(1.5f)
        font.draw(batch, item.name, detailsX + 20f, detailsY + detailsH - 20f)

        // Описание
        font.color = Color.WHITE
        font.data.setScale(1.2f)
        font.draw(batch, item.description, detailsX + 20f, detailsY + detailsH - 60f)

        // Кнопки действий
        val buttonY = detailsY + 50f
        val buttonW = 120f
        val buttonH = 50f

        // Кнопка использования/экипировки
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

        // Кнопка выбросить
        val dropX = detailsX + 200f
        dropButtonRect.set(dropX, buttonY, buttonW, buttonH)

        batch.color = if (isDropPressed) Color.LIGHT_GRAY else Color(0.8f, 0.3f, 0.3f, 1f)
        batch.draw(whitePixel, dropX, buttonY, buttonW, buttonH)
        font.color = Color.WHITE
        font.draw(batch, "DROP", dropX + 30f, buttonY + 32f)

        font.data.setScale(1f)


        // Дополнительная информация для экипируемых предметов
        if (item.isEquippable && item.equipmentItem != null) {
            val eqItem = item.equipmentItem!!
            font.data.setScale(0.9f)
            font.color = Color.LIGHT_GRAY
            font.draw(batch, "Level required: ${eqItem.level}", detailsX + 20f, detailsY + detailsH - 95f)
            font.draw(batch, "Class: ${eqItem.allowedClasses.joinToString { it.displayName }}", detailsX + 20f, detailsY + detailsH - 120f)

            // Бонусы
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
    }

    fun addItem(item: Item) {
        println("=== ADDING ITEM TO INVENTORY ===")
        println("Item: ${item.name}, Quantity: ${item.quantity}")

        // Для экипируемых предметов - особая логика
        if (item.isEquippable && item.equipmentItem != null) {
            val existingItem = items.find {
                it.equipmentItem?.id == item.equipmentItem?.id && !it.isEquipped
            }
            if (existingItem != null) {
                existingItem.quantity += item.quantity
                println("  Stacked with existing equipment: now ${existingItem.quantity}")
            } else {
                items.add(item.copy())
                println("  Added new equipment item")
            }
        }
        // Для обычных предметов
        else {
            val existingItem = items.find { it.name == item.name && !it.isEquippable }
            if (existingItem != null) {
                existingItem.quantity += item.quantity
                println("  Stacked with existing: now ${existingItem.quantity}")
            } else {
                items.add(item)
                println("  Added new item")
            }
        }
    }

    fun removeItem(itemName: String, quantity: Int = 1) {
        val item = items.find { it.name == itemName }
        item?.let {
            it.quantity -= quantity
            if (it.quantity <= 0) {
                items.remove(it)
            }
        }
    }

    fun getEquippableItems(): List<Item>
    {
        return items.filter { it.isEquippable }
    }
}
