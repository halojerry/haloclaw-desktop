package vip.mate.agent.prompt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prompt 文件加载器
 * <p>
 * 从 classpath:/prompts/ 目录加载 .txt 文件，使用 ConcurrentHashMap 做线程安全的懒加载缓存。
 * <p>
 * 未来扩展点：可在 loadPrompt() 中增加"先查数据库覆盖 → 再读 resource → 最后代码兜底"的优先级链，
 * 但本次只实现 resource 读取。
 *
 * @author MateClaw Team
 */
@Slf4j
public final class PromptLoader {

    private static final String PROMPT_PATH_PREFIX = "prompts/";

    private static final ConcurrentHashMap<String, String> promptCache = new ConcurrentHashMap<>();

    private PromptLoader() {}

    /**
     * 加载 prompt 文件内容
     *
     * @param promptName 文件名（不含路径前缀和 .txt 后缀），例如 "graph/summarize-system"
     * @return 文件文本内容
     * @throws RuntimeException 文件不存在或读取失败时抛出，不会静默返回空字符串
     */
    public static String loadPrompt(String promptName) {
        return promptCache.computeIfAbsent(promptName, name -> {
            String fileName = PROMPT_PATH_PREFIX + name + ".txt";
            try (InputStream inputStream = PromptLoader.class.getClassLoader().getResourceAsStream(fileName)) {
                if (inputStream == null) {
                    throw new RuntimeException("Prompt 文件不存在: " + fileName);
                }
                return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.error("加载 Prompt 失败！{}", e.getMessage(), e);
                throw new RuntimeException("加载 Prompt 失败: " + name, e);
            }
        });
    }

    /**
     * 清空缓存
     */
    public static void clearCache() {
        promptCache.clear();
    }

    /**
     * 获取缓存大小
     *
     * @return 已缓存的 prompt 数量
     */
    public static int getCacheSize() {
        return promptCache.size();
    }
}
