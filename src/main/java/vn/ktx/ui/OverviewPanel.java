package vn.ktx.ui;

import vn.ktx.dao.KtxDao;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/** Số liệu thật được tải ngoài luồng giao diện. */
public final class OverviewPanel extends JPanel {
    private final JLabel[] values={UiTheme.label("—",30,true),UiTheme.label("—",30,true),UiTheme.label("—",30,true)};
    private final JLabel message=UiTheme.label("",12,false);
    private boolean loading;
    public OverviewPanel(boolean compact) {
        super(new BorderLayout(0,16)); setOpaque(false);
        JPanel stats=new JPanel(new GridLayout(1,3,16,0)); stats.setOpaque(false);
        String[] labels={"Sinh viên đang ở","Phòng còn chỗ","Yêu cầu chờ duyệt"};
        Color[] colors={UiTheme.BLUE,UiTheme.GREEN,new Color(190,117,18)};
        for(int i=0;i<3;i++) {
            JPanel card=new JPanel(new BorderLayout(10,8)); card.setBorder(new CompoundBorder(new LineBorder(UiTheme.LINE),new EmptyBorder(18,22,18,22)));
            values[i].setForeground(colors[i]); card.add(values[i],BorderLayout.NORTH); JLabel text=UiTheme.label(labels[i],14,false); text.setForeground(UiTheme.MUTED); card.add(text,BorderLayout.SOUTH); stats.add(card);
        }
        add(stats,BorderLayout.NORTH);
        if(!compact) {
            JPanel guide=new JPanel(); guide.setLayout(new BoxLayout(guide,BoxLayout.Y_AXIS)); guide.setBorder(new EmptyBorder(22,22,22,22));
            guide.add(UiTheme.label("Bắt đầu công việc",20,true)); guide.add(Box.createVerticalStrut(18));
            String[][] steps={{"01  Quản lý cư dân","Mở Sinh viên hoặc Phòng ở để tra cứu, thêm hồ sơ và xếp phòng."},
                    {"02  Xử lý yêu cầu","Mở Đăng ký & chuyển phòng để duyệt yêu cầu; theo dõi báo hỏng tại Sửa chữa & tài sản."},
                    {"03  Thu phí hằng tháng","Nhập Điện nước, kiểm tra Hóa đơn & thanh toán, sau đó ghi nhận khoản thu."},
                    {"04  Theo dõi hạn và thông báo","Xem Nhắc hạn; gửi thông báo chung hoặc riêng cho từng phòng."}};
            for(String[] step:steps) { guide.add(UiTheme.label(step[0],15,true)); guide.add(Box.createVerticalStrut(7)); JLabel detail=UiTheme.label(step[1],14,false); detail.setForeground(UiTheme.MUTED); guide.add(detail); guide.add(Box.createVerticalStrut(24)); }
            JPanel center=new JPanel(new BorderLayout()); center.setOpaque(false); center.add(guide,BorderLayout.NORTH); add(center,BorderLayout.CENTER);
        }
        JPanel footer=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0)); footer.setOpaque(false); message.setForeground(UiTheme.MUTED); footer.add(message);
        if(!compact) { JButton refresh=new JButton("Làm mới số liệu"); refresh.addActionListener(e->reload()); footer.add(Box.createHorizontalStrut(14)); footer.add(refresh); }
        add(footer,BorderLayout.SOUTH);
        addHierarchyListener(e->{ if((e.getChangeFlags()&java.awt.event.HierarchyEvent.SHOWING_CHANGED)!=0 && isShowing()) reload(); });
    }
    public void reload() {
        if(loading) return; loading=true; message.setText("Đang cập nhật số liệu…");
        new SwingWorker<int[],Void>() {
            @Override protected int[] doInBackground() throws Exception { return new KtxDao().getOverview(); }
            @Override protected void done() {
                loading=false;
                try { int[] counts=get(); for(int i=0;i<3;i++) values[i].setText(Integer.toString(counts[i])); message.setText("Số liệu từ CSDL • cập nhật "+java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))); }
                catch(Exception e) { for(JLabel value:values)value.setText("—"); message.setText("Chưa tải được số liệu. Kiểm tra kết nối CSDL rồi làm mới."); }
            }
        }.execute();
    }
}
