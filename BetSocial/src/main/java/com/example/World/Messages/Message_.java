package com.example.World.Messages;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.lang.NonNull;



public record Message_(
      @Id
      Long mid,       // Primary key
      @NonNull
      Long uid,        // Foreign key to Users table
      // recipient_id is gone: who receives a message is every other member of its
      // conversation, derived from the membership rows. Naming one recipient could
      // not describe a group, and after delivery moved to membership it described
      // nothing at all. The column remains, unread, pending a drop.
      @NotEmpty
      String description,// The actual message description
      @NotNull
      Integer media_type,
      @NonNull
      Long created_at, // Timestamp of message creation
      Long deleted_at,
      @NonNull
      Long gid,  // Foreign key to Group table
      // is_read is gone from the row. Whether a message has been read is a fact
      // about each reader, not about the message, and lives on their membership
      // row as last_read_timestamp - a single boolean here could not express
      // "read by 3 of 5". MessageView derives it for the client. The column
      // remains, unwritten, pending a drop.
      @Version
      Integer m_version // Version number for optimistic locking

) {
}
