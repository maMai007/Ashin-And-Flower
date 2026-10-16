package ashinandflower;

public final class ShadowGhost extends Enemy {

    // Provisional balance values.
    private static final int HP = 60;
    private static final int DAMAGE = 12;
    private static final double SPEED = 2;
    // Farther than WanderingGhost (200) because ShadowGhost is the more aggressive enemy.
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
            return;              // ระหว่างกระเด็น ไม่เดินเข้าหาและไม่ทำ contact damage
        }
        if (isDefeated() || player == null || player.isDead()) {
            return;
        }
        // เดินสุ่มไปเรื่อย ๆ จนกว่า Player จะเข้าใกล้ จึงค่อยไล่
        if (distanceTo(player) <= FOLLOW_RANGE) {
            moveToward(player);
        } else {
            wander();
        }
        attack(player);
    }
}
