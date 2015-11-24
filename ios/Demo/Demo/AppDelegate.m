//
//  AppDelegate.m
//  Demo
//
//  Created by pwilkniss on 11/2/15.
//  Copyright © 2015 CleverTap. All rights reserved.
//

#import "AppDelegate.h"
#import <CleverTapSDK/CleverTap.h>
#import <CleverTapSDK/CleverTapSyncDelegate.h>
#import <AWSCore/AWSCore.h>
#import <AWSCognito/AWSCognito.h>
#import <AWSLambda/AWSLambda.h>
#import <CloudKit/CloudKit.h>

void(^fetchedUserCKRecord)(CKRecord *record, NSError *error);

@interface AppDelegate () <CleverTapSyncDelegate> {
    CleverTap *clevertap;
}

@property(nonatomic,retain) NSString *iCloudUserID;
@property(nonatomic,retain) CKRecordID *myRecordId;
@property(nonatomic,retain) CKDiscoveredUserInfo *me;
@property(nonatomic,retain) CKRecord *myRecord;

@end

@implementation AppDelegate


- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    // Override point for customization after application launch.
    
#ifdef DEBUG
    [CleverTap setDebugLevel:1277182231];
    //[CleverTap setDebugLevel:1];
#endif
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(didReceiveCleverTapProfileDidChangeNotification:)
                                                 name:CleverTapProfileDidChangeNotification object:nil];
    
    
    clevertap = [CleverTap autoIntegrate];
    [clevertap setSyncDelegate:self];
    
    NSDate *lastTimeAppLaunched = [[NSDate alloc] initWithTimeIntervalSince1970:[clevertap userGetPreviousVisitTime]];
    NSLog(@"last App Launch %@", lastTimeAppLaunched);
    
    // register for push notifications
    
    UIUserNotificationType types = UIUserNotificationTypeBadge |
    UIUserNotificationTypeSound | UIUserNotificationTypeAlert;
    
    UIUserNotificationSettings *notificationSettings =
    [UIUserNotificationSettings settingsForTypes:types categories:nil];
    
    [[UIApplication sharedApplication] registerUserNotificationSettings:notificationSettings];
    
    [[UIApplication sharedApplication] registerForRemoteNotifications];
    
    // init aws
    AWSCognitoCredentialsProvider *credentialsProvider = [[AWSCognitoCredentialsProvider alloc] initWithRegionType:AWSRegionUSEast1 identityPoolId:@"us-east-1:1a357948-2716-4c63-abdf-2711d9c5cefe"];
    
    AWSServiceConfiguration *configuration = [[AWSServiceConfiguration alloc] initWithRegion:AWSRegionUSEast1
                                                                         credentialsProvider:credentialsProvider];
    AWSServiceManager.defaultServiceManager.defaultServiceConfiguration = configuration;
    
    // check for notification
    NSDictionary *notification = [launchOptions objectForKey:UIApplicationLaunchOptionsRemoteNotificationKey];
    
    if(notification) {
        [self application:application didReceiveRemoteNotificationFromLaunch:notification];
    }
    
    [self setProfile];
    
    [self updateQuote];
    
    return YES;
}

# pragma mark CT Profile

-(void)didReceiveCleverTapProfileDidChangeNotification:(NSNotification*)notification {
    NSDictionary *updates = notification.userInfo;
    NSLog(@"didReceiveCleverTapProfileDidChangeNotification called with %@", updates);
    [self parseAndHandleCleverTapProfileUpdates:updates];
}

# pragma mark Delegate

-(void)parseAndHandleCleverTapProfileUpdates:(NSDictionary*)updates {
    NSDictionary *profile = [updates objectForKey:@"profile"];
    //NSDictionary *events = [updates objectForKey:@"events"];
    
    if(profile) {
        NSDictionary *quoteId = [profile objectForKey:@"quoteId"];
        if(quoteId) {
            NSString *newQuoteId = [quoteId objectForKey:@"newValue"];
            if(newQuoteId) {
                [self fetchQuote:newQuoteId];
            }
        }
    }
}

- (void)profileDataUpdated:(NSDictionary*)updates {
    NSLog(@"profileDataUpdated called with %@", updates);
    [self parseAndHandleCleverTapProfileUpdates:updates];
}

-(void)updateQuote {
    NSString *quoteId = [clevertap profileGet:@"quoteId"];
    [self fetchQuote:quoteId];
}

-(void)setProfile {
    
    
    NSInteger seconds = [[NSTimeZone localTimeZone] secondsFromGMT];
    NSInteger hoursOffset = seconds/3600;
    NSLog(@"hours is %ld", (long) hoursOffset);
    
    
    NSMutableDictionary *profile = [NSMutableDictionary dictionaryWithObjects:@[@"earth", [NSString stringWithFormat:@"UTC%ld", hoursOffset], [UIDevice currentDevice].name] forKeys:@[@"personalityType", @"timeZone", @"deviceName"]];
    
    NSLog(@"profile is %@", profile);
    [clevertap profilePush:profile];
}

-(void)fetchQuote:(NSString*)quoteId {
    AWSLambdaInvoker *lambdaInvoker = [AWSLambdaInvoker defaultLambdaInvoker];
    NSDictionary *parameters;
    
    if(quoteId) {
         parameters = @{@"operation" : @"fetchQuoteFromId",
                                     @"quoteId"   : quoteId,
                                     @"isError"   : @NO};

    } else {
        NSString *type = [clevertap profileGet:@"personalityType"];
        if(type) {
            parameters = @{@"operation" : @"fetchQuoteForType",
                           @"p_type"   : type,
                           @"isError"   : @NO};
        }
        
    }
    
    if(!parameters) return ;
    
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
                [self displayQuote:task.result];
            });
        }
        return nil;
    }];
}


-(void)displayQuote:(NSDictionary*)quote {

    UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"QOTD"
                                                    message:[quote objectForKey:@"quote"]
                                                   delegate:self
                                          cancelButtonTitle:@"Close"
                                          otherButtonTitles:nil];
    [alert show];
}

#pragma mark URL handling
- (BOOL)application:(UIApplication *)application
            openURL:(NSURL *)url
  sourceApplication:(NSString *)sourceApplication
         annotation:(id)annotation {
    
    NSLog(@"open url %@", url.description);
    
    NSString *scheme = [url scheme];
    
    if([scheme isEqualToString:@"ctdemo"]) {
        NSString *host = [url host];
        NSString *path = [[url path] stringByReplacingOccurrencesOfString:@"/" withString:@""];
        if([host isEqualToString:@"quote"]) {
            [self fetchQuote:path];
            return YES;
        }
    }
    
    return NO;
}

# pragma mark Push Notifications

- (void)application:(UIApplication *)application
didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken {
    NSLog(@"Lifecycle: application:didRegisterForRemoteNotificationsWithDeviceToken:");
    NSLog(@"APNs device token %@", deviceToken);
    
}

-(void)application:(UIApplication *)application didFailToRegisterForRemoteNotificationsWithError:(NSError *)error {
    NSLog(@"did fail to register for remote notification: %@", error);
}


-(void) application:(UIApplication *)application didReceiveRemoteNotificationFromLaunch:(NSDictionary *)userInfo {
    // don't do anything if we are active when the notification is received
    if (application.applicationState == UIApplicationStateActive) return ;
    
    __block NSDictionary *_userInfo = userInfo;
    
    dispatch_time_t delay = dispatch_time(DISPATCH_TIME_NOW, NSEC_PER_SEC * 1.0);
    dispatch_after(delay, dispatch_get_main_queue(), ^(void){
         [self application:application didReceiveRemoteNotification:_userInfo];
    });
   
}

- (void) application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo {
    NSLog(@"I received a push notification!");
    NSLog(@"didReceiveRemoteNotification: UserInfo: %@", userInfo);
    
    if (application.applicationState == UIApplicationStateActive) return ;
    
    NSString *quoteId = [userInfo objectForKey:@"q"];
    if(quoteId) {
         [self fetchQuote:quoteId];
    }
}

- (void) application:(UIApplication *)application didReceiveLocalNotification:(UILocalNotification *)notification {
    
    NSLog(@"I received a local notification!");
    NSLog(@"didReceiveLocalNotification: UserInfo: %@", notification);
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
