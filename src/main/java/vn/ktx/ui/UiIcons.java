package vn.ktx.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.text.Normalizer;
import java.util.Locale;

/** Biểu tượng vector cùng nét, vẽ rõ ở các mức phóng đại và không lệ thuộc font emoji. */
public final class UiIcons implements Icon {
    private final String kind;
    private final int size;
    private UiIcons(String kind,int size) { this.kind=kind; this.size=size; }
    public static Icon page(String key) {
        String kind=switch(key) {
            case "overview" -> "dashboard";
            case "students" -> "students";
            case "rooms" -> "room";
            case "contract","contracts" -> "contract";
            case "services" -> "service";
            case "guests" -> "guests";
            case "requests" -> "requests";
            case "application" -> "application";
            case "transfer" -> "transfer";
            case "maintenance","repairs" -> "repair";
            case "assets" -> "asset";
            case "billing" -> "invoice";
            case "payments" -> "payment";
            case "utilities" -> "water";
            case "reports" -> "chart";
            case "notices" -> "bell";
            case "violations" -> "shield";
            case "reminders" -> "clock";
            case "accounts" -> "account";
            case "logout" -> "logout";
            default -> "document";
        };
        return new UiIcons(kind,20);
    }
    public static Icon tab(String title) {
        String label=Normalizer.normalize(title,Normalizer.Form.NFD).replaceAll("\\p{M}","").replace('đ','d').toLowerCase(Locale.ROOT);
        String kind;
        if(label.contains("doanh thu")) kind="chart";
        else if(label.contains("chuyen phong")) kind="transfer";
        else if(label.contains("dang ky")) kind="application";
        else if(label.contains("sua chua")) kind="repair";
        else if(label.contains("tai san")) kind="asset";
        else if(label.contains("hop dong")) kind="contract";
        else if(label.contains("thanh toan")) kind="payment";
        else if(label.contains("cong no")) kind="wallet";
        else if(label.contains("hoa don")) kind="invoice";
        else if(label.contains("vi pham")) kind="shield";
        else if(label.contains("thong bao")) kind="bell";
        else if(label.contains("khach")) kind="guests";
        else if(label.contains("nhat ky")) kind="clock";
        else if(label.contains("xe")) kind="vehicle";
        else if(label.contains("su dung")) kind="usage";
        else if(label.contains("dich vu")) kind="service";
        else kind="document";
        return new UiIcons(kind,18);
    }
    @Override public int getIconWidth() { return size; }
    @Override public int getIconHeight() { return size; }
    @Override public void paintIcon(Component component,Graphics graphics,int x,int y) {
        Graphics2D g=(Graphics2D)graphics.create();
        try {
            g.translate(x,y); g.scale(size/24.0,size/24.0);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            Color color=component.isEnabled()?component.getForeground():UiTheme.MUTED;
            if(component instanceof JTabbedPane tabs) color=tabs.getSelectedIndex()==tabs.indexOfTab(this)?UiTheme.BLUE:UiTheme.MUTED;
            g.setColor(color==null?UiTheme.INK:color); g.setStroke(new BasicStroke(1.7f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            switch(kind) {
                case "dashboard" -> { rect(g,3,3,7,7); rect(g,14,3,7,7); rect(g,3,14,7,7); rect(g,14,14,7,7); }
                case "students","guests" -> {
                    circle(g,9,7,3); path(g,3,21,3,18,5,14,13,14,15,18,15,21);
                    g.drawArc(14,4,6,6,-80,240); path(g,18,14,21,17,21,21);
                }
                case "room" -> { rect(g,5,3,14,18); line(g,2,21,22,21); line(g,9,3,9,21); circle(g,15,12,0.7); }
                case "contract" -> { document(g); path(g,8,16,10,18,15,13); line(g,8,8,15,8); }
                case "service" -> { rect(g,3,7,18,14); path(g,8,7,8,3,16,3,16,7); line(g,3,12,21,12); line(g,10,12,10,15); line(g,14,12,14,15); }
                case "requests" -> { rect(g,5,4,14,17); rect(g,9,2,6,4); line(g,9,11,15,11); path(g,9,16,11,18,16,13); }
                case "application" -> { document(g); line(g,8,9,15,9); line(g,12,13,12,19); line(g,9,16,15,16); }
                case "transfer" -> { path(g,3,7,20,7,16,3); line(g,20,7,16,11); path(g,21,17,4,17,8,13); line(g,4,17,8,21); }
                case "repair" -> { path(g,14,4,13,9,17,11,21,8,21,13,17,16,13,15,6,22,2,18,10,11,9,7,11,3,14,4); }
                case "asset" -> { path(g,3,7,12,3,21,7,21,18,12,22,3,18,3,7,12,11,21,7); line(g,12,11,12,22); line(g,7,5,16,9); }
                case "invoice" -> { path(g,5,2,19,2,19,22,16,20,13,22,10,20,7,22,5,21,5,2); line(g,8,7,16,7); line(g,8,12,16,12); line(g,8,16,13,16); }
                case "payment" -> { rect(g,2,5,20,15); line(g,2,10,22,10); line(g,6,15,10,15); }
                case "wallet" -> { rect(g,3,6,18,15); path(g,3,8,3,4,17,2,17,6); rect(g,14,11,8,6); circle(g,17,14,0.7); }
                case "water" -> { Path2D drop=new Path2D.Double(); drop.moveTo(12,2); drop.curveTo(10,6,4,11,4,15); drop.curveTo(4,24,20,24,20,15); drop.curveTo(20,11,14,6,12,2); g.draw(drop); g.drawArc(8,12,8,7,180,85); }
                case "chart" -> { path(g,3,3,3,21,22,21); rect(g,7,13,3,5); rect(g,13,9,3,9); rect(g,19,4,3,14); }
                case "bell" -> { path(g,4,17,6,14,6,9,8,5,12,3,16,5,18,9,18,14,20,17,4,17); g.drawArc(9,17,6,5,180,180); line(g,12,2,12,3); }
                case "shield" -> { path(g,12,2,21,6,20,14,17,19,12,22,7,19,4,14,3,6,12,2); line(g,12,7,12,13); circle(g,12,17,0.6); }
                case "clock" -> { circle(g,12,12,9); path(g,12,6,12,12,16,14); }
                case "account" -> { circle(g,12,7,4); path(g,4,22,4,19,7,15,17,15,20,19,20,22); }
                case "logout" -> { path(g,10,3,3,3,3,21,10,21); path(g,15,7,20,12,15,17); line(g,8,12,20,12); }
                case "vehicle" -> { path(g,3,12,5,6,18,6,21,12,21,19,18,19,18,16,6,16,6,19,3,19,3,12,21,12); circle(g,7,13.5,0.7); circle(g,17,13.5,0.7); }
                case "usage" -> { rect(g,5,4,14,17); rect(g,9,2,6,4); path(g,8,12,10,14,15,9); line(g,8,18,16,18); }
                default -> { document(g); line(g,8,10,16,10); line(g,8,14,16,14); line(g,8,18,13,18); }
            }
        } finally { g.dispose(); }
    }
    private static void document(Graphics2D g) { path(g,5,2,15,2,20,7,20,22,5,22,5,2); path(g,15,2,15,7,20,7); }
    private static void line(Graphics2D g,double x1,double y1,double x2,double y2) { g.draw(new Line2D.Double(x1,y1,x2,y2)); }
    private static void rect(Graphics2D g,double x,double y,double width,double height) { g.draw(new RoundRectangle2D.Double(x,y,width,height,2,2)); }
    private static void circle(Graphics2D g,double x,double y,double radius) { g.draw(new Ellipse2D.Double(x-radius,y-radius,radius*2,radius*2)); }
    private static void path(Graphics2D g,double... points) { Path2D p=new Path2D.Double(); p.moveTo(points[0],points[1]); for(int i=2;i<points.length;i+=2)p.lineTo(points[i],points[i+1]); g.draw(p); }
}
