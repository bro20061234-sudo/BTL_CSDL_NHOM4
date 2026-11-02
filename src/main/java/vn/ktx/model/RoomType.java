package vn.ktx.model;

import java.math.BigDecimal;
import java.util.List;

public enum RoomType {
    STANDARD("Phòng tiêu chuẩn",4,"1200000",List.of(new Asset("Quạt",1),new Asset("Máy lạnh",1),new Asset("Giường",4))),
    PREMIUM("Phòng cao cấp",2,"2000000",List.of(new Asset("Quạt",1),new Asset("Máy lạnh",1),new Asset("Giường",2),new Asset("Tủ quần áo",2),new Asset("Bàn học",2))),
    VIP("Phòng VIP",2,"3000000",List.of(new Asset("Quạt",1),new Asset("Máy lạnh",1),new Asset("Giường",2),new Asset("Tủ quần áo",2),new Asset("Bàn học",2),new Asset("Tủ lạnh",1),new Asset("Máy nước nóng",1)));
    public record Asset(String name,int quantity) { }
    private final String label; private final int capacity; private final BigDecimal price; private final List<Asset> assets;
    RoomType(String label,int capacity,String price,List<Asset> assets) { this.label=label;this.capacity=capacity;this.price=new BigDecimal(price);this.assets=assets; }
    public int capacity() { return capacity; }
    public BigDecimal price() { return price; }
    public List<Asset> assets() { return assets; }
    public String description() { return assets.stream().map(a->a.name()+" × "+a.quantity()).collect(java.util.stream.Collectors.joining(", ")); }
    @Override public String toString() { return label; }
    public static RoomType find(String label) { for(RoomType type:values()) if(type.label.equals(label)) return type; return null; }
}
