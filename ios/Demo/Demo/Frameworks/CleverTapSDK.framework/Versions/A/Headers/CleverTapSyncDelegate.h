//
// Created by Jude Pereira on 21/11/2015.
// Copyright (c) 2015 CleverTap. All rights reserved.
//

#import <Foundation/Foundation.h>

@protocol CleverTapSyncDelegate <NSObject>
@required
- (void)profileDataUpdated:(NSDictionary*)updates;
@end