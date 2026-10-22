package com.expirymate.item;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ItemService {
    private final ItemRepository items;
    private final String demoUserId;

    public ItemService(ItemRepository items, @Value("${app.demo-user-id}") String demoUserId) {
        this.items = items;
        this.demoUserId = demoUserId;
    }

    @Transactional(readOnly = true)
    public Page<Item> list(int page, int pageSize, String sortBy, String sortOrder, ItemStatus status, String category) {
        String property = switch (sortBy == null ? "expires_on" : sortBy) {
            case "name" -> "name";
            case "created_at" -> "createdAt";
            case "expires_on" -> "expiresOn";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的排序字段");
        };
        Sort.Direction direction = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
        var pageable = PageRequest.of(Math.max(0, page - 1), Math.min(Math.max(1, pageSize), 100), Sort.by(direction, property));
        if (status != null) return items.findAllByUserIdAndDeletedAtIsNullAndStatus(demoUserId, status, pageable);
        if (category != null && !category.isBlank()) return items.findAllByUserIdAndDeletedAtIsNullAndCategory(demoUserId, category, pageable);
        return items.findAllByUserIdAndDeletedAtIsNull(demoUserId, pageable);
    }

    @Transactional
    public Item create(ItemRequest request) {
        validate(request);
        return items.save(Item.create(UUID.randomUUID().toString(), demoUserId, request));
    }

    @Transactional
    public Item update(String id, ItemRequest request) {
        Item item = get(id);
        item.apply(request, true);
        validateItem(item);
        return items.save(item);
    }

    @Transactional
    public void delete(String id) { get(id).softDelete(); }

    @Transactional(readOnly = true)
    public Item get(String id) {
        return items.findByIdAndUserIdAndDeletedAtIsNull(id, demoUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "物品不存在"));
    }

    private void validate(ItemRequest request) {
        if (request.expiresOn() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写到期日期");
        if ((request.shelfLifeValue() == null) != (request.shelfLifeUnit() == null))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "保质期数值和单位需要同时填写");
        validateDates(request.manufacturedOn(), request.purchasedOn(), request.openedOn(), request.expiresOn());
    }
    private void validateDates(Item item) {
        validateDates(item.getManufacturedOn(), item.getPurchasedOn(), item.getOpenedOn(), item.getExpiresOn());
    }
    private void validateItem(Item item) {
        if (item.getName() == null || item.getName().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "物品名称不能为空");
        if (item.getQuantity() == null || item.getQuantity().signum() <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "数量必须大于 0");
        if ((item.getShelfLifeValue() == null) != (item.getShelfLifeUnit() == null)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "保质期数值和单位需要同时填写");
        validateDates(item);
    }
    private void validateDates(java.time.LocalDate manufactured, java.time.LocalDate purchased, java.time.LocalDate opened, java.time.LocalDate expires) {
        if (expires == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写到期日期");
        if (manufactured != null && purchased != null && purchased.isBefore(manufactured)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "购买日期不能早于生产日期");
        if (manufactured != null && expires.isBefore(manufactured)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "到期日期不能早于生产日期");
        if (opened != null && purchased != null && opened.isBefore(purchased)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "开封日期不能早于购买日期");
    }
}
