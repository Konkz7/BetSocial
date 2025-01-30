package com.example.World.Groups;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.lang.NonNull;

public record GroupDTO(
       @NotEmpty
       String group_name,
       @NonNull
       Integer sort) {
}
