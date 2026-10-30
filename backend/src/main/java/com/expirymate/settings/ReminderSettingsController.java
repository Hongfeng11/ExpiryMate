package com.expirymate.settings;

import com.expirymate.auth.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings/reminders")
public class ReminderSettingsController {
    private final ReminderSettingsService service;

    public ReminderSettingsController(ReminderSettingsService service) { this.service = service; }

    @GetMapping
    public ReminderSettingsResponse get(@AuthenticationPrincipal UserPrincipal user) { return service.get(user.getId()); }

    @PutMapping
    public ReminderSettingsResponse update(@AuthenticationPrincipal UserPrincipal user,
                                           @RequestBody ReminderSettingsRequest request) {
        return service.update(user.getId(), request);
    }
}
