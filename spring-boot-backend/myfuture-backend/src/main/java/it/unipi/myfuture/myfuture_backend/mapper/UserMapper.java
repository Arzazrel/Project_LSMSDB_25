package it.unipi.myfuture.myfuture_backend.mapper;

import it.unipi.myfuture.myfuture_backend.dto.user.*;
import it.unipi.myfuture.myfuture_backend.model.User;

import java.time.Instant;

/**
 * UserMapper Mapper handles conversion between User entity and User DTOs.
 * Used inside service layer to keep business logic clean.
 */
public class UserMapper {

    // -------------------------------------- request → entity --------------------------------------

    /**
     * Convert UserRequestDTO to User entity. Used for user registration.
     *
     * @param dto user request DTO
     * @return user entity
     */
    public static User toEntity(UserRequestDTO dto) {
        User user = new User();

        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        user.setPasswordHash(dto.getPassword());
        user.setCash(0.0);
        user.setBlockedCash(0.0);
        user.setRegistrationDate(Instant.now());
        user.setDeleted(false);
        user.setSuspended(false);

        return user;
    }

    /**
     * Update mutable fields of an existing user. Used for account update.
     *
     * @param user existing user entity
     * @param userRequest user request DTO
     */
    public static void updateEntity(User user, UserRequestDTO userRequest) {
        user.setFirstName(userRequest.getFirstName());
        user.setLastName(userRequest.getLastName());
        user.setPhone(userRequest.getPhone());
    }

    // -------------------------------------- entity → response --------------------------------------

    /**
     * Convert User entity to UserResponseDTO.
     *
     * @param user user entity
     * @return user response DTO
     */
    public static UserResponseDTO toResponseDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();

        dto.setUserId(user.getUserId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setCash(user.getCash());
        dto.setBlockedCash(user.getBlockedCash());

        return dto;
    }
}