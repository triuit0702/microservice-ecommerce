package net.javaguides.product_service.jobs;


import lombok.RequiredArgsConstructor;
import net.javaguides.product_service.service.CloudinaryService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class TempImageCleanupJob {


    private final CloudinaryService cloudinaryService;

    @Scheduled(
            cron = "0 0 0 * * *",
            zone = "Asia/Ho_Chi_Minh"
    )
    public void cleanupAtMidnight() {
        cleanupUnsavedImages("DAILY_CRON");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void runAtStartup() {
        cleanupUnsavedImages("STARTUP");
    }

    private void cleanupUnsavedImages(String trigger) {
        // logic xoá ảnh temp ở đây
        cloudinaryService.deleteTempImage(trigger);
    }
}
