package com.expirymate.item;

import jakarta.validation.Valid;
import java.util.Map;
import com.expirymate.auth.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
    @RequestMapping("/api/v1/lists/{listId}/items")
public class ItemController {
    private final ItemService service;
    public ItemController(ItemService service) { this.service = service; }

    @GetMapping
    public Map<String, Object> list(
            @PathVariable String listId,
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "100") int pageSize,
            @RequestParam(name = "sort_by", defaultValue = "expires_on") String sortBy,
            @RequestParam(name = "sort_order", defaultValue = "asc") String sortOrder,
            @RequestParam(required = false) ItemStatus status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false, name = "q") String keyword) {
        Page<Item> result = service.list(listId, user.getId(), page, pageSize, sortBy, sortOrder, status, category, location, keyword);
        return Map.of("items", result.getContent().stream().map(ItemResponse::from).toList(),
                "page", result.getNumber() + 1, "pageSize", result.getSize(), "total", result.getTotalElements());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemResponse create(@PathVariable String listId, @AuthenticationPrincipal UserPrincipal user,
                               @Valid @RequestBody ItemRequest request) {
        return ItemResponse.from(service.create(listId, user.getId(), request));
    }

    @GetMapping("/{id}")
    public ItemResponse get(@PathVariable String listId, @PathVariable String id,
                            @AuthenticationPrincipal UserPrincipal user) {
        return ItemResponse.from(service.get(listId, user.getId(), id));
    }

    @PatchMapping("/{id}")
    public ItemResponse update(@PathVariable String listId, @PathVariable String id,
                               @AuthenticationPrincipal UserPrincipal user, @RequestBody ItemRequest request) {
        return ItemResponse.from(service.update(listId, user.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String listId, @PathVariable String id,
                       @AuthenticationPrincipal UserPrincipal user) {
        service.delete(listId, user.getId(), id);
    }
}
