package naresh.com.chat.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import naresh.com.chat.MyApplication;
import naresh.com.chat.R;
import naresh.com.chat.adapter.ChatAdapter;
import naresh.com.chat.broadcast.ConnectivityReceiver;
import naresh.com.chat.callbacks.BasicCallback;
import naresh.com.chat.databinding.MainActivityBinding;
import naresh.com.chat.pojo.ChatMessage;
import naresh.com.chat.utils.Constants;
import naresh.com.chat.utils.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static android.widget.Toast.LENGTH_LONG;

public class MainActivity extends AppCompatActivity
        implements ConnectivityReceiver.ConnectivityReceiverListener {

    private String TAG = MainActivity.class.getSimpleName();
    private MainActivityBinding mMainActivityBinding;
    private Pubnub mPubNub;
    private MenuItem mHereNow;
    private ListView mListView;
    private ChatAdapter mChatAdapter;
    private SharedPreferences mSharedPrefs;
    private String username;
    private String channel = "MainChat";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMainActivityBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        checkConnection();

        mSharedPrefs = getSharedPreferences(Constants.CHAT_PREFS, MODE_PRIVATE);
        if (!mSharedPrefs.contains(Constants.CHAT_USERNAME)) {
            Intent toLogin = new Intent(this, LoginActivity.class);
            startActivity(toLogin);
            return;
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            Log.e("Main-bundle",
                    extras.toString() + " Has Chat: " + extras.getString(Constants.CHAT_ROOM));
            if (extras.containsKey(Constants.CHAT_ROOM)) {
                this.channel = extras.getString(Constants.CHAT_ROOM);
            }
        }

        this.username = mSharedPrefs.getString(Constants.CHAT_USERNAME, "Anonymous");

        this.mListView = mMainActivityBinding.chatList;
        this.mChatAdapter = new ChatAdapter(this, new ArrayList<ChatMessage>(), username);
        this.mChatAdapter.userPresence(this.username,
                "join"); // Set user to online. Status changes handled in presence
        setupAutoScroll();
        this.mListView.setAdapter(mChatAdapter);
        setupListView();
        mMainActivityBinding.channelBar.setText(this.channel);

        initPubNub();

        mMainActivityBinding.btnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(v);
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.mHereNow = menu.findItem(R.id.action_here_now);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_here_now:
                hereNow(true);
                return true;
            case R.id.action_sign_out:
                signOut();
                return true;

           /* case R.id.action_gcm_register:
                gcmRegister();
                return true;
            case R.id.action_gcm_unregister:
                gcmUnregister();
                return true;*/
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Might want to unsubscribe from PubNub here and create background service to listen while
     * app is not in foreground.
     * PubNub will stop subscribing when screen is turned off for this demo, messages will be loaded
     * when app is opened through a call to history.
     * The best practice would be creating a background service in onStop to handle messages.
     */
    @Override
    protected void onStop() {
        super.onStop();
        if (this.mPubNub != null) this.mPubNub.unsubscribeAll();
    }

    /**
     * Instantiate PubNub object if it is null. Subscribe to channel and pull old messages via
     * history.
     */
    @Override
    protected void onRestart() {
        super.onRestart();
        if (this.mPubNub == null) {
            initPubNub();
        } else {
            subscribeWithPresence();
            history();
        }
    }

    /**
     * I remove the PubNub object in onDestroy since turning the screen off triggers onStop and
     * I wanted PubNub to receive messages while the screen is off.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Instantiate PubNub object with username as UUID
     * Then subscribe to the current channel with presence.
     * Finally, populate the listview with past messages from history
     */
    private void initPubNub() {
        this.mPubNub = new Pubnub(Constants.PUBLISH_KEY, Constants.SUBSCRIBE_KEY);
        this.mPubNub.setUUID(this.username);
        subscribeWithPresence();
        history();
        //   gcmRegister();
    }

    /**
     * Use PubNub to send any sort of data
     *
     * @param type The type of the data, used to differentiate groupMessage from directMessage
     * @param data The payload of the publish
     */
    public void publish(String type, JSONObject data) {
        JSONObject json = new JSONObject();
        try {
            json.put("type", type);
            json.put("data", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        this.mPubNub.publish(this.channel, json, new BasicCallback());
    }

    /**
     * Update here now number, uses a call to the pubnub hereNow function.
     *
     * @param displayUsers If true, display a modal of users in room.
     */
    public void hereNow(final boolean displayUsers) {
        this.mPubNub.hereNow(this.channel, new Callback() {
            @Override
            public void successCallback(String channel, Object response) {
                try {
                    JSONObject json = (JSONObject) response;
                    final int occ = json.getInt("occupancy");
                    final JSONArray hereNowJSON = json.getJSONArray("uuids");
                    Log.d("JSON_RESP", "Here Now: " + json.toString());
                    final Set<String> usersOnline = new HashSet<String>();
                    usersOnline.add(username);
                    for (int i = 0; i < hereNowJSON.length(); i++) {
                        usersOnline.add(hereNowJSON.getString(i));
                    }
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mHereNow != null) mHereNow.setTitle(String.valueOf(occ));
                            mChatAdapter.setOnlineNow(usersOnline);
                            // if (displayUsers)
                            //     alertHereNow(usersOnline);
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Called at login time, sets meta-data of users' log-in times using the PubNub State API.
     * Information is retrieved in getStateLogin
     */
    public void setStateLogin() {
        Callback callback = new Callback() {
            @Override
            public void successCallback(String channel, Object response) {
                Log.d("PUBNUB", "State: " + response.toString());
            }
        };
        try {
            JSONObject state = new JSONObject();
            state.put(Constants.STATE_LOGIN, System.currentTimeMillis());
            this.mPubNub.setState(this.channel, this.mPubNub.getUUID(), state, callback);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get state information. Information is deleted when user unsubscribes from channel
     * so display a user not online message if there is no UUID data attached to the
     * channel's state
     */
    public void getStateLogin(final String user) {
        Callback callback = new Callback() {
            @Override
            public void successCallback(String channel, Object response) {
                if (!(response instanceof JSONObject)) return; // Ignore if not JSON
                try {
                    JSONObject state = (JSONObject) response;
                    final boolean online = state.has(Constants.STATE_LOGIN);
                    final long loginTime = online ? state.getLong(Constants.STATE_LOGIN) : 0;

                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!online) {
                                Toast.makeText(MainActivity.this, user + " is not online.", Toast.LENGTH_SHORT)
                                        .show();
                            } else {
                                Toast.makeText(MainActivity.this,
                                        user + " logged in since " + ChatAdapter.formatTimeStamp(loginTime),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                    Log.d("PUBNUB", "State: " + response.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
        this.mPubNub.getState(this.channel, user, callback);
    }

    /**
     * Subscribe to channel, when subscribe connection is established, in connectCallback, subscribe
     * to presence, set login time with setStateLogin and update hereNow information.
     * When a message is received, in successCallback, get the ChatMessage information from the
     * received JSONObject and finally put it into the listview's ChatAdapter.
     * Chat adapter calls notifyDatasetChanged() which updates UI, meaning must run on UI thread.
     */
    public void subscribeWithPresence() {
        Callback subscribeCallback = new Callback() {
            @Override
            public void successCallback(String channel, Object message) {
                if (message instanceof JSONObject) {
                    try {
                        JSONObject jsonObj = (JSONObject) message;
                        JSONObject json = jsonObj.getJSONObject("data");
                        String name = json.getString(Constants.JSON_USER);
                        String msg = json.getString(Constants.JSON_MSG);
                        long time = json.getLong(Constants.JSON_TIME);
                        if (name.equals(mPubNub.getUUID())) return; // Ignore own messages
                        final ChatMessage chatMsg = new ChatMessage(name, msg, time);
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mChatAdapter.addMessage(chatMsg);
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                Log.d("PUBNUB", "Channel: " + channel + " Msg: " + message.toString());
            }

            @Override
            public void connectCallback(String channel, Object message) {
                Log.d("Subscribe", "Connected! " + message.toString());
                hereNow(false);
                setStateLogin();
            }

        };
        try {
            mPubNub.subscribe(this.channel, subscribeCallback);
            presenceSubscribe();
        } catch (PubnubException e) {
            e.printStackTrace();
        }
    }

    /**
     * Subscribe to presence. When user join or leave are detected, update the hereNow number
     * as well as add/remove current user from the chat adapter's userPresence array.
     * This array is used to see what users are currently online and display a green dot next
     * to users who are online.
     */
    public void presenceSubscribe() {
        Callback callback = new Callback() {
            @Override
            public void successCallback(String channel, Object response) {
                Log.i("PN-pres",
                        "Pres: " + response.toString() + " class: " + response.getClass().toString());
                if (response instanceof JSONObject) {
                    JSONObject json = (JSONObject) response;
                    Log.d("PN-main", "Presence: " + json.toString());
                    try {
                        final int occ = json.getInt("occupancy");
                        final String user = json.getString("uuid");
                        final String action = json.getString("action");
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mChatAdapter.userPresence(user, action);
                                mHereNow.setTitle(String.valueOf(occ));
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void errorCallback(String channel, PubnubError error) {
                Log.d("Presence", "Error: " + error.toString());
            }
        };
        try {
            this.mPubNub.presence(this.channel, callback);
        } catch (PubnubException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get last 100 messages sent on current channel from history.
     */
    public void history() {
        if (mPubNub == null) {
            initPubNub();
        }
        this.mPubNub.history(this.channel, 100, false, new Callback() {
            @Override
            public void successCallback(String channel, final Object message) {
                try {
                    JSONArray json = (JSONArray) message;
                    Log.d("History", json.toString());
                    final JSONArray messages = json.getJSONArray(0);
                    final List<ChatMessage> chatMsgs = new ArrayList<ChatMessage>();
                    for (int i = 0; i < messages.length(); i++) {
                        try {
                            if (!messages.getJSONObject(i).has("data")) continue;
                            JSONObject jsonMsg = messages.getJSONObject(i).getJSONObject("data");
                            String name = jsonMsg.getString(Constants.JSON_USER);
                            String msg = jsonMsg.getString(Constants.JSON_MSG);
                            long time = jsonMsg.getLong(Constants.JSON_TIME);
                            ChatMessage chatMsg = new ChatMessage(name, msg, time);
                            chatMsgs.add(chatMsg);
                        } catch (JSONException e) { // Handle errors silently
                            e.printStackTrace();
                        }
                    }

                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "History loaded..!", Toast.LENGTH_SHORT).show();
                            mChatAdapter.setMessages(chatMsgs);
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void errorCallback(String channel, PubnubError error) {
                Log.d("History", error.toString());
            }
        });
    }

    /**
     * Log out, remove username from SharedPreferences, unsubscribe from PubNub, and send user back
     * to the LoginActivity
     */
    public void signOut() {
        this.mPubNub.unsubscribeAll();
        SharedPreferences.Editor edit = mSharedPrefs.edit();
        edit.remove(Constants.CHAT_USERNAME);
        edit.apply();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("oldUsername", this.username);
        startActivity(intent);
    }

    /**
     * Setup the listview to scroll to bottom anytime it receives a message.
     */
    private void setupAutoScroll() {
        this.mChatAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                mListView.setSelection(mChatAdapter.getCount() - 1);
                // mListView.smoothScrollToPosition(mChatAdapter.getCount()-1);
            }
        });
    }

    /**
     * On message click, display the last time the user logged in.
     */
    private void setupListView() {
        this.mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ChatMessage chatMsg = mChatAdapter.getItem(position);
                //    sendNotification(chatMsg.getUsername());
            }
        });
    }

    /**
     * Publish message to current channel.
     */
    public void sendMessage(View view) {
        String message = mMainActivityBinding.EditTextMessage.getText().toString();
        if (message.equals("")) return;
        mMainActivityBinding.EditTextMessage.setText("");
        ChatMessage chatMsg = new ChatMessage(username, message, System.currentTimeMillis());
        try {
            JSONObject json = new JSONObject();
            json.put(Constants.JSON_USER, chatMsg.getUsername());
            json.put(Constants.JSON_MSG, chatMsg.getMessage());
            json.put(Constants.JSON_TIME, chatMsg.getTimeStamp());
            publish(Constants.JSON_GROUP, json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mChatAdapter.addMessage(chatMsg);
    }

    /**
     * Create an alert dialog with a list of users who are here now.
     * When a user's name is clicked, get their state information and display it with Toast.
     */
    private void alertHereNow(Set<String> userSet) {
        List<String> users = new ArrayList<String>(userSet);
        LayoutInflater li = LayoutInflater.from(this);
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("Here Now");
        alertDialog.setNegativeButton("Done", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        final ArrayAdapter<String> hnAdapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, users);
        alertDialog.setAdapter(hnAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String user = hnAdapter.getItem(which);
                getStateLogin(user);
            }
        });
        alertDialog.show();
    }


    // Method to manually check connection status

    private void checkConnection() {
        boolean isConnected = ConnectivityReceiver.isConnected();
        showSnack(isConnected);
    }

    // Showing the status in Snackbar
    private void showSnack(boolean isConnected) {
        String message;
        int color, textColor;
        if (isConnected) {
            message = "Good! Connected to Internet";
            color = Color.WHITE;
        } else {
            message = getResources().getString(R.string.internet_not_connected);
            textColor = Color.RED;
            color = Color.DKGRAY;
            Snackbar snackbar = Snackbar
                    .make(findViewById(R.id.btnSendMessage), message, Snackbar.LENGTH_LONG);
            View sbView = snackbar.getView();
            TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
            textView.setTextColor(textColor);
            textView.setBackgroundColor(color);
            snackbar.show();
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
        // register connection status listener
        MyApplication.getInstance().setConnectivityListener(this);
    }

    /**
     * Callback will be triggered when there is change in
     * network connection
     */
    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        showSnack(isConnected);
    }

    /**
     * Create an alert dialog with a text view to enter a new channel to join. If the channel is
     * not empty, unsubscribe from the current channel and join the new one.
     * Then, get messages from history and update the channelView which displays current channel.
     */

 /* public void changeChannel(View view) {
    LayoutInflater li = LayoutInflater.from(this);
    View promptsView = li.inflate(R.layout.channel_change, null);

    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
    alertDialogBuilder.setView(promptsView);

    final EditText userInput = (EditText) promptsView.findViewById(R.id.editTextDialogUserInput);
    userInput.setText(this.channel);                       // Set text to current ID
    userInput.setSelection(userInput.getText().length());  // Move cursor to end

    alertDialogBuilder.setCancelable(false)
        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            String newChannel = userInput.getText().toString();
            if (newChannel.equals("")) return;

            mPubNub.unsubscribe(channel);
            mChatAdapter.clearMessages();
            channel = newChannel;
            mMainActivityBinding.channelBar.setText(channel);
            subscribeWithPresence();
            history();
          }
        })
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            dialog.cancel();
          }
        });
    AlertDialog alertDialog = alertDialogBuilder.create();
    alertDialog.show();
  }*/
}
