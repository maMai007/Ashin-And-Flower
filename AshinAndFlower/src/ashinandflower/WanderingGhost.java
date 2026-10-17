package ashinandflower;

public final class WanderingGhost extends Enemy {

    // ค่า Balance ชั่วคราว
    private static final int HP = 45;
    private static final int DAMAGE = 8;
    private static final double SPEED = 1.5;
    private static final double FOLLOW_RANGE = 200;

    public WanderingGhost(int x, int y) {
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
        if (distanceTo(player) <= FOLLOW_RANGE) {
            moveToward(player);
        } else {
            wander();
        }
        attack(player);
    }
}
