package ru.triplethall.rpgturnbased;

import static java.lang.Math.random;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
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
import java.util.ArrayList;
import java.util.List;
import kotlin.Pair;

import ru.triplethall.rpgturnbased.GameMap;
import ru.triplethall.rpgturnbased.Player;
import ru.triplethall.rpgturnbased.PauseMenu;
import ru.triplethall.rpgturnbased.SoundManager;
import ru.triplethall.rpgturnbased.ClassSelectionListener;


public class RPGTurnbased extends ApplicationAdapter implements ClassSelectionListener {
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
    private Texture settingsButtonTexture;   // <-- ДОБАВЛЕНО
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


    // Бары для главного экрана
    private StatBar mainHealthBar;
    private StatBar mainManaBar;


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
        mainMenu = new MainMenu(
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight(),
            this
        );
        mainMenu.show();
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

        int margin = 20;
        float btnSize = 120;
        // Опускаем кнопку статистики ниже (300 пикселей от верхнего края)
        float startY = Gdx.graphics.getHeight() - btnSize - 150f;
        statsButtonRect = new Rectangle(2 * btnSize + margin, startY, btnSize, btnSize);

        player = new Player();
        shopMenu = new ShopMenu(font, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), inventory, player);
        player.spawnOnShore(gameMap);
        player.setOnEnterForest(new Player.OnEnterForestListener() {
            @Override
            public void onEnterForest(int x, int y) {
                System.out.println("DEBUG JAVA: onEnterForest вызван!");
                battleScene.startBattle(x, y, 1);
            }
        });
        battleScene.setPlayer(player);

        // Создаём бары для главного экрана (такие же, как в бою, с отступом 160 пикселей сверху)
        float barWidth = 400f;
        float barHeight = 100f;
        float barX = 20f;
        float healthBarY = Gdx.graphics.getHeight() - 100f;
        float manaBarY = healthBarY - barHeight + 25f;
        mainHealthBar = new StatBar(barX, healthBarY, barWidth, barHeight, new Color(0.478f, 0.220f, 0.008f, 1f));
        mainManaBar = new StatBar(barX, manaBarY, barWidth, barHeight, new Color(0.129f, 0.216f, 0.471f, 1f));

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
        boolean caveMenuClicked = caveMenu.handleInput();
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
            && !caveMenu.isVisible()
        ) {
            handlePlayerInput();
        }

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

        // Рисуем бары только если не в бою (используем тот же метод, что и в сцене боя)
        if (!battleScene.isActive()) {
            battleScene.drawPlayerBars(batch, mainHealthBar, mainManaBar, barTexture, 10f);
        }

        batch.draw(statsButtonTexture, statsButtonRect.x, statsButtonRect.y, statsButtonRect.width, statsButtonRect.height);

        if (battleScene.isActive()) {
            battleScene.render(batch, whitePixel, player);
        }
        cityMenu.render(batch, shapeRenderer);
        caveMenu.render(batch, shapeRenderer);
        shopMenu.render(batch, shapeRenderer, whitePixel);
        batch.end();
    }

    private void drawBarText(SpriteBatch batch, StatBar bar, String text, float scale) {
        GlyphLayout layout = new GlyphLayout(font, text);
        float textX = bar.getX() + (bar.getWidth() - layout.width) / 2;
        float textY = bar.getY() + (bar.getHeight() + layout.height) / 2;
        font.getData().setScale(scale);
        font.setColor(Color.BLACK);
        font.draw(batch, text, textX + 2f, textY - 2f);
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
        if (Gdx.input.justTouched() && !cameraControl.isDragging()) {
            Vector3 grid = screenToGrid(Gdx.input.getX(), Gdx.input.getY());
            int targetX = (int) grid.x;
            int targetY = (int) grid.y;
            Gdx.app.log("MOVE_DEBUG", "Clicked on {" + targetX + "," + targetY + "} | Terrain = " + gameMap.getTerrain(targetX, targetY));
            if (gameMap.getTerrain(targetX, targetY) == TerrainType.CAVEENTRANCE) {
                int dx = Math.abs(player.getX() - targetX);
                int dy = Math.abs(player.getY() - targetY);
                boolean isNear = (dx <= 1 && dy <= 1) && !(dx == 0 && dy == 0);
                if (isNear) {
                    caveMenu.show();
                    return;
                }
            }
            if (gameMap.getTerrain(targetX, targetY) == TerrainType.CITY || gameMap.getTerrain(targetX, targetY) == TerrainType.CITYANCHOR) {
                int dx = Math.abs(player.getX() - targetX);
                int dy = Math.abs(player.getY() - targetY);
                boolean isNear = (dx <= 1 && dy <= 1) && !(dx == 0 && dy == 0);
                if (isNear) {
                    cityMenu.show();
                    return;
                }
            }

            Gdx.app.log("MOVE_DEBUG", "tryMoveTo " + targetX + "," + targetY + " from " + player.getX() + "," + player.getY());
            if (player.tryMoveTo(targetX, targetY, gameMap)) {
                Gdx.app.log("MOVE_DEBUG", "Move successful");
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
                            mainMenu = new MainMenu(
                                Gdx.graphics.getWidth(),
                                Gdx.graphics.getHeight(),
                                this);

                            int margin = 20;
                            float btnSize = 120;
                            float startY = Gdx.graphics.getHeight() - btnSize - 300f;  // сдвиг кнопки статистики вниз
                            statsButtonRect = new Rectangle(2 * btnSize + margin, startY, btnSize, btnSize);

                            player = new Player();
                            shopMenu = new ShopMenu(font, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), inventory, player);
                            player.spawnOnShore(gameMap);
                            player.setOnEnterForest(new Player.OnEnterForestListener() {
                                @Override
                                public void onEnterForest(int x, int y) {
                                    System.out.println("DEBUG JAVA: onEnterForest вызван!");
                                    battleScene.startBattle(x, y, 1);
                                }
                            });
                            battleScene.setPlayer(player);

                            // Создаём бары для главного экрана
                            float barWidth = 400f;
                            float barHeight = 100f;
                            float barX = 20f;
                            float healthBarY = Gdx.graphics.getHeight() - 160f;   // подняты выше
                            float manaBarY = healthBarY - barHeight - 5f;
                            mainHealthBar = new StatBar(barX, healthBarY, barWidth, barHeight, Color.RED);
                            mainManaBar = new StatBar(barX, manaBarY, barWidth, barHeight, Color.BLUE);

                            gameStarted = false;
                        }
                    }
                }
            }
        }
    }
    @Override
    public void onClassSelected(PlayerClasses playerClass) {
        if (playerClass == null) {
            // Отмена выбора — показываем главное меню
            if (mainMenu != null) mainMenu.show();
            isSelectingClass = false;
        } else {
            // Применение класса к игроку
            selectedPlayerClass = playerClass;
            if (player != null && selectedPlayerClass != null) {
                selectedPlayerClass.applyToPlayer(player);
                // Если у игрока есть метод learnSkillsForClass — раскомментируй:
                // player.learnSkillsForClass();
            }
            // Старт игры
            isSelectingClass = false;
            gameStarted = true;
            if (mainMenu != null) mainMenu.hide();
            SoundManager.stopMusic();
            SoundManager.startPlaylist(false);
        }
    }
}


