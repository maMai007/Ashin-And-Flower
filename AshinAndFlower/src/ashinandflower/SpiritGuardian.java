package ashinandflower;

public final class SpiritGuardian extends Enemy {

    // ค่า Balance ชั่วคราว
    private static final int HP = 240;
    private static final int DAMAGE = 16;
    private static final double SPEED = 1;

    public SpiritGuardian(int x, int y) {
        super(x, y);
        setStats(HP, DAMAGE, SPEED);
    }

    // บอสเดินตามช้า ๆ และทำแค่ contact damage
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
        moveToward(player);
        attack(player);
    }
}
