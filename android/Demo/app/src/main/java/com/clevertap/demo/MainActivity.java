package com.clevertap.demo;

import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;

import android.os.AsyncTask;
import android.widget.Toast;
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

public class MainActivity extends AppCompatActivity {

    private CleverTapAPI ct = null;

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

        try {
            // CleverTap
            //CleverTapAPI.setDebugLevel(1277182231);
            CleverTapAPI.setDebugLevel(1);
            ct = CleverTapAPI.getInstance(getApplicationContext());
            ct.enablePersonalization();

            TimeZone tz = TimeZone.getDefault();
            Date now = new Date();
            int offsetFromUtc = tz.getOffset(now.getTime()) / 1000;
            int hours = offsetFromUtc/3600;

            //Log.d("LOGGER", Integer.toString(hours));

            HashMap<String, Object> profileUpdate = new HashMap<String, Object>();
            profileUpdate.put("personalityType", "metal");
            profileUpdate.put("timeZone", "UTC"+hours);

            ct.profile.push(profileUpdate);

        } catch (CleverTapMetaDataNotFoundException | CleverTapPermissionsNotSatisfied e) {
            // handle appropriately
            e.printStackTrace();
        }

        // Initialize the Amazon Cognito credentials provider
        CognitoCachingCredentialsProvider cognitoProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:1a357948-2716-4c63-abdf-2711d9c5cefe", // Identity Pool ID
                Regions.US_EAST_1 // Region
        );


        // Create LambdaInvokerFactory, to be used to instantiate the Lambda proxy.
        LambdaInvokerFactory factory = new LambdaInvokerFactory(this.getApplicationContext(),
                Regions.US_EAST_1, cognitoProvider);

        // Create the Lambda proxy object with a default Json data binder.
        lambda = factory.build(ILambdaInvoker.class);

        if(savedInstanceState == null) {
            handleIntent(getIntent());
        }

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

                // Display a quick message
                Toast.makeText(MainActivity.this,"Made contact with AWS lambda", Toast.LENGTH_LONG).show();
            }
        }.execute(event);
    }

    @SuppressWarnings("unchecked")
    private void fetchQuote(String quoteId) {
        // The Lambda function invocation results in a network call.
        // Make sure it is not called from the main thread.

        Map event = new HashMap();
        event.put("operation", "fetchQuoteFromId");
        event.put("quoteId", quoteId);

        new AsyncTask<Map, Void, String>() {
            @Override
            protected String doInBackground(Map... params) {
                // invoke "ping"; method. In case it fails, it will throw a
                // LambdaFunctionException.
                try {
                    return lambda.fetchQuoteFromId(params[0]);
                } catch (Exception e) {
                    Log.e("Lambda TAG",e.getLocalizedMessage());
                    return null;

                }
            }

            @Override
            protected void onPostExecute(String result) {
                if (result == null) {
                    return;
                }

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("QOTD")
                        .setMessage(result)
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

    @Override
    public void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        handleIntent(intent);

    }

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
