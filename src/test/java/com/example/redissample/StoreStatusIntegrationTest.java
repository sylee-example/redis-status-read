package com.example.redissample;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.redissample.domain.StoreStatus;
import com.example.redissample.service.StoreStatusService;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import redis.embedded.RedisServer;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StoreStatusIntegrationTest {

  // ponytail: Redis 를 쓰는 테스트 클래스가 하나뿐이라 여기서 직접 띄움. 늘어나면 공통 베이스 클래스로 옮길 것
  private static final int REDIS_PORT = 16379;

  static {
    try {
      // JVM 종료 시 라이브러리가 프로세스를 정리함
      RedisServer.newRedisServer().port(REDIS_PORT).bind("127.0.0.1").build().start();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @DynamicPropertySource
  static void redisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.port", () -> REDIS_PORT);
  }

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private StoreStatusService storeStatusService;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private StringRedisTemplate redisTemplate;

  @Autowired
  private TransactionTemplate transactionTemplate;

  @BeforeEach
  void setUp() {
    jdbcTemplate.update("delete from store");
    jdbcTemplate.update("insert into store (id, name, status) values (1, '강남점', 'OPEN')");
    redisTemplate.delete(redisTemplate.keys("storeStatus::*"));
  }

  @Test
  @DisplayName("한 번 조회한 상태는 Redis 에서 읽는다 (DB 를 직접 바꿔도 TTL 동안은 캐시 값)")
  void readsStatusFromCacheAfterFirstLookup() {
    assertThat(storeStatusService.getStatus(1L)).contains(StoreStatus.OPEN);

    jdbcTemplate.update("update store set status = 'CLOSED' where id = 1"); // 앱을 거치지 않은 외부 변경

    assertThat(storeStatusService.getStatus(1L)).contains(StoreStatus.OPEN);
  }

  @Test
  @DisplayName("캐시 항목에는 TTL 이 걸린다 (외부 변경은 최대 TTL 뒤에 반영)")
  void cachedStatusExpires() {
    storeStatusService.getStatus(1L);

    assertThat(redisTemplate.getExpire("storeStatus::1")).isPositive();
  }

  @Test
  @DisplayName("상태를 바꾸면 커밋 후 Redis 값도 새 상태로 바뀐다 (커밋 전에 바꾸면 롤백될 값이 다른 요청에 보임)")
  void updatesCachedStatusAfterCommit() {
    storeStatusService.getStatus(1L); // OPEN 캐싱

    transactionTemplate.executeWithoutResult(tx -> {
      storeStatusService.changeStatus(1L, StoreStatus.CLOSED);
      assertThat(storeStatusService.getStatus(1L)).contains(StoreStatus.OPEN); // 커밋 전: Redis 는 아직 옛 값
    });

    // 커밋 후: DB 를 일부러 다른 값으로 바꿔둬도 Redis 에 들어간 새 값(CLOSED)이 나와야 함
    jdbcTemplate.update("update store set status = 'OPEN' where id = 1");
    assertThat(storeStatusService.getStatus(1L)).contains(StoreStatus.CLOSED);
  }

  @Test
  @DisplayName("상태 변경 API 직후 주문 API 는 바뀐 상태를 본다")
  void orderApiSeesStatusChangedByApi() throws Exception {
    mockMvc.perform(post("/api/stores/1/orders"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true)); // 이때 OPEN 이 캐싱됨

    mockMvc.perform(patch("/api/stores/1/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"CLOSED\"}"))
        .andExpect(status().isOk());

    mockMvc.perform(post("/api/stores/1/orders"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.errorCode").value("STORE_NOT_OPEN"));
  }

  @Test
  @DisplayName("없는 매장은 404")
  void unknownStoreReturnsNotFound() throws Exception {
    mockMvc.perform(post("/api/stores/999/orders"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("STORE_NOT_FOUND"));

    mockMvc.perform(patch("/api/stores/999/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"CLOSED\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("STORE_NOT_FOUND"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"{\"status\":\"UNKNOWN\"}", "{}"})
  @DisplayName("잘못된 상태 변경 요청은 400 (공통 응답 형식)")
  void invalidStatusRequestReturnsBadRequest(String body) throws Exception {
    mockMvc.perform(patch("/api/stores/1/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
  }
}
