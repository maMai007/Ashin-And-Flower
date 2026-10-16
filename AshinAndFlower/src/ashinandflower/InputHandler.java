package ashinandflower;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * รับ Input จากคีย์บอร์ดและเมาส์ แล้วเก็บไว้ให้ Game อ่านในแต่ละเฟรม
 *
 * เมธอดของ Listener ทำงานบน EDT ส่วน Game อ่านค่าบน game thread
 * field ทุกตัวจึงประกาศเป็น volatile เพื่อให้ game thread มองเห็นค่าล่าสุดเสมอ
 */
public class InputHandler implements KeyListener, MouseListener {

    // ใช้ถามว่าตอนนี้อยู่หน้าจอไหน (คลิกนับเป็นการโจมตีเฉพาะตอนกำลังเล่น)
    private final Game game;

    // ---------- Input ที่ EDT เขียนและ game thread อ่าน ----------
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

    // ============================================================
    //  Getter และเมธอดเคลียร์ค่า (เรียกจาก game thread)
    // ============================================================

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

    /** ใช้คำขอโจมตีไปแล้ว (หนึ่งคลิกต่อการโจมตีหนึ่งครั้ง) */
    public void clearAttackRequest() {
        attackRequested = false;
    }

    /** ทิ้งคลิกที่ค้างอยู่ เรียกท้ายเฟรมเพื่อไม่ให้คลิกเดิมไปโดนปุ่มของหน้าจอถัดไป */
    public void clearClick() {
        clickPending = false;
    }

    /** คืน true หนึ่งครั้งเมื่อมีคลิกค้างอยู่และตกในกรอบปุ่มที่ส่งมา */
    public boolean takeClick(Rectangle button) {
        if (!clickPending) {
            return false;
        }
        return button.contains(clickX, clickY);
    }

    // ============================================================
    //  Event handling (ทำงานบน EDT เขียนลง field ที่เป็น volatile เท่านั้น)
    // ============================================================

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
        // ไม่ใช้ แต่ต้อง override ตามสัญญาของ KeyListener
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
        // ใช้ mousePressed แทนเพื่อให้โจมตีตอบสนองทันทีที่กด
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
