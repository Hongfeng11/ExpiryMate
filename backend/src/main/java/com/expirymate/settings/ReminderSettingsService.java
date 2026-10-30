package com.expirymate.settings;

import java.time.LocalTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReminderSettingsService {
    private final ReminderPreferenceRepository preferences;

    public ReminderSettingsService(ReminderPreferenceRepository preferences) {
        this.preferences = preferences;
    }

    @Transactional
    public ReminderSettingsResponse get(String userId) {
        return ReminderSettingsResponse.from(preferences.findById(userId)
                .orElseGet(() -> preferences.save(new ReminderPreference(userId))));
    }

    @Transactional
    public ReminderSettingsResponse update(String userId, ReminderSettingsRequest request) {
        List<Integer> days = request.remindBeforeDays();
        LocalTime time = request.preferredTime();
        if (days == null || days.isEmpty() || days.size() > 6
                || days.stream().anyMatch(day -> day == null || day < 0 || day > 30)
                || days.stream().distinct().count() != days.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "提醒天数需为 0 到 30 天之间的不重复数值，且至少选择一项");
        }
        if (time == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择每日检查时间");

        ReminderPreference preference = preferences.findById(userId)
                .orElseGet(() -> new ReminderPreference(userId));
        preference.update(request.enabled(), days.stream().sorted(java.util.Comparator.reverseOrder()).toList(), time);
        return ReminderSettingsResponse.from(preferences.save(preference));
    }
}
