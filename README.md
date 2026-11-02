# Quản lý Ký túc xá Sinh viên

**Bãi đỗ xe và tài sản:** chọn phòng để xem riêng tài sản; quầy gửi/lấy xe chung cho vé tháng và vé lượt dùng giờ thực tế, không nhập giờ vào/ra. Xem [hướng dẫn mới](docs/BAI_DO_XE_VA_TAI_SAN.md). Bản này không thêm SQL nếu đã nâng cấp 003.

**Cập nhật nghiệp vụ 003:** mẫu phòng 4–2–2 người và tài sản mặc định, vé xe theo khung ngày/đêm, điện nước theo tháng sử dụng, thanh toán toàn bộ/một phần và ba mức vi phạm. **CSDL đang dùng cần chạy thêm [003_business_updates.sql](003_business_updates.sql)** rồi khởi động lại ứng dụng. Xem [hướng dẫn chi tiết](docs/CAP_NHAT_NGHIEP_VU_003.md).

**Giao diện mới:** menu bên trái, tìm kiếm/lọc sinh viên, bảng dễ đọc và cổng sinh viên riêng. Xem [hướng dẫn giao diện mới](docs/GIAO_DIEN_MOI.md). Khởi động lại ứng dụng bằng `./run.ps1` để dùng bản mới; cần chạy migration 003 cho phiên bản hiện tại.

Đã bổ sung **10 tính năng mở rộng**: đăng nhập/phân quyền, đơn đăng ký lưu trú, hợp đồng, chuyển phòng, sửa chữa, tài sản, điện nước, công nợ/thanh toán, thông báo/nhắc hạn và vi phạm. Xem [hướng dẫn cài đặt và demo](docs/TINH_NANG_MO_RONG.md).

**Nâng cấp CSDL hiện có:** nếu đã dùng bản mở rộng 002, chỉ chạy thêm `003_business_updates.sql`. Cài mới thì chạy `datasinhvien.sql`, `002_extensions.sql` rồi `003_business_updates.sql`. Lần đầu mở ứng dụng sẽ yêu cầu tạo tài khoản quản trị viên.

Ứng dụng desktop Java Swing theo DAO pattern, dùng JDBC để thao tác với MySQL. Giao diện có các màn hình quản lý sinh viên, phòng, dịch vụ, lượt sử dụng dịch vụ, xe vé tháng, khách đến thăm, hóa đơn theo tháng và báo cáo.

## Yêu cầu

- JDK 17 trở lên
- Maven 3.9 trở lên
- MySQL 8.0 trở lên

## Cài đặt cơ sở dữ liệu

Chạy toàn bộ script `datasinhvien.sql` trong MySQL Workbench hoặc MySQL CLI. Script tạo cơ sở dữ liệu `QuanLyKyTucXa`, các bảng, trigger giới hạn sức chứa phòng/số xe và dữ liệu mẫu.

## Cấu hình kết nối

Mặc định ứng dụng kết nối:

```text
jdbc:mysql://localhost:3306/QuanLyKyTucXa
user: root
password: (trống)
```

Có thể ghi đè thông tin kết nối bằng biến môi trường:

- `KTX_DB_URL` — JDBC URL (bao gồm tên database)
- `KTX_DB_USER` — tài khoản MySQL
- `KTX_DB_PASSWORD` — mật khẩu MySQL

Ví dụ PowerShell:

```powershell
$env:KTX_DB_URL = "jdbc:mysql://localhost:3306/QuanLyKyTucXa?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh"
$env:KTX_DB_USER = "root"
$env:KTX_DB_PASSWORD = "mat-khau-cua-ban"
mvn clean package
mvn exec:java
```

Maven tải MySQL Connector/J và JCalendar (JDateChooser) theo `pom.xml`.

## Sử dụng

- **Sinh viên:** thêm, sửa, xóa và tìm theo mã, họ tên hoặc lớp. Khi chọn phòng, ứng dụng kiểm tra sức chứa; trigger trong MySQL tiếp tục bảo vệ dữ liệu ở phía cơ sở dữ liệu.
- **Phòng ở / Dịch vụ:** xem, thêm, sửa và xóa danh mục. Ứng dụng chặn xóa phòng còn sinh viên, dịch vụ đã có lịch sử sử dụng và xe đã có nhật ký gửi để tránh mất dữ liệu lịch sử.
- **Nghiệp vụ:** ghi nhận/xóa lượt sử dụng dịch vụ, đăng ký/xóa xe vé tháng, ghi nhận/xóa lượt gửi xe và lượt khách.
- **Hóa đơn tháng:** tính theo từng sinh viên, xuất danh sách ra tệp văn bản UTF-8 hoặc in toàn bộ danh sách / dòng hóa đơn được chọn. Tiền phòng tính nguyên tháng; dịch vụ tính theo số lượng sử dụng; xe vé tháng tính theo đơn giá xe đăng ký trước ngày đầu tháng kế tiếp; phí phát sinh lấy từ `NhatKyGuiXe.TienPhatSinh`.
- **Báo cáo:** tổng hợp sử dụng dịch vụ theo khoảng ngày; đếm khách theo tuần ISO hoặc tháng; tổng doanh thu từng dịch vụ theo tháng.

Phí xe phát sinh 3.000 đồng từ lượt thứ ba trong ngày được trigger `chk_TinhPhiGuiXe` tính khi ghi nhận lượt gửi xe trong ứng dụng.

## Cấu trúc chính

```text
src/main/java/vn/ktx/
  Main.java
  config/Database.java
  dao/KtxDao.java
  model/Student.java
  model/Room.java
  model/Service.java
  model/Invoice.java
  ui/MainFrame.java
```
