package com.teamProject.lostArkProject.teaching.service;

import com.teamProject.lostArkProject.teaching.dao.MessageDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.teamProject.lostArkProject.teaching.dto.MenteeApplyDTO;
import com.teamProject.lostArkProject.teaching.dto.MenteeDTO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageDAO messageDAO;

    @Override
    public void acceptMentee(String menteeMemberId, String mentorDiscordId) {
        // 구현 내용
    }

    @Override
    public MenteeApplyDTO getMenteeApplyDetail(Map<String, Object> param) {
        return messageDAO.getMenteeApplyDetail(param);
    }

    @Override
    public List<MenteeDTO> getMenteeDetail(String menteeMemberId) {
        return messageDAO.getMenteeDetail(menteeMemberId);
    }

    @Override
    public Map<String, Object> getMenteeCharacterInfo(String menteeMemberId) {
        return messageDAO.getMenteeCharacterInfo(menteeMemberId);
    }

    @Override
    public void acceptMenteeApply(String mentorMemberId, String menteeMemberId) {
        MenteeApplyDTO dto = new MenteeApplyDTO();
        dto.setMentorMemberId(mentorMemberId);
        dto.setMenteeMemberId(menteeMemberId);
        messageDAO.acceptMenteeApply(dto);
    }

    @Override
    public void rejectMenteeApplyWithReason(String mentorMemberId, String menteeMemberId, String rejectReason, boolean blockMentee) {
        // 1. 멘티 신청을 거절 사유와 함께 업데이트
        MenteeApplyDTO dto = new MenteeApplyDTO();
        dto.setMentorMemberId(mentorMemberId);
        dto.setMenteeMemberId(menteeMemberId);
        dto.setRejectedReason(rejectReason);
        
        messageDAO.rejectMenteeApplyWithReason(dto);
        
        // 2. 차단 여부가 true인 경우 DISABLE_MENTEE 테이블에 추가
        if (blockMentee) {
            Map<String, String> params = new HashMap<>();
            params.put("mentorMemberId", mentorMemberId);
            params.put("menteeMemberId", menteeMemberId);
            messageDAO.insertDisableMentee(params);
        }
    }

    @Override
    public List<Map<String, Object>> getAllMenteeAppliesByMentor(String mentorMemberId) {
        return messageDAO.getAllMenteeAppliesByMentor(mentorMemberId);
    }

    @Override
    public List<Map<String, Object>> getAllMenteeAppliesByMentee(String menteeMemberId) {
        return messageDAO.getAllMenteeAppliesByMentee(menteeMemberId);
    }




}
