package com.clevertap.demo.lambda;

/**
 * Created by pwilkniss on 10/31/15.
 */
import com.amazonaws.mobileconnectors.lambdainvoker.LambdaFunction;
import java.util.Map;

public interface ILambdaInvoker {
    @LambdaFunction(functionName = "DemoAPI")
    String ping(Map event);
    @LambdaFunction(functionName = "DemoAPI")
    String fetchQuoteFromId(Map event);
}
