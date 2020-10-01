package me.tadeas.collectanswer.types;

/**
 * This file was created by Tadeáš Drab on 17. 5. 2020
 */
public enum ReactionType {

    LIKE(0);

    private int id;

    ReactionType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
