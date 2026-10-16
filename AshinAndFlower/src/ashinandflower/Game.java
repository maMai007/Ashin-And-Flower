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

/**
 * หน้าต่างเกมและตัวควบคุม Game Flow ทั้งหมด
 *
 * ตามเอกสาร ⑧ GUI ไม่มีคลาสแยก คลาสนี้จึงรับหน้าที่ 4 อย่างพร้อมกัน
 * 1) เป็น JPanel ที่ส่งข้อมูลเกมให้ GameRenderer วาดทุกหน้าจอ
 * 2) เป็น game thread ที่เดินลูป 60 FPS
 * 3) เป็น state machine ของ GameState
 * 4) อ่าน Input จาก InputHandler แล้วนำไปใช้ในแต่ละเฟรม
 *
 * เรื่อง Thread: Swing ประมวลผล Event ทั้งหมดบน Event Dispatch Thread (EDT)
 * ถ้าเอา game loop ไปวนบน EDT หน้าจอจะค้างเพราะ EDT ไม่ว่างไปวาดภาพ
 * เกมนี้จึงสร้าง thread ของตัวเองหนึ่งเส้นสำหรับ update + repaint
 * ส่วน field ที่ถูกเขียนจาก EDT (ปุ่มที่กด, การคลิก) อยู่ใน InputHandler และเป็น volatile
 * เพื่อให้ game thread มองเห็นค่าล่าสุดเสมอ
 */
public class Game extends JPanel implements Runnable {

    // ---------- ค่าคงที่ของลูปและหน้าจอ ----------
    private static final int FPS = 60;
    private static final long FRAME_TIME_MS = 1000 / FPS;   // 16 ms
    private static final int LEVEL_CLEAR_FRAMES = 2 * FPS;  // แสดงหน้า Level Clear 2 วินาที
    static final int FIRST_LEVEL = 1;   // GameRenderer ใช้ตัดสินว่าจะแสดงข้อความสอนหรือไม่
    private static final int LAST_LEVEL = 3;

    // ---------- สถานะเกม ----------
    private GameState state;
    private Player player;
    private Level currentLevel;
    private int currentLevelNumber;
    private int levelClearFrames;
    private String gameOverReason;

    // ---------- เอฟเฟกต์ภาพตอนต่อสู้ (หน่วยเป็นเฟรม นับถอยหลังใน updatePlaying) ----------
    private int frameCounter;              // นับเฟรมไปเรื่อย ๆ ใช้ทำจังหวะกะพริบของ Player
    private int attackFlashFrames;         // เวลาที่เหลือของรอยฟัน
    private Direction attackFlashFacing;   // ทิศที่ฟัน ณ ตอนคลิก ใช้วาดรอยฟันให้ตรงทิศ

    // ---------- ตัวแปรของ thread ----------
    // running ถูกอ่านโดย game thread และเขียนโดย EDT จึงต้องเป็น volatile
    private volatile boolean running;
    private Thread gameThread;

    // ---------- Input จากคีย์บอร์ดและเมาส์ ----------
    private final InputHandler input;

    // ---------- ไฟล์รูป ฟอนต์ และเสียง ----------
    private final AssetManager assets;

    // ---------- ตัววาดหน้าจอ ----------
    private final GameRenderer renderer;

    public Game() {
        assets = new AssetManager(LAST_LEVEL);
        renderer = new GameRenderer(assets);

        // เพลงพื้นหลังเล่นวนตลอดตั้งแต่หน้าเมนูจนปิดเกม (โหลดไม่ได้ = null เกมจะเงียบแต่เล่นต่อได้)
        // Clip เล่นเสียงบน thread ของตัวเอง จึงไม่ต้องสร้าง thread เพิ่ม
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

    // ============================================================
    //  Thread: เริ่ม หยุด และตัวลูปหลัก
    // ============================================================

    /** สร้างหน้าต่างเกมและสตาร์ท game thread (เรียกจาก main บน EDT) */
    public void startGame() {
        JFrame frame = new JFrame("Ashin & Flower");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        // ตอนกดปิดหน้าต่าง ให้หยุดลูปและปิดเพลงก่อนโปรแกรมจบ
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
        // เป็น daemon thread เพื่อให้ปิดหน้าต่างแล้ว thread ไม่ค้างอยู่เบื้องหลัง
        gameThread.setDaemon(true);
        gameThread.start();
    }

    /**
     * ลูปหลักของเกม ทำงานบน game thread ไม่ใช่ EDT
     * หนึ่งรอบ = update ตรรกะหนึ่งเฟรม แล้วสั่งวาด จากนั้นนอนรอให้ครบ 16 ms
     * repaint() เรียกจาก thread ไหนก็ได้ เพราะ Swing จะไปเข้าคิววาดบน EDT ให้เอง
     */
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
                    // ถูกสั่งหยุดระหว่างนอน: คืนสถานะ interrupt แล้วออกจากลูป
                    Thread.currentThread().interrupt();
                    running = false;
                }
            }
        }
    }

    /** สั่งให้ game thread หยุดอย่างปลอดภัย (ไม่ใช้ Thread.stop ซึ่ง deprecated) */
    public void stopGame() {
        running = false;
        Clip bgmClip = assets.getBgmClip();
        if (bgmClip != null) {
            bgmClip.stop();
            bgmClip.close();
        }
    }

    // ============================================================
    //  State machine
    // ============================================================

    /** อัปเดตหนึ่งเฟรม แยกตามหน้าจอปัจจุบัน (⑧ §8 รับ Input ตามหน้าจอเท่านั้น) */
    public void update() {
        switch (state) {
            case MAIN_MENU -> updateMainMenu();
            case PLAYING -> updatePlaying();
            case LEVEL_CLEAR -> updateLevelClear();
            case GAME_OVER -> updateGameOver();
            case WIN -> updateWin();
        }
    }

    /** หน้าจอปัจจุบัน (InputHandler ใช้ตัดสินว่าคลิกนับเป็นการโจมตีหรือไม่) */
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
        // 0) นับถอยหลังเวลาอมตะของ Player และ cooldown ของดาบ
        player.tick();
        player.getSword().tick();

        // นับเวลาของเอฟเฟกต์ภาพ
        frameCounter++;
        if (attackFlashFrames > 0) {
            attackFlashFrames--;
        }

        // 1) เดินตามปุ่มที่กดค้างอยู่ แล้วกันไม่ให้ออกนอกกรอบพื้นที่เล่น
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

        // 2) โจมตีหนึ่งครั้งต่อหนึ่งคลิก จึงเคลียร์ flag ทันทีหลังใช้
        //    ฟันจริงเฉพาะตอนดาบพร้อม และแสดงรอยฟันเฉพาะตอนที่ฟันจริงเท่านั้น
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

        // 4) ตรวจเงื่อนไขจบเฟรม: ผ่านด่านก่อน แล้วค่อยแพ้
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

    /** หน้า Level Clear ไม่รับ Input และเปลี่ยน Level เองเมื่อครบเวลา (⑧ §2) */
    private void updateLevelClear() {
        // ให้ cooldown ดาบและเวลาอมตะเดินต่อ ไม่งั้นจะค้างข้ามไปด่านถัดไป
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

    /** เริ่มเกมใหม่ทั้งหมดจาก Level 1 และรีเซ็ตความคืบหน้า (⑧ ปุ่ม Play) */
    public void newGame() {
        player = new Player();
        gameOverReason = "";
        changeLevel(FIRST_LEVEL);
    }

    /** เล่น Level ที่แพ้ใหม่ตั้งแต่ต้น HP กลับเป็นเต็ม ศัตรูและดอกไม้กลับสู่สถานะเริ่มต้น */
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

    /** สร้าง Level ใหม่ ตั้งตำแหน่งเริ่มต้นของ Player แล้วเข้าสู่ Gameplay */
    private void changeLevel(int levelNumber) {
        currentLevelNumber = levelNumber;
        currentLevel = new Level(levelNumber);
        player.setPosition(currentLevel.getPlayerStartX(), currentLevel.getPlayerStartY());
        // ล้างรอยฟันค้างจากด่านก่อน (ใช้ร่วมกันทั้ง New Game, เปลี่ยนด่าน และ Retry)
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

    // ============================================================
    //  การวาดหน้าจอ (ทำงานบน EDT)
    // ============================================================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        // ส่งเฉพาะข้อมูลที่หน้าจอนั้นใช้วาด ให้ GameRenderer
        switch (state) {
            case MAIN_MENU -> renderer.drawMainMenu(g2);
            case PLAYING -> renderer.drawGameplay(g2, player, currentLevel,
                    frameCounter, attackFlashFrames, attackFlashFacing);
            case LEVEL_CLEAR -> renderer.drawLevelClear(g2, currentLevelNumber);
            case GAME_OVER -> renderer.drawGameOver(g2, gameOverReason);
            case WIN -> renderer.drawWin(g2);
        }
    }

    // ============================================================
    //  Entry point
    // ============================================================

    public static void main(String[] args) {
        // สร้าง GUI บน EDT ตามกติกาของ Swing แล้วค่อยสตาร์ท game thread
        SwingUtilities.invokeLater(() -> new Game().startGame());
    }
}
