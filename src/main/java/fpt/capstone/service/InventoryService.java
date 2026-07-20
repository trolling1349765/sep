package fpt.capstone.service;

import fpt.capstone.dto.request.StockAdjustmentRequest;
import fpt.capstone.dto.response.InventoryBalanceResponse;
import fpt.capstone.dto.response.InventoryTransactionResponse;
import fpt.capstone.dto.response.PageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface InventoryService {

    PageResponse<InventoryBalanceResponse> listBalances(int page, int size, String sort, String dir);

    PageResponse<InventoryTransactionResponse> transactions(String itemId, int page, int size);

    InventoryTransactionResponse adjust(String itemId, StockAdjustmentRequest request, List<MultipartFile> files);
}
