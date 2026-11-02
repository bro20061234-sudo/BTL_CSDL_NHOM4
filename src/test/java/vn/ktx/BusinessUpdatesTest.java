package vn.ktx;

import vn.ktx.config.Database;
import vn.ktx.dao.*;
import vn.ktx.dao.ExtensionDao.Module;
import vn.ktx.model.*;
import vn.ktx.ui.*;
import java.sql.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Chạy sau ExtensionIntegrationTest trên CSDL thử nghiệm riêng, đã nâng cấp 003. */
public final class BusinessUpdatesTest {
    private static int checks;
    public static void main(String[] args) throws Exception {
        if(!"1".equals(System.getenv("KTX_RUN_INTEGRATION")) || !System.getenv().getOrDefault("KTX_DB_URL","").startsWith("jdbc:mysql://127.0.0.1:33317/")) throw new IllegalStateException("Chỉ chạy trên CSDL thử nghiệm cổng 33317.");
        KtxDao core=new KtxDao(); ExtensionDao staff=new ExtensionDao(new AuthDao().login("testadmin","password123".toCharArray()));
        ExtensionDao self=new ExtensionDao(new AuthDao().login("student1","password123".toCharArray()));
        int index=0;
        for(RoomType type:RoomType.values()) {
            String room="NEW"+(++index);core.saveRoom(room,room,type.toString(),type.price(),type.capacity(),true);
            equal(type.capacity(),((Number)scalar("SELECT SoNguoiToiDa FROM Phong WHERE MaPhong='"+room+"'")).intValue(),"Sức chứa mẫu phòng");
            equal(type.assets().size(),((Number)scalar("SELECT COUNT(*) FROM TaiSan WHERE MaPhong='"+room+"'")).intValue(),"Tài sản mặc định theo mẫu");
            equal(type.capacity(),((Number)scalar("SELECT SoLuong FROM TaiSan WHERE MaPhong='"+room+"' AND TenTaiSan='Giường'")).intValue(),"Số giường theo số người");
            core.saveRoom(room,room,type.toString(),new BigDecimal("1500000"),type.capacity(),false);
            equal(type.assets().size(),((Number)scalar("SELECT COUNT(*) FROM TaiSan WHERE MaPhong='"+room+"'")).intValue(),"Sửa phòng không nhân đôi tài sản");
        }
        reject(()->core.saveRoom("INVALID","BAD",RoomType.STANDARD.toString(),BigDecimal.ONE,2,true),"Chặn sai sức chứa mẫu");
        reject(()->core.saveRoom("INVALID","BAD","Tự nhập",BigDecimal.ONE,4,true),"Không tạo loại phòng tùy ý");
        equal(0,((Number)scalar("SELECT COUNT(*) FROM Phong WHERE MaPhong='INVALID'")).intValue(),"Lưu phòng lỗi không để lại dữ liệu");
        quote("2026-11-02T06:00","2026-11-02T18:00",3000);
        quote("2026-11-02T18:00","2026-11-03T06:00",5000);
        quote("2026-11-02T17:00","2026-11-02T19:00",8000);
        quote("2026-11-02T05:00","2026-11-02T07:00",8000);
        quote("2026-11-02T07:00","2026-11-02T08:00",3000);
        quote("2026-11-02T23:00","2026-11-03T01:00",5000);
        quote("2026-11-02T06:00","2026-11-03T06:00",8000);
        quote("2026-11-02T06:00","2026-11-04T06:00",16000);
        quote("2026-11-02T06:00","2026-11-02T18:00:00.000000001",8000);
        quote("1999-12-31T18:00","2000-01-01T06:00",5000);
        try { ParkingFee.calculate(LocalDateTime.now(),LocalDateTime.now().minusHours(1));throw new AssertionError("Giờ gửi sai"); } catch(IllegalArgumentException expected) { checks++; }
        Map<String,String> utility=map("MaPhong","P101","NgayLap","2026-11-01","DienCu","0","DienMoi","10","NuocCu","0","NuocMoi","0","GiaDien","1000","GiaNuoc","0");
        staff.create(Module.UTILITY,utility);
        equal("2026-10-01",scalar("SELECT Ky FROM DienNuoc WHERE MaPhong='P101'").toString(),"01/11 lập tiền tháng 10");
        equal("2026-11-01",scalar("SELECT NgayLap FROM DienNuoc WHERE MaPhong='P101'").toString(),"Lưu riêng ngày lập");
        equalMoney("10000",(BigDecimal)scalar("SELECT p.SoTien FROM PhanBoDienNuoc p JOIN DienNuoc d ON d.ID=p.MaDienNuoc WHERE d.MaPhong='P101'"),"Chia theo cư dân tháng sử dụng");
        reject(()->staff.create(Module.UTILITY,utility),"Chặn trùng tháng điện nước");
        Map<String,String> wrong=new HashMap<>(utility);wrong.put("Ky","2026-11-01");
        reject(()->staff.create(Module.UTILITY,wrong),"Không tính nhầm tháng hiện tại vào ngày 01");
        wrong.put("Ky","2026-10-01");wrong.put("NgayLap",LocalDate.now().plusDays(1).toString());
        reject(()->staff.create(Module.UTILITY,wrong),"Không lập phí tương lai");
        execute("UPDATE LichSuLuuTru SET NgayVao='2026-10-01' WHERE MaSV='TESTSV' AND MaPhong='T001'");
        Map<String,String> oldResident=new HashMap<>(utility);oldResident.put("MaPhong","T001");staff.create(Module.UTILITY,oldResident);
        equal(1,((Number)scalar("SELECT COUNT(*) FROM PhanBoDienNuoc p JOIN DienNuoc d ON d.ID=p.MaDienNuoc WHERE d.MaPhong='T001' AND p.MaSV='TESTSV'")).intValue(),"Sinh viên đã trả phòng vẫn nhận đúng phí tháng trước");
        check(self.list(Module.UTILITY,LocalDate.of(2026,10,1)).size()==1,"Lọc tháng giữ đúng quyền sinh viên");
        for(Object[] row:staff.list(Module.UTILITY,LocalDate.of(2026,10,1))) equal("2026-10-01",row[2].toString(),"Bộ lọc tháng không lẫn tháng khác");
        Map<String,String> reminder=map("MaSV","SV002","NgayViPham",LocalDate.now().toString(),"NoiDung","Đi trễ","HinhThuc","NHAC_NHO","TienPhat","0","XuLy","Lần đầu");
        staff.create(Module.VIOLATION,reminder);
        check(scalar("SELECT MaPhong FROM SinhVien WHERE MaSV='SV002'")!=null,"Nhắc nhở không trả phòng");
        Map<String,String> badReminder=new HashMap<>(reminder);badReminder.put("TienPhat","5000");
        reject(()->staff.create(Module.VIOLATION,badReminder),"Nhắc nhở không kèm tiền phạt");
        Map<String,String> fine=new HashMap<>(reminder);fine.put("HinhThuc","HANH_CHINH");fine.put("TienPhat","5000");staff.create(Module.VIOLATION,fine);
        LocalDate current=LocalDate.now().withDayOfMonth(1);
        equalMoney("5000",(BigDecimal)invoice(core,"SV002",current)[9],"Phạt hành chính vào đúng tháng");
        fine.put("TienPhat","0");reject(()->staff.create(Module.VIOLATION,fine),"Phạt hành chính phải có tiền");
        Map<String,String> part=map("MaSV","SV002","Ky",current.toString(),"CheDo","PARTIAL","SoTien","100000","GhiChu","Trả một phần");
        BigDecimal before=staff.remaining("SV002",current);staff.create(Module.PAYMENT,part);
        equalMoney(before.subtract(new BigDecimal("100000")).toPlainString(),staff.remaining("SV002",current),"Trả một phần giảm dư nợ");
        Map<String,String> over=new HashMap<>(part);over.put("SoTien","9999999999");reject(()->staff.create(Module.PAYMENT,over),"Chặn thu vượt dư nợ");
        Map<String,String> full=new HashMap<>(part);full.put("CheDo","FULL");full.put("SoTien","1");staff.create(Module.PAYMENT,full);
        equalMoney("0",staff.remaining("SV002",current),"Thanh toán toàn bộ lấy số nợ thật tại thời điểm lưu");
        reject(()->staff.create(Module.PAYMENT,full),"Không thu lại khoản đã hết nợ");
        reject(()->self.create(Module.PAYMENT,part),"Sinh viên không được tự ghi nhận thu tiền");
        staff.create(Module.CONTRACT,map("MaSV","SV003","MaPhong","P102","NgayBatDau",current.toString(),"NgayKetThuc",current.plusMonths(6).toString(),"TienCoc","100000"));
        core.addVehicle("SV003","BAN-TEST","Xe máy",LocalDate.now());
        int vehicle=((Number)scalar("SELECT MaXe FROM XeVeThang WHERE BienSo='BAN-TEST'")).intValue();
        staff.create(Module.APPLICATION,map("MaSV","SV003","LoaiPhong",RoomType.STANDARD.toString(),"GhiChu",""));
        staff.create(Module.TRANSFER,map("MaSV","SV003","PhongMoi","NEW3","LyDo","Thử nghiệm"));
        BigDecimal rentBefore=(BigDecimal)invoice(core,"SV003",current)[3];
        Map<String,String> suspension=map("MaSV","SV003","NgayViPham",LocalDate.now().toString(),"NoiDung","Vi phạm nghiêm trọng","HinhThuc","DINH_CHI","TienPhat","0","XuLy","Chấm dứt lưu trú");
        staff.create(Module.VIOLATION,suspension);
        check(scalar("SELECT MaPhong FROM SinhVien WHERE MaSV='SV003'")==null,"Đình chỉ trả phòng");
        equal("DINH_CHI",scalar("SELECT TrangThai FROM HopDong WHERE MaSV='SV003'"),"Hợp đồng có trạng thái đình chỉ");
        equal(LocalDate.now().toString(),scalar("SELECT NgayKetThuc FROM XeVeThang WHERE MaXe="+vehicle).toString(),"Vé tháng kết thúc khi đình chỉ");
        equal("TU_CHOI",scalar("SELECT TrangThai FROM DonDangKy WHERE MaSV='SV003'"),"Hủy đơn đang chờ");
        equal("TU_CHOI",scalar("SELECT TrangThai FROM ChuyenPhong WHERE MaSV='SV003'"),"Hủy yêu cầu chuyển phòng đang chờ");
        equalMoney(rentBefore.toPlainString(),(BigDecimal)invoice(core,"SV003",current)[3],"Đình chỉ không xóa tiền phòng đã phát sinh");
        equalMoney("0",(BigDecimal)invoice(core,"SV003",current.plusMonths(1))[3],"Không tính tiền phòng tháng sau đình chỉ");
        equalMoney("0",(BigDecimal)invoice(core,"SV003",current.plusMonths(1))[5],"Không thu vé tháng tháng sau đình chỉ");
        reject(()->staff.create(Module.APPLICATION,map("MaSV","SV003","LoaiPhong",RoomType.STANDARD.toString())),"Không đăng ký ở lại sau đình chỉ");
        reject(()->core.addVehicle("SV003","NEW-BAN","Xe máy",LocalDate.now()),"Không đăng ký vé tháng sau đình chỉ");
        reject(()->core.addParkingLog(vehicle,LocalDateTime.now()),"Vé đã đình chỉ không được ghi lượt mới");
        reject(()->execute("UPDATE SinhVien SET MaPhong='NEW3' WHERE MaSV='SV003'"),"Trigger chặn xếp lại phòng sau đình chỉ");
        reject(()->staff.create(Module.VIOLATION,suspension),"Không đình chỉ hai lần");
        Map<String,String> payOld=map("MaSV","SV003","Ky",current.toString(),"CheDo","FULL","GhiChu","Thu công nợ cũ");staff.create(Module.PAYMENT,payOld);
        equalMoney("0",staff.remaining("SV003",current),"Sinh viên đình chỉ vẫn thanh toán được công nợ cũ");
        equal("Đình chỉ lưu trú",core.findStudents("SV003").get(0)[8],"Trạng thái sinh viên hiển thị đình chỉ");
        javax.swing.SwingUtilities.invokeAndWait(()->{
            UiTheme.install();MonthPicker picker=new MonthPicker(YearMonth.of(2026,10));equal(YearMonth.of(2026,10),picker.getMonth(),"Bộ chọn tháng/năm");
            picker.setMonth(YearMonth.of(2025,12));equal(YearMonth.of(2025,12),picker.getMonth(),"Bộ chọn chuyển năm");
            check(new ParkingPanel().getComponentCount()>0,"Khởi tạo quầy gửi xe không cần nhập giờ");
        });
        System.out.println("PASS: "+checks+" business update checks.");
    }
    private static Object[] invoice(KtxDao dao,String student,LocalDate month) throws Exception { return dao.getInvoices(month.getMonthValue(),month.getYear()).stream().filter(r->student.equals(r[0])).findFirst().orElseThrow(); }
    private static void quote(String in,String out,long price) { equalMoney(Long.toString(price),ParkingFee.calculate(LocalDateTime.parse(in),LocalDateTime.parse(out)).total(),"Tính phí khung ngày/đêm "+in+" → "+out); }
    private static Map<String,String> map(String... fields) { Map<String,String> result=new HashMap<>();for(int i=0;i<fields.length;i+=2)result.put(fields[i],fields[i+1]);return result; }
    private static Object scalar(String sql) throws Exception { try(Connection c=Database.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)) { r.next();return r.getObject(1); } }
    private static void execute(String sql) throws SQLException { try(Connection c=Database.getConnection();Statement s=c.createStatement()) { s.executeUpdate(sql); } }
    private static void equal(Object expected,Object actual,String message) { check(Objects.equals(expected,actual),message+" ["+expected+" != "+actual+"]"); }
    private static void equalMoney(String expected,BigDecimal actual,String message) { check(new BigDecimal(expected).compareTo(actual)==0,message); }
    private static void check(boolean valid,String message) { if(!valid)throw new AssertionError(message);checks++; }
    private static void reject(Action action,String message) throws Exception { try { action.run(); } catch(SQLException expected) { checks++;return; }throw new AssertionError(message); }
    @FunctionalInterface private interface Action { void run() throws Exception; }
}
