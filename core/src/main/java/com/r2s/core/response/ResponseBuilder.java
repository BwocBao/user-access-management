//package com.r2s.core.response;
//
//import com.r2s.core.dto.ApiResponse;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Component;
//
///**
// 8. * Utility class for building consistent API responses
// 9. */
//@Component
//@RequiredArgsConstructor
//public class ResponseBuilder {
//
//    /**
//     15. * Build a success response with data and message
//     16. * @param data Response data
//     17. * @param message Success message
//     18. * @return ResponseEntity with ApiResponse
//     19. */
//    public <T> ResponseEntity<ApiResponse<T>> buildSuccessResponse(T data, String message) {
//        return ResponseEntity.ok(ApiResponse.success(data, message));
//    }
//
//    /**
//     25. * Build an error response with message
//     26. * @param message Error message
//     27. * @return ResponseEntity with ApiResponse
//     28. */
//    public <T> ResponseEntity<ApiResponse<T>> buildErrorResponse(String message) {
//        return ResponseEntity.badRequest().body(ApiResponse.error(message));
//    }
//    /**
//     34. * Build a success response with data only
//     35. * @param data Response data
//     36. * @return ResponseEntity with ApiResponse
//     37. */
//    public <T> ResponseEntity<ApiResponse<T>> buildSuccessResponse(T data) {
//        return buildSuccessResponse(data, "Success");
//    }
//    /**
//     43. * Build a not found response
//     44. * @param message Error message
//     45. * @return ResponseEntity with ApiResponse
//     46. */
//    public <T> ResponseEntity<ApiResponse<T>> buildNotFoundResponse(String message) {
//        return ResponseEntity.notFound().build();
//    }
//
//    /**
//     52. * Build an unauthorized response
//     53. * @param message Error message
//     54. * @return ResponseEntity with ApiResponse
//     55. */
//    public <T> ResponseEntity<ApiResponse<T>> buildUnauthorizedResponse(String message) {
//        return ResponseEntity.status(401).body(ApiResponse.error(message));
//    }
//    /**
//     61. * Build a forbidden response
//     62. * @param message Error message
//     63. * @return ResponseEntity with ApiResponse
//     64. */
//    public <T> ResponseEntity<ApiResponse<T>> buildForbiddenResponse(String message) {
//        return ResponseEntity.status(403).body(ApiResponse.error(message));
//    }
//}

