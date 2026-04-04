package vip.mate.cron.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vip.mate.common.result.R;
import vip.mate.cron.model.CronJobDTO;
import vip.mate.cron.service.CronJobService;

import java.util.List;

/**
 * 定时任务管理接口
 *
 * @author MateClaw Team
 */
@Tag(name = "定时任务管理")
@RestController
@RequestMapping("/api/v1/cron-jobs")
@RequiredArgsConstructor
public class CronJobController {

    private final CronJobService cronJobService;

    @Operation(summary = "获取定时任务列表")
    @GetMapping
    public R<List<CronJobDTO>> list() {
        return R.ok(cronJobService.list());
    }

    @Operation(summary = "获取定时任务详情")
    @GetMapping("/{id}")
    public R<CronJobDTO> get(@PathVariable Long id) {
        return R.ok(cronJobService.getById(id));
    }

    @Operation(summary = "创建定时任务")
    @PostMapping
    public R<CronJobDTO> create(@RequestBody CronJobDTO dto) {
        return R.ok(cronJobService.create(dto));
    }

    @Operation(summary = "更新定时任务")
    @PutMapping("/{id}")
    public R<CronJobDTO> update(@PathVariable Long id, @RequestBody CronJobDTO dto) {
        return R.ok(cronJobService.update(id, dto));
    }

    @Operation(summary = "删除定时任务")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        cronJobService.delete(id);
        return R.ok();
    }

    @Operation(summary = "启用/禁用定时任务")
    @PutMapping("/{id}/toggle")
    public R<Void> toggle(@PathVariable Long id, @RequestParam boolean enabled) {
        cronJobService.toggle(id, enabled);
        return R.ok();
    }

    @Operation(summary = "立即执行定时任务")
    @PostMapping("/{id}/run")
    public R<Void> runNow(@PathVariable Long id) {
        cronJobService.runNow(id);
        return R.ok();
    }
}
