package vn.ktx.dao;

import vn.ktx.config.Database;
import vn.ktx.model.UserSession;
import java.sql.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/** Các thao tác nghiệp vụ mới kiểm tra quyền tại DAO, không chỉ ẩn nút trên giao diện. */
public final class ExtensionDao {
    public enum Module {
        APPLICATION("Đăng ký lưu trú", "DonDangKy", "ID,MaSV,LoaiPhong,NgayGui,TrangThai,GhiChu",
                "Mã, Sinh viên, Loại phòng, Ngày gửi, Trạng thái, Ghi chú", true),
        CONTRACT("Hợp đồng", "HopDong", "ID,MaSV,MaPhong,NgayBatDau,NgayKetThuc,TienCoc,TrangThai",
                "Mã, Sinh viên, Phòng, Bắt đầu, Kết thúc, Tiền cọc, Trạng thái", false),
        TRANSFER("Chuyển phòng", "ChuyenPhong", "ID,MaSV,PhongCu,PhongMoi,LyDo,NgayGui,TrangThai",
                "Mã, Sinh viên, Phòng cũ, Phòng mới, Lý do, Ngày gửi, Trạng thái", true),
        REPAIR("Sửa chữa", "SuaChua", "ID,MaSV,MaPhong,MoTa,Anh,NgayBao,TrangThai",
                "Mã, Sinh viên, Phòng, Mô tả, Ảnh, Ngày báo, Trạng thái", true),
        ASSET("Tài sản", "TaiSan", "ID,MaPhong,TenTaiSan,SoLuong,TinhTrang,BienBan,NgayBanGiao",
                "Mã, Phòng, Tài sản, Số lượng, Tình trạng, Biên bản bàn giao, Ngày bàn giao", false),
        UTILITY("Điện nước", "DienNuoc", "ID,MaPhong,Ky,DienCu,DienMoi,NuocCu,NuocMoi,GiaDien,GiaNuoc",
                "Mã, Phòng, Kỳ, Điện cũ, Điện mới, Nước cũ, Nước mới, Giá điện, Giá nước", false),
        PAYMENT("Thanh toán", "ThanhToan", "ID,MaSV,Ky,SoTien,NgayThanhToan,GhiChu",
                "Mã biên nhận, Sinh viên, Kỳ, Số tiền, Ngày thanh toán, Ghi chú", false),
        NOTICE("Thông báo", "ThongBao", "ID,TieuDe,NoiDung,MaPhong,Han,NgayTao",
                "Mã, Tiêu đề, Nội dung, Phòng (trống = toàn KTX), Hạn, Ngày tạo", false),
        VIOLATION("Vi phạm", "ViPham", "ID,MaSV,NgayViPham,NoiDung,XuLy,TienPhat",
                "Mã, Sinh viên, Ngày vi phạm, Nội dung, Xử lý, Tiền phạt", false);
        public final String title, table, columns;
        public final String[] headers;
        public final boolean studentCanCreate;
        Module(String title,String table,String columns,String headers,boolean studentCanCreate) {
            this.title=title; this.table=table; this.columns=columns;
            this.headers=headers.split(", "); this.studentCanCreate=studentCanCreate;
        }
    }
    private final UserSession user;
    public ExtensionDao(UserSession user) { this.user=Objects.requireNonNull(user); }
    private void staff() throws SQLException { if(!user.isStaff()) throw new SQLException("Chức năng dành cho nhân viên quản lý."); }
    private String owner(String id) throws SQLException {
        if (!user.isStaff() && !Objects.equals(id,user.studentId())) throw new SQLException("Không được thao tác dữ liệu của sinh viên khác.");
        return id;
    }
    public List<Object[]> list(Module m) throws SQLException {
        String sql="SELECT "+m.columns+" FROM "+m.table;
        List<Object> args=new ArrayList<>();
        if(!user.isStaff()) {
            if(m==Module.NOTICE) sql+=" WHERE MaPhong IS NULL OR MaPhong=(SELECT MaPhong FROM SinhVien WHERE MaSV=?)";
            else if(m==Module.ASSET) sql+=" WHERE MaPhong=(SELECT MaPhong FROM SinhVien WHERE MaSV=?)";
            else if(m==Module.UTILITY) sql+=" WHERE ID IN (SELECT MaDienNuoc FROM PhanBoDienNuoc WHERE MaSV=?)";
            else sql+=" WHERE MaSV=?";
            args.add(user.studentId());
        }
        try(Connection c=Database.getConnection()) { return query(c,sql+" ORDER BY ID DESC",args.toArray()); }
    }
    public List<String> students() throws SQLException {
        if(!user.isStaff()) return List.of(user.studentId());
        return new KtxDao().getStudentChoices();
    }
    public List<String> rooms() throws SQLException {
        try(Connection c=Database.getConnection()) {
            List<String> result=new ArrayList<>();
            for(Object[] row:query(c,"SELECT MaPhong FROM Phong ORDER BY MaPhong")) result.add(row[0].toString());
            return result;
        }
    }
    public List<String> roomTypes() throws SQLException {
        try(Connection c=Database.getConnection()) {
            List<String> result=new ArrayList<>();
            for(Object[] row:query(c,"SELECT DISTINCT LoaiPhong FROM Phong ORDER BY LoaiPhong")) result.add(row[0].toString());
            return result;
        }
    }
    public void create(Module m, Map<String,String> v) throws SQLException {
        if(!m.studentCanCreate) staff();
        String sv=v.get("MaSV"); if(sv!=null) owner(sv);
        try(Connection c=Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                switch(m) {
                    case APPLICATION -> {
                        lockStudent(c,sv);
                        if(!query(c,"SELECT ID FROM DonDangKy WHERE MaSV=? AND TrangThai='CHO_DUYET'",sv).isEmpty()) fail("Đã có đơn đang chờ duyệt.");
                        if(query(c,"SELECT MaPhong FROM Phong WHERE LoaiPhong=?",required(v,"LoaiPhong")).isEmpty()) fail("Loại phòng không tồn tại.");
                        update(c,"INSERT INTO DonDangKy(MaSV,LoaiPhong,NgayGui,GhiChu) VALUES (?,?,CURRENT_DATE,?)",sv,v.get("LoaiPhong"),v.getOrDefault("GhiChu",""));
                    }
                    case CONTRACT -> {
                        lockStudent(c,sv);
                        LocalDate from=date(v,"NgayBatDau"),to=date(v,"NgayKetThuc");
                        if(to.isBefore(from)) fail("Ngày kết thúc phải từ ngày bắt đầu trở đi.");
                        if(!query(c,"SELECT ID FROM HopDong WHERE MaSV=? AND TrangThai='HIEU_LUC' FOR UPDATE",sv).isEmpty()) fail("Sinh viên đã có hợp đồng hiệu lực.");
                        assign(c,sv,required(v,"MaPhong"));
                        update(c,"INSERT INTO HopDong(MaSV,MaPhong,NgayBatDau,NgayKetThuc,TienCoc) VALUES (?,?,?,?,?)",sv,v.get("MaPhong"),from,to,money(v,"TienCoc",false));
                    }
                    case TRANSFER -> {
                        Object[] student=lockStudent(c,sv);
                        String target=required(v,"PhongMoi");
                        if(student[0]==null || target.equals(student[0])) fail("Cần đang ở KTX và chọn phòng khác phòng hiện tại.");
                        if(!query(c,"SELECT ID FROM ChuyenPhong WHERE MaSV=? AND TrangThai='CHO_DUYET'",sv).isEmpty()) fail("Đã có yêu cầu chuyển phòng chờ duyệt.");
                        update(c,"INSERT INTO ChuyenPhong(MaSV,PhongCu,PhongMoi,LyDo,NgayGui) VALUES (?,?,?,?,CURRENT_DATE)",sv,student[0],target,required(v,"LyDo"));
                    }
                    case REPAIR -> {
                        Object[] student=lockStudent(c,sv);
                        if(student[0]==null) fail("Sinh viên chưa có phòng ở.");
                        update(c,"INSERT INTO SuaChua(MaSV,MaPhong,MoTa,Anh,NgayBao) VALUES (?,?,?,?,CURRENT_DATE)",sv,student[0],required(v,"MoTa"),v.getOrDefault("Anh",""));
                    }
                    case ASSET -> {
                        int quantity=integer(required(v,"SoLuong")); if(quantity<=0) fail("Số lượng phải lớn hơn 0.");
                        update(c,"INSERT INTO TaiSan(MaPhong,TenTaiSan,SoLuong,TinhTrang,BienBan,NgayBanGiao) VALUES (?,?,?,?,?,?)",required(v,"MaPhong"),required(v,"TenTaiSan"),quantity,required(v,"TinhTrang"),required(v,"BienBan"),date(v,"NgayBanGiao"));
                    }
                    case UTILITY -> utility(c,v);
                    case PAYMENT -> {
                        lockStudent(c,sv);
                        LocalDate period=period(v); BigDecimal amount=money(v,"SoTien",true);
                        BigDecimal debt=balance(c,sv,period)[2];
                        if(amount.compareTo(debt)>0) fail("Số tiền vượt công nợ còn lại: "+debt);
                        update(c,"INSERT INTO ThanhToan(MaSV,Ky,SoTien,GhiChu) VALUES (?,?,?,?)",sv,period,amount,v.getOrDefault("GhiChu",""));
                    }
                    case NOTICE -> update(c,"INSERT INTO ThongBao(TieuDe,NoiDung,MaPhong,Han) VALUES (?,?,?,?)",required(v,"TieuDe"),required(v,"NoiDung"),blank(v.get("MaPhong")),date(v,"Han"));
                    case VIOLATION -> {
                        lockStudent(c,sv);
                        update(c,"INSERT INTO ViPham(MaSV,NgayViPham,NoiDung,XuLy,TienPhat) VALUES (?,?,?,?,?)",sv,date(v,"NgayViPham"),required(v,"NoiDung"),required(v,"XuLy"),money(v,"TienPhat",false));
                    }
                }
                c.commit();
            } catch(Exception e) { c.rollback(); if(e instanceof SQLException s) throw s; throw new SQLException(e.getMessage(),e); }
        }
    }
    private void utility(Connection c,Map<String,String> v) throws SQLException {
        String room=required(v,"MaPhong");
        if(query(c,"SELECT MaPhong FROM Phong WHERE MaPhong=? FOR UPDATE",room).isEmpty()) fail("Phòng không tồn tại.");
        BigDecimal oldE=money(v,"DienCu",false),newE=money(v,"DienMoi",false),oldW=money(v,"NuocCu",false),newW=money(v,"NuocMoi",false);
        if(newE.compareTo(oldE)<0 || newW.compareTo(oldW)<0) fail("Chỉ số mới không được nhỏ hơn chỉ số cũ.");
        BigDecimal priceE=money(v,"GiaDien",false),priceW=money(v,"GiaNuoc",false);
        LocalDate period=period(v);
        List<Object[]> residents=query(c,"SELECT MaSV FROM SinhVien WHERE MaPhong=? AND NgayVaoKTX<? ORDER BY MaSV FOR UPDATE",room,period.plusMonths(1));
        if(residents.isEmpty()) fail("Phòng không có sinh viên để phân bổ.");
        update(c,"INSERT INTO DienNuoc(MaPhong,Ky,DienCu,DienMoi,NuocCu,NuocMoi,GiaDien,GiaNuoc) VALUES (?,?,?,?,?,?,?,?)",room,period,oldE,newE,oldW,newW,priceE,priceW);
        int id=((Number)query(c,"SELECT LAST_INSERT_ID()").get(0)[0]).intValue();
        BigDecimal total=newE.subtract(oldE).multiply(priceE).add(newW.subtract(oldW).multiply(priceW)).setScale(2,RoundingMode.HALF_UP);
        BigDecimal share=total.divide(BigDecimal.valueOf(residents.size()),2,RoundingMode.DOWN),remaining=total;
        for(int i=0;i<residents.size();i++) {
            BigDecimal part=i==residents.size()-1?remaining:share;
            update(c,"INSERT INTO PhanBoDienNuoc VALUES (?,?,?)",id,residents.get(i)[0],part); remaining=remaining.subtract(part);
        }
    }
    public void transition(Module m,int id,String action,String value) throws SQLException {
        staff();
        try(Connection c=Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                List<Object[]> rows=query(c,"SELECT "+m.columns+" FROM "+m.table+" WHERE ID=? FOR UPDATE",id);
                if(rows.isEmpty()) fail("Bản ghi không tồn tại.");
                Object[] r=rows.get(0);
                switch(m) {
                    case APPLICATION -> {
                        if(!"CHO_DUYET".equals(r[4])) fail("Đơn đã được xử lý.");
                        if("APPROVE".equals(action)) {
                            lockStudent(c,r[1].toString());
                            if(query(c,"SELECT MaPhong FROM Phong WHERE MaPhong=? AND LoaiPhong=?",value,r[2]).isEmpty()) fail("Chọn phòng đúng loại đã đăng ký.");
                            if(query(c,"SELECT MaPhong FROM SinhVien WHERE MaSV=? AND MaPhong IS NOT NULL",r[1]).size()>0) fail("Sinh viên đã có phòng. Hãy dùng chuyển phòng.");
                            assign(c,r[1].toString(),value);
                        } else if(!"REJECT".equals(action)) fail("Thao tác không hợp lệ.");
                        update(c,"UPDATE DonDangKy SET TrangThai=? WHERE ID=?","APPROVE".equals(action)?"DA_DUYET":"TU_CHOI",id);
                    }
                    case TRANSFER -> {
                        if(!"CHO_DUYET".equals(r[6])) fail("Yêu cầu đã được xử lý.");
                        if("APPROVE".equals(action)) {
                            Object[] current=lockStudent(c,r[1].toString());
                            if(!Objects.equals(current[0],r[2])) fail("Phòng hiện tại đã thay đổi. Hãy từ chối và tạo yêu cầu mới.");
                            assign(c,r[1].toString(),r[3].toString());
                            update(c,"UPDATE HopDong SET MaPhong=? WHERE MaSV=? AND TrangThai='HIEU_LUC'",r[3],r[1]);
                        } else if(!"REJECT".equals(action)) fail("Thao tác không hợp lệ.");
                        update(c,"UPDATE ChuyenPhong SET TrangThai=? WHERE ID=?","APPROVE".equals(action)?"DA_DUYET":"TU_CHOI",id);
                    }
                    case CONTRACT -> {
                        lockStudent(c,r[1].toString());
                        if(!"HIEU_LUC".equals(r[6])) fail("Hợp đồng đã thanh lý.");
                        if("RENEW".equals(action)) {
                            LocalDate end=parseDate(value);
                            if(!end.isAfter(((java.sql.Date)r[4]).toLocalDate())) fail("Ngày gia hạn phải sau ngày kết thúc hiện tại.");
                            update(c,"UPDATE HopDong SET NgayKetThuc=? WHERE ID=?",end,id);
                        } else if("CLOSE".equals(action)) {
                            update(c,"UPDATE HopDong SET TrangThai='DA_THANH_LY' WHERE ID=?",id);
                            update(c,"UPDATE SinhVien SET MaPhong=NULL WHERE MaSV=? AND MaPhong=?",r[1],r[2]);
                        } else fail("Thao tác không hợp lệ.");
                    }
                    case REPAIR -> {
                        if("TIEP_NHAN".equals(r[6]) && "DANG_XU_LY".equals(value) || "DANG_XU_LY".equals(r[6]) && "HOAN_THANH".equals(value))
                            update(c,"UPDATE SuaChua SET TrangThai=? WHERE ID=?",value,id);
                        else fail("Chuyển trạng thái theo thứ tự tiếp nhận → đang xử lý → hoàn thành.");
                    }
                    case ASSET -> update(c,"UPDATE TaiSan SET TinhTrang=? WHERE ID=?",nonempty(value),id);
                    default -> fail("Không hỗ trợ cập nhật mục này.");
                }
                c.commit();
            } catch(Exception e) { c.rollback(); if(e instanceof SQLException s) throw s; throw new SQLException(e.getMessage(),e); }
        }
    }
    private Object[] lockStudent(Connection c,String id) throws SQLException {
        List<Object[]> rows=query(c,"SELECT MaPhong FROM SinhVien WHERE MaSV=? FOR UPDATE",id);
        if(rows.isEmpty()) fail("Sinh viên không tồn tại."); return rows.get(0);
    }
    private void assign(Connection c,String student,String room) throws SQLException {
        List<Object[]> rooms=query(c,"SELECT SoNguoiToiDa FROM Phong WHERE MaPhong=? FOR UPDATE",room);
        if(rooms.isEmpty()) fail("Phòng không tồn tại.");
        int count=((Number)query(c,"SELECT COUNT(*) FROM SinhVien WHERE MaPhong=? AND MaSV<>?",room,student).get(0)[0]).intValue();
        if(count>=((Number)rooms.get(0)[0]).intValue()) fail("Phòng đã đủ số người tối đa.");
        update(c,"UPDATE SinhVien SET MaPhong=? WHERE MaSV=?",room,student);
    }
    public List<Object[]> debts(LocalDate period) throws SQLException {
        List<Object[]> result=new ArrayList<>();
        Map<String,BigDecimal> totals=new HashMap<>();
        for(Object[] row:new KtxDao().getInvoices(period.getMonthValue(),period.getYear())) totals.put(row[0].toString(),new BigDecimal(row[7].toString()));
        try(Connection c=Database.getConnection()) {
            Map<String,BigDecimal> payments=new HashMap<>();
            String sql="SELECT MaSV,SUM(SoTien) FROM ThanhToan WHERE Ky=?";
            Object[] args=user.isStaff()?new Object[]{period}:new Object[]{period,user.studentId()};
            if(!user.isStaff()) sql+=" AND MaSV=?";
            for(Object[] row:query(c,sql+" GROUP BY MaSV",args)) payments.put(row[0].toString(),new BigDecimal(row[1].toString()));
            for(String sv:students()) {
                BigDecimal total=totals.getOrDefault(sv,BigDecimal.ZERO),paid=payments.getOrDefault(sv,BigDecimal.ZERO);
                result.add(new Object[]{sv,period,total,paid,total.subtract(paid)});
            }
        }
        return result;
    }
    private BigDecimal[] balance(Connection c,String sv,LocalDate period) throws SQLException {
        BigDecimal total=BigDecimal.ZERO;
        for(Object[] r:new KtxDao().getInvoices(period.getMonthValue(),period.getYear())) if(sv.equals(r[0])) total=new BigDecimal(r[7].toString());
        BigDecimal paid=new BigDecimal(query(c,"SELECT COALESCE(SUM(SoTien),0) FROM ThanhToan WHERE MaSV=? AND Ky=?",sv,period).get(0)[0].toString());
        return new BigDecimal[]{total,paid,total.subtract(paid)};
    }
    public List<Object[]> reminders() throws SQLException {
        try(Connection c=Database.getConnection()) {
            String scope=user.isStaff()?"":" AND MaSV=?";
            Object[] args=user.isStaff()?new Object[]{}:new Object[]{user.studentId()};
            List<Object[]> rows=query(c,"SELECT MaSV,NgayKetThuc,'Hợp đồng sắp hết hạn / quá hạn' FROM HopDong WHERE TrangThai='HIEU_LUC' AND NgayKetThuc<=DATE_ADD(CURRENT_DATE,INTERVAL 30 DAY)"+scope,args);
            LocalDate month=LocalDate.now().withDayOfMonth(1);
            for(int i=0;i<3;i++) for(Object[] b:debts(month.minusMonths(i)))
                if(((BigDecimal)b[4]).signum()>0) rows.add(new Object[]{b[0],b[1],"Chưa thanh toán đủ: "+b[4]+" đồng"});
            return rows;
        }
    }
    public List<Object[]> accounts() throws SQLException {
        if(!user.isAdmin()) throw new SQLException("Chỉ quản trị viên được xem tài khoản.");
        try(Connection c=Database.getConnection()) { return query(c,"SELECT TenDangNhap,VaiTro,MaSV FROM TaiKhoan ORDER BY TenDangNhap"); }
    }
    public static List<Object[]> query(Connection c,String sql,Object... args) throws SQLException {
        try(PreparedStatement p=c.prepareStatement(sql)) {
            bind(p,args);
            try(ResultSet r=p.executeQuery()) {
                List<Object[]> rows=new ArrayList<>(); int n=r.getMetaData().getColumnCount();
                while(r.next()) { Object[] row=new Object[n]; for(int i=0;i<n;i++) row[i]=r.getObject(i+1); rows.add(row); }
                return rows;
            }
        }
    }
    private static void update(Connection c,String sql,Object... args) throws SQLException {
        try(PreparedStatement p=c.prepareStatement(sql)) { bind(p,args); p.executeUpdate(); }
    }
    private static void bind(PreparedStatement p,Object[] args) throws SQLException {
        for(int i=0;i<args.length;i++) { Object v=args[i]; p.setObject(i+1,v instanceof LocalDate d?java.sql.Date.valueOf(d):v); }
    }
    private static String blank(String s) { return s==null || s.isBlank()?null:s.trim(); }
    private static void fail(String message) throws SQLException { throw new SQLException(message); }
    private static String nonempty(String value) throws SQLException { if(blank(value)==null) fail("Vui lòng nhập đầy đủ thông tin."); return value.trim(); }
    private static String required(Map<String,String> v,String key) throws SQLException { return nonempty(v.get(key)); }
    private static LocalDate date(Map<String,String> v,String key) throws SQLException { return parseDate(required(v,key)); }
    private static LocalDate parseDate(String s) throws SQLException {
        try { return LocalDate.parse(s); } catch(Exception e) { throw new SQLException("Ngày phải có dạng yyyy-MM-dd."); }
    }
    private static LocalDate period(Map<String,String> v) throws SQLException {
        LocalDate d=date(v,"Ky"); if(d.getDayOfMonth()!=1 || d.getYear()<2000 || d.getYear()>2200) fail("Kỳ phải là ngày đầu tháng, năm 2000–2200."); return d;
    }
    private static int integer(String s) throws SQLException {
        try { return Integer.parseInt(s); } catch(Exception e) { throw new SQLException("Số lượng phải là số nguyên."); }
    }
    private static BigDecimal money(Map<String,String> v,String key,boolean positive) throws SQLException {
        try {
            BigDecimal amount=new BigDecimal(required(v,key));
            if(amount.signum()<0 || positive && amount.signum()==0 || amount.scale()>2 || amount.compareTo(new BigDecimal("9999999999.99"))>0) fail("Giá trị tiền/chỉ số không hợp lệ: "+key);
            return amount;
        } catch(NumberFormatException e) { throw new SQLException("Giá trị phải là số: "+key); }
    }
}
