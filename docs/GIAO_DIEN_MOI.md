# Giao diện KTX Manager

Giao diện desktop đã được đổi theo bản thiết kế xem trước: menu bên trái, nền sáng, màu xanh chủ đạo, chữ Segoe UI, bảng rộng với dòng cao và trạng thái màu có nhãn tiếng Việt. Các biểu mẫu dùng chung kiểu chữ, nút bấm và bộ chọn ngày.

## Cách chạy

Đóng cửa sổ ứng dụng cũ rồi chạy `./run.ps1` trong thư mục chứa `pom.xml`, hoặc chạy lại `mvn compile exec:java`/chạy lớp `vn.ktx.Main` trong IDE. Không cần chạy thêm SQL nếu trước đó đã nâng cấp bằng `migrations/002_extensions.sql`.

## Điều hướng

- **Tổng quan:** số sinh viên đang ở, số phòng còn chỗ và tổng đơn đăng ký/chuyển phòng đang chờ duyệt. Các số liệu được truy vấn từ CSDL, không dùng số minh họa.
- **Quản lý:** Sinh viên, Phòng ở, Hợp đồng, Dịch vụ & gửi xe, Khách đến thăm.
- **Yêu cầu:** Đăng ký & chuyển phòng, Sửa chữa & tài sản.
- **Tài chính:** Hóa đơn & thanh toán, Điện nước, Báo cáo.
- **Hệ thống:** Thông báo & vi phạm, Nhắc hạn, Tài khoản (chỉ quản trị viên).

Các chức năng liên quan nằm trong các tab ngang của cùng một mục. Menu có thanh cuộn khi chiều cao cửa sổ không đủ. Tên tài khoản và nút Đăng xuất nằm dưới cùng; đăng xuất sẽ trở về màn hình đăng nhập.

## Sinh viên

Gõ mã, họ tên hoặc lớp để lọc ngay trên danh sách đã tải; kết hợp với bộ lọc phòng. Bảng chính chỉ hiển thị 6 cột cần dùng thường xuyên. Thông tin CCCD, ngày sinh và quê quán vẫn nằm trong hồ sơ; chọn **Xem hồ sơ** hoặc nhấp đúp dòng để xem. Khi chọn một sinh viên, thanh dưới bảng cho phép chỉnh sửa, gửi yêu cầu chuyển phòng hoặc xóa.

Tìm kiếm/lọc chỉ thay đổi danh sách hiển thị. Việc chỉnh sửa/xóa luôn lấy bản ghi gốc tương ứng, kể cả sau khi sắp xếp. Dòng trạng thái phía dưới hiển thị tổng số kết quả đang nhìn thấy.

## Các màn hình khác

- Nút tạo mới được ghi rõ nghiệp vụ: Lập hợp đồng, Báo hỏng, Lập phí điện nước, Ghi nhận thanh toán…
- Chọn **Xem chi tiết** để đọc đầy đủ mô tả dài, nội dung thông báo hoặc biên bản.
- Ngày trong bảng hiển thị `dd/MM/yyyy`; biểu mẫu nghiệp vụ có lịch chọn ngày. Riêng ô nhập kỳ trên màn hình công nợ vẫn dùng `yyyy-MM-01` như nhãn hướng dẫn.
- Tiền hiển thị có dấu phân cách hàng nghìn; khi nhập số tiền không gõ dấu phân cách. Nhấp tiêu đề cột để sắp xếp; cột tiền được sắp theo giá trị số.
- Dữ liệu bảng và thẻ tổng quan được tải ở luồng nền; cửa sổ vẫn có thể thao tác trong lúc tải. Bảng trống phân biệt trạng thái đang tải và chưa có dữ liệu.
- Nhấn Làm mới hoặc mở lại mục để lấy thay đổi từ CSDL. Giao diện chỉ hiển thị dữ liệu và chức năng đúng vai trò đã đăng nhập.

## Cổng sinh viên

Sinh viên có menu riêng cho hợp đồng, đăng ký lưu trú, chuyển phòng, công nợ, lịch sử thanh toán, điện nước, báo hỏng, tài sản, thông báo, vi phạm và nhắc hạn. Không có menu quản lý tài khoản hoặc danh sách hồ sơ của sinh viên khác. Quyền xử lý vẫn được kiểm tra trong DAO.

Ảnh xuất từ giao diện Swing thật dùng dữ liệu kiểm thử: `output/ui/quan-ly-sinh-vien.png`.

## Kiểm chứng

Biên dịch Java 17, kiểm thử nghiệp vụ/phân quyền bằng `ExtensionIntegrationTest`, kiểm tra giao diện và xuất ảnh bằng `UiRegressionTest`. Các kiểm thử CSDL chỉ chạy trên máy chủ thử nghiệm riêng ở cổng 33317 với `KTX_RUN_INTEGRATION=1`. Không chạy các kiểm thử này trên CSDL đang sử dụng. Máy chủ dùng để kiểm chứng là MariaDB 10.4.32 qua MySQL Connector/J 9.4.0.

Kết quả ngày 01/11/2026: **46 kiểm tra nghiệp vụ/phân quyền và 17 kiểm tra giao diện đạt**, gồm tải nền, lọc/tìm sinh viên, giữ hồ sơ đầy đủ khi ẩn cột, chọn dòng sau sắp xếp, sắp xếp tiền theo số, hiển thị trạng thái tiếng Việt, menu theo vai trò và bố cục ở hai kích thước. Ảnh được render từ thành phần Swing thật mà không mở cửa sổ kiểm thử lên màn hình người dùng. Chưa tự động hóa thao tác chuột trên toàn bộ hộp thoại.
