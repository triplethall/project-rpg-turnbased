package ru.triplethall.rpgturnbased

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import kotlin.math.abs

class CameraControl (
    val camera: OrthographicCamera,
    private val mapWidth: Float,
    private val mapHeight: Float
) {
    private val touchStart = Vector3()
    private val cameraStartPos = Vector3()
    private var isDragging = false
    private var initialPinchDistance = -1f
    private var zoomStart = 1f

    private val DRAG_THRESHOLD =50f

    fun update() {
        handleInput()
        clampCamera()
        camera.update()
    }

    private fun handleInput() {
        val p0 = Gdx.input.isTouched(0)
        val p1 = Gdx.input.isTouched(1)

        // Зум двумя пальцами
        if (p0 && p1) {
            if (initialPinchDistance < 0) {
                initialPinchDistance = getPinchDistance()
                zoomStart = camera.zoom
            } else {
                val cur = getPinchDistance()
                val scale = cur / initialPinchDistance
                if (abs(scale - 1f) > 0.02f) {
                    camera.zoom = MathUtils.clamp(zoomStart / scale, 0.25f, 1.5f)
                }
            }
        } else {
            initialPinchDistance = -1f

            // Перетаскивание одним пальцем — ЧЕРЕЗ unproject
            if (p0) {
                val x = Gdx.input.getX(0).toFloat()
                val y = Gdx.input.getY(0).toFloat()

                if (!isDragging) {
                    // Начало драга
                    val dx = abs(x - touchStart.x)
                    val dy = abs(y - touchStart.y)

                    // Если палец сдвинулся больше чем на 10 пикселей — это драг
                    if (dx > DRAG_THRESHOLD || dy > DRAG_THRESHOLD) {
                        isDragging = true
                        // Запоминаем точку для перетаскивания
                        touchStart.set(x, y, 0f)
                        cameraStartPos.set(camera.position)
                    }
                } else {
                    // Конвертируем ОБЕ точки в мировые координаты
                    val startWorld: Vector3 =
                        camera.unproject(Vector3(touchStart.x, touchStart.y, 0f))
                    val curWorld: Vector3 = camera.unproject(Vector3(x, y, 0f))

                    // Дельта в мировых координатах
                    val worldDx = curWorld.x - startWorld.x
                    val worldDy = curWorld.y - startWorld.y

                    // Двигаем камеру в ПРОТИВОПОЛОЖНУЮ сторону от движения пальца
                    camera.position.set(
                        cameraStartPos.x - worldDx,
                        cameraStartPos.y - worldDy,
                        camera.position.z
                    )
                }
            } else {
                // Палец отпущен — сбрасываем драг и обновляем точку для следующего касания
                isDragging = false
            }

        }
    }
    private fun getPinchDistance(): Float {
        val x1 = Gdx.input.getX(0)
        val y1 = (Gdx.graphics.height - Gdx.input.getY(0))
        val x2 = Gdx.input.getX(1)
        val y2 = (Gdx.graphics.height - Gdx.input.getY(1))

        val dx = x1 - x2
        val dy = y1 - y2
        return kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }
    private fun clampCamera() {
        // Видимая область в мировых координатах (с учётом зума)
        val visibleWidth = camera.viewportWidth * camera.zoom
        val visibleHeight = camera.viewportHeight * camera.zoom

        val halfVisibleW = visibleWidth / 2
        val halfVisibleH = visibleHeight / 2

        val bufferFactor = 0.7f
        val bufferW = halfVisibleW * bufferFactor
        val bufferH = halfVisibleH * bufferFactor

        // Ограничиваем X с буфером
        val minX = halfVisibleW - bufferW // Край карты может уйти к центру
        val maxX: Float = mapWidth - halfVisibleW + bufferW // с другой стороны тоже

        if (minX < maxX) {
            camera.position.x = MathUtils.clamp(camera.position.x, minX, maxX)
        } else {
            camera.position.x = mapWidth / 2
        }

        // Ограничиваем Y с буфером
        val minY = halfVisibleH - bufferH
        val maxY: Float = mapHeight - halfVisibleH + bufferH

        if (minY < maxY) {
            camera.position.y = MathUtils.clamp(camera.position.y, minY, maxY)
        } else {
            camera.position.y = mapHeight / 2
        }
    }
    fun isDragging(): Boolean = isDragging
}
