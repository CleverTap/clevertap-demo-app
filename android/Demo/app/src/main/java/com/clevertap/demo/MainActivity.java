package com.clevertap.demo;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.content.Intent;
import android.text.TextUtils;
import android.os.AsyncTask;
import android.view.MenuInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.app.FragmentManager;
import android.content.DialogInterface.OnClickListener;

import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.SyncListener;
import com.clevertap.android.sdk.exceptions.CleverTapMetaDataNotFoundException;
import com.clevertap.android.sdk.exceptions.CleverTapPermissionsNotSatisfied;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.Map;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.mobileconnectors.lambdainvoker.LambdaInvokerFactory;

import com.clevertap.demo.lambda.ILambdaInvoker;

import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity implements SyncListener,
        PersonalityTypeFormFragment.OnFragmentInteractionListener,
        SettingsFragment.OnFragmentInteractionListener, FragmentManager.OnBackStackChangedListener {

    private CleverTapAPI clevertap;
    private ILambdaInvoker lambda;

    private String currentPersonalityType;
    private String currentQuoteId;
    private String currentQuote;
    private String currentAuthor;
    private ArrayList<String> lastQuotesViewed;
    Boolean runningQuoteFromIntent = false;

    private static final String QUOTE_FRAG_TAG = "quoteFragTag";
    private static final String PT_FORM_FRAG_TAG = "ptFormFragTag";
    private static final String SETTINGS_FRAG_TAG = "settingsFragTag";

    private Menu cachedMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initCleverTap();

        // webservice access via AWS SDK
        initWebService();

        inflateLastQuotesViewed();

        String personalityType = clevertap.profile.getProperty("personalityType");
        Log.d("PR_GET_TYPE", personalityType != null ? personalityType : "is null");
        if (personalityType != null) {
            displayLatestQuote();
        }

        if (savedInstanceState == null) {
            handleIntent(getIntent());
        }

        // fragment nav
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.addOnBackStackChangedListener(this);

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        runningQuoteFromIntent = false;
        persistQuotesViewed();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }


    // CleverTap

    // SyncListener
    public void profileDataUpdated(JSONObject updates) {
        String personalityType = clevertap.profile.getProperty("personalityType");
        Log.d("PR_GET_TYPE", personalityType != null ? personalityType : "personality type is null");

        if (personalityType == null) {
            showPersonalityTypeFormFragment();
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!runningQuoteFromIntent) {
                        displayLatestQuote();
                    }

                }
            });
        }
    }

    private void initCleverTap() {
        try {
            // initialize CleverTap
            CleverTapAPI.setDebugLevel(1277182231);
            //CleverTapAPI.setDebugLevel(1);
            clevertap = CleverTapAPI.getInstance(getApplicationContext());
            clevertap.setSyncListener(this);

        } catch (CleverTapMetaDataNotFoundException | CleverTapPermissionsNotSatisfied e) {
            // handle appropriately
            e.printStackTrace();
        }
    }

    public void setProfile(String personalityType) {

        if (clevertap == null || personalityType == null) {
            return;
        }

        currentPersonalityType = personalityType;

        TimeZone tz = TimeZone.getDefault();
        Date now = new Date();
        int offsetFromUtc = tz.getOffset(now.getTime()) / 1000;
        int hours = offsetFromUtc / 3600;

        HashMap<String, Object> profileUpdate = new HashMap<String, Object>();
        profileUpdate.put("personalityType", personalityType);
        profileUpdate.put("timeZone", "UTC" + hours);

        clevertap.profile.push(profileUpdate);
    }

    // sets the do not disturb status:  pass false to prevent communications
    public void setPushAvailability(Boolean on) {
        HashMap<String, Object> profileUpdate = new HashMap<String, Object>();
        profileUpdate.put("canPush", on);
        clevertap.profile.push(profileUpdate);
    }

    // sets the do not disturb status:  pass false to prevent communications
    public void setEmailAvailability(Boolean on) {
        HashMap<String, Object> profileUpdate = new HashMap<String, Object>();
        profileUpdate.put("canEmail", on);
        clevertap.profile.push(profileUpdate);
    }

    private void checkSetProfileQuoteId(String quoteId) {

        // only set it if it doesn't already exist on the profile
        if (clevertap == null || quoteId == null) {
            return;
        }

        if (clevertap.profile.getProperty("quoteId") != null) {
            return;
        }

        HashMap<String, Object> profileUpdate = new HashMap<String, Object>();
        profileUpdate.put("quoteId", quoteId);

        clevertap.profile.push(profileUpdate);
    }

    // webservice is via an AWS lambda function

    private void initWebService() {
        // Initialize the Amazon Cognito credentials provider
        final String CognitoIdentityPoolId = "us-east-1:1a357948-2716-4c63-abdf-2711d9c5cefe";

        CognitoCachingCredentialsProvider cognitoProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                CognitoIdentityPoolId, // Identity Pool ID
                Regions.US_EAST_1 // Region
        );

        // Create LambdaInvokerFactory, to be used to instantiate the Lambda proxy.
        LambdaInvokerFactory factory = new LambdaInvokerFactory(this.getApplicationContext(),
                Regions.US_EAST_1, cognitoProvider);

        // Create the Lambda proxy object with a default Json data binder.
        lambda = factory.build(ILambdaInvoker.class);
    }

    // ping the lambda function
    @SuppressWarnings("unchecked")
    private void pingLambda() {
        Map event = new HashMap();
        event.put("operation", "ping");
        // The Lambda function invocation results in a network call.
        // Make sure it is not called from the main thread.
        new AsyncTask<Map, Void, String>() {
            @Override
            protected String doInBackground(Map... params) {
                // invoke "ping"; method. In case it fails, it will throw a
                // LambdaFunctionException.
                try {
                    return lambda.ping(params[0]);
                } catch (Exception e) {
                    Log.e("Lambda Tag", e.getLocalizedMessage());
                    return null;

                }
            }

            @Override
            protected void onPostExecute(String result) {
                if (result == null) {
                    return;
                }

                Toast.makeText(MainActivity.this, "AWS lambda ping success", Toast.LENGTH_LONG).show();
            }
        }.execute(event);
    }

    // quote handling

    @SuppressWarnings("unchecked")
    private void fetchQuote(final String quoteId) {
        // The Lambda function invocation results in a network call.
        // Make sure it is not called from the main thread.
        Map event = new HashMap();

        if (quoteId == null) {
            String personalityType = clevertap.profile.getProperty("personalityType");
            Log.d("PR_GET_TYPE", personalityType != null ? personalityType : "is null");
            if (personalityType != null) {
                currentPersonalityType = personalityType;
            }
            event.put("operation", "fetchQuoteForType");
            event.put("p_type", currentPersonalityType);
        } else {
            event.put("operation", "fetchQuoteFromId");
            event.put("quoteId", quoteId);
        }

        new AsyncTask<Map, Void, Map>() {
            @Override
            protected Map doInBackground(Map... params) {
                try {
                    return lambda.fetchQuote(params[0]);
                } catch (Exception e) {
                    Log.e("Lambda TAG", e.getLocalizedMessage());
                    return null;

                }
            }

            @Override
            protected void onPostExecute(Map result) {
                if (result == null) {
                    return;
                }
                String quote = null;
                String _quoteId = null;

                Log.d("Lambda TAG", result.toString());

                try {
                    quote = (String) result.get("quote");
                    _quoteId = (String) result.get("quote_id");
                } catch (Exception e) {
                    Log.d("ERROR_TAG", e.getLocalizedMessage());
                }

                if (quote == null) {
                    return;
                }

                updateViewsForQuote(_quoteId, quote, (String) result.get("by"));

            }
        }.execute(event);
    }

    private void showPersonalityTypeFormFragment() {
        showSettingsButton(false);
        PersonalityTypeFormFragment ptFragment = PersonalityTypeFormFragment.newInstance(currentPersonalityType);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, ptFragment);
        transaction.addToBackStack(PT_FORM_FRAG_TAG);
        transaction.commit();
    }

    public void onFragmentInteractionPersonalityTypeForm(String personalityType) {
        setProfile(personalityType);
        displayLatestQuote();
    }

    @SuppressWarnings("unchecked")
    private void displayLatestQuote() {

        String quoteId = clevertap.profile.getProperty("quoteId");

        Log.d("PR_GET_QOT_ID", quoteId != null ? quoteId : "is null");

        if (currentQuoteId != null && quoteId != null && quoteId.equals(currentQuoteId)) {
            updateViewsForQuote(currentQuoteId, currentQuote, currentAuthor);
        } else {
            fetchQuote(quoteId);
        }
    }

    private void updateViewsForQuote(String quoteId, String quote, String author) {

        if (quote == null) {
            return;
        }

        currentQuoteId = quoteId;
        currentQuote = quote;

        currentAuthor = author != null ? author : "Unknown";

        quote += "\n\n" + currentAuthor;
        showQuoteFragment(quote);

        updateLastQuotesViewed(quoteId);

        checkSetProfileQuoteId(quoteId);
    }

    private void showQuoteFragment(String quote) {
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        showSettingsButton(true);
        QuoteFragment quoteFragment = QuoteFragment.newInstance(quote);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, quoteFragment);
        transaction.commit();
    }

    private void inflateLastQuotesViewed() {
        String quotesViewedString = clevertap.profile.getProperty("lastQuotesViewed");
        quotesViewedString = quotesViewedString != null ? quotesViewedString : "";
        lastQuotesViewed = new ArrayList<String>(java.util.Arrays.asList(quotesViewedString.split(":")));
        Log.d("PR_GET_QUOTES_VIEWED", lastQuotesViewed.toString());
    }

    private void persistQuotesViewed() {
        String quotesViewedString = TextUtils.join(":", lastQuotesViewed);
        HashMap<String, Object> profileUpdate = new HashMap<String, Object>();
        profileUpdate.put("lastQuotesViewed", quotesViewedString);
        Log.d("PR_SET_QUOTES_VIEWED", quotesViewedString);

        clevertap.profile.push(profileUpdate);
    }

    private void updateLastQuotesViewed(String quoteId) {
        if (quoteId == null || lastQuotesViewed == null || lastQuotesViewed.contains(quoteId)) {
            return;

        }
        lastQuotesViewed.add(0, quoteId);
        if (lastQuotesViewed.size() >= 10) {
            lastQuotesViewed.subList(0, 9).clear();
        }

        Log.d("QUOTES_VIEWED_UPDATE", lastQuotesViewed.toString());
    }


    // Settings

    private void showSettingsFragment() {
        showSettingsButton(false);
        SettingsFragment settingsFragment = SettingsFragment.newInstance();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, settingsFragment);
        transaction.addToBackStack(SETTINGS_FRAG_TAG);
        transaction.commit();
    }

    public void onFragmentInteractionSettings(String action) {
        if(action == null) {
            displayLatestQuote();
            return ;
        }

        if(action.equals("showPTForm")) {
            showPersonalityTypeFormFragment();
        } else {
            displayLatestQuote();
        }
    }

    // intents/deep links

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);

    }

    private void handleIntent(Intent intent) {

        if (intent.getAction().equals(Intent.ACTION_VIEW)) {
            Uri data = intent.getData();
            if (data != null) {
                Log.d("INTENT_URI", data.toString());
                handleDeepLink(data);
            }
        }
    }

    // handle deep links
    private void handleDeepLink(Uri data) {
        //To get scheme.
        String scheme = data.getScheme();

        if (scheme.equals("ctdemo")) {
            String host = data.getHost();
            if (host.equals("quote")) {
                // get path components
                List<String> pathSegments = data.getPathSegments();
                String quoteId = pathSegments.get(0);
                runningQuoteFromIntent = true;
                fetchQuote(quoteId);
            }

        }

    }

    // options menu

    private void showSettingsButton(Boolean show) {
        try {
            MenuItem item = cachedMenu.findItem(R.id.action_settings);
            if(item != null) {
                item.setVisible(show).setEnabled(show);
            }
        } catch (Exception e) {
            // no-op
        }



    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        cachedMenu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.


        int id = item.getItemId();

        if (id == android.R.id.home) {
            getSupportFragmentManager().popBackStack();
            return true;
        }

        if (id == R.id.action_settings) {
            showSettingsFragment();
            showSettingsButton(false);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackStackChanged() {
        int stackHeight = getSupportFragmentManager().getBackStackEntryCount();
        if (stackHeight > 0) {
            try {
                getSupportActionBar().setHomeButtonEnabled(true);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            try {
                showSettingsButton(true);
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                getSupportActionBar().setHomeButtonEnabled(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}