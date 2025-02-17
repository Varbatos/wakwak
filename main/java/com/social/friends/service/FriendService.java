package com.social.friends.service;

import com.social.friends.dto.request.FriendAcceptRequestDto;
import com.social.friends.dto.request.FriendDeleteRequestDto;
import com.social.friends.dto.request.FriendRejectRequestDto;
import com.social.friends.dto.request.FriendRequestDto;
import com.social.friends.dto.response.*;

import java.util.List;

public interface FriendService {
    FriendRequestResponseDto sendFriendRequest(String token, FriendRequestDto request);

    FriendRequestListResponseDto getFriendRequests(String token);

    FriendAcceptResponseDto acceptFriendRequest(String token, FriendAcceptRequestDto request);

    FriendRejectResponseDto rejectFriendRequest(String token, FriendRejectRequestDto request);

    GetFriendsResponseDto getFriends(String token);

    FriendDeleteResponseDto deleteFriend(String token, FriendDeleteRequestDto request);

    FriendSearchResponseDto searchFriends(String token, String nickname);

    SearchFriendStatusResponseDto getFriendStatus(String token, Integer targetId);

    RecommendFriendResponseDto recommendFriends(String token);

    BluetoothSearchResponseDto searchNearbyUsers(String token, List<String> deviceIds);
}
