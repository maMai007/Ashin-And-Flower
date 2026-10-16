package ashinandflower;

/**
 * ดอกไม้ในเกม ใช้คลาสเดียวทั้งสองประเภทตาม Class Analysis
 * NORMAL ฟื้น HP ให้ Player ใน Level 1-2
 * SPIRIT ปรากฏหลัง SpiritGuardian ถูกกำจัด เก็บแล้วนำไปสู่ Win Flow (ไม่เปลี่ยน Game State เอง)
 */
public class Flower {

    // ขนาดกล่องเก็บของดอกไม้ (px)
    private static final int SIZE = 48;

    // ค่า Balance (อนุมัติแล้วในแผน ⑩ §3.2)
    private static final int NORMAL_HEAL_AMOUNT = 25;

    private final int x;
    private final int y;
    private final FlowerType type;
    private final int healAmount;
    private boolean collected;
    // จำนวนเฟรมที่เหลือของวงแหวนตอนถูกเก็บ (Game อ่านไปวาด)
    private int pickupFlashFrames;

    public Flower(int x, int y, FlowerType type) {
        this.x = x;
        this.y = y;
        this.type = type;
        // Spirit Flower เป็น Objective ไม่ใช่ของฟื้น HP
        this.healAmount = (type == FlowerType.NORMAL) ? NORMAL_HEAL_AMOUNT : 0;
        this.collected = false;
    }

    /**
     * Player เดินเข้ามาเก็บ ไม่ต้องกดปุ่มเก็บ
     * ดอกหนึ่งดอกเก็บได้ครั้งเดียว และ Player ที่ตายแล้วเก็บไม่ได้
     */
    public void collect(Player player) {
        if (collected || player == null || player.isDead()) {
            return;
        }
        if (type == FlowerType.NORMAL) {
            player.heal(healAmount);
        }
        collected = true;
        // วงแหวนตอนเก็บมีเฉพาะ Spirit Flower ดอก Normal หายไปเลย
        if (type == FlowerType.SPIRIT) {
            pickupFlashFrames = 24;
        }
    }

    public boolean isPickupFlashing() {
        return pickupFlashFrames > 0;
    }

    /** นับถอยหลังเอฟเฟกต์หนึ่งเฟรม (Level เรียกทุกเฟรม แม้ดอกจะถูกเก็บไปแล้ว) */
    public void tickEffects() {
        if (pickupFlashFrames > 0) {
            pickupFlashFrames--;
        }
    }

    public int getPickupFlashFrames() {
        return pickupFlashFrames;
    }

    /** ตรวจว่า Player ทับดอกไม้อยู่หรือไม่ ใช้ตอน Level ตรวจการเก็บในแต่ละเฟรม */
    public boolean isTouching(Player player) {
        if (player == null) {
            return false;
        }
        return x < player.getX() + Player.getSize()
                && player.getX() < x + SIZE
                && y < player.getY() + Player.getSize()
                && player.getY() < y + SIZE;
    }

    public boolean isCollected() {
        return collected;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public FlowerType getType() {
        return type;
    }

    public int getHealAmount() {
        return healAmount;
    }

    public static int getSize() {
        return SIZE;
    }
}
