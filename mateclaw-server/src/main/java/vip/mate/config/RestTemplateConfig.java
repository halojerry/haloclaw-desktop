package vip.mate.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;

/**
 * RestTemplate配置
 * 配置超时、重试、拦截器等
 *
 * @author MateClaw Team
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RestTemplateConfig {

    private final CloudApiProperties cloudApiProperties;

    @Bean(name = "cloudApiRestTemplate")
    public RestTemplate cloudApiRestTemplate() {
        RestTemplate restTemplate = new RestTemplate(createRequestFactory());

        // 配置超时
        restTemplate.setRequestFactory(createRequestFactory());

        // 设置错误处理器（不自动抛出异常，由业务层处理）
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                // 跳过默认错误处理，让业务层自己处理HTTP错误
                return false;
            }
        });

        // 添加自定义拦截器
        restTemplate.getInterceptors().add((request, body, execution) -> {
            log.debug("Cloud API Request: {} {}", request.getMethod(), request.getURI());
            return execution.execute(request, body);
        });

        return restTemplate;
    }

    @Bean(name = "newApiRestTemplate")
    public RestTemplate newApiRestTemplate() {
        RestTemplate restTemplate = new RestTemplate(createRequestFactory());
        restTemplate.setRequestFactory(createRequestFactory());
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                // 跳过默认错误处理
                return false;
            }
        });
        return restTemplate;
    }

    private ClientHttpRequestFactory createRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(cloudApiProperties.getConnectTimeout()));
        factory.setReadTimeout(Duration.ofMillis(cloudApiProperties.getReadTimeout()));
        // Spring Boot 3.5: SimpleClientHttpRequestFactory 不再支持 setWriteTimeout
        // 写超时通过 readTimeout 间接控制
        return factory;
    }
}
