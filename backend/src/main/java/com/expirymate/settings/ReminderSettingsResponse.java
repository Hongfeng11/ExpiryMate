package com.expirymate.settings;

import java.time.LocalTime;
import java.util.List;

public record ReminderSettingsResponse(boolean enabled, List<Integer> remindBeforeDays, LocalTime preferredTime) {
    public static ReminderSettingsResponse from(ReminderPreference preference) {
        return new ReminderSettingsResponse(preference.isEnabled(), preference.getRemindBeforeDays(), preference.getPreferredTime());
    }
}
