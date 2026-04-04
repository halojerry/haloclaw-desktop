package vip.mate.memory;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 记忆模块自动配置
 *
 * @author MateClaw Team
 */
@Configuration
@EnableAsync
@EnableConfigurationProperties(MemoryProperties.class)
public class MemoryAutoConfiguration {
}
