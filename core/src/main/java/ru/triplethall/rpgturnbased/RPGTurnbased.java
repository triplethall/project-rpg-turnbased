package ru.triplethall.rpgturnbased;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.graphics.Color;

public class RPGTurnbased extends ApplicationAdapter {
    private SpriteBatch batch;
    private Texture image;
    private OrthographicCamera camera;
    private GameMap gameMap;
    private Texture pixelTexture;
    private final int CELL_SIZE = 32;
    private final int CELL_GAP = 4;
    private float mapWidthPixels;
    private float mapHeightPixels;
    private BitmapFont debugFont;
    private boolean showDebug = true;
    private Vector3 touchStart = new Vector3();
    private Vector3 cameraStartPos = new Vector3();
    private boolean isDragging = false;
    private float initialPinchDistance = -1;
    private float zoomStart = 1f;

    @Override
    public void create() {
        batch = new SpriteBatch();


        gameMap = new GameMap(21, 21);
        gameMap.generate();

        // Белый пиксель для отрисовки
        pixelTexture = new Texture(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(com.badlogic.gdx.graphics.Color.WHITE);
        pixmap.fill();
        pixelTexture.draw(pixmap, 0, 0);
        pixmap.dispose();

        // Размер карты в пикселях
        mapWidthPixels = gameMap.getWidth() * (CELL_SIZE + CELL_GAP);
        mapHeightPixels = gameMap.getHeight() * (CELL_SIZE + CELL_GAP);

        // Камера на основе размера ЭКРАНА, а не карты
        camera = new OrthographicCamera();
        float screenRatio = (float)Gdx.graphics.getWidth() / Gdx.graphics.getHeight();

        // Подбираем viewport так, чтобы тайлы были квадратными
        float viewWidth, viewHeight;
        if (screenRatio > 1f) {
            // Экран широкий (ландшафт)
            viewHeight = mapHeightPixels;
            viewWidth = viewHeight * screenRatio;
        } else {
            // Экран узкий (портрет)
            viewWidth = mapWidthPixels;
            viewHeight = viewWidth / screenRatio;
        }

        camera.setToOrtho(false, viewWidth, viewHeight);
        camera.position.set(mapWidthPixels / 2f, mapHeightPixels / 2f, 0);
        camera.zoom = 1f;
        camera.update();
        debugFont = new BitmapFont();
        debugFont.setColor(Color.YELLOW);
        debugFont.getData().setScale(1.5f);
        Gdx.app.log("Camera", "Viewport: " + camera.viewportWidth + "x" + camera.viewportHeight);
        Gdx.app.log("Camera", "Map: " + mapWidthPixels + "x" + mapHeightPixels);
        Gdx.app.log("Camera", "Screen: " + Gdx.graphics.getWidth() + "x" + Gdx.graphics.getHeight());
    }

    @Override
    public void render() {
        handleInput();
        clampCamera();

        ScreenUtils.clear(0.1f, 0.1f, 0.2f, 1f);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();

        for (int x = 0; x < gameMap.getWidth(); x++) {
            for (int y = 0; y < gameMap.getHeight(); y++) {
                if (gameMap.isWalkable(x, y)) {
                    batch.setColor(Color.GREEN);
                } else {
                    batch.setColor(Color.BLUE);
                }

                float posX = x * (CELL_SIZE + CELL_GAP);
                float posY = y * (CELL_SIZE + CELL_GAP);

                batch.draw(pixelTexture, posX, posY, CELL_SIZE, CELL_SIZE);
            }
        }
        if (showDebug) {
            float visibleW = camera.viewportWidth / camera.zoom;
            float visibleH = camera.viewportHeight / camera.zoom;

            debugFont.draw(batch, "Zoom: " + String.format("%.2f", camera.zoom), 10, Gdx.graphics.getHeight() - 40);
            debugFont.draw(batch, "Visible: " + String.format("%.0f", visibleW) + "x" + String.format("%.0f", visibleH), 10, Gdx.graphics.getHeight() - 70);
            debugFont.draw(batch, "Mapwidthpixels: " + String.format("%.0f", mapWidthPixels) + "x" + String.format("%.0f", mapHeightPixels), 10, Gdx.graphics.getHeight() - 100);
            debugFont.draw(batch, "Cam Pos: " + String.format("%.1f", camera.position.x) + ", " + String.format("%.1f", camera.position.y), 10, Gdx.graphics.getHeight() - 130);
            float visibleWidth = camera.viewportWidth / camera.zoom;
            float visibleHeight = camera.viewportHeight / camera.zoom;
            float minX = visibleW / 2;
            float maxX = mapWidthPixels - visibleW / 2;
            float minY = visibleH / 2;
            float maxY = mapHeightPixels - visibleH / 2;
            debugFont.draw(batch, "visiblewidth*height: " + String.format("%.1f", visibleWidth) + " to " + String.format("%.1f", visibleHeight), 10, Gdx.graphics.getHeight() - 220);
            debugFont.draw(batch, "zoom: " + String.format("%.1f", camera.zoom), 10, Gdx.graphics.getHeight() - 250);
            debugFont.draw(batch, "Clamp X: " + String.format("%.1f", minX) + " to " + String.format("%.1f", maxX), 10, Gdx.graphics.getHeight() - 160);
            debugFont.draw(batch, "Clamp Y: " + String.format("%.1f", minY) + " to " + String.format("%.1f", maxY), 10, Gdx.graphics.getHeight() - 190);
        }
        batch.setColor(Color.WHITE);
        batch.end();
    }

    private void handleInput() {
        boolean p0 = Gdx.input.isTouched(0);
        boolean p1 = Gdx.input.isTouched(1);

        // Зум двумя пальцами
        if (p0 && p1) {
            if (initialPinchDistance < 0) {
                initialPinchDistance = getPinchDistance();
                zoomStart = camera.zoom;
            } else {
                float cur = getPinchDistance();
                float scale = cur / initialPinchDistance;
                if (Math.abs(scale - 1f) > 0.02f) {
                    camera.zoom = MathUtils.clamp(zoomStart / scale, 0.25f, 1.5f);
                }
            }
        } else {
            initialPinchDistance = -1;

            // Перетаскивание одним пальцем — ЧЕРЕЗ unproject
            if (p0) {
                float x = Gdx.input.getX(0);
                float y = Gdx.input.getY(0);

                if (!isDragging) {
                    // Начало драга
                    isDragging = true;
                    touchStart.set(x, y, 0);
                    cameraStartPos.set(camera.position);
                } else {
                    // Конвертируем ОБЕ точки в мировые координаты
                    Vector3 startWorld = camera.unproject(new Vector3(touchStart.x, touchStart.y, 0));
                    Vector3 curWorld = camera.unproject(new Vector3(x, y, 0));

                    // Дельта в мировых координатах
                    float worldDx = curWorld.x - startWorld.x;
                    float worldDy = curWorld.y - startWorld.y;

                    // Двигаем камеру в ПРОТИВОПОЛОЖНУЮ сторону от движения пальца
                    camera.position.set(
                        cameraStartPos.x - worldDx,
                        cameraStartPos.y - worldDy,
                        camera.position.z
                    );
                }
            } else {
                isDragging = false;
            }
        }
    }

    private float getPinchDistance() {
        float x1 = Gdx.input.getX(0);
        float y1 = Gdx.graphics.getHeight() - Gdx.input.getY(0);
        float x2 = Gdx.input.getX(1);
        float y2 = Gdx.graphics.getHeight() - Gdx.input.getY(1);

        float dx = x1 - x2;
        float dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private void clampCamera() {
        // Видимая область в мировых координатах (с учётом зума)
        float visibleWidth = camera.viewportWidth * camera.zoom;
        float visibleHeight = camera.viewportHeight * camera.zoom;

        float halfVisibleW = visibleWidth / 2;
        float halfVisibleH = visibleHeight / 2;

        float bufferFactor = 0.7f;
        float bufferW = halfVisibleW * bufferFactor;
        float bufferH = halfVisibleH * bufferFactor;

        // Ограничиваем X с буфером
        float minX = halfVisibleW - bufferW;                    // Край карты может уйти к центру
        float maxX = mapWidthPixels - halfVisibleW + bufferW;   // с другой стороны тоже

        if (minX < maxX) {
            camera.position.x = MathUtils.clamp(camera.position.x, minX, maxX);
        } else {
            camera.position.x = mapWidthPixels / 2;
        }

        // Ограничиваем Y с буфером
        float minY = halfVisibleH - bufferH;
        float maxY = mapHeightPixels - halfVisibleH + bufferH;

        if (minY < maxY) {
            camera.position.y = MathUtils.clamp(camera.position.y, minY, maxY);
        } else {
            camera.position.y = mapHeightPixels / 2;
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        image.dispose();
        pixelTexture.dispose();

        debugFont.dispose();
    }
}
