package com.teamProject.lostArkProject.content.service;

import com.teamProject.lostArkProject.content.dao.ContentDAO;
import com.teamProject.lostArkProject.content.domain.Content;
import com.teamProject.lostArkProject.content.domain.Reward;
import com.teamProject.lostArkProject.content.domain.StartTime;
import com.teamProject.lostArkProject.content.dto.ContentDTO;
import com.teamProject.lostArkProject.content.dto.ContentStartTimeDTO;
import com.teamProject.lostArkProject.content.dto.api.CalendarApiDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentService {
    private final ContentDAO contentDAO;
    private final WebClient webClient;
    private volatile boolean running = false; // 플래그 변수

    // 외부 api의 Calendar 데이터를 프로젝트의 도메인 형식으로 변환 후 db에 저장
    @Transactional
    @Scheduled(cron = "0 0 4 ? * WED") // 매주 수요일 오전 4시에 로직 실행
    public void saveContents() {
        // 이미 실행 중이라면 메서드 호출 방지
        if(running) {
            log.warn("saveContent 작업이 이미 실행 중입니다.");
            return;
        }
        running = true;

        // db 저장 파이프라인
        Mono.fromRunnable(() -> {
            contentDAO.deleteStartTime();
            contentDAO.deleteReward();
            contentDAO.deleteContent();
            log.info("기존 컨텐츠 데이터 삭제");
        })
        .then(fetchCalendarsFromApi()) // Mono<List<...>>
        .flatMapMany(Flux::fromIterable) // Mono<List<...>>를 Flux<...>로 변환
        .map(this::toDomain) // api 객체를 도메인 객체로 변환하는 메서드 호출
        .flatMap(this::insertContents) // 변환된 데이터를 db에 저장하는 메서드 호출
        .then(Mono.just("컨텐츠 데이터 저장 완료"))
        .doOnSuccess(log::info)
        .doOnError(e -> log.error("컨텐츠 데이터 저장 중 에러 발생: \n{}", e.getMessage()))
        .doFinally(signalType -> running = false) // 플래그 변수 초기화
        .subscribe(); // 등록 (구독)
        /**
         *  reactive stream은 선언적 프로그래밍 모델입니다.
         *  map(), flatmap() 등의 메서드는 작업의 파이프라인을 설정할 뿐 로직이 실행되지는 않습니다.
         *  따라서 파이프라인만 정의하고 로직을 실행하면 아무런 작업도 수행하지 않습니다.
         *
         *  subscribe()는 파이프라인의 모든 연산자를 실행하고 데이터 처리를 하는 역할을 합니다.
         *  파이프라인을 "구축"해둔 후, subscribe()로 "실행"하는 개념이고, 이를 구독이라 합니다.
         *
         *  @Scheduled로 메서드를 호출할 때는 subscribe() 메서드 없이 작업이 실행되지 않습니다.
         *  컨트롤러에서 호출할 때는 Mono 반환 시점에 Spring Webflux가 내부적으로 subscribe()를 호출하기 때문입니다.
         */
    }

    // 외부 api의 Calendar 데이터를 CalendarApiDTO 객체에 매핑
    public Mono<List<CalendarApiDTO>> fetchCalendarsFromApi() {
        log.info("fetchCalendarsFromApi 메서드 호출");

        return webClient.get()
                .uri("/gamecontents/calendar")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<CalendarApiDTO>>() {});
        /**
         *  bodyToMono() 혹은 bodyToFlux()는 외부 api의 응답 데이터를 처리하는 방식에서 차이가 있습니다.
         *
         *  1. bodyToMono()
         *      - bodyToMono()는 응답 데이터를 한 번에 가져옵니다.
         *      - 매개변수로는 가져오는 데이터의 데이터 타입(String.class 혹은 ParameterizedTypeReperence)을 입력합니다.
         *      - bodyToMono(String.class):
         *          응답 데이터를 String (JSON 텍스트) 타입으로 반환받습니다.
         *          이 방식은 텍스트이기 때문에 로그 출력 등의 작업을 할 때는 간단하다는 장점이 있습니다.
         *          하지만 데이터를 실제로 활용하기 위해선 ObjectMapper 객체로 매핑하는 과정이 추가로 필요합니다.
         *
         *      - bodyToMono(new ParameterizedTypeReference<List<Obj>>() {}:
         *          응답 데이터를 List (Java 객체)로 반환받습니다.
         *          응답 데이터의 스펙이 [ ... ] 등의 배열로 시작하는 경우에 적합합니다.
         *          데이터를 객체에 매핑하기 때문에, 이 데이터는 바로 사용할 수 있습니다.
         *          객체에 매핑할 때는 각 필드에 @JsonProperty() 애노테이션을 사용합니다. 애노테이션에는 외부 API의 필드명을 입력합니다.
         *
         *  2. bodyToFlux()
         *      - bodyToFlux()는 응답 데이터를 스트리밍 형식으로 처리합니다.
         *      - 즉 모든 데이터를 한 번에 받아오지 않고 순차적으로 처리합니다.
         *      - bodyToFlux()는 비동기 방식으로 동작하며, 리액티브 프로그래밍의 "구독(subscribe)" 모델을 통해 데이터를 처리합니다.
         *
         *      - 응답받는 데이터가 매우 많은 경우에 적절하지만, 이 데이터에 접근하는 방식이 복잡합니다.
         *      - 따라서 응답받는 데이터의 양이 적다면 비추천합니다.
         */
    }

    // db 저장
    @Transactional
    protected Mono<Void> insertContents(Content content) {
        return Mono.fromRunnable(() -> {
            // content 테이블 저장
            contentDAO.saveContent(content);
            int contentNumber = content.getContentNumber();

            // start_time 테이블 저장
            List<StartTime> startTimes = content.getStartTimes();
            startTimes.forEach(startTime -> startTime.setContentNumber(contentNumber)); // contentNumber 매핑
            contentDAO.saveStartTime(startTimes);

            // reward 테이블 저장
            List<Reward> rewards = content.getRewards();
            rewards.forEach(reward -> reward.setContentNumber(contentNumber)); // contentNumber 매핑
            contentDAO.saveReward(content.getRewards());
        });
    }

    // api 데이터를 도메인 객체 형식으로 변환하는 메서드
    private Content toDomain(CalendarApiDTO calendarApiDTO) {
        Content content = new Content();

        content.setContentName(calendarApiDTO.getContentsName());
        content.setContentIconLink(calendarApiDTO.getContentsIcon());
        content.setMinItemLevel(calendarApiDTO.getMinItemLevel());
        content.setContentLocation(calendarApiDTO.getLocation());
        content.setContentCategory(calendarApiDTO.getCategoryName());

        // api에서 받아온 StartTimes 데이터를 db의 start_times 테이블에 맞게 가공
        List<StartTime> startTimes = Optional.ofNullable(calendarApiDTO.getStartTimes())
                .orElse(List.of("2099-12-30T00:00:00"))
                .stream()
                .map(startTime -> {
                    StartTime st = new StartTime();
                    st.setContentStartTime(LocalDateTime.parse(startTime));
                    return st;
                })
                .toList();
        content.setStartTimes(startTimes);

        // api에서 받아온 RewardItems 데이터를 db의 reward 테이블에 맞게 가공
        List<Reward> rewards = calendarApiDTO.getRewardItems().stream()
                .flatMap(rewardItem -> rewardItem.getItems().stream()
                        .map(item -> {
                            Reward reward = new Reward();

                            reward.setRewardItemName(item.getName());
                            reward.setRewardItemLevel(rewardItem.getItemLevel());
                            reward.setRewardItemIconLink(item.getIcon());
                            reward.setRewardItemGrade(item.getGrade());

                            return reward;
                        })
                ).toList();
        content.setRewards(rewards);

        return content;
    }

    // content 테이블의 모든 데이터 조회
    public List<ContentDTO> getContentsAll() {
        log.info("getContentsAll() 호출");
        return contentDAO.getContentsAll();
    }

    // content, start_time 테이블을 조회 후 매핑
    public List<ContentStartTimeDTO> getContentStartTimes() {
        log.info("content, start_time 테이블을 조회합니다.");
        return contentDAO.getContentStartTimes();
    }
}