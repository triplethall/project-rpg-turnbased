package ru.triplethall.rpgturnbased;

import static java.lang.Math.random;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import java.util.ArrayList;
import java.util.List;
import kotlin.Pair;

import ru.triplethall.rpgturnbased.GameMap;
import ru.triplethall.rpgturnbased.Player;
import ru.triplethall.rpgturnbased.PauseMenu;
import ru.triplethall.rpgturnbased.SoundManager;

public class RPGTurnbased extends ApplicationAdapter implements ClassSelectionListener{
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
        chestMenu = new ChestMenu(font);
        cityMenu = new CityMenu(font, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        classSelectionMenu = new ClassSelectionMenu(
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
        whitePixel = new com.badlogic.gdx.graphics.Texture(pixmap);
        pauseButtonTexture = new Texture("pauseButton.png");
        inventoryButtonTexture = new Texture("inventorybtn.png");
        statsButtonTexture = new Texture("statsbtn.png");
        BGArena = new Texture("bg/forest_light_arena.png");
        statsBackgroundTexture = new Texture("menus/bgs/statsmenubg.png");
        continueButtonTexture = new Texture("menus/buttons/continue.png");
        exitButtonTexture = new Texture("menus/buttons/exit.png");
        settingsButtonTexture = new Texture("menus/buttons/options.png");
        pauseBackgroundTexture = new Texture("menus/bgs/menubg.png");

        // Загружаем текстуру для полосок (должна быть в assets)
        try {
            barTexture = new Texture("playerbarsbg.png");
        } catch (Exception e) {
            Gdx.app.error("RPG", "playerbarsbg.png not found, using whitePixel");
            barTexture = whitePixel;
        }

        SoundManager.playMusic("music/mainMenu.mp3", true);
        pixmap.dispose();

        uiCamera = new OrthographicCamera();
        uiCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        battleScene = new BattleScene(font, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), gameMap, BGArena, whitePixel, barTexture);
        battleScene.loadAssets();

        pauseMenu = new PauseMenu(font,
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight(),
            pauseButtonTexture,
            statsBackgroundTexture,
            continueButtonTexture,
            exitButtonTexture,
            pauseBackgroundTexture,
            settingsButtonTexture);

        inventory = new Inventory(font,
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight(),
            inventoryButtonTexture);

        mainMenu = new MainMenu(
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight(),
            this);

        int margin = 20;
        float btnSize = 120;
        float startY = Gdx.graphics.getHeight() - btnSize;
        statsButtonRect = new Rectangle(2 * btnSize + margin, startY, btnSize, btnSize);

        player = new Player();
        player.loadMapModel();
        shopMenu = new ShopMenu(font, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), inventory, player);
        player.spawnOnShore(gameMap);
        player.setOnEnterForest(new Player.OnEnterForestListener() {
            @Override
            public void onEnterForest(int x, int y) {
                System.out.println("DEBUG JAVA: onEnterForest вызван!");
                battleScene.startBattle(x, y, 1);
            }
        });
        player.syncRenderPos(CELL_SIZE, CELL_GAP); // Синхронизация стартовой позиции
        battleScene.setPlayer(player);
        gameStarted = false;
    }

    @Override
    public void render() {
        if (!gameStarted && !isSelectingClass) {
            mainMenu.handleInput();
            ScreenUtils.clear(0.05f, 0.05f, 0.1f, 1f);
            batch.setProjectionMatrix(uiCamera.combined);
            mainMenu.render(batch, shapeRenderer);
            return;
        }

        if (isSelectingClass) {
            classSelectionMenu.handleInput();
            ScreenUtils.clear(0.05f, 0.05f, 0.1f, 1f);
            batch.setProjectionMatrix(uiCamera.combined);
            classSelectionMenu.render(batch, shapeRenderer);
            return;
        }

        boolean menuClicked = pauseMenu.handleInput(player);
        isPaused = pauseMenu.isVisible();
        boolean chestClicked = chestMenu.handleInput();
        boolean cityMenuClicked = cityMenu.handleInput();
        boolean shopClicked = false;
        if (cityMenu.isShopClicked()) {
            shopMenu.show();
            shopClicked = true;
        }
        boolean shopMenuClicked = shopMenu.handleInput();

        if (battleScene.isActive()) {
            battleScene.update(Gdx.graphics.getDeltaTime());
            battleScene.handleInput(player);
        } else if (!isPaused
            && !menuClicked
            && !chestMenu.isVisible()
            && !cityMenu.isVisible()
            && !shopMenu.isVisible()
        ) {
            handlePlayerInput();
        }
        player.updateMovement(Gdx.graphics.getDeltaTime());

        ScreenUtils.clear(0.1f, 0.1f, 0.2f, 1f);

        if (!isPaused) {
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

        inventory.handleInput(player);

        if (Gdx.input.justTouched()) {
            float touchX = Gdx.input.getX();
            float touchY = Gdx.input.getY();
            float gameY = Gdx.graphics.getHeight() - touchY;
            if (statsButtonRect.contains(touchX, gameY)) {
                pauseMenu.toggleStats();
            }
        }

        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();

        chestMenu.render(batch, whitePixel);
        pauseMenu.render(batch, whitePixel, player);
        inventory.render(batch, whitePixel, player);

        if (!battleScene.isActive()) {
            // Координаты общего фона (рамки)
            float bgX = 20f;
            float bgY = Gdx.graphics.getHeight() - 220f;
            float bgWidth = 500f;      // ширина рамки (подберите под экран)
            float bgHeight = 160f;      // высота рамки

            // 1. Рисуем общую рамку (текстура растянется)
            batch.draw(barTexture, bgX, bgY, bgWidth, bgHeight);

            // 2. Вычисляем внутренние отступы
            float paddingX = bgWidth * 0.165f;
            float paddingY = bgHeight * 0.35f;
            float innerWidth = bgWidth - paddingX * 2;
            float innerHeight = bgHeight - paddingY * 2;
            float innerX = bgX + paddingX;
            float innerY = bgY + paddingY;

            // 3. Параметры двух полосок (здоровье сверху, мана снизу)
            float barHeight = innerHeight * 0.4f;        // высота каждой полоски
            float barSpacing = innerHeight * 0.1f;       // зазор между полосками
            float healthBarY = innerY + innerHeight - barHeight;
            float manaBarY = healthBarY - barHeight - barSpacing;

            // === Полоска здоровья ===
            float healthPercent = (float) player.getCurrentHealth() / player.getMaxHealth();
            batch.setColor(Color.BLACK);
            batch.draw(whitePixel, innerX, healthBarY, innerWidth, barHeight);
            batch.setColor(Color.RED);
            batch.draw(whitePixel, innerX, healthBarY, innerWidth * healthPercent, barHeight);

            // === Полоска маны ===
            float manaPercent = (float) player.getCurrentMana() / player.getMaxMana();
            batch.setColor(Color.BLACK);
            batch.draw(whitePixel, innerX, manaBarY, innerWidth, barHeight);
            batch.setColor(Color.BLUE);
            batch.draw(whitePixel, innerX, manaBarY, innerWidth * manaPercent, barHeight);

            // === Текст поверх полосок ===
            GlyphLayout layout = new GlyphLayout();
            font.getData().setScale(1.2f);

            String healthText = player.getCurrentHealth() + "/" + player.getMaxHealth();
            layout.setText(font, healthText);
            float healthTextX = innerX + (innerWidth - layout.width) / 2;
            float healthTextY = healthBarY + (barHeight + layout.height) / 2;
            font.setColor(Color.BLACK);
            font.draw(batch, healthText, healthTextX + 2f, healthTextY - 2f);
            font.setColor(Color.WHITE);
            font.draw(batch, healthText, healthTextX, healthTextY);

            String manaText = player.getCurrentMana() + "/" + player.getMaxMana();
            layout.setText(font, manaText);
            float manaTextX = innerX + (innerWidth - layout.width) / 2;
            float manaTextY = manaBarY + (barHeight + layout.height) / 2;
            font.setColor(Color.BLACK);
            font.draw(batch, manaText, manaTextX + 2f, manaTextY - 2f);
            font.setColor(Color.WHITE);
            font.draw(batch, manaText, manaTextX, manaTextY);

            batch.setColor(Color.WHITE);
            font.getData().setScale(1f);
        }

        batch.draw(statsButtonTexture, statsButtonRect.x, statsButtonRect.y, statsButtonRect.width, statsButtonRect.height);

        if (battleScene.isActive()) {
            battleScene.render(batch, whitePixel, player);
        }
        cityMenu.render(batch, shapeRenderer);
        shopMenu.render(batch, shapeRenderer, whitePixel);
        batch.end();
    }


    private void drawBarText(SpriteBatch batch, StatBar bar, String text, float scale) {
        GlyphLayout layout = new GlyphLayout(font, text);
        float textX = bar.getX() + (bar.getWidth() - layout.width) / 2;
        float textY = bar.getY() + (bar.getHeight() + layout.height) / 2;
        font.getData().setScale(scale);
        // Тень
        font.setColor(Color.BLACK);
        font.draw(batch, text, textX + 2f, textY - 2f);
        // Основной текст
        font.setColor(Color.WHITE);
        font.draw(batch, text, textX, textY);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
    }

    private Vector3 screenToGrid(float screenX, float screenY) {
        Vector3 world = cameraControl.getCamera().unproject(new Vector3(screenX, screenY, 0));
        int gridX = (int) (world.x / (CELL_SIZE + CELL_GAP));
        int gridY = (int) (world.y / (CELL_SIZE + CELL_GAP));
        return new Vector3(gridX, gridY, 0);
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
            if (player.tryMoveTo(targetX, targetY, gameMap, CELL_SIZE, CELL_GAP))  {
                SoundManager.playSound("sounds/step.mp3");

                if (gameMap.collectChest(targetX, targetY)) {
                    SoundManager.playSound("sounds/openSunduk.mp3");
                    chestMenu.show();

                    if (gameMap.hasEnemies()) {
                        List<Pair<Integer, Integer>> enemyCells = gameMap.getEnemiesNear(targetX, targetY, 2);
                        List<BattleEnemy> enemiesList = new ArrayList<>();
                        for (int i = 0; i < enemyCells.size(); i++) {
                            BattleEnemy enemy = BattleEnemy.Companion.createRandomEnemies(1).get(0);
                            enemiesList.add(enemy);
                        }
                        battleScene.startBattleWithEnemies(enemiesList, enemyCells);
                        chestMenu.hide();
                    }
                }
                if (gameMap.getTerrain(targetX, targetY) == TerrainType.ENEMY) {
                    battleScene.startBattle(targetX, targetY);
                }
            }
        }
    }

    public void onClassSelected(PlayerClasses playerClass) {
        if (playerClass == null) {
            mainMenu.show();
            isSelectingClass = false;
        } else {
            selectedPlayerClass = playerClass;
            applyClassToPlayer();
            startGameAfterClassSelection();
        }
    }

    private void applyClassToPlayer() {
        if (selectedPlayerClass != null) {
            player.changeClass(selectedPlayerClass);
            player.learnSkillsForClass();
            System.out.println("Выбран класс: " + selectedPlayerClass.getDisplayName());
            System.out.println(selectedPlayerClass.getStatsDescription());
        }
    }

    private void startGameAfterClassSelection() {
        isSelectingClass = false;
        gameStarted = true;
        mainMenu.hide();
        SoundManager.stopMusic();
        SoundManager.startPlaylist(false);
    }

    public void showClassSelection() {
        isSelectingClass = true;
        classSelectionMenu.show();
    }

    public void startGame() {
        showClassSelection();
        mainMenu.hide();
        SoundManager.stopMusic();
        SoundManager.startPlaylist(false);
    }

    @Override
    public void dispose() {
        batch.dispose();
        if (image != null) image.dispose();
        if (classSelectionMenu != null) classSelectionMenu.dispose();
        if (pixelTexture != null) pixelTexture.dispose();
        if (whitePixel != null) whitePixel.dispose();
        if (pauseButtonTexture != null) pauseButtonTexture.dispose();
        if (statsButtonTexture != null) statsButtonTexture.dispose();
        if (settingsButtonTexture != null) settingsButtonTexture.dispose();
        if (BGArena != null) BGArena.dispose();
        if (statsBackgroundTexture != null) statsBackgroundTexture.dispose();
        if (continueButtonTexture != null) continueButtonTexture.dispose();
        if (exitButtonTexture != null) exitButtonTexture.dispose();
        if (pauseBackgroundTexture != null) pauseBackgroundTexture.dispose();
        if (mainMenu != null) mainMenu.dispose();
        mapRenderer.dispose();
        font.dispose();
        SoundManager.dispose();
    }
}
