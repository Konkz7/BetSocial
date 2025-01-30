package com.example.World.Groups;

public enum Sort {
    PRIVATE(0),
    GROUP(1);

    Sort(int i) {

    }

    public static int sortToInt(Sort sort){
        return switch (sort) {
            case PRIVATE -> 0;
            case GROUP -> 1;
        };
    }
}