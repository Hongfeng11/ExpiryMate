package com.expirymate.item;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/items")
public class ItemController {
    private final ItemService service;
    public ItemController(ItemService service) { this.service = service; }

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "100") int pageSize,
            @RequestParam(name = "sort_by", defaultValue = "expires_on") String sortBy,
            @RequestParam(name = "sort_order", defaultValue = "asc") String sortOrder,
            @RequestParam(required = false) ItemStatus status,
            @RequestParam(required = false) String category) {
        Page<Item> result = service.list(page, pageSize, sortBy, sortOrder, status, category);
        return Map.of("items", result.getContent().stream().map(ItemResponse::from).toList(),
                "page", result.getNumber() + 1, "pageSize", result.getSize(), "total", result.getTotalElements());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemResponse create(@Valid @RequestBody ItemRequest request) {
        return ItemResponse.from(service.create(request));
    }

    @GetMapping("/{id}")
    public ItemResponse get(@PathVariable String id) { return ItemResponse.from(service.get(id)); }

    @PatchMapping("/{id}")
    public ItemResponse update(@PathVariable String id, @RequestBody ItemRequest request) {
        return ItemResponse.from(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) { service.delete(id); }
}
