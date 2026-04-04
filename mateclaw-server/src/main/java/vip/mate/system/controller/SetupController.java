package vip.mate.system.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import vip.mate.common.result.R;
import vip.mate.config.DatabaseBootstrapRunner;

/**
 * Setup API for first-run initialization.
 * <p>
 * Called by the Desktop splash screen to initialize the database
 * with the user's chosen language before navigating to the main UI.
 * These endpoints require no authentication.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/setup")
@RequiredArgsConstructor
public class SetupController {

    private final DatabaseBootstrapRunner bootstrapRunner;

    /**
     * Check whether the application has been initialized.
     *
     * @return { "initialized": true/false }
     */
    @GetMapping("/status")
    public R<SetupStatus> getStatus() {
        return R.ok(new SetupStatus(bootstrapRunner.isInitialized()));
    }

    /**
     * Initialize the application with the chosen language.
     * This seeds the database with locale-specific data (agents, tools, descriptions).
     *
     * @param request { "language": "zh-CN" | "en-US" }
     * @return success or conflict
     */
    @PostMapping("/init")
    public R<String> init(@RequestBody InitRequest request) {
        String language = request.getLanguage();
        if (language == null || language.isBlank()) {
            language = "zh-CN";
        }
        if (!"zh-CN".equals(language) && !"en-US".equals(language)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported language: " + language);
        }

        boolean success = bootstrapRunner.initWithLocale(language);
        if (!success) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Application already initialized");
        }

        log.info("Application initialized with language={}", language);
        return R.ok("Initialized with " + language);
    }

    @Data
    public static class InitRequest {
        private String language;
    }

    @Data
    public static class SetupStatus {
        private final boolean initialized;
    }
}
