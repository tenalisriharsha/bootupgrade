package dev.bootupgrade.samples.restservice;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/** Incoming request DTO. {@code javax.validation} -> {@code jakarta.validation} after the Spring Boot 3 upgrade. */
public class WidgetRequest {

    @NotBlank
    private String name;

    @Min(0)
    private int quantity;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
