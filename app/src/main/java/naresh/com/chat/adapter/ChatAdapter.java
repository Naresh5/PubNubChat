package naresh.com.chat.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import naresh.com.chat.R;
import naresh.com.chat.pojo.ChatMessage;

import static android.content.ContentValues.TAG;

/**
 * Created by admin1234 on 7/26/17.
 */

public class ChatAdapter extends ArrayAdapter<ChatMessage> {
  private final Context context;
  private LayoutInflater inflater;
  private List<ChatMessage> values;
  private Set<String> onlineNow = new HashSet<String>();
  private String myUserName;
  private ChatMessage chatMessageObj;

  public ChatAdapter(Context context, List<ChatMessage> values, String myUserName) {
    super(context, R.layout.chat_row_layout, android.R.id.text1, values);
    this.context = context;
    this.inflater = LayoutInflater.from(context);
    this.values = values;
    this.myUserName = myUserName;
  }

  class ViewHolder {
    TextView user, userLeft;
    TextView message, messageLeft;
    TextView timeStamp, timeStampLeft;
    View userPresence, userPresenceLeft;
    ChatMessage chatMsg, chatMsgLeft;
    LinearLayout chatHolder, chatHolderLeft;
  }

  @Override
  public View getView(final int position, View convertView, ViewGroup parent) {
    chatMessageObj = new ChatMessage();
    chatMessageObj = this.values.get(position);
    Log.e(TAG, "getView: " + myUserName);
    ViewHolder holder;
    if (convertView == null) {
      holder = new ViewHolder();
      convertView = inflater.inflate(R.layout.chat_layout, parent, false);

      holder.user = (TextView) convertView.findViewById(R.id.chat_user);
      holder.message = (TextView) convertView.findViewById(R.id.chat_message);
      holder.timeStamp = (TextView) convertView.findViewById(R.id.chat_time);
      holder.userPresence = convertView.findViewById(R.id.user_presence);
      holder.chatHolder = (LinearLayout) convertView.findViewById(R.id.chatHolder);

      holder.userLeft = (TextView) convertView.findViewById(R.id.chat_userLeft);
      holder.messageLeft = (TextView) convertView.findViewById(R.id.chat_messageLeft);
      holder.timeStampLeft = (TextView) convertView.findViewById(R.id.chat_timeLeft);
      holder.userPresenceLeft = convertView.findViewById(R.id.user_presenceLeft);
      holder.chatHolderLeft = (LinearLayout) convertView.findViewById(R.id.chatHolderLeft);

      convertView.setTag(holder);
    } else {
      holder = (ViewHolder) convertView.getTag();
    }

    if (!chatMessageObj.getUsername().equals(myUserName)) {
      holder.chatHolder.setVisibility(View.VISIBLE);
      holder.chatHolderLeft.setVisibility(View.GONE);
      holder.user.setText(chatMessageObj.getUsername());
      holder.message.setText(chatMessageObj.getMessage());
      holder.timeStamp.setText(formatTimeStamp(chatMessageObj.getTimeStamp()));
      holder.chatMsg = chatMessageObj;
      holder.userPresence.setBackgroundDrawable( // If online show the green presence dot
          this.onlineNow.contains(chatMessageObj.getUsername()) ? context.getResources()
              .getDrawable(R.drawable.online_circle) : null);
    } else {

      holder.chatHolderLeft.setVisibility(View.VISIBLE);
      holder.chatHolder.setVisibility(View.GONE);
      holder.userLeft.setText(chatMessageObj.getUsername());
      holder.messageLeft.setText(chatMessageObj.getMessage());
      holder.timeStampLeft.setText(formatTimeStamp(chatMessageObj.getTimeStamp()));
      holder.chatMsgLeft = chatMessageObj;
      holder.userPresenceLeft.setBackgroundDrawable( // If online show the green presence dot
          this.onlineNow.contains(chatMessageObj.getUsername()) ? context.getResources()
              .getDrawable(R.drawable.online_circle) : null);
    }

    return convertView;
  }

  @Override
  public int getCount() {
    return this.values.size();
  }

  /**
   * Method to add a single message and update the listview.
   *
   * @param chatMsg Message to be added
   */
  public void addMessage(ChatMessage chatMsg) {
    this.values.add(chatMsg);
    notifyDataSetChanged();
  }

  /**
   * Method to add a list of messages and update the listview.
   *
   * @param chatMsgs Messages to be added
   */
  public void setMessages(List<ChatMessage> chatMsgs) {
    this.values.clear();
    this.values.addAll(chatMsgs);
    notifyDataSetChanged();
  }

  /**
   * Handle users. Fill the onlineNow set with current users. Data is used to display a green dot
   * next to users who are currently online.
   * @param user UUID of the user online.
   * @param action The presence action
   */
  public void userPresence(String user, String action) {
    boolean isOnline = action.equals("join") || action.equals("state-change");
    if (!isOnline && this.onlineNow.contains(user)) {
      this.onlineNow.remove(user);
    } else if (isOnline && !this.onlineNow.contains(user)) this.onlineNow.add(user);

    notifyDataSetChanged();
  }

  /**
   * Overwrite the onlineNow array with all the values attained from a call to hereNow().
   */
  public void setOnlineNow(Set<String> onlineNow) {
    this.onlineNow = onlineNow;
    notifyDataSetChanged();
  }

  /**
   * Format the long System.currentTimeMillis() to a better looking timestamp. Uses a calendar
   * object to format with the user's current time zone.
   */
  public static String formatTimeStamp(long timeStamp) {
    // Create a DateFormatter object for displaying date in specified format.
    SimpleDateFormat formatter = new SimpleDateFormat("hh:mm a");

    // Create a calendar object that will convert the date and time value in milliseconds to date.
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(timeStamp);
    return formatter.format(calendar.getTime());
  }

  public void clearMessages() {
    this.values.clear();
    notifyDataSetChanged();
  }
}