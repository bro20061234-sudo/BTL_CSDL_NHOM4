# Cập nhật nghiệp vụ 003

**Giao diện mới hơn:** hai màn hình gửi xe được gộp thành **Bãi đỗ xe**, tự ghi thời gian thực khi bấm gửi/lấy. Tài sản có bộ lọc phòng. Xem `BAI_DO_XE_VA_TAI_SAN.md`; mục 2 bên dưới mô tả cách tính phí, các ô nhập thời gian và tab cũ đã được thay thế.

## Nâng cấp trong HeidiSQL

Nếu đã chạy bản trước, chỉ mở **003_business_updates.sql** ở thư mục gốc dự án, chọn toàn bộ và nhấn **F9** trong kết nối đang chứa `QuanLyKyTucXa`. Sau đó đóng ứng dụng cũ, chạy lại `./run.ps1` hoặc chạy lại lớp `vn.ktx.Main` trong IDE.

Cài mới thì chạy lần lượt `datasinhvien.sql`, `002_extensions.sql`, `003_business_updates.sql`. Không chạy lại file tạo dữ liệu mẫu trên CSDL đang sử dụng. Script 003 bổ sung cột/bảng/trigger, không xóa phòng, sinh viên, phí hay thanh toán cũ; có thể chạy lại. Cần quyền CREATE ROUTINE, CREATE TRIGGER và ALTER TABLE.

## 1. Chọn mẫu phòng và tạo tài sản mặc định

Mở **Phòng ở → Thêm phòng**. Chỉ cần nhập mã/số phòng, chọn một trong ba mẫu; sức chứa và danh sách tài sản được điền tự động.

| Loại phòng | Sức chứa | Tài sản tạo khi thêm phòng | Giá gợi ý/tháng |
|---|---:|---|---:|
| Phòng tiêu chuẩn | 4 | 1 quạt, 1 máy lạnh, 4 giường | 1.200.000đ |
| Phòng cao cấp | 2 | 1 quạt, 1 máy lạnh, 2 giường, 2 tủ quần áo, 2 bàn học | 2.000.000đ |
| Phòng VIP | 2 | Như cao cấp, thêm 1 tủ lạnh, 1 máy nước nóng | 3.000.000đ |

Giá trên là giá gợi ý và vẫn sửa được trước khi lưu. Xem các đồ được tạo tại **Sửa chữa & tài sản → Tài sản phòng**. Thêm phòng và tạo tài sản là cùng một giao dịch: có lỗi thì không lưu dở phòng. Sửa thông tin phòng không nhân đôi tài sản cùng tên; đổi mẫu chỉ bổ sung tài sản còn thiếu, không tự xóa tài sản đã bàn giao hoặc đổi số lượng đang theo dõi. Phòng cũ giữ nguyên tên loại, sức chứa và tài sản tới khi người quản lý chủ động sửa; không được giảm sức chứa xuống dưới số sinh viên hiện đang ở.

## 2. Gửi xe không vé tháng

Mở **Dịch vụ & gửi xe → Xe không vé tháng**:

- Nhập biển số và thời gian gửi, chọn **Gửi xe** để tạo lượt tạm trong bảng.
- Khi lấy xe, chọn đúng dòng, nhập thời gian lấy và chọn **Lấy xe / thu tiền**.
- Có thể chỉ nhập hai mốc thời gian rồi chọn **Tính tiền lượt gửi** để báo giá trước.

Mỗi khung ngày **[06:00,18:00)** là **3.000đ**; mỗi khung đêm **[18:00,06:00 hôm sau)** là **5.000đ**. Cộng từng khung có thời gian gửi xe, không tính thêm khung bắt đầu đúng lúc lấy xe.

| Gửi → lấy | Số tiền |
|---|---:|
| 06:00 → 18:00 | 3.000đ |
| 18:00 → 06:00 hôm sau | 5.000đ |
| 17:00 → 19:00 | 8.000đ |
| 05:00 → 07:00 | 8.000đ |
| 23:00 → 01:00 hôm sau | 5.000đ |
| 06:00 → 06:00 hôm sau | 8.000đ |

Vé lượt thu trực tiếp, không cộng vào hóa đơn sinh viên. Theo phạm vi đề bài, danh sách này không lưu CSDL: chỉ tồn tại trong phiên làm việc và bị xóa khi đóng ứng dụng. Các xe vé tháng vẫn có danh sách và nhật ký riêng trong CSDL.

## 3. Điện nước theo tháng sử dụng

Mục **Điện nước** dùng bộ chọn **Tháng / Năm**, mặc định tháng trước. Biểu mẫu ghi riêng **Ngày lập** và **Tháng sử dụng**. Đổi ngày lập sẽ gợi ý lại tháng trước ngày đó.

Ví dụ: ngày **01/11/2026** lập phí thì chọn **tháng 10/2026**. Khoản đó được lưu, lọc và cộng vào hóa đơn **tháng 10**, không phải tháng 11. Tương tự, 01/01/2027 mặc định tính tháng 12/2026. Người quản lý có thể chọn một tháng cũ hơn để lập bù; không lập cho tháng chưa kết thúc hoặc ngày lập ở tương lai.

Danh sách người chịu phí lấy từ **lịch sử đã ở phòng trong tháng sử dụng**, bao gồm người đã chuyển hoặc trả phòng trước ngày lập. Chia đều cho mỗi sinh viên từng ở trong tháng, không tính tỷ lệ số ngày. Tổng tiền và quy tắc làm tròn được giữ nguyên; mỗi phòng/tháng chỉ lập một lần. Các phần đã chốt không bị chia lại khi sinh viên chuyển phòng sau đó.

## 4. Thanh toán toàn bộ hoặc một phần

Mở **Hóa đơn & thanh toán → Lịch sử thanh toán → Ghi nhận thanh toán**. Chọn sinh viên và tháng hóa đơn; biểu mẫu hiển thị dư nợ của tháng đó.

- **Thanh toán toàn bộ:** không cần gõ số tiền, hệ thống lấy toàn bộ số còn nợ thực tế tại thời điểm lưu.
- **Thanh toán một phần:** nhập khoản trả lớn hơn 0 và không vượt quá dư nợ. Phần còn lại tiếp tục hiển thị ở công nợ.

Không thu lại tháng đã hết nợ. Mỗi khoản thu vẫn có biên nhận và ngày thu riêng. Sinh viên chỉ xem công nợ/lịch sử, không tự xác nhận đã thanh toán.

## 5. Ba hình thức xử lý vi phạm

Mở **Thông báo & vi phạm → Vi phạm nội quy → Lập biên bản**:

- **Nhắc nhở:** lưu nội dung và ghi chú, tiền phạt bằng 0, không thay đổi phòng/hợp đồng.
- **Phạt hành chính:** nhập tiền phạt lớn hơn 0; cộng vào hóa đơn tháng xảy ra vi phạm, không trả phòng.
- **Đình chỉ lưu trú:** sau hộp thoại xác nhận, hệ thống trả phòng ngay, chuyển hợp đồng hiệu lực sang Đình chỉ, kết thúc vé tháng, từ chối các đơn đang chờ và chặn đăng ký/xếp phòng lại. Đây là chấm dứt lưu trú, không phải tạm ngưng vài ngày.

Đình chỉ không xóa hồ sơ, lịch sử lưu trú, tiền phòng đã phát sinh, phí điện nước hoặc công nợ. Vẫn được thu nợ cũ. Từ tháng sau không thu tiếp tiền phòng/vé tháng đã chấm dứt. Các thao tác này nằm trong cùng một giao dịch; trigger CSDL tiếp tục chặn việc gán lại phòng hoặc đăng ký vé tháng cho người đã đình chỉ.

## Kiểm chứng

Kiểm thử riêng trên MariaDB 10.4.32 qua MySQL Connector/J, cổng 33317; không tác động CSDL đang dùng. Chạy theo thứ tự `ExtensionIntegrationTest`, `BusinessUpdatesTest`, `UiRegressionTest` sau khi import đủ ba script. Các kiểm thử yêu cầu `KTX_RUN_INTEGRATION=1` và URL trỏ tới cổng thử nghiệm. Kiểm tra có các mốc 6h/18h, qua đêm/nhiều ngày, tháng sử dụng/ngày lập, cư dân lịch sử, thu một phần/toàn bộ, kết thúc lưu trú và bảo vệ tại CSDL.

Kết quả ngày 02/11/2026: biên dịch thành công với `--release 17`; **46 kiểm tra hồi quy, 64 kiểm tra nghiệp vụ mới và 23 kiểm tra giao diện đạt**. Các nút gửi/lấy xe vé lượt và bộ chọn tháng đã được kiểm tra trên thành phần Swing thật; thanh công cụ xuống dòng không che nút ở chiều rộng hẹp. Chạy lại migration giữ nguyên 4 bản ghi thanh toán và 1 hồ sơ đình chỉ trong CSDL thử nghiệm. Chưa kiểm chứng trên máy chủ MySQL 8 thực tế.
