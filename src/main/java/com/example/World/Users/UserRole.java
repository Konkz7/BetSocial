package com.example.World.Users;


public enum UserRole {
    USER(0),
    SUPERUSER(1),
    ADMIN(2);

    UserRole(int i) {
    }

    public static int roleToInt(UserRole role){
        return switch (role) {
            case USER -> 0;
            case SUPERUSER -> 1;
            case ADMIN -> 2;
        };
    }

}
