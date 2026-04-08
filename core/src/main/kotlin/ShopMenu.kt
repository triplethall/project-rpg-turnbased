package ru.triplethall.rpgturnbased

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle

class ShopMenu(
    private val font: BitmapFont,
    private val screenWidth: Float,
    private val screenHeight: Float,
    private val inventory: Inventory,
    private val player: Player
) {
    var isVisible = false
        private set
    private val windowWidth = 1500f
    private val windowHeight = 900f
    private val closeButtonRect = Rectangle()
    // ранелья магазина
    private val shopPanelRect = Rectangle()
    private val shopItemRects = mutableListOf<Rectangle>()
    private var selectedShopItemIndex = -1
    private var selectedShopItem: EquipmentItem? = null
    // панелька инвентария
    private val inventoryPanelRect = Rectangle()
    private val inventoryItemRects = mutableListOf<Rectangle>()
    private var selectedInvItemIndex = -1
    private var selectedInvItem: Item? = null

    private val shopItems = mutableListOf<EquipmentItem>()
    init {
        initShopItems()
    }
    private fun initShopItems() {
        shopItems.addAll(listOf(
            EquipmentDatabase.IRON_CHESTPLATE,
            EquipmentDatabase.IRON_SPEAR,
            EquipmentDatabase.IRON_BOOTS,
            EquipmentDatabase.IRON_HELMET,
            EquipmentDatabase.PATH_DEFENDER,
            EquipmentDatabase.RAILBLADE,
            EquipmentDatabase.GRAN_SUDARUSKA,
            EquipmentDatabase.DEFENCE_RUNE
        ))
    }

    private fun getWindowRect(): Rectangle {
        val x = (screenWidth - windowWidth) / 2
        val y = (screenHeight - windowHeight) / 2
        return Rectangle(x, y, windowWidth, windowHeight)
    }

    private fun updatePanels()
    {
        val window = getWindowRect()
        shopPanelRect.set(window.x + 50f,
            window.y + 70f,
            window.width * 0.5f,
            window.height - 100f)
        inventoryPanelRect.set(
            shopPanelRect.x + shopPanelRect.width + 50f,
            window.y + 70f,
            window.width * 0.5f,
            window.height - 100f
        )
        val closeW = 70f
        val closeH = 70f
        val closeX = window.x + window.width - closeW - 15f
        val closeY = window.y + window.height - closeH - 15f
        closeButtonRect.set(closeX, closeY, closeW, closeH)
    }

    fun show() {
        isVisible = true
        selectedShopItemIndex = -1
        selectedShopItem = null
        selectedInvItemIndex = -1
        selectedInvItem = null
    }

    fun hide() {
        isVisible = false
        selectedShopItemIndex = -1
        selectedShopItem = null
        selectedInvItemIndex = -1
        selectedInvItem = null
    }

    fun handleInput(): Boolean {
        if (!isVisible) return false

        val touchX = Gdx.input.x.toFloat()
        val touchY = Gdx.input.y.toFloat()
        val yInverted = screenHeight - touchY

        updatePanels()

        if (Gdx.input.justTouched()) {
            if (closeButtonRect.contains(touchX, yInverted)) {
                hide()
                return true
            }
            if (selectedShopItem != null || selectedInvItem != null)
            {
                val window = getWindowRect()
                val detailsW = 750f
                val detailsH = 200f
                val detailsX = window.x + (window.width - detailsW) / 2
                val detailsY = window.y + 20f

                // Кнопка BUY/SELL (правая часть панели)
                val buttonWidth = 150f
                val buttonHeight = 40f
                val buttonX = detailsX + detailsW - buttonWidth - 20f
                val buttonY = detailsY + detailsH - 50f

                val buttonRect = Rectangle(buttonX, buttonY, buttonWidth, buttonHeight)

                if (buttonRect.contains(touchX, yInverted)) {
                    if (selectedShopItem != null) {
                        buySelectedItem()
                    } else if (selectedInvItem != null) {
                        sellSelectedItem()
                    }
                    return true
                }
            }
            shopItemRects.forEachIndexed { index, rectangle ->
                if (index < shopItems.size && rectangle.contains(touchX, yInverted))
                {
                    selectedShopItemIndex = index
                    selectedShopItem = shopItems[index]
                    selectedInvItemIndex = -1
                    selectedInvItem = null
                    return true
                }
            }
            val equipableItems = inventory.getEquippableItems()
            inventoryItemRects.forEachIndexed { index, rectangle ->
                if (index < equipableItems.size && rectangle.contains(touchX, yInverted))
                {
                    selectedShopItemIndex = -1
                    selectedShopItem = null
                    selectedInvItemIndex = index
                    selectedInvItem = equipableItems[index]
                    return true
                }
            }
        }

        return false
    }
    fun render(batch: SpriteBatch, shapeRenderer: ShapeRenderer, whitePixel: Texture) {
        if (!isVisible) return

        val window = getWindowRect()
        updatePanels()

        batch.end()
        shapeRenderer.setAutoShapeType(true)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(0f, 0f, 0f, 0.7f)
        shapeRenderer.rect(0f, 0f, screenWidth, screenHeight)
        shapeRenderer.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 1f)
        shapeRenderer.rect(window.x, window.y, window.width, window.height)
        shapeRenderer.end()

        batch.begin()

        font.color = Color.YELLOW
        font.data.setScale(2f)
        font.draw(batch, "SHOP", window.x + (window.width / 2) - 35f, window.y + window.height - 25f)
        font.color = Color.GOLD
        font.draw(batch, "EXP: ${player.experience}", window.width + 60f, window.y + 45f)

        font.data.setScale(1.7f)
        font.color = Color.CYAN
        font.draw(batch, "FOR SALE", shopPanelRect.x + 20f, shopPanelRect.y + shopPanelRect.height - 10f)
        font.draw(batch, "YOUR ITEMS", inventoryPanelRect.x + 20f, inventoryPanelRect.y + inventoryPanelRect.height - 10f)

        batch.end()
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(0.8f, 0.2f, 0.2f, 1f)
        shapeRenderer.rect(closeButtonRect.x, closeButtonRect.y, closeButtonRect.width, closeButtonRect.height)
        shapeRenderer.end()
        batch.begin()
        font.color = Color.WHITE
        font.data.setScale(2f)
        font.draw(batch, "X", closeButtonRect.x + 20f, closeButtonRect.y + 40f)

        renderShopItems(batch, shapeRenderer, whitePixel)
        renderInventoryItems(batch, shapeRenderer, whitePixel)

        if (selectedShopItem != null && selectedShopItemIndex != -1) {
            renderItemDetails(batch, shapeRenderer, window, selectedShopItem!!, whitePixel, isShopItem = true)
        } else if (selectedInvItem != null && selectedInvItemIndex != -1) {
            renderItemDetails(batch, shapeRenderer, window, selectedInvItem!!, whitePixel, isShopItem = false)
        }

        font.data.setScale(1f)
        batch.color = Color.WHITE
        font.color = Color.WHITE
    }

    private fun renderShopItems(batch: SpriteBatch, shapeRenderer: ShapeRenderer, whitePixel: Texture) {
        val startX = shopPanelRect.x + 15f
        val startY = shopPanelRect.y + shopPanelRect.height - 60f
        val itemSize = 100f
        val padding = 15f
        val itemsPerRow = 4

        shopItemRects.clear()

        shopItems.forEachIndexed { index, item ->
            val row = index / itemsPerRow
            val col = index % itemsPerRow
            val x = startX + col * (itemSize + padding)
            val y = startY - row * (itemSize + padding)

            if (y - itemSize < shopPanelRect.y + 20f) return@forEachIndexed

            val itemRect = Rectangle(x, y - itemSize, itemSize, itemSize)
            shopItemRects.add(itemRect)

            if (index == selectedShopItemIndex) {
                batch.end()
                shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
                shapeRenderer.setColor(1f, 1f, 0f, 1f)
                shapeRenderer.rect(x - 3f, y - itemSize - 3f, itemSize + 6f, itemSize + 6f)
                shapeRenderer.end()
                batch.begin()
            }

            batch.color = Color.GRAY
            batch.draw(whitePixel, x, y - itemSize, itemSize, itemSize)

            batch.color = when (item.type) {
                EquipmentType.WEAPON -> Color(0.8f, 0.4f, 0.2f, 1f)
                EquipmentType.HELMET, EquipmentType.CHESTPLATE, EquipmentType.BOOTS -> Color(0.4f, 0.6f, 0.8f, 1f)
                EquipmentType.SHIELD -> Color(0.6f, 0.5f, 0.3f, 1f)
                else -> Color(0.5f, 0.3f, 0.7f, 1f)
            }
            batch.draw(whitePixel, x + 10f, y - itemSize + 10f, itemSize - 20f, itemSize - 20f)

            font.color = Color.WHITE
            font.data.setScale(1f)
            val shortName = if (item.name.length > 12) item.name.take(10) + ".." else item.name
            font.draw(batch, shortName, x + 5f, y - itemSize + 15f)
        }
    }

    private fun renderInventoryItems(batch: SpriteBatch, shapeRenderer: ShapeRenderer, whitePixel: Texture) {
        val startX = inventoryPanelRect.x + 15f
        val startY = inventoryPanelRect.y + inventoryPanelRect.height - 60f
        val itemSize = 100f
        val padding = 15f
        val itemsPerRow = 4

        inventoryItemRects.clear()

        val equipableItems = inventory.getEquippableItems()

        equipableItems.forEachIndexed { index, item ->
            val row = index / itemsPerRow
            val col = index % itemsPerRow
            val x = startX + col * (itemSize + padding)
            val y = startY - row * (itemSize + padding)

            if (y - itemSize < inventoryPanelRect.y + 20f) return@forEachIndexed

            val itemRect = Rectangle(x, y - itemSize, itemSize, itemSize)
            inventoryItemRects.add(itemRect)

            if (index == selectedInvItemIndex) {
                batch.end()
                shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
                shapeRenderer.setColor(1f, 1f, 0f, 1f)
                shapeRenderer.rect(x - 3f, y - itemSize - 3f, itemSize + 6f, itemSize + 6f)
                shapeRenderer.end()
                batch.begin()
            }

            batch.color = if (item.isEquipped) Color(0.4f, 0.3f, 0.15f, 1f) else Color.GRAY
            batch.draw(whitePixel, x, y - itemSize, itemSize, itemSize)

            val eqItem = item.equipmentItem
            batch.color = when (eqItem?.type) {
                EquipmentType.WEAPON -> Color(0.8f, 0.4f, 0.2f, 1f)
                EquipmentType.HELMET, EquipmentType.CHESTPLATE, EquipmentType.BOOTS -> Color(0.4f, 0.6f, 0.8f, 1f)
                EquipmentType.SHIELD -> Color(0.6f, 0.5f, 0.3f, 1f)
                else -> Color(0.5f, 0.3f, 0.7f, 1f)
            }
            batch.draw(whitePixel, x + 10f, y - itemSize + 10f, itemSize - 20f, itemSize - 20f)

            font.color = Color.WHITE
            font.data.setScale(1f)
            val shortName = if (item.name.length > 12) item.name.take(10) + ".." else item.name
            font.draw(batch, shortName, x + 5f, y - itemSize + 15f)
        }
    }

    private fun renderItemDetails(batch: SpriteBatch, shapeRenderer: ShapeRenderer, window: Rectangle, item: Any, whitePixel: Texture, isShopItem: Boolean) {
        val detailsW = 750f
        val detailsH = 200f
        val detailsX = window.x + (window.width - detailsW) / 2
        val detailsY = window.y + 20f
        batch.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(0.1f, 0.1f, 0.1f, 0.90f)
        shapeRenderer.rect(detailsX, detailsY, detailsW, detailsH)
        shapeRenderer.end()

        batch.begin()
        if (isShopItem && item is EquipmentItem) {
            font.color = Color.YELLOW
            font.data.setScale(1.5f)
            font.draw(batch, item.name, detailsX + 15f, detailsY + detailsH - 15f)

            font.color = Color.LIGHT_GRAY
            font.data.setScale(1.2f)
            font.draw(batch, item.description.take(50), detailsX + 15f, detailsY + detailsH - 40f)

            font.color = Color.GOLD
            val price = item.level * 10
            font.draw(batch, "Price: $price", detailsX + 15f, detailsY + detailsH - 65f)
        } else if (!isShopItem && item is Item) {
            font.color = Color.YELLOW
            font.data.setScale(1.5f)
            font.draw(batch, item.name, detailsX + 15f, detailsY + detailsH - 15f)

            font.color = Color.LIGHT_GRAY
            font.data.setScale(1.2f)
            font.draw(batch, item.description.take(50), detailsX + 15f, detailsY + detailsH - 40f)

            font.color = Color.GOLD
            val price = (item.equipmentItem?.level ?: 1) * 5
            font.draw(batch, "Price: $price", detailsX + 15f, detailsY + detailsH - 65f)
        }
        batch.end()
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        if (isShopItem)
        {
            shapeRenderer.setColor(Color.GREEN)
        } else {
            shapeRenderer.setColor(Color.ORANGE)
        }
        val buttonWidth = 150f
        val buttonHeight = 40f
        val buttonX = detailsX + detailsW - buttonWidth - 20f
        val buttonY = detailsY + detailsH - 50f
        shapeRenderer.rect(buttonX, buttonY, buttonWidth, buttonHeight)
        shapeRenderer.end()
        batch.begin()
        font.color = Color.WHITE
        font.data.setScale(1.5f)
        val buttonText = if (isShopItem) "BUY" else "SELL"
        val textOffset = if (isShopItem) 50f else 40f
        font.draw(batch, buttonText, buttonX + textOffset, buttonY + buttonHeight - 12f)

        if (isShopItem && item is EquipmentItem) {
            val price = item.level * 10
            if (player.experience < price) {
                font.color = Color.RED
                font.data.setScale(1f)
                font.draw(batch, "Not enough exp!", buttonX, buttonY - 5f)
            }
        }
    }
    // покупка предмета
    private fun buySelectedItem() {
        val item = selectedShopItem ?: return
        val price = item.level * 10

        // проверяем хватает ли опыта
        if (player.experience < price) {
            println("no exp\nprice: $price\namount: ${player.experience}")
            return
        }

        // списываем опыт
        player.experience -= price

        // добавляем предмет в инвентарь
        inventory.addEquipmentItem(item)

        println("bought ${item.name} for $price exp")

        // сбрасываем выделение
        selectedShopItemIndex = -1
        selectedShopItem = null
    }
    // продажа предмета
    private fun sellSelectedItem() {
        val item = selectedInvItem ?: return

        // нельзя продать экипированный предмет
        if (item.isEquipped) {
            return
        }

        val eqItem = item.equipmentItem ?: return
        val price = eqItem.level * 5

        // удаляем предмет из инвентаря
        inventory.removeItem(item.name, 1)

        // добавляем опыт
        player.experience += price

        println("sold ${item.name} for $price exp")

        // сбрасываем выделение
        selectedInvItemIndex = -1
        selectedInvItem = null
    }

}
