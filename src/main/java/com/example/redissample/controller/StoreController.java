package com.example.redissample.controller;

import com.example.redissample.common.ApiResponse;
import com.example.redissample.dto.ChangeStatusRequest;
import com.example.redissample.dto.OrderResponse;
import com.example.redissample.service.OrderService;
import com.example.redissample.service.StoreStatusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stores/{storeId}")
@RequiredArgsConstructor
public class StoreController {

  private final OrderService orderService;
  private final StoreStatusService storeStatusService;

  // 샘플 비즈니스 API: 매장 상태를 확인하고 주문 접수
  @PostMapping("/orders")
  public ApiResponse<OrderResponse> placeOrder(@PathVariable Long storeId) {
    return ApiResponse.ok(orderService.placeOrder(storeId));
  }

  // 상태 변경 API: DB 커밋 후 Redis 값도 새 상태로 갱신 → 다음 요청부터 새 상태
  @PatchMapping("/status")
  public ApiResponse<Void> changeStatus(@PathVariable Long storeId, @Valid @RequestBody ChangeStatusRequest request) {
    storeStatusService.changeStatus(storeId, request.status());
    return ApiResponse.ok(null);
  }
}
