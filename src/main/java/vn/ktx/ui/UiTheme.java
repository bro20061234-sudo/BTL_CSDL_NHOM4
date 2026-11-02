package vn.ktx.ui;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

/** Giao diện dùng chung, không phụ thuộc thư viện giao diện ngoài. */
public final class UiTheme {
    public static final Color CANVAS=new Color(245,247,251), WHITE=Color.WHITE;
    public static final Color INK=new Color(23,38,69), MUTED=new Color(103,117,140);
    public static final Color BLUE=new Color(30,102,241), LINE=new Color(224,230,240);
    public static final Color SELECTED=new Color(232,241,255), GREEN=new Color(22,128,85), RED=new Color(195,47,58);
    public static final Font FONT=new Font("Segoe UI",Font.PLAIN,15);
    private UiTheme() { }
    public static void install() {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch(Exception e) { throw new IllegalStateException(e); }
        for(Object key:java.util.Collections.list(UIManager.getDefaults().keys()))
            if(key.toString().endsWith(".font")) UIManager.put(key,new javax.swing.plaf.FontUIResource(FONT));
        UIManager.put("Panel.background",WHITE); UIManager.put("Label.foreground",INK);
        UIManager.put("TextField.foreground",INK); UIManager.put("TextField.background",WHITE);
        UIManager.put("TextField.border",new CompoundBorder(new LineBorder(LINE),new EmptyBorder(8,10,8,10)));
        UIManager.put("PasswordField.border",UIManager.get("TextField.border"));
        UIManager.put("ComboBox.background",WHITE); UIManager.put("ComboBox.foreground",INK);
        UIManager.put("Table.selectionBackground",SELECTED); UIManager.put("Table.selectionForeground",INK);
        UIManager.put("Table.gridColor",LINE); UIManager.put("TableHeader.background",CANVAS);
        UIManager.put("TableHeader.foreground",MUTED); UIManager.put("TableHeader.font",FONT.deriveFont(Font.BOLD,13));
        UIManager.put("TabbedPane.selected",WHITE); UIManager.put("TabbedPane.background",CANVAS);
        UIManager.put("TabbedPane.contentAreaColor",WHITE); UIManager.put("TabbedPane.tabInsets",new Insets(10,16,10,16));
        UIManager.put("OptionPane.okButtonText","Lưu"); UIManager.put("OptionPane.cancelButtonText","Hủy");
        UIManager.put("OptionPane.yesButtonText","Đồng ý"); UIManager.put("OptionPane.noButtonText","Hủy");
        UIManager.put("ScrollPane.border",new LineBorder(LINE));
        UIManager.put("ButtonUI",FlatButtonUI.class.getName());
        UIManager.put("ScrollBarUI",QuietScrollBarUI.class.getName());
        UIManager.put("ScrollBar.width",10);
    }
    public static JLabel label(String text,int size,boolean bold) {
        JLabel label=new JLabel(text); label.setFont(FONT.deriveFont(bold?Font.BOLD:Font.PLAIN,(float)size)); label.setForeground(INK); return label;
    }
    public static JButton primary(String text) { JButton b=new JButton(text); b.putClientProperty("kind","primary"); return b; }
    public static void styleButton(JButton b) {
        if(b.getClientProperty("kind")==null) {
            String text=java.util.Objects.toString(b.getText(),"");
            if(text.startsWith("Thêm") || text.startsWith("Tạo") || text.startsWith("Ghi nhận") || text.startsWith("Đăng ký") || text.startsWith("Gửi đơn") || text.startsWith("Lập ") || text.equals("Báo hỏng") || text.equals("Yêu cầu chuyển phòng") || text.equals("Duyệt")) b.putClientProperty("kind","primary");
            else if(text.startsWith("Xóa") || text.equals("Từ chối") || text.equals("Thanh lý")) b.putClientProperty("kind","danger");
        }
        b.setFont(FONT.deriveFont("nav".equals(b.getClientProperty("kind"))?Font.PLAIN:Font.BOLD,14)); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(10,14,10,14)); b.setFocusPainted(false); b.setOpaque(false); b.setContentAreaFilled(false);
        b.setUI(new FlatButtonUI());
    }
    public static void decorate(Component component) {
        if(component instanceof JButton b) styleButton(b);
        else if(component instanceof JTabbedPane tabs) {
            for(int i=0;i<tabs.getTabCount();i++) if(tabs.getIconAt(i)==null) tabs.setIconAt(i,UiIcons.tab(tabs.getTitleAt(i)));
        }
        else if(component instanceof JTable t) styleTable(t);
        else if(component instanceof JTextArea a) { a.setLineWrap(true); a.setWrapStyleWord(true); }
        else if(component instanceof JComboBox<?> c) { if(!(c.getParent() instanceof MonthPicker)) c.setPreferredSize(new Dimension(Math.max(140,c.getPreferredSize().width),38)); c.setMaximumRowCount(12); }
        if(component instanceof Container container) for(Component c:container.getComponents()) decorate(c);
    }
    public static void styleTable(JTable t) {
        t.setFont(FONT.deriveFont(16f)); t.setForeground(INK); t.setRowHeight(48); t.setShowVerticalLines(false); t.setShowHorizontalLines(true);
        t.setGridColor(LINE); t.setIntercellSpacing(new Dimension(0,1)); t.setSelectionBackground(SELECTED); t.setSelectionForeground(INK);
        t.setFillsViewportHeight(true); t.setBackground(WHITE); t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.getTableHeader().setFont(FONT.deriveFont(Font.BOLD,14)); t.getTableHeader().setPreferredSize(new Dimension(0,44));
        t.getTableHeader().setBackground(CANVAS); t.getTableHeader().setReorderingAllowed(false);
        t.setDefaultRenderer(Object.class,new CellRenderer());
        t.setDefaultRenderer(Number.class,new CellRenderer());
        t.setDefaultRenderer(java.util.Date.class,new CellRenderer());
        for(int i=0;i<t.getColumnCount();i++)
            if(t.getColumnName(i).equals("Trạng thái")) { t.getColumnModel().getColumn(i).setCellRenderer(new StatusRenderer()); t.getColumnModel().getColumn(i).setPreferredWidth(150); }
        t.putClientProperty("terminateEditOnFocusLost",Boolean.TRUE);
        if(t.getParent() instanceof JViewport viewport && viewport.getParent() instanceof JScrollPane scroll) scroll.setColumnHeaderView(t.getTableHeader());
    }
    public static String status(String raw) {
        return Map.ofEntries(Map.entry("CHO_DUYET","Chờ duyệt"),Map.entry("DA_DUYET","Đã duyệt"),Map.entry("TU_CHOI","Từ chối"),
                Map.entry("HIEU_LUC","Hiệu lực"),Map.entry("DA_THANH_LY","Đã thanh lý"),Map.entry("TIEP_NHAN","Tiếp nhận"),
                Map.entry("DANG_XU_LY","Đang xử lý"),Map.entry("HOAN_THANH","Hoàn thành"),Map.entry("ADMIN","Quản trị viên"),
                Map.entry("STAFF","Nhân viên"),Map.entry("STUDENT","Sinh viên"),Map.entry("FULL","Thanh toán toàn bộ"),Map.entry("PARTIAL","Thanh toán một phần"),
                Map.entry("NHAC_NHO","Nhắc nhở"),Map.entry("HANH_CHINH","Phạt hành chính"),Map.entry("DINH_CHI","Đình chỉ lưu trú")).getOrDefault(raw,raw);
    }
    public static class CellRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table,Object value,boolean selected,boolean focus,int row,int column) {
            super.getTableCellRendererComponent(table,value,selected,false,row,column);
            String raw=value==null?"":value.toString(); String display=status(raw);
            if(value instanceof java.sql.Date d) display=d.toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            else if(value instanceof java.sql.Timestamp d) display=d.toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            else if(value instanceof java.time.LocalDate d) display=d.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            else if(value instanceof java.math.BigDecimal n) display=NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(n);
            if(table.getColumnName(column).startsWith("Tháng")) {
                if(value instanceof java.sql.Date d) display=d.toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("MM/yyyy"));
                else if(value instanceof java.time.LocalDate d) display=d.format(java.time.format.DateTimeFormatter.ofPattern("MM/yyyy"));
            }
            setText(display); setToolTipText(display); setBorder(new EmptyBorder(0,12,0,12));
            setBackground(selected?SELECTED:row%2==0?WHITE:new Color(250,251,253)); setForeground(INK);
            if(!raw.equals(status(raw))) {
                setForeground(raw.equals("TU_CHOI") || raw.equals("DINH_CHI")?RED:raw.equals("CHO_DUYET") || raw.equals("DANG_XU_LY")?new Color(160,105,15):GREEN);
                setFont(FONT.deriveFont(Font.BOLD,13));
            } else setFont(FONT.deriveFont(16f));
            setHorizontalAlignment(value instanceof Number?SwingConstants.RIGHT:SwingConstants.LEFT);
            return this;
        }
    }
    private static final class StatusRenderer extends DefaultTableCellRenderer {
        private Color pillBackground=WHITE,rowBackground=WHITE;
        @Override public Component getTableCellRendererComponent(JTable table,Object value,boolean selected,boolean focus,int row,int column) {
            super.getTableCellRendererComponent(table,value,selected,false,row,column);
            String raw=value==null?"":value.toString(),text=status(raw);
            boolean warning=java.util.Set.of("CHO_DUYET","DANG_XU_LY","TIEP_NHAN","Chờ xếp phòng").contains(raw);
            boolean rejected=raw.equals("TU_CHOI") || raw.equals("DINH_CHI") || raw.equals("Đình chỉ lưu trú"),closed=raw.equals("DA_THANH_LY");
            Color foreground=rejected?RED:closed?MUTED:warning?new Color(151,98,14):GREEN;
            pillBackground=rejected?new Color(255,235,238):closed?CANVAS:warning?new Color(255,244,215):new Color(224,247,234);
            rowBackground=selected?SELECTED:row%2==0?WHITE:new Color(250,251,253);
            setText("●  "+text); setFont(FONT.deriveFont(Font.BOLD,13)); setForeground(foreground); setBorder(new EmptyBorder(0,20,0,8)); setOpaque(false); setToolTipText(text); return this;
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D graphics=(Graphics2D)g.create(); graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(rowBackground); graphics.fillRect(0,0,getWidth(),getHeight());
            graphics.setColor(pillBackground); int width=Math.min(getWidth()-16,getFontMetrics(getFont()).stringWidth(getText())+24);
            graphics.fillRoundRect(8,(getHeight()-30)/2,width,30,22,22); graphics.dispose(); super.paintComponent(g);
        }
    }
    public static class FlatButtonUI extends BasicButtonUI {
        public static javax.swing.plaf.ComponentUI createUI(JComponent c) { return new FlatButtonUI(); }
        @Override public void installUI(JComponent c) {
            super.installUI(c); AbstractButton b=(AbstractButton)c;
            b.setOpaque(false); b.setBorder(new EmptyBorder(10,14,10,14)); b.setRolloverEnabled(true); b.setFocusPainted(false);
        }
        @Override public void paint(Graphics g,JComponent c) {
            AbstractButton b=(AbstractButton)c; Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            String kind=String.valueOf(b.getClientProperty("kind")); boolean primary="primary".equals(kind),nav="nav".equals(kind);
            boolean active=Boolean.TRUE.equals(b.getClientProperty("active"));
            Color fill=primary?BLUE:nav && active?SELECTED:WHITE;
            if(b.getModel().isRollover() || b.getModel().isPressed()) fill=primary?BLUE.darker():SELECTED;
            if(!b.isEnabled()) fill=CANVAS;
            g2.setColor(fill); g2.fillRoundRect(0,0,c.getWidth()-1,c.getHeight()-1,10,10);
            if(!nav) { g2.setColor(primary?BLUE:LINE); g2.drawRoundRect(0,0,c.getWidth()-1,c.getHeight()-1,10,10); }
            if(b.hasFocus()) { g2.setColor(BLUE); g2.drawRoundRect(2,2,c.getWidth()-5,c.getHeight()-5,8,8); }
            b.setForeground(!b.isEnabled()?MUTED:primary?WHITE:"danger".equals(kind)?RED:active?BLUE:INK);
            g2.dispose(); super.paint(g,c);
        }
    }
    public static class QuietScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        public static javax.swing.plaf.ComponentUI createUI(JComponent c) { return new QuietScrollBarUI(); }
        @Override protected JButton createDecreaseButton(int orientation) { return zeroButton(); }
        @Override protected JButton createIncreaseButton(int orientation) { return zeroButton(); }
        private JButton zeroButton() { JButton b=new JButton(); b.setPreferredSize(new Dimension(0,0)); b.setMinimumSize(new Dimension(0,0)); b.setMaximumSize(new Dimension(0,0)); return b; }
        @Override protected void paintTrack(Graphics g,JComponent c,Rectangle r) { g.setColor(WHITE); g.fillRect(r.x,r.y,r.width,r.height); }
        @Override protected void paintThumb(Graphics g,JComponent c,Rectangle r) {
            Graphics2D graphics=(Graphics2D)g.create(); graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(205,215,230)); graphics.fillRoundRect(r.x+2,r.y+2,Math.max(4,r.width-4),Math.max(4,r.height-4),8,8); graphics.dispose();
        }
    }
}
