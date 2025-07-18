package com.teamProject.lostArkProject.academy.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

@Getter
@Setter
@Alias("academy_board")
@Builder
public class AcademyBoard {
    private int academyBoardNumber;
    private String writer;
    private String title;
    private String content;
    private String raid;
    private String image;
    private LocalDateTime createdAt;
}
