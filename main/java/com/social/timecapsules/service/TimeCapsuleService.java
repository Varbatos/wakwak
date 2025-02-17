package com.social.timecapsules.service;

import com.social.timecapsules.dto.request.CreateTimeCapsuleRequestDto;
import com.social.timecapsules.dto.request.GetTimeCapsuleMapRequestDto;
import com.social.timecapsules.dto.request.TimeCapsuleDeleteRequestDto;
import com.social.timecapsules.dto.response.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TimeCapsuleService {
    public CreateTimeCapsuleResponseDto createTimeCapsule(String token, CreateTimeCapsuleRequestDto request, List<MultipartFile> files);

    public GetTimeCapsuleMapResponseDto getTimeCapsulesOnMap(String token, GetTimeCapsuleMapRequestDto request);

    GetTimeCapsuleMapListResponseDto getAccessibleTimeCapsules(String token);

    TimeCapsuleDetailResponseDto getTimeCapsuleDetail(String token, Integer capsuleId);

    TimeCapsuleDeleteResponseDto deleteTimeCapsule(String token, TimeCapsuleDeleteRequestDto request);

    TimeCapsuleCollectResponseDto collectTimeCapsules(String token);

}