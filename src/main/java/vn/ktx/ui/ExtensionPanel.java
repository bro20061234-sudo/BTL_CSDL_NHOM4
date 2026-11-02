package vn.ktx.ui;

import vn.ktx.dao.AuthDao;
import vn.ktx.dao.ExtensionDao;
import vn.ktx.dao.ExtensionDao.Module;
import vn.ktx.dao.KtxDao;
import vn.ktx.model.UserSession;
import javax.swing.*;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

/** Các màn hình mở rộng dùng cùng kiểu bảng và hộp thoại với ứng dụng gốc. */
public final class ExtensionPanel extends JPanel {
    private final UserSession user;
    private final ExtensionDao dao;
    public ExtensionPanel(UserSession user) {
        this(user,true);
    }
    public ExtensionPanel(UserSession user,boolean standalone) {
        super(new BorderLayout()); this.user=user; dao=new ExtensionDao(user);
        if(!standalone) return;
        WorkspacePanel workspace=new WorkspacePanel(user,()->vn.ktx.Main.logout(this));
        workspace.group("Lưu trú của tôi");
        workspace.page("contract","Hợp đồng","Hợp đồng lưu trú","Thời hạn lưu trú, tiền cọc và trạng thái hợp đồng.",()->modulePanel(Module.CONTRACT));
        workspace.page("application","Đăng ký lưu trú","Đăng ký lưu trú","Chọn loại phòng, gửi đơn và theo dõi kết quả xét duyệt.",()->modulePanel(Module.APPLICATION));
        workspace.page("transfer","Chuyển phòng","Yêu cầu chuyển phòng","Gửi yêu cầu chuyển sang phòng khác và theo dõi trạng thái.",()->modulePanel(Module.TRANSFER));
        workspace.group("Dịch vụ & tài chính");
        workspace.page("billing","Hóa đơn & công nợ","Hóa đơn & công nợ","Xem chi phí theo tháng, số tiền đã trả và còn nợ.",this::billingPanel);
        workspace.page("payments","Lịch sử thanh toán","Lịch sử thanh toán","Tra cứu các khoản đã thanh toán và xuất biên nhận.",()->modulePanel(Module.PAYMENT));
        workspace.page("utilities","Điện nước","Điện nước","Xem chỉ số điện nước của các kỳ đã phân bổ cho bạn.",()->modulePanel(Module.UTILITY));
        workspace.group("Hỗ trợ & thông tin");
        workspace.page("repairs","Báo hỏng & sửa chữa","Báo hỏng & sửa chữa","Mô tả sự cố trong phòng và theo dõi tiến độ xử lý.",()->modulePanel(Module.REPAIR));
        workspace.page("assets","Tài sản phòng","Tài sản phòng","Danh sách tài sản và tình trạng bàn giao của phòng hiện tại.",()->modulePanel(Module.ASSET));
        workspace.page("notices","Thông báo","Thông báo","Thông báo chung của ký túc xá và thông báo dành cho phòng bạn.",()->modulePanel(Module.NOTICE));
        workspace.page("violations","Nội quy & vi phạm","Nội quy & vi phạm","Xem biên bản, hình thức xử lý và tiền phạt nếu có.",()->modulePanel(Module.VIOLATION));
        workspace.page("reminders","Nhắc hạn","Nhắc hạn","Theo dõi hạn hợp đồng và các khoản cần thanh toán.",this::reminderPanel);
        if(user.isAdmin()) workspace.page("accounts","Tài khoản","Quản lý tài khoản","Tạo tài khoản và phân quyền truy cập.",this::accountPanel);
        add(workspace,BorderLayout.CENTER); workspace.showPage("billing");
    }
    public void createTransfer(String studentId) {
        run(()-> { pendingStudent=studentId; try { if(form(Module.TRANSFER)!=null) message("Đã gửi yêu cầu chuyển phòng. Mở Đăng ký & chuyển phòng để duyệt."); } finally { pendingStudent=null; } });
    }
    private String pendingStudent;
    private java.time.YearMonth pendingMonth;
    private String pendingRoom;
    public JPanel modulePanel(Module m) {
        JPanel panel=new JPanel(new BorderLayout(6,6)); JTable table=table(); JPanel actions=new JPanel(new WrapLayout());
        JComboBox<String> assetRoom=m==Module.ASSET?new JComboBox<>(new String[]{"— Chọn phòng —"}):null;
        if(assetRoom!=null) { actions.add(new JLabel("Phòng:"));actions.add(assetRoom);table.putClientProperty("emptyMessage","Chọn phòng để xem tài sản của phòng đó."); }
        MonthPicker monthFilter=(m==Module.UTILITY || m==Module.PAYMENT)?new MonthPicker(java.time.YearMonth.now().minusMonths(m==Module.UTILITY?1:0)):null;
        if(monthFilter!=null) { actions.add(new JLabel(m==Module.UTILITY?"Tháng sử dụng:":"Tháng hóa đơn:")); actions.add(monthFilter); }
        Runnable refresh=()-> {
            LocalDate month=monthFilter==null?null:monthFilter.getMonth().atDay(1);
            String room=assetRoom==null || assetRoom.getSelectedIndex()<=0?null:(String)assetRoom.getSelectedItem();
            if(assetRoom!=null) table.putClientProperty("emptyMessage",room==null?"Chọn phòng để xem tài sản của phòng đó.":"Phòng "+room+" chưa có tài sản.");
            loadAsync(table,m.headers,()->assetRoom==null?dao.list(m,month):dao.assets(room));
        };
        if(assetRoom!=null) {
            assetRoom.addActionListener(e->refresh.run());
            panel.addHierarchyListener(e->{
                if((e.getChangeFlags()&java.awt.event.HierarchyEvent.SHOWING_CHANGED)!=0 && panel.isShowing()) {
                    new SwingWorker<List<String>,Void>() {
                        protected List<String> doInBackground() throws Exception { return dao.assetRooms(); }
                        protected void done() {
                            try {
                                String previous=(String)assetRoom.getSelectedItem();List<String> rooms=get();
                                assetRoom.removeAllItems();assetRoom.addItem("— Chọn phòng —");rooms.forEach(assetRoom::addItem);
                                if(rooms.contains(previous)) assetRoom.setSelectedItem(previous);else if(rooms.size()==1) assetRoom.setSelectedIndex(1);
                            } catch(Exception failure) { message("Không tải được danh sách phòng. Hãy mở lại mục Tài sản phòng."); }
                        }
                    }.execute();
                }
            });
        }
        if(monthFilter!=null) monthFilter.onChange(refresh);
        button(actions,"Làm mới",refresh);
        String createLabel=switch(m) {
            case APPLICATION -> "Gửi đơn đăng ký"; case CONTRACT -> "Lập hợp đồng"; case TRANSFER -> "Yêu cầu chuyển phòng";
            case REPAIR -> "Báo hỏng"; case ASSET -> "Thêm tài sản"; case UTILITY -> "Lập phí điện nước";
            case PAYMENT -> "Ghi nhận thanh toán"; case NOTICE -> "Tạo thông báo"; case VIOLATION -> "Lập biên bản";
        };
        if(user.isStaff() || m.studentCanCreate) button(actions,createLabel,()->run(()->{
            if(assetRoom!=null && assetRoom.getSelectedIndex()<=0) { message("Chọn phòng trước khi thêm tài sản.");return; }
            pendingRoom=assetRoom==null?null:(String)assetRoom.getSelectedItem();
            pendingMonth=monthFilter==null?null:monthFilter.getMonth();
            try { Map<String,String> values=form(m); if(values!=null) { refresh.run(); message("Đã lưu "+m.title.toLowerCase()+"."); } }
            finally { pendingMonth=null; pendingRoom=null; }
        }));
        if(user.isStaff()) switch(m) {
            case APPLICATION, TRANSFER -> {
                button(actions,"Duyệt",()->run(()->{
                    Object[] row=selected(table); if(row==null) return;
                    String room=null;
                    if(m==Module.APPLICATION) {
                        List<ExtensionDao.ApplicationRoom> available=dao.applicationRooms(id(row));
                        JComboBox<ExtensionDao.ApplicationRoom> rooms=new JComboBox<>(available.toArray(ExtensionDao.ApplicationRoom[]::new));
                        UiTheme.decorate(rooms);
                        if(JOptionPane.showConfirmDialog(this,rooms,"Xếp phòng · "+row[1]+" · "+row[2],JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION) return;
                        ExtensionDao.ApplicationRoom chosen=(ExtensionDao.ApplicationRoom)rooms.getSelectedItem();
                        if(chosen!=null) room=chosen.id();
                    }
                    if(m==Module.APPLICATION && room==null) return;
                    if(confirm("Duyệt yêu cầu số "+row[0]+"?")) { dao.transition(m,id(row),"APPROVE",room); refresh.run();message(m==Module.APPLICATION?"Đã duyệt và xếp sinh viên "+row[1]+" vào phòng "+room+".":"Đã duyệt chuyển phòng."); }
                }));
                button(actions,"Từ chối",()->run(()->{
                    Object[] row=selected(table); if(row!=null && confirm("Từ chối yêu cầu số "+row[0]+"?")) { dao.transition(m,id(row),"REJECT",null); refresh.run(); }
                }));
            }
            case CONTRACT -> {
                button(actions,"Gia hạn",()->run(()->{
                    Object[] row=selected(table); if(row==null) return;
                    String end=JOptionPane.showInputDialog(this,"Ngày kết thúc mới (yyyy-MM-dd)",row[4].toString());
                    if(end!=null) { dao.transition(m,id(row),"RENEW",end); refresh.run(); }
                }));
                button(actions,"Thanh lý",()->run(()->{
                    Object[] row=selected(table);
                    if(row!=null && confirm("Thanh lý hợp đồng và trả phòng? Tiền cọc cần đối soát với sinh viên.")) { dao.transition(m,id(row),"CLOSE",null); refresh.run(); }
                }));
            }
            case REPAIR -> button(actions,"Cập nhật tiến độ",()->run(()->{
                Object[] row=selected(table); if(row==null) return;
                String next=choose("Trạng thái tiếp theo",List.of("DANG_XU_LY","HOAN_THANH"));
                if(next!=null) { dao.transition(m,id(row),"STATUS",next); refresh.run(); }
            }));
            case ASSET -> button(actions,"Cập nhật tình trạng",()->run(()->{
                Object[] row=selected(table); if(row==null) return;
                String status=JOptionPane.showInputDialog(this,"Tình trạng (tốt / hỏng / mất...)",row[4]);
                if(status!=null) { dao.transition(m,id(row),"STATUS",status); refresh.run(); }
            }));
            default -> { }
        }
        if(m==Module.PAYMENT) button(actions,"Xuất biên nhận",()->run(()->{
            Object[] row=selected(table); if(row==null) return;
            export("BIÊN NHẬN THANH TOÁN KTX\nMã biên nhận: "+row[0]+"\nSinh viên: "+row[1]+"\nKỳ: "+row[2]+"\nSố tiền: "+row[3]+" đồng\nNgày thanh toán: "+row[4]+"\nGhi chú: "+row[5]+"\n");
        }));
        if(m==Module.REPAIR) button(actions,"Xem ảnh",()->run(()->{
            Object[] row=selected(table); if(row==null || row[4]==null || row[4].toString().isBlank()) return;
            java.nio.file.Path path=java.nio.file.Path.of(row[4].toString());
            java.awt.image.BufferedImage picture=javax.imageio.ImageIO.read(path.toFile());
            if(picture==null) throw new IllegalArgumentException("Tệp không phải ảnh được hỗ trợ.");
            Image scaled=picture.getScaledInstance(640,-1,Image.SCALE_SMOOTH);
            JOptionPane.showMessageDialog(this,new JScrollPane(new JLabel(new ImageIcon(scaled))),"Ảnh báo hỏng",JOptionPane.PLAIN_MESSAGE);
        }));
        button(actions,"Xem chi tiết",()-> {
            Object[] row=selected(table); if(row==null) return;
            JPanel fields=new JPanel(); fields.setLayout(new BoxLayout(fields,BoxLayout.Y_AXIS)); fields.setBorder(new javax.swing.border.EmptyBorder(14,14,14,14));
            for(int i=0;i<m.headers.length;i++) {
                JLabel caption=UiTheme.label(m.headers[i],13,true); caption.setAlignmentX(LEFT_ALIGNMENT); fields.add(caption); fields.add(Box.createVerticalStrut(6));
                JTextArea value=new JTextArea(UiTheme.status(Objects.toString(row[i],"—"))); value.setEditable(false); value.setLineWrap(true); value.setWrapStyleWord(true); value.setRows(Objects.toString(row[i],"").length()>100?3:1); value.setBackground(UiTheme.CANVAS); value.setBorder(new javax.swing.border.EmptyBorder(8,10,8,10)); value.setAlignmentX(LEFT_ALIGNMENT); fields.add(value); fields.add(Box.createVerticalStrut(14));
            }
            JScrollPane scroll=new JScrollPane(fields); scroll.setPreferredSize(new Dimension(650,Math.min(600,m.headers.length*82)));
            JOptionPane.showMessageDialog(this,scroll,m.title+" · "+row[0],JOptionPane.PLAIN_MESSAGE);
        });
        button(actions,"Xuất danh sách",()->run(()->exportTable(table,m.title)));
        panel.add(actions,BorderLayout.NORTH); panel.add(new JScrollPane(table),BorderLayout.CENTER);
        String helpText=switch(m) {
            case APPLICATION -> " Đơn xin xếp phòng cho sinh viên CHƯA CÓ PHÒNG. Chọn đơn Chờ duyệt → Duyệt → chọn phòng đúng loại còn chỗ. Sinh viên đã có phòng dùng tab Chuyển phòng.";
            case TRANSFER -> " Phòng cũ, ngày gửi được lưu tự động; duyệt sẽ chuyển phòng và cập nhật hợp đồng trong cùng giao dịch.";
            case UTILITY -> " Ngày 01 lập tiền tháng trước (01/11 → tháng 10). Chia cho sinh viên đã ở trong tháng sử dụng; mỗi phòng/tháng chỉ lập một lần.";
            case CONTRACT -> " Mỗi sinh viên chỉ có một hợp đồng hiệu lực; gia hạn ngày kết thúc hoặc thanh lý và trả phòng.";
            case PAYMENT -> " Chọn tháng hóa đơn. Thanh toán toàn bộ số còn nợ hoặc nhập số tiền trả một phần; không được thu vượt nợ.";
            case NOTICE -> " Phòng để trống: toàn KTX; chọn mã phòng: chỉ cư dân phòng đó. Hạn dùng cho đóng tiền hoặc lịch bảo trì.";
            default -> " Bấm Làm mới để xem dữ liệu. Chọn một dòng để thực hiện thao tác.";
        };
        JLabel help=new JLabel("<html><body style='width:650px'>"+helpText+"</body></html>"); help.setForeground(UiTheme.MUTED); help.setBorder(new javax.swing.border.EmptyBorder(10,0,0,0));
        panel.add(help,BorderLayout.SOUTH);
        // Chỉ truy vấn khi mở tab, tránh hàng loạt kết nối khi khởi động.
        panel.addHierarchyListener(e->{ if((e.getChangeFlags()&java.awt.event.HierarchyEvent.SHOWING_CHANGED)!=0 && panel.isShowing()) refresh.run(); });
        return panel;
    }
    private record Input(String key,String label,String type,String initial) { }
    private Input f(String key,String label,String type,String initial) { return new Input(key,label,type,initial); }
    private Map<String,String> form(Module m) throws Exception {
        String today=LocalDate.now().toString(),period=(pendingMonth!=null?pendingMonth:java.time.YearMonth.now().minusMonths(m==Module.UTILITY?1:0)).atDay(1).toString();
        Input sv=f("MaSV","Sinh viên","student","");
        Input room=f("MaPhong","Phòng","room","");
        Input ky=f("Ky",m==Module.UTILITY?"Tháng sử dụng:":"Tháng hóa đơn:","month",period);
        List<Input> inputs=switch(m) {
            case APPLICATION -> List.of(sv,f("LoaiPhong","Loại phòng","type",""),f("GhiChu","Ghi chú","text",""));
            case CONTRACT -> List.of(sv,room,f("NgayBatDau","Ngày bắt đầu","text",today),f("NgayKetThuc","Ngày kết thúc","text",LocalDate.now().plusMonths(6).toString()),f("TienCoc","Tiền đặt cọc","text","0"));
            case TRANSFER -> List.of(sv,f("PhongMoi","Phòng muốn chuyển","room",""),f("LyDo","Lý do","text",""));
            case REPAIR -> List.of(sv,f("MoTa","Mô tả hư hỏng","text",""),f("Anh","Ảnh (tùy chọn)","image",""));
            case ASSET -> List.of(room,f("TenTaiSan","Tên tài sản","text",""),f("SoLuong","Số lượng","text","1"),f("TinhTrang","Tình trạng","text","Tốt"),f("BienBan","Biên bản bàn giao","text",""),f("NgayBanGiao","Ngày bàn giao","text",today));
            case UTILITY -> List.of(room,f("NgayLap","Ngày lập (tính cho tháng trước)","text",today),ky,f("DienCu","Điện cũ (kWh)","text","0"),f("DienMoi","Điện mới (kWh)","text","0"),f("NuocCu","Nước cũ (m³)","text","0"),f("NuocMoi","Nước mới (m³)","text","0"),f("GiaDien","Giá điện / kWh","text","3000"),f("GiaNuoc","Giá nước / m³","text","15000"));
            case PAYMENT -> List.of(sv,ky,f("CheDo","Cách thanh toán","paymentMode",""),f("SoDuNo","Dư nợ tháng đã chọn (đồng)","text",""),f("SoTien","Số tiền thanh toán (đồng)","text",""),f("GhiChu","Ghi chú / phương thức","text","Tiền mặt"));
            case NOTICE -> List.of(f("TieuDe","Tiêu đề","text",""),f("NoiDung","Nội dung","text",""),f("MaPhong","Phòng (trống = toàn KTX)","optionalRoom",""),f("Han","Hạn / Ngày thực hiện","text",today));
            case VIOLATION -> List.of(sv,f("NgayViPham","Ngày vi phạm","text",today),f("NoiDung","Nội dung vi phạm","text",""),f("HinhThuc","Hình thức xử lý","treatment",""),f("TienPhat","Tiền phạt hành chính (đồng)","text","0"),f("XuLy","Ghi chú / quyết định xử lý","text",""));
        };
        JPanel panel=new JPanel(new GridLayout(0,2,12,14)); Map<String,JComponent> controls=new LinkedHashMap<>();
        for(Input input:inputs) {
            JComponent control;
            switch(input.type) {
                case "month" -> control=new MonthPicker(java.time.YearMonth.from(LocalDate.parse(input.initial)));
                case "paymentMode","treatment" -> {
                    JComboBox<String> options=combo(input.type.equals("paymentMode")?List.of("FULL","PARTIAL"):List.of("NHAC_NHO","HANH_CHINH","DINH_CHI"));
                    options.setRenderer(new DefaultListCellRenderer() { @Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus) { return super.getListCellRendererComponent(list,UiTheme.status(Objects.toString(value,"")),index,selected,focus); } });
                    control=options;
                }
                case "student" -> { JComboBox<String> students=combo(dao.students()); if(pendingStudent!=null) students.setSelectedItem(pendingStudent); control=students; }
                case "room" -> { JComboBox<String> rooms=combo(dao.rooms());if(m==Module.ASSET && pendingRoom!=null) { rooms.setSelectedItem(pendingRoom);rooms.setEnabled(false); }control=rooms; }
                case "type" -> control=combo(dao.roomTypes());
                case "optionalRoom" -> { List<String> rooms=new ArrayList<>(dao.rooms()); rooms.add(0,""); control=combo(rooms); }
                default -> {
                    if(input.key.startsWith("Ngay") || input.key.equals("Han") || input.key.equals("Ky")) {
                        com.toedter.calendar.JDateChooser date=new com.toedter.calendar.JDateChooser(); date.setDateFormatString("dd/MM/yyyy");
                        if(!input.initial.isEmpty()) date.setDate(new java.util.Date(java.sql.Date.valueOf(input.initial).getTime())); control=date;
                    } else if(Set.of("MoTa","NoiDung","LyDo","BienBan","GhiChu","XuLy").contains(input.key)) {
                        JTextArea area=new JTextArea(input.initial,3,28); area.setLineWrap(true); area.setWrapStyleWord(true); area.setBorder(new javax.swing.border.LineBorder(UiTheme.LINE)); control=area;
                    } else control=new JTextField(input.initial,28);
                }
            }
            controls.put(input.key,control); panel.add(new JLabel(input.label));
            if("image".equals(input.type)) {
                JPanel imagePanel=new JPanel(new BorderLayout()); imagePanel.add(control,BorderLayout.CENTER);
                JButton browse=new JButton("Chọn..."); imagePanel.add(browse,BorderLayout.EAST);
                JTextField target=(JTextField)control;
                browse.addActionListener(e->{ JFileChooser chooser=new JFileChooser(); if(chooser.showOpenDialog(this)==JFileChooser.APPROVE_OPTION) target.setText(chooser.getSelectedFile().getAbsolutePath()); });
                panel.add(imagePanel);
            } else if(control instanceof JTextArea) panel.add(new JScrollPane(control));
            else panel.add(control);
        }
        if(m==Module.UTILITY) {
            com.toedter.calendar.JDateChooser issued=(com.toedter.calendar.JDateChooser)controls.get("NgayLap");
            issued.addPropertyChangeListener("date",event->{ if(issued.getDate()!=null) ((MonthPicker)controls.get("Ky")).setMonth(java.time.YearMonth.from(issued.getDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()).minusMonths(1)); });
        }
        if(m==Module.VIOLATION) {
            JComboBox<?> kind=(JComboBox<?>)controls.get("HinhThuc"); JTextField fine=(JTextField)controls.get("TienPhat");
            Runnable toggle=()-> { boolean enabled="HANH_CHINH".equals(kind.getSelectedItem()); fine.setEditable(enabled); if(!enabled) fine.setText("0"); };
            kind.addActionListener(e->toggle.run()); toggle.run();
            panel.add(new JLabel("Đình chỉ lưu trú:")); panel.add(new JLabel("<html>Trả phòng, chấm dứt hợp đồng và vé tháng.<br>Giữ lịch sử, công nợ; chặn xếp phòng lại.</html>"));
        }
        if(m==Module.PAYMENT) bindPayment(controls);
        UiTheme.decorate(panel);
        while(JOptionPane.showConfirmDialog(this,panel,"Thêm · "+m.title,JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION) {
            Map<String,String> values=new LinkedHashMap<>();
            controls.forEach((key,c)-> {
                String value;
                if(c instanceof MonthPicker month) value=month.getMonth().atDay(1).toString();
                else if(c instanceof JComboBox<?> cb) value=Objects.toString(cb.getSelectedItem(),"");
                else if(c instanceof com.toedter.calendar.JDateChooser date) value=date.getDate()==null?"":date.getDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString();
                else value=((javax.swing.text.JTextComponent)c).getText().trim();
                values.put(key,value);
            });
            if(m==Module.VIOLATION && "DINH_CHI".equals(values.get("HinhThuc")) && !confirm("Đình chỉ lưu trú sinh viên "+values.get("MaSV")+"? Sinh viên sẽ bị trả phòng, chấm dứt hợp đồng/vé tháng và không thể xếp phòng lại.")) continue;
            // Giữ dữ liệu người dùng vừa nhập khi có lỗi xác thực.
            try { dao.create(m,values); return values; }
            catch(Exception e) { JOptionPane.showMessageDialog(this,e.getMessage(),"Không thể lưu",JOptionPane.WARNING_MESSAGE); }
        }
        return null;
    }
    private void bindPayment(Map<String,JComponent> controls) {
        JComboBox<?> student=(JComboBox<?>)controls.get("MaSV"),mode=(JComboBox<?>)controls.get("CheDo");
        MonthPicker month=(MonthPicker)controls.get("Ky"); JTextField debt=(JTextField)controls.get("SoDuNo"),amount=(JTextField)controls.get("SoTien");
        debt.setEditable(false); int[] request={0};
        Runnable toggle=()->{ boolean full="FULL".equals(mode.getSelectedItem()); amount.setEditable(!full); if(full) amount.setText(debt.getText()); else amount.setText(""); };
        Runnable refresh=()->{
            String sv=Objects.toString(student.getSelectedItem(),""); LocalDate selected=month.getMonth().atDay(1); int ticket=++request[0]; debt.setText("Đang tải…");
            new SwingWorker<java.math.BigDecimal,Void>() {
                protected java.math.BigDecimal doInBackground() throws Exception { return dao.remaining(sv,selected); }
                protected void done() { if(ticket!=request[0]) return; try { debt.setText(get().toPlainString()); if("FULL".equals(mode.getSelectedItem())) amount.setText(debt.getText()); } catch(Exception error) { debt.setText("Không tải được dư nợ"); } }
            }.execute();
        };
        student.addActionListener(e->refresh.run()); month.onChange(refresh); mode.addActionListener(e->toggle.run());toggle.run();refresh.run();
    }
    public JPanel billingPanel() {
        JPanel panel=new JPanel(new BorderLayout()); JPanel actions=new JPanel(new WrapLayout()); JTable table=table();
        MonthPicker period=new MonthPicker(java.time.YearMonth.now());
        actions.add(period);
        Runnable refresh=()->run(()-> { LocalDate d=period.getMonth().atDay(1); loadAsync(table,new String[]{"Sinh viên","Tháng","Tổng phải trả","Đã trả","Còn nợ"},()->dao.debts(d)); });
        period.onChange(refresh);
        button(actions,"Xem công nợ",refresh);
        button(actions,"Chi tiết hóa đơn",()->run(()->{
            LocalDate d=period.getMonth().atDay(1);
            List<Object[]> rows=new ArrayList<>(new KtxDao().getInvoices(d.getMonthValue(),d.getYear()));
            if(!user.isStaff()) rows.removeIf(r->!user.studentId().equals(r[0]));
            JTable detail=table(); fill(detail,KtxDao.INVOICE_COLUMNS,rows);
            JScrollPane scroll=new JScrollPane(detail); scroll.setPreferredSize(new Dimension(1100,350));
            JOptionPane.showMessageDialog(this,scroll,"Hóa đơn "+d,JOptionPane.PLAIN_MESSAGE);
        }));
        button(actions,"Xuất công nợ",()->run(()->exportTable(table,"CÔNG NỢ KTX")));
        panel.add(actions,BorderLayout.NORTH); panel.add(new JScrollPane(table),BorderLayout.CENTER);
        panel.addHierarchyListener(e->{ if((e.getChangeFlags()&java.awt.event.HierarchyEvent.SHOWING_CHANGED)!=0 && panel.isShowing()) refresh.run(); });
        return panel;
    }
    public JPanel reminderPanel() {
        JPanel panel=new JPanel(new BorderLayout()); JTable table=table(); JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT));
        Runnable refresh=()->loadAsync(table,new String[]{"Sinh viên","Ngày / Kỳ","Nội dung nhắc hạn"},dao::reminders);
        button(actions,"Kiểm tra hạn hợp đồng / Công nợ 3 tháng gần nhất",refresh);
        panel.add(actions,BorderLayout.NORTH); panel.add(new JScrollPane(table),BorderLayout.CENTER);
        panel.addHierarchyListener(e->{ if((e.getChangeFlags()&java.awt.event.HierarchyEvent.SHOWING_CHANGED)!=0 && panel.isShowing()) refresh.run(); });
        return panel;
    }
    public JPanel accountPanel() {
        JPanel panel=new JPanel(new BorderLayout()); JTable table=table(); JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT));
        Runnable refresh=()->loadAsync(table,new String[]{"Tên đăng nhập","Vai trò","Sinh viên"},dao::accounts);
        button(actions,"Làm mới",refresh);
        button(actions,"Tạo tài khoản",()->run(()->{
            JTextField name=new JTextField(20); JPasswordField password=new JPasswordField(20);
            JComboBox<String> role=combo(List.of("STUDENT","STAFF","ADMIN")); JComboBox<String> student=combo(dao.students());
            JPanel form=new JPanel(new GridLayout(0,2,8,8));
            form.add(new JLabel("Tên đăng nhập")); form.add(name); form.add(new JLabel("Mật khẩu (ít nhất 8 ký tự)")); form.add(password);
            form.add(new JLabel("Vai trò")); form.add(role); form.add(new JLabel("Mã SV (chỉ vai trò STUDENT)")); form.add(student);
            role.setRenderer(new DefaultListCellRenderer() { @Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus) { return super.getListCellRendererComponent(list,UiTheme.status(Objects.toString(value,"")),index,selected,focus); } });
            role.addActionListener(e->student.setEnabled("STUDENT".equals(role.getSelectedItem())));
            UiTheme.decorate(form);
            if(JOptionPane.showConfirmDialog(this,form,"Tạo tài khoản",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION) return;
            char[] secret=password.getPassword();
            try { new AuthDao().create(user,name.getText(),secret,Objects.toString(role.getSelectedItem()),(String)student.getSelectedItem()); }
            finally { Arrays.fill(secret,'\0'); password.setText(""); }
            refresh.run();
        }));
        panel.add(actions,BorderLayout.NORTH); panel.add(new JScrollPane(table),BorderLayout.CENTER);
        panel.addHierarchyListener(e->{ if((e.getChangeFlags()&java.awt.event.HierarchyEvent.SHOWING_CHANGED)!=0 && panel.isShowing()) refresh.run(); });
        return panel;
    }
    private LocalDate readPeriod(JTextField field) {
        LocalDate d=LocalDate.parse(field.getText().trim());
        if(d.getDayOfMonth()!=1 || d.getYear()<2000 || d.getYear()>2200) throw new IllegalArgumentException("Kỳ phải là ngày đầu tháng, năm 2000–2200."); return d;
    }
    private JTable table() { JTable t=new DataTable(); t.setAutoCreateRowSorter(true); UiTheme.styleTable(t); return t; }
    private JComboBox<String> combo(List<String> values) { return new JComboBox<>(values.toArray(String[]::new)); }
    private void fill(JTable table,String[] headers,List<Object[]> rows) {
        table.setModel(new ReadOnlyTableModel(rows.toArray(Object[][]::new),headers));
        UiTheme.styleTable(table);
    }
    private void loadAsync(JTable table,String[] headers,Rows source) {
        int request=table.getClientProperty("request") instanceof Integer n?n+1:1; table.putClientProperty("request",request);
        table.putClientProperty("loading",Boolean.TRUE); table.repaint();
        table.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)); table.setToolTipText("Đang tải dữ liệu…");
        if(table.getColumnCount()==0) fill(table,headers,List.of());
        new SwingWorker<List<Object[]>,Void>() {
            @Override protected List<Object[]> doInBackground() throws Exception { return source.get(); }
            @Override protected void done() {
                if(!Objects.equals(table.getClientProperty("request"),request)) return;
                table.putClientProperty("loading",Boolean.FALSE);
                table.setCursor(Cursor.getDefaultCursor()); table.setToolTipText(null);
                try { fill(table,headers,get()); }
                catch(Exception e) { Throwable cause=e.getCause()==null?e:e.getCause(); JOptionPane.showMessageDialog(ExtensionPanel.this,cause.getMessage(),"Không tải được dữ liệu",JOptionPane.ERROR_MESSAGE); }
            }
        }.execute();
    }
    private Object[] selected(JTable table) {
        int view=table.getSelectedRow(); if(view<0) { message("Hãy chọn một dòng trước."); return null; }
        int row=table.convertRowIndexToModel(view); Object[] values=new Object[table.getColumnCount()];
        for(int i=0;i<values.length;i++) values[i]=table.getModel().getValueAt(row,i); return values;
    }
    private int id(Object[] row) { return ((Number)row[0]).intValue(); }
    private void button(JPanel panel,String title,Runnable action) { JButton b=new JButton(title); UiTheme.styleButton(b); b.addActionListener(e->action.run()); panel.add(b); }
    private String choose(String title,List<String> values) {
        JComboBox<String> combo=combo(values); combo.setRenderer(new DefaultListCellRenderer() { @Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus) { return super.getListCellRendererComponent(list,UiTheme.status(Objects.toString(value,"")),index,selected,focus); } });
        UiTheme.decorate(combo); return JOptionPane.showConfirmDialog(this,combo,title,JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION?(String)combo.getSelectedItem():null;
    }
    private boolean confirm(String text) { return JOptionPane.showConfirmDialog(this,text,"Xác nhận",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION; }
    private void message(String text) { JOptionPane.showMessageDialog(this,text); }
    private void run(Action action) { try { action.run(); } catch(Exception e) { JOptionPane.showMessageDialog(this,e.getMessage(),"Không thể thực hiện",JOptionPane.ERROR_MESSAGE); } }
    private void export(String text) throws Exception {
        JFileChooser chooser=new JFileChooser();
        if(chooser.showSaveDialog(this)==JFileChooser.APPROVE_OPTION) {
            java.nio.file.Path path=chooser.getSelectedFile().toPath();
            if(Files.exists(path) && !confirm("Tệp đã tồn tại. Ghi đè?")) return;
            Files.writeString(path,text,StandardCharsets.UTF_8); message("Đã xuất tệp UTF-8.");
        }
    }
    private void exportTable(JTable table,String title) throws Exception {
        StringBuilder b=new StringBuilder(title).append('\n');
        for(int i=0;i<table.getColumnCount();i++) b.append(table.getColumnName(i)).append('\t'); b.append('\n');
        for(int r=0;r<table.getRowCount();r++) { for(int c=0;c<table.getColumnCount();c++) b.append(Objects.toString(table.getValueAt(r,c),"").replace('\t',' ').replace('\n',' ')).append('\t'); b.append('\n'); }
        export(b.toString());
    }
    @FunctionalInterface private interface Action { void run() throws Exception; }
    @FunctionalInterface private interface Rows { List<Object[]> get() throws Exception; }
}
