package ru.triplethall.rpgturnbased;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.graphics.Color;

import ru.triplethall.rpgturnbased.GameMap;
import ru.triplethall.rpgturnbased.Player;
import ru.triplethall.rpgturnbased.PauseMenu;

public class RPGTurnbased extends ApplicationAdapter {
    private SpriteBatch batch;
    private CameraControl cameraControl;
    private MapRenderer mapRenderer;
    private PauseMenu pauseMenu;
    private com.badlogic.gdx.graphics.Texture whitePixel;
    private boolean isPaused = false;
    private OrthographicCamera uiCamera;
    private Texture image;
    private GameMap gameMap;
    private Player player;
    private BitmapFont font;
    private Texture pixelTexture;
    private final int CELL_SIZE = 32;
    private final int CELL_GAP = 4;
    private float mapWidthPixels;
    private float mapHeightPixels;

    @Override
    public void create() {
        batch = new SpriteBatch();

        gameMap = new GameMap(21, 21);
        gameMap.generate();


        // Размер карты в пикселях
        mapWidthPixels = gameMap.getWidth() * (CELL_SIZE + CELL_GAP);
        mapHeightPixels = gameMap.getHeight() * (CELL_SIZE + CELL_GAP);

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
        OrthographicCamera camera = new OrthographicCamera();
        camera.setToOrtho(false, viewWidth, viewHeight);
        camera.position.set(mapWidthPixels / 2f, mapHeightPixels / 2f, 0);
        camera.zoom = 1f;
        camera.update();
        cameraControl = new CameraControl(camera, mapWidthPixels, mapHeightPixels);

        mapRenderer = new MapRenderer(gameMap, CELL_SIZE, CELL_GAP);

        font = new BitmapFont();
        font.setColor(Color.YELLOW);
        font.getData().setScale(1.5f);

        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(com.badlogic.gdx.graphics.Color.WHITE);
        pixmap.fill();
        whitePixel = new com.badlogic.gdx.graphics.Texture(pixmap);
        pixmap.dispose();

        pauseMenu = new PauseMenu(font, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        player = new Player();
        player.spawnOnShore(gameMap);
    }

    @Override
    public void render() {
        boolean menuClicked = pauseMenu.handleInput(player);
        isPaused = pauseMenu.isVisible();

        if (!isPaused && !menuClicked) {
            handlePlayerInput();
        }

        ScreenUtils.clear(0.1f, 0.1f, 0.2f, 1f);

        cameraControl.update();


        batch.setProjectionMatrix(cameraControl.getCamera().combined);
        batch.begin();
        mapRenderer.render(batch, player);
        player.render(batch, font, CELL_SIZE, CELL_GAP);
        batch.end();


        OrthographicCamera uiCamera = new OrthographicCamera();
        uiCamera.setToOrtho(true, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        pauseMenu.render(batch, whitePixel, player);
        batch.end();
    }

    private Vector3 screenToGrid(float screenX, float screenY) {
        Vector3 world = cameraControl.getCamera().unproject(new Vector3(screenX, screenY, 0));
        int gridX = (int)(world.x / (CELL_SIZE + CELL_GAP));
        int gridY = (int)(world.y / (CELL_SIZE + CELL_GAP));
        return new Vector3(gridX, gridY, 0);
    }

    private void handlePlayerInput() {
        if (Gdx.input.justTouched() && !cameraControl.isDragging()) {
            float touchX = Gdx.input.getX();
            float touchY = Gdx.input.getY();

            Vector3 grid = screenToGrid(touchX, touchY);
            int targetX = (int)grid.x;
            int targetY = (int)grid.y;

            player.tryMoveTo(targetX, targetY, gameMap);
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        if (image != null) image.dispose();
        if (pixelTexture != null) pixelTexture.dispose();
        if (whitePixel != null) whitePixel.dispose();
        mapRenderer.dispose();
        font.dispose();
    }
}
