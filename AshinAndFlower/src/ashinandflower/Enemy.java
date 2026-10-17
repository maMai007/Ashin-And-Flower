package ashinandflower;

import java.util.Random;

public abstract class Enemy {

    // ขนาดกล่องชนของศัตรู (px)
    private static final int SIZE = 72;

    private int x;
    private int y;
    private int hp;
    private int maxHp;
    private int damage;
    private boolean defeated;
    private double speed;

    // เศษพิกเซลสะสมข้ามเฟรม ให้ความเร็วทศนิยมใช้กับพิกัด int ได้
    private double remainderX;
    private double remainderY;

    // เฟรมที่รอก่อนชน Player ซ้ำ (0.8 วินาที)
    private static final int CONTACT_COOLDOWN_FRAMES = 48;
    private int contactCooldown;

    // เฟรมที่เหลือของการกะพริบขาวหลังโดนดาบ
    private int hitFlashFrames;

    // การกระเด็นหลังโดนดาบ
    private int knockbackFrames;
    private double knockbackX, knockbackY;

    // การเดินสุ่ม: สุ่มทิศใหม่ทุก WANDER_FRAMES เฟรม
    private static final int WANDER_FRAMES = 60;
    private final Random random = new Random();
    private Direction wanderDirection;
    private int wanderFramesLeft;

    protected Enemy(int x, int y) {
        this.x = x;
        this.y = y;
    }

    // subclass เรียกใน constructor เพื่อตั้งค่าสถานะ
    protected final void setStats(int hp, int damage, double speed) {
        this.hp = Math.max(0, hp);
        this.maxHp = this.hp;
        this.damage = Math.max(0, damage);
        this.speed = Math.max(0, speed);
    }

    public void takeDamage(int amount) {
        if (defeated || amount <= 0) {
            return;
        }
        hp = Math.max(0, hp - amount);
        hitFlashFrames = 6;
        checkHP();
    }

    // ทำ contact damage เมื่อแตะ Player ตาม cooldown
    public void attack(Player player) {
        if (defeated || player == null || player.isDead()) {
            return;
        }
        if (contactCooldown > 0) {
            contactCooldown--;
            return;
        }
        if (isTouching(player)) {
            player.takeDamage(damage);
            contactCooldown = CONTACT_COOLDOWN_FRAMES;
        }
    }

    public boolean isDefeated() {
        return defeated;
    }

    protected void checkHP() {
        if (hp == 0 && !defeated) {
            defeat();
        }
    }

    protected void defeat() {
        defeated = true;
        knockbackFrames = 0;
    }

    public abstract void update(Player player);

    // นับถอยหลังเอฟเฟกต์ภาพหนึ่งเฟรม
    protected void tickEffects() {
        if (hitFlashFrames > 0) {
            hitFlashFrames--;
        }
    }

    public boolean isHitFlashing() {
        return hitFlashFrames > 0;
    }

    // เริ่มกระเด็นไปตามทิศที่ Player ฟัน
    public void applyKnockback(Direction dir) {
        if (defeated || dir == null) {
            return;
        }
        knockbackFrames = 8;
        double p = 7.0;   // px ต่อเฟรม รวม 56 px
        switch (dir) {
            case UP:
                knockbackX = 0;
                knockbackY = -p;
                break;
            case DOWN:
                knockbackX = 0;
                knockbackY = p;
                break;
            case LEFT:
                knockbackX = -p;
                knockbackY = 0;
                break;
            case RIGHT:
                knockbackX = p;
                knockbackY = 0;
                break;
        }
    }

    public boolean isKnockedBack() {
        return knockbackFrames > 0;
    }

    // กระเด็นหนึ่งเฟรม
    protected void stepKnockback() {
        if (knockbackFrames > 0) {
            moveBy(knockbackX, knockbackY);
            knockbackFrames--;
        }
    }

    // เดินไปทิศที่สุ่มได้
    protected void wander() {
        if (wanderDirection == null || wanderFramesLeft <= 0) {
            Direction[] directions = Direction.values();
            wanderDirection = directions[random.nextInt(directions.length)];
            wanderFramesLeft = WANDER_FRAMES;
        }
        wanderFramesLeft--;
        double step = speed;
        switch (wanderDirection) {
            case UP:
                moveBy(0, -step);
                break;
            case DOWN:
                moveBy(0, step);
                break;
            case LEFT:
                moveBy(-step, 0);
                break;
            case RIGHT:
                moveBy(step, 0);
                break;
        }
    }

    protected boolean isTouching(Player player) {
        return x < player.getX() + Player.getSize()
                && player.getX() < x + SIZE
                && y < player.getY() + Player.getSize()
                && player.getY() < y + SIZE;
    }

    // ระยะระหว่างจุดกลางตัวศัตรูกับ Player (px)
    protected double distanceTo(Player player) {
        double dx = (player.getX() + Player.getSize() / 2.0) - (x + SIZE / 2.0);
        double dy = (player.getY() + Player.getSize() / 2.0) - (y + SIZE / 2.0);
        return Math.hypot(dx, dy);
    }

    // เดินเข้าหา Player ไม่เกิน speed px
    protected void moveToward(Player player) {
        double dx = player.getX() - (x + remainderX);
        double dy = player.getY() - (y + remainderY);
        double distance = Math.hypot(dx, dy);
        if (distance == 0) {
            return;
        }
        double step = Math.min(speed, distance);
        moveBy(dx / distance * step, dy / distance * step);
    }

    protected void moveBy(double dx, double dy) {
        if (defeated) {
            return;
        }
        remainderX += dx;
        remainderY += dy;
        int stepX = (int) remainderX;
        int stepY = (int) remainderY;
        x += stepX;
        y += stepY;
        remainderX -= stepX;
        remainderY -= stepY;
    }

    /** กันศัตรูไม่ให้เดินออกนอกกรอบพื้นที่เล่น */
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

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getHp() {
        return hp;
    }

    /** HP เริ่มต้น ใช้คำนวณ HP Bar */
    public int getMaxHp() {
        return maxHp;
    }

    public static int getSize() {
        return SIZE;
    }
}
