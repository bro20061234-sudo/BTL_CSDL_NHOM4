package vn.ktx.ui;

import com.toedter.calendar.JDateChooser;
import vn.ktx.dao.KtxDao;
import vn.ktx.model.Student;
import vn.ktx.model.RoomType;

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
    private final JTable studentTable = new DataTable();
    private final JTable roomTable = new DataTable();
    private final JTable serviceTable = new DataTable();
    private final JTable usageTable = new DataTable();
    private final JTable vehicleTable = new DataTable();
    private final JTable guestTable = new DataTable();
    private final JTable invoiceTable = new DataTable();
    private final JTextField studentSearch = new JTextField(22);
    private final JSpinner invoiceMonth = new JSpinner(new SpinnerNumberModel(LocalDate.now().getMonthValue(), 1, 12, 1));
    private final JSpinner invoiceYear = new JSpinner(new SpinnerNumberModel(LocalDate.now().getYear(), 2000, 2200, 1));
    private String invoiceCaption = "";
    private WorkspacePanel workspace;
    private ExtensionPanel extensions;
    private final JComboBox<String> studentRoomFilter = new JComboBox<>(new String[]{"Tất cả phòng", "Chưa xếp phòng"});
    private final JLabel studentSelection = UiTheme.label("Chọn một sinh viên để thao tác.",13,false);
    private final JLabel studentCount = UiTheme.label("",12,false);
    private OverviewPanel studentOverview;

    public MainFrame(vn.ktx.model.UserSession session) {
        if (!session.isStaff()) throw new IllegalArgumentException("Chức năng dành cho nhân viên quản lý.");
        setTitle("Quản lý Ký túc xá Sinh viên");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 740));
        setSize(1440, 920);
        setLocationRelativeTo(null);
        extensions = new ExtensionPanel(session, false);
        workspace = new WorkspacePanel(session, () -> vn.ktx.Main.logout(this));
        workspace.group("Tổng quan");
        workspace.page("overview", "Bảng điều khiển", "Tổng quan ký túc xá", "Theo dõi cư dân, phòng trống và công việc cần xử lý.", () -> new OverviewPanel(false));
        workspace.group("Quản lý");
        workspace.page("students", "Sinh viên", "Quản lý sinh viên", "Tra cứu hồ sơ và quản lý thông tin lưu trú.", this::studentPanel);
        workspace.page("rooms", "Phòng ở", "Quản lý phòng ở", "Giá thuê, sức chứa và số sinh viên đang ở trong từng phòng.", this::roomPanel);
        workspace.page("contracts", "Hợp đồng", "Hợp đồng lưu trú", "Lập hợp đồng, gia hạn thời gian ở và thanh lý khi trả phòng.", () -> extensions.modulePanel(vn.ktx.dao.ExtensionDao.Module.CONTRACT));
        workspace.page("services", "Dịch vụ & gửi xe", "Dịch vụ & gửi xe", "Dịch vụ, đăng ký vé tháng và quầy gửi/lấy xe theo thời gian thực.", () -> WorkspacePanel.tabs(new String[]{"Danh mục dịch vụ", "Sử dụng dịch vụ", "Xe vé tháng", "Bãi đỗ xe"}, servicePanel(), usagePanel(), vehiclePanel(), parkingPanel()));
        workspace.page("guests", "Khách đến thăm", "Khách đến thăm", "Ghi nhận thông tin khách và sinh viên được thăm.", this::guestPanel);
        workspace.group("Yêu cầu");
        workspace.page("requests", "Đăng ký & chuyển phòng", "Đăng ký & chuyển phòng", "Duyệt đơn lưu trú, kiểm tra chỗ trống và xử lý chuyển phòng.", () -> WorkspacePanel.tabs(new String[]{"Đăng ký lưu trú", "Chuyển phòng"}, extensions.modulePanel(vn.ktx.dao.ExtensionDao.Module.APPLICATION), extensions.modulePanel(vn.ktx.dao.ExtensionDao.Module.TRANSFER)));
        workspace.page("maintenance", "Sửa chữa & tài sản", "Sửa chữa & tài sản", "Tiếp nhận sự cố, cập nhật tiến độ và theo dõi tài sản bàn giao.", () -> WorkspacePanel.tabs(new String[]{"Yêu cầu sửa chữa", "Tài sản phòng"}, extensions.modulePanel(vn.ktx.dao.ExtensionDao.Module.REPAIR), extensions.modulePanel(vn.ktx.dao.ExtensionDao.Module.ASSET)));
        workspace.group("Tài chính");
        workspace.page("billing", "Hóa đơn & thanh toán", "Hóa đơn & thanh toán", "Tính phí theo tháng, theo dõi công nợ và ghi nhận khoản thu.", () -> WorkspacePanel.tabs(new String[]{"Hóa đơn tháng", "Công nợ", "Lịch sử thanh toán"}, invoicePanel(), extensions.billingPanel(), extensions.modulePanel(vn.ktx.dao.ExtensionDao.Module.PAYMENT)));
        workspace.page("utilities", "Điện nước", "Điện nước hằng tháng", "Nhập chỉ số, đơn giá và phân bổ tiền điện nước cho cư dân.", () -> extensions.modulePanel(vn.ktx.dao.ExtensionDao.Module.UTILITY));
        workspace.page("reports", "Báo cáo", "Báo cáo & thống kê", "Tổng hợp dịch vụ, khách đến thăm và doanh thu theo thời gian.", this::reportPanel);
        workspace.group("Hệ thống");
        workspace.page("notices", "Thông báo & vi phạm", "Thông báo & vi phạm", "Gửi thông báo, theo dõi nội quy và các biên bản vi phạm.", () -> WorkspacePanel.tabs(new String[]{"Thông báo", "Vi phạm nội quy"}, extensions.modulePanel(vn.ktx.dao.ExtensionDao.Module.NOTICE), extensions.modulePanel(vn.ktx.dao.ExtensionDao.Module.VIOLATION)));
        workspace.page("reminders", "Nhắc hạn", "Nhắc hạn & công nợ", "Các hợp đồng sắp hết hạn và khoản thu chưa thanh toán đủ.", extensions::reminderPanel);
        if (session.isAdmin()) workspace.page("accounts", "Tài khoản", "Quản lý tài khoản", "Tạo tài khoản và phân quyền cho nhân viên, sinh viên.", extensions::accountPanel);
        setContentPane(workspace);
        workspace.showPage("students");
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
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        JPanel top = new JPanel(new BorderLayout(0, 16));
        studentOverview = new OverviewPanel(true);
        top.add(studentOverview, BorderLayout.NORTH);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        JButton search = button("Tìm kiếm");
        JButton add = button("Thêm sinh viên");
        JButton edit = button("Chỉnh sửa");
        JButton delete = button("Xóa");
        JButton refresh = button("Làm mới");
        actions.add(new JLabel("Tìm kiếm:"));
        studentSearch.setToolTipText("Nhập mã sinh viên, họ tên hoặc lớp; kết quả lọc ngay khi nhập.");
        actions.add(studentSearch);
        actions.add(studentRoomFilter);
        actions.add(refresh);
        actions.add(add);
        top.add(actions, BorderLayout.SOUTH);
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(studentTable), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(0,12));
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));
        JButton profile = button("Xem hồ sơ"); JButton transfer = button("Chuyển phòng");
        footer.add(profile); footer.add(edit); footer.add(transfer); footer.add(delete);
        JPanel selectionBar = new JPanel(new BorderLayout(12,0)); selectionBar.add(studentSelection,BorderLayout.WEST); selectionBar.add(footer,BorderLayout.EAST);
        bottom.add(selectionBar,BorderLayout.NORTH); bottom.add(studentCount,BorderLayout.SOUTH); panel.add(bottom,BorderLayout.SOUTH);
        search.addActionListener(e -> applyStudentFilter());
        studentSearch.addActionListener(e -> applyStudentFilter());
        studentSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyStudentFilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyStudentFilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyStudentFilter(); }
        });
        studentRoomFilter.addActionListener(e -> applyStudentFilter());
        studentTable.getSelectionModel().addListSelectionListener(e -> {
            Object[] row=selectedOrNull(studentTable); boolean chosen=row!=null;
            studentSelection.setText(chosen?"Đã chọn: "+row[0]+" · "+row[2]:"Chọn một sinh viên để thao tác.");
            for(JButton b:new JButton[]{profile,edit,transfer,delete}) b.setEnabled(chosen);
        });
        for(JButton b:new JButton[]{profile,edit,transfer,delete}) b.setEnabled(false);
        profile.addActionListener(e -> showStudentProfile());
        transfer.addActionListener(e -> { Object[] row=selected(studentTable); if(row!=null) extensions.createTransfer(row[0].toString()); });
        studentTable.addMouseListener(new java.awt.event.MouseAdapter() { @Override public void mouseClicked(java.awt.event.MouseEvent e) { if(e.getClickCount()==2) showStudentProfile(); } });
        refresh.addActionListener(e -> {
            studentSearch.setText("");
            studentRoomFilter.setSelectedIndex(0);
            refreshStudents(); studentOverview.reload();
        });
        add.addActionListener(e -> studentForm(null));
        edit.addActionListener(e -> {
            Object[] row = selected(studentTable);
            if (row != null) studentForm(row);
        });
        delete.addActionListener(e -> deleteSelected(studentTable, "sinh viên", row -> dao.deleteStudent(row[0].toString())));
        onShow(panel, this::refreshStudents);
        return panel;
    }

    public void refreshStudents() { load(studentTable, KtxDao.STUDENT_COLUMNS, () -> dao.findStudents("")); }

    @SuppressWarnings("unchecked")
    private void applyStudentFilter() {
        if (!(studentTable.getRowSorter() instanceof javax.swing.table.TableRowSorter<?> sorter)) return;
        String term=studentSearch.getText().trim().toLowerCase(java.util.Locale.forLanguageTag("vi-VN"));
        String room=String.valueOf(studentRoomFilter.getSelectedItem());
        ((javax.swing.table.TableRowSorter<javax.swing.table.TableModel>)sorter).setRowFilter(new javax.swing.RowFilter<javax.swing.table.TableModel,Integer>() {
            @Override public boolean include(Entry<? extends javax.swing.table.TableModel,? extends Integer> entry) {
                String r=entry.getStringValue(6);
                boolean matchesRoom=room.equals("Tất cả phòng") || room.equals("Chưa xếp phòng") && r.isBlank() || room.equals(r);
                boolean matchesText=term.isEmpty();
                for(int c:new int[]{0,2,4}) matchesText |= entry.getStringValue(c).toLowerCase(java.util.Locale.forLanguageTag("vi-VN")).contains(term);
                return matchesRoom && matchesText;
            }
        });
        studentCount.setText("Hiển thị "+studentTable.getRowCount()+" / "+studentTable.getModel().getRowCount()+" sinh viên • Nhấp đúp một dòng để xem hồ sơ.");
    }

    private void showStudentProfile() {
        Object[] row=selected(studentTable); if(row==null) return;
        JPanel details=new JPanel(new GridLayout(0,2,16,14));
        for(int i=0;i<KtxDao.STUDENT_COLUMNS.length;i++) { details.add(UiTheme.label(KtxDao.STUDENT_COLUMNS[i],13,true)); details.add(UiTheme.label(text(row[i]),14,false)); }
        JOptionPane.showMessageDialog(this,details,"Hồ sơ · "+row[2],JOptionPane.PLAIN_MESSAGE);
    }

    private void onShow(JPanel panel,Runnable action) {
        panel.addHierarchyListener(e -> { if((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED)!=0 && panel.isShowing()) action.run(); });
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
        onShow(panel, this::refreshRooms);
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
        onShow(panel, this::refreshServices);
        return panel;
    }

    private JPanel operationsPanel() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Sử dụng dịch vụ", usagePanel());
        tabs.addTab("Xe vé tháng", vehiclePanel());
        tabs.addTab("Bãi đỗ xe", parkingPanel());
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
        onShow(panel, this::refreshUsages);
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
        onShow(panel, this::refreshVehicles);
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
        onShow(panel, this::refreshGuests);
        return panel;
    }

    private JPanel parkingPanel() {
        return new ParkingPanel();
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
        onShow(panel, this::refreshInvoices);
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
        JTable table = new DataTable();
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
        JTable table = new DataTable();
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
        JTable table = new DataTable();
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
        JComboBox<Object> type = new JComboBox<>(RoomType.values());
        if(existing!=null) {
            RoomType matched=RoomType.find(text(existing[2]));
            if(matched!=null) type.setSelectedItem(matched);
            else { type.addItem(existing[2]); type.setSelectedItem(existing[2]); }
        }
        JTextField price = field(existing == null ? RoomType.STANDARD.price().toPlainString() : text(existing[3]));
        JTextField capacity = field(existing == null ? "4" : text(existing[4])); capacity.setEditable(false);
        JTextArea assets = new JTextArea(3,28); assets.setEditable(false); assets.setLineWrap(true); assets.setWrapStyleWord(true); assets.setBackground(UiTheme.CANVAS);
        Runnable showTemplate=()-> {
            if(type.getSelectedItem() instanceof RoomType selected) { capacity.setText(Integer.toString(selected.capacity())); assets.setText(selected.description()); }
            else assets.setText("Phòng cũ: giữ nguyên tài sản hiện có. Chọn một trong ba mẫu nếu muốn chuyển loại.");
        };
        showTemplate.run();
        type.addActionListener(e -> { showTemplate.run(); if(type.getSelectedItem() instanceof RoomType selected) price.setText(selected.price().toPlainString()); });
        if (!confirmForm("Thông tin phòng", new Object[]{"Mã phòng:", id, "Số phòng:", number, "Loại phòng:", type,
                "Đơn giá/tháng (có thể chỉnh):", price, "Sức chứa tự động:", capacity, "Tài sản mặc định:", new JScrollPane(assets)})) return;
        if (required(id, number, price, capacity)) {
            error("Vui lòng nhập đầy đủ thông tin phòng.");
            return;
        }
        try {
            BigDecimal amount = new BigDecimal(price.getText().trim());
            int max = Integer.parseInt(capacity.getText().trim());
            runAction(() -> {
                dao.saveRoom(id.getText().trim(), number.getText().trim(), type.getSelectedItem().toString(), amount, max, insert);
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
        refreshStudents();
        if(studentOverview!=null) studentOverview.reload();
        refreshRooms();
        refreshServices();
        refreshUsages();
        refreshVehicles();
        refreshGuests();
    }

    private void refreshRooms() { load(roomTable, KtxDao.ROOM_COLUMNS, dao::getRooms); }
    private void refreshServices() { load(serviceTable, KtxDao.SERVICE_COLUMNS, dao::getServices); }
    private void refreshUsages() { load(usageTable, KtxDao.USAGE_COLUMNS, dao::getUsages); }
    private void refreshVehicles() { load(vehicleTable, KtxDao.VEHICLE_COLUMNS, dao::getVehicles); }
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
                .append("Tiền phòng + dịch vụ cơ bản + xe vé tháng + phí phát sinh + điện nước + tiền phạt").append(System.lineSeparator());
        for (int i = 0; i < invoiceTable.getRowCount(); i++) {
            Object[] row = new Object[invoiceTable.getColumnCount()];
            for (int j = 0; j < row.length; j++) row[j] = invoiceTable.getValueAt(i, j);
            out.append(invoiceTextForRow(row)).append(System.lineSeparator());
        }
        return out.toString();
    }

    private String invoiceTextForRow(Object[] row) {
        return String.format("%s | %s | Phòng %s | Phòng: %s | Dịch vụ: %s | Xe tháng: %s | Xe phát sinh: %s | Điện nước: %s | Phạt: %s | TỔNG: %s",
                row[0], row[1], row[2], money(row[3]), money(row[4]), money(row[5]), money(row[6]), money(row[8]), money(row[9]), money(row[7]));
    }

    private String money(Object value) {
        if (value instanceof Number n) return String.format("%,.0f đ", n.doubleValue());
        return String.valueOf(value);
    }

    private void load(JTable table, String[] columns, SqlSupplier supplier) {
        int request=table.getClientProperty("request") instanceof Integer n?n+1:1;
        table.putClientProperty("request",request); table.putClientProperty("loading",Boolean.TRUE); table.repaint(); table.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
        if(workspace!=null) workspace.status("Đang tải dữ liệu…");
        new javax.swing.SwingWorker<List<Object[]>,Void>() {
            @Override protected List<Object[]> doInBackground() throws Exception { return supplier.get(); }
            @Override protected void done() {
                if(!java.util.Objects.equals(table.getClientProperty("request"),request)) return;
                table.putClientProperty("loading",Boolean.FALSE);
                table.setCursor(java.awt.Cursor.getDefaultCursor());
                try {
                    List<Object[]> data=get();
                    String[] headers=columns;
                    if(table==studentTable) { headers=java.util.Arrays.copyOf(columns,columns.length+1); headers[columns.length]="Trạng thái"; }
                    DefaultTableModel model=new ReadOnlyTableModel(headers);
                    for(Object[] row:data) {
                        if(table==studentTable) { Object[] extended=java.util.Arrays.copyOf(row,9); extended[8]=row.length>8?row[8]:row[6]==null || row[6].toString().isBlank()?"Chờ xếp phòng":"Đang ở"; model.addRow(extended); }
                        else model.addRow(row);
                    }
                    table.setModel(model); table.setAutoCreateRowSorter(true); UiTheme.styleTable(table);
                    if(table==studentTable) {
                        for(int index:new int[]{5,3,1}) table.removeColumn(table.getColumnModel().getColumn(index));
                        table.getColumnModel().getColumn(1).setPreferredWidth(220);
                        table.getColumnModel().getColumn(4).setPreferredWidth(140);
                        String selected=String.valueOf(studentRoomFilter.getSelectedItem());
                        java.util.TreeSet<String> roomIds=new java.util.TreeSet<>();
                        for(Object[] row:data) if(row[6]!=null && !row[6].toString().isBlank()) roomIds.add(row[6].toString());
                        studentRoomFilter.removeAllItems(); studentRoomFilter.addItem("Tất cả phòng"); studentRoomFilter.addItem("Chưa xếp phòng");
                        for(String room:roomIds) studentRoomFilter.addItem(room);
                        if(roomIds.contains(selected) || selected.equals("Chưa xếp phòng")) studentRoomFilter.setSelectedItem(selected);
                        applyStudentFilter();
                    }
                    if(workspace!=null) workspace.status("Đã tải "+data.size()+" bản ghi • Dữ liệu từ CSDL");
                } catch(Exception exception) {
                    if(workspace!=null) workspace.status("Không tải được dữ liệu. Hãy thử Làm mới.");
                    if(isDisplayable()) showError(exception.getCause() instanceof Exception e?e:exception);
                }
            }
        }.execute();
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
        Object[] data = new Object[table.getModel().getColumnCount()];
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
        JButton button=new JButton(label); UiTheme.styleButton(button); return button;
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
        UiTheme.decorate(form);
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

}
