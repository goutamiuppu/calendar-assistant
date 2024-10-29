package org.assignment.repository;

import org.assignment.domainmodel.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    @Query("SELECT m FROM Meeting m WHERE (m.owner.id = :employeeId OR :employeeId IN (SELECT p.id FROM m.participants p)) AND " +
            "((m.startTime <= :end AND m.endTime > :start) OR " +
            "(m.startTime < :end AND m.endTime >= :end) OR " +
            "(m.startTime >= :start AND m.endTime <= :end))")
    List<Meeting> findOverlappingMeetings(
            @Param("employeeId") Long employeeId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}