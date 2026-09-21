package com.example.redissample.mapper;

import com.example.redissample.domain.StoreStatus;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StoreMapper {

  @Select("SELECT status FROM store WHERE id = #{storeId}")
  Optional<StoreStatus> findStatusById(Long storeId);

  @Update("UPDATE store SET status = #{status} WHERE id = #{storeId}")
  int updateStatus(@Param("storeId") Long storeId, @Param("status") StoreStatus status);
}
