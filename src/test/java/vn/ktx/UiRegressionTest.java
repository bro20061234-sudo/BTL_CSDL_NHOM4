package vn.ktx;

import vn.ktx.ui.*;
import vn.ktx.dao.AuthDao;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

/** Kiểm tra giao diện thực và xuất ảnh mà không mở cửa sổ lên màn hình người dùng. */
public final class UiRegressionTest {
    private static int checks;
    public static void main(String[] args) throws Exception {
        if(!"1".equals(System.getenv("KTX_RUN_INTEGRATION")) || !System.getenv().getOrDefault("KTX_DB_URL","").startsWith("jdbc:mysql://127.0.0.1:33317/"))
            throw new IllegalStateException("Cần CSDL thử nghiệm cổng 33317 và KTX_RUN_INTEGRATION=1.");
        var session=new AuthDao().login("testadmin","password123".toCharArray());
        MainFrame[] frame=new MainFrame[1]; JTable[] students=new JTable[1]; WorkspacePanel[] workspace=new WorkspacePanel[1];
        CountDownLatch loaded=new CountDownLatch(1);
        try {
            SwingUtilities.invokeAndWait(()-> {
                UiTheme.install(); frame[0]=new MainFrame(session); workspace[0]=(WorkspacePanel)frame[0].getContentPane();
                students[0]=find(workspace[0],JTable.class).get(0);
                students[0].addPropertyChangeListener("model",e->loaded.countDown());
                frame[0].refreshStudents();
                for(OverviewPanel overview:find(workspace[0],OverviewPanel.class)) overview.reload();
            });
            check(loaded.await(30,TimeUnit.SECONDS),"Tải dữ liệu sinh viên nền");
            SwingUtilities.invokeAndWait(()-> {
                JTable table=students[0];
                check(table.getColumnCount()==6,"Bảng chính hiển thị 6 cột dễ đọc");
                check(table.getModel().getColumnCount()==9,"Hồ sơ đầy đủ vẫn được giữ trong model");
                check(table.getTableHeader().getParent()!=null,"Tiêu đề cột được hiển thị trong vùng cuộn");
                check(table.getRowHeight()>=44,"Chiều cao dòng dễ đọc");
                check(table.getRowCount()==4,"Nạp đầy đủ 4 hồ sơ thử nghiệm");
                JTextField search=find(workspace[0],JTextField.class).get(0);
                search.setText("SV002"); check(table.getRowCount()==1,"Tìm mã SV ngay khi nhập");
                check("SV002".equals(table.getValueAt(0,0)),"Kết quả đúng sau khi ẩn cột");
                search.setText("");
                JComboBox<?> room=find(workspace[0],JComboBox.class).get(0);
                room.setSelectedItem("P101"); check(table.getRowCount()==1,"Lọc phòng đúng dữ liệu");
                room.setSelectedIndex(0);
                table.getRowSorter().toggleSortOrder(1); table.setRowSelectionInterval(0,0);
                check(table.convertRowIndexToModel(0)>=0,"Chọn dòng sau khi sắp xếp");
                check(workspace[0].pageKeys().size()==14,"Menu quản trị có đầy đủ 14 trang");
                for(String page:workspace[0].pageKeys()) workspace[0].showPage(page);
                check(workspace[0].pageKeys().contains("accounts"),"Menu quản trị có tài khoản");
                workspace[0].showPage("students");
                ReadOnlyTableModel amounts=new ReadOnlyTableModel(new Object[][]{{new java.math.BigDecimal("100000")},{new java.math.BigDecimal("9000")}},new String[]{"Tiền"});
                JTable sortTable=new JTable(amounts); sortTable.setAutoCreateRowSorter(true); sortTable.getRowSorter().toggleSortOrder(0);
                check(new java.math.BigDecimal("9000").equals(sortTable.getValueAt(0,0)),"Sắp xếp tiền theo số, không theo chữ");
                UiTheme.CellRenderer renderer=new UiTheme.CellRenderer();
                JLabel label=(JLabel)renderer.getTableCellRendererComponent(table,"CHO_DUYET",false,false,0,0);
                check("Chờ duyệt".equals(label.getText()),"Trạng thái được dịch tiếng Việt");
                UiTheme.decorate(new JButton()); check(true,"Nút chỉ có biểu tượng được trang trí an toàn");
                MainFrame staffFrame=new MainFrame(new vn.ktx.model.UserSession("staff","STAFF",null));
                try { check(!((WorkspacePanel)staffFrame.getContentPane()).pageKeys().contains("accounts"),"Nhân viên không có menu quản lý tài khoản"); }
                finally { staffFrame.dispose(); }
            });
            // Đợi worker số liệu tổng quan; không chặn EDT.
            for(int attempt=0;attempt<100;attempt++) {
                boolean[] ready={false};
                SwingUtilities.invokeAndWait(()->ready[0]=find(workspace[0],JLabel.class).stream().anyMatch(l->l.getText()!=null && l.getText().startsWith("Số liệu từ CSDL")));
                if(ready[0]) break;
                Thread.sleep(50);
            }
            SwingUtilities.invokeAndWait(()-> {
                try {
                    workspace[0].setSize(1440,880); layout(workspace[0]);
                    BufferedImage image=new BufferedImage(1440,880,BufferedImage.TYPE_INT_RGB); Graphics2D g=image.createGraphics(); workspace[0].printAll(g); g.dispose();
                    Files.createDirectories(Path.of("output/ui")); javax.imageio.ImageIO.write(image,"png",Path.of("output/ui/quan-ly-sinh-vien.png").toFile());
                    workspace[0].setSize(1180,700); layout(workspace[0]);
                    check(students[0].getWidth()>700,"Bảng vẫn có diện tích đọc ở kích thước tối thiểu");
                } catch(Exception e) { throw new RuntimeException(e); }
            });
            System.out.println("PASS: "+checks+" UI checks; screenshot: output/ui/quan-ly-sinh-vien.png");
        } finally { SwingUtilities.invokeAndWait(()-> { if(frame[0]!=null) frame[0].dispose(); }); }
    }
    private static void check(boolean okay,String name) { if(!okay) throw new AssertionError(name); checks++; }
    private static <T extends Component> List<T> find(Component parent,Class<T> type) {
        List<T> result=new ArrayList<>(); if(type.isInstance(parent)) result.add(type.cast(parent));
        if(parent instanceof Container container) for(Component child:container.getComponents()) result.addAll(find(child,type)); return result;
    }
    private static void layout(Container parent) { parent.doLayout(); for(Component child:parent.getComponents()) if(child instanceof Container container) layout(container); }
}
