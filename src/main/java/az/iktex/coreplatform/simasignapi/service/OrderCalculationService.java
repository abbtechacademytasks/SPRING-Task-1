package az.iktex.coreplatform.simasignapi.service;

import az.iktex.coreplatform.simasignapi.dto.OrderItemRequest;
import az.iktex.coreplatform.simasignapi.dto.OrderItemResponse;
import az.iktex.coreplatform.simasignapi.dto.OrderRequest;
import az.iktex.coreplatform.simasignapi.dto.OrderResponse;
import az.iktex.coreplatform.simasignapi.enums.CustomerType;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderCalculationService {
    private static final int QUANTITY_DISCOUNT_THRESHOLD = 10;
    private static final BigDecimal QUANTITY_DISCOUNT_RATE = new BigDecimal("0.05");
    private static final BigDecimal VIP_DISCOUNT_RATE = new BigDecimal("0.15");
    private static final BigDecimal MAX_DISCOUNT_RATE = new BigDecimal("0.30");

    private static final String PROMO_YENI10 = "YENI10";
    private static final String PROMO_ENDIRIM20 = "ENDIRIM20";
    private static final BigDecimal YENI10_RATE = new BigDecimal("0.10");
    private static final BigDecimal ENDIRIM20_AMOUNT = new BigDecimal("20.00");
    private static final BigDecimal ENDIRIM20_MIN_SUBTOTAL = new BigDecimal("100.00");

    private static final BigDecimal VAT_RATE = new BigDecimal("0.18");
    private static final BigDecimal FREE_DELIVERY_THRESHOLD = new BigDecimal("200.00");
    private static final BigDecimal BAKU_DELIVERY_FEE = new BigDecimal("5.00");
    private static final BigDecimal OTHER_CITY_DELIVERY_FEE = new BigDecimal("10.00");

    public OrderResponse calculate(OrderRequest request) {
        if (!isValid(request)) {
            return null;
        }

        OrderResponse response = new OrderResponse();
        List<OrderItemResponse> calculatedItems = new ArrayList<>();
        List<String> appliedRules = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (OrderItemRequest item : request.getItems()) {
            OrderItemResponse lineItemResponse = calculateLine(item);
            calculatedItems.add(lineItemResponse);
            subtotal = subtotal.add(lineItemResponse.getFinalLineTotal());
            if (item.getQuantity() >= QUANTITY_DISCOUNT_THRESHOLD) {
                appliedRules.add("Say endirimi 5%: " + item.getProductName());
            }
        }

        BigDecimal customerDiscount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        if (request.getCustomerType() == CustomerType.VIP) {
            customerDiscount = subtotal.multiply(VIP_DISCOUNT_RATE)
                    .setScale(2, RoundingMode.HALF_UP);
            appliedRules.add("VIP endirimi 15%");
        }

        BigDecimal promoDiscount = calculatePromoDiscount(request.getPromoCode(), subtotal);
        if (PROMO_YENI10.equals(request.getPromoCode())) {
            appliedRules.add("YENI10 promo kodu 10%");
        } else if (PROMO_ENDIRIM20.equals(request.getPromoCode())) {
            if (subtotal.compareTo(ENDIRIM20_MIN_SUBTOTAL) >= 0) {
                appliedRules.add("ENDIRIM20 promo kodu 20.00 AZN");
            } else {
                appliedRules.add("ENDIRIM20 tətbiq olunmadı: minimum məbləğ 100.00 AZN");
            }
        }
        BigDecimal maxDiscount = subtotal.multiply(MAX_DISCOUNT_RATE)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalDiscount = promoDiscount.add(customerDiscount);
        if (totalDiscount.compareTo(maxDiscount) > 0) {
            totalDiscount = maxDiscount;
            appliedRules.add("Endirim limiti tətbiq olundu: maksimum 30%");
        }
        BigDecimal amountAfterDiscount = subtotal.subtract(totalDiscount)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal vat = amountAfterDiscount.multiply(VAT_RATE)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal deliveryFee = calculateDeliveryFee(amountAfterDiscount, request.getDeliveryCity());
        if (amountAfterDiscount.compareTo(FREE_DELIVERY_THRESHOLD) >= 0) {
            appliedRules.add("Pulsuz çatdırılma");
        }
        BigDecimal grandTotal = amountAfterDiscount.add(vat).add(deliveryFee)
                .setScale(2, RoundingMode.HALF_UP);

        response.setPromoDiscount(promoDiscount);
        response.setItems(calculatedItems);
        response.setSubtotal(subtotal);
        response.setCustomerDiscount(customerDiscount);
        response.setTotalDiscount(totalDiscount);
        response.setAmountAfterDiscount(amountAfterDiscount);
        response.setVat(vat);
        response.setDeliveryFee(deliveryFee);
        response.setGrandTotal(grandTotal);
        response.setAppliedRules(appliedRules);

        return response;
    }

    private boolean isValid(OrderRequest request) {
        if (request == null) {
            System.out.println("XƏTA: Sifariş boş ola bilməz");
            return false;
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            System.out.println("XƏTA: Sifarişdə ən azı bir məhsul olmalıdır");
            return false;
        }

        if (request.getCustomerType() == null) {
            System.out.println("XƏTA: Müştəri tipi mütləqdir");
            return false;
        }

        if (request.getDeliveryCity() == null || request.getDeliveryCity().isBlank()) {
            System.out.println("XƏTA: Çatdırılma şəhəri mütləqdir");
            return false;
        }

        for (OrderItemRequest item : request.getItems()) {
            if (item == null) {
                System.out.println("XƏTA: Məhsul boş ola bilməz");
                return false;
            }

            if (item.getProductName() == null || item.getProductName().isBlank()) {
                System.out.println("XƏTA: Məhsulun adı boş ola bilməz");
                return false;
            }

            if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                System.out.println("XƏTA: Qiymət 0-dan böyük olmalıdır");
                return false;
            }

            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                System.out.println("XƏTA: Say 1-dən kiçik ola bilməz");
                return false;
            }
        }

        return true;
    }

    private OrderItemResponse calculateLine(OrderItemRequest item) {
        OrderItemResponse response = new OrderItemResponse();
        response.setProductName(item.getProductName());
        response.setUnitPrice(
                item.getUnitPrice().setScale(2, RoundingMode.HALF_UP)
        );
        response.setQuantity(item.getQuantity());
        BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal quantityDiscount = BigDecimal.ZERO
                .setScale(2, RoundingMode.HALF_UP);

        if (item.getQuantity() >= QUANTITY_DISCOUNT_THRESHOLD) {
            quantityDiscount = lineTotal.multiply(QUANTITY_DISCOUNT_RATE)
                    .setScale(2, RoundingMode.HALF_UP);
        }
        response.setLineTotal(lineTotal);
        response.setQuantityDiscount(quantityDiscount);

        BigDecimal finalLineTotal = lineTotal.subtract(quantityDiscount)
                .setScale(2, RoundingMode.HALF_UP);
        response.setFinalLineTotal(finalLineTotal);

        return response;
    }

    private BigDecimal calculatePromoDiscount(String code, BigDecimal subtotal) {
        if (code == null || code.isBlank()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        switch (code) {
            case PROMO_YENI10:
                return subtotal.multiply(YENI10_RATE)
                        .setScale(2, RoundingMode.HALF_UP);
            case PROMO_ENDIRIM20:
                if (subtotal.compareTo(ENDIRIM20_MIN_SUBTOTAL) >= 0) {
                    return ENDIRIM20_AMOUNT.setScale(2, RoundingMode.HALF_UP);
                } else {
                    return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                }
            default:
                System.out.println("XƏBƏRDARLIQ: Promo kod tapılmadı: " + code);
                return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
    }

    private BigDecimal calculateDeliveryFee(BigDecimal amount, String city) {
        if (amount.compareTo(FREE_DELIVERY_THRESHOLD) >= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        if ("Bakı".equals(city) || "BAKI".equals(city) || "Baku".equals(city)) {
            return BAKU_DELIVERY_FEE.setScale(2, RoundingMode.HALF_UP);
        } else {
            return OTHER_CITY_DELIVERY_FEE.setScale(2, RoundingMode.HALF_UP);
        }
    }
}
