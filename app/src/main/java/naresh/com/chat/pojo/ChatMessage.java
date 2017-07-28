package naresh.com.chat.pojo;

public class ChatMessage {

  private String username;
  private String message;
  private long timeStamp;

  public ChatMessage(String username, String message, long timeStamp) {
    this.username = username;
    this.message = message;
    this.timeStamp = timeStamp;
  }

  public ChatMessage() {

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
