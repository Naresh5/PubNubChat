package naresh.com.chat;

import android.app.Application;
import naresh.com.chat.broadcast.ConnectivityReceiver;

/**
 * Created by bhavdip on 28/7/17.
 */

public class MyApplication extends Application {

  private static MyApplication mInstance;

  @Override
  public void onCreate() {
    super.onCreate();

    mInstance = this;
  }

  public static synchronized MyApplication getInstance() {
    return mInstance;
  }

  public void setConnectivityListener(ConnectivityReceiver.ConnectivityReceiverListener listener) {
    ConnectivityReceiver.connectivityReceiverListener = listener;
  }

}
