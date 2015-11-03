//
//  AppDelegate.m
//  Demo
//
//  Created by pwilkniss on 11/2/15.
//  Copyright © 2015 CleverTap. All rights reserved.
//

#import "AppDelegate.h"
#import <CleverTapSDK/CleverTap.h>
#import <AWSCore/AWSCore.h>
#import <AWSCognito/AWSCognito.h>
#import <AWSLambda/AWSLambda.h>

@interface AppDelegate ()

@end

@implementation AppDelegate


- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    // Override point for customization after application launch.
    
#ifdef DEBUG
    [CleverTap setDebugLevel:1277182231];
#endif
    
    [CleverTap notifyApplicationLaunchedWithOptions:launchOptions];
    [CleverTap enablePersonalization];
    
    NSDate *lastTimeAppLaunched = [[NSDate alloc] initWithTimeIntervalSince1970:[[CleverTap session] getPreviousVisitTime]];
    NSLog(@"last App Launch %@", lastTimeAppLaunched);
    
    // register for push notifications
    
    UIUserNotificationType types = UIUserNotificationTypeBadge |
    UIUserNotificationTypeSound | UIUserNotificationTypeAlert;
    
    UIUserNotificationSettings *notificationSettings =
    [UIUserNotificationSettings settingsForTypes:types categories:nil];
    
    [[UIApplication sharedApplication] registerUserNotificationSettings:notificationSettings];
    
    [[UIApplication sharedApplication] registerForRemoteNotifications];
    
    
    NSInteger seconds = [[NSTimeZone localTimeZone] secondsFromGMT];
    NSInteger hoursOffset = seconds/3600;
    NSLog(@"hours is %ld", (long) hoursOffset);
    
    NSDictionary *_profile = @{
                               @"personalityType":@"water",
                               @"timeZone": [NSString stringWithFormat:@"UTC%ld", hoursOffset],
                               @"Email":@"peter@clevertap.com"
                               };
    
    NSMutableDictionary *profile = [NSMutableDictionary dictionaryWithDictionary:_profile];
    
    NSLog(@"profile is %@", profile);
    [[CleverTap push] profile:profile];
    
    // init aws
    AWSCognitoCredentialsProvider *credentialsProvider = [[AWSCognitoCredentialsProvider alloc] initWithRegionType:AWSRegionUSEast1 identityPoolId:@"us-east-1:1a357948-2716-4c63-abdf-2711d9c5cefe"];
    
    AWSServiceConfiguration *configuration = [[AWSServiceConfiguration alloc] initWithRegion:AWSRegionUSEast1
                                                                         credentialsProvider:credentialsProvider];
    AWSServiceManager.defaultServiceManager.defaultServiceConfiguration = configuration;
    
    // test calling the AWS Lambda Demo API
    AWSLambdaInvoker *lambdaInvoker = [AWSLambdaInvoker defaultLambdaInvoker];
    
    NSDictionary *parameters = @{@"operation" : @"fetchQuoteFromId",
                                 @"quoteId"   : @"123456",
                                 @"isError"   : @NO};
    
    [[lambdaInvoker invokeFunction:@"DemoAPI"
                        JSONObject:parameters] continueWithBlock:^id(AWSTask *task) {
        if (task.error) {
            NSLog(@"Error: %@", task.error);
        }
        if (task.exception) {
            NSLog(@"Exception: %@", task.exception);
        }
        if (task.result) {
            NSLog(@"Result: %@", task.result);
            
            dispatch_async(dispatch_get_main_queue(), ^{
                //[self printOutputJSON:task.result];
            });
        }
        return nil;
    }];
    
    return YES;
}

#pragma mark URL handling

- (BOOL)application:(UIApplication *)application
            openURL:(NSURL *)url
  sourceApplication:(NSString *)sourceApplication
         annotation:(id)annotation {
    
    NSString *link = url.description;
    NSString *message = [NSString stringWithFormat:@"The app %@ asked me to open the URL: %@", sourceApplication, link];
    NSLog(@"%@", message);
    [CleverTap handleOpenURL:url sourceApplication:sourceApplication];
    
    return YES;
}

# pragma mark Push Notifications

- (void)application:(UIApplication *)application
didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken {
    NSLog(@"Lifecycle: application:didRegisterForRemoteNotificationsWithDeviceToken:");
    [CleverTap setPushToken:deviceToken];
    NSLog(@"APNs device token %@", deviceToken);
}

-(void)application:(UIApplication *)application didFailToRegisterForRemoteNotificationsWithError:(NSError *)error {
    NSLog(@"did fail to register for remote notification: %@", error);
}


- (void) application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo {
    NSLog(@"I received a push notification!");
    NSLog(@"didReceiveRemoteNotification: UserInfo: %@", userInfo);
    [CleverTap handleNotificationWithData:userInfo];
}

- (void) application:(UIApplication *)application didReceiveLocalNotification:(UILocalNotification *)notification {
    
    NSLog(@"I received a local notification!");
    NSLog(@"didReceiveLocalNotification: UserInfo: %@", notification);
    [CleverTap handleNotificationWithData:notification];
}

- (void)applicationWillResignActive:(UIApplication *)application {
    // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
    // Use this method to pause ongoing tasks, disable timers, and throttle down OpenGL ES frame rates. Games should use this method to pause the game.
}

- (void)applicationDidEnterBackground:(UIApplication *)application {
    // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
    // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
}

- (void)applicationWillEnterForeground:(UIApplication *)application {
    // Called as part of the transition from the background to the inactive state; here you can undo many of the changes made on entering the background.
}

- (void)applicationDidBecomeActive:(UIApplication *)application {
    // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
}

- (void)applicationWillTerminate:(UIApplication *)application {
    // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
}

@end
