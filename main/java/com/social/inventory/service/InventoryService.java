package com.social.inventory.service;

import com.social.inventory.dto.response.ClothesInventoryResponseDto;
import com.social.inventory.dto.response.GetClothesInventoryResponseDto;
import com.social.inventory.dto.response.TimeCapsuleInventoryResponseDto;

public interface InventoryService {
    GetClothesInventoryResponseDto getClothesInventory(String token);

    ClothesInventoryResponseDto getClothesDetail(String token, Integer itemId);

    TimeCapsuleInventoryResponseDto getReadTimeCapsules(String token);
}
