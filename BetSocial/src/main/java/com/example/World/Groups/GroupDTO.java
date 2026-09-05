package com.example.World.Groups;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.lang.NonNull;

public record GroupDTO(

       @NonNull
       Long uid,
       @NonNull
       Long other_uid
) {
}
