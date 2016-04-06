package com.clevertap.demo;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import java.util.ArrayList;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.mobileconnectors.lambdainvoker.LambdaInvokerFactory;

import com.clevertap.demo.lambda.ILambdaInvoker;

import org.json.JSONObject;


public class MainActivity extends AppCompatActivity implements SyncListener,
        PersonalityTypeFormFragment.OnFragmentInteractionListener,
        SettingsFragment.OnFragmentInteractionListener,
        FragmentManager.OnBackStackChangedListener {

    private CleverTapAPI clevertap;
    private ILambdaInvoker lambda;

    private String currentPersonalityType;
    private String currentQuoteId;
    private String currentQuote;
    private String currentAuthor;
    Boolean runningQuoteFromIntent = false;
    Boolean showQuoteOnResume = false;
    Boolean launched = false;
    Boolean initialSyncComplete = false;

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

        String personalityType = null;
        if (clevertap != null) {
            personalityType = (String ) clevertap.profile.getProperty("personalityType");
            Log.d("PR_GET_TYPE", personalityType != null ? personalityType : "is null");
        }

        if (personalityType != null) {
            currentPersonalityType = personalityType;
            setTimeZone();
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

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                launch();
            }
        }, 15000);

    }


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {

        super.onResume();
        if(showQuoteOnResume) {
            showQuoteFragment(currentQuote);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        runningQuoteFromIntent = false;
    }

    // CleverTap

    private void initCleverTap() {

        try {
            // initialize CleverTap
            CleverTapAPI.setDebugLevel(1);
            clevertap = CleverTapAPI.getInstance(getApplicationContext());
            clevertap.enablePersonalization();
            clevertap.setSyncListener(this);

        } catch (CleverTapMetaDataNotFoundException | CleverTapPermissionsNotSatisfied e) {
            // handle appropriately
            e.printStackTrace();
        }
    }

    // SyncListener

    public void profileDidInitialize(String CleverTapID){
        Log.d("PR_INITIALIZED", CleverTapID);
        Log.d("PR_CT_ID", clevertap.getCleverTapID());

        if (clevertap.profile.getProperty("personalityType") != null) {
            launch();
        }
    }

    public void profileDataUpdated(JSONObject updates) {

        Log.d("PR_UPDATES", updates.toString());
        JSONObject profileUpdates = (JSONObject) updates.opt("profile");

        if(profileUpdates == null) {
            return ;
        }
        Boolean needUpdate = !initialSyncComplete || (profileUpdates.opt("quoteId") != null || profileUpdates.opt("personalityType") != null);

        if(!needUpdate) {
            return ;
        }

        initialSyncComplete = true;

        launch();

    }

    private void launch() {

        if(launched) return ;

        launched = true;

        String personalityType = (String ) clevertap.profile.getProperty("personalityType");
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
    public void setInitialProfile(String personalityType) {

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

        HashMap<String, Object> event = new HashMap<String, Object>();
        event.put("value", personalityType);
        clevertap.event.push("chosePersonalityType", event);
    }

    private void setTimeZone() {
        TimeZone tz = TimeZone.getDefault();
        Date now = new Date();
        int offsetFromUtc = tz.getOffset(now.getTime()) / 1000;
        int hours = offsetFromUtc / 3600;

        HashMap<String, Object> profileUpdate = new HashMap<String, Object>();
        profileUpdate.put("timeZone", "UTC" + hours);
        clevertap.profile.push(profileUpdate);
    }

    // sets the do not disturb status:  pass false to prevent communications
    public void setPushEnabled(Boolean on) {
        if (clevertap == null) {
            return;
        }
        HashMap<String, Object> profileUpdate = new HashMap<String, Object>();
        profileUpdate.put("canPush", on);
        clevertap.profile.push(profileUpdate);
    }

    // sets the do not disturb status:  pass false to prevent communications
    public void setEmailEnabled(Boolean on) {
        if (clevertap == null) {
            return;
        }
        HashMap<String, Object> profileUpdate = new HashMap<String, Object>();
        profileUpdate.put("canEmail", on);
        clevertap.profile.push(profileUpdate);
    }

    public Boolean getPushEnabled() {
        if (clevertap == null) {
            return false;
        }

        Object _canPush = clevertap.profile.getProperty("canPush");
        return (_canPush != null) ? Boolean.valueOf(_canPush.toString()) : true;
    }

    public Boolean getEmailEnabled() {
        if (clevertap == null) {
            return false;
        }

        Object _canEmail = clevertap.profile.getProperty("canEmail");
        return (_canEmail != null) ? Boolean.valueOf(_canEmail.toString()) : false;
    }

    public String getEmailAddress() {
        if (clevertap == null) {
            return null;
        }

        return (String ) clevertap.profile.getProperty("Email");
    }

    public String getPersonalityType() {
        if (clevertap == null) {
            return null;
        }

        return  (String ) clevertap.profile.getProperty("personalityType");

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

        if(clevertap == null || email == null) {
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

        if (clevertap == null) {
            return false;
        }

        Object _hasSeen = clevertap.profile.getProperty("hasSeenInstructions");
        return (_hasSeen != null) ? Boolean.valueOf(_hasSeen.toString()) : false;

    }

    public void setHasSeenInstructions(Boolean hasSeen) {
        if (clevertap == null) {
            return;
        }

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

        if(workingIndicator == null) {
            workingIndicator = new ProgressDialog(MainActivity.this);
            workingIndicator.setMessage("Fetching Quote...");
            workingIndicator.setCancelable(false);
            workingIndicator.setIndeterminate(true);
        }

        workingIndicator.setTitle("Your type is " + personalityType);
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
            String quoteId = clevertap.profile.getProperty("quoteId").toString();
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

        transaction.commitAllowingStateLoss();
    }

    public void onFragmentInteractionPersonalityTypeForm(String personalityType) {

        Boolean forceReset = false;
        if (!personalityType.equals(currentPersonalityType)) {
            currentPersonalityType = personalityType;
            setInitialProfile(personalityType);
            forceReset = true;
        }

        displayLatestQuote(forceReset);
    }

    private void showQuoteFragment(String quote) {

        if(workingIndicator != null) {
            workingIndicator.hide();
        }

        try {
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
        catch(IllegalStateException e) {
            showQuoteOnResume = true;
            return ;
        }

        showQuoteOnResume = false;

        showSettingsButton(true);
        QuoteFragment quoteFragment = QuoteFragment.newInstance(quote);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, quoteFragment);
        transaction.commitAllowingStateLoss();
    }

    private void showSettingsFragment() {

        showSettingsButton(false);
        SettingsFragment settingsFragment = SettingsFragment.newInstance();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, settingsFragment);
        transaction.addToBackStack(SETTINGS_FRAG_TAG);
        transaction.commitAllowingStateLoss();
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
