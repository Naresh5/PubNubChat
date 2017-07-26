package naresh.com.chat.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import naresh.com.chat.R;
import naresh.com.chat.databinding.LoginActivityBinding;
import naresh.com.chat.utils.Constants;

public class LoginActivity extends AppCompatActivity {
    LoginActivityBinding mLoginActivityBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLoginActivityBinding = DataBindingUtil.setContentView(this, R.layout.activity_login);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String lastUsername = extras.getString("oldUsername", "");
            mLoginActivityBinding.loginUsername.setText(lastUsername);
        }

        mLoginActivityBinding.buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                joinChat(v);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Takes the username from the EditText, check its validity and saves it if valid.
     * Then, redirects to the MainActivity.
     *
     * @param view Button clicked to trigger call to joinChat
     */
    public void joinChat(View view) {
        String username = mLoginActivityBinding.loginUsername.getText().toString();
        if (!validUsername(username))
            return;

        SharedPreferences sp = getSharedPreferences(Constants.CHAT_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor edit = sp.edit();
        edit.putString(Constants.CHAT_USERNAME, username);
        edit.apply();

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    /**
     * Optional function to specify what a username in your chat app can look like.
     *
     * @param username The name entered by a user.
     * @return
     */
    private boolean validUsername(String username) {
        if (username.length() == 0) {
            mLoginActivityBinding.loginUsername.setError("Username cannot be empty.");
            return false;
        }
        if (username.length() > 16) {
            mLoginActivityBinding.loginUsername.setError("Username too long.");
            return false;
        }
        return true;
    }
}
