package common.custom;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/**
 *
 * @author kamyshev.a
 */
public final class CheckBoxList extends JList {

    protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

    public CheckBoxList() {
        setCellRenderer(new CellRenderer());
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point point = e.getPoint();
                if (e.getButton() == MouseEvent.BUTTON1 && point.x < 15) {
                    int index = locationToIndex(e.getPoint());                   
                    if (index != -1) {
                        JCheckBox checkbox = (JCheckBox) getModel().getElementAt(index);
                        checkbox.setSelected(!checkbox.isSelected());
                        repaint();
                    }
                }
            }
        }
        );
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    protected class CellRenderer implements ListCellRenderer {

        @Override
        public Component getListCellRendererComponent(
                JList list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JCheckBox checkbox = (JCheckBox) value;
            checkbox.setPreferredSize(new Dimension(0, 30));
            checkbox.setBackground(isSelected
                    ? getSelectionBackground() : getBackground());
            checkbox.setForeground(isSelected
                    ? getSelectionForeground() : getForeground());
            checkbox.setEnabled(isEnabled());
            checkbox.setFont(getFont());
            checkbox.setFocusPainted(false);
            checkbox.setBorderPainted(true);
            checkbox.setBorder(isSelected
                    ? UIManager.getBorder(
                            "List.focusCellHighlightBorder") : noFocusBorder);
            return checkbox;
        }
    }
}
