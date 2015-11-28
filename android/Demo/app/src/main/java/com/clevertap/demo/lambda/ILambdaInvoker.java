package com.clevertap.demo.lambda;

/**
 * Created by pwilkniss on 10/31/15.
 */
import com.amazonaws.mobileconnectors.lambdainvoker.LambdaFunction;

import org.json.JSONObject;

import java.util.Map;

public interface ILambdaInvoker {
    @LambdaFunction(functionName = "DemoAPI")
    Map fetchQuote(Map event);
}
