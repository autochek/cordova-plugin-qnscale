//
// Created by chang.lee on 2020/12/12.
//

#import "QnscaleResponse.h"


@implementation QnscaleResponse
/**
 * Json 문자열을 반환한다.
 * @return Json 문자열
 */
- (NSString*) toJson {
	NSString *result = @"";

	NSError *jsonSerializationError = nil;

	NSMutableDictionary *propertyDictionary = [NSMutableDictionary dictionaryWithCapacity:1];
	[propertyDictionary setObject:[NSNumber numberWithBool:self.result] forKey:@"result"];
	[propertyDictionary setObject:self.message forKey:@"message"];
	if(self.data != nil) {
		[propertyDictionary setObject:self.data forKey:@"data"];

//		NSMutableArray<NSString*> *jsonDatas = [[NSMutableArray alloc] init];
//
//		// 모든 데이터에 대해서 처리
//		for(NSDictionary *item in self.data) {
//			NSData *jsonData = [NSJSONSerialization dataWithJSONObject:item options:NSJSONWritingPrettyPrinted error:&jsonSerializationError];
//			if(!jsonSerializationError) {
//				[jsonDatas addObject:[[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding]];
//			}
//		}
//		NSData *jsonData = [NSJSONSerialization dataWithJSONObject:jsonDatas options:NSJSONWritingPrettyPrinted error:&jsonSerializationError];
//		if(!jsonSerializationError) {
//			NSString *dataJson = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
//			[propertyDictionary setObject:dataJson forKey:@"data"];
//		}
	}

	NSData *jsonData = [NSJSONSerialization dataWithJSONObject:propertyDictionary options:NSJSONWritingPrettyPrinted error:&jsonSerializationError];

	if(!jsonSerializationError)
		result = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];

	NSLog(@"json : %@", result);

	return result;
}
@end