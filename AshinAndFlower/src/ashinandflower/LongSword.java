package ashinandflower;

import java.util.List;

/** ดาบของ Player ฟันศัตรูในกรวย 90 องศาด้านหน้า */
public class LongSword {

    // ค่า Balance
    private static final int DEFAULT_DAMAGE = 20;
    private static final int DEFAULT_RANGE = 88;
    private static final int COOLDOWN_FRAMES = 24;

    private int damage;
    private int range;
    private int cooldownFrames;

    public LongSword() {
        this.damage = DEFAULT_DAMAGE;
        this.range = DEFAULT_RANGE;
    }

    /** ฟันหนึ่งครั้ง โดนได้หลายตัว ไม่มีผลถ้ายังติด cooldown */
    public void attack(Player player, List<Enemy> enemies) {
        if (cooldownFrames > 0) {
            return;
        }
        if (player == null || player.isDead() || enemies == null) {
            return;
        }
        cooldownFrames = COOLDOWN_FRAMES;
        for (Enemy enemy : enemies) {
            if (enemy == null || enemy.isDefeated()) {
                continue;
            }
            if (checkHit(player, enemy)) {
                enemy.takeDamage(damage);
                // ผีกระเด็นไปตามทิศที่ฟัน
                enemy.applyKnockback(player.getFacing());
            }
        }
    }

    /** ตรวจว่าศัตรูอยู่ในระยะและในกรวยดาบหรือไม่ */
    private boolean checkHit(Player player, Enemy enemy) {
        double playerCenterX = player.getX() + Player.getSize() / 2.0;
        double playerCenterY = player.getY() + Player.getSize() / 2.0;
        double enemyCenterX = enemy.getX() + Enemy.getSize() / 2.0;
        double enemyCenterY = enemy.getY() + Enemy.getSize() / 2.0;

        double dx = enemyCenterX - playerCenterX;
        double dy = enemyCenterY - playerCenterY;

        // นอกระยะดาบ (วัดถึงขอบตัวผี จึงบวกครึ่งตัว)
        double reach = range + Enemy.getSize() / 2.0;
        if (Math.hypot(dx, dy) > reach) {
            return false;
        }

        // ศัตรูที่ทับตัว Player โดนเสมอไม่ว่าหันทางไหน
        if (isOverlappingPlayer(player, enemy)) {
            return true;
        }

        // กรวย ±45 องศา ผ่อนขอบด้วย slack ครึ่งตัวผี
        double slack = Enemy.getSize() / 2.0;
        switch (player.getFacing()) {
            case UP:
                return dy <= 0 && Math.abs(dx) <= Math.abs(dy) + slack;
            case DOWN:
                return dy >= 0 && Math.abs(dx) <= Math.abs(dy) + slack;
            case LEFT:
                return dx <= 0 && Math.abs(dy) <= Math.abs(dx) + slack;
            case RIGHT:
                return dx >= 0 && Math.abs(dy) <= Math.abs(dx) + slack;
            default:
                return false;
        }
    }

    private boolean isOverlappingPlayer(Player player, Enemy enemy) {
        return enemy.getX() < player.getX() + Player.getSize()
                && player.getX() < enemy.getX() + Enemy.getSize()
                && enemy.getY() < player.getY() + Player.getSize()
                && player.getY() < enemy.getY() + Enemy.getSize();
    }

    /** นับถอยหลัง cooldown หนึ่งเฟรม */
    public void tick() {
        if (cooldownFrames > 0) {
            cooldownFrames--;
        }
    }

    public boolean isReady() {
        return cooldownFrames == 0;
    }

    public int getRange() {
        return range;
    }
}
