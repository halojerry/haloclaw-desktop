package vip.mate.exception;

import lombok.Getter;
import vip.mate.common.result.ResultCode;

/**
 * MateClaw 业务异常
 *
 * @author MateClaw Team
 */
@Getter
public class MateClawException extends RuntimeException {

    private final int code;

    public MateClawException(String message) {
        super(message);
        this.code = 500;
    }

    public MateClawException(int code, String message) {
        super(message);
        this.code = code;
    }

    public MateClawException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.code = resultCode.getCode();
    }
}
