package com.example.World.Bets;

public enum Status {
    ACTIVE(0),
    PENDING(1),
    ACCEPTED(2),
    REJECTED(3),
    CANCELLED(4),
    APPROVED(5)
   ;

    Status(int i) {

    }

    public static int statusToInt(Status status){
        return switch (status) {
            case ACTIVE -> 0;
            case PENDING -> 1;
            case ACCEPTED -> 2;
            case REJECTED -> 3;
            case CANCELLED -> 4;
            case APPROVED -> 5;
        };
    }
}
