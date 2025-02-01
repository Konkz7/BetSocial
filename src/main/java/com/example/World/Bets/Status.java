package com.example.World.Bets;

public enum Status {
    ACTIVE,
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELLED,
    APPROVED;


    public int toInt(){
        return switch (this) {
            case ACTIVE -> 0;
            case PENDING -> 1;
            case ACCEPTED -> 2;
            case REJECTED -> 3;
            case CANCELLED -> 4;
            case APPROVED -> 5;
        };
    }
}
