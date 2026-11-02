package vn.ktx;

import vn.ktx.config.Database;
import vn.ktx.dao.*;
import vn.ktx.model.*;
import vn.ktx.ui.*;
import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.List;

/** Chạy sau các kiểm tra nghiệp vụ trên CSDL thử nghiệm, không nhập giờ vào giao diện. */
public final class ParkingFlowTest {
    private static int checks;
    public static void main(String[] args) throws Exception {
        if(!"1".equals(System.getenv("KTX_RUN_INTEGRATION")) || !System.getenv().getOrDefault("KTX_DB_URL","").startsWith("jdbc:mysql://127.0.0.1:33317/")) throw new IllegalStateException("Chỉ chạy trên cổng CSDL thử nghiệm 33317.");
        MutableClock clock=new MutableClock("2026-11-07T17:00:00");
        ParkingService service=new ParkingService(new ParkingDao(),clock);
        ParkingService.Entry casual=service.enter("59A-123.45");
        check(!casual.monthly(),"Nhận diện xe chưa có vé tháng");
        check(casual.entered().equals(clock.local()),"Giờ vào lấy tự động từ đồng hồ");
        reject(()->service.enter("59a12345"),"Chặn gửi trùng biển số khác dấu chấm/gạch");
        ParkingService.Entry monthly=service.enter("18b254321");
        check(monthly.monthly(),"Nhận diện vé tháng theo biển số chuẩn hóa");
        check(service.active().size()==2,"Xe tháng và xe lượt cùng trong danh sách");
        reject(()->new ParkingService(new ParkingDao(),clock).enter("18B2-54321"),"CSDL chặn xe tháng vào trùng từ cửa sổ khác");
        clock.set("2026-11-07T19:00:00");
        equalMoney("8000",service.active().stream().filter(e->e.id().equals(casual.id())).findFirst().orElseThrow().estimate(service.now()),"Phí hiện tại đổi khi qua 18h");
        ParkingService.Receipt receipt=service.leave(casual.id());
        equalMoney("8000",receipt.amount(),"Lấy xe tính tiền từ giờ thực tế");
        check(receipt.exited().equals(clock.local()),"Giờ ra ghi tại lúc bấm lấy xe");
        check(service.active().stream().noneMatch(e->e.id().equals(casual.id())),"Xe ra không còn trong danh sách");
        reject(()->service.leave(casual.id()),"Không thu lại vé lượt đã lấy");
        equalMoney("0",service.leave(monthly.id()).amount(),"Vé tháng còn lượt miễn phí");
        reject(()->service.leave(monthly.id()),"Không lấy cùng lượt vé tháng hai lần");
        try(Connection c=Database.getConnection()) {
            check(ExtensionDao.query(c,"SELECT ThoiGianLay FROM NhatKyGuiXe WHERE MaLuot=?",Long.parseLong(monthly.id().substring(2))).get(0)[0]!=null,"Nhật ký vé tháng lưu thời gian lấy");
        }
        for(int i=0;i<3;i++) {
            clock.set("2026-11-08T0"+(7+i)+":00:00");ParkingService.Entry entry=service.enter("18B2-54321");
            clock.set("2026-11-08T"+String.format("%02d",8+i)+":00:00");
            equalMoney(i<2?"0":"3000",service.leave(entry.id()).amount(),"Phí vé tháng lượt "+(i+1));
        }
        clock.set("2026-11-09T10:00:00");ParkingService.Entry immediate=service.enter("SAME-TIME");
        equalMoney("3000",service.leave(immediate.id()).amount(),"Lấy ngay vẫn tính một khung, không cần nhập giờ");
        ParkingService.Entry backward=service.enter("BACK-TIME");clock.set("2026-11-09T09:00:00");
        reject(()->service.leave(backward.id()),"Chặn giờ hệ thống lùi trước giờ gửi");
        check(service.active().stream().anyMatch(e->e.id().equals(backward.id())),"Không làm mất xe khi lỗi đồng hồ");
        clock.set("2026-11-09T11:00:00");service.leave(backward.id());
        ExtensionDao admin=new ExtensionDao(new AuthDao().login("testadmin","password123".toCharArray()));
        ExtensionDao student=new ExtensionDao(new AuthDao().login("student1","password123".toCharArray()));
        check(admin.assets("NEW1").size()==3,"Lọc tài sản phòng tiêu chuẩn");
        check(admin.assets("NEW2").size()==5,"Lọc tài sản phòng cao cấp");
        check(admin.assets("NEW1").stream().allMatch(r->"NEW1".equals(r[1])),"Không trộn tài sản giữa phòng");
        check(admin.assets(null).isEmpty(),"Chưa chọn phòng không liệt kê tất cả");
        check(student.assets("NEW1").isEmpty(),"Sinh viên không xem được tài sản phòng khác");
        check(student.assetRooms().equals(List.of("T002")),"Bộ chọn phòng sinh viên chỉ có phòng đang ở");
        ParkingPanel[] panel=new ParkingPanel[1];JTable[] table=new JTable[1];
        SwingUtilities.invokeAndWait(()->{
            UiTheme.install();panel[0]=new ParkingPanel(service);table[0]=find(panel[0],JTable.class).get(0);
            check(find(panel[0],JSpinner.class).isEmpty(),"Giao diện không có ô giờ vào/ra");
            find(panel[0],JTextField.class).get(0).setText("UI-59A");button(panel[0],"Gửi xe").doClick();
        });
        await(()->table[0].getRowCount()==1);
        SwingUtilities.invokeAndWait(()-> {
            check("UI-59A".equals(table[0].getValueAt(0,0)),"Nút gửi xe đưa xe vào bảng");
            check(!table[0].isCellEditable(0,5),"Không tự sửa phí trong bảng");
            table[0].getRowSorter().toggleSortOrder(1);table[0].setRowSelectionInterval(0,0);
            clock.set("2026-11-09T19:00:00");button(panel[0],"Lấy xe / tính tiền").doClick();
        });
        await(()->table[0].getRowCount()==0);
        SwingUtilities.invokeAndWait(()->check(find(panel[0],JLabel.class).stream().anyMatch(l->l.getText()!=null && l.getText().contains("8.000")),"Nút lấy xe hiển thị tiền thực thu"));
        JPanel[] assets=new JPanel[1];JTable[] assetTable=new JTable[1];JComboBox<?>[] rooms=new JComboBox<?>[1];
        SwingUtilities.invokeAndWait(()-> {
            assets[0]=new ExtensionPanel(new UserSession("testadmin","ADMIN",null),false).modulePanel(ExtensionDao.Module.ASSET);
            assetTable[0]=find(assets[0],JTable.class).get(0);JComboBox<String> room=cast(find(assets[0],JComboBox.class).get(0));rooms[0]=room;
            room.addItem("NEW1");room.addItem("NEW2");room.setSelectedItem("NEW1");
        });
        await(()->assetTable[0].getRowCount()==3);
        SwingUtilities.invokeAndWait(()->{ check("NEW1".equals(assetTable[0].getValueAt(0,1)),"Đổi phòng nạp đúng bảng tài sản");rooms[0].setSelectedItem("NEW2"); });
        await(()->assetTable[0].getRowCount()==5);
        SwingUtilities.invokeAndWait(()->check("NEW2".equals(assetTable[0].getValueAt(0,1)),"Bảng chỉ hiển thị tài sản phòng vừa chọn"));
        System.out.println("PASS: "+checks+" realtime parking and asset filter checks.");
    }
    @SuppressWarnings("unchecked") private static JComboBox<String> cast(JComboBox<?> value) { return (JComboBox<String>)value; }
    private static JButton button(Component root,String text) { return find(root,JButton.class).stream().filter(b->text.equals(b.getText())).findFirst().orElseThrow(); }
    private static void await(Condition condition) throws Exception { for(int i=0;i<200;i++) { boolean[] okay={false};SwingUtilities.invokeAndWait(()->okay[0]=condition.test());if(okay[0])return;Thread.sleep(50); }throw new AssertionError("Không cập nhật giao diện sau 10 giây"); }
    private static <T extends Component> List<T> find(Component root,Class<T> type) { List<T> result=new ArrayList<>();if(type.isInstance(root))result.add(type.cast(root));if(root instanceof Container parent)for(Component child:parent.getComponents())result.addAll(find(child,type));return result; }
    private static void equalMoney(String amount,BigDecimal actual,String message) { check(new BigDecimal(amount).compareTo(actual)==0,message); }
    private static void check(boolean okay,String message) { if(!okay)throw new AssertionError(message);checks++; }
    private static void reject(Action action,String message) throws Exception { try { action.run(); } catch(SQLException expected) { checks++;return; }throw new AssertionError(message); }
    @FunctionalInterface private interface Action { void run() throws Exception; }
    @FunctionalInterface private interface Condition { boolean test(); }
    private static final class MutableClock extends Clock {
        private volatile Instant instant;
        MutableClock(String value) { set(value); }
        void set(String value) { instant=LocalDateTime.parse(value).atZone(getZone()).toInstant(); }
        LocalDateTime local() { return LocalDateTime.ofInstant(instant,getZone()); }
        public ZoneId getZone() { return ZoneId.of("Asia/Ho_Chi_Minh"); }
        public Clock withZone(ZoneId zone) { return Clock.fixed(instant,zone); }
        public Instant instant() { return instant; }
    }
}
