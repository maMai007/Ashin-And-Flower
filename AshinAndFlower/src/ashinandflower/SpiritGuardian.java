package ashinandflower;

public final class SpiritGuardian extends Enemy {

    // Provisional balance values.
    private static final int HP = 240;
    private static final int DAMAGE = 16;
    private static final double SPEED = 1;

    public SpiritGuardian(int x, int y) {
        super(x, y);
        setStats(HP, DAMAGE, SPEED);
    }

    // Boss follows slowly and deals contact damage only; no special attack.
    @Override
    public void update(Player player) {
        tickEffects();
        if (isKnockedBack()) {
            stepKnockback();
            return;              // ระหว่างกระเด็น ไม่เดินเข้าหาและไม่ทำ contact damage
        }
        if (isDefeated() || player == null || player.isDead()) {
            return;
        }
        moveToward(player);
        attack(player);
    }
}
