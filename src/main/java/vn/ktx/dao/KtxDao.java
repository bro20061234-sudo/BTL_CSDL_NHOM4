package vn.ktx.dao;

import vn.ktx.config.Database;
import vn.ktx.model.Student;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class KtxDao {
    public static final String[] STUDENT_COLUMNS = {"Mã SV", "CCCD", "Họ tên", "Ngày sinh", "Lớp", "Quê quán", "Mã phòng", "Ngày vào KTX"};
    public static final String[] ROOM_COLUMNS = {"Mã phòng", "Số phòng", "Loại phòng", "Đơn giá/tháng", "Sức chứa", "Đang ở"};
    public static final String[] SERVICE_COLUMNS = {"Mã DV", "Tên dịch vụ", "Đơn giá"};
    public static final String[] USAGE_COLUMNS = {"Mã sử dụng", "Mã SV", "Sinh viên", "Mã DV", "Dịch vụ", "Thời gian", "Số lượng"};
    public static final String[] VEHICLE_COLUMNS = {"Mã xe", "Mã SV", "Sinh viên", "Biển số", "Loại xe", "Ngày đăng ký", "Phí tháng"};
    public static final String[] PARKING_COLUMNS = {"Mã lượt", "Mã xe", "Sinh viên", "Biển số", "Thời gian gửi", "Thời gian lấy", "Phí phát sinh"};
    public static final String[] GUEST_COLUMNS = {"Mã khách", "CCCD", "Họ tên", "Ngày sinh", "Mã SV", "Sinh viên", "Ngày đến"};
    public static final String[] INVOICE_COLUMNS = {"Mã SV", "Họ tên", "Mã phòng", "Tiền phòng", "Dịch vụ cơ bản", "Xe vé tháng", "Xe phát sinh", "Tổng tiền"};

    public void testConnection() throws SQLException {
        try (Connection connection = Database.getConnection()) {
            if (!connection.isValid(3)) throw new SQLException("Không thể xác thực kết nối đến MySQL.");
        }
    }

    public List<Object[]> findStudents(String search) throws SQLException {
        String pattern = "%" + (search == null ? "" : search.trim()) + "%";
        return query("SELECT MaSV, SoCMT, HoTen, NgaySinh, Lop, QueQuan, MaPhong, NgayVaoKTX " +
                        "FROM SinhVien WHERE MaSV LIKE ? OR HoTen LIKE ? OR Lop LIKE ? ORDER BY MaSV",
                pattern, pattern, pattern);
    }

    public void saveStudent(Student student, boolean insert) throws SQLException {
        validateRoomCapacity(student.getRoomId(), insert ? null : student.getId());
        String sql = insert
                ? "INSERT INTO SinhVien (MaSV, SoCMT, HoTen, NgaySinh, Lop, QueQuan, MaPhong, NgayVaoKTX) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                : "UPDATE SinhVien SET SoCMT=?, HoTen=?, NgaySinh=?, Lop=?, QueQuan=?, MaPhong=?, NgayVaoKTX=? WHERE MaSV=?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int i = 1;
            if (insert) statement.setString(i++, student.getId());
            statement.setString(i++, student.getIdentityNumber());
            statement.setString(i++, student.getFullName());
            statement.setDate(i++, Date.valueOf(student.getBirthDate()));
            statement.setString(i++, student.getClassName());
            statement.setString(i++, student.getHometown());
            statement.setString(i++, blankToNull(student.getRoomId()));
            statement.setDate(i++, Date.valueOf(student.getAdmissionDate()));
            if (!insert) statement.setString(i, student.getId());
            requireUpdated(statement.executeUpdate(), insert ? "Mã sinh viên đã tồn tại." : "Không tìm thấy sinh viên.");
        }
    }

    private void validateRoomCapacity(String roomId, String excludingStudentId) throws SQLException {
        if (roomId == null || roomId.isBlank()) return;
        String sql = "SELECT p.SoNguoiToiDa, COUNT(s.MaSV) AS DangO FROM Phong p " +
                "LEFT JOIN SinhVien s ON s.MaPhong=p.MaPhong " +
                "WHERE p.MaPhong=? GROUP BY p.MaPhong, p.SoNguoiToiDa";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, roomId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new SQLException("Mã phòng không tồn tại.");
                int occupied = rs.getInt("DangO");
                if (excludingStudentId != null) {
                    try (PreparedStatement count = connection.prepareStatement(
                            "SELECT COUNT(*) FROM SinhVien WHERE MaPhong=? AND MaSV=?")) {
                        count.setString(1, roomId);
                        count.setString(2, excludingStudentId);
                        try (ResultSet countRs = count.executeQuery()) {
                            if (countRs.next()) occupied -= countRs.getInt(1);
                        }
                    }
                }
                if (occupied >= rs.getInt("SoNguoiToiDa")) {
                    throw new SQLException("Phòng đã đạt số lượng người ở tối đa.");
                }
            }
        }
    }

    public void deleteStudent(String id) throws SQLException {
        execute("DELETE FROM SinhVien WHERE MaSV=?", id);
    }

    public List<Object[]> getRooms() throws SQLException {
        return query("SELECT p.MaPhong, p.SoPhong, p.LoaiPhong, p.DonGia, p.SoNguoiToiDa, COUNT(s.MaSV) " +
                "FROM Phong p LEFT JOIN SinhVien s ON s.MaPhong=p.MaPhong " +
                "GROUP BY p.MaPhong, p.SoPhong, p.LoaiPhong, p.DonGia, p.SoNguoiToiDa ORDER BY p.SoPhong");
    }

    public List<Object[]> getRoomChoices() throws SQLException {
        return query("SELECT MaPhong, SoPhong FROM Phong ORDER BY SoPhong");
    }

    public void saveRoom(String id, String number, String type, BigDecimal price, int capacity, boolean insert) throws SQLException {
        if (price.signum() < 0 || capacity < 1) throw new SQLException("Đơn giá phải không âm và sức chứa phải lớn hơn 0.");
        String sql = insert
                ? "INSERT INTO Phong (MaPhong, SoPhong, LoaiPhong, DonGia, SoNguoiToiDa) VALUES (?, ?, ?, ?, ?)"
                : "UPDATE Phong SET SoPhong=?, LoaiPhong=?, DonGia=?, SoNguoiToiDa=? WHERE MaPhong=?";
        if (insert) execute(sql, id, number, type, price, capacity);
        else execute(sql, number, type, price, capacity, id);
    }

    public void deleteRoom(String id) throws SQLException {
        if (hasRows("SELECT 1 FROM SinhVien WHERE MaPhong=? LIMIT 1", id)) {
            throw new SQLException("Không thể xóa phòng đang có sinh viên. Hãy chuyển sinh viên sang phòng khác trước.");
        }
        execute("DELETE FROM Phong WHERE MaPhong=?", id);
    }

    public List<Object[]> getServices() throws SQLException {
        return query("SELECT MaDV, TenDV, DonGia FROM DichVu ORDER BY MaDV");
    }

    public List<Object[]> getServiceChoices() throws SQLException {
        return query("SELECT MaDV, TenDV FROM DichVu ORDER BY TenDV");
    }

    public void saveService(String id, String name, BigDecimal price, boolean insert) throws SQLException {
        if (price.signum() < 0) throw new SQLException("Đơn giá phải không âm.");
        if (insert) execute("INSERT INTO DichVu (MaDV, TenDV, DonGia) VALUES (?, ?, ?)", id, name, price);
        else execute("UPDATE DichVu SET TenDV=?, DonGia=? WHERE MaDV=?", name, price, id);
    }

    public void deleteService(String id) throws SQLException {
        if (hasRows("SELECT 1 FROM SuDungDichVu WHERE MaDV=? LIMIT 1", id)) {
            throw new SQLException("Không thể xóa dịch vụ đã có lịch sử sử dụng.");
        }
        execute("DELETE FROM DichVu WHERE MaDV=?", id);
    }

    public List<Object[]> getUsages() throws SQLException {
        return query("SELECT sd.MaSD, sd.MaSV, sv.HoTen, sd.MaDV, dv.TenDV, sd.ThoiGianSuDung, sd.SoLuong " +
                "FROM SuDungDichVu sd JOIN SinhVien sv ON sv.MaSV=sd.MaSV " +
                "JOIN DichVu dv ON dv.MaDV=sd.MaDV ORDER BY sd.ThoiGianSuDung DESC");
    }

    public void addUsage(String studentId, String serviceId, LocalDateTime usedAt, int quantity) throws SQLException {
        if (quantity < 1) throw new SQLException("Số lượng phải lớn hơn 0.");
        execute("INSERT INTO SuDungDichVu (MaSV, MaDV, ThoiGianSuDung, SoLuong) VALUES (?, ?, ?, ?)",
                studentId, serviceId, Timestamp.valueOf(usedAt), quantity);
    }

    public void deleteUsage(int usageId) throws SQLException {
        execute("DELETE FROM SuDungDichVu WHERE MaSD=?", usageId);
    }

    public List<Object[]> getVehicles() throws SQLException {
        return query("SELECT x.MaXe, x.MaSV, sv.HoTen, x.BienSo, x.LoaiXe, x.NgayDangKy, x.DonGiaThang " +
                "FROM XeVeThang x JOIN SinhVien sv ON sv.MaSV=x.MaSV ORDER BY x.MaXe DESC");
    }

    public void addVehicle(String studentId, String plate, String type, LocalDate registrationDate) throws SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement count = connection.prepareStatement("SELECT COUNT(*) FROM XeVeThang WHERE MaSV=?")) {
            count.setString(1, studentId);
            try (ResultSet rs = count.executeQuery()) {
                rs.next();
                if (rs.getInt(1) >= 2) throw new SQLException("Mỗi sinh viên chỉ được đăng ký tối đa 2 xe vé tháng.");
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO XeVeThang (MaSV, BienSo, LoaiXe, NgayDangKy, DonGiaThang) VALUES (?, ?, ?, ?, 100000)")) {
                insert.setString(1, studentId);
                insert.setString(2, plate);
                insert.setString(3, type);
                insert.setDate(4, Date.valueOf(registrationDate));
                insert.executeUpdate();
            }
        }
    }

    public void deleteVehicle(int vehicleId) throws SQLException {
        if (hasRows("SELECT 1 FROM NhatKyGuiXe WHERE MaXe=? LIMIT 1", vehicleId)) {
            throw new SQLException("Không thể xóa xe đã có nhật ký gửi xe để bảo toàn lịch sử phí.");
        }
        execute("DELETE FROM XeVeThang WHERE MaXe=?", vehicleId);
    }

    public List<Object[]> getParkingLogs() throws SQLException {
        return query("SELECT n.MaLuot, x.MaXe, sv.HoTen, x.BienSo, n.ThoiGianGui, n.ThoiGianLay, n.TienPhatSinh " +
                "FROM NhatKyGuiXe n JOIN XeVeThang x ON x.MaXe=n.MaXe " +
                "JOIN SinhVien sv ON sv.MaSV=x.MaSV ORDER BY n.ThoiGianGui DESC");
    }

    public List<Object[]> getVehicleChoices() throws SQLException {
        return query("SELECT x.MaXe, x.BienSo, x.MaSV FROM XeVeThang x ORDER BY x.MaXe");
    }

    public void addParkingLog(int vehicleId, LocalDateTime sentAt) throws SQLException {
        execute("INSERT INTO NhatKyGuiXe (MaXe, ThoiGianGui) VALUES (?, ?)",
                vehicleId, Timestamp.valueOf(sentAt));
    }

    public void deleteParkingLog(int logId) throws SQLException {
        execute("DELETE FROM NhatKyGuiXe WHERE MaLuot=?", logId);
    }

    public List<Object[]> getGuests() throws SQLException {
        return query("SELECT k.MaKhach, k.SoCMT, k.HoTen, k.NgaySinh, k.MaSV, sv.HoTen, k.NgayDen " +
                "FROM KhachTham k JOIN SinhVien sv ON sv.MaSV=k.MaSV ORDER BY k.NgayDen DESC, k.MaKhach DESC");
    }

    public void addGuest(String identityNumber, String name, LocalDate birthDate, String studentId, LocalDate visitDate) throws SQLException {
        execute("INSERT INTO KhachTham (SoCMT, HoTen, NgaySinh, MaSV, NgayDen) VALUES (?, ?, ?, ?, ?)",
                identityNumber, name, Date.valueOf(birthDate), studentId, Date.valueOf(visitDate));
    }

    public void deleteGuest(int guestId) throws SQLException {
        execute("DELETE FROM KhachTham WHERE MaKhach=?", guestId);
    }

    public List<Object[]> getInvoices(int month, int year) throws SQLException {
        if (month < 1 || month > 12 || year < 2000 || year > 2200) throw new SQLException("Tháng hoặc năm không hợp lệ.");
        String sql = "SELECT sv.MaSV, sv.HoTen, COALESCE(p.MaPhong, ''), COALESCE(p.DonGia, 0), " +
                "COALESCE(dv.TienDV, 0), COALESCE(xe.TienXe, 0), COALESCE(park.TienPhatSinh, 0), " +
                "(COALESCE(p.DonGia, 0)+COALESCE(dv.TienDV, 0)+COALESCE(xe.TienXe, 0)+COALESCE(park.TienPhatSinh, 0)) AS Tong " +
                "FROM SinhVien sv LEFT JOIN Phong p ON p.MaPhong=sv.MaPhong " +
                "LEFT JOIN (SELECT sd.MaSV, SUM(sd.SoLuong*d.DonGia) TienDV FROM SuDungDichVu sd " +
                "JOIN DichVu d ON d.MaDV=sd.MaDV WHERE sd.ThoiGianSuDung>=? AND sd.ThoiGianSuDung<? GROUP BY sd.MaSV) dv ON dv.MaSV=sv.MaSV " +
                "LEFT JOIN (SELECT MaSV, SUM(DonGiaThang) TienXe FROM XeVeThang WHERE NgayDangKy<? GROUP BY MaSV) xe ON xe.MaSV=sv.MaSV " +
                "LEFT JOIN (SELECT x.MaSV, SUM(n.TienPhatSinh) TienPhatSinh FROM NhatKyGuiXe n JOIN XeVeThang x ON x.MaXe=n.MaXe " +
                "WHERE n.ThoiGianGui>=? AND n.ThoiGianGui<? GROUP BY x.MaSV) park ON park.MaSV=sv.MaSV ORDER BY sv.MaSV";
        LocalDate first = LocalDate.of(year, month, 1);
        LocalDate next = first.plusMonths(1);
        List<Object[]> rows = query(sql, Timestamp.valueOf(first.atStartOfDay()),
                Timestamp.valueOf(next.atStartOfDay()), Date.valueOf(next),
                Timestamp.valueOf(first.atStartOfDay()), Timestamp.valueOf(next.atStartOfDay()));
        List<Object[]> normalized = new ArrayList<>(rows.size());
        for (Object[] row : rows) normalized.add(new Object[]{
                row[0], row[1], row[2], row[3], row[4], row[5], row[6], row[7]
        });
        return normalized;
    }

    public List<Object[]> reportServiceUsage(LocalDate from, LocalDate to) throws SQLException {
        return query("SELECT sv.MaSV, sv.HoTen, dv.TenDV, SUM(sd.SoLuong), SUM(sd.SoLuong*dv.DonGia) " +
                        "FROM SuDungDichVu sd JOIN SinhVien sv ON sv.MaSV=sd.MaSV JOIN DichVu dv ON dv.MaDV=sd.MaDV " +
                        "WHERE sd.ThoiGianSuDung>=? AND sd.ThoiGianSuDung<? " +
                        "GROUP BY sv.MaSV, sv.HoTen, dv.MaDV, dv.TenDV ORDER BY sv.MaSV, dv.TenDV",
                Timestamp.valueOf(from.atStartOfDay()), Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
    }

    public List<Object[]> reportGuests(LocalDate from, LocalDate to, boolean byWeek) throws SQLException {
        String period = byWeek ? "YEARWEEK(k.NgayDen, 3)" : "DATE_FORMAT(k.NgayDen, '%Y-%m')";
        return query("SELECT " + period + " AS Ky, sv.MaSV, sv.HoTen, k.SoCMT, k.HoTen, COUNT(*) AS SoLan " +
                        "FROM KhachTham k JOIN SinhVien sv ON sv.MaSV=k.MaSV " +
                        "WHERE k.NgayDen>=? AND k.NgayDen<? " +
                        "GROUP BY Ky, sv.MaSV, sv.HoTen, k.SoCMT, k.HoTen ORDER BY Ky DESC, sv.MaSV",
                Date.valueOf(from), Date.valueOf(to.plusDays(1)));
    }

    public List<Object[]> reportRevenue(int month, int year) throws SQLException {
        LocalDate first = LocalDate.of(year, month, 1);
        LocalDate next = first.plusMonths(1);
        return query("SELECT dv.MaDV, dv.TenDV, COALESCE(SUM(sd.SoLuong*dv.DonGia), 0) AS DoanhThu " +
                        "FROM DichVu dv LEFT JOIN SuDungDichVu sd ON sd.MaDV=dv.MaDV " +
                        "AND sd.ThoiGianSuDung>=? AND sd.ThoiGianSuDung<? " +
                        "GROUP BY dv.MaDV, dv.TenDV ORDER BY dv.MaDV",
                Timestamp.valueOf(first.atStartOfDay()), Timestamp.valueOf(next.atStartOfDay()));
    }

    public List<String> getStudentChoices() throws SQLException {
        List<String> ids = new ArrayList<>();
        for (Object[] row : query("SELECT MaSV FROM SinhVien ORDER BY MaSV")) ids.add(row[0].toString());
        return ids;
    }

    private List<Object[]> query(String sql, Object... values) throws SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, values);
            try (ResultSet result = statement.executeQuery()) {
                List<Object[]> rows = new ArrayList<>();
                int columns = result.getMetaData().getColumnCount();
                while (result.next()) {
                    Object[] row = new Object[columns];
                    for (int column = 1; column <= columns; column++) row[column - 1] = result.getObject(column);
                    rows.add(row);
                }
                return rows;
            }
        }
    }

    private void execute(String sql, Object... values) throws SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, values);
            requireUpdated(statement.executeUpdate(), "Không tìm thấy bản ghi cần thay đổi.");
        }
    }

    private boolean hasRows(String sql, Object... values) throws SQLException {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, values);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private void bind(PreparedStatement statement, Object... values) throws SQLException {
        for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
    }

    private void requireUpdated(int count, String message) throws SQLException {
        if (count == 0) throw new SQLException(message);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
