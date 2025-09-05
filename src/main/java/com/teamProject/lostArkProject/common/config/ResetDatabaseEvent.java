package com.teamProject.lostArkProject.common.config;

import com.teamProject.lostArkProject.collectible.service.CollectibleService;
import com.teamProject.lostArkProject.content.service.ContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/** 애플리케이션 실행 시 데이터베이스 초기화 처리 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ResetDatabaseEvent implements ApplicationListener<ApplicationReadyEvent> {
    private final ContentService contentService;
    private final CollectibleService collectibleService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("데이터베이스 초기화 작업을 시작합니다.");
        contentService.saveContents();
        // collectibleService.saveRecommendCollectible();
    }
}
