package org.assignment.service;

import org.assignment.domainmodel.Employee;
import org.assignment.domainmodel.Meeting;
import org.assignment.repository.EmployeeRepository;
import org.assignment.repository.MeetingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("CalendarService Tests")
class CalendarServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private MeetingRepository meetingRepository;

    private CalendarService calendarService;
    private Employee owner;
    private Employee participant1;
    private Employee participant2;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        calendarService = new CalendarService(employeeRepository, meetingRepository);

        baseTime = LocalDateTime.now()
                .withHour(10)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        owner = createEmployee(1L, "Test Owner");
        participant1 = createEmployee(2L, "Participant 1");
        participant2 = createEmployee(3L, "Participant 2");
    }

    @Nested
    @DisplayName("Book Meeting Tests")
    class BookMeetingTests {

        @Test
        @DisplayName("Should successfully book a valid meeting")
        void bookMeeting_Success() {
            // Arrange
            Meeting meeting = createValidMeeting();
            when(employeeRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
            when(meetingRepository.save(any(Meeting.class))).thenReturn(meeting);

            // Act
            Meeting result = calendarService.bookMeeting(owner.getId(), meeting);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getOwner()).isEqualTo(owner);
            verify(meetingRepository).save(meeting);
        }

        @Test
        @DisplayName("Should throw RuntimeException when owner not found")
        void bookMeeting_OwnerNotFound_ThrowsException() {
            // Arrange
            Meeting meeting = createValidMeeting();
            when(employeeRepository.findById(anyLong())).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> calendarService.bookMeeting(1L, meeting));
            assertThat(exception.getMessage()).isEqualTo("Owner not found with ID: 1");
            verify(meetingRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Find Free Slots Tests")
    class FindFreeSlotsTests {

        @Test
        @DisplayName("Should find free slots when no meetings exist")
        void findFreeSlots_NoExistingMeetings_Success() {
            // Arrange
            when(employeeRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
            when(employeeRepository.findById(participant1.getId())).thenReturn(Optional.of(participant1));
            when(meetingRepository.findOverlappingMeetings(anyLong(), any(), any()))
                    .thenReturn(Collections.emptyList());

            // Act
            List<Map<String, Object>> freeSlots = calendarService.findFreeSlots(
                    owner.getId(), participant1.getId(), Duration.ofMinutes(30));

            // Assert
            assertThat(freeSlots).isNotEmpty();
            verify(meetingRepository, times(2)).findOverlappingMeetings(anyLong(), any(), any());
        }

        @Test
        @DisplayName("Should throw RuntimeException when employee not found")
        void findFreeSlots_EmployeeNotFound_ThrowsException() {
            // Arrange
            when(employeeRepository.findById(anyLong())).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> calendarService.findFreeSlots(1L, 2L, Duration.ofMinutes(30)));
            assertThat(exception.getMessage()).contains("Employee 1 not found");
        }
    }

    @Nested
    @DisplayName("Find Conflicts Tests")
    class FindConflictsTests {

        @Test
        @DisplayName("Should find conflicts successfully")
        void findConflicts_Success() {
            // Arrange
            Meeting meeting = createValidMeeting();
            meeting.setParticipants(Arrays.asList(participant1, participant2));
            Meeting conflictingMeeting = createValidMeeting();

            when(meetingRepository.findOverlappingMeetings(eq(participant1.getId()), any(), any()))
                    .thenReturn(Collections.singletonList(conflictingMeeting));
            when(meetingRepository.findOverlappingMeetings(eq(participant2.getId()), any(), any()))
                    .thenReturn(Collections.emptyList());

            // Act
            List<Employee> conflicts = calendarService.findConflicts(meeting);

            // Assert
            assertThat(conflicts)
                    .hasSize(1)
                    .contains(participant1);
        }

        @Test
        @DisplayName("Should find no conflicts when all employees are free")
        void findConflicts_NoConflicts() {
            // Arrange
            Meeting meeting = createValidMeeting();
            meeting.setParticipants(Arrays.asList(participant1, participant2));
            when(meetingRepository.findOverlappingMeetings(anyLong(), any(), any()))
                    .thenReturn(Collections.emptyList());

            // Act
            List<Employee> conflicts = calendarService.findConflicts(meeting);

            // Assert
            assertThat(conflicts).isEmpty();
        }
    }

    private Employee createEmployee(Long id, String name) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setName(name);
        return employee;
    }

    private Meeting createValidMeeting() {
        Meeting meeting = new Meeting();
        meeting.setId(1L);
        meeting.setTitle("Test Meeting");
        meeting.setStartTime(baseTime.plusHours(1));
        meeting.setEndTime(baseTime.plusHours(2));
        meeting.setOwner(owner);
        return meeting;
    }
}