package az.iktex.coreplatform.simasignapi.controller;

import az.iktex.coreplatform.simasignapi.dto.OrderRequest;
import az.iktex.coreplatform.simasignapi.dto.OrderResponse;
import az.iktex.coreplatform.simasignapi.service.OrderCalculationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sima-sign/api/orders")
public class OrderController {
    private final OrderCalculationService orderCalculationService;

    public OrderController(OrderCalculationService orderCalculationService) {
        this.orderCalculationService = orderCalculationService;
    }

    @PostMapping("/calculate")
    public OrderResponse calculate(@RequestBody OrderRequest request) {
        return orderCalculationService.calculate(request);
    }
}
