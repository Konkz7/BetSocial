package com.example.World.Messages;

public enum MediaType {
    TEXT,
    IMAGE,
    VIDEO;


    public int toInt(){
        return switch (this) {
            case TEXT -> 0;
            case IMAGE -> 1;
            case VIDEO -> 2;
        };
    }
}
