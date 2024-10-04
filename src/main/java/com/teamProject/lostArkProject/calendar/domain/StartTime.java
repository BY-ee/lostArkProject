package com.teamProject.lostArkProject.calendar.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class StartTime {
    private long id;
    private long calendarId;
    private LocalDateTime startTime;
}
