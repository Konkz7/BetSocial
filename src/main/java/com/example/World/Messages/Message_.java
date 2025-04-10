package com.example.World.Messages;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.lang.NonNull;



public record Message_(
      @Id
      Long mid,       // Primary key
      @NonNull
      Long uid,        // Foreign key to Users table
      Long recipient_id,     // Foreign key to Users table, can be null for group messages
      @NotEmpty
      String description,          // The actual message description
      @NonNull
      Long created_at, // Timestamp of message creation
      Long deleted_at,
      @NonNull
      Long gid,  // Foreign key to Group table
      @NonNull
      Boolean is_read,           // Whether the message has been is_read
      @Version
      Integer m_version // Version number for optimistic locking
){
      @Override
      @NonNull
      public Long created_at() {
            if(deleted_at() != null){
                  if (created_at >= deleted_at){
                        throw new IllegalStateException("created_at cannot be after deleted_at");
                  }
            }

            return created_at;
      }
}
