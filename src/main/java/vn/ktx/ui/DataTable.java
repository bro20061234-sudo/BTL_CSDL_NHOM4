package vn.ktx.ui;

import javax.swing.*;
import java.awt.*;

/** Bảng có thông báo rõ ràng khi đang tải hoặc không có kết quả. */
public final class DataTable extends JTable {
    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if(getRowCount()!=0) return;
        Graphics2D graphics=(Graphics2D)g.create();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        boolean loading=Boolean.TRUE.equals(getClientProperty("loading"));
        String text=loading?"Đang tải dữ liệu…":getClientProperty("emptyMessage") instanceof String message?message:"Chưa có dữ liệu để hiển thị";
        graphics.setFont(UiTheme.FONT.deriveFont(Font.BOLD,16)); graphics.setColor(UiTheme.MUTED);
        int y=Math.max(60,getHeight()/2-10); graphics.drawString(text,Math.max(16,(getWidth()-graphics.getFontMetrics().stringWidth(text))/2),y);
        if(!loading) { text="Thử làm mới hoặc thay đổi bộ lọc."; graphics.setFont(UiTheme.FONT.deriveFont(14f)); graphics.drawString(text,Math.max(16,(getWidth()-graphics.getFontMetrics().stringWidth(text))/2),y+28); }
        graphics.dispose();
    }
}
