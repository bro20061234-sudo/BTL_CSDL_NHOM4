package vn.ktx.ui;

import com.toedter.calendar.JDateChooser;
import vn.ktx.dao.KtxDao;
import vn.ktx.model.Student;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public class MainFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private transient final KtxDao dao = new KtxDao();
    private final JTable studentTable = new JTable();
    private final JTable roomTable = new JTable();
    private final JTable serviceTable = new JTable();
    private final JTable usageTable = new JTable();
    private final JTable vehicleTable = new JTable();
    private final JTable parkingTable = new JTable();
    private final JTable guestTable = new JTable();
    private final JTable invoiceTable = new JTable();
    private final JTextField studentSearch = new JTextField(22);
    private final JSpinner invoiceMonth = new JSpinner(new SpinnerNumberModel(LocalDate.now().getMonthValue(), 1, 12, 1));
    private final JSpinner invoiceYear = new JSpinner(new SpinnerNumberModel(LocalDate.now().getYear(), 2000, 2200, 1));
    private String invoiceCaption = "";

    public MainFrame() {
        setTitle("Quản lý Ký túc xá Sinh viên");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 660));
        setSize(1250, 780);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        add(header(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Sinh viên", studentPanel());
        tabs.addTab("Phòng ở", roomPanel());
        tabs.addTab("Dịch vụ", servicePanel());
        tabs.addTab("Nghiệp vụ", operationsPanel());
        tabs.addTab("Hóa đơn tháng", invoicePanel());
        tabs.addTab("Báo cáo", reportPanel());
        add(tabs, BorderLayout.CENTER);
        add(new JLabel("  Kết nối MySQL qua JDBC · Dữ liệu lấy trực tiếp từ cơ sở dữ liệu", SwingConstants.LEFT), BorderLayout.SOUTH);
        refreshAll();
    }

    private JPanel header() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        JLabel title = new JLabel("QUẢN LÝ KÝ TÚC XÁ SINH VIÊN");
        title.setFont(title.getFont().deriveFont(21f));
        JButton connection = new JButton("Kiểm tra kết nối");
        connection.addActionListener(event -> runAction(() -> {
            dao.testConnection();
            info("Kết nối MySQL thành công.");
        }));
        panel.add(title, BorderLayout.WEST);
        panel.add(connection, BorderLayout.EAST);
        return panel;
    }

    private JPanel studentPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton search = button("Tìm kiếm");
        JButton add = button("Thêm sinh viên");
        JButton edit = button("Sửa");
        JButton delete = button("Xóa");
        JButton refresh = button("Làm mới");
        actions.add(new JLabel("Mã / Họ tên / Lớp:"));
        actions.add(studentSearch);
        actions.add(search);
        actions.add(add);
        actions.add(edit);
        actions.add(delete);
        actions.add(refresh);
        panel.add(actions, BorderLayout.NORTH);
        panel.add(new JScrollPane(studentTable), BorderLayout.CENTER);
        search.addActionListener(e -> load(studentTable, KtxDao.STUDENT_COLUMNS, () -> dao.findStudents(studentSearch.getText())));
        refresh.addActionListener(e -> {
            studentSearch.setText("");
            load(studentTable, KtxDao.STUDENT_COLUMNS, () -> dao.findStudents(""));
        });
        add.addActionListener(e -> studentForm(null));
        edit.addActionListener(e -> {
            Object[] row = selected(studentTable);
            if (row != null) studentForm(row);
        });
        delete.addActionListener(e -> deleteSelected(studentTable, "sinh viên", row -> dao.deleteStudent(row[0].toString())));
        return panel;
    }

    private JPanel roomPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel actions = actionBar();
        JButton add = button("Thêm phòng");
        JButton edit = button("Sửa phòng");
        JButton delete = button("Xóa phòng");
        JButton refresh = button("Làm mới");
        actions.add(add);
        actions.add(edit);
        actions.add(delete);
        actions.add(refresh);
        panel.add(actions, BorderLayout.NORTH);
        panel.add(new JScrollPane(roomTable), BorderLayout.CENTER);
        add.addActionListener(e -> roomForm(null));
        edit.addActionListener(e -> {
            Object[] row = selected(roomTable);
            if (row != null) roomForm(row);
        });
        delete.addActionListener(e -> deleteSelected(roomTable, "phòng", row -> dao.deleteRoom(row[0].toString())));
        refresh.addActionListener(e -> refreshRooms());
        return panel;
    }

    private JPanel servicePanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel actions = actionBar();
        JButton add = button("Thêm dịch vụ");
        JButton edit = button("Sửa");
        JButton delete = button("Xóa");
        JButton refresh = button("Làm mới");
        actions.add(add);
        actions.add(edit);
        actions.add(delete);
        actions.add(refresh);
        panel.add(actions, BorderLayout.NORTH);
        panel.add(new JScrollPane(serviceTable), BorderLayout.CENTER);
        add.addActionListener(e -> serviceForm(null));
        edit.addActionListener(e -> {
            Object[] row = selected(serviceTable);
            if (row != null) serviceForm(row);
        });
        delete.addActionListener(e -> deleteSelected(serviceTable, "dịch vụ", row -> dao.deleteService(row[0].toString())));
        refresh.addActionListener(e -> refreshServices());
        return panel;
    }

    private JPanel operationsPanel() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Sử dụng dịch vụ", usagePanel());
        tabs.addTab("Xe vé tháng", vehiclePanel());
        tabs.addTab("Nhật ký gửi xe", parkingPanel());
        tabs.addTab("Khách đến thăm", guestPanel());
        return wrap(tabs);
    }

    private JPanel usagePanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel actions = actionBar();
        JButton add = button("Ghi nhận sử dụng");
        JButton delete = button("Xóa lượt");
        JButton refresh = button("Làm mới");
        actions.add(add);
        actions.add(delete);
        actions.add(refresh);
        panel.add(actions, BorderLayout.NORTH);
        panel.add(new JScrollPane(usageTable), BorderLayout.CENTER);
        add.addActionListener(e -> usageForm());
        delete.addActionListener(e -> deleteSelected(usageTable, "lượt sử dụng", row -> dao.deleteUsage(number(row[0]))));
        refresh.addActionListener(e -> refreshUsages());
        return panel;
    }

    private JPanel vehiclePanel() {
        JTable table = vehicleTable;
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel actions = actionBar();
        JButton add = button("Đăng ký xe");
        JButton delete = button("Xóa đăng ký");
        JButton refresh = button("Làm mới");
        actions.add(add);
        actions.add(delete);
        actions.add(refresh);
        panel.add(actions, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        add.addActionListener(e -> vehicleForm());
        delete.addActionListener(e -> deleteSelected(table, "đăng ký xe", row -> dao.deleteVehicle(number(row[0]))));
        refresh.addActionListener(e -> refreshVehicles());
        return panel;
    }

    private JPanel guestPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel actions = actionBar();
        JButton add = button("Ghi nhận khách");
        JButton delete = button("Xóa lượt khách");
        JButton refresh = button("Làm mới");
        actions.add(add);
        actions.add(delete);
        actions.add(refresh);
        panel.add(actions, BorderLayout.NORTH);
        panel.add(new JScrollPane(guestTable), BorderLayout.CENTER);
        add.addActionListener(e -> guestForm());
        delete.addActionListener(e -> deleteSelected(guestTable, "lượt khách", row -> dao.deleteGuest(number(row[0]))));
        refresh.addActionListener(e -> refreshGuests());
        return panel;
    }

    private JPanel parkingPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel actions = actionBar();
        JButton add = button("Ghi nhận lượt gửi");
        JButton delete = button("Xóa lượt");
        JButton refresh = button("Làm mới");
        actions.add(add);
        actions.add(delete);
        actions.add(refresh);
        panel.add(actions, BorderLayout.NORTH);
        panel.add(new JScrollPane(parkingTable), BorderLayout.CENTER);
        add.addActionListener(e -> parkingForm());
        delete.addActionListener(e -> deleteSelected(parkingTable, "lượt gửi xe",
                row -> dao.deleteParkingLog(number(row[0]))));
        refresh.addActionListener(e -> refreshParkingLogs());
        return panel;
    }

    private JPanel invoicePanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel controls = actionBar();
        JButton calculate = button("Tính hóa đơn");
        JButton export = button("Xuất danh sách");
        JButton print = button("In hóa đơn");
        controls.add(new JLabel("Tháng:"));
        controls.add(invoiceMonth);
        controls.add(new JLabel("Năm:"));
        controls.add(invoiceYear);
        controls.add(calculate);
        controls.add(export);
        controls.add(print);
        panel.add(controls, BorderLayout.NORTH);
        panel.add(new JScrollPane(invoiceTable), BorderLayout.CENTER);
        calculate.addActionListener(e -> refreshInvoices());
        export.addActionListener(e -> exportInvoices());
        print.addActionListener(e -> printInvoices());
        return panel;
    }

    private JPanel reportPanel() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Sử dụng dịch vụ", serviceUsageReportPanel());
        tabs.addTab("Khách đến thăm", guestReportPanel());
        tabs.addTab("Doanh thu dịch vụ", revenueReportPanel());
        return wrap(tabs);
    }

    private JPanel serviceUsageReportPanel() {
        JTable table = new JTable();
        JDateChooser from = dateChooser(LocalDate.now().withDayOfMonth(1));
        JDateChooser to = dateChooser(LocalDate.now());
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel bar = actionBar();
        JButton run = button("Thống kê");
        bar.add(new JLabel("Từ ngày:"));
        bar.add(from);
        bar.add(new JLabel("Đến ngày:"));
        bar.add(to);
        bar.add(run);
        panel.add(bar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        run.addActionListener(e -> {
            LocalDate start = dateFrom(from);
            LocalDate end = dateFrom(to);
            if (start == null || end == null || end.isBefore(start)) {
                error("Vui lòng chọn khoảng ngày hợp lệ.");
                return;
            }
            load(table, new String[]{"Mã SV", "Sinh viên", "Dịch vụ", "Tổng số lượng", "Tổng tiền"},
                    () -> dao.reportServiceUsage(start, end));
        });
        return panel;
    }

    private JPanel guestReportPanel() {
        JTable table = new JTable();
        JDateChooser from = dateChooser(LocalDate.now().withDayOfMonth(1));
        JDateChooser to = dateChooser(LocalDate.now());
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel bar = actionBar();
        JButton weekly = button("Thống kê theo tuần");
        JButton monthly = button("Thống kê theo tháng");
        bar.add(new JLabel("Từ ngày:"));
        bar.add(from);
        bar.add(new JLabel("Đến ngày:"));
        bar.add(to);
        bar.add(weekly);
        bar.add(monthly);
        panel.add(bar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        weekly.addActionListener(e -> loadGuestReport(table, from, to, true));
        monthly.addActionListener(e -> loadGuestReport(table, from, to, false));
        return panel;
    }

    private JPanel revenueReportPanel() {
        JTable table = new JTable();
        JSpinner month = new JSpinner(new SpinnerNumberModel(LocalDate.now().getMonthValue(), 1, 12, 1));
        JSpinner year = new JSpinner(new SpinnerNumberModel(LocalDate.now().getYear(), 2000, 2200, 1));
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel bar = actionBar();
        JButton run = button("Thống kê doanh thu");
        bar.add(new JLabel("Tháng:"));
        bar.add(month);
        bar.add(new JLabel("Năm:"));
        bar.add(year);
        bar.add(run);
        panel.add(bar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        run.addActionListener(e -> load(table, new String[]{"Mã DV", "Dịch vụ", "Doanh thu"},
                () -> dao.reportRevenue(number(month.getValue()), number(year.getValue()))));
        return panel;
    }

    private void studentForm(Object[] existing) {
        boolean insert = existing == null;
        JTextField id = field(existing == null ? "" : text(existing[0]));
        id.setEditable(insert);
        JTextField identity = field(existing == null ? "" : text(existing[1]));
        JTextField name = field(existing == null ? "" : text(existing[2]));
        JDateChooser birth = dateChooser(existing == null ? null : localDate(existing[3]));
        JTextField className = field(existing == null ? "" : text(existing[4]));
        JTextField hometown = field(existing == null ? "" : text(existing[5]));
        JComboBox<String> room = new JComboBox<>();
        room.addItem("");
        try {
            for (Object[] choice : dao.getRoomChoices()) room.addItem(choice[0].toString());
        } catch (SQLException exception) {
            showError(exception);
            return;
        }
        if (existing != null && existing[6] != null) room.setSelectedItem(existing[6].toString());
        JDateChooser admission = dateChooser(existing == null ? LocalDate.now() : localDate(existing[7]));
        Object[] fields = {"Mã sinh viên:", id, "Số CCCD:", identity, "Họ tên:", name, "Ngày sinh:", birth,
                "Lớp:", className, "Quê quán:", hometown, "Mã phòng (để trống nếu chưa xếp):", room,
                "Ngày vào KTX:", admission};
        if (!confirmForm("Thông tin sinh viên", fields)) return;
        if (required(id, identity, name, className, hometown) || dateFrom(birth) == null || dateFrom(admission) == null) {
            error("Vui lòng nhập đầy đủ thông tin và chọn ngày hợp lệ.");
            return;
        }
        runAction(() -> {
            dao.saveStudent(new Student(id.getText().trim(), identity.getText().trim(), name.getText().trim(),
                    dateFrom(birth), className.getText().trim(), hometown.getText().trim(),
                    (String) room.getSelectedItem(), dateFrom(admission)), insert);
            refreshAll();
            info(insert ? "Đã thêm sinh viên." : "Đã cập nhật sinh viên.");
        });
    }

    private void roomForm(Object[] existing) {
        boolean insert = existing == null;
        JTextField id = field(existing == null ? "" : text(existing[0]));
        id.setEditable(insert);
        JTextField number = field(existing == null ? "" : text(existing[1]));
        JTextField type = field(existing == null ? "" : text(existing[2]));
        JTextField price = field(existing == null ? "" : text(existing[3]));
        JTextField capacity = field(existing == null ? "" : text(existing[4]));
        if (!confirmForm("Thông tin phòng", new Object[]{"Mã phòng:", id, "Số phòng:", number, "Loại phòng:", type,
                "Đơn giá/tháng:", price, "Sức chứa tối đa:", capacity})) return;
        if (required(id, number, type, price, capacity)) {
            error("Vui lòng nhập đầy đủ thông tin phòng.");
            return;
        }
        try {
            BigDecimal amount = new BigDecimal(price.getText().trim());
            int max = Integer.parseInt(capacity.getText().trim());
            runAction(() -> {
                dao.saveRoom(id.getText().trim(), number.getText().trim(), type.getText().trim(), amount, max, insert);
                refreshRooms();
                info("Đã lưu thông tin phòng.");
            });
        } catch (NumberFormatException exception) {
            error("Đơn giá và sức chứa phải là số hợp lệ.");
        }
    }

    private void serviceForm(Object[] existing) {
        boolean insert = existing == null;
        JTextField id = field(existing == null ? "" : text(existing[0]));
        id.setEditable(insert);
        JTextField name = field(existing == null ? "" : text(existing[1]));
        JTextField price = field(existing == null ? "" : text(existing[2]));
        if (!confirmForm("Thông tin dịch vụ", new Object[]{"Mã dịch vụ:", id, "Tên dịch vụ:", name, "Đơn giá:", price})) return;
        if (required(id, name, price)) {
            error("Vui lòng nhập đầy đủ thông tin dịch vụ.");
            return;
        }
        try {
            BigDecimal amount = new BigDecimal(price.getText().trim());
            runAction(() -> {
                dao.saveService(id.getText().trim(), name.getText().trim(), amount, insert);
                refreshServices();
                info("Đã lưu thông tin dịch vụ.");
            });
        } catch (NumberFormatException exception) {
            error("Đơn giá phải là số hợp lệ.");
        }
    }

    private void usageForm() {
        JComboBox<String> student = studentChoices();
        JComboBox<String> service = serviceChoices();
        if (student.getItemCount() == 0 || service.getItemCount() == 0) {
            error("Cần có ít nhất một sinh viên và một dịch vụ trước khi ghi nhận sử dụng.");
            return;
        }
        JDateChooser date = dateChooser(LocalDate.now());
        JTextField time = field("12:00");
        JTextField quantity = field("1");
        if (!confirmForm("Ghi nhận sử dụng dịch vụ", new Object[]{"Mã sinh viên:", student, "Mã dịch vụ:", service,
                "Ngày sử dụng:", date, "Giờ (HH:mm):", time, "Số lượng:", quantity})) return;
        try {
            LocalDate selectedDate = dateFrom(date);
            LocalDateTime usedAt = LocalDateTime.of(selectedDate, java.time.LocalTime.parse(time.getText().trim()));
            int count = Integer.parseInt(quantity.getText().trim());
            runAction(() -> {
                dao.addUsage((String) student.getSelectedItem(), (String) service.getSelectedItem(), usedAt, count);
                refreshUsages();
                info("Đã ghi nhận lượt sử dụng.");
            });
        } catch (RuntimeException exception) {
            error("Ngày, giờ hoặc số lượng không hợp lệ. Giờ nhập theo dạng HH:mm.");
        }
    }

    private void vehicleForm() {
        JComboBox<String> student = studentChoices();
        if (student.getItemCount() == 0) {
            error("Cần thêm sinh viên trước khi đăng ký xe.");
            return;
        }
        JTextField plate = field("");
        JTextField type = field("Xe máy");
        JDateChooser date = dateChooser(LocalDate.now());
        if (!confirmForm("Đăng ký xe vé tháng", new Object[]{"Mã sinh viên:", student, "Biển số:", plate,
                "Loại xe:", type, "Ngày đăng ký:", date})) return;
        if (student.getSelectedItem() == null || required(plate, type) || dateFrom(date) == null) {
            error("Vui lòng nhập đầy đủ thông tin xe.");
            return;
        }
        runAction(() -> {
            dao.addVehicle((String) student.getSelectedItem(), plate.getText().trim(), type.getText().trim(), dateFrom(date));
            refreshVehicles();
            info("Đã đăng ký xe vé tháng.");
        });
    }

    private void guestForm() {
        JTextField identity = field("");
        JTextField name = field("");
        JDateChooser birth = dateChooser(null);
        JComboBox<String> student = studentChoices();
        if (student.getItemCount() == 0) {
            error("Cần thêm sinh viên trước khi ghi nhận khách.");
            return;
        }
        JDateChooser visit = dateChooser(LocalDate.now());
        if (!confirmForm("Ghi nhận khách đến thăm", new Object[]{"CCCD khách:", identity, "Họ tên:", name,
                "Ngày sinh:", birth, "Mã sinh viên được thăm:", student, "Ngày đến:", visit})) return;
        if (student.getSelectedItem() == null || required(identity, name) || dateFrom(birth) == null || dateFrom(visit) == null) {
            error("Vui lòng nhập đầy đủ thông tin khách.");
            return;
        }
        runAction(() -> {
            dao.addGuest(identity.getText().trim(), name.getText().trim(), dateFrom(birth),
                    (String) student.getSelectedItem(), dateFrom(visit));
            refreshGuests();
            info("Đã ghi nhận khách đến thăm.");
        });
    }

    private void parkingForm() {
        JComboBox<VehicleChoice> vehicle = new JComboBox<>();
        try {
            for (Object[] row : dao.getVehicleChoices()) {
                vehicle.addItem(new VehicleChoice(number(row[0]), text(row[1]), text(row[2])));
            }
        } catch (SQLException exception) {
            showError(exception);
            return;
        }
        if (vehicle.getItemCount() == 0) {
            error("Cần đăng ký xe vé tháng trước khi ghi nhận lượt gửi.");
            return;
        }
        JDateChooser date = dateChooser(LocalDate.now());
        JTextField time = field(java.time.LocalTime.now().withSecond(0).toString());
        if (!confirmForm("Ghi nhận lượt gửi xe", new Object[]{"Xe (mã - biển số - sinh viên):", vehicle,
                "Ngày gửi:", date, "Giờ gửi (HH:mm):", time})) return;
        try {
            LocalDateTime sentAt = LocalDateTime.of(dateFrom(date), java.time.LocalTime.parse(time.getText().trim()));
            VehicleChoice selected = (VehicleChoice) vehicle.getSelectedItem();
            runAction(() -> {
                dao.addParkingLog(selected.id(), sentAt);
                refreshParkingLogs();
                refreshInvoices();
                info("Đã ghi nhận lượt gửi xe. Phí phát sinh (nếu có) do MySQL trigger tự tính.");
            });
        } catch (RuntimeException exception) {
            error("Ngày hoặc giờ không hợp lệ. Giờ nhập theo dạng HH:mm.");
        }
    }

    private JComboBox<String> studentChoices() {
        JComboBox<String> choice = new JComboBox<>();
        try {
            for (String id : dao.getStudentChoices()) choice.addItem(id);
        } catch (SQLException exception) {
            showError(exception);
        }
        return choice;
    }

    private JComboBox<String> serviceChoices() {
        JComboBox<String> choice = new JComboBox<>();
        try {
            for (Object[] service : dao.getServiceChoices()) choice.addItem(service[0].toString());
        } catch (SQLException exception) {
            showError(exception);
        }
        return choice;
    }

    private void refreshAll() {
        try {
            dao.testConnection();
        } catch (SQLException exception) {
            showError(exception);
            return;
        }
        load(studentTable, KtxDao.STUDENT_COLUMNS, () -> dao.findStudents(studentSearch.getText()));
        refreshRooms();
        refreshServices();
        refreshUsages();
        refreshVehicles();
        refreshParkingLogs();
        refreshGuests();
    }

    private void refreshRooms() { load(roomTable, KtxDao.ROOM_COLUMNS, dao::getRooms); }
    private void refreshServices() { load(serviceTable, KtxDao.SERVICE_COLUMNS, dao::getServices); }
    private void refreshUsages() { load(usageTable, KtxDao.USAGE_COLUMNS, dao::getUsages); }
    private void refreshVehicles() { load(vehicleTable, KtxDao.VEHICLE_COLUMNS, dao::getVehicles); }
    private void refreshParkingLogs() { load(parkingTable, KtxDao.PARKING_COLUMNS, dao::getParkingLogs); }
    private void refreshGuests() { load(guestTable, KtxDao.GUEST_COLUMNS, dao::getGuests); }

    private void refreshInvoices() {
        int month = number(invoiceMonth.getValue());
        int year = number(invoiceYear.getValue());
        invoiceCaption = "HÓA ĐƠN KÝ TÚC XÁ THÁNG " + month + "/" + year;
        load(invoiceTable, KtxDao.INVOICE_COLUMNS, () -> dao.getInvoices(month, year));
    }

    private void loadGuestReport(JTable table, JDateChooser from, JDateChooser to, boolean weekly) {
        LocalDate start = dateFrom(from);
        LocalDate end = dateFrom(to);
        if (start == null || end == null || end.isBefore(start)) {
            error("Vui lòng chọn khoảng ngày hợp lệ.");
            return;
        }
        load(table, new String[]{"Tuần / Tháng", "Mã SV", "Sinh viên", "CCCD khách", "Tên khách", "Số lần đến"},
                () -> dao.reportGuests(start, end, weekly));
    }

    private void exportInvoices() {
        if (invoiceTable.getRowCount() == 0) {
            error("Hãy tính hóa đơn trước khi xuất.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("hoa-don-" + invoiceMonth.getValue() + "-" + invoiceYear.getValue() + ".txt"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Object[] row = selectedOrNull(invoiceTable);
        try {
            Files.writeString(chooser.getSelectedFile().toPath(),
                    row == null ? invoiceText() : invoiceTextForRow(row), StandardCharsets.UTF_8);
            info(row == null ? "Đã xuất danh sách hóa đơn." : "Đã xuất hóa đơn của sinh viên được chọn.");
        } catch (IOException exception) {
            showError(exception);
        }
    }

    private void printInvoices() {
        Object[] row = selectedOrNull(invoiceTable);
        if (row == null && invoiceTable.getRowCount() == 0) {
            error("Hãy tính hóa đơn trước khi in.");
            return;
        }
        JTextArea printable = new JTextArea();
        printable.setText(row == null ? invoiceText() : invoiceTextForRow(row));
        try {
            if (!printable.print()) info("Đã hủy thao tác in.");
        } catch (java.awt.print.PrinterException exception) {
            showError(exception);
        }
    }

    private String invoiceText() {
        StringBuilder out = new StringBuilder(invoiceCaption).append(System.lineSeparator())
                .append("Tiền phòng + dịch vụ cơ bản + xe vé tháng + phí phát sinh").append(System.lineSeparator());
        for (int i = 0; i < invoiceTable.getRowCount(); i++) {
            Object[] row = new Object[invoiceTable.getColumnCount()];
            for (int j = 0; j < row.length; j++) row[j] = invoiceTable.getValueAt(i, j);
            out.append(invoiceTextForRow(row)).append(System.lineSeparator());
        }
        return out.toString();
    }

    private String invoiceTextForRow(Object[] row) {
        return String.format("%s | %s | Phòng %s | Phòng: %s | Dịch vụ: %s | Xe tháng: %s | Xe phát sinh: %s | TỔNG: %s",
                row[0], row[1], row[2], money(row[3]), money(row[4]), money(row[5]), money(row[6]), money(row[7]));
    }

    private String money(Object value) {
        if (value instanceof Number n) return String.format("%,.0f đ", n.doubleValue());
        return String.valueOf(value);
    }

    private void load(JTable table, String[] columns, SqlSupplier supplier) {
        try {
            List<Object[]> data = supplier.get();
            DefaultTableModel model = new DefaultTableModel(columns, 0) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            for (Object[] row : data) model.addRow(row);
            table.setModel(model);
            table.setAutoCreateRowSorter(true);
        } catch (SQLException exception) {
            showError(exception);
        }
    }

    private Object[] selected(JTable table) {
        Object[] row = selectedOrNull(table);
        if (row == null) error("Vui lòng chọn một dòng trong danh sách.");
        return row;
    }

    private Object[] selectedOrNull(JTable table) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;
        int rowIndex = table.convertRowIndexToModel(viewRow);
        Object[] data = new Object[table.getColumnCount()];
        for (int i = 0; i < data.length; i++) data[i] = table.getModel().getValueAt(rowIndex, i);
        return data;
    }

    private void deleteSelected(JTable table, String description, RowAction action) {
        Object[] row = selected(table);
        if (row == null) return;
        if (JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa " + description + " đã chọn?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        runAction(() -> {
            action.run(row);
            refreshAll();
            info("Đã xóa " + description + ".");
        });
    }

    private JPanel wrap(java.awt.Component component) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    private JPanel actionBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        return panel;
    }

    private JButton button(String label) {
        return new JButton(label);
    }

    private JTextField field(String value) {
        return new JTextField(value, 20);
    }

    private JDateChooser dateChooser(LocalDate value) {
        JDateChooser chooser = new JDateChooser();
        chooser.setDateFormatString("yyyy-MM-dd");
        chooser.setPreferredSize(new Dimension(170, 25));
        if (value != null) chooser.setDate(java.util.Date.from(value.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        return chooser;
    }

    private LocalDate dateFrom(JDateChooser chooser) {
        if (chooser.getDate() == null) return null;
        return chooser.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private boolean confirmForm(String title, Object[] fields) {
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        for (int i = 0; i < fields.length; i += 2) {
            form.add(new JLabel(fields[i].toString()));
            form.add((java.awt.Component) fields[i + 1]);
        }
        return JOptionPane.showConfirmDialog(this, form, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    private boolean required(JTextField... fields) {
        for (JTextField field : fields) if (field.getText().isBlank()) return true;
        return false;
    }

    private LocalDate localDate(Object value) {
        if (value instanceof Date date) return date.toLocalDate();
        if (value instanceof java.util.Date date) return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return LocalDate.parse(value.toString());
    }

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }

    private int number(Object value) {
        return ((Number) value).intValue();
    }

    private void runAction(SqlAction action) {
        try {
            action.run();
        } catch (SQLException exception) {
            showError(exception);
        }
    }

    private void info(String message) {
        JOptionPane.showMessageDialog(this, message, "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(this, message, "Vui lòng kiểm tra", JOptionPane.WARNING_MESSAGE);
    }

    private void showError(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) message = "Đã xảy ra lỗi khi xử lý dữ liệu.";
        JOptionPane.showMessageDialog(this, message, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    @FunctionalInterface
    private interface SqlAction {
        void run() throws SQLException;
    }

    @FunctionalInterface
    private interface SqlSupplier {
        List<Object[]> get() throws SQLException;
    }

    @FunctionalInterface
    private interface RowAction {
        void run(Object[] row) throws SQLException;
    }

    private record VehicleChoice(int id, String plate, String studentId) {
        @Override
        public String toString() {
            return id + " - " + plate + " (" + studentId + ")";
        }
    }
}
