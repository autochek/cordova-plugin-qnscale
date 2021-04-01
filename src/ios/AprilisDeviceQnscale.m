/********* Qnscale.m Cordova Plugin Implementation *******/

#import <Cordova/CDV.h>
#import <QNSDK/QNBleApi.h>
#import "AprilisDeviceQnscaleResponse.h"
#import "AprilisDeviceQnscaleData.h"

#define AutoChekBodyCompositionMainService		@"FFF0"
#define AutoChekBodyCompositionMainServiceRead	@"FFF1"
#define PathForConfigFile						@"APRILIS20191216"
#define TAG										@"aprilis.autochek.care.Qnscale"

@interface Qnscale <CBCentralManagerDelegate, CBPeripheralDelegate, QNBleProtocolDelegate, QNScaleDataListener> : CDVPlugin {
  // Member variables go here.
}

/**
 * Cordova 실행 명령 정보 객체
 */
@property (nonatomic, strong) CDVInvokedUrlCommand *currentCommand;
/**
 * 블루투스 관리자
 */
@property (nonatomic, strong) CBCentralManager *centralManager;
/**
 * 블루투스 api
 */
@property (nonatomic, strong) QNBleApi *bleApi;
/**
 * 블루투스 상태
 */
@property(nonatomic, assign, readwrite) CBManagerState bleState;
/**
 * 장치 아이디
 */
@property (nonatomic, strong) NSString *deviceId;
/**
 * 사용자 아이디
 */
@property (nonatomic, strong) NSString *userId;
/**
 * 키
 */
@property (nonatomic, strong) NSNumber *height;
/**
 * 성별
 */
@property (nonatomic, strong) NSString *gender;
/**
 * 생년월일
 */
@property (nonatomic, strong) NSDate *birthDate;
/**
 * 연결된 블루투스 기기
 */
@property (nonatomic, strong) QNBleDevice *bleDevice;
/**
 * 사용자 정보
 */
@property (nonatomic, strong) QNUser *user;
/**
 * 현재 연결된 장치
 */
@property (nonatomic, strong) CBPeripheral *currentPeripheral;
/**
 * 블루투스 프로토콜 핸들러
 */
@property (nonatomic, strong) QNBleProtocolHandler *protocolHandle;

/**
 * 장치 연결
 * @param command Cordova 실행 명령 정보 객체
 */
- (void)connect:(CDVInvokedUrlCommand*)command;
/**
 * 동기화
 * @param command Cordova 실행 명령 정보 객체
 */
- (void)syncData:(CDVInvokedUrlCommand*)command;
/**
 * 장치 연결 해제
 * @param command Cordova 실행 명령 정보 객체
 */
- (void)disconnect:(CDVInvokedUrlCommand*)command;
/**
 * 결과를 콜백으로 반환한다.
 * @param command Cordova 실행 명령 정보 객체
 * @param result 성공 여부
 * @param message 성공/에러 메세지
 */
- (void)callbackResult:(CDVInvokedUrlCommand*)command result:(BOOL)result message:(NSString*)message;
/**
 * 결과를 콜백으로 반환한다.
 * @param command Cordova 실행 명령 정보 객체
 * @param result 성공 여부
 * @param message 성공/에러 메세지
 * @param data 데이터
 */
- (void)callbackResult:(CDVInvokedUrlCommand*)command result:(BOOL)result message:(NSString*)message data:(NSDictionary*)data;
@end

@implementation Qnscale

/**
 * 장치 연결
 * @param command Cordova 실행 명령 정보 객체
 */
- (void)connect:(CDVInvokedUrlCommand*)command
{
    CDVPluginResult* pluginResult = nil;

    self.currentCommand = command;

    self.deviceId = [command.arguments objectAtIndex:0];
	NSNumber *connectionTimeoutSec = [command.arguments objectAtIndex:1];
	self.userId = [command.arguments objectAtIndex:2];
	self.gender = [command.arguments objectAtIndex:3];
	NSNumber *year = [command.arguments objectAtIndex:4];
	self.height = [command.arguments objectAtIndex:5];

	NSString *birthDateString = [NSString stringWithFormat:@"%d-01-02", year];

	// Date Formatter 생성
	NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
	[dateFormatter setDateFormat:@"yyyy-MM-dd"];
	self.birthDate = [dateFormatter dateFromString:birthDateString];

	NSLog(@"[%@] birthDate: %@, date: %@", TAG, birthDateString, self.birthDate);

	NSLog(@"[%@] Qnscale connect : deviceId=%@, connectionTimeoutSec=%@, userId=%@, height=%@, gender=%@, birthDate=%@"
			, TAG, self.deviceId, connectionTimeoutSec, self.userId, self.height, self.gender, self.birthDate);

	// 디바이스 아이디가 유효하지 않은 경우
    if (self.deviceId != nil && [self.deviceId length] > 0) {

		// 백그라운드에서 실행
		[self.commandDelegate runInBackground:^{

			// 연결 타임 아웃이 지정된 경우
			if(connectionTimeoutSec > 0) {
				// 특정 시간 동안 검색이 되지 않는 경우 타임 아웃 시키는 핸들러와 실행 생성
				dispatch_after(dispatch_time(DISPATCH_TIME_NOW, connectionTimeoutSec.intValue * NSEC_PER_SEC), dispatch_get_main_queue(), ^{
					if(self.bleDevice != nil) {
						[self.bleApi stopBleDeviceDiscorvery:^(NSError *error) {
							if(error)
								[self callbackResult:command result:false message:@"Fail to build user"];
							// 타임 아웃 에러 반환
							[self callbackResult:command result:false message:@"Connection timeout"];
						}];
					}
				});
			}

			// API가 초기화 되지 않은 경우
			if(self.bleApi == nil) {

				QNConfig *config = [[QNBleApi sharedBleApi] getConfig];
				config.showPowerAlertKey = NO;

				// 설정 파일 경로
				NSString* configFilePath = [[NSBundle mainBundle] pathForResource:PathForConfigFile ofType:@"qn"];
				// SDK를 초기화 한다.
				[[QNBleApi sharedBleApi] initSdk:PathForConfigFile firstDataFile:configFilePath callback:^(NSError *error) {
					if(error == nil) {
						NSLog(@"[%@] QNBleApi.initSdk success", TAG);

						self.bleApi = [QNBleApi sharedBleApi];
//						// 프로토콜 핸들러 설정
//						self.bleApi.bleProtocolListener = self;
						// 데이터 리스너
						self.bleApi.dataListener = self;
					}
					// 초기화 결과가 실패인 경우 (에러 출력)
					else {
						// 초기화 에러 반환
						[self callbackResult:command result:false message:@"Fail to initialize API"];
					}
				}];

				// 블루투스 관리자가 생성되지 않은 경우
				if (self.centralManager == nil) {
					NSLog(@"[%@] CBCentralManager.initWithDelegate", TAG);
					self.centralManager =  [[CBCentralManager alloc] initWithDelegate:self queue:nil options:@{CBCentralManagerOptionShowPowerAlertKey : [NSNumber numberWithBool:YES]}];
				}
			}
			// API가 초기화 되어 있는 경우
			else {
				// 장치가 검색되지 않은 경우
				if(self.bleDevice == nil) {

					// 연결된 장치 정보가 없는 경우
					if(self.bleDevice == nil || self.currentPeripheral != nil) {
						// 사용자 정보 객체 생성 및 장치 검색
						[self buildUserAndStartDiscovery:self.currentCommand userId:self.userId height:self.height.doubleValue gender:self.gender birthDate:self.birthDate];
					}
					// 연결된 장치 정보가 존재하는 경우
					else {
						// 장치 연결
						[self connectToDevice:self.bleDevice peripheral:self.currentPeripheral];
					}
				}
			}
		}];

    }
    else {
		[self callbackResult:command result:false message:@"Invalid deviceId"];
    }
}

/**
 * 동기화
 * @param command Cordova 실행 명령 정보 객체
 */
- (void)syncData:(CDVInvokedUrlCommand*)command
{
    CDVPluginResult* pluginResult = nil;

	self.currentCommand = command;

	NSLog(@"[%@] Qnscale syncData", TAG);

	// API가 초기화 되지 않은 경우
	if(self.bleApi == nil) {
		[self callbackResult:command result:false message:@"Need to run the connection first."];
	}
	// API가 초기화 되어 있는 경우
	else {
		// 백그라운드에서 실행
		[self.commandDelegate runInBackground:^{
			self.protocolHandle = [self.bleApi buildProtocolHandler:self.bleDevice user:self.user wifiConfig:nil delegate:self callback:^(NSError *error) {
				if (error)
					[self callbackResult:command result:false message:[NSString stringWithFormat:@"Failed to build ProtocolHandler, reason: %@",error]];
			}];
			[self.currentPeripheral discoverServices:nil];
		}];
	}
}

/**
 * 장치 연결 해제
 * @param command Cordova 실행 명령 정보 객체
 */
- (void)disconnect:(CDVInvokedUrlCommand*)command
{
    CDVPluginResult* pluginResult = nil;

	self.currentCommand = command;

	NSLog(@"[%@] Qnscale disconnect", TAG);

	// API가 초기화 되지 않은 경우
	if(self.bleApi == nil) {
		[self callbackResult:command result:true message:@""];
	}
	// API가 초기화 되어 있는 경우
	else {
		// 백그라운드에서 실행
		[self.commandDelegate runInBackground:^{

			// 장치 연결 해제
			[self disconnectDevice];
			[self callbackResult:command result:true message:@""];
		}];
	}
}

#pragma mark - Custom methods

/**
 * 결과를 콜백으로 반환한다.
 * @param command Cordova 실행 명령 정보 객체
 * @param result 성공 여부
 * @param message 성공/에러 메세지
 */
- (void)callbackResult:(CDVInvokedUrlCommand*)command result:(BOOL)result message:(NSString*)message
{
	QnscaleResponse *response = [[QnscaleResponse alloc] init];
	response.result = result;
	response.message = message;

	if(result)
		[self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:[response toJson]] callbackId:command.callbackId];
	else {
		NSLog(@"[%@] %@", TAG, message);
		[self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[response toJson]] callbackId:command.callbackId];
	}
}

/**
 * 결과를 콜백으로 반환한다.
 * @param command Cordova 실행 명령 정보 객체
 * @param result 성공 여부
 * @param message 성공/에러 메세지
 * @param data 데이터
 */
- (void)callbackResult:(CDVInvokedUrlCommand*)command result:(BOOL)result message:(NSString*)message data:(NSArray<NSDictionary*> *)data
{
	QnscaleResponse *response = [[QnscaleResponse alloc] init];
	response.result = result;
	response.message = message;
	response.data = data;

	if(result)
		[self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:[response toJson]] callbackId:command.callbackId];
	else {
		NSLog(@"[%@] %@", TAG, message);
		[self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[response toJson]] callbackId:command.callbackId];
	}

}

/**
 * 사용자 정보 객체 생성 및 장치 검색
 * @param command Cordova 실행 명령 정보 객체
 * @param userId 사용자 아아디
 * @param height 키
 * @param gender 성별
 * @param birthDate 생년월일
 */
- (void) buildUserAndStartDiscovery:(CDVInvokedUrlCommand*)command userId:(NSString*)userId height:(double)height gender:(NSString*)gender birthDate:(NSDate*)birthDate {

//	NSLog(@"[%@] buildUserAndStartDiscovery", TAG);

	if(self.bleApi != nil) {

		// 사용자 정보 생성
		QNUser *user = [self.bleApi buildUser:userId
									   height:height
									   gender:gender
									 birthday:birthDate
									 callback:^(NSError *error) {
										 if(error)
											 [self callbackResult:command result:false message:@"Fail to build user"];
									 }];

		// 사용자 저장
		self.user = user;

		// 장치 검색
		[self.centralManager scanForPeripheralsWithServices:nil options:nil];
	}
	else {
		[self callbackResult:command result:false message:@"The API must be initialized first."];
	}
}

/**
 * 장치 연결
 * @param bleDevice QNBleDevice 객체
 * @param peripheral CBPeripheral 객체
 */
- (void) connectToDevice:(QNBleDevice *)bleDevice peripheral:(CBPeripheral *)peripheral {

//	NSLog(@"[%@] connectToDevice", TAG);

	self.bleDevice = bleDevice;
	self.currentPeripheral = peripheral;
	self.currentPeripheral.delegate = self;

	NSMutableDictionary *configDic = [NSMutableDictionary dictionary];
	[configDic setObject:[NSNumber numberWithBool:NO] forKey:CBConnectPeripheralOptionNotifyOnConnectionKey];
	[configDic setObject:[NSNumber numberWithBool:NO] forKey:CBConnectPeripheralOptionNotifyOnDisconnectionKey];
	[configDic setObject:[NSNumber numberWithBool:NO] forKey:CBConnectPeripheralOptionNotifyOnNotificationKey];
	[configDic setObject:[NSNumber numberWithBool:NO] forKey:CBConnectPeripheralOptionNotifyOnNotificationKey];

	// 장치 연결
	[self.centralManager connectPeripheral:self.currentPeripheral options:configDic];
}

/**
 * 장치 연결 해제
 * @param bleDevice QNBleDevice 객체
 * @param peripheral CBPeripheral 객체
 */
- (void) disconnectDevice {

//	NSLog(@"[%@] disconnectDevice", TAG);

	if(self.currentPeripheral) {
		// 장치 연결 해제
		[self.centralManager cancelPeripheralConnection:self.currentPeripheral];
	}

	self.bleDevice = nil;
	self.currentPeripheral = nil;
}

#pragma mark - CBCentralManagerDelegate

/**
 * 블루투스 상태 업데이트 이벤트
 * @param central 블루투스 매니져
 */
- (void)centralManagerDidUpdateState:(nonnull CBCentralManager *)central {

//	NSLog(@"[%@] centralManagerDidUpdateState : %@", TAG, @([central state]));

	if (central.state == CBManagerStatePoweredOn) {

		// 장치가 검색되지 않은 경우
		if(self.bleDevice == nil)
			// 사용자 정보 객체 생성 및 장치 검색
			[self buildUserAndStartDiscovery:self.currentCommand userId:self.userId height:self.height.doubleValue gender:self.gender birthDate:self.birthDate];

	}
}

/**
 * 장치 연결 이벤트
 * @param central 블루투스 매니져
 * @param peripheral 연결 장치 정보 객체
 */
- (void)centralManager:(CBCentralManager *)central didConnectPeripheral:(CBPeripheral *)peripheral {

	NSLog(@"[%@] didConnectPeripheral", TAG);

	[self callbackResult:self.currentCommand result:true message:@""];
}

/**
 * 장치 검색 이벤트
 * @param central 블루투스 매니져
 * @param peripheral 장치 정보
 * @param advertisementData 데이터
 * @param RSSI rssi
 */
- (void)centralManager:(CBCentralManager *)central didDiscoverPeripheral:(CBPeripheral *)peripheral advertisementData:(NSDictionary<NSString *,id> *)advertisementData RSSI:(NSNumber *)RSSI {

	if(peripheral != nil) {

		NSLog(@"[%@] didDiscoverPeripheral : identifier=%@, name=%@, description=%@", TAG, peripheral.identifier, peripheral.name, peripheral.description);

		// 장치 아이디가 일치하는 경우
		if([peripheral.identifier.UUIDString isEqualToString:self.deviceId]) {
			// 스캔 중지
			[self.centralManager stopScan];

			NSLog(@"[%@] didDiscoverPeripheral matched device : identifier=%@, name=%@, description=%@", TAG, peripheral.identifier, peripheral.name, peripheral.description);

			// QNBleDevice 생성
			QNBleDevice *bleDevice = [self.bleApi buildDevice:peripheral rssi:RSSI advertisementData:advertisementData callback:^(NSError *error) {
			}];
			if(bleDevice != nil) {
				[self connectToDevice:bleDevice peripheral:peripheral];
			}
		}
	}

}

#pragma mark - CBPeripheralDelegate

/**
 * 서비스 검색
 * @param peripheral
 * @param error
 */
- (void)peripheral:(CBPeripheral *)peripheral didDiscoverServices:(NSError *)error {

//	NSLog(@"[%@] didDiscoverServices", TAG);

	if (error) {
//		NSLog(@"[%@] didDiscoverServices > %@ 장치 서비스 검색 오류", TAG, peripheral.name);
		return;
	}

	// 모든 서비스에 대해서 처리
	for (CBService *service in peripheral.services) {
//		NSLog(@"[%@] didDiscoverServices > %@ 장치 서비스 검색: %@", TAG, peripheral.name, service.UUID.UUIDString.uppercaseString);
		[peripheral discoverCharacteristics:nil forService:service];
	}
}

/** 서비스에 대한 특성이 발견된 경우 */
- (void)peripheral:(CBPeripheral *)peripheral didDiscoverCharacteristicsForService:(nonnull CBService *)service error:(nullable NSError *)error {

//	NSLog(@"[%@] didDiscoverCharacteristicsForService : %@", TAG, service.UUID.UUIDString.uppercaseString);

	if (error) {
//		NSLog(@"[%@] didDiscoverCharacteristicsForService > %@ 장치 특성 검색 오류", TAG, peripheral.name);
		return;
	}

	for (CBCharacteristic *characteristics in service.characteristics) {
		NSString *uuidStr = characteristics.UUID.UUIDString.uppercaseString;

//		NSLog(@"[%@] didDiscoverCharacteristicsForService > %@ 장치 특성 검색: %@", TAG, peripheral.name, uuidStr);

		if ([uuidStr isEqualToString:AutoChekBodyCompositionMainServiceRead]){
			[peripheral setNotifyValue:YES forCharacteristic:characteristics];
		}
	}
	if ([service.UUID.UUIDString isEqualToString:AutoChekBodyCompositionMainService]) {
		[self.protocolHandle prepare:service.UUID.UUIDString];
//		NSLog(@"[%@] didDiscoverCharacteristicsForService > protocolHandle prepare for : %@ service", TAG, service.UUID.UUIDString);
	}
}

/**
 * 데이터 응답 쓰기
 */
- (void)peripheral:(CBPeripheral *)peripheral didWriteValueForCharacteristic:(CBCharacteristic *)characteristic error:(NSError *)error {

//	NSLog(@"[%@] didWriteValueForCharacteristic", TAG);

	if (error) {
//		NSLog(@"[%@] didWriteValueForCharacteristic > %@ 데이터 채널 오류  %@", TAG,characteristic.UUID.UUIDString,error.description);
	}
}

/** 데이터 업데이트 */
- (void)peripheral:(CBPeripheral *)peripheral didUpdateValueForCharacteristic:(nonnull CBCharacteristic *)characteristic error:(nullable NSError *)error {

//	NSLog(@"[%@] didUpdateValueForCharacteristic", TAG);

	dispatch_async(dispatch_get_main_queue(), ^{

		NSString *serviceUUID;
		for (CBService *service in self.currentPeripheral.services) {
			NSString *serviceUUIDStr = service.UUID.UUIDString;
			if ([serviceUUIDStr isEqualToString:AutoChekBodyCompositionMainService]) {
				serviceUUID = serviceUUIDStr;
			}
		}
//		NSLog(@"[%@] didUpdateValueForCharacteristic > protocolHandle onGetBleData : service=%@, characteristic=%@", TAG, serviceUUID, characteristic.UUID.UUIDString);
		[self.protocolHandle onGetBleData:serviceUUID characteristicUUID:characteristic.UUID.UUIDString data:characteristic.value];
	});
}

#pragma mark - QNBleProtocolDelegate


- (void)writeCharacteristicValue:(NSString *)serviceUUID characteristicUUID:(NSString *)characteristicUUID data:(NSData *)data {

//	NSLog(@"[%@] WriteCharacteristicValue : %@", TAG, characteristicUUID);
	[self writeData:data uuidMode:characteristicUUID];
}

- (void)writeData:(NSData *)writeData uuidMode:(NSString *)writeUUIDMode{

//	NSLog(@"[%@] WriteData : writeUUIDMode = %@", TAG, writeUUIDMode);

	const Byte *bytes = writeData.bytes;
	NSString *dataStr = @"";
	for (int i = 0; i < writeData.length; i ++) {
		dataStr = [NSString stringWithFormat:@"%@ %02x",dataStr,bytes[i]];
	}

	if (self.currentPeripheral == nil) {
//		[self callbackResult:self.currentCommand result:false message:@"currentPeripheral is nil"];
		return;
	}

	CBCharacteristic *characteristic = nil;
	for (CBService *service in self.currentPeripheral.services) {

//		NSLog(@"[%@] writeData > service : %@", TAG, service.UUID.UUIDString);

		for (CBCharacteristic *characteristics in service.characteristics) {
			NSString *uuidStr = characteristics.UUID.UUIDString.uppercaseString;

//			NSLog(@"[%@] writeData > characteristics : %@", TAG, uuidStr);

			if ([uuidStr isEqualToString:writeUUIDMode]) {
				characteristic = characteristics;
			}
		}
	}

	if (characteristic) {

//		NSLog(@"[%@] writeData > found characteristic : %@", TAG, characteristic);

		if (characteristic.properties & CBCharacteristicPropertyWriteWithoutResponse) {
//			NSLog(@"[%@] writeData > write value for type : Write Without Response", TAG);
			[self.currentPeripheral writeValue:writeData forCharacteristic:characteristic type:CBCharacteristicWriteWithoutResponse];
		}else{
//			NSLog(@"[%@] writeData > write value for type : Write With Response", TAG);
			[self.currentPeripheral writeValue:writeData forCharacteristic:characteristic type:CBCharacteristicWriteWithResponse];
		}
	}else {
//		NSLog(@"[%@] writeData > characteristic is nil", TAG);
		[self callbackResult:self.currentCommand result:false message:@"characteristic is nil"];
	}
}

- (void)readCharacteristicValue:(NSString *)serviceUUID characteristicUUID:(NSString *)characteristicUUID {
	NSLog(@"[%@] readCharacteristicValue", TAG);

}

#pragma mark - QNScaleDataListener

/**
 스케일에 의해 업로드 된 실시간 몸무게 값 콜백 함수

 @param device QNBleDevice 장치 정보 객체
 @param weight 몸무게
*/
- (void)onGetUnsteadyWeight:(QNBleDevice *)device weight:(double)weight {
//	NSLog(@"[%@] onGetUnsteadyWeight : %f", TAG, weight);
}

/**
 APP에 연결하지 않은 상태에서 측정한 데이터 콜백 함수

 @param device QNBleDevice 장치 정보 객체
 @param scaleData 데이터 결과
*/
- (void)onGetScaleData:(QNBleDevice *)device data:(QNScaleData *)scaleData {
//	NSLog(@"[%@] onGetScaleData", TAG);

	// 데이터 저장 객체 생성
	AprilisDeviceQnscaleData *responseData = [[AprilisDeviceQnscaleData alloc] init];

	NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
	[dateFormatter setDateFormat:@"yyyy-MM-dd HH:mm:ss"];
	NSString *measureTimeString;

	// 측정 시간이 유효하지 않은 경우
	if(scaleData.measureTime == nil)
		measureTimeString = [dateFormatter stringFromDate:[NSDate date]];
	// 측정 시간이 유효한 경우
	else
		measureTimeString = [dateFormatter stringFromDate:scaleData.measureTime];
	responseData.measureTime = measureTimeString;

	// 모든 측정 항목을 가져온다.
	NSArray<QNScaleItemData*> *items = [scaleData getAllItem];
	// 모든 측정 항목에 대해서 처리
	for (QNScaleItemData *item in items) {

		if(item.value) {
			if ([item.name compare:@"bone mass"] == 0)
				responseData.boneMass = item.value;
			else if ([item.name compare:@"BMR"] == 0)
				responseData.bmr = item.value;
			else if ([item.name compare:@"weight"] == 0)
				responseData.weight = item.value;
			else if ([item.name compare:@"metabolic age"] == 0)
				responseData.metabolicAge = item.value;
			else if ([item.name compare:@"body fat rate"] == 0)
				responseData.bodyFatRate = item.value;
			else if ([item.name compare:@"body type"] == 0)
				responseData.bodyType = item.value;
			else if ([item.name compare:@"muscle mass"] == 0)
				responseData.muscleMass = item.value;
			else if ([item.name compare:@"body water rate"] == 0)
				responseData.bodyWaterRate = item.value;
			else if ([item.name compare:@"protein"] == 0)
				responseData.protein = item.value;
			else if ([item.name compare:@"muscle rate"] == 0)
				responseData.muscleRate = item.value;
			else if ([item.name compare:@"visceral fat"] == 0)
				responseData.visceralFat = item.value;
			else if ([item.name compare:@"BMI"] == 0)
				responseData.bmi = item.value;
		}

	}

	[self callbackResult:self.currentCommand result:true message:@"Data received" data:[responseData getDictionary]];
}

/**
 APP에 연결하지 않은 상태에서 측정하여 장비에 저장된 데이터 콜백 함수

 @param device QNBleDevice 장치 정보 객체
 @param storedDataList 장비에 저장된 데이터 목록
*/
- (void)onGetStoredScale:(QNBleDevice *)device data:(NSArray<QNScaleStoreData *> *)storedDataList {

}


@end
