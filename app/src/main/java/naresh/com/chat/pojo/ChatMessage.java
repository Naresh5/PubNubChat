package naresh.com.chat.pojo;

/**
 * Created by admin1234 on 7/26/17.
 */

public class ChatMessage {

    private String username;
    private String message;
    private long timeStamp;

    public ChatMessage(String username, String message, long timeStamp) {
        this.username = username;
        this.message = message;
        this.timeStamp = timeStamp;
    }

    public String getUsername() {
        return username;
    }

    public String getMessage() {
        return message;
    }

    public long getTimeStamp() {
        return timeStamp;
    }


}
