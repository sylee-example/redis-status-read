package com.example.redissample.common;

// 모든 API 공통 응답 형식
public record ApiResponse<T>(boolean success, T data, String errorCode, String message) {

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(true, data, null, null);
  }

  public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
    return new ApiResponse<>(false, null, errorCode.name(), errorCode.getMessage());
  }
}
