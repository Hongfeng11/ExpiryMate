package com.expirymate.shared;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ListAccessService {
    private final SharedListMemberRepository members;

    public ListAccessService(SharedListMemberRepository members) { this.members = members; }

    @Transactional(readOnly = true)
    public SharedListMember requireMember(String listId, String userId) {
        return members.findByListIdAndUserId(listId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "清单不存在或无权访问"));
    }

    @Transactional(readOnly = true)
    public SharedListMember requireEditor(String listId, String userId) {
        SharedListMember member = requireMember(listId, userId);
        if (!member.getRole().equals("OWNER") && !member.getRole().equals("EDITOR"))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前账号只有查看权限");
        return member;
    }

    @Transactional(readOnly = true)
    public SharedListMember requireOwner(String listId, String userId) {
        SharedListMember member = requireMember(listId, userId);
        if (!member.getRole().equals("OWNER"))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有清单管理员可以执行此操作");
        return member;
    }
}
