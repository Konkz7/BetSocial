package com.example.World.Groups;

public enum Sort {
    PRIVATE(0),
    GROUP(1);

    Sort(int i) {

    }

    public int sortToInt(){
        return switch (this) {
            case PRIVATE -> 0;
            case GROUP -> 1;
        };
    }
}