package org.assignment.domainmodel;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
public class Meeting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @JsonBackReference
    @ManyToOne
    private Employee owner;

    @ManyToMany
    private List<Employee> participants;

    @Override
    public String toString() {
        return "Meeting{id=" + id + ", title='" + title + "', startTime=" + startTime + ", endTime=" + endTime + ", ownerId=" + (owner != null ? owner.getId() : null) + "}";
    }
}
