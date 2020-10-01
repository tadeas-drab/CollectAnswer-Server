package me.tadeas.collectanswer.objects;

/**
 * This file was created by Tadeáš Drab on 17. 5. 2020
 */
public class User {

    private int id;
    private String token;

    public User(int id, String token) {
        this.id = id;
        this.token = token;
    }

    public int getId() {
        return id;
    }

    public String getToken() {
        return token;
    }
}
