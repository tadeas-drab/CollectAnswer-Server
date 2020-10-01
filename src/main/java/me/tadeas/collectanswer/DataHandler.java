package me.tadeas.collectanswer;

import me.tadeas.collectanswer.objects.Comment;
import me.tadeas.collectanswer.objects.Post;
import me.tadeas.collectanswer.objects.Reaction;
import me.tadeas.collectanswer.objects.User;
import me.tadeas.collectanswer.types.ReactionTextType;
import me.tadeas.collectanswer.types.ReactionType;

import java.security.SecureRandom;
import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * This file was created by Tadeáš Drab on 7. 5. 2020
 */
public class DataHandler {

    public DBResponseType tryRegisterUsername(String username, String password) {
        String SQL_QUERY_SELECT = "SELECT * FROM accounts WHERE username = ?";
        String SQL_QUERY_INSERT = "INSERT INTO accounts (username, password) VALUES (?, PASSWORD(?))";

        try (Connection connection = DataSource.getConnection();
             PreparedStatement pst = connection.prepareStatement( SQL_QUERY_SELECT );) {
            pst.setString(1, username);
            pst.setQueryTimeout(5);

            try (ResultSet resultSet = pst.executeQuery();) {
                if(resultSet.next())
                    return DBResponseType.ALREADY_IN_DB;
            }

            PreparedStatement p = connection.prepareStatement( SQL_QUERY_INSERT );

            p.setString(1, username);
            p.setString(2, password);
            p.execute();

            return DBResponseType.SUCCESSFUl;
        } catch (SQLException e) {
            e.printStackTrace();
            return DBResponseType.ERROR_CONNECTING;
        }
    }

    public User tryLoginUsername(String username, String password) {
        String SQL_QUERY_SELECT = "SELECT id FROM accounts WHERE username = ? AND password = PASSWORD(?)";
        String SQL_QUERY_INSERT = "INSERT INTO currently_logged (username, token) VALUES (?, ?)";

        try (Connection connection = DataSource.getConnection();
             PreparedStatement pst = connection.prepareStatement( SQL_QUERY_SELECT );) {
            pst.setString(1, username);
            pst.setString(2, password);
            pst.setQueryTimeout(5);

            try (ResultSet resultSet = pst.executeQuery();) {
                if(!resultSet.next())
                    return null;

                String token = generateSafeToken();
                PreparedStatement p = connection.prepareStatement( SQL_QUERY_INSERT );
                p.setString(1, username);
                p.setString(2, token);

                p.execute();
                return new User(resultSet.getInt("id"), token);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isUserValid(String username, String token) {
        String SQL_QUERY_SELECT = "SELECT * FROM currently_logged WHERE username = ? AND token = ?";

        try (Connection connection = DataSource.getConnection();
             PreparedStatement pst = connection.prepareStatement( SQL_QUERY_SELECT );) {
            pst.setString(1, username);
            pst.setString(2, token);
            pst.setQueryTimeout(5);

            try (ResultSet resultSet = pst.executeQuery();) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getUserName(int userId) {
        String SQL_QUERY_SELECT = "SELECT * FROM accounts WHERE id = ?";

        try (Connection connection = DataSource.getConnection();
             PreparedStatement pst = connection.prepareStatement( SQL_QUERY_SELECT );) {
            pst.setInt(1, userId);
            pst.setQueryTimeout(5);

            try (ResultSet resultSet = pst.executeQuery();) {
                return resultSet.next() ? resultSet.getString("username") : null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean tryPostNewPost(int authorId, String text) {
        String SQL_QUERY_INSERT = "INSERT INTO posts (authorId, text) VALUES (?, ?)";

        try (Connection connection = DataSource.getConnection();
             PreparedStatement pst = connection.prepareStatement( SQL_QUERY_INSERT );) {
            pst.setInt(1, authorId);
            pst.setString(2, text);
            pst.setQueryTimeout(5);

            pst.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean tryPostNewComment(int authorId, int postId, String text) {
        String SQL_QUERY_INSERT = "INSERT INTO comments (authorId, postId, text) VALUES (?, ?, ?)";

        try (Connection connection = DataSource.getConnection();
             PreparedStatement pst = connection.prepareStatement( SQL_QUERY_INSERT );) {
            pst.setInt(1, authorId);
            pst.setInt(2, postId);
            pst.setString(3, text);
            pst.setQueryTimeout(5);

            pst.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean tryAddReaction(int authorId, int textId, boolean value, ReactionType reactionType, ReactionTextType reactionTextType) {
        String SQL_QUERY_INSERT = "INSERT INTO reactions (authorId, textId, type, value, textType) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE value = ?";

        try (Connection connection = DataSource.getConnection();
             PreparedStatement pst = connection.prepareStatement( SQL_QUERY_INSERT );) {
            pst.setInt(1, authorId);
            pst.setInt(2, textId);
            pst.setInt(3, reactionType.getId());
            pst.setBoolean(4, value);
            pst.setInt(5, reactionTextType.getId());
            pst.setBoolean(6, value);
            pst.setQueryTimeout(5);

            pst.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Integer> getLatestsPostsId(int lastId) {
        List<Integer> posts = new ArrayList<>();
        String SQL_QUERY_SELECT = "SELECT id FROM posts ORDER BY date DESC LIMIT 5";

        if(lastId >= 0)
            SQL_QUERY_SELECT = "SELECT id FROM posts WHERE id < ? ORDER BY date DESC LIMIT 5";

        try (Connection connection = DataSource.getConnection();
             PreparedStatement pst = connection.prepareStatement( SQL_QUERY_SELECT );) {
            if(lastId >= 0)
                pst.setInt(1, lastId);

            try (ResultSet resultSet = pst.executeQuery();) {
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");

                    posts.add(id);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return posts;
    }

    public Post getPostsPerId(int lastId) {
        String SQL_QUERY_SELECT = "SELECT * FROM posts WHERE id = ?";

        try (Connection connection = DataSource.getConnection();
             PreparedStatement pst = connection.prepareStatement( SQL_QUERY_SELECT );) {
            pst.setInt(1, lastId);

            try (ResultSet resultSet = pst.executeQuery();) {
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    int authorId = resultSet.getInt("authorId");
                    String text = resultSet.getString("text");
                    Timestamp date = resultSet.getTimestamp("date");

                    return new Post(id, authorId, getUserName(authorId), text, date, getReactions(ReactionTextType.POST, id, true));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Integer> getLatestsSearchPostsId(int lastId, String[] words) {
        List<Integer> posts = new ArrayList<>();

        String toQueryAdd = "";
        for(int i = 0;i < words.length;i++) {
            toQueryAdd += "text LIKE ?" + (words.length - i <= 1 ? "" : " OR ");
        }

        String SQL_QUERY_SELECT = "SELECT id FROM posts WHERE " + toQueryAdd + " ORDER BY date DESC LIMIT 5";

        if(lastId >= 0)
            SQL_QUERY_SELECT = "SELECT id FROM posts WHERE id < ? AND (" + toQueryAdd + ") ORDER BY date DESC LIMIT 5";

        try (Connection connection = DataSource.getConnection();
             PreparedStatement pst = connection.prepareStatement( SQL_QUERY_SELECT );) {
            int index = 1;
            if(lastId >= 0) {
                pst.setInt(index, lastId);
                index++;
            }
            for (String word : words) {
                pst.setString(index, "%" + word + "%");
                index++;
            }

            try (ResultSet resultSet = pst.executeQuery();) {
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");

                    posts.add(id);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return posts;
    }

    public List<Integer> getCommentForPost(int postId, int lastId) {
        List<Integer> comments = new ArrayList<>();
        String SQL_QUERY_SELECT = "SELECT id FROM comments WHERE postId = ? ORDER BY date ASC LIMIT 5";

        if(lastId >= 0)
            SQL_QUERY_SELECT = "SELECT id FROM comments WHERE postId = ? AND id > ? ORDER BY date ASC LIMIT 5";

        try (Connection connection = DataSource.getConnection();
             PreparedStatement pst = connection.prepareStatement( SQL_QUERY_SELECT );) {
            pst.setInt(1, postId);
            if(lastId >= 0)
                pst.setInt(2, lastId);

            try (ResultSet resultSet = pst.executeQuery();) {
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");

                    comments.add(id);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return comments;
    }

    public Comment getCommentForPostPerId(int postId, int commentId) {
        String SQL_QUERY_SELECT = "SELECT * FROM comments WHERE id = ? AND postId = ?";

        try (Connection connection = DataSource.getConnection();
             PreparedStatement pst = connection.prepareStatement( SQL_QUERY_SELECT );) {
            pst.setInt(1, commentId);
            pst.setInt(2, postId);

            try (ResultSet resultSet = pst.executeQuery();) {
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    int authorId = resultSet.getInt("authorId");
                    String text = resultSet.getString("text");
                    Timestamp date = resultSet.getTimestamp("date");

                    return new Comment(id, authorId, getUserName(authorId), postId, text, date, getReactions(ReactionTextType.COMMENT, commentId, true));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Reaction> getReactions(ReactionTextType reactionTextType, int textId, boolean onlyValueTrue) {
        List<Reaction> reactions = new ArrayList<>();
        String SQL_QUERY_SELECT = "SELECT * FROM reactions WHERE textType = ? AND textId = ?";
        if(onlyValueTrue)
            SQL_QUERY_SELECT = "SELECT * FROM reactions WHERE textType = ? AND textId = ? AND value = ?";

        try (Connection connection = DataSource.getConnection();
             PreparedStatement pst = connection.prepareStatement( SQL_QUERY_SELECT );) {
            pst.setInt(1, reactionTextType.getId());
            pst.setInt(2, textId);
            if(onlyValueTrue)
                pst.setBoolean(3, true);

            try (ResultSet resultSet = pst.executeQuery();) {
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    int authorId = resultSet.getInt("authorId");
                    int type = resultSet.getInt("type");
                    boolean value = resultSet.getBoolean("value");
                    Timestamp date = resultSet.getTimestamp("date");

                    System.out.println("Found reaction for " + textId + " " + reactionTextType.name());

                    reactions.add(new Reaction(id, authorId, textId, type, value, reactionTextType.getId(), date));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reactions;
    }

    private String generateSafeToken() {
        SecureRandom random = new SecureRandom();
        byte bytes[] = new byte[128];
        random.nextBytes(bytes);
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String token = encoder.encodeToString(bytes);
        return token;
    }
}
