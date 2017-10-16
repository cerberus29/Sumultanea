package com.example.cj.sumultanea;

public class CharacterPool {
    public static final int CHARACTER_PANDA = 0;
    public static final int CHARACTER_DUEL = 1;
    public static final int CHARACTER_PUMP = 2;
    public static final int CHARACTER_WAR = 3;
    public static final int CHARACTER_DEFAULT = CHARACTER_PUMP;
    public static final Character[] charactersList = {
            new Character("Happy Panda", R.drawable.bluepanda),
            new Character("Some guy from Assassins Creed", R.drawable.duel),
            new Character("Pumpkin Man", R.drawable.pump),
            new Character("Little Warrior", R.drawable.war),
    };
}
