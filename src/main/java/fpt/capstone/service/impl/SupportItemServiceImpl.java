package fpt.capstone.service.impl;

import fpt.capstone.annotation.Auditable;
import fpt.capstone.dto.request.SupportItemRequest;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.dto.response.SupportItemResponse;
import fpt.capstone.entity.GoodsInventory;
import fpt.capstone.entity.SupportItem;
import fpt.capstone.enums.Action;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.enums.Table;
import fpt.capstone.repository.GoodsInventoryRepository;
import fpt.capstone.repository.SupportItemRepository;
import fpt.capstone.service.SupportItemService;
import fpt.capstone.util.CodeGenerator;
import fpt.capstone.util.SecurityUtil;
import fpt.capstone.util.TextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupportItemServiceImpl implements SupportItemService {

    private static final int MAX_PAGE_SIZE = 100;

    private final SupportItemRepository supportItemRepository;
    private final GoodsInventoryRepository goodsInventoryRepository;
    private final CodeGenerator codeGenerator;
    private final SecurityUtil securityUtil;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SupportItemResponse> search(int page, int size, String q, String sort, String dir) {
        if (page < 0 || size < 1) {
            throw badRequest(ErrorCode.ARGUMENT_INVALID);
        }
        if (!"name".equals(sort)) {
            throw badRequest(ErrorCode.ARGUMENT_INVALID);
        }
        Sort.Direction direction;
        if ("asc".equalsIgnoreCase(dir)) {
            direction = Sort.Direction.ASC;
        } else if ("desc".equalsIgnoreCase(dir)) {
            direction = Sort.Direction.DESC;
        } else {
            throw badRequest(ErrorCode.ARGUMENT_INVALID);
        }
        String query = (q == null || q.isBlank()) ? null : q.trim();
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE),
                Sort.by(direction, "name").and(Sort.by(Sort.Direction.ASC, "id")));

        Page<SupportItem> items = supportItemRepository.search(query, pageable);
        // Batch-load balances for the page rather than N+1.
        Map<String, GoodsInventory> balances = goodsInventoryRepository
                .findByItemIdIn(items.getContent().stream().map(SupportItem::getId).toList())
                .stream()
                .collect(Collectors.toMap(g -> g.getItem().getId(), Function.identity()));
        return PageResponse.from(items.map(item -> {
            GoodsInventory g = balances.get(item.getId());
            return SupportItemResponse.from(item,
                    g != null ? g.getQuantityOnHand() : 0,
                    g != null ? g.getReservedQuantity() : 0);
        }));
    }

    @Override
    @Transactional
    @Auditable(action = Action.SUPPORT_ITEM_CREATE, entity = Table.SUPPORT_ITEM)
    public SupportItemResponse quickCreate(SupportItemRequest request) {
        String normalized = TextNormalizer.normalize(request.getName());
        if (supportItemRepository.existsActiveByNormalizedName(normalized)) {
            throw badRequest(ErrorCode.ITEM_DUPLICATED);
        }
        String actor = securityUtil.getCurrentUserId();
        SupportItem item = SupportItem.builder()
                .code(codeGenerator.next("VP"))
                .name(request.getName())
                .normalizedName(normalized)
                .unit(request.getUnit())
                .description(request.getDescription())
                .createAt(LocalDate.now())
                .createBy(actor)
                .build();
        item = supportItemRepository.save(item);
        // Inventory row is created lazily on first receipt post.
        return SupportItemResponse.from(item, 0, 0);
    }

    private static ResponseStatusException badRequest(ErrorCode code) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, code.name());
    }
}
