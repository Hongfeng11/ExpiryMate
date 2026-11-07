package com.expirymate.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.expirymate.auth.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {
    private final AiService service;
    public AiController(AiService service) { this.service = service; }

    @PostMapping("/chat")
    public JsonNode chat(@AuthenticationPrincipal UserPrincipal user, @Valid @RequestBody ChatRequest request) {
        return service.chat(request.messages().stream().map(message -> Map.of("role", message.role(), "content", message.content())).toList(), request.foods());
    }

    @PostMapping("/recognize")
    public JsonNode recognize(@AuthenticationPrincipal UserPrincipal user, @Valid @RequestBody RecognizeRequest request) {
        return service.recognize(request.imageDataUrl());
    }

    public record ChatRequest(@NotEmpty @Size(max = 30) List<@Valid Message> messages,
                              @Size(max = 300) List<Map<String, Object>> foods) {}
    public record Message(@NotBlank @Pattern(regexp = "user|assistant") String role,
                          @NotBlank @Size(max = 4000) String content) {}
    public record RecognizeRequest(@NotBlank @Size(max = 16_800_000) String imageDataUrl) {}
}
