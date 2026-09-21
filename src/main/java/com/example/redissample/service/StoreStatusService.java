package com.example.redissample.service;

import com.example.redissample.common.BusinessException;
import com.example.redissample.common.ErrorCode;
import com.example.redissample.domain.StoreStatus;
import com.example.redissample.mapper.StoreMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 매장 상태 조회/변경 + Redis 캐시 (조회: Cache-Aside, 변경: Write-Through).
 * - 조회: Redis 에 있으면 바로 반환, 없으면 DB 조회 후 TTL 과 함께 저장
 * - 변경: DB 커밋 후 Redis 값도 새 상태로 갱신 → 다음 조회부터 바로 새 값
 * - 앱 밖에서 DB 를 바꾼 경우: TTL(application.yml) 이 지나야 반영
 *
 * 캐시 애너테이션은 프록시로 동작하므로 이 클래스 안에서 getStatus() 를 호출하면 캐시를 안 탐.
 * 반드시 다른 빈(예: OrderService)에서 호출할 것.
 */
@Service
@RequiredArgsConstructor
public class StoreStatusService {

  // Redis 키: storeStatus::{storeId}
  private static final String CACHE_NAME = "storeStatus";

  private final StoreMapper storeMapper;

  // 없는 매장(empty)도 캐싱됨 → 잘못된 id 반복 요청이 DB 로 안 감. 매장 생성 API 를 만들면 생성 후 evict 할 것
  // ponytail: 키 만료 순간 동시에 들어온 요청은 각자 DB 를 조회함 (PK 조회라 부담 작음).
  //   sync = true 는 spring-data-redis 3.5 기본 설정에선 이걸 막지 못하고 트랜잭션 연동도 우회해서 안 씀.
  //   조회가 무거워지면 로컬 per-key 락이나 Caffeine L1 캐시를 앞에 둘 것
  @Cacheable(cacheNames = CACHE_NAME, key = "#storeId")
  public Optional<StoreStatus> getStatus(Long storeId) {
    return storeMapper.findStatusById(storeId);
  }

  // DB 를 바꾸고, 커밋 후 Redis 값도 새 상태로 덮어씀 → 다음 조회부터 DB 안 가고 바로 새 값
  // (커밋 후 실행은 CacheConfig 의 transactionAware. 롤백되거나 예외가 나면 Redis 는 안 건드림)
  // ponytail: 요청이 드물게 겹치면 옛 값이 최대 TTL 동안 남을 수 있음
  //   ① 같은 매장 동시 변경 시 Redis 쓰기 순서가 DB 커밋 순서와 뒤바뀜
  //   ② 캐시 miss 조회가 옛 값을 읽고, 이 변경보다 늦게 Redis 에 씀
  //   ①이 문제면 @CachePut 대신 @CacheEvict(삭제 → 다음 조회가 DB 에서 다시 읽음)로 바꿀 것
  @Transactional
  @CachePut(cacheNames = CACHE_NAME, key = "#storeId")
  public StoreStatus changeStatus(Long storeId, StoreStatus status) {
    if (storeMapper.updateStatus(storeId, status) == 0) {
      throw new BusinessException(ErrorCode.STORE_NOT_FOUND);
    }
    return status; // @CachePut: 이 반환값이 Redis 에 저장됨
  }
}
