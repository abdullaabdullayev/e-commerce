package com.example.authservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegisterResponse {
    private Long id;
    private Long accountId;
    private String email;
    private String firstName;
    private String lastName;

}
