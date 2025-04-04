package com.example.World.Friends;

public enum Stage {
    PENDING,
    ACCEPTED,
    BLOCKED;

    public int toInt (){
        return switch (this) {
            case PENDING -> 0;
            case ACCEPTED -> 1;
            case BLOCKED -> 2;
        };
    }
}
