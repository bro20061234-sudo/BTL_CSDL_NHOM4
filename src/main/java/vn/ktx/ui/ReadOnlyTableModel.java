package vn.ktx.ui;

import javax.swing.table.DefaultTableModel;

/** Giữ đúng kiểu cột để sắp xếp số tiền, số lượng và ngày theo giá trị thật. */
public final class ReadOnlyTableModel extends DefaultTableModel {
    public ReadOnlyTableModel(Object[][] rows,String[] columns) { super(rows,columns); }
    public ReadOnlyTableModel(String[] columns) { super(columns,0); }
    @Override public boolean isCellEditable(int row,int column) { return false; }
    @Override public Class<?> getColumnClass(int column) {
        for(int row=0;row<getRowCount();row++) if(getValueAt(row,column)!=null) return getValueAt(row,column).getClass();
        return Object.class;
    }
}
