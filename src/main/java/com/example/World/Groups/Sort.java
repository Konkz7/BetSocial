package com.example.World.Groups;

public enum Sort {
    PRIVATE,
    GROUP;



    public int sortToInt(){
        return switch (this) {
            case PRIVATE -> 0;
            case GROUP -> 1;
        };
    }
}