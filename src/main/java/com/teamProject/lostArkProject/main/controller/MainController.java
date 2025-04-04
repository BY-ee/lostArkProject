package com.teamProject.lostArkProject.main.controller;

import com.teamProject.lostArkProject.alarm.domain.Alarm;
import com.teamProject.lostArkProject.alarm.service.AlarmService;
import com.teamProject.lostArkProject.collectible.dto.CollectiblePointSummaryDTO;
import com.teamProject.lostArkProject.collectible.service.CollectibleService;
import com.teamProject.lostArkProject.common.dto.PaginatedRequestDTO;
import com.teamProject.lostArkProject.member.domain.Member;
import com.teamProject.lostArkProject.notice.domain.Notice;
import com.teamProject.lostArkProject.notice.service.NoticeService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MainController {
    private final CollectibleService collectibleService;
    private final AlarmService alarmService;
    private final NoticeService noticeService;

    // 메인페이지
    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        PaginatedRequestDTO requestDTO = new PaginatedRequestDTO(1, 3);
        List<Notice> noticeList = noticeService.getNoticeList(requestDTO);
        Member member = (Member) session.getAttribute("member");
        if (member != null) {
            List<Alarm> alarms = alarmService.getAllAlarm(member.getMemberId());
            log.info("alarms: {}", alarms);
            model.addAttribute("alarms", alarms);
        }
        model.addAttribute("noticeList", noticeList);

        return "index";
    }

    // 내실
    @GetMapping("/collectible")
    public String getCharacterCollectible(Model model, HttpSession session) {
        Member member = (Member) session.getAttribute("member"); // http 세션에서 가져온 닉네임
        if(member == null) {
            return "member/signin";
        }
        List<CollectiblePointSummaryDTO> collectibleItemList = collectibleService.getCollectiblePointSummary(member.getMemberId());
        model.addAttribute("collectibleItemList", collectibleItemList);
        return "collectible/collectible"; // 결과 뷰로 이동
    }

}