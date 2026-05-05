package ru.triplethall.rpgturnbased

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle

class LootWindow(private val font: BitmapFont) {
    var isVisible = false
    private var lootItems = mutableListOf<Item>()
    private val windowRect = Rectangle()
    private val closeBtnRect = Rectangle()

    fun show(items: List<Item>) {
        this.lootItems = items.toMutableList()
        isVisible = true
        // Центрируем окно
        val w = 500f
        val h = 300f
        windowRect.set((Gdx.graphics.width - w) / 2, (Gdx.graphics.height - h) / 2, w, h)
        closeBtnRect.set(windowRect.x + (w - 120f) / 2, windowRect.y + 20f, 120f, 50f)
    }

    fun handleInput() {
        if (!isVisible) return
        if (Gdx.input.justTouched()) {
            val tx = Gdx.input.x.toFloat()
            val ty = Gdx.graphics.height - Gdx.input.y.toFloat()
            if (closeBtnRect.contains(tx, ty) || !windowRect.contains(tx, ty)) {
                isVisible = false
            }
        }
    }

    fun render(batch: SpriteBatch, whitePixel: Texture) {
        if (!isVisible) return
        val ownedBatch = !batch.isDrawing
        if (ownedBatch) batch.begin()
        // Фон окна
        batch.color = Color(0.1f, 0.1f, 0.1f, 0.95f)
        batch.draw(whitePixel, windowRect.x, windowRect.y, windowRect.width, windowRect.height)

        font.data.setScale(1.5f)
        font.color = Color.GOLD
        font.draw(batch, "ВЫ НАШЛИ:", windowRect.x + 150f, windowRect.y + windowRect.height - 30f)

        // Отрисовка предметов
        lootItems.forEachIndexed { index, item ->
            val iconX = windowRect.x + 50f
            val iconY = windowRect.y + windowRect.height - 120f - (index * 60f)

            // Квадратик-иконка
            batch.color = if (item.isEquippable) Color.ORANGE else Color.RED
            batch.draw(whitePixel, iconX, iconY, 40f, 40f)

            // Название предмета
            font.data.setScale(1.2f)
            font.color = Color.WHITE
            font.draw(batch, "${item.name} x${item.quantity}", iconX + 60f, iconY + 30f)
        }

        // Кнопка OK
        batch.color = Color.GRAY
        batch.draw(whitePixel, closeBtnRect.x, closeBtnRect.y, closeBtnRect.width, closeBtnRect.height)
        font.draw(batch, "OK", closeBtnRect.x + 45f, closeBtnRect.y + 35f)
        if (ownedBatch) batch.end()

    }
}
