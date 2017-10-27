package com.example.cj.sumultanea;

public class CharacterPool {
    public static final Character[] charactersList = {
            new Character(
                    R.string.bluepanda_name,
                    R.drawable.bluepanda,
                    R.drawable.bluepanda_hurt,
                    R.drawable.bluepanda_death,
                    R.drawable.bluepanda_attack,
                    R.string.Panda_Lore,
                    100, 5, 0, 10),
            new Character(
                    R.string.duel_name,
                    R.drawable.duel,
                    R.drawable.duel_hurt,
                    R.drawable.duel_death,
                    R.drawable.duel_attack,
                    R.string.Duelist_Lore,
                    80, 10, 20, 7),
            new Character(
                    R.string.pump_name,
                    R.drawable.pump,
                    R.drawable.pump_hurt,
                    R.drawable.pump_death,
                    R.drawable.pump_attack,
                    R.string.Scarecrow_Lore,
                    70, 10, 15, 10),
            new Character(R.string.war_name,
                    R.drawable.war,
                    R.drawable.war_hurt,
                    R.drawable.war_death,
                    R.drawable.war_attack,
                    R.string.Warrior_Lore,
                    100, 5, 10, 13),
    };
}