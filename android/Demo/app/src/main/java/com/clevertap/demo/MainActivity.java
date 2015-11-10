package com.clevertap.demo;

import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Toast;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;

import android.os.AsyncTask;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

import com.clevertap.android.sdk.CleverTapAPI;
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

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private CleverTapAPI clevertap;
    private ILambdaInvoker lambda;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        initCleverTap();

        // webservice access via AWS SDK
        initWebService();

        // TODO temp; replace with UI
        String personalityType = clevertap.profile.getProperty("personalityType");
        if(personalityType == null) {
            // TODO show UI here
            setProfile("water");
        }
        else {
            displayLatestQuote();
        }

        if(savedInstanceState == null) {
            handleIntent(getIntent());
        }

    }

    // CleverTap

    private void initCleverTap() {
        try {
            // initialize CleverTap
            CleverTapAPI.setDebugLevel(1277182231);
            //CleverTapAPI.setDebugLevel(1);
            clevertap = CleverTapAPI.getInstance(getApplicationContext());
            clevertap.enablePersonalization();

        } catch (CleverTapMetaDataNotFoundException | CleverTapPermissionsNotSatisfied e) {
            // handle appropriately
            e.printStackTrace();
        }
    }

    private void setProfile(String personalityType) {

        if(clevertap == null || personalityType == null) {
            return ;
        }

        TimeZone tz = TimeZone.getDefault();
        Date now = new Date();
        int offsetFromUtc = tz.getOffset(now.getTime()) / 1000;
        int hours = offsetFromUtc/3600;

        HashMap<String, Object> profileUpdate = new HashMap<String, Object>();
        profileUpdate.put("personalityType", personalityType);
        profileUpdate.put("timeZone", "UTC" + hours);

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
                    Log.e("Lambda Tag",e.getLocalizedMessage());
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

    @SuppressWarnings("unchecked")
    private void fetchQuote(String quoteId) {
        // The Lambda function invocation results in a network call.
        // Make sure it is not called from the main thread.
        Map event = new HashMap();

        if(quoteId == null) {
            String personalityType = clevertap.profile.getProperty("personalityType");
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
                    Log.e("Lambda TAG",e.getLocalizedMessage());
                    return null;

                }
            }

            @Override
            protected void onPostExecute(Map result) {
                if (result == null) {
                    return;
                }
                String quote = null;

                Log.d("Lambda TAG", result.toString());

                try {
                    quote = (String)result.get("quote");
                } catch (Exception e) {
                    Log.d("ERROR_TAG", e.getLocalizedMessage());
                }

                if(quote == null) {
                    return ;
                }

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("QOTD")
                        .setMessage(quote)
                        .setCancelable(false)
                        .setPositiveButton("ok", new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // whatever...
                            }
                        }).create().show();
            }
        }.execute(event);
    }

    @SuppressWarnings("unchecked")
    private void displayLatestQuote() {
        String quoteId = clevertap.profile.getProperty("quoteId");
        fetchQuote(quoteId);
    }

    // intents/deep links

    @Override
    public void onNewIntent(Intent intent){
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

        if(scheme.equals("ctdemo")) {
            //To get server name.
            String host = data.getHost();
            if(host.equals("quote")) {
                // get path components
                List<String> pathSegments = data.getPathSegments();
                String quoteId = pathSegments.get(0);
                fetchQuote(quoteId);
            }

        }

    }

    // TODO remove
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
