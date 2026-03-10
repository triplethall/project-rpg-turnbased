package ru.triplethall.rpgturnbased;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
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

    private Vector3 touchStart = new Vector3();
    private Vector3 cameraStartPos = new Vector3();
    private boolean isDragging = false;
    private float initialPinchDistance = -1;
    private float zoomStart = 1f;

    @Override
    public void create() {
        batch = new SpriteBatch();
        image = new Texture("libgdx.png");

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

        Gdx.app.log("Camera", "Viewport: " + camera.viewportWidth + "x" + camera.viewportHeight);
        Gdx.app.log("Camera", "Map: " + mapWidthPixels + "x" + mapHeightPixels);
        Gdx.app.log("Camera", "Screen: " + Gdx.graphics.getWidth() + "x" + Gdx.graphics.getHeight());
    }

    @Override
    public void render() {
        handleInput();
        //clampCamera();

        ScreenUtils.clear(0.1f, 0.1f, 0.2f, 1f);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();

        for (int x = 0; x < gameMap.getWidth(); x++) {
            for (int y = 0; y < gameMap.getHeight(); y++) {
                if (gameMap.isWalkable(x, y)) {
                    batch.setColor(Color.GRAY);
                } else {
                    batch.setColor(Color.RED);
                }

                float posX = x * (CELL_SIZE + CELL_GAP);
                float posY = y * (CELL_SIZE + CELL_GAP);

                batch.draw(pixelTexture, posX, posY, CELL_SIZE, CELL_SIZE);
            }
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
                    camera.zoom = MathUtils.clamp(zoomStart / scale, 0.25f, 10f);
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
        float visibleWidth = camera.viewportWidth / camera.zoom;
        float visibleHeight = camera.viewportHeight / camera.zoom;

        // Половина видимой области
        float halfVisibleW = visibleWidth / 2;
        float halfVisibleH = visibleHeight / 2;

        // Ограничиваем X: центр камеры не может уйти дальше края карты
        float minX = halfVisibleW;
        float maxX = mapWidthPixels - halfVisibleW;

        // Если карта меньше экрана — центрируем
        if (minX >= maxX) {
            camera.position.x = mapWidthPixels / 2;
        } else {
            camera.position.x = MathUtils.clamp(camera.position.x, minX, maxX);
        }

        // Ограничиваем Y: центр камеры не может уйти дальше края карты
        float minY = halfVisibleH;
        float maxY = mapHeightPixels - halfVisibleH;

        // Если карта меньше экрана — центрируем
        if (minY >= maxY) {
            camera.position.y = mapHeightPixels / 2;
        } else {
            camera.position.y = MathUtils.clamp(camera.position.y, minY, maxY);
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        image.dispose();
        pixelTexture.dispose();
    }
}
