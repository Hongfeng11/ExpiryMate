package com.expirymate.config;

import com.expirymate.user.User;
import com.expirymate.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.mode", havingValue = "local-demo", matchIfMissing = true)
public class DemoUserInitializer implements ApplicationRunner {
    private final UserRepository users;
    private final String demoUserId;

    public DemoUserInitializer(UserRepository users, @Value("${app.demo-user-id}") String demoUserId) {
        this.users = users;
        this.demoUserId = demoUserId;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!users.existsById(demoUserId)) {
            users.save(new User(demoUserId, "local-demo@expirymate.invalid", "LOCAL_DEMO_ONLY"));
        }
    }
}
