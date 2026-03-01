package com.petsapp.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standardowy wrapper dla wszystkich odpowiedzi API. Kazdy endpoint zwraca ApiResponse zamiast
 * surowego obiektu.
 *
 * <p>Sukces: {"success": true, "data": {...}} Blad: {"success": false, "error": {...}} Lista:
 * {"success": true, "data": [...], "pagination": {...}}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    boolean success, T data, ErrorDetail error, PaginationMeta pagination) {

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(true, data, null, null);
  }

  public static <T> ApiResponse<T> ok(T data, PaginationMeta pagination) {
    return new ApiResponse<>(true, data, null, pagination);
  }

  public static <T> ApiResponse<T> error(ErrorDetail errorDetail) {
    return new ApiResponse<>(false, null, errorDetail, null);
  }

  /** Metadane paginacji dla endpointow zwracajacych listy z cursor scrolling. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record PaginationMeta(String cursor, boolean hasMore) {}

  /** Szczegoly bledu zwracane do klienta. fields jest opcjonalne — tylko przy VALIDATION_ERROR. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record ErrorDetail(String code, String message, Object fields) {

    public static ErrorDetail of(ErrorCode code, String message) {
      return new ErrorDetail(code.name(), message, null);
    }

    public static ErrorDetail of(ErrorCode code, String message, Object fields) {
      return new ErrorDetail(code.name(), message, fields);
    }
  }
}
