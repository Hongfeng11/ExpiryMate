package com.expirymate.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "reminder_preferences")
public class ReminderPreference {
    @Id
    @Column(name = "user_id", length = 36, columnDefinition = "char(36)")
    private String userId;

    @Column(nullable = false)
    private boolean enabled = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "remind_before_days", nullable = false, columnDefinition = "json")
    private List<Integer> remindBeforeDays = List.of(7, 3, 1, 0);

    @Column(name = "preferred_time", nullable = false)
    private LocalTime preferredTime = LocalTime.of(9, 0);

    protected ReminderPreference() {}

    public ReminderPreference(String userId) { this.userId = userId; }

    public void update(boolean enabled, List<Integer> days, LocalTime time) {
        this.enabled = enabled;
        this.remindBeforeDays = List.copyOf(days);
        this.preferredTime = time;
    }

    public String getUserId() { return userId; }
    public boolean isEnabled() { return enabled; }
    public List<Integer> getRemindBeforeDays() { return remindBeforeDays; }
    public LocalTime getPreferredTime() { return preferredTime; }
}
