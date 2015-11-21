//
// Created by Jude Pereira on 21/11/2015.
// Copyright (c) 2015 CleverTap. All rights reserved.
//

#import <Foundation/Foundation.h>

@protocol CleverTapNakedAPIDelegate <NSObject>
@required
- (void)profileDataUpdated;
@end