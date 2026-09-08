package com.example.World.Groups;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * The request body for creating a group.
 *
 * The creator is not listed in {@code members} - they are taken from the session
 * and added as the first administrator, so a client cannot create a group it does
 * not belong to.
 */
public record GroupCreateDTO(

        @NotEmpty
        @Size(max = 100)
        String group_name,

        List<Long> members
) {
}
