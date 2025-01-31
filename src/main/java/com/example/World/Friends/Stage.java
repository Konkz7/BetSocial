package com.example.World.Friends;

public enum Stage {
    NONE(0),
    PENDING(1),
    ACCEPTED(2),
    REJECTED(3);

    private final int stage;

    Stage(int stage) {
        this.stage = stage;
    }

    public int toInt (){
        return switch (this) {
            case NONE -> 0;
            case PENDING -> 1;
            case ACCEPTED -> 2;
            case REJECTED -> 3;
        };
    }
}
