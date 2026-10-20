package com.store.billing.util;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class ArrowKeyNavigationUtil {

    public static void bindGrid(Component[][] grid) {
        if (grid == null) return;
        
        List<Component> flatList = new ArrayList<>();
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[r].length; c++) {
                Component comp = grid[r][c];
                if (comp != null && !flatList.contains(comp)) {
                    flatList.add(comp);
                }
            }
        }

        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[r].length; c++) {
                Component current = grid[r][c];
                if (current == null) continue;

                Component left = getAdjacent(grid, r, c, 0, -1);
                Component right = getAdjacent(grid, r, c, 0, 1);
                Component up = getAdjacent(grid, r, c, -1, 0);
                Component down = getAdjacent(grid, r, c, 1, 0);
                
                Component next = null;
                int idx = flatList.indexOf(current);
                if (idx >= 0 && idx < flatList.size() - 1) {
                    next = flatList.get(idx + 1);
                }

                bind(current, left, right, up, down, next);
            }
        }
    }

    private static Component getAdjacent(Component[][] grid, int r, int c, int dr, int dc) {
        Component origin = grid[r][c];
        int curR = r;
        int curC = c;

        while (true) {
            curR += dr;
            curC += dc;

            if (curR < 0 || curR >= grid.length) {
                return null;
            }

            int rowLen = grid[curR].length;
            if (rowLen == 0) {
                return null;
            }

            int checkC = curC;
            if (checkC < 0 || checkC >= rowLen) {
                if (dr == 0) {
                    // Moving horizontally past the row boundaries
                    return null;
                }
                checkC = Math.max(0, Math.min(checkC, rowLen - 1));
            }

            Component target = grid[curR][checkC];
            if (target != null && target != origin) {
                return target;
            }

            if (dr == 0 && dc == 0) {
                return null;
            }
        }
    }

    public static void bind(Component comp, Component left, Component right, Component up, Component down, Component next) {
        if (comp == null) return;
        Component target = comp;
        if (comp instanceof JComboBox && ((JComboBox<?>) comp).isEditable()) {
            Component ed = ((JComboBox<?>) comp).getEditor().getEditorComponent();
            if (ed != null) target = ed;
        }

        Component finalTarget = target;
        finalTarget.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isAltDown() || e.isControlDown()) {
                    return; // Let OS/Global shortcuts handle modified keys
                }
                int key = e.getKeyCode();
                if (key == KeyEvent.VK_LEFT) {
                    if (left != null && shouldNavigateHorizontal(comp, true)) {
                        focusComponent(left);
                        e.consume();
                    }
                } else if (key == KeyEvent.VK_RIGHT) {
                    if (right != null && shouldNavigateHorizontal(comp, false)) {
                        focusComponent(right);
                        e.consume();
                    }
                } else if (key == KeyEvent.VK_UP) {
                    if (comp instanceof JComboBox && ((JComboBox<?>) comp).isPopupVisible()) {
                        return;
                    }
                    if (up != null) {
                        focusComponent(up);
                        e.consume();
                    }
                } else if (key == KeyEvent.VK_DOWN) {
                    if (comp instanceof JComboBox && ((JComboBox<?>) comp).isPopupVisible()) {
                        return;
                    }
                    if (down != null) {
                        focusComponent(down);
                        e.consume();
                    }
                } else if (key == KeyEvent.VK_ENTER) {
                    if (comp instanceof JButton || comp instanceof JTextArea) {
                        return;
                    }
                    if (comp instanceof JComboBox && ((JComboBox<?>) comp).isPopupVisible()) {
                        return;
                    }
                    if (next != null) {
                        focusComponent(next);
                        e.consume();
                    } else {
                        finalTarget.transferFocus();
                        e.consume();
                    }
                }
            }
        });
    }

    private static boolean shouldNavigateHorizontal(Component comp, boolean isLeft) {
        if (comp instanceof JTextComponent) {
            JTextComponent tc = (JTextComponent) comp;
            int pos = tc.getCaretPosition();
            int selStart = tc.getSelectionStart();
            int selEnd = tc.getSelectionEnd();
            if (selStart != selEnd) {
                return true;
            }
            if (isLeft && pos == 0) return true;
            if (!isLeft && pos == tc.getText().length()) return true;
            return false;
        }
        if (comp instanceof JComboBox) {
            JComboBox<?> cb = (JComboBox<?>) comp;
            if (cb.isEditable()) {
                Component ed = cb.getEditor().getEditorComponent();
                if (ed instanceof JTextComponent) {
                    JTextComponent tc = (JTextComponent) ed;
                    int pos = tc.getCaretPosition();
                    if (isLeft && pos == 0) return true;
                    if (!isLeft && pos == tc.getText().length()) return true;
                    return false;
                }
            }
            return !cb.isPopupVisible();
        }
        return true;
    }

    public static void focusComponent(Component c) {
        if (c == null) return;
        if (c instanceof JComboBox && ((JComboBox<?>) c).isEditable()) {
            Component ed = ((JComboBox<?>) c).getEditor().getEditorComponent();
            if (ed != null) {
                ed.requestFocusInWindow();
                if (ed instanceof JTextComponent) {
                    ((JTextComponent) ed).selectAll();
                }
            }
        } else {
            c.requestFocusInWindow();
            if (c instanceof JTextComponent) {
                ((JTextComponent) c).selectAll();
            }
        }
    }
}
