package ru.triplethall.rpgturnbased;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import kotlin.Pair;
import java.util.ArrayList;
import java.util.List;

public class RPGTurnbased extends ApplicationAdapter implements ClassSelectionListener {
    private SpriteBatch batch;
    private CameraControl cameraControl;
    private MapRenderer mapRenderer;
    private Inventory inventory;
    private PauseMenu pauseMenu;
    private Texture whitePixel;
    private boolean isPaused = false;
    private OrthographicCamera uiCamera;
    private GameMap gameMap;
    private Player player;
    private BitmapFont font;
    private Texture pixelTexture;
    private Texture pauseButtonTexture;
    private Texture inventoryButtonTexture;
    private Texture continueButtonTexture;
    private Texture exitButtonTexture;
    private Texture barTexture;
    private Texture statsButtonTexture;
    private Texture pauseBackgroundTexture;
    private Texture statsBackgroundTexture;
    private Texture settingsButtonTexture;
    private Rectangle statsButtonRect;
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
    private CityMenu cityMenu;
    private CaveMenu caveMenu;
    private ClassSelectionMenu classSelectionMenu;
    private boolean isSelectingClass = false;
    private PlayerClasses selectedPlayerClass = null;
    private ShopMenu shopMenu;

    @Override
    public void create() {
        font = new BitmapFont();
        font.setColor(Color.YELLOW);
        font.getData().setScale(1.5f);

        chestClosed = new Texture("bg/chest_closed.png");
        chestOpen = new Texture("bg/chest_open.png");

        cityMenu = new CityMenu(font, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        // Инициализация CaveMenu (проверь конструктор, если нужны параметры)
        caveMenu = new CaveMenu(font, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        classSelectionMenu = new ClassSelectionMenu(
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight(),
            this
        );

        mainMenu = new MainMenu(
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight(),
            this
        );

        gameMap = new GameMap(21, 21, chestMenu);
        gameMap.generate(1, 1);

        mapWidthPixels = gameMap.getWidth() * (CELL_SIZE + CELL_GAP);
        mapHeightPixels = gameMap.getHeight() * (CELL_SIZE + CELL_GAP);

        float screenRatio = (float) Gdx.graphics.getWidth() / Gdx.graphics.getHeight();
        float viewWidth, viewHeight;
        if (screenRatio > 1f) {
            viewHeight = mapHeightPixels;
            viewWidth = viewHeight * screenRatio;
        } else {
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
        whitePixel = new Texture(pixmap);
        pixmap.dispose();

        pauseButtonTexture = new Texture("pauseButton.png");
        inventoryButtonTexture = new Texture("inventorybtn.png");
        statsButtonTexture = new Texture("statsbtn.png");
        BGArena = new Texture("bg/forest_light_arena.png");
        statsBackgroundTexture = new Texture("menus/bgs/statsmenubg.png");
        continueButtonTexture = new Texture("menus/buttons/continue.png");
        exitButtonTexture = new Texture("menus/buttons/exit.png");
        settingsButtonTexture = new Texture("menus/buttons/options.png");
        pauseBackgroundTexture = new Texture("menus/bgs/menubg.png");

        try {
            barTexture = new Texture("playerbarsbg.png");
        } catch (Exception e) {
            Gdx.app.error("RPG", "playerbarsbg.png not found, using whitePixel");
            barTexture = whitePixel;
        }

        SoundManager.playMusic("music/mainMenu.mp3", true);

        uiCamera = new OrthographicCamera();
        uiCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        battleScene = new BattleScene(font, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), gameMap, BGArena, whitePixel, barTexture);
        battleScene.loadAssets();

        pauseMenu = new PauseMenu(font, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(),
            pauseButtonTexture, statsBackgroundTexture, continueButtonTexture, exitButtonTexture, pauseBackgroundTexture, settingsButtonTexture);

        inventory = new Inventory(font, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), inventoryButtonTexture);

        float btnSize = 120f;
        float startY = Gdx.graphics.getHeight() - 140f;
        statsButtonRect = new Rectangle(270f, startY, btnSize, btnSize);
        player = new Player();
        player.loadMapModel();
        shopMenu = new ShopMenu(font, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), inventory, player);
        player.spawnOnShore(gameMap);

        chestMenu = new ChestMenu(font, player, gameMap, battleScene);

        player.setOnEnterForest(new Player.OnEnterForestListener() {
            @Override
            public void onEnterForest(int x, int y) {
                battleScene.startBattle(x, y, 1);
            }
        });
        player.syncRenderPos(CELL_SIZE, CELL_GAP); // Синхронизация стартовой позиции
        battleScene.setPlayer(player);
        gameStarted = false;
    }

    @Override
    public void render() {
        // Главное меню
        if (!gameStarted && !isSelectingClass) {
            if (mainMenu != null) {

                mainMenu.handleInput();
                ScreenUtils.clear(0.05f, 0.05f, 0.1f, 1f);
                batch.setProjectionMatrix(uiCamera.combined);
                mainMenu.render(batch, shapeRenderer);
            }
            return;
        }

        // Выбор класса
        if (isSelectingClass) {
            ScreenUtils.clear(0.05f, 0.05f, 0.1f, 1f);
            batch.setProjectionMatrix(uiCamera.combined);
            // batch.begin();  ← ЗАКОММЕНТИРУЙ ЭТУ СТРОКУ
            classSelectionMenu.handleInput();
            classSelectionMenu.render(batch, shapeRenderer);
            // batch.end();    ← И ЭТУ

            return;
        }

        // Обработка меню
        boolean menuClicked = pauseMenu.handleInput(player);
        isPaused = pauseMenu.isVisible();
        chestMenu.handleInput();
        boolean cityMenuClicked = cityMenu.handleInput();
        boolean caveMenuClicked = caveMenu.handleInput();
        boolean shopClicked = false;
        if (cityMenu.isShopClicked()) {
            shopMenu.show();
            shopClicked = true;
        }
        boolean shopMenuClicked = shopMenu.handleInput();

        // Игровой цикл
        if (battleScene.isActive()) {
            battleScene.update(Gdx.graphics.getDeltaTime());
            battleScene.handleInput(player);
        } else if (!isPaused && !menuClicked && !chestMenu.isVisible() && !cityMenu.isVisible() && !shopMenu.isVisible() && !caveMenu.isVisible()) {
            handlePlayerInput();
        }
        player.updateMovement(Gdx.graphics.getDeltaTime());



        // Рендер мира
        ScreenUtils.clear(0.1f, 0.1f, 0.2f, 1f);
        if (!isPaused) cameraControl.update();
        mapRenderer.update(Gdx.graphics.getDeltaTime());

        if (!battleScene.isShowingEndScreen()) {
            batch.setProjectionMatrix(cameraControl.getCamera().combined);
            batch.begin();
            mapRenderer.render(batch, player);
            player.render(batch, font, CELL_SIZE, CELL_GAP);
            batch.end();
        }

        inventory.handleInput(player);

        // Тач для статистики
        if (Gdx.input.justTouched()) {
            float touchX = Gdx.input.getX();
            float touchY = Gdx.input.getY();
            float gameY = Gdx.graphics.getHeight() - touchY;
            if (statsButtonRect.contains(touchX, gameY)) {
                pauseMenu.toggleStats();
            }
        }

        // Рендер UI
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();

        chestMenu.render(batch, whitePixel);
        pauseMenu.render(batch, whitePixel, player);
        inventory.render(batch, whitePixel, player);

        // Ручная отрисовка баров
        if (!battleScene.isActive()) {
            // Параметры расположения и размеров (подобраны, чтобы не перекрывать кнопки)
            float bgX = 20f;
            float bgY = Gdx.graphics.getHeight() - 250f;
            float bgWidth = 500f;
            float bgHeight = 140f;

            // Рисуем фон (текстура бара)
            batch.draw(barTexture, bgX, bgY, bgWidth, bgHeight);

            // Параметры заливки (как в BattleScene)
            float fillXOffset = 35f;      // отступ слева для полоски
            float fillHeight = 40f;       // высота цветной заливки
            float hpFillYOffset = 23f;    // смещение полоски HP внутри фона
            float mpFillYOffset = 55f;    // смещение полоски MP

            // --- HP ---
            float hpFillX = bgX + fillXOffset;
            float hpFillY = bgY + hpFillYOffset;
            float hpFullW = bgWidth - fillXOffset * 2;
            float hpPercent = (float) player.getCurrentHealth() / player.getMaxHealth();
            float hpFillW = hpFullW * hpPercent;

            // Чёрный фон под полоской HP
            batch.setColor(Color.BLACK);
            batch.draw(whitePixel, hpFillX, hpFillY, hpFullW, fillHeight);
            // Красная полоска HP
            batch.setColor(Color.RED);
            batch.draw(whitePixel, hpFillX, hpFillY, hpFillW, fillHeight);
            // Эффект объёма для HP (тёмная полоска сверху, светлая снизу)
            batch.setColor(new Color(0f, 0f, 0f, 0.2f));
            batch.draw(whitePixel, hpFillX, hpFillY, hpFillW, fillHeight / 2f);
            batch.setColor(new Color(1f, 1f, 1f, 0.25f));
            batch.draw(whitePixel, hpFillX, hpFillY + fillHeight * 0.75f, hpFillW, fillHeight / 4f);

            // --- MP ---
            float mpFillX = bgX + fillXOffset;
            float mpFillY = bgY + mpFillYOffset;
            float mpFullW = bgWidth - fillXOffset * 2;
            float mpPercent = (float) player.getCurrentMana() / player.getMaxMana();
            float mpFillW = mpFullW * mpPercent;

            batch.setColor(Color.BLACK);
            batch.draw(whitePixel, mpFillX, mpFillY, mpFullW, fillHeight);
            batch.setColor(Color.BLUE);
            batch.draw(whitePixel, mpFillX, mpFillY, mpFillW, fillHeight);
            batch.setColor(new Color(0f, 0f, 0f, 0.2f));
            batch.draw(whitePixel, mpFillX, mpFillY, mpFillW, fillHeight / 2f);
            batch.setColor(new Color(1f, 1f, 1f, 0.25f));
            batch.draw(whitePixel, mpFillX, mpFillY + fillHeight * 0.75f, mpFillW, fillHeight / 4f);

            // Текст с тенью (как в BattleScene)
            GlyphLayout layout = new GlyphLayout();
            font.getData().setScale(1.2f);

            String hpText = player.getCurrentHealth() + "/" + player.getMaxHealth();
            layout.setText(font, hpText);
            float hpTextX = hpFillX + (hpFullW - layout.width) / 2;
            float hpTextY = hpFillY + (fillHeight + layout.height) / 2;
            font.setColor(Color.BLACK);
            font.draw(batch, hpText, hpTextX + 2f, hpTextY - 2f);
            font.setColor(Color.WHITE);
            font.draw(batch, hpText, hpTextX, hpTextY);

            String mpText = player.getCurrentMana() + "/" + player.getMaxMana();
            layout.setText(font, mpText);
            float mpTextX = mpFillX + (mpFullW - layout.width) / 2;
            float mpTextY = mpFillY + (fillHeight + layout.height) / 2;
            font.setColor(Color.BLACK);
            font.draw(batch, mpText, mpTextX + 2f, mpTextY - 2f);
            font.setColor(Color.WHITE);
            font.draw(batch, mpText, mpTextX, mpTextY);

            // Сброс цвета и масштаба
            batch.setColor(Color.WHITE);
            font.getData().setScale(1f);
        }

        batch.draw(statsButtonTexture, statsButtonRect.x, statsButtonRect.y, statsButtonRect.width, statsButtonRect.height);
        if (battleScene.isActive()) battleScene.render(batch, whitePixel, player);
        cityMenu.render(batch, shapeRenderer);
        caveMenu.render(batch, shapeRenderer); // Рендер CaveMenu
        shopMenu.render(batch, shapeRenderer, whitePixel);
        batch.end();
    }

    private void drawBarText(SpriteBatch batch, String text, float x, float y) {
        GlyphLayout layout = new GlyphLayout(font, text);
        font.setColor(Color.BLACK);
        font.draw(batch, text, x + 2f, y - 2f);
        font.setColor(Color.WHITE);
        font.draw(batch, text, x, y);
        font.setColor(Color.WHITE);
    }

    private Vector3 screenToGrid(float screenX, float screenY) {
        Vector3 world = cameraControl.getCamera().unproject(new Vector3(screenX, screenY, 0));
        return new Vector3((int) (world.x / (CELL_SIZE + CELL_GAP)), (int) (world.y / (CELL_SIZE + CELL_GAP)), 0);
    }

    private void handlePlayerInput() {
        if (player.isMoving()) return;
        if (Gdx.input.justTouched() && !cameraControl.isDragging()) {
            Vector3 grid = screenToGrid(Gdx.input.getX(), Gdx.input.getY());
            int targetX = (int) grid.x;
            int targetY = (int) grid.y;
            if (gameMap.getTerrain(targetX, targetY) == TerrainType.CITY || gameMap.getTerrain(targetX, targetY) == TerrainType.CITYANCHOR) {
                int dx = Math.abs(player.getX() - targetX);
                int dy = Math.abs(player.getY() - targetY);
                boolean isNear = (dx <= 1 && dy <= 1) && !(dx == 0 && dy == 0);
                if (isNear) {
                    cityMenu.show();
                }
                return;
            }
            if (player.tryMoveTo(targetX, targetY, gameMap, CELL_SIZE, CELL_GAP))
            {
                SoundManager.playSound("sounds/step.mp3");

                if (gameMap.getTerrain(targetX, targetY) == TerrainType.Chest)
                {
                    int mimicSize = gameMap.getMimicSize(targetX, targetY);
                    chestMenu.show(targetX, targetY, mimicSize);
                }
            }
            if (gameMap.getTerrain(targetX, targetY) == TerrainType.ENEMY) {
                battleScene.startBattle(targetX, targetY);
            }
        }
    }

    private boolean isAdjacent(int tx, int ty) {
        int dx = Math.abs(player.getX() - tx);
        int dy = Math.abs(player.getY() - ty);
        return (dx <= 1 && dy <= 1) && !(dx == 0 && dy == 0);
    }

    @Override
    public void onClassSelected(PlayerClasses playerClass) {
        if (playerClass == null) {
            if (mainMenu != null) mainMenu.show();
            isSelectingClass = false;
        } else {
            selectedPlayerClass = playerClass;
            applyClassToPlayer();
            startGameAfterClassSelection();
        }
    }

    private void applyClassToPlayer() {
        if (selectedPlayerClass != null && player != null) {
            player.changeClass(selectedPlayerClass);
            player.learnSkillsForClass();
        }
    }

    private void startGameAfterClassSelection() {
        isSelectingClass = false;
        gameStarted = true;
        if (mainMenu != null) mainMenu.hide();
        SoundManager.stopMusic();
        SoundManager.startPlaylist(false);
    }

    public void showClassSelection() {
        isSelectingClass = true;
        classSelectionMenu.show();
    }

    public void startGame() {
        showClassSelection();
        if (mainMenu != null) mainMenu.hide();
        SoundManager.stopMusic();
        SoundManager.startPlaylist(false);
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        shapeRenderer.dispose();
        mapRenderer.dispose();
        SoundManager.dispose();

        if (whitePixel != null) whitePixel.dispose();
        if (chestClosed != null) chestClosed.dispose();
        if (chestOpen != null) chestOpen.dispose();
        if (pauseButtonTexture != null) pauseButtonTexture.dispose();
        if (inventoryButtonTexture != null) inventoryButtonTexture.dispose();
        if (continueButtonTexture != null) continueButtonTexture.dispose();
        if (exitButtonTexture != null) exitButtonTexture.dispose();
        if (barTexture != null) barTexture.dispose();
        if (statsButtonTexture != null) statsButtonTexture.dispose();
        if (pauseBackgroundTexture != null) pauseBackgroundTexture.dispose();
        if (statsBackgroundTexture != null) statsBackgroundTexture.dispose();
        if (settingsButtonTexture != null) settingsButtonTexture.dispose();
        if (BGArena != null) BGArena.dispose();
        if (classSelectionMenu != null) classSelectionMenu.dispose();
        if (mainMenu != null) mainMenu.dispose();
    }
}
