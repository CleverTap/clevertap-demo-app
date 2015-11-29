package com.clevertap.demo;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.MenuInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.app.FragmentManager;
import android.app.ProgressDialog;

import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.SyncListener;
import com.clevertap.android.sdk.exceptions.CleverTapMetaDataNotFoundException;
import com.clevertap.android.sdk.exceptions.CleverTapPermissionsNotSatisfied;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.Map;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.mobileconnectors.lambdainvoker.LambdaInvokerFactory;

import com.clevertap.demo.lambda.ILambdaInvoker;

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
    Boolean runningQuoteFromIntent = false;

    private static final String QUOTE_FRAG_TAG = "quoteFragTag";
    private static final String PT_FORM_FRAG_TAG = "ptFormFragTag";
    private static final String SETTINGS_FRAG_TAG = "settingsFragTag";

    private Menu cachedMenu;

    ProgressDialog workingIndicator;

    // lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // initialize CleverTap
        initCleverTap();

        // webservice access via AWS SDK
        initWebService();

        String personalityType = clevertap.profile.getProperty("personalityType");
        Log.d("PR_GET_TYPE", personalityType != null ? personalityType : "is null");

        if (personalityType != null) {
            currentPersonalityType = personalityType;
            displayLatestQuote(false);
        } else {
            // no personality type set and we have seen the form; reshow the form
            if(getHasSeenInstructions()) {
                showPersonalityTypeFormFragment();
            }
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
    }

    // CleverTap

    // SyncListener
    public void profileDataUpdated(JSONObject updates) {

        String personalityType = clevertap.profile.getProperty("personalityType");
        currentPersonalityType = personalityType;
        Log.d("PR_GET_TYPE", personalityType != null ? personalityType : "personality type is null");

        if (personalityType == null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!runningQuoteFromIntent) {
                        showPersonalityTypeFormFragment();
                    }
                }
            });

        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!runningQuoteFromIntent) {
                        displayLatestQuote(false);
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
    public void setPushEnabled(Boolean on) {

        HashMap<String, Object> profileUpdate = new HashMap<String, Object>();
        profileUpdate.put("canPush", on);
        clevertap.profile.push(profileUpdate);
    }

    // sets the do not disturb status:  pass false to prevent communications
    public void setEmailEnabled(Boolean on) {

        HashMap<String, Object> profileUpdate = new HashMap<String, Object>();
        profileUpdate.put("canEmail", on);
        clevertap.profile.push(profileUpdate);
    }

    public Boolean getPushEnabled() {

        String canPush = clevertap.profile.getProperty("canPush");
        if(canPush == null) {
            // push defaults to enabled
            canPush = "true";
        }

        return Boolean.valueOf(canPush);
    }

    public Boolean getEmailEnabled() {
        String canEmail = clevertap.profile.getProperty("canEmail");
        if(canEmail == null) {
            // email defaults to disabled
            canEmail = "false";
        }

        return Boolean.valueOf(canEmail);
    }

    public String getEmailAddress() {
        return clevertap.profile.getProperty("Email");
    }

    public String getPersonalityType() {
        return clevertap.profile.getProperty("personalityType");

    }

    public int getPersonalityTypeColorId() {

        int colorId = 0;
        String pType = getPersonalityType();

        if(pType != null) {
            switch (pType) {
                case "earth":
                    colorId = ContextCompat.getColor(getApplicationContext(), R.color.earthseekbar_color);
                    break;
                case "fire":
                    colorId = ContextCompat.getColor(getApplicationContext(), R.color.fireseekbar_color);
                    break;
                case "metal":
                    colorId = ContextCompat.getColor(getApplicationContext(), R.color.metalseekbar_color);
                    break;
                case "water":
                    colorId = ContextCompat.getColor(getApplicationContext(), R.color.waterseekbar_color);
                    break;
                case "wood":
                    colorId = ContextCompat.getColor(getApplicationContext(), R.color.woodseekbar_color);
                    break;

            }
        }

        return colorId;

    }

    public void setEmailAddress(String email) {

        if(email == null) {
            return ;
        }

        HashMap<String, Object> profileUpdate = new HashMap<String, Object>();
        profileUpdate.put("Email", email);
        profileUpdate.put("canEmail", true);
        clevertap.profile.push(profileUpdate);
    }

    private void checkSetProfileQuoteId(String quoteId, Boolean force) {

        // only set it if it doesn't already exist on the profile
        if (clevertap == null || quoteId == null) {
            return;
        }

        if (!force && clevertap.profile.getProperty("quoteId") != null) {
            return;
        }

        HashMap<String, Object> profileUpdate = new HashMap<String, Object>();
        profileUpdate.put("quoteId", quoteId);

        clevertap.profile.push(profileUpdate);
    }

    public Boolean getHasSeenInstructions() {
        String hasSeen = clevertap.profile.getProperty("hasSeenInstructions");
        if(hasSeen == null) {
            hasSeen = "false";
        }
        return Boolean.valueOf(hasSeen);
    }

    public void setHasSeenInstructions(Boolean hasSeen) {
        HashMap<String, Object> profileUpdate = new HashMap<String, Object>();
        profileUpdate.put("hasSeenInstructions", hasSeen);
        clevertap.profile.push(profileUpdate);
    }

    // webservice is via AWS lambda function

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

    // quote handling
    @SuppressWarnings("unchecked")
    private void fetchQuote(final String quoteId, String personalityType, final Boolean forceReset) {
        // The Lambda function invocation results in a network call.
        // Make sure it is not called from the main thread.

        workingIndicator = new ProgressDialog(MainActivity.this);
        workingIndicator.setTitle("Your type is "+personalityType);
        workingIndicator.setMessage("Fetching Quote...");
        workingIndicator.setCancelable(false);
        workingIndicator.setIndeterminate(true);
        workingIndicator.show();

        Map event = new HashMap();

        if(quoteId == null && personalityType == null) {
            return ;
        }

        if (quoteId == null) {
            event.put("operation", "fetchQuoteForType");
            event.put("p_type", personalityType);
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
                    if(workingIndicator != null) {
                        workingIndicator.hide();
                    }
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

                updateViewsForQuote(_quoteId, quote, (String) result.get("by"), forceReset);

            }
        }.execute(event);
    }

    // UI/fragment handling

    @SuppressWarnings("unchecked")
    private void displayLatestQuote(Boolean forceReset) {

        if(forceReset) {
            fetchQuote(null, currentPersonalityType, true);

        } else {
            String quoteId = clevertap.profile.getProperty("quoteId");
            Log.d("PR_GET_QOT_ID", quoteId != null ? quoteId : "is null");

            if (currentQuoteId != null && quoteId != null && quoteId.equals(currentQuoteId)) {
                updateViewsForQuote(currentQuoteId, currentQuote, currentAuthor, forceReset);
            } else {
                fetchQuote(quoteId, currentPersonalityType, false);
            }
        }

    }

    private void updateViewsForQuote(String quoteId, String quote, String author, Boolean forceReset) {

        if (quote == null) {
            return;
        }

        currentQuoteId = quoteId;
        currentQuote = quote;

        currentAuthor = author != null ? author : "Unknown";

        quote += "\n\n" + currentAuthor;
        showQuoteFragment(quote);
        checkSetProfileQuoteId(quoteId, forceReset);
    }

    // fragments

    private void showPersonalityTypeFormFragment() {

        showSettingsButton(false);
        PersonalityTypeFormFragment ptFragment = PersonalityTypeFormFragment.newInstance();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, ptFragment);

        if(currentPersonalityType != null) {
            transaction.addToBackStack(PT_FORM_FRAG_TAG);
        }

        transaction.commit();
    }

    public void onFragmentInteractionPersonalityTypeForm(String personalityType) {

        Boolean forceReset = false;
        if (!personalityType.equals(currentPersonalityType)) {
            currentPersonalityType = personalityType;
            setProfile(personalityType);
            forceReset = true;
        }

        displayLatestQuote(forceReset);
    }

    private void showQuoteFragment(String quote) {

        if(workingIndicator != null) {
            workingIndicator.hide();
        }

        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        showSettingsButton(true);
        QuoteFragment quoteFragment = QuoteFragment.newInstance(quote);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, quoteFragment);
        transaction.commit();
    }

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
            displayLatestQuote(false);
            return ;
        }

        if(action.equals("showPTForm")) {
            showPersonalityTypeFormFragment();
        } else {
            displayLatestQuote(false);
        }
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
                fetchQuote(quoteId, currentPersonalityType, false);
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

        //start with the settings button hidden
        showSettingsButton(false);

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
}