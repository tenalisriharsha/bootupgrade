package dev.bootupgrade.samples.restservice;

public class Widget {

    private final long id;
    private final String name;
    private final int quantity;

    public Widget(long id, String name, int quantity) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }
}
