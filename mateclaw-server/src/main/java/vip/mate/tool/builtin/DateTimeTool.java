package vip.mate.tool.builtin;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 内置工具：日期时间
 *
 * @author MateClaw Team
 */
@Component
public class DateTimeTool {

    @Tool(description = "获取当前日期和时间，返回格式为 yyyy-MM-dd HH:mm:ss")
    public String getCurrentDateTime() {
        return LocalDateTime.now(ZoneId.of("Asia/Shanghai"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Tool(description = "获取当前日期，返回格式为 yyyy-MM-dd")
    public String getCurrentDate() {
        return LocalDateTime.now(ZoneId.of("Asia/Shanghai"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    @Tool(description = "获取当前时间，返回格式为 HH:mm:ss")
    public String getCurrentTime() {
        return LocalDateTime.now(ZoneId.of("Asia/Shanghai"))
                .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}
