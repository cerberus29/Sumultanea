package com.example.cj.sumultanea;

public class CharacterPool {
    public static final int CHARACTER_PANDA = 0;
    public static final int CHARACTER_DUEL = 1;
    public static final int CHARACTER_PUMP = 2;
    public static final int CHARACTER_WAR = 3;
    public static final int CHARACTER_DEFAULT = CHARACTER_PUMP;
    public static final Character[] charactersList = {
            new Character("Happy Panda", R.drawable.bluepanda, R.drawable.bluepanda_hurt, R.drawable.bluepanda_death, R.drawable.bluepanda_attack),
            new Character("Some guy from Assassins Creed", R.drawable.duel, R.drawable.duel_hurt, R.drawable.duel_death, R.drawable.duel_attack),
            new Character("Pumpkin Man", R.drawable.pump, R.drawable.pump_hurt, R.drawable.pump_death, R.drawable.pump_attack),
            new Character("Little Warrior", R.drawable.war, R.drawable.war_hurt, R.drawable.war_death, R.drawable.war_attack),
    };
}
