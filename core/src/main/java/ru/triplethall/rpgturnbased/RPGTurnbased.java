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
    private Texture statsButtonTexture;
    private Texture pauseBackgroundTexture;
    private Texture statsBackgroundTexture;
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
        pauseBackgroundTexture = new Texture("menus/bgs/menubg.png");

        // Включаем музыку главного меню (она будет играть до старта игры)
        SoundManager.playMusic("music/mainMenu.mp3", true);
        pixmap.dispose();

        uiCamera = new OrthographicCamera();
        uiCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        battleScene = new BattleScene(font, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), gameMap, BGArena, whitePixel);
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

        int margin = 20;
        float btnSize = 120;
        float startY = Gdx.graphics.getHeight() - btnSize;
        statsButtonRect = new Rectangle(2 * btnSize + margin, startY, btnSize, btnSize);

        player = new Player();
        player.spawnOnShore(gameMap);
        player.setOnEnterForest(new Player.OnEnterForestListener() {
            @Override
            public void onEnterForest(int x, int y) {
                System.out.println("DEBUG JAVA: onEnterForest вызван!"); // Добавьте эту строку
                battleScene.startBattle(x, y, 1);
            }
        });
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

        // Обработка выбора класса
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

        if (battleScene.isActive()) {
            battleScene.update(Gdx.graphics.getDeltaTime());
            battleScene.handleInput(player);
        } else if (!isPaused && !menuClicked && !chestMenu.isVisible() && !cityMenu.isVisible()) {
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
        batch.draw(statsButtonTexture, statsButtonRect.x, statsButtonRect.y, statsButtonRect.width, statsButtonRect.height);

        if (battleScene.isActive()) {
            battleScene.render(batch, whitePixel, player);
        }
        cityMenu.render(batch, shapeRenderer);
        batch.end();
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

            if (gameMap.getTerrain(targetX, targetY) == TerrainType.CITY) {
                int dx = Math.abs(player.getX() - targetX);
                int dy = Math.abs(player.getY() - targetY);
                boolean isNear = (dx <= 1 && dy <= 1) && !(dx == 0 && dy == 0);
                if (isNear) {
                    cityMenu.show();
                }
                return;
            }
            if (player.tryMoveTo(targetX, targetY, gameMap)) {
                SoundManager.playSound("sounds/step.mp3");

                if (gameMap.collectChest(targetX, targetY)) {
                    SoundManager.playSound("sounds/openSunduk.mp3");
                    chestMenu.show();
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
            selectedPlayerClass.applyToPlayer(player);
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
        // Останавливаем музыку главного меню и запускаем плейлист для карты
        SoundManager.stopMusic();
        SoundManager.startPlaylist(false); // false = без перемешивания, можно true для случайного порядка
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
        if (BGArena != null) BGArena.dispose();
        if (statsBackgroundTexture != null) statsBackgroundTexture.dispose();
        if (continueButtonTexture != null) continueButtonTexture.dispose();
        if (exitButtonTexture != null) exitButtonTexture.dispose();
        if (pauseBackgroundTexture != null) pauseBackgroundTexture.dispose();
        if (mainMenu != null) mainMenu.dispose();
        mapRenderer.dispose();
        font.dispose();
        SoundManager.dispose(); // обязательно освободить ресурсы звуков
    }
}
