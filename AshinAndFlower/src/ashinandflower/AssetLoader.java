package ashinandflower;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

/**
 * โหลดและเก็บไฟล์ทั้งหมดของเกม (รูป ฟอนต์ เสียง) จากโฟลเดอร์ res
 *
 * Game สร้างคลาสนี้หนึ่งตัวตอนเริ่มเกม แล้วขอไฟล์ผ่าน getter
 * ถ้ารูปหรือฟอนต์หายจะ throw ทันทีตอนสร้าง ส่วนเสียงที่ใช้ไม่ได้จะเป็น null
 */
public class AssetLoader {

    // ---------- รูปภาพ ----------
    // ภาพเดียวใช้ร่วมกันทุกหน้าจอเมนู
    private final BufferedImage menuBackground;
    // ฉากของแต่ละ Level เก็บตาม index 1 ถึง levelCount (ช่อง 0 ไม่ใช้ เพื่อให้เลข Level ตรงกับ index)
    private final BufferedImage[] levelBackgrounds;
    // sprite ของ Player เก็บตามทิศ ใช้ Direction.ordinal() เป็น index
    private final BufferedImage[] playerSprites = new BufferedImage[Direction.values().length];
    private final BufferedImage wanderingGhostSprite;
    private final BufferedImage shadowGhostSprite;
    private final BufferedImage spiritGuardianSprite;
    private final BufferedImage flowerNormalSprite;
    private final BufferedImage flowerSpiritSprite;
    // รูปพื้นหลังปุ่ม มีข้อความอยู่ในรูปแล้ว จึงไม่ต้องวาดตัวอักษรทับ
    private final BufferedImage buttonPlay;
    private final BufferedImage buttonRetry;
    private final BufferedImage buttonMenu;
    // ฟอนต์พิกเซลต้นแบบ ขนาด 1 แล้วค่อย deriveFont ตามขนาดที่ใช้
    private Font pixelFont;

    // ---------- เสียง ----------
    // เพลงพื้นหลังเพลงเดียว (โหลดไม่ได้ = null เกมจะเงียบแต่เล่นต่อได้)
    private Clip bgmClip;

    /** โหลดไฟล์ทั้งหมด levelCount คือจำนวน Level ที่มีฉากให้โหลด (bg_level1.png ถึง bg_levelN.png) */
    public AssetLoader(int levelCount) {
        menuBackground = loadImage("bg_menu.png");
        levelBackgrounds = new BufferedImage[levelCount + 1];
        for (int i = 1; i <= levelCount; i++) {
            levelBackgrounds[i] = loadImage("bg_level" + i + ".png");
        }
        playerSprites[Direction.UP.ordinal()] = loadImage("player_up.png");
        playerSprites[Direction.DOWN.ordinal()] = loadImage("player_down.png");
        playerSprites[Direction.LEFT.ordinal()] = loadImage("player_left.png");
        playerSprites[Direction.RIGHT.ordinal()] = loadImage("player_right.png");
        wanderingGhostSprite = loadImage("wandering_ghost.png");
        shadowGhostSprite = loadImage("shadow_ghost.png");
        spiritGuardianSprite = loadImage("spirit_guardian.png");
        flowerNormalSprite = loadImage("flower_normal.png");
        flowerSpiritSprite = loadImage("flower_spirit.png");
        buttonPlay = loadImage("btn_play.png");
        buttonRetry = loadImage("btn_retry.png");
        buttonMenu = loadImage("btn_menu.png");
        pixelFont = loadFont("PressStart2P-Regular.ttf");
        bgmClip = loadClip("bgm.wav");
    }

    // ============================================================
    //  การหาไฟล์และโหลดไฟล์
    // ============================================================

    /**
     * หาไฟล์ใน res/<folder> โดยไล่ขึ้นจากที่อยู่ของไฟล์ .class (เช่น bin -> AshinAndFlower)
     * วิธีนี้ทำงานได้ไม่ว่าจะสั่งรันจากโฟลเดอร์ไหน ถ้าไม่เจอคืน null
     */
    private File findResFile(String folder, String fileName) throws URISyntaxException {
        File dir = new File(getClass().getProtectionDomain()
                .getCodeSource().getLocation().toURI()).getAbsoluteFile();
        for (int i = 0; i < 5 && dir != null; i++) {
            File candidate = new File(dir, "res/" + folder + "/" + fileName);
            if (candidate.isFile()) {
                return candidate;
            }
            dir = dir.getParentFile();
        }
        return null;
    }

    /** โหลดไฟล์รูปจาก res/images */
    private BufferedImage loadImage(String fileName) {
        try {
            File file = findResFile("images", fileName);
            if (file != null) {
                return ImageIO.read(file);
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("โหลดรูปไม่สำเร็จ: " + fileName, e);
        }
        throw new RuntimeException("ไม่พบไฟล์รูป: " + fileName);
    }

    /** โหลดไฟล์ฟอนต์จาก res/fonts */
    private Font loadFont(String fileName) {
        try {
            File file = findResFile("fonts", fileName);
            if (file != null) {
                return Font.createFont(Font.TRUETYPE_FONT, file);
            }
        } catch (Exception e) {
            throw new RuntimeException("โหลดฟอนต์ไม่สำเร็จ: " + fileName, e);
        }
        throw new RuntimeException("ไม่พบไฟล์ฟอนต์: " + fileName);
    }

    /**
     * โหลดไฟล์เสียงจาก res/audio เป็น Clip
     *
     * ใช้ javax.sound.sampled.Clip เพราะ java.applet.AudioClip ถูกถอดออกจาก JDK 24 แล้ว
     * ถ้าเครื่องไม่มีอุปกรณ์เสียงหรือเปิดไฟล์ไม่ได้ จะคืน null แล้วเกมเล่นต่อแบบไม่มีเสียง
     */
    private Clip loadClip(String fileName) {
        try {
            File file = findResFile("audio", fileName);
            if (file != null) {
                AudioInputStream ais = AudioSystem.getAudioInputStream(file);
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                return clip;
            }
        } catch (Exception e) {
            // เช่น เครื่องไม่มีลำโพง/การ์ดเสียง: ข้ามไปเล่นแบบไม่มีเสียง
        }
        return null;
    }

    // ============================================================
    //  Getter
    // ============================================================

    public BufferedImage getMenuBackground() {
        return menuBackground;
    }

    public BufferedImage getLevelBackground(int levelNumber) {
        return levelBackgrounds[levelNumber];
    }

    public BufferedImage getPlayerSprite(Direction direction) {
        return playerSprites[direction.ordinal()];
    }

    public BufferedImage getWanderingGhostSprite() {
        return wanderingGhostSprite;
    }

    public BufferedImage getShadowGhostSprite() {
        return shadowGhostSprite;
    }

    public BufferedImage getSpiritGuardianSprite() {
        return spiritGuardianSprite;
    }

    public BufferedImage getFlowerNormalSprite() {
        return flowerNormalSprite;
    }

    public BufferedImage getFlowerSpiritSprite() {
        return flowerSpiritSprite;
    }

    public BufferedImage getButtonPlay() {
        return buttonPlay;
    }

    public BufferedImage getButtonRetry() {
        return buttonRetry;
    }

    public BufferedImage getButtonMenu() {
        return buttonMenu;
    }

    /** คืน null ถ้าโหลดเสียงไม่ได้ */
    public Clip getBgmClip() {
        return bgmClip;
    }

    // ขนาดต้องเป็นจำนวนเท่าของ 8 เพราะ Press Start 2P ออกแบบบนกริด 8px ถ้าไม่ลงตัวพิกเซลจะเบี้ยว
    public Font getTitleFont() {
        return pixelFont.deriveFont(Font.PLAIN, 32f);
    }

    public Font getBodyFont() {
        return pixelFont.deriveFont(Font.PLAIN, 16f);
    }
}
