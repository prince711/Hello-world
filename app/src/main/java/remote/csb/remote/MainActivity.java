package remote.csb.remote;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.dvdo.remote.R;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    private static final int REQ_SIGN_IN_REQUIRED = 9002;
    private GoogleApiClient mGoogleApiClient;
    private static final String TAG = "SignInActivity";
    private static final int RC_SIGN_IN = 9001;

    private TextView mStatusTextView;
    private ProgressDialog mProgressDialog;
    private boolean loggedIn = true;
    private String scope = "https://www.googleapis.com/auth/youtube https://www.googleapis.com/auth/userinfo.profile";
    private String result;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Set the dimensions of the sign-in button.
        SignInButton signInButton = (SignInButton) findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(this);
        signInButton.setSize(SignInButton.SIZE_STANDARD);

        // Configure sign-in to request the user's ID, email address, and basic
// profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
//                .requestIdToken("334810922425-f8fh45hl9uj2ulvjsrf3r6gh3r48ghn3.apps.googleusercontent.com")
//                .requestServerAuthCode("334810922425-f8fh45hl9uj2ulvjsrf3r6gh3r48ghn3.apps.googleusercontent.com")
                .requestScopes(new Scope("https://www.googleapis.com/auth/youtube"))
                .requestScopes(new Scope("https://www.googleapis.com/auth/youtube.upload"))
                .build();

        // Build a GoogleApiClient with access to the Google Sign-In API and the
// options specified by gso.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {

                        Log.d(TAG, "Connected");

                    }

                    @Override
                    public void onConnectionSuspended(int i) {

                        Log.d(TAG, "Suspended");
                    }
                })
                .build();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                if (!loggedIn)
                    signIn();
                else signOut();
                break;
        }
    }

    private void signIn() {

        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
        if (requestCode == REQ_SIGN_IN_REQUIRED && resultCode == RESULT_OK) {
            // We had to sign in - now we can finish off the token request.
            new RetrieveTokenTask().execute();
        }
    }


    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();

            AccountManager accountManager = AccountManager.get(this);
            new RetrieveTokenTask().execute(acct.getEmail());
          /*  accountManager.getAuthToken(acct, "AUTH_TOKEN_TYPE", null, this, new AccountManagerCallback() {
                public void run(AccountManagerFuture future) {
                    try {
                        String token = future.getResult().getString(AccountManager.KEY_AUTHTOKEN);
                        Log.d(TAG, "Access Token........." + token);
//                        fetchPrivateVideoList = new FetchPrivateVideoList(webView, mActivity, token);
//                        fetchPrivateVideoList.execute();
//                        useToken(account, token);
                    } catch (Exception e) {
//                        onAccessDenied();
                        Log.d(TAG, "Exceptiion........." + e);
                    }
                }
            }, null);*/
            updateUI(true);
        } else {
            // Signed out, show unauthenticated UI.
            updateUI(false);
        }
    }

    private void updateUI(boolean signedIn) {
        if (signedIn) {
//            findViewById(R.id.sign_in_button)
            loggedIn = true;

//            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
//            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.VISIBLE);
        } else {
//            mStatusTextView.setText(R.string.signed_out);

            loggedIn = false;
            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
//            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.GONE);
        }
    }

    private void signOut() {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        // [START_EXCLUDE]
                        updateUI(false);
                        // [END_EXCLUDE]
                    }
                });
    }


    private class RetrieveTokenTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String accountName = params[0];
            String scopes = "oauth2:profile email";
            String token = null;
            try {
                token = GoogleAuthUtil.getToken(getApplicationContext(), accountName, scopes);

            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            } catch (UserRecoverableAuthException e) {
                startActivityForResult(e.getIntent(), REQ_SIGN_IN_REQUIRED);
                //REQ_SIGN_IN_REQUIRED = 55664;
            } catch (GoogleAuthException e) {
                Log.e(TAG, e.getMessage());
            }
            return token;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            new UploadIdAsyncTask().execute(s);
            Log.i("AccessToken", s);
        }
    }

   /* *//**
     * Hit API to get list of uploaded videos.
     * @param
     * @return
     *//*
    private String getVideoList(String accessToken) {

        String urlVideoList = "https://www.googleapis.com/youtube/v3/subscriptions?part=snippet&maxResults=50&mine=true&access_token=" + accessToken + "";
//        String urlVideoList="https://www.googleapis.com/youtube/v3/activities?access_token=" + accessToken + "&part=snippet,contentDetails&home=true&maxResults=20";
        HttpURLConnection httpURLConnection = null;
        InputStream inputStream = null;
        String data = "";

        try {

            URL url = new URL(urlVideoList);
            Log.d(TAG, " url for channel id list: " + url);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.connect();
            // Reading data from url
            inputStream = httpURLConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    inputStream));
            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            data = sb.toString();
            Log.d(TAG, " Response data: " + data);
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }*/

    class CustomAsyncTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            getVideoList(strings[0],token);

            return null;
        }
    }


    /**
     * Hit API to get list of uploaded videos.
     *
     * @param uploadID
     * @param accessToken
     * @return
     */
    private String getVideoList(String uploadID, String accessToken) {
        String urlVideoList = "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&playlistId=" + uploadID + "&access_token=" + accessToken + "&maxResults=15";
        HttpURLConnection httpURLConnection = null;
        InputStream inputStream = null;
        String data = "";

        try {
            URL url = new URL(urlVideoList);
            Log.d(TAG, " url for Video list: " + url);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.connect();
            // Reading data from url
            inputStream = httpURLConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    inputStream));
            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            data = sb.toString();
            Log.d(TAG, " Response data: " + data);
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    /**
     * method used to hit API and get response for content details(Upload id)
     *
     * @param access_token
     * @return
     * @throws IOException
     */
    private String getUploadID(String access_token) throws IOException {
        String url = "https://www.googleapis.com/youtube/v3/channels?part=contentDetails";
        String mine = "true";
        String urlVideos = url + "&mine=" + mine + "&access_token=" + access_token;
        URL obj = new URL(urlVideos);
        Log.d(TAG, " urlVideos : " + urlVideos);
        Log.d(TAG, " access_token : " + access_token);
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) obj.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // optional default is GET
        con.setRequestMethod("GET");

        //add request header
//        con.setRequestProperty("User-Agent", USER_AGENT);
        int responseCode = 0;
        String responseMessage = "";
        try {
            responseCode = con.getResponseCode();
            responseMessage = con.getResponseMessage();
            Log.d(TAG, "content value: " + con.getContent() + " responseCode: " + responseCode + " responseMessage: " + responseMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        result = response.toString();
        //print result
        Log.d(TAG, " Response containing content details: " + result);
        return result;
    }

    class UploadIdAsyncTask extends AsyncTask<String, Void, String> {


        @Override
        protected String doInBackground(String... strings) {

            String s = "";
            token = strings[0];
            try {
                s = getUploadID(strings[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return s;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (!token.isEmpty() && !s.isEmpty())
                new CustomAsyncTask().execute(s);
        }
    }

}
