package ashinandflower;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/** รับ Input จากคีย์บอร์ดและเมาส์ให้ Game อ่านทุกเฟรม */
public class InputHandler implements KeyListener, MouseListener {

    // ใช้ถามว่าอยู่หน้าจอไหน
    private final Game game;

    // volatile เพราะ EDT เขียนและ game thread อ่าน
    private volatile boolean upPressed;
    private volatile boolean downPressed;
    private volatile boolean leftPressed;
    private volatile boolean rightPressed;
    private volatile boolean attackRequested;
    private volatile boolean clickPending;
    private volatile int clickX;
    private volatile int clickY;

    public InputHandler(Game game) {
        this.game = game;
    }

    // Getter และเมธอดเคลียร์ค่า

    public boolean isUpPressed() {
        return upPressed;
    }

    public boolean isDownPressed() {
        return downPressed;
    }

    public boolean isLeftPressed() {
        return leftPressed;
    }

    public boolean isRightPressed() {
        return rightPressed;
    }

    public boolean isAttackRequested() {
        return attackRequested;
    }

    /** ล้างคำขอโจมตี */
    public void clearAttackRequest() {
        attackRequested = false;
    }

    /** ทิ้งคลิกที่ค้างอยู่ */
    public void clearClick() {
        clickPending = false;
    }

    /** คืน true ถ้าคลิกที่ค้างอยู่โดนปุ่มนี้ */
    public boolean takeClick(Rectangle button) {
        if (!clickPending) {
            return false;
        }
        return button.contains(clickX, clickY);
    }

    // รับ Event (ทำงานบน EDT)

    @Override
    public void keyPressed(KeyEvent e) {
        setKey(e.getKeyCode(), true);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        setKey(e.getKeyCode(), false);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // ไม่ใช้
    }

    private void setKey(int keyCode, boolean pressed) {
        switch (keyCode) {
            case KeyEvent.VK_W -> upPressed = pressed;
            case KeyEvent.VK_S -> downPressed = pressed;
            case KeyEvent.VK_A -> leftPressed = pressed;
            case KeyEvent.VK_D -> rightPressed = pressed;
            default -> {
                // ปุ่มอื่นไม่มีผลในเกมนี้
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        clickX = e.getX();
        clickY = e.getY();
        clickPending = true;
        if (game.getState() == GameState.PLAYING) {
            attackRequested = true;
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // ใช้ mousePressed แทน
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
}
