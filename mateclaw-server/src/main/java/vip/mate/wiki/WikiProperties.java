package vip.mate.wiki;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Wiki 知识库配置
 *
 * @author MateClaw Team
 */
@Data
@ConfigurationProperties(prefix = "mate.wiki")
public class WikiProperties {

    /** 是否启用 Wiki 知识库功能 */
    private boolean enabled = true;

    /** LLM 单次处理最大字符数（超过则分块） */
    private int maxChunkSize = 30000;

    /** 注入 agent prompt 的最大字符数 */
    private int maxContextChars = 10000;

    /** 单个原始材料最多生成的 Wiki 页面数 */
    private int maxPagesPerRaw = 15;

    /** 上传后是否自动触发处理 */
    private boolean autoProcessOnUpload = true;

    /** 上传文件存储目录 */
    private String uploadDir = "./data/wiki-uploads";

    /** 目录扫描最大文件数 */
    private int maxScanFiles = 500;

    /** 扫描时跳过大于此大小的文件（字节），默认 50MB */
    private long maxScanFileSize = 50 * 1024 * 1024;
}
