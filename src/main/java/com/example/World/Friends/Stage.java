package com.example.World.Friends;

public enum Stage {
    NONE,
    PENDING,
    ACCEPTED,
    REJECTED;

    public int toInt (){
        return switch (this) {
            case NONE -> 0;
            case PENDING -> 1;
            case ACCEPTED -> 2;
            case REJECTED -> 3;
        };
    }
}
