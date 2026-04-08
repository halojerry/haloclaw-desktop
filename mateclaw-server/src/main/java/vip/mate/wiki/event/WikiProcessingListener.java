package vip.mate.wiki.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import vip.mate.wiki.service.WikiProcessingService;

/**
 * Wiki 处理事件监听器
 * <p>
 * 异步处理原始材料消化事件。
 *
 * @author MateClaw Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WikiProcessingListener {

    private final WikiProcessingService processingService;

    @Async
    @EventListener
    public void onWikiProcessing(WikiProcessingEvent event) {
        log.info("[Wiki] Processing event received: rawId={}, kbId={}", event.getRawMaterialId(), event.getKbId());
        try {
            processingService.processRawMaterial(event.getRawMaterialId());
        } catch (Exception e) {
            log.error("[Wiki] Async processing failed for rawId={}: {}", event.getRawMaterialId(), e.getMessage(), e);
        }
    }
}
