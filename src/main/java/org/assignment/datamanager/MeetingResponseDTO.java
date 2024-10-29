package org.assignment.datamanager;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class MeetingResponseDTO {
    private Long id;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private EmployeeDTO owner;
    private List<EmployeeDTO> participants;
}
