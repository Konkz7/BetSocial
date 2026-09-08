package com.example.World.Groups;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * The request body for renaming a group.
 *
 * A body rather than a query parameter, for the same reason creation uses one: a
 * group name is free text that may contain spaces, ampersands or hashes, and
 * every one of those has to survive a round trip intact.
 */
public record GroupRenameDTO(

        @NotEmpty
        @Size(max = 100)
        String group_name
) {
}
