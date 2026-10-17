package ashinandflower;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

/** โหลดและเก็บรูป ฟอนต์ และเสียงทั้งหมดของเกมจากใน jar หรือโฟลเดอร์ res */
public class AssetLoader {

    // หน้าจอเมนู
    private final BufferedImage menuBackground;
    // ฉากของแต่ละ Level
    private final BufferedImage[] levelBackgrounds;
    // sprite ของ Player
    private final BufferedImage[] playerSprites = new BufferedImage[Direction.values().length];
    private final BufferedImage wanderingGhostSprite;
    private final BufferedImage shadowGhostSprite;
    private final BufferedImage spiritGuardianSprite;
    private final BufferedImage flowerNormalSprite;
    private final BufferedImage flowerSpiritSprite;
    // รูปปุ่ม
    private final BufferedImage buttonPlay;
    private final BufferedImage buttonRetry;
    private final BufferedImage buttonMenu;
    // ฟอนต์
    private Font pixelFont;

    // เพลงพื้นหลัง
    private Clip bgmClip;

    /** โหลดไฟล์ทั้งหมด levelCount คือจำนวนฉาก Level */
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

    // การหาไฟล์และโหลดไฟล์

    /** หา <folder>/<fileName> จาก classpath (ใน jar) ก่อน ไม่เจอค่อยหาโฟลเดอร์ res ในเครื่อง ไม่เจอคืน null */
    private URL findResource(String folder, String fileName) throws IOException, URISyntaxException {
        URL url = getClass().getResource("/" + folder + "/" + fileName);
        if (url != null) {
            return url;
        }
        File file = findResFile(folder, fileName);
        return file != null ? file.toURI().toURL() : null;
    }

    /** หา res/<folder>/<fileName> โดยไล่ขึ้นจากโฟลเดอร์ .class และโฟลเดอร์ที่สั่งรัน ไม่เจอคืน null */
    private File findResFile(String folder, String fileName) throws URISyntaxException {
        File classDir = new File(getClass().getProtectionDomain()
                .getCodeSource().getLocation().toURI()).getAbsoluteFile();
        File runDir = new File("").getAbsoluteFile();

        File[] startPoints = { classDir, runDir };
        for (File start : startPoints) {
            File dir = start;
            for (int i = 0; i < 5 && dir != null; i++) {
                File candidate = new File(dir, "res/" + folder + "/" + fileName);
                if (candidate.isFile()) {
                    return candidate;
                }
                dir = dir.getParentFile();
            }
        }
        return null;
    }

    private BufferedImage loadImage(String fileName) {
        try {
            URL url = findResource("images", fileName);
            if (url != null) {
                return ImageIO.read(url);
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("โหลดรูปไม่สำเร็จ: " + fileName, e);
        }
        throw new RuntimeException("ไม่พบไฟล์รูป: " + fileName);
    }

    private Font loadFont(String fileName) {
        try {
            URL url = findResource("fonts", fileName);
            if (url != null) {
                try (InputStream in = url.openStream()) {
                    return Font.createFont(Font.TRUETYPE_FONT, in);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("โหลดฟอนต์ไม่สำเร็จ: " + fileName, e);
        }
        throw new RuntimeException("ไม่พบไฟล์ฟอนต์: " + fileName);
    }

    /** โหลดเสียงจาก audio เป็น Clip เล่นไม่ได้คืน null */
    private Clip loadClip(String fileName) {
        try {
            URL url = findResource("audio", fileName);
            if (url != null) {
                AudioInputStream ais = AudioSystem.getAudioInputStream(url);
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                return clip;
            }
        } catch (Exception e) {
            // ไม่มีอุปกรณ์เสียง: เล่นต่อแบบไม่มีเสียง
        }
        return null;
    }

    // Getter

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

    // ขนาดฟอนต์ต้องเป็นจำนวนเท่าของ 8 ตามกริดของ Press Start 2P
    public Font getTitleFont() {
        return pixelFont.deriveFont(Font.PLAIN, 32f);
    }

    public Font getBodyFont() {
        return pixelFont.deriveFont(Font.PLAIN, 16f);
    }
}
