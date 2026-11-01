package vn.ktx.ui;

import vn.ktx.dao.AuthDao;
import vn.ktx.model.UserSession;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public final class LoginDialog {
    private LoginDialog() { }
    public static UserSession login(Component parent) {
        AuthDao dao=new AuthDao();
        while(true) {
            try {
                boolean setup=dao.needsSetup();
                JTextField name=new JTextField(20);
                JPasswordField password=new JPasswordField(20);
                JPanel form=new JPanel(new BorderLayout(0,24)); form.setBorder(new javax.swing.border.EmptyBorder(18,22,18,22));
                JPanel intro=new JPanel(new GridLayout(0,1,0,8));
                JLabel brand=UiTheme.label("KTX Manager",28,true); brand.setForeground(UiTheme.BLUE); intro.add(brand);
                intro.add(UiTheme.label(setup?"Tạo tài khoản quản trị viên đầu tiên":"Chào mừng bạn trở lại",18,true));
                JLabel hint=UiTheme.label(setup?"Mật khẩu tối thiểu 8 ký tự.":"Đăng nhập để quản lý và theo dõi thông tin ký túc xá.",13,false); hint.setForeground(UiTheme.MUTED); intro.add(hint);
                form.add(intro,BorderLayout.NORTH);
                JPanel fields=new JPanel(new GridLayout(0,1,0,9));
                fields.add(new JLabel("Tên đăng nhập")); fields.add(name);
                fields.add(new JLabel("Mật khẩu")); fields.add(password);
                JCheckBox reveal=new JCheckBox("Hiển thị mật khẩu"); reveal.setOpaque(false); char echo=password.getEchoChar(); reveal.addActionListener(e->password.setEchoChar(reveal.isSelected()?'\0':echo)); fields.add(reveal);
                form.add(fields,BorderLayout.CENTER);
                JLabel note=UiTheme.label("Sinh viên chưa có tài khoản: liên hệ ban quản lý KTX.",12,false); note.setForeground(UiTheme.MUTED); form.add(note,BorderLayout.SOUTH);
                UiTheme.decorate(form);
                int result=JOptionPane.showOptionDialog(parent,form,setup?"Khởi tạo quản trị viên":"Đăng nhập KTX",JOptionPane.DEFAULT_OPTION,JOptionPane.PLAIN_MESSAGE,null,new String[]{setup?"Tạo quản trị viên":"Đăng nhập","Hủy"},setup?"Tạo quản trị viên":"Đăng nhập");
                if(result!=0) return null;
                char[] secret=password.getPassword();
                try {
                    if(setup) dao.setup(name.getText(),secret);
                    return dao.login(name.getText(),secret);
                } finally { Arrays.fill(secret,'\0'); password.setText(""); }
            } catch(Exception e) {
                JOptionPane.showMessageDialog(parent,e.getMessage()+"\nNếu chưa nâng cấp CSDL, chạy migrations/002_extensions.sql trước.","Không thể đăng nhập",JOptionPane.ERROR_MESSAGE);
                if(JOptionPane.showConfirmDialog(parent,"Thử lại kết nối / đăng nhập?","KTX",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return null;
            }
        }
    }
}
