package me.tadeas.collectanswer;

import com.google.gson.Gson;
import me.tadeas.collectanswer.objects.Comment;
import me.tadeas.collectanswer.objects.Post;
import me.tadeas.collectanswer.objects.User;
import me.tadeas.collectanswer.types.ReactionTextType;
import me.tadeas.collectanswer.types.ReactionType;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This file was created by Tadeáš Drab on 4. 5. 2020
 */
public class CollectServer {

    private static boolean running = true;
    private static Gson gson = new Gson();
    private static DataHandler dataHandler = new DataHandler();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(1025);

        System.out.println("Connecting to DB...");
        try (Connection connection = DataSource.getConnection()) {
            System.out.println("Connected");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        System.out.println("Server socket ready...");
        while (running) {
            final Socket socket = serverSocket.accept();
            new Thread() {
                @Override
                public void run() {
                    try {
                        InputStream in = socket.getInputStream();
                        OutputStream os = socket.getOutputStream();

                        // Receiving
                        byte[] lenBytes = new byte[4];
                        in.read(lenBytes, 0, 4);
                        int len = (((lenBytes[3] & 0xff) << 24) | ((lenBytes[2] & 0xff) << 16) |
                                ((lenBytes[1] & 0xff) << 8) | (lenBytes[0] & 0xff));
                        byte[] receivedBytes = new byte[len];
                        in.read(receivedBytes, 0, len);
                        String received = new String(receivedBytes, 0, len);

                        System.out.println("Server received: " + received);

                        // Sending
                        String toSend = handleResponseAndSend(received);
                        byte[] toSendBytes = toSend.getBytes("UTF-8");
                        int toSendLen = toSendBytes.length;
                        byte[] toSendLenBytes = new byte[4];
                        toSendLenBytes[0] = (byte)(toSendLen & 0xff);
                        toSendLenBytes[1] = (byte)((toSendLen >> 8) & 0xff);
                        toSendLenBytes[2] = (byte)((toSendLen >> 16) & 0xff);
                        toSendLenBytes[3] = (byte)((toSendLen >> 24) & 0xff);
                        os.write(toSendLenBytes);
                        os.write(toSendBytes);

                        System.out.println("Send: " + toSend);

                        socket.close();
                    }catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    System.out.println("thread id " + getId());
                }
            }.start();
        }
    }

    private static String handleResponseAndSend(String response) {
        Map<String, String> object = gson.fromJson(response, Map.class);
        Map<String, Object> ret = new HashMap<>();

        if(object != null) {
            if(object.containsKey("type") && object.get("type").equals("registration")) {
                ret.put("type", "registration");

                String username = object.get("username");
                String password = object.get("password");

                DBResponseType dbResponseType = dataHandler.tryRegisterUsername(username.toLowerCase(), password);

                if(dbResponseType == DBResponseType.SUCCESSFUl) {
                    ret.put("status", "success");
                }else if(dbResponseType == DBResponseType.ALREADY_IN_DB){
                    ret.put("status", "already_registered");
                }else if(dbResponseType == DBResponseType.ERROR_CONNECTING){
                    ret.put("status", "error");
                }
            }else if(object.containsKey("type") && object.get("type").equals("login")) {
                ret.put("type", "login");

                String username = object.get("username");
                String password = object.get("password");

                User user = dataHandler.tryLoginUsername(username.toLowerCase(), password);

                if(user != null) {
                    ret.put("status", "success");
                    ret.put("token", user.getToken());
                    ret.put("userId", user.getId() + "");
                }else {
                    ret.put("status", "bad_password");
                }
            }else if(object.containsKey("type") && object.get("type").equals("post")) {
                ret.put("type", "post");

                String clientUsername = object.get("username");
                int userId = Integer.parseInt(object.get("userId"));
                String token = object.get("token");
                String text = object.get("text");

                String username = dataHandler.getUserName(userId);

                if(username == null || clientUsername == null) {
                    ret.put("status", "error");
                }else {
                    boolean valid = dataHandler.isUserValid(username.toLowerCase(), token);

                    if(username.equalsIgnoreCase(clientUsername.toLowerCase()) && valid) {
                        if(dataHandler.tryPostNewPost(userId, text)) {
                            ret.put("status", "success");
                        }else{
                            ret.put("status", "error");
                        }
                    }else{
                        ret.put("status", "error");
                    }
                }
            }else if(object.containsKey("type") && object.get("type").equals("comment")) {
                ret.put("type", "comment");

                String clientUsername = object.get("username");
                int userId = Integer.parseInt(object.get("userId"));
                String token = object.get("token");
                int postId = Integer.parseInt(object.get("postId"));
                String text = object.get("text");

                String username = dataHandler.getUserName(userId);

                if(username == null || clientUsername == null) {
                    ret.put("status", "error");
                }else {
                    boolean valid = dataHandler.isUserValid(username.toLowerCase(), token);

                    if(username.equalsIgnoreCase(clientUsername.toLowerCase()) && valid) {
                        if(dataHandler.tryPostNewComment(userId, postId, text)) {
                            ret.put("status", "success");
                        }else{
                            ret.put("status", "error");
                        }
                    }else{
                        ret.put("status", "error");
                    }
                }
            }else if(object.containsKey("type") && object.get("type").equals("getposts")) {
                ret.put("type", "getposts");

                String clientUsername = object.get("username");
                int userId = Integer.parseInt(object.get("userId"));
                String token = object.get("token");
                int lastPostId = Integer.parseInt(object.get("lastPostId"));

                String username = dataHandler.getUserName(userId);

                if(username == null || clientUsername == null) {
                    ret.put("status", "error");
                }else {
                    boolean valid = dataHandler.isUserValid(username.toLowerCase(), token);

                    if(username.equalsIgnoreCase(clientUsername.toLowerCase()) && valid) {
                        List<Integer> postList = dataHandler.getLatestsPostsId(lastPostId);
                        ret.put("status", "success");

                        ret.put("posts", postList);
                    }else{
                        ret.put("status", "error");
                    }
                }
            }else if(object.containsKey("type") && object.get("type").equals("getsearchposts")) {
                ret.put("type", "getsearchposts");

                String clientUsername = object.get("username");
                int userId = Integer.parseInt(object.get("userId"));
                String token = object.get("token");
                int lastPostId = Integer.parseInt(object.get("lastPostId"));
                String words = object.get("words");
                String[] wordsSplit = words.split(" ");
                if(words.split(" ").length <= 1)
                    wordsSplit = new String[] {words};

                String username = dataHandler.getUserName(userId);

                if(username == null || clientUsername == null) {
                    ret.put("status", "error");
                }else {
                    boolean valid = dataHandler.isUserValid(username.toLowerCase(), token);

                    if(username.equalsIgnoreCase(clientUsername.toLowerCase()) && valid) {
                        List<Integer> postList = dataHandler.getLatestsSearchPostsId(lastPostId, wordsSplit);
                        ret.put("status", "success");

                        ret.put("searchposts", postList);
                    }else{
                        ret.put("status", "error");
                    }
                }
            }else if(object.containsKey("type") && object.get("type").equals("getpostsperid")) {
                ret.put("type", "getpostsperid");

                String clientUsername = object.get("username");
                int userId = Integer.parseInt(object.get("userId"));
                String token = object.get("token");
                int postId = Integer.parseInt(object.get("postId"));

                String username = dataHandler.getUserName(userId);

                if(username == null || clientUsername == null) {
                    ret.put("status", "error");
                }else {
                    boolean valid = dataHandler.isUserValid(username.toLowerCase(), token);

                    if(username.equalsIgnoreCase(clientUsername.toLowerCase()) && valid) {
                        Post post = dataHandler.getPostsPerId(postId);
                        if(post != null) {
                            ret.put("status", "success");

                            ret.put("post", post);
                        }else {
                            ret.put("status", "error");
                        }
                    }else{
                        ret.put("status", "error");
                    }
                }
            }else if(object.containsKey("type") && object.get("type").equals("getcomments")) {
                ret.put("type", "getcomments");

                String clientUsername = object.get("username");
                int userId = Integer.parseInt(object.get("userId"));
                String token = object.get("token");
                int postId = Integer.parseInt(object.get("postId"));
                int lastCommentId = Integer.parseInt(object.get("lastCommentId"));

                String username = dataHandler.getUserName(userId);

                if(username == null || clientUsername == null) {
                    ret.put("status", "error");
                }else {
                    boolean valid = dataHandler.isUserValid(username.toLowerCase(), token);

                    if(username.equalsIgnoreCase(clientUsername.toLowerCase()) && valid) {
                        List<Integer> postList = dataHandler.getCommentForPost(postId, lastCommentId);
                        ret.put("status", "success");

                        ret.put("comments", postList);
                    }else{
                        ret.put("status", "error");
                    }
                }
            }else if(object.containsKey("type") && object.get("type").equals("getcommentssperid")) {
                ret.put("type", "getcommentssperid");

                String clientUsername = object.get("username");
                int userId = Integer.parseInt(object.get("userId"));
                String token = object.get("token");
                int postId = Integer.parseInt(object.get("postId"));
                int commentId = Integer.parseInt(object.get("commentId"));

                String username = dataHandler.getUserName(userId);

                if(username == null || clientUsername == null) {
                    ret.put("status", "error");
                }else {
                    boolean valid = dataHandler.isUserValid(username.toLowerCase(), token);

                    if(username.equalsIgnoreCase(clientUsername.toLowerCase()) && valid) {
                        Comment comment = dataHandler.getCommentForPostPerId(postId, commentId);
                        if(comment != null){
                            ret.put("status", "success");
                            ret.put("comment", comment);
                        }else {
                            ret.put("status", "error");
                        }
                    }else{
                        ret.put("status", "error");
                    }
                }
            }else if(object.containsKey("type") && object.get("type").equals("reaction")) {
                ret.put("type", "reaction");

                String clientUsername = object.get("username");
                int userId = Integer.parseInt(object.get("userId"));
                String token = object.get("token");
                int textType = Integer.parseInt(object.get("textType"));
                int postId = Integer.parseInt(object.get("postId"));
                String value = object.get("value");

                String username = dataHandler.getUserName(userId);

                if(username == null || clientUsername == null) {
                    ret.put("status", "error");
                }else {
                    boolean valid = dataHandler.isUserValid(username.toLowerCase(), token);

                    if(username.equalsIgnoreCase(clientUsername.toLowerCase()) && valid) {
                        if(dataHandler.tryAddReaction(userId, postId, value.equalsIgnoreCase("true"), ReactionType.LIKE, ReactionTextType.getType(textType))) {
                            ret.put("status", "success");
                        }else{
                            ret.put("status", "error");
                        }
                    }else{
                        ret.put("status", "error");
                    }
                }
            }


        }
        return gson.toJson(ret);
    }
}
