//
// Created by chang.lee on 2020/12/12.
//

#import <Foundation/Foundation.h>


@interface QnscaleResponse : NSObject
/**
 * 성공 여부
 */
@property (nonatomic, readwrite) BOOL result;
/**
 * 성공/에러 메세지
 */
@property (nonatomic, copy) NSString *message;
/**
 * 결과 데이터
 */
@property (nonatomic, copy) NSArray<NSDictionary*> *data;
/**
 * Json 문자열을 반환한다.
 * @return Json 문자열
 */
- (NSString*) toJson;
@end