package com.expirymate.settings;

import java.time.LocalTime;
import java.util.List;

public record ReminderSettingsRequest(boolean enabled, List<Integer> remindBeforeDays, LocalTime preferredTime) {}
