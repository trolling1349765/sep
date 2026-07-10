package fpt.capstone.service;

import fpt.capstone.dto.request.InventoryRequest;
import fpt.capstone.dto.response.InventoryResponse;

import java.util.List;

public interface GoodsInventoryService {
    InventoryResponse createInventory(InventoryRequest request);

    InventoryResponse updateInventory(int id, InventoryRequest request);

    InventoryResponse getInventoryById(int id);

    List<InventoryResponse> getAllInventories();

    List<InventoryResponse> searchByItemName(String keyword);

    InventoryResponse adjustQuantity(int id, int quantityAdjustment, String reason);

    void deleteInventory(int id);
}