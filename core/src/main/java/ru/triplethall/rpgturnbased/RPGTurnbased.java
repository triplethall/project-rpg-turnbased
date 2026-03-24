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
    private Inventory inventory;
    private PauseMenu pauseMenu;
    private com.badlogic.gdx.graphics.Texture whitePixel;
    private boolean isPaused = false;
    private OrthographicCamera uiCamera;
    private Texture image;
    private GameMap gameMap;
    private Player player;
    private BitmapFont font;
    private Texture pixelTexture;
    private Texture pauseButtonTexture;
    private Texture BGArena;
    private final int CELL_SIZE = 32;
    private final int CELL_GAP = 4;
    private float mapWidthPixels;
    private float mapHeightPixels;
    private BattleScene battleScene;
    private ChestMenu chestMenu;
    private Texture chestClosed;
    private Texture chestOpen;

    @Override
    public void create() {
        chestClosed = new Texture("bg/chest_closed.png");
        chestOpen = new Texture("bg/chest_open.png");
        font = new BitmapFont();
        chestMenu = new ChestMenu(font);
        batch = new SpriteBatch();

        gameMap = new GameMap(21, 21,chestMenu);
        gameMap.generate(1,1);


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

        mapRenderer = new MapRenderer(gameMap, CELL_SIZE, CELL_GAP, chestClosed, chestOpen);

        font = new BitmapFont();
        font.setColor(Color.YELLOW);
        font.getData().setScale(1.5f);

        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(com.badlogic.gdx.graphics.Color.WHITE);
        pixmap.fill();
        whitePixel = new com.badlogic.gdx.graphics.Texture(pixmap);
        pauseButtonTexture = new Texture("pauseButton.png");
        BGArena = new Texture("bg/forest_light_arena.png");
        pixmap.dispose();


        battleScene = new BattleScene(font, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), gameMap, BGArena);
        pauseMenu = new PauseMenu(font, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), pauseButtonTexture);
        inventory = new Inventory(font, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        player = new Player();
        player.spawnOnShore(gameMap);
        battleScene.setPlayer(player);
    }

    @Override
    public void render() {

        boolean menuClicked = pauseMenu.handleInput(player);
        isPaused = pauseMenu.isVisible();
        boolean chestClicked = chestMenu.handleInput(); // Обработка клика по EXIT

        // Решаем, может ли игрок ходить
        if (battleScene.isActive()) {
            battleScene.handleInput();
        } else if (!isPaused && !menuClicked && !chestMenu.isVisible()) {
            handlePlayerInput();
        }


        ScreenUtils.clear(0.1f, 0.1f, 0.2f, 1f);
        cameraControl.update();
        mapRenderer.update(Gdx.graphics.getDeltaTime());

        batch.setProjectionMatrix(cameraControl.getCamera().combined);
        batch.begin();
        mapRenderer.render(batch, player);
        player.render(batch, font, CELL_SIZE, CELL_GAP);

        inventory.render(batch, whitePixel, player);
        batch.end();


        uiCamera = new OrthographicCamera();
        uiCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();

        // Рисуем меню сундука поверх всего
        chestMenu.render(batch, whitePixel);

        pauseMenu.render(batch, whitePixel, player);

        if (battleScene.isActive()) {
            battleScene.render(batch, whitePixel, player);
        }
        batch.end();

        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();

        chestMenu.render(batch, whitePixel);
        pauseMenu.render(batch, whitePixel, player);

        if (battleScene.isActive()) {
            battleScene.render(batch, whitePixel, player);
        }
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
            Vector3 grid = screenToGrid(Gdx.input.getX(), Gdx.input.getY());
            int targetX = (int)grid.x;
            int targetY = (int)grid.y;

            // Если игрок успешно сходил
            if (player.tryMoveTo(targetX, targetY, gameMap)) {

                if (gameMap.collectChest(targetX, targetY)) {
                    chestMenu.show();
                }

                if (gameMap.getTerrain(targetX, targetY) == TerrainType.ENEMY) {
                    battleScene.startBattle(targetX, targetY);
                }
            }
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        if (image != null) image.dispose();
        if (pixelTexture != null) pixelTexture.dispose();
        if (whitePixel != null) whitePixel.dispose();
        if (pauseButtonTexture != null) pauseButtonTexture.dispose();
        if (BGArena != null) BGArena.dispose();
        mapRenderer.dispose();
        font.dispose();
    }
}
