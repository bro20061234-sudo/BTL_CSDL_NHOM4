package vn.ktx.dao;

import vn.ktx.config.Database;
import vn.ktx.model.ParkingService;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.math.BigDecimal;

/** Vé tháng giữ lịch sử trong CSDL. Lượt đã lấy xe không còn nằm trong danh sách xe trong bãi. */
public final class ParkingDao {
    private static final String CANON="UPPER(REPLACE(REPLACE(REPLACE(x.BienSo,'-',''),'.',''),' ',''))";
    private static final String ACTIVE="SELECT n.MaLuot,x.BienSo,s.HoTen,n.ThoiGianGui,n.TienPhatSinh FROM NhatKyGuiXe n JOIN XeVeThang x ON x.MaXe=n.MaXe JOIN SinhVien s ON s.MaSV=x.MaSV WHERE n.ThoiGianLay IS NULL";
    public List<ParkingService.Entry> active() throws SQLException {
        try(Connection c=Database.getConnection()) {
            List<ParkingService.Entry> result=new ArrayList<>();
            for(Object[] row:ExtensionDao.query(c,ACTIVE+" ORDER BY n.ThoiGianGui DESC,n.MaLuot DESC")) result.add(entry(row));
            return result;
        }
    }
    /** Null nghĩa là biển số chưa có vé tháng hợp lệ: thu theo lượt. */
    public ParkingService.Entry enter(String plate,LocalDateTime now) throws SQLException {
        String canonical=ParkingService.canonical(plate);
        try(Connection c=Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                List<Object[]> vehicles=ExtensionDao.query(c,"SELECT x.MaXe,s.HoTen FROM XeVeThang x JOIN SinhVien s ON s.MaSV=x.MaSV WHERE "+CANON+"=? AND x.NgayDangKy<=? AND (x.NgayKetThuc IS NULL OR x.NgayKetThuc>=?) AND NOT EXISTS(SELECT 1 FROM DinhChiLuuTru d WHERE d.MaSV=x.MaSV AND d.Ngay<=?) FOR UPDATE",canonical,java.sql.Date.valueOf(now.toLocalDate()),java.sql.Date.valueOf(now.toLocalDate()),java.sql.Date.valueOf(now.toLocalDate()));
                if(!ExtensionDao.query(c,ACTIVE+" AND "+CANON+"=? FOR UPDATE",canonical).isEmpty()) throw new SQLException("Xe này đã ở trong bãi. Hãy lấy xe trước khi gửi lại.");
                if(vehicles.size()>1) throw new SQLException("Biển số có nhiều đăng ký vé tháng. Hãy sửa đăng ký trùng trước.");
                if(vehicles.isEmpty()) { c.commit();return null; }
                try(PreparedStatement p=c.prepareStatement("INSERT INTO NhatKyGuiXe(MaXe,ThoiGianGui) VALUES (?,?)",Statement.RETURN_GENERATED_KEYS)) {
                    p.setObject(1,vehicles.get(0)[0]);p.setTimestamp(2,Timestamp.valueOf(now));p.executeUpdate();
                    try(ResultSet keys=p.getGeneratedKeys()) {
                        if(!keys.next()) throw new SQLException("Không lấy được mã lượt gửi.");
                        long id=keys.getLong(1);Object[] row=ExtensionDao.query(c,ACTIVE+" AND n.MaLuot=?",id).get(0);
                        ParkingService.Entry result=entry(row);c.commit();return result;
                    }
                }
            } catch(SQLException | RuntimeException error) { c.rollback();throw error; }
        }
    }
    public ParkingService.Receipt leave(long id,LocalDateTime now) throws SQLException {
        try(Connection c=Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                List<Object[]> rows=ExtensionDao.query(c,ACTIVE+" AND n.MaLuot=? FOR UPDATE",id);
                if(rows.isEmpty()) throw new SQLException("Xe đã được lấy hoặc không còn trong bãi. Hãy làm mới danh sách.");
                ParkingService.Entry vehicle=entry(rows.get(0));
                if(now.isBefore(vehicle.entered())) throw new SQLException("Đồng hồ hiện tại sớm hơn giờ gửi. Hãy kiểm tra giờ hệ thống.");
                try(PreparedStatement p=c.prepareStatement("UPDATE NhatKyGuiXe SET ThoiGianLay=? WHERE MaLuot=? AND ThoiGianLay IS NULL")) {
                    p.setTimestamp(1,Timestamp.valueOf(now));p.setLong(2,id);
                    if(p.executeUpdate()!=1) throw new SQLException("Lượt gửi đã được xử lý.");
                }
                c.commit();return new ParkingService.Receipt(vehicle,now,vehicle.monthlyFee());
            } catch(SQLException error) { c.rollback();throw error; }
        }
    }
    private static ParkingService.Entry entry(Object[] row) {
        LocalDateTime time=row[3] instanceof LocalDateTime local?local:((Timestamp)row[3]).toLocalDateTime();
        return new ParkingService.Entry("M:"+row[0],row[1].toString(),row[2].toString(),true,time,new BigDecimal(row[4].toString()));
    }
}
