package ru.triplethall.rpgturnbased;

import static java.lang.Math.random;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;

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
    private Texture inventoryButtonTexture;
    private Texture continueButtonTexture;
    private Texture exitButtonTexture;
    private Texture statsButtonTexture;
    private Texture pauseBackgroundTexture;
    private Texture statsBackgroundTexture;
    private Rectangle statsButtonRect;  // прямоугольник кнопки статистики
    private Texture BGArena;
    private final int CELL_SIZE = 32;
    private final int CELL_GAP = 4;
    private float mapWidthPixels;
    private float mapHeightPixels;
    private BattleScene battleScene;
    private ChestMenu chestMenu;
    private Texture chestClosed;
    private Texture chestOpen;
    private ShapeRenderer shapeRenderer;
    private MainMenu mainMenu;
    private boolean gameStarted = false;

    @Override
    public void create() {
        font = new BitmapFont();
        font.setColor(Color.YELLOW);
        font.getData().setScale(1.5f);
        chestClosed = new Texture("bg/chest_closed.png");
        chestOpen = new Texture("bg/chest_open.png");
        chestMenu = new ChestMenu(font);
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

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



        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(com.badlogic.gdx.graphics.Color.WHITE);
        pixmap.fill();
        whitePixel = new com.badlogic.gdx.graphics.Texture(pixmap);
        pauseButtonTexture = new Texture("pauseButton.png");
        inventoryButtonTexture = new Texture("inventorybtn.png");
        statsButtonTexture = new Texture("statsbtn.png");   // загружаем текстуру для статистики
        BGArena = new Texture("bg/forest_light_arena.png");
        statsBackgroundTexture = new Texture("menus/bgs/statsmenubg.png");
        continueButtonTexture = new Texture("menus/buttons/continue.png");
        exitButtonTexture = new Texture("menus/buttons/exit.png");
        pauseBackgroundTexture = new Texture("menus/bgs/menubg.png");
        pixmap.dispose();

        uiCamera = new OrthographicCamera();
        uiCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        battleScene = new BattleScene(font, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), gameMap, BGArena);
        pauseMenu = new PauseMenu(font,
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight(),
            pauseButtonTexture,
            statsBackgroundTexture,
            continueButtonTexture,
            exitButtonTexture,
            pauseBackgroundTexture);

        inventory = new Inventory(font,
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight(),
            inventoryButtonTexture);

        mainMenu = new MainMenu(
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight(),
            this);

        // Создаём прямоугольник для кнопки статистик
        int margin = 20;
        float btnSize = 120;
        float startY = Gdx.graphics.getHeight() - btnSize;
        statsButtonRect = new Rectangle(2 * btnSize + margin, startY, btnSize, btnSize);

        player = new Player();
        player.spawnOnShore(gameMap);
        battleScene.setPlayer(player);
        gameStarted = false;
    }

    @Override
    public void render() {
        if (!gameStarted) {
            mainMenu.handleInput();
            ScreenUtils.clear(0.05f, 0.05f, 0.1f, 1f);
            batch.setProjectionMatrix(uiCamera.combined);
            mainMenu.render(batch, shapeRenderer);
            return;
        }

        boolean menuClicked = pauseMenu.handleInput(player);
        isPaused = pauseMenu.isVisible();
        boolean chestClicked = chestMenu.handleInput();

        // Логика игры (ход игрока, бой, сундук)
        if (battleScene.isActive()) {
            battleScene.handleInput(player);
        } else if (!isPaused && !menuClicked && !chestMenu.isVisible()) {
            handlePlayerInput();
        }

        // Очистка экрана
        ScreenUtils.clear(0.1f, 0.1f, 0.2f, 1f);

        // Обновляем камеру только если меню паузы НЕ активно
        if (!isPaused) {
            cameraControl.update();
        }
        mapRenderer.update(Gdx.graphics.getDeltaTime());

        // Отрисовка игрового мира (карта, игрок)
        batch.setProjectionMatrix(cameraControl.getCamera().combined);
        batch.begin();
        mapRenderer.render(batch, player);
        player.render(batch, font, CELL_SIZE, CELL_GAP);
        batch.end();

        // Обработка инвентаря
        inventory.handleInput(player);

        // Обработка нажатия на кнопку статистики
        if (Gdx.input.justTouched()) {
            float touchX = Gdx.input.getX();
            float touchY = Gdx.input.getY();
            float gameY = Gdx.graphics.getHeight() - touchY; // преобразуем в координаты UI (начало снизу)
            if (statsButtonRect.contains(touchX, gameY)) {
                pauseMenu.toggleStats();
            }
        }

        // Отрисовка интерфейса (меню паузы, сундук, инвентарь, кнопка статистики, бой)
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();

        chestMenu.render(batch, whitePixel);
        pauseMenu.render(batch, whitePixel, player);
        inventory.render(batch, whitePixel, player);

        // Рисуем кнопку статистики
        batch.draw(statsButtonTexture, statsButtonRect.x, statsButtonRect.y, statsButtonRect.width, statsButtonRect.height);

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
    public void startGame()
    {
        gameStarted = true;
        mainMenu.hide();
    }

    @Override
    public void dispose() {
        batch.dispose();
        if (image != null) image.dispose();
        if (pixelTexture != null) pixelTexture.dispose();
        if (whitePixel != null) whitePixel.dispose();
        if (pauseButtonTexture != null) pauseButtonTexture.dispose();
        if (statsButtonTexture != null) statsButtonTexture.dispose();
        if (BGArena != null) BGArena.dispose();
        if (statsBackgroundTexture != null) statsBackgroundTexture.dispose();
        if (continueButtonTexture != null) continueButtonTexture.dispose();
        if (exitButtonTexture != null) exitButtonTexture.dispose();
        if (pauseBackgroundTexture != null) pauseBackgroundTexture.dispose();
        if (mainMenu != null) mainMenu.dispose();
        mapRenderer.dispose();
        font.dispose();
    }
}
