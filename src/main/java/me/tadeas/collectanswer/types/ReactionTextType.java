package me.tadeas.collectanswer.types;

/**
 * This file was created by Tadeáš Drab on 17. 5. 2020
 */
public enum ReactionTextType {

    POST(0),
    COMMENT(1);

    private int id;

    ReactionTextType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static ReactionTextType getType(int id) {
        for(ReactionTextType reactionTextType : values()) {
            if(reactionTextType.getId() == id)
                return reactionTextType;
        }
        return null;
    }
}
