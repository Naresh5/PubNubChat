package naresh.com.chat.callbacks;

import android.util.Log;

import com.pubnub.api.Callback;
import com.pubnub.api.PubnubError;

/**
 * Created by admin1234 on 7/26/17.
 */

public class BasicCallback extends Callback {

    public BasicCallback(){

    }

    @Override
    public void successCallback(String channel, Object response) {
        Log.e("PUBNUB", "Success: " + response.toString());
    }

    @Override
    public void connectCallback(String channel, Object message) {
        Log.e("PUBNUB", "Connect: " + message.toString());
    }

    @Override
    public void errorCallback(String channel, PubnubError error) {
        Log.e("PUBNUB", "Error: " + error.toString());
    }

}
