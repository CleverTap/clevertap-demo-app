#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import "CleverTapEventDetail.h"
#import "CleverTapUTMDetail.h"

@protocol CleverTapSyncDelegate;

#pragma clang diagnostic push
#pragma ide diagnostic ignored "OCUnusedMethodInspection"

@interface CleverTap : NSObject

/* ------------------------------------------------------------------------------------------------------
 * Initialization
 */

/*!
@method

@abstract
Initializes and returns a singleton instance of the API.

@discussion
This method will set up a singleton instance of the CleverTap class, when you want to make calls to CleverTap
elsewhere in your code, you can use this singleton or call sharedInstance.

 */
+ (CleverTap *)sharedInstance;


/*!
 @method
 
 @abstract
 Auto integrates CleverTap and initializes and returns a singleton instance of the API.
 
 @discussion
 This method will auto integrate CleverTap to automatically handle device token registration and 
 push notification/url referrer tracking, and set up a singleton instance of the CleverTap class, 
 when you want to make calls to CleverTap elsewhere in your code, you can use this singleton or call sharedInstance.
 
 This is accomplished by proxying the AppDelegate and "inserting" a CleverTap AppDelegate
 behind the AppDelegate. The proxy will first call the AppDelegate and then call the CleverTap AppDelegate.
 
 */
+ (CleverTap *)autoIntegrate;


/* ------------------------------------------------------------------------------------------------------
 * User Profile/Action Events/Session API
 */


#pragma mark Profile API

/*!
 @method
 
 @abstract
 Set properties on the current user profile.
 
 @discussion
 Property keys must be NSString and values must be one of NSString, NSNumber, BOOL
 or NSDate.
 
 Keys are limited to 32 characters.
 Values are limited to 120 bytes.  
 Longer will be truncated.
 Maximum number of custom profile attributes is 63
 
 @param properties       properties dictionary
 */
- (void)profilePush:(NSDictionary *)properties;

/*!
 @method
 
 @abstract
 Convenience method to set the Facebook Graph User properties on the user profile.
 
 @discussion
 If you support social login via FB connect in your app and are using the Facebook library in your app,
 you can push a GraphUser object of the user.
 Be sure that you’re sending a GraphUser object of the currently logged in user.
 
 @param fbGraphUser       fbGraphUser Facebook Graph User object
 
 */
- (void)profilePushGraphUser:(id)fbGraphUser;

/*!
 @method
 
 @abstract
 Convenience method to set the Google Plus User properties on the user profile.
 
 @discussion
 If you support social login via Google Plus in your app and are using the Google Plus library in your app,
 you can set a GTLPlusPerson object on the user profile, after a successful login.
 
 @param googleUser       GTLPlusPerson object
 
 */
- (void)profilePushGooglePlusUser:(id)googleUser;

/*!
 @method
 
 @abstract
 Get a user profile property.
 
 @discussion
 Be sure to call enablePersonalization (typically once at app launch) prior to using this method.
 If the property is not available or enablePersonalization has not been called, this call will return nil.
 
 @param propertyName          property name
 
 */
- (id)profileGet:(NSString *)propertyName;

#pragma mark User Action Events API

/*!
 @method
 
 @abstract
 Record an event.
 
 Reserved event names: "Stayed", "Notification Clicked", "Notification Viewed", "UTM Visited", "Notification Sent", "App Launched", "wzrk_d", are prohibited.
 
 @param event           event name
 */
- (void)recordEvent:(NSString *)event;

/*!
 @method
 
 @abstract
 Records an event with properties.
 
 @discussion
 Property keys must be NSString and values must be one of NSString, NSNumber, BOOL or NSDate.
 Reserved event names: "Stayed", "Notification Clicked", "Notification Viewed", "UTM Visited", "Notification Sent", "App Launched", "wzrk_d", are prohibited.
 Keys are limited to 32 characters.
 Values are limited to 40 bytes.
 Longer will be truncated.
 Maximum number of event properties is 16.
 
 @param event           event name
 @param properties      properties dictionary
 */
- (void)recordEvent:(NSString *)event withProps:(NSDictionary *)properties;

/*!
 @method
 
 @abstract
 Records the special Charged event with properties.
 
 @discussion
 Charged is a special event in CleverTap. It should be used to track transactions or purchases.
 Recording the Charged event can help you analyze how your customers are using your app, or even to reach out to loyal or lost customers.
 The transaction total or subscription charge should be recorded in an event property called “Amount” in the chargeDetails param.
 Set your transaction ID or the receipt ID as the value of the "Charged ID" property of the chargeDetails param.
 
 You can send an array of purchased item dictionaries via the items param.
 
 Property keys must be NSString and values must be one of NSString, NSNumber, BOOL or NSDATE.
 Keys are limited to 32 characters.
 Values are limited to 40 bytes.
 Longer will be truncated.
 
 @param chargeDetails   charge transaction details dictionary
 @param items           charged items array
 */
- (void)recordChargedEventWithDetails:(NSDictionary *)chargeDetails andItems:(NSArray *)items;

/*!
 @method
 
 @abstract
 Record an error event.
 
 @param message           error message
 @param code              int error code
 */

- (void)recordErrorWithMessage:(NSString *)message andErrorCode:(int)code;

/*!
 @method
 
 @abstract
 Get the time of the first recording of the event.
 
 @param event           event name
 */
- (NSTimeInterval)eventGetFirstTime:(NSString *)event;

/*!
 @method
 
 @abstract
 Get the time of the last recording of the event.
 
 @param event           event name
 */

- (NSTimeInterval)eventGetLastTime:(NSString *)event;

/*!
 @method
 
 @abstract
 Get the number of occurrences of the event.
 
 @param event           event name
 */
- (int)eventGetOccurrences:(NSString *)event;

/*!
 @method
 
 @abstract
 Get the user's event history.
 
 @discussion
 Returns a dictionary of CleverTapEventDetail objects (eventName, firstTime, lastTime, occurrences), keyed by eventName.
 
 */
- (NSDictionary *)userGetEventHistory;

/*!
 @method
 
 @abstract
 Get the details for the event.
 
 @discussion
 Returns a CleverTapEventDetail object (eventName, firstTime, lastTime, occurrences)
 
 @param event           event name
 */
- (CleverTapEventDetail *)eventGetDetail:(NSString *)event;


#pragma mark Session API

/*!
 @method
 
 @abstract
 Get the elapsed time of the current user session.
 */
- (NSTimeInterval)sessionGetTimeElapsed;

/*!
 @method
 
 @abstract
 Get the utm referrer details for this user session.
 
 @discussion
 Returns a CleverTapUTMDetail object (source, medium and campaign).
 
 */
- (CleverTapUTMDetail *)sessionGetUTMDetails;

/*!
 @method
 
 @abstract
 Get the total number of visits by this user.
 */
- (int)userGetTotalVisits;

/*!
 @method
 
 @abstract
 Get the total screens viewed by this user.
 
 */
- (int)userGetScreenCount;

/*!
 @method
 
 @abstract
 Get the last prior visit time for this user.
 
 */
- (NSTimeInterval)userGetPreviousVisitTime;

/* ------------------------------------------------------------------------------------------------------
* Synchronization
*/


/**
 @abstract Posted when the CleverTap User Profile has changed in repsonse to a synchronization call to the CleverTap servers.
 
 @discussion 
 CleverTap provides a flexible notification system for informing applications when changes have occured
 to the CleverTap User Profile in response to synchronization activities.
 
 CleverTap leverages the NSNotification broadcast mechanism to notify your application when changes occur.
 Your application should observe CleverTapProfileDidChangeNotification in order to receive notifications.
 
 */

extern NSString *const CleverTapProfileDidChangeNotification;


/*!
 
 @method
 
 @abstract The `CleverTapSyncDelegate` protocol provides an additional/alternative method for
 notifying your application (the adopting delegate) about synchronization-related changes to the user profile.
 
 @see CleverTapSyncDelegate.h
 
 @discussion
 This sets the CleverTapSyncDelegate.
 
 @param delegate     an object conforming to the CleverTapSyncDelegate Protocol
 */

- (void)setSyncDelegate:(id <CleverTapSyncDelegate>)delegate;



/* ------------------------------------------------------------------------------------------------------
 * Notifications
 */

/*!
 @method
 
 @abstract
 Register the device to receive push notifications.
 
 @discussion
 This will associate the device token with the current user to allow push notifications to the user.
 
 @param pushToken     device token as returned from application:didRegisterForRemoteNotificationsWithDeviceToken:
 */
- (void)setPushToken:(NSData *)pushToken;

/*!
 @method
 
 @abstract
 Track and process a push notification based on its payload.
 
 @discussion
 By calling this method, CleverTap will automatically track user notification interaction for you.
 If the push notification contains a deep link, CleverTap will handle the call to application:openUrl: with the deep link.
 
 @param data         notification payload
 */
- (void)handleNotificationWithData:(id)data;

/*!
 @method
 
 @abstract
 Manually initiate the display of any pending in app notifications.
 
 */
- (void)showInAppNotificationIfAny;


/* ------------------------------------------------------------------------------------------------------
 * Referrer tracking
 */

/*!
 @method
 
 @abstract
 Track incoming referrers.
 
 @discussion
 By calling this method, CleverTap will automatically track incoming referrer utm details.

 
 @param url                     the incoming NSURL
 @param sourceApplication       the source application
 */
- (void)handleOpenURL:(NSURL *)url sourceApplication:(NSString *)sourceApplication;

/*!
 @method
 
 @abstract
 Manually track incoming referrers.
 
 @discussion
 Call this to manually track the utm details for an incoming install referrer.
 
 
 @param source                   the utm source
 @param medium                   the utm medium
 @param campaign                 the utm campaign
 */
- (void)pushInstallReferrerSource:(NSString *)source
                           medium:(NSString *)medium
                         campaign:(NSString *)campaign;




/* ------------------------------------------------------------------------------------------------------
 * Admin
 */

/*!
 @method
 
 @abstract
 Set the debug logging level
 
 @discussion
 0 = off, 1 = on
 
 @param level  the level to set (0 or 1)
 
 */
+ (void)setDebugLevel:(int)level;

/*!
 @method
 
 @abstract
 Change the CleverTap accountID and token
 
 @discussion
 Changes the CleverTap account associated with the app on the fly.  Should only used during testing.
 Instead, considering relying on the separate -Test account created for your app in CleverTap.
 
 @param accountId   the CleverTap account id
 @param token       the CleverTap account token
 
 */
- (void)changeCredentialsWithAccountID:(NSString *)accountID andToken:(NSString *)token;

#pragma mark deprecations as of version 2.0.3

+ (CleverTap *)push __attribute__((deprecated("Deprecated as of version 2.0.3, use sharedInstance instead")));

+ (CleverTap *)event __attribute__((deprecated("Deprecated as of version 2.0.3, use sharedInstance instead")));

+ (CleverTap *)profile __attribute__((deprecated("Deprecated as of version 2.0.3, use sharedInstance instead")));

+ (CleverTap *)session  __attribute__((deprecated("Deprecated as of version 2.0.3, use sharedInstance instead")));

+ (void)changeCredentialsWithAccountID:(NSString *)accountID andToken:(NSString *)token  __attribute__((deprecated("Deprecated as of version 2.0.3, use [[CleverTap sharedInstance] changeCredentialsWithAccountID:andToken:] instead")));

- (void)eventName:(NSString *)event __attribute__((deprecated("Deprecated as of version 2.0.3, use recordEvent: instead")));

- (void)eventName:(NSString *)event eventProps:(NSDictionary *)properties __attribute__((deprecated("Deprecated as of version 2.0.3, use recordEvent:withProps: instead")));

- (void)chargedEventWithDetails:(NSDictionary *)chargeDetails andItems:(NSArray *)items __attribute__((deprecated("Deprecated as of version 2.0.3, use recordChargedEventWithDetails:andItems: instead")));

- (void)profile:(NSDictionary *)profileDictionary __attribute__((deprecated("Deprecated as of version 2.0.3, use profileSet: instead")));

- (void)graphUser:(id)fbGraphUser __attribute__((deprecated("Deprecated as of version 2.0.3, use profileSetGraphUser: instead")));

- (void)googlePlusUser:(id)googleUser __attribute__((deprecated("Deprecated as of version 2.0.3, use profileSetGooglePlusUser: instead")));

+ (void)setPushToken:(NSData *)pushToken __attribute__((deprecated("Deprecated as of version 2.0.3, use [[CleverTap sharedInstance] setPushToken:] instead")));

+ (void)notifyApplicationLaunchedWithOptions:(NSDictionary *)launchOptions __attribute__((deprecated));

+ (void)showInAppNotificationIfAny __attribute__((deprecated("Deprecated as of version 2.0.3, use [[CleverTap sharedInstance] showInAppNotificationIfAny] instead")));

+ (void)handleNotificationWithData:(id)data __attribute__((deprecated("Deprecated as of version 2.0.3, use [[CleverTap sharedInstance] handleNotificationWithData:] instead")));

+ (void)handleOpenURL:(NSURL *)url sourceApplication:(NSString *)sourceApplication __attribute__((deprecated("Deprecated as of version 2.0.3, use [[CleverTap sharedInstance] handleOpenUrl:sourceApplication:] instead")));

+ (void)notifyViewLoaded:(UIViewController *)viewController __attribute__((deprecated));

+ (void)pushInstallReferrerSource:(NSString *)source
                           medium:(NSString *)medium
                         campaign:(NSString *)campaign __attribute__((deprecated("Deprecated as of version 2.0.3, use [[CleverTap sharedInstance] pushInstallReferrerSource:medium:campaign] instead")));

#pragma mark Event API messages

- (NSTimeInterval)getFirstTime:(NSString *)event __attribute__((deprecated("Deprecated as of version 2.0.3, use eventGetFirstTime: instead")));

- (NSTimeInterval)getLastTime:(NSString *)event __attribute__((deprecated("Deprecated as of version 2.0.3, use eventGetLastTime: instead")));

- (int)getOccurrences:(NSString *)event __attribute__((deprecated("Deprecated as of version 2.0.3, use eventGetOccurrences: instead")));

- (NSDictionary *)getHistory __attribute__((deprecated("Deprecated as of version 2.0.3, use userGetEventHistory: instead")));

- (CleverTapEventDetail *)getEventDetail:(NSString *)event __attribute__((deprecated("Deprecated as of version 2.0.3, use eventGetDetail: instead")));

#pragma mark Profile API messages

- (id)getProperty:(NSString *)propertyName __attribute__((deprecated("Deprecated as of version 2.0.3, use profileGet: instead")));

#pragma mark Session API messages

- (NSTimeInterval)getTimeElapsed __attribute__((deprecated("Deprecated as of version 2.0.3, use sessionGetTimeElapsed: instead")));

- (int)getTotalVisits __attribute__((deprecated("Deprecated as of version 2.0.3, use userGetTotalVisits: instead")));

- (int)getScreenCount __attribute__((deprecated("Deprecated as of version 2.0.3, use userGetScreenCount: instead")));

- (NSTimeInterval)getPreviousVisitTime __attribute__((deprecated("Deprecated as of version 2.0.3, use userGetPreviousVisitTime: instead")));

- (CleverTapUTMDetail *)getUTMDetails __unused  __attribute__((deprecated("Deprecated as of version 2.0.3, use sessionGetUTMDetails: instead")));


@end

#pragma clang diagnostic pop