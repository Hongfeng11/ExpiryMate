package com.expirymate.shared;

import com.expirymate.auth.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class SharedListController {
    private final SharedListService service;
    public SharedListController(SharedListService service) { this.service = service; }

    @GetMapping("/lists")
    public List<SharedListResponse> list(@AuthenticationPrincipal UserPrincipal user) {
        return service.listForUser(user.getId());
    }

    @PostMapping("/lists")
    @ResponseStatus(HttpStatus.CREATED)
    public SharedListResponse create(@AuthenticationPrincipal UserPrincipal user,
                                     @Valid @RequestBody CreateListRequest request) {
        return service.create(user.getId(), request.name());
    }

    @PostMapping("/lists/{listId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public InvitationResponse invite(@AuthenticationPrincipal UserPrincipal user, @PathVariable String listId,
                                     @RequestBody(required = false) CreateInvitationRequest request) {
        return service.createInvitation(user.getId(), listId, request == null ? null : request.role());
    }

    @PostMapping("/invitations/accept")
    public SharedListResponse accept(@AuthenticationPrincipal UserPrincipal user,
                                     @Valid @RequestBody AcceptInvitationRequest request) {
        return service.acceptInvitation(user.getId(), request.token());
    }
}
