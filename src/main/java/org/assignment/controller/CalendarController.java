package org.assignment.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.assignment.datamanager.EmployeeDTO;
import org.assignment.datamanager.MeetingDTO;
import org.assignment.datamanager.MeetingResponseDTO;
import org.assignment.domainmodel.Employee;
import org.assignment.domainmodel.Meeting;
import org.assignment.repository.EmployeeRepository;
import org.assignment.service.CalendarService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/calendar")
@Slf4j
public class CalendarController {
    private final CalendarService calendarService;
    private final EmployeeRepository employeeRepository;

    public CalendarController(CalendarService calendarService, EmployeeRepository employeeRepository) {
        this.calendarService = calendarService;
        this.employeeRepository = employeeRepository;
    }

    @PostMapping("/meetings")
    public ResponseEntity<MeetingResponseDTO> bookMeeting(
            @RequestParam Long ownerId,
            @Valid @RequestBody MeetingDTO meetingDTO) {
        log.info("Received request to book meeting. Owner ID: {}, Meeting Title: {}",
                ownerId, meetingDTO.getTitle());

        Meeting meeting = convertToMeeting(meetingDTO, ownerId);
        Meeting bookedMeeting = calendarService.bookMeeting(ownerId, meeting);

        MeetingResponseDTO response = convertToMeetingResponseDTO(bookedMeeting);
        log.info("Successfully booked meeting with ID: {}", bookedMeeting.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/free-slots")
    public ResponseEntity<List<Map<String, Object>>> findFreeSlots(
            @RequestParam Long employee1Id,
            @RequestParam Long employee2Id,
            @RequestParam int durationMinutes) {
        log.info("Searching for free slots. Employee1 ID: {}, Employee2 ID: {}, Duration: {} minutes",
                employee1Id, employee2Id, durationMinutes);

        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("Duration must be positive");
        }

        List<Map<String, Object>> freeSlots = calendarService.findFreeSlots(
                employee1Id, employee2Id, Duration.ofMinutes(durationMinutes));

        log.info("Found {} free slots", freeSlots.size());
        return ResponseEntity.ok(freeSlots);
    }

    @PostMapping("/conflicts")
    public ResponseEntity<List<EmployeeDTO>> findConflicts(@Valid @RequestBody MeetingDTO meetingDTO) {
        log.info("Checking conflicts for meeting. Title: {}, Start: {}, End: {}",
                meetingDTO.getTitle(), meetingDTO.getStartTime(), meetingDTO.getEndTime());

        Meeting meeting = convertToMeeting(meetingDTO, meetingDTO.getOwnerId());
        List<EmployeeDTO> conflicts = calendarService.findConflicts(meeting).stream()
                .map(this::convertToEmployeeDTO)
                .collect(Collectors.toList());

        log.info("Found {} conflicting employees for meeting '{}'",
                conflicts.size(), meetingDTO.getTitle());
        return ResponseEntity.ok(conflicts);
    }

    private MeetingResponseDTO convertToMeetingResponseDTO(Meeting meeting) {
        MeetingResponseDTO dto = new MeetingResponseDTO();
        dto.setId(meeting.getId());
        dto.setTitle(meeting.getTitle());
        dto.setStartTime(meeting.getStartTime());
        dto.setEndTime(meeting.getEndTime());
        dto.setOwner(convertToEmployeeDTO(meeting.getOwner()));
        dto.setParticipants(meeting.getParticipants().stream()
                .map(this::convertToEmployeeDTO)
                .collect(Collectors.toList()));
        return dto;
    }

    private Meeting convertToMeeting(MeetingDTO meetingDTO, Long ownerId) {
        log.debug("Converting MeetingDTO to Meeting entity. Owner ID: {}", ownerId);
        Meeting meeting = new Meeting();
        meeting.setTitle(meetingDTO.getTitle());
        meeting.setStartTime(meetingDTO.getStartTime());
        meeting.setEndTime(meetingDTO.getEndTime());

        Employee owner = employeeRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Owner not found with ID: " + ownerId));
        meeting.setOwner(owner);

        List<Employee> participants = employeeRepository.findAllById(meetingDTO.getParticipantIds());
        if (participants.size() != meetingDTO.getParticipantIds().size()) {
            throw new IllegalArgumentException("One or more participants not found");
        }
        meeting.setParticipants(participants);

        return meeting;
    }

    private EmployeeDTO convertToEmployeeDTO(Employee employee) {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setId(employee.getId());
        dto.setName(employee.getName());
        return dto;
    }
}
