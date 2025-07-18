package com.teamProject.lostArkProject.member.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamProject.lostArkProject.collectible.domain.CharacterInfo;
import com.teamProject.lostArkProject.common.utils.CommonUtils;
import com.teamProject.lostArkProject.member.dao.MemberDAO;
import com.teamProject.lostArkProject.member.domain.Member;
import com.teamProject.lostArkProject.member.domain.MemberCharacter;
import com.teamProject.lostArkProject.member.dto.CertificationDTO;
import com.teamProject.lostArkProject.member.dto.CharacterCertificationDTO;
import com.teamProject.lostArkProject.member.dto.EquipmentDTO;
import com.teamProject.lostArkProject.member.dto.api.CharacterImageApiDTO;
import com.teamProject.lostArkProject.member.dto.api.EquipmentApiDTO;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class MemberService {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final MemberDAO memberDAO;
    private final BCryptPasswordEncoder passwordEncoder;

    public MemberService(WebClient webClient, ObjectMapper objectMapper, MemberDAO memberDAO, BCryptPasswordEncoder passwordEncoder) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.memberDAO = memberDAO;
        this.passwordEncoder = passwordEncoder;
    }

    //회원가입 로직
    public void signupMember(Member member) {
        memberDAO.insertMember(member);
    }

    //로그인 비밀번호 확인
    public boolean checkSignin(String memberId, String insertPW) {
        String DB_PW = memberDAO.getMemberPW(memberId);
        return passwordEncoder.matches(insertPW, DB_PW);
    }

    //대표 캐릭터 닉네임 가져오기
    public String getRepresentativeCharacterNickname(String memberId) {
        return memberDAO.getRepresentativeCharacter(memberId);
    }

    //이메일 체크
    public boolean checkEmail(String memberId) {
        if(memberDAO.checkMemberId(memberId)==null) return false;
        return true;
    }
    public void changePassword(String memberId, String memberPW) {
        memberDAO.updateMemberPW(memberId, memberPW);
    }

    public Mono<String> getRosterLevel(String characterName) {
        return webClient.get()
                .uri("/armories/characters/{name}", characterName)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(apiResponse -> {
                    try {
                        System.out.println("API 응답 원문: " + apiResponse);
                        JsonNode root = objectMapper.readTree(apiResponse);
                        JsonNode profile = root.path("ArmoryProfile");

                        if (profile.isMissingNode()) {
                            return Mono.error(new RuntimeException("ArmoryProfile 항목이 없습니다"));
                        }

                        String rosterLevel = profile.path("ExpeditionLevel").asText();
                        if (rosterLevel == null || rosterLevel.isBlank()) {
                            return Mono.error(new RuntimeException("ExpeditionLevel이 비어 있습니다"));
                        }

                        return Mono.just(rosterLevel);
                    } catch (Exception e) {
                        return Mono.error(new RuntimeException("API 응답 파싱 실패", e));
                    }
                });
    }

    public List<MemberCharacter> getMemberCharacterList(List<CharacterInfo> characterInfoList, String rosterLevel, String memberId) {
        return characterInfoList.stream()
                .map(info -> {
                    MemberCharacter member = new MemberCharacter();
                    member.setCharacterNickname(info.getCharacterName());
                    member.setServerName(info.getServerName());
                    member.setCharacterClass(info.getCharacterClassName());
                    member.setItemLevel(info.getItemAvgLevel());
                    member.setRosterLevel(rosterLevel);
                    member.setMemberId(memberId);

                    return member;
                })
                .collect(Collectors.toList());
    }

    public void insertMemberCharacter(List<MemberCharacter> memberCharacterList) {
        for (MemberCharacter memberCharacter : memberCharacterList) {
            memberDAO.insertMemberCharacter(memberCharacter);
        }
    }

    public Mono<List<CharacterInfo>> getCharacterInfo(String characterName) {
        return webClient.get()
                .uri("/characters/" + characterName + "/siblings")
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(apiResponse -> {
                    try {
                        List<CharacterInfo> characterInfos = objectMapper.readValue(apiResponse, new TypeReference<List<CharacterInfo>>() {});
                        return Mono.just(characterInfos);
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                });
    }

    public boolean updateRCN(String memberId, String RCN) {
        List<CharacterInfo> characterInfoList = getCharacterInfo(RCN).block();
        if (characterInfoList == null || characterInfoList.isEmpty()) return false;
        String rosterLevel = getRosterLevel(RCN).block();
        List<MemberCharacter> memberCharacterList = getMemberCharacterList(characterInfoList, rosterLevel, memberId);

        memberDAO.deleteMemberCharacter(memberId);
        insertMemberCharacter(memberCharacterList);
        memberDAO.updateRCN(memberId, RCN);
        return true;
    }

    public Mono<CharacterCertificationDTO> requestCertification(String nickname) {
        // 1. 이미지, 장비 api 데이터 요청
        Mono<CharacterImageApiDTO> characterImageMono = webClient.get()
                .uri("/armories/characters/{name}/profiles", nickname)
                .retrieve()
                .bodyToMono(CharacterImageApiDTO.class);
        Mono<List<EquipmentApiDTO>> equipmentApiMono = webClient.get()
                .uri("/armories/characters/{name}/equipment", nickname)
                .retrieve()
                .bodyToFlux(EquipmentApiDTO.class)
                .collectList();

        // 2. 해제할 장비를 설정
        Mono<List<EquipmentDTO>> equipmentMono = equipmentApiMono.map(equipmentApiDTOList -> {
            // 불필요한 장비 필터링, 중복 장비에 순번 부여
            AtomicInteger ringIdx = new AtomicInteger(1);   // 반지 순번 정수 객체 (동시성 이슈)
            AtomicInteger earringIdx = new AtomicInteger(1);  // map에서 숫자 연산/비교를 위한 정수 객체 (동시성 이슈)
            List<EquipmentApiDTO> filteredlist = equipmentApiDTOList.stream()
                    .filter(equipmentApiDTO -> !"부적".equals(equipmentApiDTO.getType()) && !"문장".equals(equipmentApiDTO.getType()))  // 부적 제외
                    .map(equipmentApiDTO -> {
                        if ("반지".equals(equipmentApiDTO.getType())) {
                            equipmentApiDTO.setType("반지" + ringIdx.getAndIncrement());
                        } else if ("귀걸이".equals(equipmentApiDTO.getType())) {
                            equipmentApiDTO.setType("귀걸이" + earringIdx.getAndIncrement());
                        }
                        return equipmentApiDTO;
                    })
                    .collect(Collectors.toList());

            // 장비 순서 무작위로 설정
            Collections.shuffle(filteredlist);

            // 해제할 장비 설정
            AtomicInteger count = new AtomicInteger(1);  // map에서 숫자 연산/비교를 위한 정수 객체 (동시성 이슈)
            return filteredlist.stream()
                    .map(equipmentApiDTO -> {  // 해제 여부 설정
                        EquipmentDTO equipmentDTO = new EquipmentDTO();
                        equipmentDTO.setType(equipmentApiDTO.getType());
                        equipmentDTO.setName(equipmentApiDTO.getName());
                        equipmentDTO.setIcon(equipmentApiDTO.getIcon());
                        equipmentDTO.setGrade(equipmentApiDTO.getGrade());
                        if (count.getAndIncrement() <= 2) {
                            equipmentDTO.setUnequippedRequired(true);
                        } else {
                            equipmentDTO.setUnequippedRequired(false);
                        }
                        return equipmentDTO;
                    })
                    .toList();
                }
        );

        // 3. 해제할 장비만 필터링한 후, type (무기, 상의, ...)을 key로 하는 Map으로 변환
        Mono<Map<String, EquipmentDTO>> equipmentMap = equipmentMono.map(equipmentDTOList ->
                CommonUtils.listToNumberedKeyMap(equipmentDTOList, e -> e.getType())
        );

        // 4. 캐릭터 이미지 리스트, 해제 장비 설정된 장비 맵을 묶어서 반환
        return Mono.zip(characterImageMono, equipmentMap)
                .map(tuple -> {
                    CharacterImageApiDTO t1 = tuple.getT1();       // 이미지
                    Map<String, EquipmentDTO> t2 = tuple.getT2();  // 해제할 장비

                    return new CharacterCertificationDTO(t1, t2);
                });
    }

    public Mono<List<CertificationDTO>> getCertification(String nickname) {
        Mono<List<EquipmentApiDTO>> equipmentApiMono = webClient.get()
                .uri("/armories/characters/{name}/equipment", nickname)
                .retrieve()
                .bodyToFlux(EquipmentApiDTO.class)
                .collectList();

        return equipmentApiMono.map(equipmentApiDTOList -> {
                    // 불필요한 장비 필터링, 중복 장비에 순번 부여
                    AtomicInteger ringIdx = new AtomicInteger(1);   // 반지 순번 정수 객체 (동시성 이슈)
                    AtomicInteger earringIdx = new AtomicInteger(1);  // map에서 숫자 연산/비교를 위한 정수 객체 (동시성 이슈)


                    return equipmentApiDTOList.stream()
                    // 1) “부적” 제외
                    .filter(dto -> !"부적".equals(dto.getType()) && !"문장".equals(dto.getType()))
                    // 2) 타입에 순번 붙이고 CertificationDTO 생성
                    .map(dto -> {
                        String type = dto.getType();
                        if ("반지".equals(type)) {
                            type += ringIdx.getAndIncrement();
                        } else if ("귀걸이".equals(type)) {
                            type += earringIdx.getAndIncrement();
                        }
                        CertificationDTO cert = new CertificationDTO();
                        cert.setType(type);
                        return cert;
                    })
                    // 3) 최종 리스트로 수집
                    .collect(Collectors.toList());
        });
    }

    public void updateCertification(String memberId) {
        memberDAO.updateCertification(memberId);
    }
}
