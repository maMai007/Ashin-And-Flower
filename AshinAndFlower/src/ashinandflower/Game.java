package ashinandflower;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.sound.sampled.Clip;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** หน้าต่างเกม game loop และ state machine ของเกม */
public class Game extends JPanel implements Runnable {

    // ค่าคงที่ของลูปและหน้าจอ
    private static final int FPS = 60;
    private static final long FRAME_TIME_MS = 1000 / FPS;   // 16 ms
    private static final int LEVEL_CLEAR_FRAMES = 2 * FPS;  // 2 วินาที
    static final int FIRST_LEVEL = 1;   // ใช้ตัดสินการแสดงข้อความสอน
    private static final int LAST_LEVEL = 3;

    // สถานะเกม
    private GameState state;
    private Player player;
    private Level currentLevel;
    private int currentLevelNumber;
    private int levelClearFrames;
    private String gameOverReason;

    // เอฟเฟกต์ภาพ (หน่วยเป็นเฟรม)
    private int frameCounter;              // นับเฟรมสะสม ใช้ทำจังหวะกะพริบและลอย
    private int attackFlashFrames;         // เวลาที่เหลือของรอยฟัน
    private Direction attackFlashFacing;   // ทิศที่ฟัน ใช้วาดรอยฟัน

    // thread (running เป็น volatile เพราะ EDT เขียน)
    private volatile boolean running;
    private Thread gameThread;

    private final InputHandler input;

    private final AssetLoader assets;

    private final GameRenderer renderer;

    public Game() {
        assets = new AssetLoader(LAST_LEVEL);
        renderer = new GameRenderer(assets);

        // เล่นเพลงพื้นหลังวนตลอด (null = ไม่มีเสียง)
        Clip bgmClip = assets.getBgmClip();
        if (bgmClip != null) {
            bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
        }

        setPreferredSize(new Dimension(Level.SCREEN_WIDTH, Level.SCREEN_HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        input = new InputHandler(this);
        addKeyListener(input);
        addMouseListener(input);

        state = GameState.MAIN_MENU;
        currentLevelNumber = FIRST_LEVEL;
        gameOverReason = "";
    }

    // Thread

    /** สร้างหน้าต่างและสตาร์ท game thread */
    public void startGame() {
        JFrame frame = new JFrame("Ashin & Flower");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        // ปิดหน้าต่างแล้วหยุดลูปและปิดเพลง
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopGame();
            }
        });
        frame.add(this);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        requestFocusInWindow();

        running = true;
        gameThread = new Thread(this, "GameThread");
        // daemon เพื่อไม่ให้ thread ค้างหลังปิดหน้าต่าง
        gameThread.setDaemon(true);
        gameThread.start();
    }

    /** ลูปหลักบน game thread: update แล้ว repaint ทุก 16 ms */
    @Override
    public void run() {
        while (running) {
            long frameStart = System.currentTimeMillis();

            update();
            repaint();

            long elapsed = System.currentTimeMillis() - frameStart;
            long sleepTime = FRAME_TIME_MS - elapsed;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    // ถูก interrupt: ออกจากลูป
                    Thread.currentThread().interrupt();
                    running = false;
                }
            }
        }
    }

    /** หยุด game thread และปิดเพลง */
    public void stopGame() {
        running = false;
        Clip bgmClip = assets.getBgmClip();
        if (bgmClip != null) {
            bgmClip.stop();
            bgmClip.close();
        }
    }

    // การสลับหน้าจอ

    /** อัปเดตหนึ่งเฟรมตามหน้าจอปัจจุบัน */
    public void update() {
        switch (state) {
            case MAIN_MENU -> updateMainMenu();
            case PLAYING -> updatePlaying();
            case LEVEL_CLEAR -> updateLevelClear();
            case GAME_OVER -> updateGameOver();
            case WIN -> updateWin();
        }
    }

    /** หน้าจอปัจจุบัน */
    public GameState getState() {
        return state;
    }

    private void updateMainMenu() {
        if (input.takeClick(renderer.getPlayButton())) {
            newGame();
        }
        input.clearClick();
        input.clearAttackRequest();
    }

    private void updatePlaying() {
        // 0) นับถอยหลังเอฟเฟกต์และ cooldown
        player.tick();
        player.getSword().tick();

        frameCounter++;
        if (attackFlashFrames > 0) {
            attackFlashFrames--;
        }

        // 1) เดินตามปุ่มที่กดและกันออกนอกกรอบ
        int dx = 0;
        int dy = 0;
        if (input.isLeftPressed()) {
            dx--;
        }
        if (input.isRightPressed()) {
            dx++;
        }
        if (input.isUpPressed()) {
            dy--;
        }
        if (input.isDownPressed()) {
            dy++;
        }
        if (dx != 0 || dy != 0) {
            player.move(dx, dy);
            player.keepInside(Level.PLAY_LEFT, Level.PLAY_TOP,
                    Level.PLAY_RIGHT - Player.getSize(), Level.PLAY_BOTTOM - Player.getSize());
        }

        // 2) ฟันหนึ่งครั้งต่อคลิก เมื่อดาบพร้อม
        if (input.isAttackRequested()) {
            input.clearAttackRequest();
            if (player.getSword().isReady()) {
                player.attack(currentLevel.getEnemies());
                attackFlashFrames = 8;
                attackFlashFacing = player.getFacing();
            }
        }
        input.clearClick();

        // 3) ให้ Level เดินศัตรูและเก็บดอกไม้
        currentLevel.update(player);

        // 4) ตรวจผ่านด่านก่อน แล้วค่อยตรวจแพ้
        if (currentLevel.checkLevelClear()) {
            if (currentLevelNumber == LAST_LEVEL) {
                win();
            } else {
                state = GameState.LEVEL_CLEAR;
                levelClearFrames = LEVEL_CLEAR_FRAMES;
            }
        } else if (player.isDead()) {
            gameOver("Player Defeated");
        }
    }

    /** รอหน้า Level Clear แล้วไปด่านถัดไป */
    private void updateLevelClear() {
        // ให้ cooldown และเอฟเฟกต์เดินต่อ
        player.tick();
        player.getSword().tick();
        input.clearClick();
        input.clearAttackRequest();
        levelClearFrames--;
        if (levelClearFrames <= 0) {
            changeLevel(currentLevelNumber + 1);
        }
    }

    private void updateGameOver() {
        if (input.takeClick(renderer.getRetryButton())) {
            retry();
        } else if (input.takeClick(renderer.getMenuButton())) {
            mainMenu();
        }
        input.clearClick();
        input.clearAttackRequest();
    }

    private void updateWin() {
        if (input.takeClick(renderer.getWinMenuButton())) {
            mainMenu();
        }
        input.clearClick();
        input.clearAttackRequest();
    }

    /** เริ่มเกมใหม่จาก Level 1 */
    public void newGame() {
        player = new Player();
        gameOverReason = "";
        changeLevel(FIRST_LEVEL);
    }

    /** เล่น Level ปัจจุบันใหม่ด้วย HP เต็ม */
    public void retry() {
        player = new Player();
        gameOverReason = "";
        changeLevel(currentLevelNumber);
    }

    /** กลับหน้า Main Menu โดยทิ้งความคืบหน้าทั้งหมด */
    public void mainMenu() {
        player = null;
        currentLevel = null;
        currentLevelNumber = FIRST_LEVEL;
        gameOverReason = "";
        state = GameState.MAIN_MENU;
    }

    /** สร้าง Level ใหม่และวาง Player ที่จุดเริ่ม */
    private void changeLevel(int levelNumber) {
        currentLevelNumber = levelNumber;
        currentLevel = new Level(levelNumber);
        player.setPosition(currentLevel.getPlayerStartX(), currentLevel.getPlayerStartY());
        // ล้างรอยฟันค้างจากด่านก่อน
        attackFlashFrames = 0;
        state = GameState.PLAYING;
    }

    private void gameOver(String reason) {
        gameOverReason = reason;
        state = GameState.GAME_OVER;
    }

    private void win() {
        state = GameState.WIN;
    }

    // การวาดหน้าจอ

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        // ส่งข้อมูลให้ GameRenderer วาด
        switch (state) {
            case MAIN_MENU -> renderer.drawMainMenu(g2);
            case PLAYING -> renderer.drawGameplay(g2, player, currentLevel,
                    frameCounter, attackFlashFrames, attackFlashFacing);
            case LEVEL_CLEAR -> renderer.drawLevelClear(g2, currentLevelNumber);
            case GAME_OVER -> renderer.drawGameOver(g2, gameOverReason);
            case WIN -> renderer.drawWin(g2);
        }
    }

    // จุดเริ่มต้นโปรแกรม

    public static void main(String[] args) {
        // สร้าง GUI บน EDT
        SwingUtilities.invokeLater(() -> new Game().startGame());
    }
}
