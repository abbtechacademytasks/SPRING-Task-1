package az.iktex.coreplatform.simasignapi.dto;

import java.math.BigDecimal;

public class OrderItemRequest {
    private String productName;
    private BigDecimal unitPrice;
    private Integer quantity;

    public OrderItemRequest() {
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
