package vip.mate.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一API响应封装
 *
 * @author MateClaw Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    /** 状态码 */
    private int code;

    /** 消息 */
    private String message;

    /** 数据 */
    private T data;

    /** 请求ID */
    private String requestId;

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "success", data, null);
    }

    public static <T> ApiResult<T> success(String message, T data) {
        return new ApiResult<>(200, message, data, null);
    }

    public static <T> ApiResult<T> error(int code, String message) {
        return new ApiResult<>(code, message, null, null);
    }

    public static <T> ApiResult<T> error(String message) {
        return new ApiResult<>(500, message, null, null);
    }

    public boolean isSuccess() {
        return code == 200 || code == 0;
    }
}
