package com.expirymate.item;

import java.time.LocalDate;
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
    public Page<Item> list(int page, int pageSize, String sortBy, String sortOrder, ItemStatus status,
                           String category, String location, String keyword) {
        String property = switch (sortBy == null ? "expires_on" : sortBy) {
            case "name" -> "name";
            case "created_at" -> "createdAt";
            case "expires_on" -> "expiresOn";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的排序字段");
        };
        Sort.Direction direction = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
        var pageable = PageRequest.of(Math.max(0, page - 1), Math.min(Math.max(1, pageSize), 100), Sort.by(direction, property));
        return items.searchItems(demoUserId, status, blankToNull(category), blankToNull(location), blankToNull(keyword), pageable);
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    @Transactional
    public Item create(ItemRequest request) {
        Item item = Item.create(UUID.randomUUID().toString(), demoUserId, request, request.expiresOn());
        item.setExpiresOn(resolveEffectiveExpiry(item, request.expiresOn()));
        validateItem(item);
        validateDates(item.getManufacturedOn(), item.getPurchasedOn(), item.getOpenedOn(), item.getExpiresOn());
        return items.save(item);
    }

    @Transactional
    public Item update(String id, ItemRequest request) {
        Item item = get(id);
        item.apply(request, true);
        if (request.status() == ItemStatus.used_up || request.status() == ItemStatus.discarded)
            item.setQuantity(java.math.BigDecimal.ZERO);
        boolean expiryInputsChanged = request.expiresOn() != null || request.packageExpiresOn() != null
                || request.clearPackageExpiresOn() == Boolean.TRUE || request.manufacturedOn() != null
                || request.shelfLifeValue() != null || request.shelfLifeUnit() != null
                || request.clearShelfLife() == Boolean.TRUE || request.openedOn() != null
                || request.clearOpenedOn() == Boolean.TRUE || request.openedShelfLifeValue() != null
                || request.openedShelfLifeUnit() != null || request.clearOpenedShelfLife() == Boolean.TRUE;
        if (expiryInputsChanged) item.setExpiresOn(resolveEffectiveExpiry(item, request.expiresOn()));
        validateItem(item);
        validateDates(item.getManufacturedOn(), item.getPurchasedOn(), item.getOpenedOn(), item.getExpiresOn());
        return items.save(item);
    }

    @Transactional
    public void delete(String id) { get(id).softDelete(); }

    @Transactional(readOnly = true)
    public Item get(String id) {
        return items.findByIdAndUserIdAndDeletedAtIsNull(id, demoUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "物品不存在"));
    }

    private LocalDate resolveEffectiveExpiry(Item item, LocalDate legacyExpiry) {
        LocalDate baseExpiry = item.getPackageExpiresOn();
        if (baseExpiry == null && item.getManufacturedOn() != null
                && item.getShelfLifeValue() != null && item.getShelfLifeUnit() != null) {
            baseExpiry = addShelfLife(item.getManufacturedOn(), item.getShelfLifeValue(), item.getShelfLifeUnit());
        }

        LocalDate openedExpiry = null;
        if (item.getOpenedOn() != null && item.getOpenedShelfLifeValue() != null && item.getOpenedShelfLifeUnit() != null) {
            openedExpiry = addShelfLife(item.getOpenedOn(), item.getOpenedShelfLifeValue(), item.getOpenedShelfLifeUnit());
        }

        if (baseExpiry == null) baseExpiry = openedExpiry;
        if (baseExpiry == null) baseExpiry = legacyExpiry;
        if (baseExpiry == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "请填写包装到期日、生产日期和保质期，或开封日期和开封后期限");
        }
        return openedExpiry != null && openedExpiry.isBefore(baseExpiry) ? openedExpiry : baseExpiry;
    }

    private LocalDate addShelfLife(LocalDate start, Integer value, ShelfLifeUnit unit) {
        if (value < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "保质期必须大于 0");
        return switch (unit) {
            case days -> start.plusDays(value);
            case months -> start.plusMonths(value);
            case years -> start.plusYears(value);
        };
    }

    private void validateItem(Item item) {
        if (item.getName() == null || item.getName().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "物品名称不能为空");
        if (item.getQuantity() == null || item.getQuantity().signum() < 0
                || (item.getStatus() == ItemStatus.active && item.getQuantity().signum() == 0))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "使用中的物品数量必须大于 0；已用完或已丢弃的物品数量可以为 0");
        if ((item.getShelfLifeValue() == null) != (item.getShelfLifeUnit() == null))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "保质期数值和单位需要同时填写");
        if ((item.getOpenedShelfLifeValue() == null) != (item.getOpenedShelfLifeUnit() == null))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "开封后期限数值和单位需要同时填写");
        if (item.getOpenedShelfLifeValue() != null && item.getOpenedOn() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "填写开封后期限时，请同时填写开封日期");
        if (item.getExpiresOn() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请提供可计算到期日的信息");
    }

    private void validateDates(LocalDate manufactured, LocalDate purchased, LocalDate opened, LocalDate expires) {
        if (manufactured != null && purchased != null && purchased.isBefore(manufactured))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "购买日期不能早于生产日期");
        if (manufactured != null && expires.isBefore(manufactured))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "到期日期不能早于生产日期");
        if (opened != null && purchased != null && opened.isBefore(purchased))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "开封日期不能早于购买日期");
        if (opened != null && manufactured != null && opened.isBefore(manufactured))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "开封日期不能早于生产日期");
    }
}
