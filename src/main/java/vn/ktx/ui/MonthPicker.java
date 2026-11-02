package vn.ktx.ui;

import javax.swing.*;
import java.awt.*;
import java.time.YearMonth;

/** Chọn tháng/năm; ngày 01 chỉ dùng khi lưu SQL, không yêu cầu người dùng nhập ngày. */
public final class MonthPicker extends JPanel {
    private final JComboBox<Integer> month=new JComboBox<>(new Integer[]{1,2,3,4,5,6,7,8,9,10,11,12});
    private final JSpinner year=new JSpinner(new SpinnerNumberModel(YearMonth.now().getYear(),2000,2200,1));
    public MonthPicker(YearMonth value) {
        super(new FlowLayout(FlowLayout.LEFT,6,0));
        month.setPreferredSize(new Dimension(72,36)); year.setPreferredSize(new Dimension(92,36));
        year.setEditor(new JSpinner.NumberEditor(year,"#"));
        add(new JLabel("Tháng"));add(month);add(new JLabel("Năm"));add(year); setMonth(value);
    }
    public YearMonth getMonth() {
        try { year.commitEdit(); } catch(java.text.ParseException e) { throw new IllegalArgumentException("Năm không hợp lệ."); }
        return YearMonth.of(((Number)year.getValue()).intValue(),(Integer)month.getSelectedItem());
    }
    public void setMonth(YearMonth value) { month.setSelectedItem(value.getMonthValue()); year.setValue(value.getYear()); }
    public void onChange(Runnable callback) { month.addActionListener(e->callback.run()); year.addChangeListener(e->callback.run()); }
}
