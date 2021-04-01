//
// Created by chang.lee on 2021/04/01.
//

#import <Foundation/Foundation.h>

/**
 * 데이터 응답 클래스
 */
@interface AprilisDeviceQnscaleData : NSObject {
}

/**
 * 초기화
 * @return 아이디
 */
- (id) init;

/**
 * 객체의 속성 값을 NSDictionary로 반환한다.
 * @return NSDictionary 객체
 */
- (NSDictionary *)getDictionary;

/**
 * 측정 시간
 */
@property (nonatomic, copy) NSString *measureTime;
/**
 * 체중
 */
@property (nonatomic, readwrite) double weight;
/**
 * 골량
 */
@property (nonatomic, readwrite) double boneMass;
/**
 * BMI
 */
@property (nonatomic, readwrite) double bmi;
/**
 * BMR
 */
@property (nonatomic, readwrite) double bmr;
/**
 * 신진 대사 연령
 */
@property (nonatomic, readwrite) double metabolicAge;
/**
 * 체지방률
 */
@property (nonatomic, readwrite) double bodyFatRate;
/**
 * 체형 지수
 */
@property (nonatomic, readwrite) double bodyType;
/**
 * 근육량
 */
@property (nonatomic, readwrite) double muscleMass;
/**
 * 체수분율
 */
@property (nonatomic, readwrite) double bodyWaterRate;
/**
 * 피하 지방
 */
@property (nonatomic, readwrite) double subcutaneousFat;
/**
 * 단백질
 */
@property (nonatomic, readwrite) double protein;
/**
 * 근육 비율
 */
@property (nonatomic, readwrite) double muscleRate;
/**
 * 내장 지방
 */
@property (nonatomic, readwrite) double visceralFat;

@end