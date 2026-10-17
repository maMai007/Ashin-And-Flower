package ashinandflower;

/** ดอกไม้ NORMAL ฟื้น HP ส่วน SPIRIT เป็นเป้าหมายของ Level สุดท้าย */
public class Flower {

    // ขนาดกล่องเก็บของดอกไม้ (px)
    private static final int SIZE = 48;

    // ค่า Balance
    private static final int NORMAL_HEAL_AMOUNT = 25;

    private final int x;
    private final int y;
    private final FlowerType type;
    private final int healAmount;
    private boolean collected;
    // เฟรมที่เหลือของวงแหวนตอนถูกเก็บ
    private int pickupFlashFrames;

    public Flower(int x, int y, FlowerType type) {
        this.x = x;
        this.y = y;
        this.type = type;
        // Spirit Flower เป็น Objective ไม่ใช่ของฟื้น HP
        this.healAmount = (type == FlowerType.NORMAL) ? NORMAL_HEAL_AMOUNT : 0;
        this.collected = false;
    }

    /** เก็บดอกไม้ (ดอกหนึ่งเก็บได้ครั้งเดียว) */
    public void collect(Player player) {
        if (collected || player == null || player.isDead()) {
            return;
        }
        if (type == FlowerType.NORMAL) {
            player.heal(healAmount);
        }
        collected = true;
        // วงแหวนตอนเก็บมีเฉพาะ Spirit Flower
        if (type == FlowerType.SPIRIT) {
            pickupFlashFrames = 24;
        }
    }

    public boolean isPickupFlashing() {
        return pickupFlashFrames > 0;
    }

    /** นับถอยหลังเอฟเฟกต์หนึ่งเฟรม */
    public void tickEffects() {
        if (pickupFlashFrames > 0) {
            pickupFlashFrames--;
        }
    }

    public int getPickupFlashFrames() {
        return pickupFlashFrames;
    }

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

    public static int getSize() {
        return SIZE;
    }
}
