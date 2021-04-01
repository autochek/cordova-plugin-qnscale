//
// Created by chang.lee on 2021/04/01.
//

#import "AprilisDeviceQnscaleData.h"


@implementation AprilisDeviceQnscaleData {

}

/**
 * 초기화
 * @return 아이디
 */
- (id) init
{
	self = [super init];

	return self;
}

/**
 * 객체의 속성 값을 NSDictionary로 반환한다.
 * @return NSDictionary 객체
 */
- (NSDictionary *)getDictionary
{
	NSMutableDictionary *result = [[NSMutableDictionary alloc] init];

	[result setValue:self.measureTime forKey:@"measureTime"];
	[result setValue:[NSNumber numberWithDouble:self.weight] forKey:@"weight"];
	[result setValue:[NSNumber numberWithDouble:self.boneMass] forKey:@"boneMass"];
	[result setValue:[NSNumber numberWithDouble:self.bmi] forKey:@"bmi"];
	[result setValue:[NSNumber numberWithDouble:self.bmr] forKey:@"bmr"];
	[result setValue:[NSNumber numberWithDouble:self.metabolicAge] forKey:@"metabolicAge"];
	[result setValue:[NSNumber numberWithDouble:self.bodyFatRate] forKey:@"bodyFatRate"];
	[result setValue:[NSNumber numberWithDouble:self.bodyType] forKey:@"bodyType"];
	[result setValue:[NSNumber numberWithDouble:self.muscleMass] forKey:@"muscleMass"];
	[result setValue:[NSNumber numberWithDouble:self.bodyWaterRate] forKey:@"bodyWaterRate"];
	[result setValue:[NSNumber numberWithDouble:self.subcutaneousFat] forKey:@"subcutaneousFat"];
	[result setValue:[NSNumber numberWithDouble:self.protein] forKey:@"protein"];
	[result setValue:[NSNumber numberWithDouble:self.muscleRate] forKey:@"muscleRate"];
	[result setValue:[NSNumber numberWithDouble:self.visceralFat] forKey:@"visceralFat"];

	return result;
}
@end