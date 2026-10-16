package ashinandflower;

import java.util.ArrayList;

/**
 * เก็บข้อมูลของแต่ละ Level: ศัตรู ดอกไม้ และเงื่อนไขผ่านระดับ
 * Level 1-2 ผ่านเมื่อกำจัดศัตรูครบ, Level 3 ต้องกำจัด SpiritGuardian แล้วเก็บ Spirit Flower
 */
public class Level {

    // ขนาดหน้าจอและกรอบพื้นที่เล่น (ตามแผน ⑩ §3.3)
    public static final int SCREEN_WIDTH = 960;
    public static final int SCREEN_HEIGHT = 540;
    public static final int PLAY_LEFT = 16;
    public static final int PLAY_TOP = 48;                     // เว้นที่ให้ HUD ด้านบน
    public static final int PLAY_RIGHT = SCREEN_WIDTH - 16;    // 944
    public static final int PLAY_BOTTOM = SCREEN_HEIGHT - 16;  // 524

    private final int levelNumber;
    private final ArrayList<Enemy> enemies;
    private final ArrayList<Flower> flowers;
    private boolean cleared;
    private boolean spiritFlowerSpawned;

    public Level(int levelNumber) {
        this.levelNumber = levelNumber;
        this.enemies = new ArrayList<>();
        this.flowers = new ArrayList<>();
        this.cleared = false;
        this.spiritFlowerSpawned = false;

        createEnemies();
        createFlowers();
    }

    /**
     * สร้างศัตรูตามจำนวนที่ล็อกไว้ในเอกสาร ③
     * กระจายทั่วพื้นที่เล่น ทุกจุดอยู่ในกรอบและห่างจุดเกิด Player (444, 412) อย่างน้อย 200 px
     */
    private void createEnemies() {
        if (levelNumber == 1) {
            enemies.add(new WanderingGhost(80, 90));
            enemies.add(new WanderingGhost(780, 90));
            enemies.add(new WanderingGhost(80, 330));
            enemies.add(new WanderingGhost(780, 330));
        } else if (levelNumber == 2) {
            enemies.add(new WanderingGhost(70, 80));
            enemies.add(new WanderingGhost(800, 80));
            enemies.add(new ShadowGhost(70, 360));
            enemies.add(new ShadowGhost(800, 360));
            enemies.add(new ShadowGhost(440, 70));
        } else {
            enemies.add(new SpiritGuardian(444, 80));
        }
    }

    /** สร้างดอกไม้ Normal เฉพาะ Level 1-2 (ตามแผน ⑩ §3.2) กระจายทั่วพื้นที่เล่นเหมือนศัตรู */
    private void createFlowers() {
        if (levelNumber == 1) {
            flowers.add(new Flower(180, 230, FlowerType.NORMAL));
            flowers.add(new Flower(720, 200, FlowerType.NORMAL));
            flowers.add(new Flower(450, 120, FlowerType.NORMAL));
        } else if (levelNumber == 2) {
            flowers.add(new Flower(250, 110, FlowerType.NORMAL));
            flowers.add(new Flower(820, 180, FlowerType.NORMAL));
            flowers.add(new Flower(120, 300, FlowerType.NORMAL));
            flowers.add(new Flower(660, 330, FlowerType.NORMAL));
        }
    }

    /** เรียกทุกเฟรมจาก Game */
    public void update(Player player) {
        if (player == null || cleared) {
            return;
        }

        // 1) ศัตรูเดินและสร้าง contact damage
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            if (!enemy.isDefeated()) {
                enemy.update(player);
                enemy.keepInside(PLAY_LEFT, PLAY_TOP,
                        PLAY_RIGHT - Enemy.getSize(), PLAY_BOTTOM - Enemy.getSize());
            }
        }

        // 2) Player เดินทับดอกไม้แล้วเก็บเลย ไม่ต้องกดปุ่ม
        for (int i = 0; i < flowers.size(); i++) {
            Flower flower = flowers.get(i);
            // นับเอฟเฟกต์ก่อน เพื่อให้ดอกที่เก็บไปแล้วยังนับถอยหลังวงแหวนต่อได้
            flower.tickEffects();
            if (!flower.isCollected() && flower.isTouching(player)) {
                flower.collect(player);
            }
        }

        // 3) บอสตายแล้วให้ Spirit Flower ปรากฏ (ครั้งเดียว)
        if (levelNumber == 3 && checkBossDefeated() && !spiritFlowerSpawned) {
            spawnSpiritFlower();
        }

        // 4) ตรวจเงื่อนไขผ่านระดับ ตั้ง cleared ครั้งเดียวเพื่อกันทำงานซ้ำ
        if (levelNumber == 3) {
            // รอให้วงแหวนของ Spirit Flower เล่นจนจบก่อนค่อยไปหน้า Win
            if (checkBossDefeated() && isSpiritFlowerCollected()) {
                boolean ringFinished = true;
                for (int i = 0; i < flowers.size(); i++) {
                    if (flowers.get(i).isPickupFlashing()) {
                        ringFinished = false;
                    }
                }
                if (ringFinished) {
                    cleared = true;
                }
            }
        } else if (allEnemiesDefeated()) {
            cleared = true;
        }
    }

    public boolean checkLevelClear() {
        return cleared;
    }

    public boolean checkBossDefeated() {
        for (int i = 0; i < enemies.size(); i++) {
            if (enemies.get(i) instanceof SpiritGuardian) {
                return enemies.get(i).isDefeated();
            }
        }
        return false;
    }

    public boolean isSpiritFlowerCollected() {
        for (int i = 0; i < flowers.size(); i++) {
            Flower flower = flowers.get(i);
            if (flower.getType() == FlowerType.SPIRIT && flower.isCollected()) {
                return true;
            }
        }
        return false;
    }

    private void spawnSpiritFlower() {
        int centerX = SCREEN_WIDTH / 2 - Flower.getSize() / 2;
        int centerY = SCREEN_HEIGHT / 2 - Flower.getSize() / 2;
        flowers.add(new Flower(centerX, centerY, FlowerType.SPIRIT));
        spiritFlowerSpawned = true;
    }

    private boolean allEnemiesDefeated() {
        for (int i = 0; i < enemies.size(); i++) {
            if (!enemies.get(i).isDefeated()) {
                return false;
            }
        }
        return true;
    }

    /** จุดเริ่มต้นของ Player ในแต่ละ Level (กลางล่างของพื้นที่เล่น) */
    public int getPlayerStartX() {
        return SCREEN_WIDTH / 2 - Player.getSize() / 2;
    }

    public int getPlayerStartY() {
        return PLAY_BOTTOM - Player.getSize() - 40;
    }

    /** ชื่อ Level ที่แสดงบน HUD (ใช้ชื่อทางการภาษาอังกฤษ) */
    public String getDisplayName() {
        if (levelNumber == 1) {
            return "Forgotten Garden";
        } else if (levelNumber == 2) {
            return "Haunted Forest";
        } else {
            return "Spirit Ruins";
        }
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public ArrayList<Enemy> getEnemies() {
        return enemies;
    }

    public ArrayList<Flower> getFlowers() {
        return flowers;
    }
}
