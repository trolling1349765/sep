package fpt.capstone.service.impl;

import fpt.capstone.dto.request.InventoryRequest;
import fpt.capstone.dto.response.InventoryResponse;
import fpt.capstone.entity.GoodsInventory;
import fpt.capstone.entity.Sponsor;
import fpt.capstone.repository.GoodsInventoryRepository;
import fpt.capstone.repository.SponsorRepository;
import fpt.capstone.service.GoodsInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoodsInventoryServiceImpl implements GoodsInventoryService {

    private final GoodsInventoryRepository inventoryRepository;
    private final SponsorRepository sponsorRepository;

    @Override
    @Transactional
    public InventoryResponse createInventory(InventoryRequest request) {
        Sponsor sponsor = null;
        if (request.getSponsorId() != null) {
            sponsor = sponsorRepository.findById(request.getSponsorId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nhà tài trợ"));
        }

        GoodsInventory item = GoodsInventory.builder()
                .sponsor(sponsor)
                .itemName(request.getItemName())
                .unit(request.getUnit())
                .quantity(request.getQuantity())
                .reservedQuantity(0)
                .conditionStatus(request.getConditionStatus() != null ? request.getConditionStatus() : "NEW")
                .location(request.getLocation())
                .receiptDate(request.getReceiptDate() != null ? request.getReceiptDate() : LocalDate.now())
                .status(request.getQuantity() > 0 ? "AVAILABLE" : "DEPLETED")
                .notes(request.getNotes())
                .createAt(LocalDate.now())
                .build();
        item = inventoryRepository.save(item);
        return toResponse(item);
    }

    @Override
    @Transactional
    public InventoryResponse updateInventory(int id, InventoryRequest request) {
        GoodsInventory item = inventoryRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy vật phẩm trong kho"));

        Sponsor sponsor = null;
        if (request.getSponsorId() != null) {
            sponsor = sponsorRepository.findById(request.getSponsorId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nhà tài trợ"));
        }

        item.setSponsor(sponsor);
        item.setItemName(request.getItemName());
        item.setUnit(request.getUnit());
        item.setQuantity(request.getQuantity());
        item.setConditionStatus(request.getConditionStatus());
        item.setLocation(request.getLocation());
        item.setReceiptDate(request.getReceiptDate());
        item.setNotes(request.getNotes());
        item.setUpdateAt(LocalDate.now());

        item = inventoryRepository.save(item);
        return toResponse(item);
    }

    @Override
    public InventoryResponse getInventoryById(int id) {
        GoodsInventory item = inventoryRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy vật phẩm trong kho"));
        return toResponse(item);
    }

    @Override
    public List<InventoryResponse> getAllInventories() {
        return inventoryRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<InventoryResponse> searchByItemName(String keyword) {
        return inventoryRepository.findByItemNameContainingIgnoreCase(keyword).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public InventoryResponse adjustQuantity(int id, int quantityAdjustment, String reason) {
        GoodsInventory item = inventoryRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy vật phẩm trong kho"));

        int newQuantity = item.getQuantity() + quantityAdjustment;
        if (newQuantity < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng tồn kho không đủ");
        }

        item.setQuantity(newQuantity);
        if (newQuantity == 0) {
            item.setStatus("DEPLETED");
        } else {
            item.setStatus("AVAILABLE");
        }
        item.setUpdateAt(LocalDate.now());

        item = inventoryRepository.save(item);
        return toResponse(item);
    }

    @Override
    @Transactional
    public void deleteInventory(int id) {
        GoodsInventory item = inventoryRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy vật phẩm trong kho"));
        item.setIsDelete(true);
        item.setUpdateAt(LocalDate.now());
        inventoryRepository.save(item);
    }

    private InventoryResponse toResponse(GoodsInventory item) {
        return InventoryResponse.builder()
                .id(item.getId())
                .sponsorId(item.getSponsor() != null ? item.getSponsor().getId() : null)
                .sponsorName(item.getSponsor() != null ? item.getSponsor().getName() : null)
                .itemName(item.getItemName())
                .unit(item.getUnit())
                .quantity(item.getQuantity())
                .reservedQuantity(item.getReservedQuantity())
                .availableQuantity(item.getQuantity() - item.getReservedQuantity())
                .conditionStatus(item.getConditionStatus())
                .location(item.getLocation())
                .receiptDate(item.getReceiptDate())
                .status(item.getStatus())
                .notes(item.getNotes())
                .createAt(item.getCreateAt())
                .build();
    }
}