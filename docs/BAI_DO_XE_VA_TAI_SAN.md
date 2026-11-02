# Chọn phòng xem tài sản và quầy bãi đỗ xe

## Tài sản theo phòng

Vào **Sửa chữa & tài sản → Tài sản phòng**, chọn mã phòng trong bộ lọc phía trên. Bảng chỉ tải tài sản của phòng được chọn; chưa chọn thì không liệt kê tài sản cả KTX. Khi thêm tài sản, phòng được cố định theo bộ lọc. Sinh viên chỉ được chọn và xem phòng mình đang ở; DAO vẫn kiểm tra quyền khi truy vấn.

## Quầy gửi/lấy xe

Vào **Dịch vụ & gửi xe → Bãi đỗ xe**. Hai tab cũ Nhật ký gửi xe và Xe không vé tháng đã được gộp vào đây; tab Xe vé tháng vẫn dùng để đăng ký/quản lý vé.

1. Nhập biển số rồi bấm **Gửi xe** hoặc Enter. Hệ thống tự ghi giờ hiện tại, kiểm tra vé tháng còn hiệu lực và đưa xe vào bảng.
2. Bảng chỉ hiện xe **đang trong bãi**, gồm biển số, loại vé, sinh viên/khách, giờ vào, thời gian đã gửi và phí hiện tại. Có ô tìm biển số. Không cần nhập ngày/giờ gửi hoặc lấy.
3. Khi xe ra, chọn dòng xe và bấm **Lấy xe / tính tiền**. Hệ thống lấy giờ hiện tại để chốt tiền, thông báo số tiền và đưa xe ra khỏi bảng. Không thể xử lý lại lượt đã lấy.

Phí hiện tại và đồng hồ cập nhật mỗi giây. Danh sách vé tháng tải lại từ CSDL mỗi 10 giây khi mở màn hình, hoặc bấm Làm mới. Sắp xếp/tìm kiếm bảng không làm đổi xe được chọn để lấy.

### Cách tính phí

- **Vé lượt:** mỗi khung 06h–18h là 3.000đ, 18h–06h hôm sau là 5.000đ; cộng các khung đã gửi. Ví dụ 17h vào, 19h lấy: 8.000đ. Tiền thu tại quầy.
- **Vé tháng:** giữ phí phát sinh do trigger tính theo số lượt gửi trong ngày; giờ lấy được cập nhật vào nhật ký. Phí phát sinh vẫn vào hóa đơn sinh viên, không ghi thêm một khoản thu tiền mặt để tránh thu hai lần.
- Vé tháng hết hạn hoặc bị đình chỉ không được nhận diện thành vé tháng hợp lệ. Biển số có nhiều đăng ký tháng hợp lệ cần sửa dữ liệu trùng trước khi gửi.

Nhật ký vé tháng vẫn lưu trong CSDL kể cả sau khi xe ra. Theo phạm vi đề bài, xe vé lượt không lưu CSDL: danh sách giữ trong bộ nhớ trong suốt phiên ứng dụng (kể cả đăng xuất/đăng nhập lại), chỉ bị xóa khi thoát hẳn ứng dụng. Không dùng danh sách vé lượt này để quản lý bãi qua lần khởi động lại.

## Chạy bản mới

Nếu đã nâng cấp CSDL đến **003_business_updates.sql**, không cần thêm SQL cho thay đổi này. Đóng ứng dụng cũ và chạy lại `./run.ps1` hoặc lớp `vn.ktx.Main` trong IDE.

## Kiểm chứng

Ngày 02/11/2026: biên dịch Java 17; **46 kiểm tra hồi quy + 64 kiểm tra nghiệp vụ 003 + 32 kiểm tra bãi xe/lọc tài sản + 22 kiểm tra giao diện đạt**. Kiểm tra riêng trên MariaDB 10.4.32, cổng 33317. `ParkingFlowTest` dùng đồng hồ có thể điều khiển để kiểm tra qua 18h, thời điểm xe ra, lấy ngay, đồng hồ lùi, gửi trùng, xử lý lại lượt đã lấy, vé tháng miễn phí/phát sinh và các nút Swing thật. Chưa kiểm chứng trên máy chủ MySQL 8 thực tế.
