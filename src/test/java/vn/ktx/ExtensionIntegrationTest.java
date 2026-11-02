package vn.ktx;

import vn.ktx.config.Database;
import vn.ktx.dao.*;
import vn.ktx.dao.ExtensionDao.Module;
import vn.ktx.model.UserSession;
import java.sql.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.*;

/** Chạy trên CSDL thử nghiệm riêng đã import hai script SQL, không chạy trên dữ liệu thật. */
public final class ExtensionIntegrationTest {
    private static int checks;
    public static void main(String[] args) throws Exception {
        if (!"1".equals(System.getenv("KTX_RUN_INTEGRATION")) ||
                !System.getenv().getOrDefault("KTX_DB_URL", "").startsWith("jdbc:mysql://127.0.0.1:33317/"))
            throw new IllegalStateException("Chỉ chạy với CSDL thử nghiệm cổng 33317 và KTX_RUN_INTEGRATION=1.");
        AuthDao auth=new AuthDao();
        check(auth.needsSetup(),"CSDL thử nghiệm chưa có tài khoản");
        auth.setup("testadmin","password123".toCharArray());
        UserSession admin=auth.login("testadmin","password123".toCharArray());
        check(admin.isAdmin(),"Đăng nhập quản trị viên");
        reject(()->auth.setup("otheradmin","password123".toCharArray()),"Không khởi tạo lại quản trị viên");
        reject(()->auth.login("testadmin","wrongpassword".toCharArray()),"Sai mật khẩu");
        auth.create(admin,"student1","password123".toCharArray(),"STUDENT","SV001");
        auth.create(admin,"student2","password123".toCharArray(),"STUDENT","SV002");
        auth.create(admin,"staff","password123".toCharArray(),"STAFF",null);
        UserSession student=auth.login("student1","password123".toCharArray());
        ExtensionDao staff=new ExtensionDao(admin),self=new ExtensionDao(student);
        reject(()->auth.create(student,"bad","password123".toCharArray(),"ADMIN",null),"Sinh viên không tạo tài khoản");
        reject(()->new ExtensionDao(auth.login("staff","password123".toCharArray())).accounts(),"Nhân viên không quản lý tài khoản");
        String hash=AuthDao.hash("password123".toCharArray());
        check(AuthDao.verify("password123".toCharArray(),hash) && !AuthDao.verify("wrong".toCharArray(),hash),"PBKDF2 xác thực");
        check(!hash.equals(AuthDao.hash("password123".toCharArray())),"Salt ngẫu nhiên");
        execute("INSERT INTO Phong VALUES ('T001','T01','Test',900000,1),('T002','T02','Test',900000,2)");
        execute("INSERT INTO SinhVien VALUES ('TESTSV','999999999991','Test Student','2003-01-01','TEST','Test',NULL,'2026-10-01')");
        staff.create(Module.APPLICATION,map("MaSV","TESTSV","LoaiPhong","Test","GhiChu","Đăng ký"));
        int application=last(staff,Module.APPLICATION);
        check(staff.applicationRooms(application).size()==2,"Chỉ gợi ý phòng đúng loại còn chỗ");
        staff.transition(Module.APPLICATION,application,"APPROVE","T001");
        check("T001".equals(scalar("SELECT MaPhong FROM SinhVien WHERE MaSV='TESTSV'")),"Duyệt đơn xếp phòng");
        reject(()->staff.transition(Module.APPLICATION,application,"APPROVE","T001"),"Không duyệt hai lần");
        reject(()->staff.applicationRooms(application),"Không mở xếp phòng cho đơn đã xử lý");
        staff.create(Module.CONTRACT,map("MaSV","TESTSV","MaPhong","T001","NgayBatDau","2026-10-01","NgayKetThuc","2026-12-31","TienCoc","500000"));
        int contract=last(staff,Module.CONTRACT);
        staff.transition(Module.CONTRACT,contract,"RENEW","2027-06-30");
        check("2027-06-30".equals(scalar("SELECT NgayKetThuc FROM HopDong WHERE ID="+contract).toString()),"Gia hạn hợp đồng");
        reject(()->staff.transition(Module.CONTRACT,contract,"RENEW","2026-01-01"),"Chặn ngày gia hạn sai");
        reject(()->staff.create(Module.CONTRACT,map("MaSV","SV001","MaPhong","T001","NgayBatDau","2026-10-01","NgayKetThuc","2026-12-31","TienCoc","0")),"Chặn phòng đầy khi lập hợp đồng");
        self.create(Module.TRANSFER,map("MaSV","SV001","PhongMoi","T001","LyDo","Test đầy phòng"));
        int transfer=last(self,Module.TRANSFER);
        reject(()->staff.transition(Module.TRANSFER,transfer,"APPROVE",null),"Chuyển vào phòng đầy bị chặn");
        check("CHO_DUYET".equals(scalar("SELECT TrangThai FROM ChuyenPhong WHERE ID="+transfer)),"Rollback yêu cầu khi phòng đầy");
        staff.transition(Module.TRANSFER,transfer,"REJECT",null);
        self.create(Module.TRANSFER,map("MaSV","SV001","PhongMoi","T002","LyDo","Chuyển phòng"));
        staff.transition(Module.TRANSFER,last(self,Module.TRANSFER),"APPROVE",null);
        check("T002".equals(scalar("SELECT MaPhong FROM SinhVien WHERE MaSV='SV001'")),"Chuyển phòng thành công");
        reject(()->self.create(Module.TRANSFER,map("MaSV","SV002","PhongMoi","T002","LyDo","Trái quyền")),"Không thao tác sinh viên khác");
        self.create(Module.REPAIR,map("MaSV","SV001","MoTa","Hỏng đèn","Anh",""));
        int repair=last(self,Module.REPAIR);
        reject(()->self.transition(Module.REPAIR,repair,"STATUS","HOAN_THANH"),"Sinh viên không duyệt sửa chữa");
        reject(()->staff.transition(Module.REPAIR,repair,"STATUS","HOAN_THANH"),"Không nhảy cóc tiến độ");
        staff.transition(Module.REPAIR,repair,"STATUS","DANG_XU_LY");
        staff.transition(Module.REPAIR,repair,"STATUS","HOAN_THANH");
        check("HOAN_THANH".equals(scalar("SELECT TrangThai FROM SuaChua WHERE ID="+repair)),"Hoàn thành sửa chữa");
        staff.create(Module.ASSET,map("MaPhong","T002","TenTaiSan","Quạt","SoLuong","2","TinhTrang","Tốt","BienBan","BB001","NgayBanGiao","2026-10-01"));
        staff.transition(Module.ASSET,last(staff,Module.ASSET),"STATUS","Hỏng 1 chiếc");
        check(self.list(Module.ASSET).size()==1,"Sinh viên xem tài sản phòng mình");
        execute("UPDATE LichSuLuuTru SET NgayVao='2026-10-01' WHERE MaSV='SV001' AND MaPhong='T002'");
        execute("UPDATE LichSuLuuTru SET NgayRa='2026-09-30' WHERE MaSV='SV001' AND MaPhong='P101'");
        staff.create(Module.UTILITY,map("MaPhong","T002","Ky","2026-10-01","DienCu","10","DienMoi","20","NuocCu","0","NuocMoi","2","GiaDien","3000","GiaNuoc","15000"));
        check(new BigDecimal("60000").compareTo((BigDecimal)scalar("SELECT SUM(SoTien) FROM PhanBoDienNuoc WHERE MaSV='SV001'"))==0,"Phân bổ điện nước");
        reject(()->staff.create(Module.UTILITY,map("MaPhong","T002","Ky","2026-10-01","DienCu","10","DienMoi","20","NuocCu","0","NuocMoi","2","GiaDien","3000","GiaNuoc","15000")),"Chặn trùng kỳ điện nước");
        reject(()->staff.create(Module.UTILITY,map("MaPhong","T002","Ky","2026-11-01","DienCu","20","DienMoi","10","NuocCu","0","NuocMoi","2","GiaDien","3000","GiaNuoc","15000")),"Chặn chỉ số mới nhỏ hơn cũ");
        staff.create(Module.VIOLATION,map("MaSV","SV001","NgayViPham","2026-10-10","NoiDung","Ồn","XuLy","Phạt","TienPhat","50000"));
        Object[] invoice=new KtxDao().getInvoices(10,2026).stream().filter(r->r[0].equals("SV001")).findFirst().orElseThrow();
        check(((BigDecimal)invoice[8]).compareTo(new BigDecimal("60000"))==0 && ((BigDecimal)invoice[9]).compareTo(new BigDecimal("50000"))==0,"Hóa đơn gồm điện nước và phạt");
        LocalDate period=LocalDate.of(2026,10,1);
        BigDecimal before=(BigDecimal)self.debts(period).get(0)[4];
        staff.create(Module.PAYMENT,map("MaSV","SV001","Ky","2026-10-01","SoTien","100000","GhiChu","Tiền mặt"));
        check(before.subtract(new BigDecimal("100000")).compareTo((BigDecimal)self.debts(period).get(0)[4])==0,"Thanh toán từng phần giảm công nợ");
        reject(()->staff.create(Module.PAYMENT,map("MaSV","SV001","Ky","2026-10-01","SoTien","9999999","GhiChu","")),"Chặn thanh toán vượt công nợ");
        reject(()->self.create(Module.PAYMENT,map("MaSV","SV001","Ky","2026-10-01","SoTien","1","GhiChu","")),"Sinh viên không tự xác nhận thanh toán");
        check(self.list(Module.PAYMENT).size()==1 && self.debts(period).size()==1,"Sinh viên chỉ xem thanh toán và nợ của mình");
        staff.create(Module.NOTICE,map("TieuDe","Chung","NoiDung","Thông báo chung","MaPhong","","Han","2026-11-15"));
        staff.create(Module.NOTICE,map("TieuDe","Riêng","NoiDung","Thông báo P101","MaPhong","P101","Han","2026-11-15"));
        check(self.list(Module.NOTICE).size()==1,"Lọc thông báo theo phòng");
        check(staff.list(Module.NOTICE).size()==2,"Quản lý xem mọi thông báo");
        check(!self.reminders().isEmpty(),"Nhắc công nợ");
        for(Module m:Module.values()) for(Object[] row:self.list(m))
            if(!Set.of(Module.ASSET,Module.UTILITY,Module.NOTICE).contains(m)) check("SV001".equals(row[1]),"Lọc quyền: "+m);
        staff.create(Module.TRANSFER,map("MaSV","TESTSV","PhongMoi","T002","LyDo","Chuyển hợp đồng"));
        staff.transition(Module.TRANSFER,last(staff,Module.TRANSFER),"APPROVE",null);
        check("T002".equals(scalar("SELECT MaPhong FROM HopDong WHERE ID="+contract)),"Chuyển phòng cập nhật hợp đồng");
        check(new BigDecimal("60000").compareTo((BigDecimal)scalar("SELECT SUM(SoTien) FROM PhanBoDienNuoc WHERE MaSV='SV001'"))==0,"Chuyển phòng không chia lại phí đã chốt");
        execute("UPDATE LichSuLuuTru SET NgayVao='2026-09-01' WHERE MaPhong='T002'");
        staff.create(Module.UTILITY,map("MaPhong","T002","Ky","2026-09-01","NgayLap","2026-10-01","DienCu","0","DienMoi","1","NuocCu","0","NuocMoi","0","GiaDien","1.01","GiaNuoc","0"));
        check(new BigDecimal("1.01").compareTo((BigDecimal)scalar("SELECT SUM(p.SoTien) FROM PhanBoDienNuoc p JOIN DienNuoc d ON d.ID=p.MaDienNuoc WHERE d.Ky='2026-09-01'"))==0,"Phân bổ số lẻ không mất tiền");
        LocalDate current=LocalDate.now().withDayOfMonth(1);
        BigDecimal rentBefore=(BigDecimal)new KtxDao().getInvoices(current.getMonthValue(),current.getYear()).stream().filter(r->r[0].equals("TESTSV")).findFirst().orElseThrow()[3];
        staff.transition(Module.CONTRACT,contract,"CLOSE",null);
        check(scalar("SELECT MaPhong FROM SinhVien WHERE MaSV='TESTSV'")==null,"Thanh lý hợp đồng trả phòng");
        BigDecimal rentAfter=(BigDecimal)new KtxDao().getInvoices(current.getMonthValue(),current.getYear()).stream().filter(r->r[0].equals("TESTSV")).findFirst().orElseThrow()[3];
        check(rentBefore.signum()>0 && rentBefore.compareTo(rentAfter)==0,"Trả phòng vẫn giữ tiền thuê nguyên tháng");
        javax.swing.SwingUtilities.invokeAndWait(()-> {
            vn.ktx.ui.UiTheme.install();
            vn.ktx.ui.ExtensionPanel adminPanel=new vn.ktx.ui.ExtensionPanel(admin);
            vn.ktx.ui.ExtensionPanel studentPanel=new vn.ktx.ui.ExtensionPanel(student);
            check(adminPanel.getComponentCount()==1,"Khởi tạo giao diện quản trị viên");
            check(studentPanel.getComponentCount()==1,"Khởi tạo giao diện sinh viên");
            vn.ktx.ui.WorkspacePanel studentWorkspace=(vn.ktx.ui.WorkspacePanel)studentPanel.getComponent(0);
            check(!studentWorkspace.pageKeys().contains("accounts"),"Sinh viên không có menu tài khoản");
            for(String page:studentWorkspace.pageKeys()) studentWorkspace.showPage(page);
            check(studentWorkspace.pageKeys().size()==11,"Các nghiệp vụ sinh viên có thể mở từ menu");
        });
        System.out.println("PASS: "+checks+" business, authorization and Swing construction checks.");
    }
    private static Map<String,String> map(String... args) { Map<String,String> m=new HashMap<>(); for(int i=0;i<args.length;i+=2)m.put(args[i],args[i+1]); return m; }
    private static int last(ExtensionDao d,Module m) throws Exception { return ((Number)d.list(m).get(0)[0]).intValue(); }
    private static void execute(String sql) throws Exception { try(Connection c=Database.getConnection();Statement s=c.createStatement()) { s.executeUpdate(sql); } }
    private static Object scalar(String sql) throws Exception { try(Connection c=Database.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)) { r.next(); return r.getObject(1); } }
    private static void check(boolean valid,String message) { if(!valid) throw new AssertionError(message); checks++; }
    private static void reject(Action action,String message) throws Exception { try { action.run(); } catch(SQLException e) { checks++; return; } throw new AssertionError(message); }
    @FunctionalInterface interface Action { void run() throws Exception; }
}
