package com.example.user.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProfileRequest {

    private Long accountId;
    private String email;
    private String firstName;
    private String lastName;
}
