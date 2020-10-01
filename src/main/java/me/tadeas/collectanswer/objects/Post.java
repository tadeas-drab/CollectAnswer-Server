package me.tadeas.collectanswer.objects;

import java.sql.Timestamp;
import java.util.List;

/**
 * This file was created by Tadeáš Drab on 17. 5. 2020
 */
public class Post {

    private int id;
    private int authorId;
    private String authorName;
    private String text;
    private Timestamp date;
    private List<Reaction> reactionList;

    public Post(int id, int authorId, String authorName, String text, Timestamp date, List<Reaction> reactionList) {
        this.id = id;
        this.authorId = authorId;
        this.authorName = authorName;
        this.text = text;
        this.date = date;
        this.reactionList = reactionList;
    }

    public int getId() {
        return id;
    }

    public int getAuthorId() {
        return authorId;
    }

    public String getText() {
        return text;
    }

    public Timestamp getDate() {
        return date;
    }

    public String getAuthorName() {
        return authorName;
    }

    public List<Reaction> getReactionList() {
        return reactionList;
    }
}
