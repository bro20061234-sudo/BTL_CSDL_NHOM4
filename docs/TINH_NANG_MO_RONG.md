# 10 tính năng bổ sung cho bài tập quản lý KTX

Ứng dụng vẫn dùng Java Swing, JDBC và MySQL. Các tính năng mở rộng được đặt trực tiếp trong menu bên trái, chia nhóm Quản lý, Yêu cầu, Tài chính và Hệ thống. Tài khoản sinh viên được mở thẳng vào cổng sinh viên và chỉ được truy vấn dữ liệu thuộc mình. Xem `GIAO_DIEN_MOI.md` để biết bố cục hiện tại.

## Nâng cấp và chạy

1. Nếu đã có dữ liệu, giữ nguyên database hiện tại và chạy `002_extensions.sql` (nếu chưa chạy) rồi `003_business_updates.sql` bằng HeidiSQL hoặc MySQL Workbench. Không chạy lại script dữ liệu mẫu trên database đang dùng.
2. Nếu cài mới, chạy `datasinhvien.sql` trước rồi chạy `002_extensions.sql` và `003_business_updates.sql`.
3. Cấu hình `KTX_DB_URL`, `KTX_DB_USER`, `KTX_DB_PASSWORD` như README. Chạy `mvn clean package` và `mvn exec:java`. Có thể dùng `./run.ps1` nếu máy có JDK và các thư viện Maven đã được tải trước.
4. Lần đầu mở ứng dụng, tạo tài khoản quản trị viên với mật khẩu ít nhất 8 ký tự. Không có mật khẩu mặc định. Những lần sau đăng nhập bằng tài khoản đã tạo.
5. Vào **Hệ thống → Tài khoản** trên menu bên trái để tạo tài khoản nhân viên hoặc sinh viên. Tạo hồ sơ sinh viên ở mục Sinh viên trước khi gắn tài khoản sinh viên với mã SV.

Script nâng cấp chỉ thêm bảng, lịch sử lưu trú và trigger mới. Có thể chạy lại script; các bảng và dữ liệu cũ được giữ lại. Tài khoản MySQL chạy migration cần quyền tạo bảng và trigger.

## Chức năng và kịch bản demo

| # | Chức năng | Cách thao tác / kết quả |
|---|---|---|
| 1 | Đăng nhập và phân quyền | ADMIN quản lý tài khoản và mọi nghiệp vụ; STAFF xử lý nghiệp vụ; STUDENT chỉ xem dữ liệu của mình và gửi đăng ký, chuyển phòng, sửa chữa. Mật khẩu lưu bằng PBKDF2-HMAC-SHA256, salt riêng, 210.000 vòng. |
| 2 | Đăng ký lưu trú | Sinh viên có hồ sơ gửi đơn chọn loại phòng, thêm ghi chú. Quản lý chọn phòng đúng loại và còn chỗ để duyệt, hoặc từ chối. Không tạo hai đơn chờ duyệt cho cùng sinh viên. |
| 3 | Hợp đồng lưu trú | Quản lý lập hợp đồng với ngày bắt đầu, ngày kết thúc, tiền cọc và phòng. Kiểm tra sức chứa, chỉ một hợp đồng hiệu lực mỗi sinh viên. Có nút gia hạn và thanh lý/trả phòng; hồ sơ hợp đồng được giữ lại. |
| 4 | Yêu cầu chuyển phòng | Gửi phòng đích và lý do. Phòng cũ, ngày gửi được lưu tự động. Khi duyệt, kiểm tra phòng cũ và sức chứa; cập nhật phòng sinh viên và hợp đồng trong cùng giao dịch. |
| 5 | Báo hỏng / sửa chữa | Sinh viên gửi mô tả và tùy chọn đường dẫn ảnh. Phòng được lấy từ hồ sơ hiện tại. Nhân viên cập nhật theo thứ tự tiếp nhận → đang xử lý → hoàn thành. Có nút xem ảnh. |
| 6 | Quản lý tài sản | Nhập phòng, tài sản, số lượng, tình trạng, nội dung biên bản bàn giao và ngày bàn giao. Quản lý cập nhật tình trạng hỏng/mất; sinh viên xem tài sản phòng mình. |
| 7 | Điện nước | Nhập kỳ, chỉ số cũ/mới và đơn giá. Hệ thống tính `(điện mới − cũ) × giá điện + (nước mới − cũ) × giá nước`, chia đều cho cư dân đã ở phòng trong tháng sử dụng theo lịch sử lưu trú. Chốt phần tiền từng sinh viên để việc chuyển phòng sau đó không làm đổi khoản đã lập. |
| 8 | Công nợ / thanh toán | Xem tổng phải trả, đã trả, còn nợ theo kỳ. Nhân viên ghi nhận từng lần thanh toán, chặn số tiền vượt công nợ; sinh viên xem lịch sử và xuất biên nhận UTF-8. Điện nước và phạt đã được cộng vào hóa đơn gốc và bản in. |
| 9 | Thông báo / nhắc hạn | Tạo thông báo toàn KTX hoặc theo phòng, có nội dung và hạn/ngày thực hiện cho đóng tiền, bảo trì. Mục Nhắc hạn hiển thị hợp đồng quá hạn hoặc còn tối đa 30 ngày và công nợ 3 tháng gần nhất. |
| 10 | Vi phạm nội quy | Nhập sinh viên, ngày vi phạm, nội dung, hình thức xử lý và tiền phạt (có thể bằng 0). Sinh viên xem biên bản của mình. Phạt được cộng vào hóa đơn tháng xảy ra vi phạm. |

## Quy tắc và phạm vi

- Biểu mẫu có lịch chọn ngày; điện nước và thanh toán chọn tháng/năm. Ngày đầu tháng chỉ là dạng lưu SQL bên trong. Nhập số tiền không có dấu phân cách hàng nghìn, tối đa 2 chữ số thập phân. Bản 003 thay thế các quy tắc cũ được mô tả tại `CAP_NHAT_NGHIEP_VU_003.md`.
- Điện nước chặn chỉ số âm, chỉ số mới nhỏ hơn cũ, phòng không có người phân bổ và trùng phòng/kỳ. Phần tiền làm tròn xuống đến 0,01 đồng; số lẻ còn lại cộng vào người cuối theo mã SV để tổng tiền không đổi.
- Khi chuyển phòng trong cùng tháng, tiền thuê cả tháng lấy **mức giá cao nhất trong các phòng đã ở**, thu một lần. Ở một ngày vẫn tính cả tháng. Giá và lịch sử phòng được lưu từ khi nâng cấp để việc trả phòng không xóa tiền thuê đã phát sinh. Tháng đã ở trước khi nâng cấp được khởi tạo theo phòng, ngày vào và giá hiện có; không thể tự khôi phục những lần chuyển phòng trước đó.
- Đơn đăng ký là biểu mẫu trong ứng dụng desktop dành cho sinh viên đã có hồ sơ; chưa có website đăng ký qua Internet.
- Ảnh báo hỏng là tệp ảnh trên máy dùng ứng dụng; không có máy chủ tải ảnh dùng chung.
- Thông báo và nhắc hạn hiển thị trong ứng dụng, cập nhật khi mở tab hoặc bấm Làm mới; chưa gửi SMS/email hay chạy lịch nền. Có thể xem công nợ các tháng xa hơn trong tab Công nợ.
- Tiền cọc được lưu trong hợp đồng để đối soát lúc thanh lý, chưa tự động hoàn cọc hoặc khấu trừ. Biên bản bàn giao và vi phạm được lưu dưới dạng nội dung văn bản.
- Hóa đơn được tính theo dữ liệu nghiệp vụ hiện có; thanh toán được ghi nhận thủ công, không kết nối cổng thanh toán ngân hàng.

## Kiểm thử

`src/test/java/vn/ktx/ExtensionIntegrationTest.java` là kiểm thử tích hợp JDBC chạy bằng Java, không cần JUnit. Chỉ chạy với máy chủ CSDL thử nghiệm riêng ở cổng **33317**, database trống đã import đủ ba script ở trên; không trỏ tới dữ liệu đang sử dụng. Đặt `KTX_RUN_INTEGRATION=1` để xác nhận đây là CSDL thử nghiệm.

```powershell
$env:KTX_DB_URL = 'jdbc:mysql://127.0.0.1:33317/QuanLyKyTucXa?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh'
$env:KTX_DB_USER = 'root'
$env:KTX_DB_PASSWORD = ''
$env:KTX_RUN_INTEGRATION = '1'
$ktxSources = @(Get-ChildItem src/main/java,src/test/java -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
javac -encoding UTF-8 --release 17 -cp 'target/dependency/*' -d target/classes $ktxSources
java '-Dfile.encoding=UTF-8' -cp 'target/classes;target/dependency/*' vn.ktx.ExtensionIntegrationTest
```

Kiểm tra bao gồm đăng nhập/salt mật khẩu, phân quyền, duyệt đơn, phòng đầy/rollback, chuyển phòng và cập nhật hợp đồng, gia hạn/thanh lý, quy trình sửa chữa, lọc tài sản/thông báo theo phòng, phân bổ điện nước/số lẻ/trùng kỳ, hóa đơn có phạt và điện nước, trả từng phần, chặn thanh toán vượt nợ và giữ tiền phòng khi trả phòng.

Kết quả kiểm chứng ngày 01/11/2026: biên dịch bằng JDK 21 với `--release 17`, **44 kiểm tra đạt** trên MariaDB 10.4.32 chạy riêng ở cổng 33317 qua MySQL Connector/J 9.4.0. Chạy lại migration giữ nguyên 4 tài khoản và 1 bản ghi thanh toán đã tạo trong kiểm thử. Các panel Swing đã được kiểm tra khởi tạo ở chế độ headless; chưa kiểm thử thao tác chuột trực tiếp hoặc chạy trên máy chủ MySQL 8 thực tế.
