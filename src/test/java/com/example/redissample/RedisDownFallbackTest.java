package com.example.redissample;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.redissample.domain.StoreStatus;
import com.example.redissample.service.StoreStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

// 아무것도 떠 있지 않은 포트로 연결 → Redis 장애 상황
@SpringBootTest(properties = "spring.data.redis.port=1")
@ActiveProfiles("test")
class RedisDownFallbackTest {

  @Autowired
  private StoreStatusService storeStatusService;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    jdbcTemplate.update("delete from store");
    jdbcTemplate.update("insert into store (id, name, status) values (1, '강남점', 'OPEN')");
  }

  @Test
  @DisplayName("Redis 가 죽어도 상태 조회는 DB 로 동작한다")
  void readsStatusFromDbWhenRedisIsDown() {
    assertThat(storeStatusService.getStatus(1L)).contains(StoreStatus.OPEN);
  }

  @Test
  @DisplayName("Redis 가 죽어도 상태 변경은 성공한다")
  void changesStatusWhenRedisIsDown() {
    storeStatusService.changeStatus(1L, StoreStatus.CLOSED);

    assertThat(jdbcTemplate.queryForObject("select status from store where id = 1", String.class))
        .isEqualTo("CLOSED");
  }
}
