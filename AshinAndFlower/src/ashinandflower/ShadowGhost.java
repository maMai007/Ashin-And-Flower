package ashinandflower;

public final class ShadowGhost extends Enemy {

    // ค่า Balance ชั่วคราว
    private static final int HP = 60;
    private static final int DAMAGE = 12;
    private static final double SPEED = 2;
    // ไล่จากระยะไกลกว่า WanderingGhost
    private static final double FOLLOW_RANGE = 280;

    public ShadowGhost(int x, int y) {
        super(x, y);
        setStats(HP, DAMAGE, SPEED);
    }

    @Override
    public void update(Player player) {
        tickEffects();
        if (isKnockedBack()) {
            stepKnockback();
            return;              // ระหว่างกระเด็นไม่เดินและไม่ทำดาเมจ
        }
        if (isDefeated() || player == null || player.isDead()) {
            return;
        }
        // ไล่ Player ถ้าอยู่ใกล้ ไม่งั้นเดินสุ่ม
        if (distanceTo(player) <= FOLLOW_RANGE) {
            moveToward(player);
        } else {
            wander();
        }
        attack(player);
    }
}
