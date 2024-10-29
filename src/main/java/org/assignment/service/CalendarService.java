package org.assignment.service;

import lombok.extern.slf4j.Slf4j;
import org.assignment.domainmodel.Employee;
import org.assignment.domainmodel.Meeting;
import org.assignment.repository.EmployeeRepository;
import org.assignment.repository.MeetingRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service to manage calendar operations.
 * Business Rules:
 * 1. Meetings cannot overlap for the same participant
 * 2. Meeting duration must be positive
 * 3. Meetings can only be scheduled during business hours (9 AM - 5 PM)
 * 4. All participants must exist in the system
 */
@Slf4j
@Service
public class CalendarService {

    private final EmployeeRepository employeeRepository;
    private final MeetingRepository meetingRepository;

    public CalendarService(EmployeeRepository employeeRepository, MeetingRepository meetingRepository) {
        this.employeeRepository = employeeRepository;
        this.meetingRepository = meetingRepository;
    }

    public Meeting bookMeeting(Long ownerId, Meeting meeting) {
        log.info("Attempting to book meeting. Owner ID: {}, Meeting Title: {}", ownerId, meeting.getTitle());
        try {
            Employee owner = employeeRepository.findById(ownerId)
                    .orElseThrow(() -> new RuntimeException("Owner not found with ID: " + ownerId));
            meeting.setOwner(owner);

            Meeting savedMeeting = meetingRepository.save(meeting);
            log.info("Successfully booked meeting. Meeting ID: {}, Owner: {}, Start Time: {}",
                    savedMeeting.getId(), owner.getName(), savedMeeting.getStartTime());
            return savedMeeting;
        } catch (RuntimeException e) {
            log.error("Failed to book meeting. Owner ID: {}, Error: {}", ownerId, e.getMessage(), e);
            throw e;
        }
    }

    public List<Map<String, Object>> findFreeSlots(Long employee1Id, Long employee2Id, Duration duration) {
        log.info("Finding free slots for employees. Employee1 ID: {}, Employee2 ID: {}, Duration: {}",
                employee1Id, employee2Id, duration);
        try {
            Employee employee1 = employeeRepository.findById(employee1Id)
                    .orElseThrow(() -> new RuntimeException("Employee 1 not found"));
            Employee employee2 = employeeRepository.findById(employee2Id)
                    .orElseThrow(() -> new RuntimeException("Employee 2 not found"));

            LocalDateTime now = LocalDateTime.now().withHour(9).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime end = now.plusDays(7).withHour(17).withMinute(0).withSecond(0).withNano(0);

            log.debug("Searching for meetings between {} and {}", now, end);

            List<Meeting> meetings1 = meetingRepository.findOverlappingMeetings(employee1Id, now, end);
            List<Meeting> meetings2 = meetingRepository.findOverlappingMeetings(employee2Id, now, end);

            log.debug("Found {} meetings for employee1 and {} meetings for employee2",
                    meetings1.size(), meetings2.size());

            List<Map<String, Object>> freeSlots = new ArrayList<>();
            LocalDateTime current = now;

            while (current.isBefore(end)) {
                if (current.getHour() >= 9 && current.getHour() < 17) {
                    LocalDateTime slotEnd = current.plus(duration);
                    if (isSlotFree(current, slotEnd, meetings1) && isSlotFree(current, slotEnd, meetings2)) {
                        Map<String, Object> slot = new HashMap<>();
                        slot.put("date", current.toLocalDate().toString());
                        slot.put("startTime", current.toLocalTime().toString());
                        slot.put("endTime", slotEnd.toLocalTime().toString());
                        freeSlots.add(slot);
                    }
                }
                current = current.plusMinutes(30);
            }

            log.info("Found {} free slots for employees {} and {}",
                    freeSlots.size(), employee1.getName(), employee2.getName());
            return freeSlots;

        } catch (RuntimeException e) {
            log.error("Error finding free slots. Employee1 ID: {}, Employee2 ID: {}, Error: {}",
                    employee1Id, employee2Id, e.getMessage(), e);
            throw e;
        }
    }

    private boolean isSlotFree(LocalDateTime start, LocalDateTime end, List<Meeting> meetings) {
        log.trace("Checking if slot is free between {} and {}", start, end);
        boolean isFree = meetings.stream().noneMatch(meeting ->
                (meeting.getStartTime().isBefore(end) && meeting.getEndTime().isAfter(start)) ||
                        (meeting.getStartTime().isAfter(start) && meeting.getStartTime().isBefore(end))
        );
        log.trace("Slot {} free between {} and {}", isFree ? "is" : "is not", start, end);
        return isFree;
    }

    public List<Employee> findConflicts(Meeting proposedMeeting) {
        log.info("Checking conflicts for meeting. Title: {}, Start: {}, End: {}",
                proposedMeeting.getTitle(),
                proposedMeeting.getStartTime(),
                proposedMeeting.getEndTime());

        List<Employee> conflictingEmployees = new ArrayList<>();

        try {
            // Check for owner conflicts
            List<Meeting> ownerConflicts = meetingRepository.findOverlappingMeetings(
                    proposedMeeting.getOwner().getId(),
                    proposedMeeting.getStartTime(),
                    proposedMeeting.getEndTime()
            );

            log.debug("Found {} conflicting meetings for owner {}",
                    ownerConflicts.size(), proposedMeeting.getOwner().getName());

            if (!ownerConflicts.isEmpty()) {
                conflictingEmployees.add(proposedMeeting.getOwner());
            }

            // Check for participant conflicts
            for (Employee participant : proposedMeeting.getParticipants()) {
                List<Meeting> participantConflicts = meetingRepository.findOverlappingMeetings(
                        participant.getId(),
                        proposedMeeting.getStartTime(),
                        proposedMeeting.getEndTime()
                );

                log.debug("Found {} conflicting meetings for participant {}",
                        participantConflicts.size(), participant.getName());

                if (!participantConflicts.isEmpty()) {
                    conflictingEmployees.add(participant);
                }
            }

            if (!conflictingEmployees.isEmpty()) {
                log.info("Found {} employees with conflicts: {}",
                        conflictingEmployees.size(),
                        conflictingEmployees.stream()
                                .map(Employee::getName)
                                .collect(Collectors.joining(", ")));
            } else {
                log.info("No conflicts found for the proposed meeting");
            }

            return conflictingEmployees;

        } catch (Exception e) {
            log.error("Error checking meeting conflicts. Meeting ID: {}, Error: {}",
                    proposedMeeting.getId(), e.getMessage(), e);
            throw e;
        }
    }
}
