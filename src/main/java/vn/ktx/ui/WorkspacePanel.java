package vn.ktx.ui;

import vn.ktx.model.UserSession;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.*;
import java.util.function.Supplier;

/** Khung điều hướng dùng chung cho quản lý và sinh viên; tạo trang khi mở lần đầu. */
public final class WorkspacePanel extends JPanel {
    private final JPanel menu=new JPanel(), cards=new JPanel(new CardLayout());
    private final JLabel heading=UiTheme.label("",28,true),subtitle=UiTheme.label("",14,false),breadcrumb=UiTheme.label("",12,false);
    private final JLabel status=UiTheme.label("Chọn một chức năng để bắt đầu.",12,false);
    private final Map<String,JButton> buttons=new LinkedHashMap<>();
    private final Map<String,Supplier<JPanel>> factories=new LinkedHashMap<>();
    private final Map<String,String[]> descriptions=new HashMap<>();
    private final Set<String> built=new HashSet<>();
    private String current;
    public WorkspacePanel(UserSession user,Runnable logout) {
        super(new BorderLayout()); setBackground(UiTheme.CANVAS);
        JPanel sidebar=new JPanel(new BorderLayout()); sidebar.setPreferredSize(new Dimension(264,0)); sidebar.setBackground(UiTheme.WHITE);
        sidebar.setBorder(new MatteBorder(0,0,0,1,UiTheme.LINE));
        JPanel brand=new JPanel(new GridLayout(0,1,0,6)); brand.setBorder(new EmptyBorder(18,22,14,16));
        JLabel title=UiTheme.label("KTX Manager",22,true); title.setForeground(UiTheme.BLUE); brand.add(title);
        JLabel caption=UiTheme.label("Quản lý ký túc xá",12,false); caption.setForeground(UiTheme.MUTED); brand.add(caption); sidebar.add(brand,BorderLayout.NORTH);
        menu.setLayout(new BoxLayout(menu,BoxLayout.Y_AXIS)); menu.setBorder(new EmptyBorder(0,12,12,12));
        JScrollPane scroll=new JScrollPane(menu); scroll.setBorder(null); scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); scroll.getVerticalScrollBar().setUnitIncrement(20);
        sidebar.add(scroll,BorderLayout.CENTER);
        JPanel profile=new JPanel(); profile.setLayout(new BoxLayout(profile,BoxLayout.Y_AXIS)); profile.setBorder(new CompoundBorder(new MatteBorder(1,0,0,0,UiTheme.LINE),new EmptyBorder(12,20,12,20)));
        profile.add(UiTheme.label(user.username(),14,true)); profile.add(Box.createVerticalStrut(5)); JLabel role=UiTheme.label(UiTheme.status(user.role()),12,false); role.setForeground(UiTheme.MUTED); profile.add(role); profile.add(Box.createVerticalStrut(8));
        JButton signOut=new JButton("Đăng xuất"); signOut.putClientProperty("kind","nav"); signOut.setHorizontalAlignment(SwingConstants.LEFT);
        signOut.setIcon(UiIcons.page("logout")); signOut.setIconTextGap(10);
        signOut.addActionListener(e->{ if(JOptionPane.showConfirmDialog(this,"Đăng xuất khỏi tài khoản hiện tại?","Đăng xuất",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) logout.run(); }); profile.add(signOut);
        sidebar.add(profile,BorderLayout.SOUTH); add(sidebar,BorderLayout.WEST);
        JPanel main=new JPanel(new BorderLayout(0,20)); main.setBackground(UiTheme.CANVAS); main.setBorder(new EmptyBorder(24,26,18,26));
        JPanel top=new JPanel(); top.setOpaque(false); top.setLayout(new BoxLayout(top,BoxLayout.Y_AXIS));
        breadcrumb.setForeground(UiTheme.MUTED); subtitle.setForeground(UiTheme.MUTED);
        top.add(breadcrumb); top.add(Box.createVerticalStrut(14)); top.add(heading); top.add(Box.createVerticalStrut(7)); top.add(subtitle);
        main.add(top,BorderLayout.NORTH); cards.setOpaque(false); main.add(cards,BorderLayout.CENTER);
        JPanel bottom=new JPanel(new BorderLayout()); bottom.setOpaque(false); bottom.setBorder(new EmptyBorder(6,0,0,0));
        status.setForeground(UiTheme.MUTED); bottom.add(status,BorderLayout.WEST); bottom.add(UiTheme.label("KTX Manager  •  "+java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),12,false),BorderLayout.EAST);
        main.add(bottom,BorderLayout.SOUTH); add(main,BorderLayout.CENTER); UiTheme.decorate(this);
    }
    public void group(String title) {
        JLabel label=UiTheme.label(title.toUpperCase(Locale.forLanguageTag("vi-VN")),11,true); label.setForeground(UiTheme.MUTED);
        label.setBorder(new EmptyBorder(12,12,6,0)); label.setAlignmentX(LEFT_ALIGNMENT); menu.add(label);
    }
    public void page(String key,String label,String title,String description,Supplier<JPanel> factory) {
        if(buttons.containsKey(key)) throw new IllegalArgumentException("Trang trùng mã: "+key);
        JButton b=new JButton(label); b.putClientProperty("kind","nav"); b.setHorizontalAlignment(SwingConstants.LEFT); UiTheme.styleButton(b);
        b.setIcon(UiIcons.page(key)); b.setIconTextGap(10); b.setToolTipText(label);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE,39)); b.setAlignmentX(LEFT_ALIGNMENT); b.addActionListener(e->showPage(key));
        menu.add(b); menu.add(Box.createVerticalStrut(1)); buttons.put(key,b); factories.put(key,factory); descriptions.put(key,new String[]{title,description});
    }
    public void showPage(String key) {
        Supplier<JPanel> factory=factories.get(key); if(factory==null) throw new IllegalArgumentException("Trang không tồn tại: "+key);
        if(!built.contains(key)) {
            JPanel page=factory.get(); page.setBorder(new CompoundBorder(new LineBorder(UiTheme.LINE),new EmptyBorder(14,14,14,14)));
            UiTheme.decorate(page); cards.add(page,key); built.add(key);
        }
        current=key; buttons.forEach((id,b)->{b.putClientProperty("active",id.equals(key)); b.repaint();});
        heading.setText(descriptions.get(key)[0]); subtitle.setText(descriptions.get(key)[1]); breadcrumb.setText("KTX Manager  /  "+buttons.get(key).getText());
        ((CardLayout)cards.getLayout()).show(cards,key); status.setText("Sẵn sàng  •  "+buttons.get(key).getText());
        cards.revalidate(); cards.repaint();
    }
    public String currentPage() { return current; }
    public Set<String> pageKeys() { return Collections.unmodifiableSet(buttons.keySet()); }
    public void status(String text) { status.setText(text); }
    public static JPanel tabs(String[] titles,JPanel... panels) {
        JTabbedPane tabs=new JTabbedPane(); for(int i=0;i<titles.length;i++) tabs.addTab(titles[i],UiIcons.tab(titles[i]),panels[i]);
        JPanel wrap=new JPanel(new BorderLayout()); wrap.add(tabs); return wrap;
    }
}
