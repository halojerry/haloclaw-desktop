package vip.mate.client;

import lombok.Getter;

/**
 * API异常封装
 *
 * @author MateClaw Team
 */
@Getter
public class ApiException extends RuntimeException {

    private final int code;
    private final String errorCode;

    public ApiException(String message) {
        super(message);
        this.code = 500;
        this.errorCode = "INTERNAL_ERROR";
    }

    public ApiException(int code, String message) {
        super(message);
        this.code = code;
        this.errorCode = "HTTP_" + code;
    }

    public ApiException(int code, String errorCode, String message) {
        super(message);
        this.code = code;
        this.errorCode = errorCode;
    }

    public ApiException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
        this.errorCode = errorCode;
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(401, "UNAUTHORIZED", message);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(403, "FORBIDDEN", message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(404, "NOT_FOUND", message);
    }

    public static ApiException badRequest(String message) {
        return new ApiException(400, "BAD_REQUEST", message);
    }

    public static ApiException serverError(String message) {
        return new ApiException(500, "SERVER_ERROR", message);
    }

    public static ApiException timeout(String message) {
        return new ApiException(408, "TIMEOUT", message);
    }

    public static ApiException tokenExpired() {
        return new ApiException(401, "TOKEN_EXPIRED", "Token已过期，请重新登录");
    }

    public static ApiException invalidToken() {
        return new ApiException(401, "INVALID_TOKEN", "Token无效，请重新登录");
    }
}
