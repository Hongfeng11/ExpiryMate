package com.expirymate.shared;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SharedListService {
    private final SharedListRepository lists;
    private final SharedListMemberRepository members;
    private final ListInvitationRepository invitations;
    private final ListAccessService access;
    private final SecureRandom random = new SecureRandom();

    public SharedListService(SharedListRepository lists, SharedListMemberRepository members,
                             ListInvitationRepository invitations, ListAccessService access) {
        this.lists = lists;
        this.members = members;
        this.invitations = invitations;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<SharedListResponse> listForUser(String userId) {
        return members.findAllByUserId(userId).stream()
                .map(member -> lists.findById(member.getListId())
                        .map(list -> response(list, member.getRole())).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional
    public SharedListResponse create(String userId, String name) {
        SharedList list = lists.save(new SharedList(UUID.randomUUID().toString(), name.trim(), userId));
        members.save(new SharedListMember(UUID.randomUUID().toString(), list.getId(), userId, "OWNER"));
        return response(list, "OWNER");
    }

    @Transactional
    public InvitationResponse createInvitation(String userId, String listId, String requestedRole) {
        access.requireOwner(listId, userId);
        String role = requestedRole == null || requestedRole.isBlank() ? "EDITOR" : requestedRole;
        if (!role.equals("EDITOR") && !role.equals("VIEWER"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邀请角色无效");
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiry = Instant.now().plus(3, ChronoUnit.DAYS);
        invitations.save(new ListInvitation(UUID.randomUUID().toString(), listId, hash(token), role, userId, expiry));
        return new InvitationResponse(token, role, expiry);
    }

    @Transactional
    public SharedListResponse acceptInvitation(String userId, String token) {
        ListInvitation invitation = invitations.findByTokenHash(hash(token.trim()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "邀请口令无效"));
        if (invitation.getAcceptedAt() != null)
            throw new ResponseStatusException(HttpStatus.GONE, "此邀请口令已使用");
        if (!invitation.getExpiresAt().isAfter(Instant.now()))
            throw new ResponseStatusException(HttpStatus.GONE, "此邀请口令已过期");
        SharedList list = lists.findById(invitation.getListId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.GONE, "清单已不存在"));
        if (members.findByListIdAndUserId(list.getId(), userId).isEmpty()) {
            members.save(new SharedListMember(UUID.randomUUID().toString(), list.getId(), userId, invitation.getRole()));
        }
        invitation.accept(userId);
        return response(list, members.findByListIdAndUserId(list.getId(), userId).orElseThrow().getRole());
    }

    private SharedListResponse response(SharedList list, String role) {
        return new SharedListResponse(list.getId(), list.getName(), role, members.countByListId(list.getId()));
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
