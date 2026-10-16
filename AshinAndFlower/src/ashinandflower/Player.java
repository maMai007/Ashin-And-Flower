package ashinandflower;

import java.util.List;

public class Player {

    // ขนาดกล่องชนของ Player (px) ใช้ร่วมกันทั้งการชนและการตรวจพื้นที่โจมตี
    private static final int SIZE = 72;

    private int x;
    private int y;
    private int hp;
    private final int maxHp = 100;
    // Provisional testing value (px per move call); final speed is decided during Balance.
    private final int speed = 3;
    private Direction facing;
    private final LongSword sword;

    // หลังโดนโจมตีจะอมตะชั่วคราว 30 เฟรม (0.5 วินาที) กันโดนศัตรูหลายตัวรุมพร้อมกันในเฟรมเดียว
    private static final int INVULNERABLE_FRAMES = 30;
    private int invulnerableFrames;

    // Sub-pixel movement carried between calls so diagonal steps work with integer coordinates.
    private double remainderX;
    private double remainderY;

    public Player() {
        // Starting position is not locked yet; provisional origin until Level layout is defined.
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

    // dx and dy are input directions (-1, 0 or 1); diagonal movement is normalized to the same speed.
    public void move(int dx, int dy) {
        dx = Integer.signum(dx);
        dy = Integer.signum(dy);
        if (dx == 0 && dy == 0) {
            return;
        }

        // On diagonal input the horizontal direction decides facing.
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

    /** สั่งโจมตีด้วย LongSword หนึ่งครั้ง ตามทิศที่ Player หันอยู่ ณ เวลาที่คลิก */
    public void attack(List<Enemy> enemies) {
        if (isDead()) {
            return;
        }
        sword.attack(this, enemies);
    }

    public LongSword getSword() {
        return sword;
    }

    /** ย้าย Player ไปจุดเริ่มต้นของ Level (ใช้ตอนเริ่มเกมและเปลี่ยน Level) */
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        this.remainderX = 0;
        this.remainderY = 0;
        this.invulnerableFrames = 0;
    }

    /** กัน Player ไม่ให้เดินออกนอกกรอบพื้นที่เล่น (แนวต้นไม้รอบนอก) */
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

    public void takeDamage(int amount) {
        if (amount <= 0 || invulnerableFrames > 0) {
            return;
        }
        hp = Math.max(0, hp - amount);
        invulnerableFrames = INVULNERABLE_FRAMES;
    }

    /** นับถอยหลังเวลาอมตะหนึ่งเฟรม (Game เรียกทุกเฟรม) */
    public void tick() {
        if (invulnerableFrames > 0) {
            invulnerableFrames--;
        }
    }

    public boolean isInvulnerable() {
        return invulnerableFrames > 0;
    }

    public void heal(int amount) {
        if (amount <= 0 || isDead()) {
            return;
        }
        hp = Math.min(maxHp, hp + amount);
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
