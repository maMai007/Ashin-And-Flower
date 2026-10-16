package ashinandflower;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import javax.sound.sampled.Clip;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * หน้าต่างเกมและตัวควบคุม Game Flow ทั้งหมด
 *
 * ตามเอกสาร ⑧ GUI ไม่มีคลาสแยก คลาสนี้จึงรับหน้าที่ 4 อย่างพร้อมกัน
 * 1) เป็น JPanel ที่วาดทุกหน้าจอ
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
    private static final int FIRST_LEVEL = 1;
    private static final int LAST_LEVEL = 3;
    private static final double BOSS_DRAW_SCALE = 1.5;   // บอสวาดใหญ่กว่ากล่องชน 1.5 เท่า

    // ---------- เลย์เอาต์กลางที่ทุกหน้าจอเมนูใช้ร่วมกัน (สลับหน้าแล้วตำแหน่งจะไม่เด้ง) ----------
    private static final int MENU_TITLE_Y = 200;
    private static final int MENU_SUBTITLE_Y = 248;
    private static final int MENU_BUTTON_Y = 316;
    private static final int BUTTON_W = 200;
    private static final int BUTTON_H = 56;
    private static final int BUTTON_GAP = 32;
    // หน้า Level Clear ไม่มีปุ่ม จึงวางข้อความกลางจอแทนการเผื่อที่ให้ปุ่ม
    private static final int CLEAR_TITLE_Y = 250;
    private static final int CLEAR_SUBTITLE_Y = 298;
    // ปุ่มเดี่ยวจัดกึ่งกลางจอ = 380, ปุ่มคู่ (200 + 32 + 200 = 432) เริ่มที่ 264
    private static final int SINGLE_BUTTON_X = (Level.SCREEN_WIDTH - BUTTON_W) / 2;
    private static final int PAIR_BUTTON_X = (Level.SCREEN_WIDTH - (BUTTON_W * 2 + BUTTON_GAP)) / 2;

    // ---------- ปุ่มบนหน้าจอ (วาดเองด้วย Graphics ไม่ใช้ JButton) ----------
    private static final Rectangle PLAY_BUTTON =
            new Rectangle(SINGLE_BUTTON_X, MENU_BUTTON_Y, BUTTON_W, BUTTON_H);
    private static final Rectangle RETRY_BUTTON =
            new Rectangle(PAIR_BUTTON_X, MENU_BUTTON_Y, BUTTON_W, BUTTON_H);
    private static final Rectangle MENU_BUTTON =
            new Rectangle(PAIR_BUTTON_X + BUTTON_W + BUTTON_GAP, MENU_BUTTON_Y, BUTTON_W, BUTTON_H);
    private static final Rectangle WIN_MENU_BUTTON =
            new Rectangle(SINGLE_BUTTON_X, MENU_BUTTON_Y, BUTTON_W, BUTTON_H);

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

    public Game() {
        assets = new AssetManager(LAST_LEVEL);

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
        if (input.takeClick(PLAY_BUTTON)) {
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
        if (input.takeClick(RETRY_BUTTON)) {
            retry();
        } else if (input.takeClick(MENU_BUTTON)) {
            mainMenu();
        }
        input.clearClick();
        input.clearAttackRequest();
    }

    private void updateWin() {
        if (input.takeClick(WIN_MENU_BUTTON)) {
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

        switch (state) {
            case MAIN_MENU -> drawMainMenu(g2);
            case PLAYING -> drawGameplay(g2);
            case LEVEL_CLEAR -> drawLevelClear(g2);
            case GAME_OVER -> drawGameOver(g2);
            case WIN -> drawWin(g2);
        }
    }

    /** วาดพื้นหลังของหน้าจอเมนูตามรูปต้นฉบับ (ใช้ภาพเดียวกันทุกหน้า) */
    private void drawMenuBackground(Graphics2D g2) {
        g2.drawImage(assets.getMenuBackground(), 0, 0, Level.SCREEN_WIDTH, Level.SCREEN_HEIGHT, null);
    }

    private void drawMainMenu(Graphics2D g2) {
        drawMenuBackground(g2);
        drawCenteredText(g2, "Ashin & Flower", assets.getTitleFont(), new Color(226, 214, 255), MENU_TITLE_Y);
        drawCenteredText(g2, "W A S D / Click", assets.getBodyFont(), new Color(150, 142, 180), MENU_SUBTITLE_Y);
        drawButton(g2, PLAY_BUTTON, assets.getButtonPlay(), "Play");
    }

    private void drawGameplay(Graphics2D g2) {
        // ฉากของ Level
        g2.drawImage(assets.getLevelBackground(currentLevel.getLevelNumber()),
                0, 0, Level.SCREEN_WIDTH, Level.SCREEN_HEIGHT, null);

        // ดอกไม้ (วนด้วย index เพราะ game thread อาจเพิ่ม Spirit Flower ระหว่างที่ EDT กำลังวาด)
        // ดอกไม้ลอยขึ้นลงตามคลื่นไซน์ และมีวงสีรอบดอก
        boolean anyFlowerCollected = false;
        for (int i = 0; i < currentLevel.getFlowers().size(); i++) {
            Flower flower = currentLevel.getFlowers().get(i);
            // บวก getX() เข้าไปให้แต่ละดอกลอยไม่พร้อมกัน
            int bob = (int) (Math.sin((frameCounter + flower.getX()) * 0.08) * 4);
            int cx = flower.getX() + Flower.getSize() / 2;
            int cy = flower.getY() + bob + Flower.getSize() / 2;

            if (flower.isCollected()) {
                anyFlowerCollected = true;
                // เพิ่งถูกเก็บ: ไม่วาดดอก แต่วาดวงแหวนสีเหลืองขยายออกแล้วจางหาย (มีเฉพาะ Spirit Flower)
                if (flower.isPickupFlashing()) {
                    int f = flower.getPickupFlashFrames();   // 24 ลงไป 0
                    int r = 20 + (24 - f) * 2;               // ขยายจาก 20 เป็น 68
                    int alpha = 200 * f / 24;                // จางหายไป
                    Stroke oldStroke = g2.getStroke();
                    g2.setStroke(new BasicStroke(3f));
                    g2.setColor(new Color(150, 255, 160, alpha));
                    g2.drawOval(cx - r, cy - r, r * 2, r * 2);
                    g2.setStroke(oldStroke);
                }
                continue;
            }

            // วงสีโปร่งวงเดียวรอบดอก (ไม่กะพริบ) ลอยตามดอก
            int glowR = 32;
            if (flower.getType() == FlowerType.SPIRIT) {
                g2.setColor(new Color(220, 180, 255, 70));
            } else {
                g2.setColor(new Color(160, 255, 170, 70));
            }
            g2.fillOval(cx - glowR, cy - glowR, glowR * 2, glowR * 2);

            BufferedImage sprite = flower.getType() == FlowerType.SPIRIT
                    ? assets.getFlowerSpiritSprite() : assets.getFlowerNormalSprite();
            g2.drawImage(sprite, flower.getX(), flower.getY() + bob,
                    Flower.getSize(), Flower.getSize(), null);
        }

        // ศัตรู
        for (int i = 0; i < currentLevel.getEnemies().size(); i++) {
            Enemy enemy = currentLevel.getEnemies().get(i);
            if (enemy.isDefeated()) {
                continue;
            }
            // บอสวาดใหญ่กว่าศัตรูทั่วไป แต่กล่องชนยังเท่ากับ Enemy.getSize() เหมือนเดิม
            int drawSize = (enemy instanceof SpiritGuardian)
                    ? (int) Math.round(Enemy.getSize() * BOSS_DRAW_SCALE) : Enemy.getSize();
            int offset = (drawSize - Enemy.getSize()) / 2;
            g2.drawImage(enemySprite(enemy), enemy.getX() - offset, enemy.getY() - offset,
                    drawSize, drawSize, null);
            // กะพริบขาวทับตัวศัตรูที่เพิ่งโดนฟัน
            if (enemy.isHitFlashing()) {
                g2.setColor(new Color(255, 255, 255, 130));
                g2.fillRect(enemy.getX(), enemy.getY(), Enemy.getSize(), Enemy.getSize());
            }
            drawHpBar(g2, enemy.getX(), enemy.getY() - 10, Enemy.getSize(),
                    enemy.getHp(), enemy.getMaxHp(), new Color(226, 90, 90));
        }

        // รอยฟัน: เส้นโค้งหนาที่ขอบนอกของระยะดาบ วาดก่อนตัว Player เพื่อไม่ให้บังตัวละคร
        if (attackFlashFrames > 0) {
            int cx = player.getX() + Player.getSize() / 2;
            int cy = player.getY() + Player.getSize() / 2;
            int r = player.getSword().getRange() - 8;      // ร่นเข้ามานิดให้ดูเป็นรอยดาบ
            int alpha = 220 * attackFlashFrames / 8;       // จางลงตามเวลา
            // drawArc วัดมุมทวนเข็มนาฬิกาจากทิศขวา จึงเริ่มที่ 45 องศาก่อนถึงทิศที่หัน
            int startAngle;
            switch (attackFlashFacing) {
                case UP -> startAngle = 45;
                case LEFT -> startAngle = 135;
                case DOWN -> startAngle = 225;
                default -> startAngle = 315;   // RIGHT
            }
            Stroke oldStroke = g2.getStroke();
            g2.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(255, 255, 255, alpha));
            g2.drawArc(cx - r, cy - r, r * 2, r * 2, startAngle, 90);
            g2.setStroke(oldStroke);
        }

        // Player และทิศที่หันอยู่ (ช่วงอมตะจะกะพริบ: ข้ามการวาดทุก ๆ 4 เฟรมสลับกัน)
        boolean blinkHidden = player.isInvulnerable() && (frameCounter / 4) % 2 == 0;
        if (!blinkHidden) {
            g2.drawImage(assets.getPlayerSprite(player.getFacing()), player.getX(), player.getY(),
                    Player.getSize(), Player.getSize(), null);
        }
        drawHpBar(g2, player.getX(), player.getY() - 10, Player.getSize(),
                player.getHp(), player.getMaxHp(), new Color(110, 220, 130));

        // HUD: ชื่อ Level มุมซ้ายบน
        g2.setFont(assets.getBodyFont());
        // เงาใต้ตัวอักษรให้ HUD อ่านออกบนพื้นหลังที่มีลวดลาย
        g2.setColor(new Color(0, 0, 0, 170));
        g2.drawString(currentLevel.getDisplayName(), 21, 37);
        g2.setColor(Color.WHITE);
        g2.drawString(currentLevel.getDisplayName(), 20, 36);

        // ข้อความสอนเฉพาะ Level 1 หายไปถาวรเมื่อเก็บดอกแรกได้
        if (currentLevel.getLevelNumber() == FIRST_LEVEL && !anyFlowerCollected) {
            drawCenteredText(g2, "เดินทับดอกไม้เพื่อฟื้น HP", new Font("Tahoma", Font.PLAIN, 15),
                    new Color(255, 255, 255, 200), 64);
        }
    }

    /** หน้า Level Clear: พื้นหลังเมนู กับข้อความ Level Clear และชื่อ Level ถัดไปกลางจอ */
    private void drawLevelClear(Graphics2D g2) {
        drawMenuBackground(g2);
        drawCenteredText(g2, "Level Clear", assets.getTitleFont(), new Color(226, 214, 255), CLEAR_TITLE_Y);
        drawCenteredText(g2, "Level " + (currentLevelNumber + 1), assets.getBodyFont(),
                new Color(170, 162, 200), CLEAR_SUBTITLE_Y);
    }

    private void drawGameOver(Graphics2D g2) {
        drawMenuBackground(g2);
        drawCenteredText(g2, "GAME OVER", assets.getTitleFont(), new Color(240, 120, 120), MENU_TITLE_Y);
        drawCenteredText(g2, gameOverReason, assets.getBodyFont(), new Color(190, 160, 160), MENU_SUBTITLE_Y);
        drawButton(g2, RETRY_BUTTON, assets.getButtonRetry(), "Retry");
        drawButton(g2, MENU_BUTTON, assets.getButtonMenu(), "Main Menu");
    }

    private void drawWin(Graphics2D g2) {
        drawMenuBackground(g2);
        drawCenteredText(g2, "YOU WIN!", assets.getTitleFont(), new Color(255, 236, 150), MENU_TITLE_Y);
        drawCenteredText(g2, "You collected the Spirit Flower!", assets.getBodyFont(),
                new Color(180, 200, 180), MENU_SUBTITLE_Y);
        drawButton(g2, WIN_MENU_BUTTON, assets.getButtonMenu(), "Main Menu");
    }

    // ---------- ตัวช่วยวาด ----------

    /** วาดปุ่มจากรูป ข้อความอยู่ในรูปแล้วจึงไม่ต้องวาดตัวอักษรทับ */
    private void drawButton(Graphics2D g2, Rectangle button, BufferedImage image, String label) {
        g2.drawImage(image, button.x, button.y, button.width, button.height, null);
    }

    private void drawCenteredText(Graphics2D g2, String text, Font font, Color color, int y) {
        g2.setFont(font);
        g2.setColor(color);
        int width = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, (Level.SCREEN_WIDTH - width) / 2, y);
    }

    private void drawHpBar(Graphics2D g2, int x, int y, int width, int hp, int maxHp, Color fill) {
        if (maxHp <= 0) {
            return;
        }
        int height = 5;
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(x, y, width, height);
        int filled = (int) Math.round((double) width * hp / maxHp);
        g2.setColor(fill);
        g2.fillRect(x, y, filled, height);
    }

    /** เลือก sprite ตามชนิดของศัตรู (ใช้ Polymorphism ผ่าน instanceof) */
    private BufferedImage enemySprite(Enemy enemy) {
        if (enemy instanceof SpiritGuardian) {
            return assets.getSpiritGuardianSprite();
        }
        if (enemy instanceof ShadowGhost) {
            return assets.getShadowGhostSprite();
        }
        return assets.getWanderingGhostSprite();
    }

    // ============================================================
    //  Entry point
    // ============================================================

    public static void main(String[] args) {
        // สร้าง GUI บน EDT ตามกติกาของ Swing แล้วค่อยสตาร์ท game thread
        SwingUtilities.invokeLater(() -> new Game().startGame());
    }
}
