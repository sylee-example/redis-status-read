package com.example.redissample.service;

import com.example.redissample.common.BusinessException;
import com.example.redissample.common.ErrorCode;
import com.example.redissample.domain.StoreStatus;
import com.example.redissample.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

  private final StoreStatusService storeStatusService;

  public OrderResponse placeOrder(Long storeId) {
    // 각 API 는 이렇게 상태를 확인 → DB 가 아니라 Redis 캐시에서 읽음
    StoreStatus status = storeStatusService.getStatus(storeId)
        .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));
    if (status != StoreStatus.OPEN) {
      throw new BusinessException(ErrorCode.STORE_NOT_OPEN);
    }

    // ponytail: 샘플이라 실제 주문 처리는 생략. 여기부터 원래 비즈니스 로직 자리
    return new OrderResponse(storeId, "주문이 접수되었습니다.");
  }
}
