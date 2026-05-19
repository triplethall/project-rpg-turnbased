package ru.triplethall.rpgturnbased;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;

public class RPGTurnbased extends ApplicationAdapter implements ClassSelectionListener {
    private SpriteBatch batch;
    private QuestGiverMenu questGiverMenu;      // NEW
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
    private Texture pauseButtonTexture;
    private Texture inventoryButtonTexture;
    private Texture continueButtonTexture;
    private Texture exitButtonTexture;
    private Texture barTexture;
    private Texture statsButtonTexture;
    private Texture pauseBackgroundTexture;
    private Texture statsBackgroundTexture;
    private Texture settingsButtonTexture;
    private Texture openButtonTexture;
    private Texture attackButtonTexture;
    private Texture ignoreButtonTexture;
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

    // Поля для пещеры
    private boolean inCave = false;
    private CaveMap caveMap;
    private CaveRenderer caveRenderer;
    private int cavePlayerX, cavePlayerY;
    private int surfaceEntryX, surfaceEntryY;

    private MainUI mainUI;

    private Texture attackTexture;
    private Texture nextTurnTexture;
    private Texture fleeTexture;
    private Texture logsTexture;

    // ---  метод для проверки открытых окон ---
    private boolean isAnyModalOpen() {
        return isPaused
            || (inventory != null && inventory.isVisible())
            || (pauseMenu != null && pauseMenu.isStatsVisible())
            || (chestMenu != null && chestMenu.isVisible())
            || (cityMenu != null && cityMenu.isVisible())
            || (shopMenu != null && shopMenu.isVisible())
            || (caveMenu != null && caveMenu.isVisible())
            || (questGiverMenu != null && questGiverMenu.isVisible());      // NEW
    }

    @Override
    public void create() {
        font = new BitmapFont();
        font.setColor(Color.YELLOW);
        font.getData().setScale(1.5f);

        chestClosed = new Texture("bg/chest_closed.png");
        chestOpen = new Texture("bg/chest_open.png");

        questGiverMenu = new QuestGiverMenu(font, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());   // NEW

        cityMenu = new CityMenu(font, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
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

        mapRenderer = new MapRenderer(gameMap, CELL_SIZE, CELL_GAP, chestClosed, chestOpen, font);

        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(com.badlogic.gdx.graphics.Color.WHITE);
        pixmap.fill();
        whitePixel = new Texture(pixmap);
        pixmap.dispose();

        // Инициализация пещеры (временная, будет перегенерирована при входе)
        caveMap = new CaveMap(25, 25);
        caveRenderer = new CaveRenderer(caveMap, CELL_SIZE, CELL_GAP, whitePixel);

        pauseButtonTexture = new Texture("pauseButton.png");
        inventoryButtonTexture = new Texture("inventorybtn.png");
        statsButtonTexture = new Texture("statsbtn.png");
        BGArena = new Texture("bg/forest_light_arena.png");
        statsBackgroundTexture = new Texture("menus/bgs/statsmenubg.png");
        continueButtonTexture = new Texture("menus/buttons/continue.png");
        exitButtonTexture = new Texture("menus/buttons/exit.png");
        settingsButtonTexture = new Texture("menus/buttons/options.png");
        pauseBackgroundTexture = new Texture("menus/bgs/menubg.png");
        openButtonTexture = new Texture("menus/buttons/OPEN_BUTTON.png");
        attackButtonTexture = new Texture("menus/buttons/ATTACK_BUTTON.png");
        ignoreButtonTexture = new Texture("menus/buttons/CLOSE_BUTTON.png");

        try {
            barTexture = new Texture("playerbarsbg.png");
        } catch (Exception e) {
            Gdx.app.error("RPG", "playerbarsbg.png not found, using whitePixel");
            barTexture = whitePixel;
        }

        try {
            attackTexture = new Texture("arena_gui/attackbtn.png");
            nextTurnTexture = new Texture("arena_gui/nextturnbtn.png");
            fleeTexture = new Texture("arena_gui/escapebtn.png");
            logsTexture = new Texture("arena_gui/logsbtn.png");
        } catch (Exception e) {
            Gdx.app.error("RPG", "Failed to load battle button textures, using whitePixel");
            attackTexture = whitePixel;
            nextTurnTexture = whitePixel;
            fleeTexture = whitePixel;
            logsTexture = whitePixel;
        }

        SoundManager.playMusic("music/mainMenu.mp3", true);

        uiCamera = new OrthographicCamera();
        uiCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        battleScene = new BattleScene(
            font,
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight(),
            gameMap,
            BGArena,
            whitePixel,
            barTexture,
            attackTexture,
            nextTurnTexture,
            fleeTexture,
            logsTexture
        );
        battleScene.loadAssets();

        pauseMenu = new PauseMenu(font, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(),
            pauseButtonTexture, statsBackgroundTexture, continueButtonTexture, exitButtonTexture, pauseBackgroundTexture, settingsButtonTexture);

        inventory = new Inventory(font, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), inventoryButtonTexture);

        player = new Player();
        player.loadMapModel();
        shopMenu = new ShopMenu(font, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), inventory, player);
        player.spawnOnShore(gameMap);

        chestMenu = new ChestMenu(font, player, gameMap, battleScene, openButtonTexture, attackButtonTexture, ignoreButtonTexture, statsBackgroundTexture);

        player.setOnEnterForest(new Player.OnEnterForestListener() {
            @Override
            public void onEnterForest(int x, int y) {
                battleScene.startBattle(x, y, 1);
            }
        });
        player.syncRenderPos(CELL_SIZE, CELL_GAP);
        battleScene.setPlayer(player);

        mainUI = new MainUI(
            font,
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight(),
            whitePixel,
            barTexture,
            pauseButtonTexture,
            inventoryButtonTexture,
            statsButtonTexture,
            pauseMenu,
            inventory,
            player
        );

        gameStarted = false;
    }

    @Override
    public void render() {
        // ---------- Главное меню ----------
        if (!gameStarted && !isSelectingClass) {
            if (mainMenu != null) {
                mainMenu.handleInput();
                ScreenUtils.clear(0.05f, 0.05f, 0.1f, 1f);
                batch.setProjectionMatrix(uiCamera.combined);
                mainMenu.render(batch, shapeRenderer);
            }
            return;
        }

        // ---------- Выбор класса ----------
        if (isSelectingClass) {
            ScreenUtils.clear(0.05f, 0.05f, 0.1f, 1f);
            batch.setProjectionMatrix(uiCamera.combined);
            classSelectionMenu.handleInput();
            classSelectionMenu.render(batch, shapeRenderer);
            return;
        }

        // ---------- Обработка ввода (пещера или поверхность) ----------
        if (inCave) {
            handleCaveInput();
        } else {
            // Обработка ввода на поверхности
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
            boolean questMenuClicked = questGiverMenu.handleInput();    // NEW

            float touchX = Gdx.input.getX();
            float touchY = Gdx.input.getY();
            boolean uiHandled = false;
            if (!battleScene.isActive() && !isPaused && !chestMenu.isVisible() && !cityMenu.isVisible() && !shopMenu.isVisible() && !caveMenu.isVisible()) {
                uiHandled = mainUI.handleInput(touchX, touchY);
            }

            if (battleScene.isActive()) {
                battleScene.update(Gdx.graphics.getDeltaTime());
                battleScene.handleInput(player);
            } else if (!isPaused && !menuClicked && !chestMenu.isVisible() && !cityMenu.isVisible() && !shopMenu.isVisible() && !caveMenu.isVisible() && !uiHandled && !isAnyModalOpen()) {
                handlePlayerInput();
            }
            player.updateMovement(Gdx.graphics.getDeltaTime());
        }

        // ---------- Рендер мира (пещера или поверхность) ----------
        ScreenUtils.clear(0.1f, 0.1f, 0.2f, 1f);

        if (inCave) {
            // Рендер пещеры
            batch.setProjectionMatrix(cameraControl.getCamera().combined);
            batch.begin();
            caveRenderer.render(batch);
            // Отрисовка игрока в пещере (жёлтый квадрат)
            float playerX = cavePlayerX * (CELL_SIZE + CELL_GAP);
            float playerY = cavePlayerY * (CELL_SIZE + CELL_GAP);
            batch.setColor(Color.GOLD);
            batch.draw(whitePixel, playerX, playerY, CELL_SIZE, CELL_SIZE);
            batch.setColor(Color.WHITE);
            batch.end();
        } else {
            // Рендер основной карты
            if (!isPaused && !isAnyModalOpen()) {
                cameraControl.update();
            }
            mapRenderer.update(Gdx.graphics.getDeltaTime());

            if (!battleScene.isShowingEndScreen()) {
                batch.setProjectionMatrix(cameraControl.getCamera().combined);
                batch.begin();
                mapRenderer.render(batch, player);
                player.render(batch, font, CELL_SIZE, CELL_GAP);
                batch.end();
            }
        }

        // ---------- Рендер UI (общий) ----------
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();

        if (!battleScene.isActive()) {
            mainUI.render(batch);
        }

        chestMenu.render(batch, whitePixel);
        pauseMenu.render(batch, whitePixel, player);
        inventory.render(batch, whitePixel, player);
        if (battleScene.isActive()) battleScene.render(batch, whitePixel, player);
        cityMenu.render(batch, shapeRenderer);
        caveMenu.render(batch, shapeRenderer);
        shopMenu.render(batch, shapeRenderer, whitePixel);
        questGiverMenu.render(batch, shapeRenderer, whitePixel);        // NEW

        if (batch.isDrawing()) batch.end();
    }

    // Обработка ввода на поверхности
    private void handlePlayerInput() {
        if (player.isMoving()) return;
        if (Gdx.input.justTouched() && !cameraControl.isDragging()) {
            Vector3 grid = screenToGrid(Gdx.input.getX(), Gdx.input.getY());
            int targetX = (int) grid.x;
            int targetY = (int) grid.y;

            if (gameMap.getTerrain(targetX, targetY) == TerrainType.CAVEENTRANCE) {
                int dx = Math.abs(player.getX() - targetX);
                int dy = Math.abs(player.getY() - targetY);
                boolean isNear = (dx <= 1 && dy <= 1) && !(dx == 0 && dy == 0);
                if (isNear) {
                    surfaceEntryX = targetX;
                    surfaceEntryY = targetY;
                    enterCave();
                }
                return;
            }

            if (gameMap.getTerrain(targetX, targetY) == TerrainType.CITY || gameMap.getTerrain(targetX, targetY) == TerrainType.CITYANCHOR) {
                int dx = Math.abs(player.getX() - targetX);
                int dy = Math.abs(player.getY() - targetY);
                boolean isNear = (dx <= 1 && dy <= 1) && !(dx == 0 && dy == 0);
                if (isNear) {
                    cityMenu.show();
                }
                return;
            }
            // NEW / UPDATE
            TerrainType terrain = gameMap.getTerrain(targetX, targetY);
            if (player.tryMoveTo(targetX, targetY, gameMap, CELL_SIZE, CELL_GAP)) {
                SoundManager.playSound("sounds/step.mp3");
                if (terrain == TerrainType.Chest) {
                    int mimicSize = gameMap.getMimicSize(targetX, targetY);
                    chestMenu.show(targetX, targetY, mimicSize);
                } else if (terrain == TerrainType.QUEST_GIVER) {
                    QuestGiver qg = gameMap.getQuestGiver(targetX, targetY);
                    if (qg != null) questGiverMenu.show(qg);
                }
            }
            if (gameMap.getTerrain(targetX, targetY) == TerrainType.ENEMY) {
                battleScene.startBattle(targetX, targetY);
            }
        }
    }

    private void handleCaveInput() {
        if (Gdx.input.justTouched() && !cameraControl.isDragging()) {
            Vector3 grid = screenToGrid(Gdx.input.getX(), Gdx.input.getY());
            int targetX = (int) grid.x;
            int targetY = (int) grid.y;
            if (targetX >= 0 && targetX < caveMap.getWidth() && targetY >= 0 && targetY < caveMap.getHeight()) {
                if (caveMap.isWalkable(targetX, targetY)) {
                    cavePlayerX = targetX;
                    cavePlayerY = targetY;
                    cameraControl.getCamera().position.set(
                        cavePlayerX * (CELL_SIZE + CELL_GAP) + CELL_SIZE / 2f,
                        cavePlayerY * (CELL_SIZE + CELL_GAP) + CELL_SIZE / 2f,
                        0
                    );
                    cameraControl.getCamera().update();
                    if (caveMap.getTile(targetX, targetY) == CaveTile.EXIT) {
                        exitCave();
                    }
                }
            }
        }
    }

    private void enterCave() {
        caveMap = new CaveMap(25, 25);
        caveRenderer = new CaveRenderer(caveMap, CELL_SIZE, CELL_GAP, whitePixel);
        cavePlayerX = caveMap.getPlayerStartX();
        cavePlayerY = caveMap.getPlayerStartY();
        inCave = true;
        cameraControl.getCamera().position.set(
            cavePlayerX * (CELL_SIZE + CELL_GAP) + CELL_SIZE / 2f,
            cavePlayerY * (CELL_SIZE + CELL_GAP) + CELL_SIZE / 2f,
            0
        );
        cameraControl.getCamera().update();
    }

    private void exitCave() {
        inCave = false;
        player.setX(surfaceEntryX);
        player.setY(surfaceEntryY);
        player.syncRenderPos(CELL_SIZE, CELL_GAP);
        cameraControl.getCamera().position.set(
            player.getX() * (CELL_SIZE + CELL_GAP) + CELL_SIZE / 2f,
            player.getY() * (CELL_SIZE + CELL_GAP) + CELL_SIZE / 2f,
            0
        );
        cameraControl.getCamera().update();
    }

    private Vector3 screenToGrid(float screenX, float screenY) {
        Vector3 world = cameraControl.getCamera().unproject(new Vector3(screenX, screenY, 0));
        int gridX = (int) (world.x / (CELL_SIZE + CELL_GAP));
        int gridY = (int) (world.y / (CELL_SIZE + CELL_GAP));
        return new Vector3(gridX, gridY, 0);
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
        if (attackTexture != null) attackTexture.dispose();
        if (nextTurnTexture != null) nextTurnTexture.dispose();
        if (fleeTexture != null) fleeTexture.dispose();
        if (logsTexture != null) logsTexture.dispose();
        if (openButtonTexture != null) openButtonTexture.dispose();
        if (attackButtonTexture != null) attackButtonTexture.dispose();
        if (ignoreButtonTexture != null) ignoreButtonTexture.dispose();
    }
}
