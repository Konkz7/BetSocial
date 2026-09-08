package com.example.World.Messages;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.lang.NonNull;



public record MessageDTO(

        // No recipient: a message goes to its conversation, and who that reaches is
        // read from the membership rows. The client used to name a recipient here
        // and the server used to believe it.
        Long gid,
        @NotEmpty
        String description ,        // The actual message description
        @NonNull
        Integer media_type
) {
}
