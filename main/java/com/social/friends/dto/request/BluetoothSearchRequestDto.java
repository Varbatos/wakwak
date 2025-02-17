package com.social.friends.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class BluetoothSearchRequestDto {
    private List<String> deviceIds;
}
