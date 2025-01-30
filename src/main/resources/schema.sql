/*
CREATE TABLE IF NOT EXISTS Threads (
    TID INT PRIMARY KEY AUTO_INCREMENT, -- Unique identifier for the thread
    Title VARCHAR(255) NOT NULL,        -- Title of the thread, cannot be empty
    Description TEXT,                   -- Optional description of the thread
    Category VARCHAR(100),              -- Category of the thread (e.g., sports, gaming, etc.)
    CreatedAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP -- Timestamp for thread creation
);
*/
