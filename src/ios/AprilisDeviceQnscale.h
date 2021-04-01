/********* AprilisDeviceQ8.m Cordova Plugin Implementation *******/

#import <Cordova/CDV.h>

@interface AprilisDeviceQnscale : CDVPlugin {
	// Member variables go here.
}

/**
 * 현재 명령
 */
@property (nonatomic, strong) CDVInvokedUrlCommand *currentCommand;

- (void)connect:(CDVInvokedUrlCommand*)command;

- (void)syncData:(CDVInvokedUrlCommand*)command;

- (void)disconnect:(CDVInvokedUrlCommand*)command;
@end
