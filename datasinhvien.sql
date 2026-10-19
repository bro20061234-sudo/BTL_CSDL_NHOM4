CREATE DATABASE IF NOT EXISTS QuanLyKyTucXa
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE QuanLyKyTucXa;

-- ========================================================
-- 1. TẠO BẢNG DỮ LIỆU
-- ========================================================

-- Bảng Phòng ở
CREATE TABLE Phong (
    MaPhong VARCHAR(10) PRIMARY KEY,
    SoPhong VARCHAR(10) NOT NULL,
    LoaiPhong VARCHAR(50) NOT NULL,
    DonGia DECIMAL(12, 2) NOT NULL,
    SoNguoiToiDa INT NOT NULL
);

-- Bảng Sinh viên
CREATE TABLE SinhVien (
    MaSV VARCHAR(10) PRIMARY KEY,
    SoCMT VARCHAR(12) UNIQUE NOT NULL,
    HoTen VARCHAR(100) NOT NULL,
    NgaySinh DATE NOT NULL,
    Lop VARCHAR(50) NOT NULL,
    QueQuan VARCHAR(100) NOT NULL,
    MaPhong VARCHAR(10),
    NgayVaoKTX DATE NOT NULL,
    FOREIGN KEY (MaPhong) REFERENCES Phong(MaPhong) ON DELETE SET NULL
);

-- Bảng Khách đến thăm
CREATE TABLE KhachTham (
    MaKhach INT AUTO_INCREMENT PRIMARY KEY,
    SoCMT VARCHAR(12) NOT NULL,
    HoTen VARCHAR(100) NOT NULL,
    NgaySinh DATE NOT NULL,
    MaSV VARCHAR(10) NOT NULL,
    NgayDen DATE NOT NULL,
    FOREIGN KEY (MaSV) REFERENCES SinhVien(MaSV) ON DELETE CASCADE
);

-- Bảng Dịch vụ cơ bản
CREATE TABLE DichVu (
    MaDV VARCHAR(10) PRIMARY KEY,
    TenDV VARCHAR(100) NOT NULL,
    DonGia DECIMAL(12, 2) NOT NULL
);

-- Bảng Sử dụng Dịch vụ cơ bản
CREATE TABLE SuDungDichVu (
    MaSD INT AUTO_INCREMENT PRIMARY KEY,
    MaSV VARCHAR(10) NOT NULL,
    MaDV VARCHAR(10) NOT NULL,
    ThoiGianSuDung DATETIME NOT NULL,
    SoLuong INT DEFAULT 1,
    FOREIGN KEY (MaSV) REFERENCES SinhVien(MaSV) ON DELETE CASCADE,
    FOREIGN KEY (MaDV) REFERENCES DichVu(MaDV) ON DELETE CASCADE
);

-- Bảng Đăng ký Xe vé tháng
CREATE TABLE XeVeThang (
    MaXe INT AUTO_INCREMENT PRIMARY KEY,
    MaSV VARCHAR(10) NOT NULL,
    BienSo VARCHAR(20) NOT NULL,
    LoaiXe VARCHAR(50) DEFAULT 'Xe may',
    NgayDangKy DATE NOT NULL,
    DonGiaThang DECIMAL(12, 2) DEFAULT 100000.00,
    FOREIGN KEY (MaSV) REFERENCES SinhVien(MaSV) ON DELETE CASCADE
);

-- Bảng Nhật ký Lượt gửi/lấy xe vé tháng
CREATE TABLE NhatKyGuiXe (
    MaLuot INT AUTO_INCREMENT PRIMARY KEY,
    MaXe INT NOT NULL,
    ThoiGianGui DATETIME NOT NULL,
    ThoiGianLay DATETIME NULL,
    TienPhatSinh DECIMAL(12, 2) DEFAULT 0.00,
    FOREIGN KEY (MaXe) REFERENCES XeVeThang(MaXe) ON DELETE CASCADE
);

-- ========================================================
-- 2. TẠO TRIGGER VÀ RÀNG BUỘC KĨ THUẬT (RÀNG BUỘC KTX)
-- ========================================================

DELIMITER //

-- Ràng buộc 1: Kiểm tra số người ở tối đa trong phòng trước khi thêm/sửa SV
CREATE TRIGGER chk_MaxPhong_Insert BEFORE INSERT ON SinhVien
FOR EACH ROW
BEGIN
    DECLARE current_count INT;
    DECLARE max_count INT;
    
    IF NEW.MaPhong IS NOT NULL THEN
        SELECT COUNT(*) INTO current_count FROM SinhVien WHERE MaPhong = NEW.MaPhong;
        SELECT SoNguoiToiDa INTO max_count FROM Phong WHERE MaPhong = NEW.MaPhong;
        
        IF current_count >= max_count THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Lỗi: Phòng đã đạt số lượng người ở tối đa!';
        END IF;
    END IF;
END //

CREATE TRIGGER chk_MaxPhong_Update BEFORE UPDATE ON SinhVien
FOR EACH ROW
BEGIN
    DECLARE current_count INT;
    DECLARE max_count INT;
    
    IF NEW.MaPhong IS NOT NULL AND (OLD.MaPhong IS NULL OR NEW.MaPhong <> OLD.MaPhong) THEN
        SELECT COUNT(*) INTO current_count FROM SinhVien WHERE MaPhong = NEW.MaPhong;
        SELECT SoNguoiToiDa INTO max_count FROM Phong WHERE MaPhong = NEW.MaPhong;
        
        IF current_count >= max_count THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Lỗi: Phòng đã đạt số lượng người ở tối đa!';
        END IF;
    END IF;
END //

-- Ràng buộc 2: Mỗi sinh viên chỉ được đăng ký tối đa 2 xe vé tháng
CREATE TRIGGER chk_MaxXe_Insert BEFORE INSERT ON XeVeThang
FOR EACH ROW
BEGIN
    DECLARE xe_count INT;
    SELECT COUNT(*) INTO xe_count FROM XeVeThang WHERE MaSV = NEW.MaSV;
    IF xe_count >= 2 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Lỗi: Mỗi sinh viên chỉ được đăng ký tối đa 2 xe vé tháng!';
    END IF;
END //

-- Ràng buộc 3: Tự động tính phí 3.000đ từ lượt gửi/lấy thứ 3 trong ngày của xe
CREATE TRIGGER chk_TinhPhiGuiXe BEFORE INSERT ON NhatKyGuiXe
FOR EACH ROW
BEGIN
    DECLARE luot_trong_ngay INT;
    SELECT COUNT(*) INTO luot_trong_ngay 
    FROM NhatKyGuiXe 
    WHERE MaXe = NEW.MaXe AND DATE(ThoiGianGui) = DATE(NEW.ThoiGianGui);
    
    -- Từ lượt thứ 3 trong ngày trở đi tính 3000 VNĐ
    IF luot_trong_ngay >= 2 THEN
        SET NEW.TienPhatSinh = 3000.00;
    ELSE
        SET NEW.TienPhatSinh = 0.00;
    END IF;
END //

DELIMITER ;

-- ========================================================
-- 3. NHẬP DỮ LIỆU MẪU (DUMMY DATA)
-- ========================================================

INSERT INTO Phong VALUES 
('P101', '101', 'Tieu chuan 4 nguoi', 1200000, 4),
('P102', '102', 'Chat luong cao 2 nguoi', 2000000, 2);

INSERT INTO SinhVien VALUES 
('SV001', '001203001111', 'Nguyen Van A', '2003-05-15', 'CNTT1', 'Ha Noi', 'P101', '2024-09-01'),
('SV002', '001203002222', 'Tran Thi B', '2003-08-20', 'CNTT2', 'Nam Dinh', 'P101', '2024-09-01'),
('SV003', '001203003333', 'Le Van C', '2003-12-10', 'ATTT1', 'Thanh Hoa', 'P102', '2024-09-01');

INSERT INTO DichVu VALUES 
('DV01', 'Giat la', 15000),
('DV02', 'Trong xe ngoai gio', 5000),
('DV03', 'Cho thue xe may', 50000),
('DV04', 'Suat an KTX', 30000);

INSERT INTO SuDungDichVu (MaSV, MaDV, ThoiGianSuDung, SoLuong) VALUES 
('SV001', 'DV01', '2026-10-05 08:30:00', 3),
('SV001', 'DV04', '2026-10-06 12:00:00', 2),
('SV002', 'DV03', '2026-10-10 14:00:00', 1),
('SV003', 'DV01', '2026-10-12 09:00:00', 5);

INSERT INTO KhachTham (SoCMT, HoTen, NgaySinh, MaSV, NgayDen) VALUES 
('036098000111', 'Nguyen Van D', '1980-01-01', 'SV001', '2026-10-02'),
('036098000111', 'Nguyen Van D', '1980-01-01', 'SV001', '2026-10-15'),
('035098000222', 'Pham Thi E', '1985-03-12', 'SV002', '2026-10-10');

INSERT INTO XeVeThang (MaSV, BienSo, LoaiXe, NgayDangKy) VALUES 
('SV001', '29B1-12345', 'Xe may', '2026-09-01'),
('SV001', '29B1-99999', 'Xe may', '2026-09-01'),
('SV002', '18B2-54321', 'Xe may', '2026-09-01');

-- Lượt gửi xe trong ngày 2026-10-18 của xe SV001 (xử lý phát sinh phí từ lượt 3)
INSERT INTO NhatKyGuiXe (MaXe, ThoiGianGui, ThoiGianLay) VALUES 
(1, '2026-10-18 07:00:00', '2026-10-18 09:00:00'),
(1, '2026-10-18 10:00:00', '2026-10-18 11:30:00'),
(1, '2026-10-18 13:00:00', '2026-10-18 15:00:00'), -- Phát sinh 3000đ
(1, '2026-10-18 17:00:00', '2026-10-18 19:00:00'); -- Phát sinh 3000đ

-- ========================================================
-- 4. CÁC CÂU LỆNH TRUY VẤN THEO YÊU CẦU
-- ========================================================

-- Yêu cầu 1: Liệt kê thông tin SV cùng số tiền trả cho tất cả dịch vụ (gồm cả tiền phòng và phí xe) theo tháng.
SELECT 
    sv.MaSV,
    sv.HoTen,
    p.MaPhong,
    CONCAT(LPAD(ThangBang.Thang, 2, '0'), '/', ThangBang.Nam) AS ThangNam,
    COALESCE(p.DonGia, 0) AS TienPhong,
    COALESCE(SUM(sd.SoLuong * dv.DonGia), 0) AS TienDichVuCoBan,
    COALESCE(TienXe.TienXeThang, 0) AS TienVeThangXe,
    COALESCE(TienPhatSinhXe.TongTienPhatSinh, 0) AS TienGuiXePhatSinh,
    (
        COALESCE(p.DonGia, 0) + 
        COALESCE(SUM(sd.SoLuong * dv.DonGia), 0) + 
        COALESCE(TienXe.TienXeThang, 0) + 
        COALESCE(TienPhatSinhXe.TongTienPhatSinh, 0)
    ) AS TongTienPhaiTra
FROM SinhVien sv
LEFT JOIN Phong p ON sv.MaPhong = p.MaPhong
CROSS JOIN (
    SELECT 10 AS Thang, 2026 AS Nam -- Tháng/Năm cần thống kê
) ThangBang
LEFT JOIN SuDungDichVu sd ON sv.MaSV = sd.MaSV 
    AND MONTH(sd.ThoiGianSuDung) = ThangBang.Thang 
    AND YEAR(sd.ThoiGianSuDung) = ThangBang.Nam
LEFT JOIN DichVu dv ON sd.MaDV = dv.MaDV
LEFT JOIN (
    SELECT MaSV, SUM(DonGiaThang) AS TienXeThang 
    FROM XeVeThang 
    GROUP BY MaSV
) TienXe ON sv.MaSV = TienXe.MaSV
LEFT JOIN (
    SELECT x.MaSV, SUM(nk.TienPhatSinh) AS TongTienPhatSinh
    FROM NhatKyGuiXe nk
    JOIN XeVeThang x ON nk.MaXe = x.MaXe
    WHERE MONTH(nk.ThoiGianGui) = 10 AND YEAR(nk.ThoiGianGui) = 2026
    GROUP BY x.MaSV
) TienPhatSinhXe ON sv.MaSV = TienPhatSinhXe.MaSV
GROUP BY sv.MaSV, sv.HoTen, p.MaPhong, p.DonGia, ThangBang.Thang, ThangBang.Nam, TienXe.TienXeThang, TienPhatSinhXe.TongTienPhatSinh;


-- Yêu cầu 2: Liệt kê thông tin SV cùng tên dịch vụ, tổng giá mỗi dịch vụ đã dùng trong khoảng thời gian (Từ ngày A đến ngày B).
SELECT 
    sv.MaSV,
    sv.HoTen,
    dv.TenDV,
    SUM(sd.SoLuong) AS TongSoLuong,
    SUM(sd.SoLuong * dv.DonGia) AS TongTienDichVu
FROM SinhVien sv
JOIN SuDungDichVu sd ON sv.MaSV = sd.MaSV
JOIN DichVu dv ON sd.MaDV = dv.MaDV
WHERE sd.ThoiGianSuDung BETWEEN '2026-10-01 00:00:00' AND '2026-10-31 23:59:59'
GROUP BY sv.MaSV, sv.HoTen, dv.MaDV, dv.TenDV;


-- Yêu cầu 3: Liệt kê thông tin SV cùng thông tin khách đến thăm trong khoảng thời gian (tháng/tuần) cùng số lần mỗi khách đến.
SELECT 
    sv.MaSV,
    sv.HoTen AS TenSinhVien,
    kt.SoCMT AS CMT_Khach,
    kt.HoTen AS TenKhach,
    kt.NgaySinh AS NgaySinhKhach,
    COUNT(kt.MaKhach) AS SoLanDenTham
FROM SinhVien sv
JOIN KhachTham kt ON sv.MaSV = kt.MaSV
WHERE kt.NgayDen BETWEEN '2026-10-01' AND '2026-10-31'
GROUP BY sv.MaSV, sv.HoTen, kt.SoCMT, kt.HoTen, kt.NgaySinh;


-- Yêu cầu 4: Liệt kê danh mục dịch vụ cùng doanh thu từng dịch vụ trong tháng.
SELECT 
    dv.MaDV,
    dv.TenDV,
    COALESCE(SUM(sd.SoLuong * dv.DonGia), 0) AS DoanhThuDichVu
FROM DichVu dv
LEFT JOIN SuDungDichVu sd ON dv.MaDV = sd.MaDV 
    AND MONTH(sd.ThoiGianSuDung) = 10 
    AND YEAR(sd.ThoiGianSuDung) = 2026
GROUP BY dv.MaDV, dv.TenDV;