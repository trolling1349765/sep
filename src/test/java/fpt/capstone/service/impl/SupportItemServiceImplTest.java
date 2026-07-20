package fpt.capstone.service.impl;

import fpt.capstone.dto.request.SupportItemRequest;
import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.dto.response.SupportItemResponse;
import fpt.capstone.entity.GoodsInventory;
import fpt.capstone.entity.SupportItem;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.repository.GoodsInventoryRepository;
import fpt.capstone.repository.SupportItemRepository;
import fpt.capstone.util.CodeGenerator;
import fpt.capstone.util.SecurityUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupportItemServiceImplTest {

    @Mock private SupportItemRepository supportItemRepository;
    @Mock private GoodsInventoryRepository goodsInventoryRepository;
    @Mock private CodeGenerator codeGenerator;
    @Mock private SecurityUtil securityUtil;

    @InjectMocks private SupportItemServiceImpl service;

    @Test
    void quickCreate_duplicateNormalizedName_shouldThrow10046() {
        when(supportItemRepository.existsActiveByNormalizedName("gao te")).thenReturn(true);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.quickCreate(SupportItemRequest.builder().name("Gạo Tẻ").unit("kg").build()));
        assertEquals(ErrorCode.ITEM_DUPLICATED.name(), ex.getReason());
        verify(supportItemRepository, never()).save(any());
    }

    @Test
    void quickCreate_valid_shouldGenerateCodeAndNormalize() {
        when(supportItemRepository.existsActiveByNormalizedName(any())).thenReturn(false);
        when(codeGenerator.next("VP")).thenReturn("VP-2026-003");
        when(securityUtil.getCurrentUserId()).thenReturn("user-1");
        when(supportItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0, SupportItem.class));

        SupportItemResponse res = service.quickCreate(
                SupportItemRequest.builder().name("Gạo Tẻ").unit("kg").description("d").build());

        ArgumentCaptor<SupportItem> captor = ArgumentCaptor.forClass(SupportItem.class);
        verify(supportItemRepository).save(captor.capture());
        assertEquals("VP-2026-003", captor.getValue().getCode());
        assertEquals("gao te", captor.getValue().getNormalizedName());
        assertEquals(0, res.getAvailable());
    }

    @Test
    void search_joinsBalanceAndComputesAvailable() {
        SupportItem item = SupportItem.builder().id("i-1").code("VP-2026-001").name("Gao").unit("kg").build();
        GoodsInventory bal = GoodsInventory.builder().item(item).quantityOnHand(100).reservedQuantity(20).build();
        when(supportItemRepository.search(any(), any())).thenReturn(new PageImpl<>(List.of(item)));
        when(goodsInventoryRepository.findByItemIdIn(any())).thenReturn(List.of(bal));

        PageResponse<SupportItemResponse> page = service.search(0, 20, null, "name", "asc");

        assertEquals(1, page.getItems().size());
        assertEquals(100, page.getItems().get(0).getQuantityOnHand());
        assertEquals(80, page.getItems().get(0).getAvailable());
    }

    @Test
    void search_badSort_shouldThrowArgumentInvalid() {
        assertThrows(ResponseStatusException.class, () -> service.search(0, 20, null, "price", "asc"));
    }
}
