package com.expirymate.settings;

import java.time.LocalTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReminderSettingsService {
    private final ReminderPreferenceRepository preferences;
    private final String demoUserId;

    public ReminderSettingsService(ReminderPreferenceRepository preferences,
                                   @Value("${app.demo-user-id}") String demoUserId) {
        this.preferences = preferences;
        this.demoUserId = demoUserId;
    }

    @Transactional
    public ReminderSettingsResponse get() {
        return ReminderSettingsResponse.from(preferences.findById(demoUserId)
                .orElseGet(() -> preferences.save(new ReminderPreference(demoUserId))));
    }

    @Transactional
    public ReminderSettingsResponse update(ReminderSettingsRequest request) {
        List<Integer> days = request.remindBeforeDays();
        LocalTime time = request.preferredTime();
        if (days == null || days.isEmpty() || days.size() > 6
                || days.stream().anyMatch(day -> day == null || day < 0 || day > 30)
                || days.stream().distinct().count() != days.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "提醒天数需为 0 到 30 天之间的不重复数值，且至少选择一项");
        }
        if (time == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择每日检查时间");

        ReminderPreference preference = preferences.findById(demoUserId)
                .orElseGet(() -> new ReminderPreference(demoUserId));
        preference.update(request.enabled(), days.stream().sorted(java.util.Comparator.reverseOrder()).toList(), time);
        return ReminderSettingsResponse.from(preferences.save(preference));
    }
}
