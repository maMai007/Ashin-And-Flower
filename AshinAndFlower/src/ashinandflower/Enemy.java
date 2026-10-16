package ashinandflower;

import java.util.Random;

public abstract class Enemy {

    // ขนาดกล่องชนของศัตรู (px); ขนาดของ Player อ่านจาก Player.getSize()
    private static final int SIZE = 72;

    private int x;
    private int y;
    private int hp;
    private int maxHp;
    private int damage;
    private boolean defeated;
    private double speed;

    // Sub-pixel movement carried between frames so fractional speeds work with integer coordinates.
    private double remainderX;
    private double remainderY;

    // Frames to wait between contact hits while still touching the Player (48 frames = 0.8 s at 60 FPS).
    private static final int CONTACT_COOLDOWN_FRAMES = 48;
    private int contactCooldown;

    // Frames left of the white hit flash after taking a sword hit (Game reads it to draw).
    private int hitFlashFrames;

    // Knockback after a sword hit: pushed knockbackX/Y px per frame for knockbackFrames frames.
    private int knockbackFrames;
    private double knockbackX, knockbackY;

    // Random wandering shared by WanderingGhost and ShadowGhost.
    // Provisional: frames before picking a new random wander direction.
    private static final int WANDER_FRAMES = 60;
    private final Random random = new Random();
    private Direction wanderDirection;
    private int wanderFramesLeft;

    protected Enemy(int x, int y) {
        this.x = x;
        this.y = y;
    }

    // Called once by each subclass constructor to set its stats.
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

    // Deals contact damage while touching the Player, at most once every CONTACT_COOLDOWN_FRAMES.
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

    // Counts down visual effects by one frame; subclasses call this first in update().
    protected void tickEffects() {
        if (hitFlashFrames > 0) {
            hitFlashFrames--;
        }
    }

    public boolean isHitFlashing() {
        return hitFlashFrames > 0;
    }

    // Starts a knockback in the direction the Player swung (LongSword calls this after a hit).
    public void applyKnockback(Direction dir) {
        if (defeated || dir == null) {
            return;
        }
        knockbackFrames = 8;
        double p = 7.0;   // px per frame, 56 px total
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

    // Moves one frame of the knockback; subclasses call this instead of moving while knocked back.
    protected void stepKnockback() {
        if (knockbackFrames > 0) {
            moveBy(knockbackX, knockbackY);
            knockbackFrames--;
        }
    }

    // Walks in a random direction, picking a new one every WANDER_FRAMES frames.
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

    // Center-to-center distance to the Player in px.
    protected double distanceTo(Player player) {
        double dx = (player.getX() + Player.getSize() / 2.0) - (x + SIZE / 2.0);
        double dy = (player.getY() + Player.getSize() / 2.0) - (y + SIZE / 2.0);
        return Math.hypot(dx, dy);
    }

    // Moves up to `speed` px toward the Player's position, without overshooting.
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

    /** HP เริ่มต้นของศัตรูตัวนี้ ใช้คำนวณสัดส่วน HP Bar */
    public int getMaxHp() {
        return maxHp;
    }

    public static int getSize() {
        return SIZE;
    }
}
