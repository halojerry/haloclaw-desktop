package vip.mate.wiki;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Wiki 知识库模块自动配置
 *
 * @author MateClaw Team
 */
@Configuration
@EnableAsync
@EnableConfigurationProperties(WikiProperties.class)
public class WikiAutoConfiguration {
}
