package vn.ktx.model;

import java.math.BigDecimal;

public class Service {
    private final String id;
    private final String name;
    private final BigDecimal price;

    public Service(String id, String name, BigDecimal price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
}
