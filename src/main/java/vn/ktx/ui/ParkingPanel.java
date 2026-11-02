package vn.ktx.ui;

import vn.ktx.model.ParkingService;
import javax.swing.*;
import java.awt.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/** Một quầy gửi/lấy xe, dùng thời gian thực, nhận diện vé tháng theo biển số. */
public final class ParkingPanel extends JPanel {
    private final ParkingService service;
    private final JTextField plate=new JTextField(16),search=new JTextField(17);
    private final JTable table=new DataTable();
    private final JLabel clock=UiTheme.label("",14,true),status=UiTheme.label("Nhập biển số rồi bấm Gửi xe.",13,false);
    private final JButton enter=UiTheme.primary("Gửi xe"),leave=UiTheme.primary("Lấy xe / tính tiền"),refresh=new JButton("Làm mới");
    private List<ParkingService.Entry> entries=List.of();
    private final Map<String,ParkingService.Entry> byId=new HashMap<>();
    private final javax.swing.Timer ticker;
    private boolean busy;
    private int ticks;
    public ParkingPanel() { this(ParkingService.shared()); }
    public ParkingPanel(ParkingService service) {
        super(new BorderLayout(0,14));this.service=service;
        JPanel header=new JPanel(new BorderLayout(0,12));JPanel send=new JPanel(new WrapLayout());
        send.add(new JLabel("Biển số xe:"));send.add(plate);send.add(enter);send.add(Box.createHorizontalStrut(15));send.add(clock);
        header.add(send,BorderLayout.NORTH);
        JPanel controls=new JPanel(new WrapLayout());controls.add(new JLabel("Tìm xe trong bãi:"));controls.add(search);controls.add(refresh);controls.add(leave);
        header.add(controls,BorderLayout.SOUTH);add(header,BorderLayout.NORTH);add(new JScrollPane(table),BorderLayout.CENTER);
        JPanel footer=new JPanel(new GridLayout(0,1,0,8));footer.add(status);
        footer.add(new JLabel("<html>Vé lượt: 06h–18h 3.000đ/khung; 18h–06h 5.000đ/khung. Vé tháng được nhận diện tự động theo biển số.<br>Chỉ hiển thị xe đang trong bãi. Giờ vào/ra tự ghi; tiền vé lượt thu tại quầy, phí vé tháng vào hóa đơn sinh viên.<br>Danh sách xe vé lượt giữ trong phiên ứng dụng; nhật ký xe vé tháng lưu trong CSDL.</html>"));add(footer,BorderLayout.SOUTH);
        table.putClientProperty("emptyMessage","Bãi đang trống hoặc không tìm thấy biển số.");table.setAutoCreateRowSorter(true);
        table.getSelectionModel().addListSelectionListener(e->leave.setEnabled(!busy && table.getSelectedRow()>=0));leave.setEnabled(false);
        enter.addActionListener(e->enter());plate.addActionListener(e->enter());refresh.addActionListener(e->reload());
        leave.addActionListener(e->{ ParkingService.Entry selected=selected();if(selected!=null) perform(()->service.leave(selected.id()),receipt->{
            String note=receipt.entry().monthly()?"Phí phát sinh ghi vào hóa đơn sinh viên.":"Thu tiền vé lượt tại quầy.";
            status.setText("Đã lấy xe "+receipt.entry().plate()+" lúc "+receipt.exited().format(DateTimeFormatter.ofPattern("HH:mm:ss"))+" · "+money(receipt.amount())+" · "+note);reload();
        }); });
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
        });
        ticker=new javax.swing.Timer(1000,e->{ updateClock();if(!busy) { paintRows();if(++ticks%10==0)reload(); } });
        addHierarchyListener(e->{ if((e.getChangeFlags()&java.awt.event.HierarchyEvent.SHOWING_CHANGED)!=0) { if(isShowing()) { ticker.start();reload(); } else ticker.stop(); } });
        updateClock();paintRows();UiTheme.decorate(this);
    }
    private void enter() {
        String number=plate.getText();perform(()->service.enter(number),entry->{ plate.setText("");plate.requestFocusInWindow();status.setText("Đã gửi xe "+entry.plate()+" · "+(entry.monthly()?"Vé tháng":"Vé lượt")+" · "+entry.entered().format(DateTimeFormatter.ofPattern("HH:mm:ss")));reload(); });
    }
    public void reload() { if(!busy) perform(service::active,rows->{ entries=rows;paintRows(); }); }
    private <T> void perform(Task<T> task,java.util.function.Consumer<T> success) {
        if(busy)return;setBusy(true);
        new SwingWorker<T,Void>() {
            protected T doInBackground() throws Exception { return task.run(); }
            protected void done() {
                setBusy(false);
                try { success.accept(get()); }
                catch(Exception error) { Throwable cause=error.getCause()==null?error:error.getCause();status.setText(cause.getMessage());JOptionPane.showMessageDialog(ParkingPanel.this,cause.getMessage(),"Không thể xử lý gửi xe",JOptionPane.WARNING_MESSAGE); }
            }
        }.execute();
    }
    private void setBusy(boolean value) { busy=value;enter.setEnabled(!value);refresh.setEnabled(!value);leave.setEnabled(!value && table.getSelectedRow()>=0);table.setCursor(value?Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR):Cursor.getDefaultCursor()); }
    private ParkingService.Entry selected() { int view=table.getSelectedRow();return view<0?null:byId.get(table.getModel().getValueAt(table.convertRowIndexToModel(view),0).toString()); }
    private void updateClock() { clock.setText("Giờ hiện tại: "+service.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))); }
    private void paintRows() {
        ParkingService.Entry selected=selected();String id=selected==null?null:selected.id();
        List<? extends javax.swing.RowSorter.SortKey> sort=table.getRowSorter()==null?List.of():new ArrayList<>(table.getRowSorter().getSortKeys());
        byId.clear();LocalDateTime now=service.now();List<Object[]> rows=new ArrayList<>();
        for(ParkingService.Entry entry:entries) { byId.put(entry.id(),entry);rows.add(new Object[]{entry.id(),entry.plate(),entry.monthly()?"Vé tháng":"Vé lượt",entry.student(),java.sql.Timestamp.valueOf(entry.entered()),elapsed(entry.entered(),now),entry.estimate(now)}); }
        table.setModel(new ReadOnlyTableModel(rows.toArray(Object[][]::new),new String[]{"Mã lượt","Biển số","Loại vé","Sinh viên / Khách","Giờ vào","Đã gửi","Phí hiện tại"}));
        UiTheme.styleTable(table);table.removeColumn(table.getColumnModel().getColumn(0));
        table.getColumnModel().getColumn(2).setPreferredWidth(190);table.getColumnModel().getColumn(3).setPreferredWidth(180);
        if(table.getRowSorter()!=null) table.getRowSorter().setSortKeys(sort);filter();
        if(id!=null) for(int r=0;r<table.getModel().getRowCount();r++) if(id.equals(table.getModel().getValueAt(r,0))) { int view=table.convertRowIndexToView(r);if(view>=0)table.setRowSelectionInterval(view,view);break; }
    }
    @SuppressWarnings("unchecked") private void filter() {
        if(table.getRowSorter() instanceof javax.swing.table.TableRowSorter<?> sorter) {
            String number=ParkingService.canonical(search.getText());
            ((javax.swing.table.TableRowSorter<javax.swing.table.TableModel>)sorter).setRowFilter(new RowFilter<>() { public boolean include(Entry<? extends javax.swing.table.TableModel,? extends Integer> row) { return ParkingService.canonical(row.getStringValue(1)).contains(number); } });
        }
    }
    private static String elapsed(LocalDateTime in,LocalDateTime now) { long seconds=Math.max(0,Duration.between(in,now).getSeconds());return (seconds/3600)+" giờ "+(seconds%3600/60)+" phút"; }
    private static String money(java.math.BigDecimal amount) { return String.format(Locale.forLanguageTag("vi-VN"),"%,.0fđ",amount); }
    @FunctionalInterface private interface Task<T> { T run() throws Exception; }
}
