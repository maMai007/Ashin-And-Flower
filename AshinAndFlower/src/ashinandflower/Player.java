package ashinandflower;

import java.util.List;

public class Player {

    // ขนาดกล่องชนของ Player (px)
    private static final int SIZE = 72;

    private int x;
    private int y;
    private int hp;
    private final int maxHp = 100;
    // px ต่อการเรียก move หนึ่งครั้ง (ค่าชั่วคราว)
    private final int speed = 3;
    private Direction facing;
    private final LongSword sword;

    // กะพริบหลังโดนโจมตี (ภาพอย่างเดียว ไม่กันดาเมจ)
    private static final int HIT_FLASH_FRAMES = 30;
    private int hitFlashFrames;

    // แถบ HP กะพริบหลังฟื้น HP
    private static final int HEAL_FLASH_FRAMES = 30;
    private int healFlashFrames;

    // เศษพิกเซลสะสม ให้การเดินทแยงใช้กับพิกัด int ได้
    private double remainderX;
    private double remainderY;

    public Player() {
        // ตำแหน่งจริงถูกตั้งด้วย setPosition ตอนเข้า Level
        x = 0;
        y = 0;
        hp = maxHp;
        facing = Direction.DOWN;
        sword = new LongSword();
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Direction getFacing() {
        return facing;
    }

    public void move(Direction direction) {
        if (direction == null) {
            return;
        }
        switch (direction) {
            case UP:
                move(0, -1);
                break;
            case DOWN:
                move(0, 1);
                break;
            case LEFT:
                move(-1, 0);
                break;
            case RIGHT:
                move(1, 0);
                break;
        }
    }

    // เดินตามทิศ dx, dy (-1, 0, 1) เดินทแยงเร็วเท่าเดินตรง
    public void move(int dx, int dy) {
        dx = Integer.signum(dx);
        dy = Integer.signum(dy);
        if (dx == 0 && dy == 0) {
            return;
        }

        // ตอนเดินทแยง ใช้ทิศแนวนอนเป็นทิศที่หัน
        if (dx != 0) {
            facing = dx < 0 ? Direction.LEFT : Direction.RIGHT;
        } else {
            facing = dy < 0 ? Direction.UP : Direction.DOWN;
        }

        double step = (dx != 0 && dy != 0) ? speed / Math.sqrt(2) : speed;
        remainderX += dx * step;
        remainderY += dy * step;
        int stepX = (int) remainderX;
        int stepY = (int) remainderY;
        x += stepX;
        y += stepY;
        remainderX -= stepX;
        remainderY -= stepY;
    }

    /** ฟันด้วย LongSword หนึ่งครั้ง */
    public void attack(List<Enemy> enemies) {
        if (isDead()) {
            return;
        }
        sword.attack(this, enemies);
    }

    public LongSword getSword() {
        return sword;
    }

    /** ย้าย Player ไปจุดเริ่มของ Level */
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        this.remainderX = 0;
        this.remainderY = 0;
        this.hitFlashFrames = 0;
        this.healFlashFrames = 0;
    }

    /** กัน Player ไม่ให้ออกนอกกรอบพื้นที่เล่น */
    public void keepInside(int minX, int minY, int maxX, int maxY) {
        if (x < minX) {
            x = minX;
        }
        if (x > maxX) {
            x = maxX;
        }
        if (y < minY) {
            y = minY;
        }
        if (y > maxY) {
            y = maxY;
        }
    }

    /** ลด HP และเริ่มกะพริบ */
    public void takeDamage(int amount) {
        if (amount <= 0) {
            return;
        }
        hp = Math.max(0, hp - amount);
        hitFlashFrames = HIT_FLASH_FRAMES;
    }

    /** นับถอยหลังเอฟเฟกต์กะพริบหนึ่งเฟรม */
    public void tick() {
        if (hitFlashFrames > 0) {
            hitFlashFrames--;
        }
        if (healFlashFrames > 0) {
            healFlashFrames--;
        }
    }

    public boolean isHitFlashing() {
        return hitFlashFrames > 0;
    }

    public boolean isHealFlashing() {
        return healFlashFrames > 0;
    }

    public void heal(int amount) {
        if (amount <= 0 || isDead()) {
            return;
        }
        int oldHp = hp;
        hp = Math.min(maxHp, hp + amount);
        // กะพริบเฉพาะตอนที่ HP เพิ่มขึ้นจริง
        if (hp > oldHp) {
            healFlashFrames = HEAL_FLASH_FRAMES;
        }
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public boolean isDead() {
        return hp == 0;
    }

    public static int getSize() {
        return SIZE;
    }
}
