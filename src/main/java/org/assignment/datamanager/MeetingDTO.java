package org.assignment.datamanager;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MeetingDTO {
    private Long id;

    private String title;

    @NotNull(message = "Start time must not be null")
    private LocalDateTime startTime;

    @NotNull(message = "End time must not be null")
    private LocalDateTime endTime;

    private Long ownerId;

    private List<Long> participantIds;
}
