package ashinandflower;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.image.BufferedImage;

/**
 * วาดทุกหน้าจอของเกมลงบน Graphics2D ที่ Game ส่งมาจาก paintComponent
 *
 * คลาสนี้ไม่เก็บสถานะเกมไว้เอง ข้อมูลที่ต้องใช้วาดจะถูกส่งมาเป็นพารามิเตอร์ทุกครั้ง
 * เก็บไว้แค่ AssetLoader (รูปและฟอนต์) กับตำแหน่งปุ่ม
 */
public class GameRenderer {

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

    private final AssetLoader assets;

    public GameRenderer(AssetLoader assets) {
        this.assets = assets;
    }

    // ============================================================
    //  ตำแหน่งปุ่ม (Game ใช้ตรวจว่าคลิกโดนปุ่มไหน)
    // ============================================================

    public Rectangle getPlayButton() {
        return PLAY_BUTTON;
    }

    public Rectangle getRetryButton() {
        return RETRY_BUTTON;
    }

    public Rectangle getMenuButton() {
        return MENU_BUTTON;
    }

    public Rectangle getWinMenuButton() {
        return WIN_MENU_BUTTON;
    }

    // ============================================================
    //  การวาดแต่ละหน้าจอ (ทำงานบน EDT)
    // ============================================================

    /** วาดพื้นหลังของหน้าจอเมนูตามรูปต้นฉบับ (ใช้ภาพเดียวกันทุกหน้า) */
    private void drawMenuBackground(Graphics2D g2) {
        g2.drawImage(assets.getMenuBackground(), 0, 0, Level.SCREEN_WIDTH, Level.SCREEN_HEIGHT, null);
    }

    public void drawMainMenu(Graphics2D g2) {
        drawMenuBackground(g2);
        drawCenteredText(g2, "Ashin & Flower", assets.getTitleFont(), new Color(226, 214, 255), MENU_TITLE_Y);
        drawCenteredText(g2, "W A S D / Click", assets.getBodyFont(), new Color(150, 142, 180), MENU_SUBTITLE_Y);
        drawButton(g2, PLAY_BUTTON, assets.getButtonPlay(), "Play");
    }

    public void drawGameplay(Graphics2D g2, Player player, Level currentLevel,
            int frameCounter, int attackFlashFrames, Direction attackFlashFacing) {
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

        // Player และทิศที่หันอยู่ (หลังโดนโจมตีจะกะพริบ: ข้ามการวาดทุก ๆ 4 เฟรมสลับกัน)
        boolean blinkHidden = player.isHitFlashing() && (frameCounter / 4) % 2 == 0;
        if (!blinkHidden) {
            g2.drawImage(assets.getPlayerSprite(player.getFacing()), player.getX(), player.getY(),
                    Player.getSize(), Player.getSize(), null);
        }
        // หลังได้รับการฟื้น แถบ HP เป็นเขียวสว่าง
        Color playerHpColor = player.isHealFlashing() ? new Color(140, 255, 150) : new Color(110, 220, 130);
        drawHpBar(g2, player.getX(), player.getY() - 10, Player.getSize(),
                player.getHp(), player.getMaxHp(), playerHpColor);

        // HUD: ชื่อ Level มุมซ้ายบน
        g2.setFont(assets.getBodyFont());
        // เงาใต้ตัวอักษรให้ HUD อ่านออกบนพื้นหลังที่มีลวดลาย
        g2.setColor(new Color(0, 0, 0, 170));
        g2.drawString(currentLevel.getDisplayName(), 21, 37);
        g2.setColor(Color.WHITE);
        g2.drawString(currentLevel.getDisplayName(), 20, 36);

        // ข้อความสอนเฉพาะ Level 1 หายไปถาวรเมื่อเก็บดอกแรกได้
        if (currentLevel.getLevelNumber() == Game.FIRST_LEVEL && !anyFlowerCollected) {
            drawCenteredText(g2, "เดินทับดอกไม้เพื่อฟื้น HP", new Font("Tahoma", Font.PLAIN, 15),
                    new Color(255, 255, 255, 200), 64);
        }
    }

    /** หน้า Level Clear: พื้นหลังเมนู กับข้อความ Level Clear และชื่อ Level ถัดไปกลางจอ */
    public void drawLevelClear(Graphics2D g2, int currentLevelNumber) {
        drawMenuBackground(g2);
        drawCenteredText(g2, "Level Clear", assets.getTitleFont(), new Color(226, 214, 255), CLEAR_TITLE_Y);
        drawCenteredText(g2, "Level " + (currentLevelNumber + 1), assets.getBodyFont(),
                new Color(170, 162, 200), CLEAR_SUBTITLE_Y);
    }

    public void drawGameOver(Graphics2D g2, String gameOverReason) {
        drawMenuBackground(g2);
        drawCenteredText(g2, "GAME OVER", assets.getTitleFont(), new Color(240, 120, 120), MENU_TITLE_Y);
        drawCenteredText(g2, gameOverReason, assets.getBodyFont(), new Color(190, 160, 160), MENU_SUBTITLE_Y);
        drawButton(g2, RETRY_BUTTON, assets.getButtonRetry(), "Retry");
        drawButton(g2, MENU_BUTTON, assets.getButtonMenu(), "Main Menu");
    }

    public void drawWin(Graphics2D g2) {
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
}
