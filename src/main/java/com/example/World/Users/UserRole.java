package com.example.World.Users;


public enum UserRole {
    USER,
    SUPERUSER,
    ADMIN;


    public int toInt(){
        return switch (this) {
            case USER -> 0;
            case SUPERUSER -> 1;
            case ADMIN -> 2;
        };
    }

}
