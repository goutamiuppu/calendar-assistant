package org.assignment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assignment.datamanager.MeetingDTO;
import org.assignment.domainmodel.Employee;
import org.assignment.domainmodel.Meeting;
import org.assignment.repository.EmployeeRepository;
import org.assignment.service.CalendarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CalendarController.class)
@DisplayName("Calendar Controller Integration Tests")
class CalendarControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CalendarService calendarService;

    @MockBean
    private EmployeeRepository employeeRepository;

    private Employee owner;
    private Employee participant1;
    private Employee participant2;
    private Meeting meeting;
    private MeetingDTO meetingDTO;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        baseTime = LocalDateTime.now()
                .withHour(10)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        owner = createEmployee(1L, "Test Owner");
        participant1 = createEmployee(2L, "Participant 1");
        participant2 = createEmployee(3L, "Participant 2");

        meeting = createMeeting();
        meetingDTO = createMeetingDTO();
    }

    @Nested
    @DisplayName("Book Meeting Endpoint Tests")
    class BookMeetingTests {

        @Test
        @DisplayName("Should successfully book a meeting")
        void bookMeeting_Success() throws Exception {
            // Arrange
            when(employeeRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
            when(employeeRepository.findAllById(any())).thenReturn(Arrays.asList(participant1, participant2));
            when(calendarService.bookMeeting(eq(owner.getId()), any(Meeting.class))).thenReturn(meeting);

            // Act & Assert
            mockMvc.perform(post("/api/calendar/meetings")
                            .param("ownerId", owner.getId().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(meetingDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(meeting.getId()))
                    .andExpect(jsonPath("$.title").value(meeting.getTitle()))
                    .andExpect(jsonPath("$.owner.id").value(owner.getId()))
                    .andExpect(jsonPath("$.owner.name").value(owner.getName()));
        }

        @Test
        @DisplayName("Should return 400 when owner not found")
        void bookMeeting_OwnerNotFound() throws Exception {
            // Arrange
            when(employeeRepository.findById(anyLong())).thenReturn(Optional.empty());

            // Act & Assert
            mockMvc.perform(post("/api/calendar/meetings")
                            .param("ownerId", "999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(meetingDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Owner not found with ID: 999"));
        }

        @Test
        @DisplayName("Should return 400 for invalid meeting data")
        void bookMeeting_InvalidData() throws Exception {
            // Arrange
            meetingDTO.setTitle(null); // Make the DTO invalid

            // Act & Assert
            mockMvc.perform(post("/api/calendar/meetings")
                            .param("ownerId", owner.getId().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(meetingDTO)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Find Free Slots Endpoint Tests")
    class FindFreeSlotsTests {

        @Test
        @DisplayName("Should successfully find free slots")
        void findFreeSlots_Success() throws Exception {
            // Arrange
            List<Map<String, Object>> freeSlots = Arrays.asList(
                    createFreeSlot(baseTime),
                    createFreeSlot(baseTime.plusHours(1))
            );
            when(calendarService.findFreeSlots(eq(1L), eq(2L), any()))
                    .thenReturn(freeSlots);

            // Act & Assert
            mockMvc.perform(get("/api/calendar/free-slots")
                            .param("employee1Id", "1")
                            .param("employee2Id", "2")
                            .param("durationMinutes", "30"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("Should return 400 for negative duration")
        void findFreeSlots_NegativeDuration() throws Exception {
            mockMvc.perform(get("/api/calendar/free-slots")
                            .param("employee1Id", "1")
                            .param("employee2Id", "2")
                            .param("durationMinutes", "-30"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Find Conflicts Endpoint Tests")
    class FindConflictsTests {

        @Test
        @DisplayName("Should successfully find conflicts")
        void findConflicts_Success() throws Exception {
            // Arrange
            when(employeeRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
            when(employeeRepository.findAllById(any())).thenReturn(Arrays.asList(participant1, participant2));
            when(calendarService.findConflicts(any(Meeting.class)))
                    .thenReturn(Collections.singletonList(participant1));

            // Act & Assert
            mockMvc.perform(post("/api/calendar/conflicts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(meetingDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(participant1.getId()));
        }

        @Test
        @DisplayName("Should return 400 for invalid meeting data")
        void findConflicts_InvalidData() throws Exception {
            // Arrange
            meetingDTO.setStartTime(null); // Make the DTO invalid

            // Act & Assert
            mockMvc.perform(post("/api/calendar/conflicts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(meetingDTO)))
                    .andExpect(status().isBadRequest());
        }
    }

    // Helper methods
    private Employee createEmployee(Long id, String name) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setName(name);
        return employee;
    }

    private Meeting createMeeting() {
        Meeting meeting = new Meeting();
        meeting.setId(1L);
        meeting.setTitle("Test Meeting");
        meeting.setStartTime(baseTime.plusHours(1));
        meeting.setEndTime(baseTime.plusHours(2));
        meeting.setOwner(owner);
        meeting.setParticipants(Arrays.asList(participant1, participant2));
        return meeting;
    }

    private MeetingDTO createMeetingDTO() {
        MeetingDTO dto = new MeetingDTO();
        dto.setTitle("Test Meeting");
        dto.setStartTime(baseTime.plusHours(1));
        dto.setEndTime(baseTime.plusHours(2));
        dto.setOwnerId(owner.getId());
        dto.setParticipantIds(Arrays.asList(participant1.getId(), participant2.getId()));
        return dto;
    }

    private Map<String, Object> createFreeSlot(LocalDateTime time) {
        Map<String, Object> slot = new HashMap<>();
        slot.put("date", time.toLocalDate().toString());
        slot.put("startTime", time.toLocalTime().toString());
        slot.put("endTime", time.plusMinutes(30).toLocalTime().toString());
        return slot;
    }
}
