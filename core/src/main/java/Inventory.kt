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
    var isEquipped: Boolean = false
)


class Inventory(
    private val font: BitmapFont,
    private val screenWidth: Float,
    private val screenHeight: Float,
    private var invbutt: Texture
) {
    var isVisible = false
        private set




    private var isItemDetailsVisible = false

    // Кнопка открытия инвентаря
    private val inventoryButtonRect = Rectangle(130f, screenHeight - 120f, 120f, 120f)

    // Основное меню инвентаря
    private val closeButtonRect = Rectangle()
    private val itemRects = mutableListOf<Rectangle>()
    private val selectedItemRect = Rectangle()
    private val equipButtonRect = Rectangle()
    private val useButtonRect = Rectangle()
    private val dropButtonRect = Rectangle()

    // Список предметов
    private val items = mutableListOf<Item>()
    private var selectedItem: Item? = null
    private var selectedItemIndex = -1

    // Флаги для визуального отклика
    private var isEquipPressed = false
    private var isUsePressed = false
    private var isDropPressed = false


    init {
        initTestItems()
    }


    private fun initTestItems() {
        items.addAll(listOf(
            Item("Health Potion", "Restores 50 HP", 3, false),
            Item("Mana Potion", "Restores 30 MP", 2, false),
            Item("Iron Sword", "A basic sword. Damage +10", 1, true),
            Item("Leather Armor", "Basic armor. Defense +15%", 1, true),
            Item("Magic Scroll", "Contains ancient magic", 1, false),
            Item("Antidote", "Cures poisoning", 1, false)
        )
        )
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

        if (!isVisible) {
            // Обработка кнопки открытия инвентаря
            if (Gdx.input.justTouched() && inventoryButtonRect.contains(touchX, invertedY)) {
                toggle()
            }
            return
        }


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
                    handleEquipItem(selectedItem!!, player)
                    return
                }
                if (useButtonRect.contains(touchX, invertedY) && !selectedItem!!.isEquippable) {
                    handleUseItem(selectedItem!!, player)
                    return
                }
                if (dropButtonRect.contains(touchX, invertedY)) {
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


    private fun handleEquipItem(item: Item, player: Player) {
        if (item.isEquippable) {
            item.isEquipped = !item.isEquipped
            // Здесь можно добавить логику изменения характеристик игрока
            if (item.isEquipped) {
                println("Equipped: ${item.name}")
                // Например: player.damage += 10 для меча
            } else {
                println("Unequipped: ${item.name}")
            }
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
                    player.currentMana = minOf(player.currentMana + 30, player.maxHealth)
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
        // Рисуем кнопку инвентаря
        batch.draw(invbutt, inventoryButtonRect.x, inventoryButtonRect.y,
            inventoryButtonRect.width, inventoryButtonRect.height)


        if (!isVisible) return


        // Затемняем фон
        batch.color = Color(0f, 0f, 0f, 0.7f)
        batch.draw(whitePixel, 0f, 0f, screenWidth, screenHeight)


        // Основная панель инвентаря
        val panelW = 1000f
        val panelH = 700f
        val panelX = (screenWidth - panelW) / 2
        val panelY = (screenHeight - panelH) / 2


        batch.color = Color.DARK_GRAY
        batch.draw(whitePixel, panelX, panelY, panelW, panelH)


        // Заголовок
        font.color = Color.YELLOW
        font.data.setScale(2f)
        font.draw(batch, "INVENTORY", panelX + 20f, panelY + panelH - 20f)


        // Кнопка закрытия
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


        // Список предметов
        renderItems(batch, whitePixel, panelX, panelY, panelW, panelH)


        // Детали предмета
        if (isItemDetailsVisible && selectedItem != null) {
            renderItemDetails(batch, whitePixel, panelX, panelY, panelW, panelH, selectedItem!!)
        }


        batch.color = Color.WHITE
        font.data.setScale(1f)
    }


    private fun renderItems(batch: SpriteBatch, whitePixel: Texture, panelX: Float, panelY: Float, panelW: Float, panelH: Float) {
        val startX = panelX + 50f
        val startY = panelY + panelH - 150f
        val itemSize = 80f
        val padding = 20f
        val itemsPerRow = 5


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
        }
    }

    private fun renderItemDetails(batch: SpriteBatch, whitePixel: Texture, panelX: Float, panelY: Float, panelW: Float, panelH: Float, item: Item) {
        val detailsW = 400f
        val detailsH = 300f
        val detailsX = panelX + panelW - detailsW - 50f
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

            batch.color = if (isEquipPressed) Color.LIGHT_GRAY else Color(0.3f, 0.6f, 0.3f, 1f)
            batch.draw(whitePixel, equipX, buttonY, buttonW, buttonH)
            font.color = Color.WHITE
            font.draw(batch, if (item.isEquipped) "UNEQUIP" else "EQUIP", equipX + 15f, buttonY + 32f)
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
    }

    fun addItem(item: Item) {
        val existingItem = items.find { it.name == item.name && !it.isEquippable }
        if (existingItem != null) {
            existingItem.quantity += item.quantity
        } else {
            items.add(item)
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
}
