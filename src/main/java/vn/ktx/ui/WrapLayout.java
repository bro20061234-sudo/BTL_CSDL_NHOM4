package vn.ktx.ui;

import java.awt.*;

/** Thanh công cụ tự xuống dòng và dành đủ chiều cao khi cửa sổ hẹp. */
public final class WrapLayout extends FlowLayout {
    public WrapLayout() { super(FlowLayout.LEFT,8,6); }
    @Override public Dimension preferredLayoutSize(Container target) {
        synchronized(target.getTreeLock()) {
            int width=target.getWidth();
            if(target.getParent()!=null && target.getParent().getWidth()>0) {
                Insets parentInsets=target.getParent().getInsets();
                width=target.getParent().getWidth()-parentInsets.left-parentInsets.right;
            }
            if(width<=0) return super.preferredLayoutSize(target);
            Insets insets=target.getInsets(); int available=Math.max(1,width-insets.left-insets.right-getHgap()*2);
            int rowWidth=0,rowHeight=0,height=0,maxWidth=0;
            for(Component c:target.getComponents()) if(c.isVisible()) {
                Dimension d=c.getPreferredSize();
                if(rowWidth>0 && rowWidth+getHgap()+d.width>available) { maxWidth=Math.max(maxWidth,rowWidth);height+=rowHeight+getVgap();rowWidth=0;rowHeight=0; }
                if(rowWidth>0) rowWidth+=getHgap(); rowWidth+=d.width;rowHeight=Math.max(rowHeight,d.height);
            }
            return new Dimension(Math.max(maxWidth,rowWidth)+insets.left+insets.right+2*getHgap(),height+rowHeight+insets.top+insets.bottom+2*getVgap());
        }
    }
}
