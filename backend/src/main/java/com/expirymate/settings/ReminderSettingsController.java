package com.expirymate.settings;

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
    public ReminderSettingsResponse get() { return service.get(); }

    @PutMapping
    public ReminderSettingsResponse update(@RequestBody ReminderSettingsRequest request) {
        return service.update(request);
    }
}
