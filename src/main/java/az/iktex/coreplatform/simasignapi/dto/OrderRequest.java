package az.iktex.coreplatform.simasignapi.dto;

import az.iktex.coreplatform.simasignapi.enums.CustomerType;

import java.util.List;

public class OrderRequest {
    private CustomerType customerType;
    private String promoCode;
    private String deliveryCity;
    private List<OrderItemRequest> items;

    public OrderRequest() {
    }

    public CustomerType getCustomerType() {
        return customerType;
    }

    public void setCustomerType(CustomerType customerType) {
        this.customerType = customerType;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(String promoCode) {
        this.promoCode = promoCode;
    }

    public String getDeliveryCity() {
        return deliveryCity;
    }

    public void setDeliveryCity(String deliveryCity) {
        this.deliveryCity = deliveryCity;
    }

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }
}
