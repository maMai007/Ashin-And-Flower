package ashinandflower;

import java.util.List;

/**
 * อาวุธชิ้นเดียวของ Player
 * โจมตีศัตรูที่อยู่ใน "กรวย 90 องศาด้านหน้า" (รัศมี 88 px) ตามทิศที่ Player หันอยู่
 * มี Attack Cooldown 24 เฟรม (0.4 วินาที) ระหว่างการฟันแต่ละครั้ง
 */
public class LongSword {

    // ค่า Balance (อนุมัติแล้วในแผน ⑩ §3.1)
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

    /**
     * โจมตีหนึ่งครั้งตามคำสั่งของผู้เล่น
     * ศัตรูแต่ละตัวรับ damage ได้ครั้งเดียวต่อการโจมตีหนึ่งครั้ง แต่ตีโดนหลายตัวพร้อมกันได้
     * ถ้ายังติด cooldown อยู่ การคลิกครั้งนั้นจะไม่มีผล
     */
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
                // ผีกระเด็นไปตามทิศที่ฟัน (ถ้าตายจากดาบนี้ applyKnockback จะไม่ทำอะไร)
                enemy.applyKnockback(player.getFacing());
            }
        }
    }

    /**
     * ตรวจว่าศัตรูอยู่ในพื้นที่โจมตีหรือไม่
     * เงื่อนไข: ดาบเอื้อมถึงตัวศัตรู (range + ครึ่งตัวศัตรู วัดจากจุดกลาง) และศัตรูอยู่ในกรวย 90 องศา
     * (±45 องศา) ตามทิศที่หัน โดยผ่อนขอบกรวยให้ครึ่งตัวศัตรู เพื่อให้ผีที่ตัวคาบเกี่ยวขอบกรวยนับว่าโดน
     */
    private boolean checkHit(Player player, Enemy enemy) {
        double playerCenterX = player.getX() + Player.getSize() / 2.0;
        double playerCenterY = player.getY() + Player.getSize() / 2.0;
        double enemyCenterX = enemy.getX() + Enemy.getSize() / 2.0;
        double enemyCenterY = enemy.getY() + Enemy.getSize() / 2.0;

        double dx = enemyCenterX - playerCenterX;
        double dy = enemyCenterY - playerCenterY;

        // อยู่นอกระยะดาบ: วัดถึงตัวผี ไม่ใช่จุดกลาง จึงบวกครึ่งตัวผีเข้าไป
        // (ผีตัวกว้าง 72 px ที่ตัวทับรอยฟัน จุดกลางอาจอยู่ไกลกว่า range)
        double reach = range + Enemy.getSize() / 2.0;
        if (Math.hypot(dx, dy) > reach) {
            return false;
        }

        // ศัตรูที่เดินมาซ้อนทับตัว Player ถือว่าอยู่ในระยะดาบเสมอ
        // ไม่ต้องสนทิศ เพราะเมื่อติดตัวกันแล้วผู้เล่นควรฟันออกได้
        if (isOverlappingPlayer(player, enemy)) {
            return true;
        }

        // ตัดเหลือกรวย 90 องศาด้านหน้า (แกน y ของหน้าจอชี้ลง)
        // ระยะตามแกนที่หันต้องไม่น้อยกว่าระยะตามแกนขวาง จึงจะอยู่ในมุม ±45 องศา
        // บวก slack ครึ่งตัวผี ให้ผีที่ตัวคาบเกี่ยวขอบกรวยนับว่าโดน
        // แต่ผีที่ยืนชิดด้านข้างพอดี (เช่น dx = -72, dy = 0 ตอนหัน UP) ยังไม่โดน เพราะ 72 > 0 + 36
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

    /** กล่องของ Player กับศัตรูทับกันอยู่หรือไม่ */
    private boolean isOverlappingPlayer(Player player, Enemy enemy) {
        return enemy.getX() < player.getX() + Player.getSize()
                && player.getX() < enemy.getX() + Enemy.getSize()
                && enemy.getY() < player.getY() + Player.getSize()
                && player.getY() < enemy.getY() + Enemy.getSize();
    }

    /** นับถอยหลัง cooldown หนึ่งเฟรม (Game เรียกทุกเฟรม) */
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
