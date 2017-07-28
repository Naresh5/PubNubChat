package naresh.com.chat.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import naresh.com.chat.R;

/**
 * Created by bhavdip on 28/7/17.
 */

public class Utility {

  static Context context;

  NetworkInfo wifiInfo, mobileInfo;



  /*public static boolean isNetworkAvailable() {
    ConnectivityManager connectivityManager =
        (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
  }*/

  public static boolean isNetworkAvailable() {
    ConnectivityManager connectivityManager;
    boolean connected = false;

    try {

      connectivityManager =
          (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

      NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
      connected = networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected();
      return connected;
    } catch (Exception e) {
      System.out.println("CheckConnectivity Exception: " + e.getMessage());
      Log.v("connectivity", e.toString());
    }
    return connected;
  }

  public static void showSnackBar(View view, int message) {
    final Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
    View snackBarView = snackbar.getView();
    snackBarView.setBackgroundResource(R.color.colorChatBg);
    snackbar.show();
  }
}
