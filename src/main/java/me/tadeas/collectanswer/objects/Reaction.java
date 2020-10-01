package me.tadeas.collectanswer.objects;

import java.sql.Timestamp;

/**
 * This file was created by Tadeáš Drab on 17. 5. 2020
 */
public class Reaction {

    private int id;
    private int accountId;
    private int textId;
    private int type;
    private boolean value;
    private int textType;
    private Timestamp date;

    public Reaction(int id, int accountId, int textId, int type, boolean value, int textType, Timestamp date) {
        this.id = id;
        this.accountId = accountId;
        this.textId = textId;
        this.type = type;
        this.value = value;
        this.textType = textType;
        this.date = date;
    }

    public int getId() {
        return id;
    }

    public int getAccountId() {
        return accountId;
    }

    public int getTextId() {
        return textId;
    }

    public int getType() {
        return type;
    }

    public boolean getValue() {
        return value;
    }

    public int getTextType() {
        return textType;
    }

    public Timestamp getDate() {
        return date;
    }
}
