package az.iktex.coreplatform.simasignapi.service;

import az.iktex.coreplatform.simasignapi.dto.OrderItemRequest;
import az.iktex.coreplatform.simasignapi.dto.OrderRequest;
import az.iktex.coreplatform.simasignapi.dto.OrderResponse;
import az.iktex.coreplatform.simasignapi.enums.CustomerType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrderCalculationServiceTest {
    @Test
    void shouldCalculateRegularOrderWithoutPromo() {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductName("Qələm");
        item.setUnitPrice(new BigDecimal("20.00"));
        item.setQuantity(2);

        OrderRequest request = new OrderRequest();
        request.setCustomerType(CustomerType.REGULAR);
        request.setDeliveryCity("Bakı");
        request.setItems(List.of(item));

        OrderCalculationService service = new OrderCalculationService();
        OrderResponse response = service.calculate(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("52.20"), response.getGrandTotal());
        assertEquals(new BigDecimal("40.00"), response.getSubtotal());
        assertEquals(new BigDecimal("0.00"), response.getTotalDiscount());
        assertEquals(new BigDecimal("7.20"), response.getVat());
        assertEquals(new BigDecimal("5.00"), response.getDeliveryFee());
    }

    @Test
    void shouldApplyQuantityDiscount() {
        OrderItemRequest item1 = new OrderItemRequest();
        item1.setProductName("Qələm");
        item1.setUnitPrice(new BigDecimal("3.00"));
        item1.setQuantity(10);

        OrderItemRequest item2 = new OrderItemRequest();
        item2.setProductName("Defter");
        item2.setUnitPrice(new BigDecimal("50.00"));
        item2.setQuantity(1);

        OrderRequest request = new OrderRequest();
        request.setCustomerType(CustomerType.REGULAR);
        request.setDeliveryCity("Gəncə");
        request.setItems(List.of(item1, item2));

        OrderCalculationService service = new OrderCalculationService();
        OrderResponse response = service.calculate(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("102.63"), response.getGrandTotal());
        assertEquals(new BigDecimal("78.50"), response.getSubtotal());
        assertEquals(new BigDecimal("0.00"), response.getTotalDiscount());
        assertEquals(new BigDecimal("14.13"), response.getVat());
        assertEquals(new BigDecimal("10.00"), response.getDeliveryFee());
        assertEquals(new BigDecimal("1.50"),
                response.getItems().getFirst().getQuantityDiscount());
        assertEquals(new BigDecimal("28.50"),
                response.getItems().getFirst().getFinalLineTotal());
    }

    @Test
    void shouldApplyVipAndYeni10Discounts() {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductName("Qulaqlıq");
        item.setUnitPrice(new BigDecimal("150.00"));
        item.setQuantity(2);

        OrderRequest request = new OrderRequest();
        request.setCustomerType(CustomerType.VIP);
        request.setPromoCode("YENI10");
        request.setDeliveryCity("Bakı");
        request.setItems(List.of(item));

        OrderCalculationService service = new OrderCalculationService();
        OrderResponse response = service.calculate(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("300.00"), response.getSubtotal());
        assertEquals(new BigDecimal("45.00"), response.getCustomerDiscount());
        assertEquals(new BigDecimal("30.00"), response.getPromoDiscount());
        assertEquals(new BigDecimal("75.00"), response.getTotalDiscount());
        assertEquals(new BigDecimal("225.00"), response.getAmountAfterDiscount());
        assertEquals(new BigDecimal("40.50"), response.getVat());
        assertEquals(new BigDecimal("0.00"), response.getDeliveryFee());
        assertEquals(new BigDecimal("265.50"), response.getGrandTotal());
        assertEquals(List.of("VIP endirimi 15%", "YENI10 promo kodu 10%",
                "Pulsuz çatdırılma"), response.getAppliedRules());
    }

    @Test
    void shouldLimitTotalDiscount() {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductName("Kitab");
        item.setUnitPrice(new BigDecimal("25.00"));
        item.setQuantity(4);

        OrderRequest request = new OrderRequest();
        request.setCustomerType(CustomerType.VIP);
        request.setPromoCode("ENDIRIM20");
        request.setDeliveryCity("Bakı");
        request.setItems(List.of(item));

        OrderCalculationService service = new OrderCalculationService();
        OrderResponse response = service.calculate(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("100.00"), response.getSubtotal());
        assertEquals(new BigDecimal("15.00"), response.getCustomerDiscount());
        assertEquals(new BigDecimal("20.00"), response.getPromoDiscount());
        assertEquals(new BigDecimal("30.00"), response.getTotalDiscount());
        assertEquals(new BigDecimal("70.00"), response.getAmountAfterDiscount());
        assertEquals(new BigDecimal("12.60"), response.getVat());
        assertEquals(new BigDecimal("5.00"), response.getDeliveryFee());
        assertEquals(new BigDecimal("87.60"), response.getGrandTotal());
        assertEquals(List.of("VIP endirimi 15%", "ENDIRIM20 promo kodu 20.00 AZN",
                "Endirim limiti tətbiq olundu: maksimum 30%"), response.getAppliedRules());
    }
    @Test
    void shouldSkipEndirim20BelowMinimumSubtotal() {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductName("Kitab");
        item.setUnitPrice(new BigDecimal("30.00"));
        item.setQuantity(3);

        OrderRequest request = new OrderRequest();
        request.setCustomerType(CustomerType.REGULAR);
        request.setPromoCode("ENDIRIM20");
        request.setDeliveryCity("Bakı");
        request.setItems(List.of(item));

        OrderCalculationService service = new OrderCalculationService();
        OrderResponse response = service.calculate(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("90.00"), response.getSubtotal());
        assertEquals(new BigDecimal("0.00"), response.getCustomerDiscount());
        assertEquals(new BigDecimal("0.00"), response.getPromoDiscount());
        assertEquals(new BigDecimal("0.00"), response.getTotalDiscount());
        assertEquals(new BigDecimal("90.00"), response.getAmountAfterDiscount());
        assertEquals(new BigDecimal("16.20"), response.getVat());
        assertEquals(new BigDecimal("5.00"), response.getDeliveryFee());
        assertEquals(new BigDecimal("111.20"), response.getGrandTotal());
        assertEquals(List.of("ENDIRIM20 tətbiq olunmadı: minimum məbləğ 100.00 AZN"),
                response.getAppliedRules());
    }

    @Test
    void shouldChargeDeliveryWhenDiscountedAmountIsBelowThreshold() {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductName("Qulaqlıq");
        item.setUnitPrice(new BigDecimal("220.00"));
        item.setQuantity(1);

        OrderRequest request = new OrderRequest();
        request.setCustomerType(CustomerType.REGULAR);
        request.setPromoCode("YENI10");
        request.setDeliveryCity("Sumqayıt");
        request.setItems(List.of(item));

        OrderCalculationService service = new OrderCalculationService();
        OrderResponse response = service.calculate(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("220.00"), response.getSubtotal());
        assertEquals(new BigDecimal("0.00"), response.getCustomerDiscount());
        assertEquals(new BigDecimal("22.00"), response.getPromoDiscount());
        assertEquals(new BigDecimal("22.00"), response.getTotalDiscount());
        assertEquals(new BigDecimal("198.00"), response.getAmountAfterDiscount());
        assertEquals(new BigDecimal("35.64"), response.getVat());
        assertEquals(new BigDecimal("10.00"), response.getDeliveryFee());
        assertEquals(new BigDecimal("243.64"), response.getGrandTotal());
        assertEquals(List.of("YENI10 promo kodu 10%"), response.getAppliedRules());
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void shouldIgnoreUnknownPromoCode(CapturedOutput output) {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductName("Qələm");
        item.setUnitPrice(new BigDecimal("10.00"));
        item.setQuantity(1);

        OrderRequest request = new OrderRequest();
        request.setCustomerType(CustomerType.REGULAR);
        request.setPromoCode("SALAM");
        request.setDeliveryCity("Bakı");
        request.setItems(List.of(item));

        OrderCalculationService service = new OrderCalculationService();
        OrderResponse response = service.calculate(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("10.00"), response.getSubtotal());
        assertEquals(new BigDecimal("0.00"), response.getCustomerDiscount());
        assertEquals(new BigDecimal("0.00"), response.getPromoDiscount());
        assertEquals(new BigDecimal("0.00"), response.getTotalDiscount());
        assertEquals(new BigDecimal("10.00"), response.getAmountAfterDiscount());
        assertEquals(new BigDecimal("1.80"), response.getVat());
        assertEquals(new BigDecimal("5.00"), response.getDeliveryFee());
        assertEquals(new BigDecimal("16.80"), response.getGrandTotal());
        assertEquals(List.of(), response.getAppliedRules());
        assertTrue(output.getOut().contains("XƏBƏRDARLIQ: Promo kod tapılmadı: SALAM"));
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void shouldRejectZeroQuantity(CapturedOutput output) {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductName("Qələm");
        item.setUnitPrice(new BigDecimal("10.00"));
        item.setQuantity(0);

        OrderRequest request = new OrderRequest();
        request.setCustomerType(CustomerType.REGULAR);
        request.setDeliveryCity("Bakı");
        request.setItems(List.of(item));

        OrderCalculationService service = new OrderCalculationService();
        OrderResponse response = service.calculate(request);

        assertNull(response);
        assertTrue(output.getOut().contains("XƏTA: Say 1-dən kiçik ola bilməz"));
    }
}
