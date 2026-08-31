package kz.bilimedu.api.auth.dto;

import kz.bilimedu.api.user.UserResponse;

public record TokenPairResponse(String accessToken, String refreshToken, UserResponse user) {
}
