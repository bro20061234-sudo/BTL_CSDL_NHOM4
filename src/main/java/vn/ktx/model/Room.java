package vn.ktx.model;

import java.math.BigDecimal;

public class Room {
    private final String id;
    private final String number;
    private final String type;
    private final BigDecimal price;
    private final int capacity;

    public Room(String id, String number, String type, BigDecimal price, int capacity) {
        this.id = id;
        this.number = number;
        this.type = type;
        this.price = price;
        this.capacity = capacity;
    }

    public String getId() { return id; }
    public String getNumber() { return number; }
    public String getType() { return type; }
    public BigDecimal getPrice() { return price; }
    public int getCapacity() { return capacity; }
}
