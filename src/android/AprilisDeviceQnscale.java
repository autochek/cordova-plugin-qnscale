package cordova.plugins.aprilis.device.qnscale;

import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yolanda.health.qnblesdk.listener.QNBleConnectionChangeListener;
import com.yolanda.health.qnblesdk.listener.QNBleDeviceDiscoveryListener;
import com.yolanda.health.qnblesdk.listener.QNResultCallback;
import com.yolanda.health.qnblesdk.listener.QNScaleDataListener;
import com.yolanda.health.qnblesdk.out.QNBleApi;
import com.yolanda.health.qnblesdk.out.QNBleBroadcastDevice;
import com.yolanda.health.qnblesdk.out.QNBleDevice;
import com.yolanda.health.qnblesdk.out.QNBleKitchenDevice;
import com.yolanda.health.qnblesdk.out.QNScaleData;
import com.yolanda.health.qnblesdk.out.QNScaleItemData;
import com.yolanda.health.qnblesdk.out.QNScaleStoreData;
import com.yolanda.health.qnblesdk.out.QNUser;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cordova.plugins.aprilis.device.lsband5s.AprilisDeviceLSBand5sResponse;


/**
 * This class echoes a string called from JavaScript.
 */
public class AprilisDeviceQnscale extends CordovaPlugin {

	/**
	 * 로깅용 태그
	 */
	private String TAG = "aprilis.autochek.care.AprilisDeviceQnscale";
	/**
	 * API 인스턴스
	 */
	private QNBleApi instance;
	/**
	 * 연결 타임 아웃 핸들러
	 */
	private android.os.Handler connectionTimeoutHandler = null;
	/**
	 * 연결 타임 아웃 실행
	 */
	private Runnable connectionTimeoutRunnable = null;
	/**
	 * 데이터 동기화 타임 아웃 핸들러
	 */
	private android.os.Handler syncDataTimeoutHandler = null;
	/**
	 * 데이터 동기화 타임 아웃 실행
	 */
	private Runnable syncDataTimeoutRunnable = null;
	/**
	 * 연결된 장치 정보 객체
	 */
	private QNBleDevice connectDevice = null;

	/**
	 * Cordova 함수 실행
	 * @param action 함수명
	 * @param args 인자 목록
	 * @param callbackContext 결과 콜백 컨텍스트
	 * @return 성공 여부
	 * @throws JSONException JSON 예외
	 */
	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

		Log.d(TAG, "execute : " + action + ", " + args);

		// 연결 요청
		if (action.equals("connect")) {

			// 연결된 장치 정보 초기화
			this.connectDevice = null;

			// 파라미터에서 사용자 정보를 저장
			String deviceId = args.getString(0);
			int connectionTimeoutSec = args.getInt(1);
			String userId = args.getString(2);
			String gender = args.getString(3);
			int year = args.getInt(4);
			int height = args.getInt(5);

			// API 인스턴스를 가져온다.
			this.instance = QNBleApi.getInstance(this.cordova.getActivity().getApplicationContext());

			// 장치 연결
			this.connect(deviceId, connectionTimeoutSec, userId, gender, year, height, callbackContext);

			return true;
		}
		// 동기화 요청
		else if (action.equals("syncData")) {

			// API 인스턴스가 유효하지 않거나 장치 정보가 유효하지 않은 경우
			if (this.instance == null || this.connectDevice == null) {
				try {
					Log.e(TAG, "execute syncData : API instance is null. please connect device first");

					// 타임 아웃 에러 반환
					callbackContext.error(new ObjectMapper().writeValueAsString(new AprilisDeviceQnscaleResponse(false, "API instance is null. please connect device first")));
				} catch (JsonProcessingException e) {
					return false;
				}
			}
			// API 인스턴스가 유효하고 장치 정보가 유효한 경우
			else {
				// 데이터 리스너 설정
				setDataListener(callbackContext);
			}

			return true;
		}
		else if (action.equals("disconnect")) {

			// API 인스턴스가 유효하지 않거나 장치 정보가 유효하지 않은 경우
			if (this.instance == null || this.connectDevice == null) {
				try {
					// 성공으로 결과 반환
					callbackContext.success(new ObjectMapper().writeValueAsString(new AprilisDeviceQnscaleResponse(true, "Device disconnected")));
				} catch (JsonProcessingException e) {
					return false;
				}
			}
			// API 인스턴스가 유효하고 장치 정보가 유효한 경우
			else {
				// 연결 해제
				this.disconnect(this.connectDevice, callbackContext);
			}

			return true;
		}

		return false;
	}

	/**
	 * 장치 접속 요청 (장치 검색 후 연결)
	 * @param deviceId 장치 아이디 (맥주소)
	 * @param connectionTimeoutSec 연결 타임 아웃 (초)
	 * @param userId 사용자 아이디
	 * @param gender 성별
	 * @param year 생년
	 * @param height 키
	 * @param callbackContext 결과 콜백 컨텍스트
	 */
	private void connect(String deviceId, int connectionTimeoutSec, String userId, String gender, int year, int height, CallbackContext callbackContext) {

		Log.d(TAG, "try to connect : " + deviceId);

		// 연결 타임 아웃이 지정된 경우
		if(connectionTimeoutSec > 0) {
			// 특정 시간 동안 검색이 되지 않는 경우 타임 아웃 시키는 핸들러와 실행 생성
			this.connectionTimeoutHandler = new android.os.Handler();
			this.connectionTimeoutRunnable = new Runnable() {
				public void run() {
					try {

						// 장치 검색 중지
						AprilisDeviceQnscale.this.instance.stopBleDeviceDiscovery(new QNResultCallback() {
							/**
							 * 장치 검색 중지 결과
							 * @param code 결과 코드
							 * @param msg 에러 메세지
							 */
							@Override
							public void onResult(int code, String msg) {
								Log.d(TAG, "scan stopped : " + code + ", " + msg);
							}
						});

						// 타임 아웃 에러 반환
						Log.e(TAG, "AprilisDeviceQnscale.connect : Connection timeout");
						callbackContext.error(new ObjectMapper().writeValueAsString(new AprilisDeviceQnscaleResponse(false, "Connection timeout")));
					} catch (JsonProcessingException e) {
					}
				}
			};
			// 10초 뒤 타임 아웃 실행
			this.connectionTimeoutHandler.postDelayed(this.connectionTimeoutRunnable, 1000 * connectionTimeoutSec);
		}

		// 설정 파일 경로
		String configFilePath = "file:///android_asset/APRILIS20191216.qn";
		// SDK를 초기화 한다.
		this.instance.initSdk("APRILIS20191216", configFilePath, new QNResultCallback() {
			/**
			 * 초기화 결과
			 * @param code 결과 코드
			 * @param msg 에러 메세지
			 */
			@Override
			public void onResult(int code, String msg) {

				Log.d(TAG, "API Initialized : " + code + ", " + msg);

				// 사용자 생년월일 저장
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
				Date birthDate = new Date();
				try {
					birthDate = dateFormat.parse(String.valueOf(year) + "-01-02");
				} catch (ParseException e) {
				}
				// 사용자 정보 생성
				QNUser user = AprilisDeviceQnscale.this.instance.buildUser(userId, height, gender, birthDate, 0, new QNResultCallback(){
					/**
					 * 사용자 정보 생성 결과
					 * @param code 결과 코드
					 * @param msg 에러 메세지
					 */
					@Override
					public void onResult(int code, String msg) {
						Log.d(TAG, "user built : " + code + ", " + msg);
					}
				});

				// 장치 검색
				AprilisDeviceQnscale.this.startDiscovery(deviceId, user, callbackContext);
			}
		});

	}

	/**
	 * 장치 검색
	 * @param deviceId 장치 아이디 (맥주소)
	 * @param user 사용자 정보
	 * @param callbackContext 결과 콜백 컨텍스트
	 */
	private void startDiscovery(String deviceId, QNUser user, CallbackContext callbackContext){

		/**
		 * 장치 검색 리스너
		 */
		this.instance.setBleDeviceDiscoveryListener(
				new QNBleDeviceDiscoveryListener() {
					/**
					 * 장치 발견 시 이벤트
					 * @param device
					 */
					@Override
					public void onDeviceDiscover(QNBleDevice device) {
						try {
							Log.d(TAG, "onDeviceDiscover : " + (new ObjectMapper().writeValueAsString(device)));
						} catch (JsonProcessingException e) {
						}

						// 해당 장치 아이디의 기기인 경우
						if (device.getMac().equals(deviceId)) {

							// 장치 검색 중지
							AprilisDeviceQnscale.this.instance.stopBleDeviceDiscovery(new QNResultCallback() {
								/**
								 * 장치 검색 중지 결과
								 * @param code 결과 코드
								 * @param msg 에러 메세지
								 */
								@Override
								public void onResult(int code, String msg) {
									Log.d(TAG, "scan stopped : " + code + ", " + msg);
								}
							});

							// 기본 데이터 리스너 설정
							AprilisDeviceQnscale.this.instance.setDataListener(new QNScaleDataListener() {
								@Override
								public void onGetUnsteadyWeight(QNBleDevice device, double weight) {}
								@Override
								public void onGetScaleData(QNBleDevice device, QNScaleData data) {}
								@Override
								public void onGetStoredScale(QNBleDevice device, List<QNScaleStoreData> storedDataList) {}
								@Override
								public void onGetElectric(QNBleDevice device, int electric) {}
								@Override
								public void onScaleStateChange(QNBleDevice device, int status) {}
							});

							// 장치 연결 상태 변경 리스너 설정
							AprilisDeviceQnscale.this.instance.setBleConnectionChangeListener(new QNBleConnectionChangeListener() {
								/**
								 * 연결 중
								 * @param device 장치 정보
								 */
								@Override
								public void onConnecting(QNBleDevice device) {
									Log.d(TAG, "BLUETOOTH DEVICE STATUS : CONNECTING");
								}

								// 연결 됨
								@Override
								public void onConnected(QNBleDevice device) {

									// 연결된 장치가 없는 경우
									//if(AprilisDeviceQnscale.this.connectDevice == null)
									{
										Log.d(TAG, "BLUETOOTH DEVICE STATUS : CONNECTED " + device);

										// 타임 아웃 취소
										if(AprilisDeviceQnscale.this.connectionTimeoutHandler != null) {
											AprilisDeviceQnscale.this.connectionTimeoutHandler.removeCallbacks(AprilisDeviceQnscale.this.connectionTimeoutRunnable);
											AprilisDeviceQnscale.this.connectionTimeoutRunnable = null;
											AprilisDeviceQnscale.this.connectionTimeoutHandler = null;
										}

										// 연결된 장치 정보 저장
										AprilisDeviceQnscale.this.connectDevice = device;

										try {
											// 성공으로 결과 반환
											callbackContext.success(new ObjectMapper().writeValueAsString(new AprilisDeviceQnscaleResponse(true, "Device connected", device)));
										} catch (JsonProcessingException e) {
										}
									}
								}

								@Override
								public void onServiceSearchComplete(QNBleDevice device) {
								}

								// 연결 해제 중
								@Override
								public void onDisconnecting(QNBleDevice device) {
									Log.d(TAG, "BLUETOOTH DEVICE STATUS : DISCONNECTING");
								}

								// 연결 해제 됨
								@Override
								public void onDisconnected(QNBleDevice device) {
									Log.d(TAG, "BLUETOOTH DEVICE STATUS : DISCONNECTED");

									// 연결 장치 정보 초기화
									AprilisDeviceQnscale.this.connectDevice = null;
								}

								// 연결 오류
								@Override
								public void onConnectError(QNBleDevice device, int errorCode) {
									Log.e(TAG, "BLUETOOTH DEVICE STATUS : ERROR " + errorCode);
								}

							});

							// 장치 연결
							AprilisDeviceQnscale.this.instance.connectDevice(device, user, new QNResultCallback(){
								/**
								 * 장치 연결 결과
								 * @param code 결과 코드
								 * @param msg 에러 메세지
								 */
								@Override
								public void onResult(int code, String msg) {
									Log.d(TAG, "device connected : " + code + ", " + msg);
								}
							});
						}
					}

					/**
					 * 장치 검색 시작 이벤트
					 */
					@Override
					public void onStartScan() {
						Log.d(TAG, "start scan");
					}

					/**
					 * 장치 검색 중지 이벤트
					 */
					@Override
					public void onStopScan() {
						Log.d(TAG, "stop scan");
					}

					/**
					 * 장치 검색 에러 이벤트
					 * @param code
					 */
					@Override
					public void onScanFail(int code) {
						Log.e(TAG, "scan fail : " + code);

						try {
							// 스캔 에러 반환
							Log.e(TAG, "AprilisDeviceQnscale.startDiscovery QNBleDeviceDiscoveryListener.onScanFail : Scan failure");
							callbackContext.error(new ObjectMapper().writeValueAsString(new AprilisDeviceQnscaleResponse(false, "Scan failure")));
						} catch (JsonProcessingException e) {
						}
					}

					@Override
					public void onBroadcastDeviceDiscover(QNBleBroadcastDevice device) {

					}

					@Override
					public void onKitchenDeviceDiscover(QNBleKitchenDevice qnBleKitchenDevice) {

					}
				}
		);

		/**
		 * 장치 검색 시작
		 */
		this.instance.startBleDeviceDiscovery(new QNResultCallback() {
			/**
			 * 장치 검색 시작 결과
			 * @param code 결과 코드
			 * @param msg 에러 메세지
			 */
			@Override
			public void onResult(int code, String msg) {
				Log.d(TAG, "start ble device : " + code + ", " + msg);
			}
		});
	}

	/**
	 * 데이터 리스너 설정
	 * @param callbackContext 결과 콜백 컨텍스트
	 */
	private void setDataListener(CallbackContext callbackContext){

		// 1분 동안 검색이 되지 않는 경우 타임 아웃 시키는 핸들러와 실행 생성
		this.syncDataTimeoutHandler = new android.os.Handler();
		this.syncDataTimeoutRunnable = new Runnable() {
			public void run() {

				try {
					// 타임 아웃 에러 반환
					Log.e(TAG, "AprilisDeviceQnscale.setDataListener : Sync. data timeout");
					callbackContext.error(new ObjectMapper().writeValueAsString(new AprilisDeviceQnscaleResponse(false, "Sync. data timeout")));
				} catch (JsonProcessingException e) {
				}
			}
		};
		// 30초 뒤 타임 아웃 실행
		this.syncDataTimeoutHandler.postDelayed(this.syncDataTimeoutRunnable, 1000 * 30);

		// 데이터 리스너 설정
		this.instance.setDataListener(new QNScaleDataListener() {

			/**
			 * 실시간 몸무게 수신
			 * @param device 장치 정보 객체
			 * @param weight 실시간 몸무게
			 */
			@Override
			public void onGetUnsteadyWeight(QNBleDevice device, double weight) {
			}

			/**
			 * 측정된 데이터 수신
			 * @param device 장치 정보 객체
			 * @param data 측정 데이터 객체
			 */
			@Override
			public void onGetScaleData(QNBleDevice device, QNScaleData data) {

				// 타임 아웃 취소
				if(AprilisDeviceQnscale.this.syncDataTimeoutHandler != null) {
					AprilisDeviceQnscale.this.syncDataTimeoutHandler.removeCallbacks(AprilisDeviceQnscale.this.syncDataTimeoutRunnable);
					AprilisDeviceQnscale.this.syncDataTimeoutRunnable = null;
					AprilisDeviceQnscale.this.syncDataTimeoutHandler = null;
				}

//				// 장치 연결 해제
//				AprilisDeviceQnscale.this.disconnect(device, null);

//				ArrayList<Map<String, Double>> datas = new ArrayList<Map<String, Double>>();
//
//				Map<String, Double> keyValues = new HashMap<String, Double>();
//				for(QNScaleItemData key : data.getAllItem() ){
//
//					keyValues.put(key.getName(), key.getValue());
//				}
//
//				datas.add(keyValues);

				AprilisDeviceQnscaleData responseData = new AprilisDeviceQnscaleData();
				for(QNScaleItemData key : data.getAllItem() ){

					switch(key.getName()) {
						case "bone mass":
							responseData.setBoneMass(key.getValue());
							break;
						case "BMR":
							responseData.setBmr(key.getValue());
							break;
						case "weight":
							responseData.setWeight(key.getValue());
							break;
						case "metabolic age":
							responseData.setMetabolicAge(key.getValue());
							break;
						case "body fat rate":
							responseData.setBodyFatRate(key.getValue());
							break;
						case "body type":
							responseData.setBodyType(key.getValue());
							break;
						case "muscle mass":
							responseData.setMuscleMass(key.getValue());
							break;
						case "body water rate":
							responseData.setBodyWaterRate(key.getValue());
							break;
						case "protein":
							responseData.setProtein(key.getValue());
							break;
						case "muscle rate":
							responseData.setMuscleRate(key.getValue());
							break;
						case "visceral fat":
							responseData.setVisceralFat(key.getValue());
							break;
						case "BMI":
							responseData.setBmi(key.getValue());
							break;
					}
				}


				try {

					Log.d(TAG, "AprilisDeviceQnscale.onGetScaleData : Sync. data = " + responseData.toString());

					// 성공으로 측정 데이터 반환
					String dataString = new ObjectMapper().writeValueAsString(new AprilisDeviceQnscaleResponse(true, "Data received"));

					Log.d(TAG, "AprilisDeviceQnscale.onGetScaleData : Sync. data string = " + dataString);

					callbackContext.success(dataString);
				} catch (JsonProcessingException e) {
					Log.e(TAG, "AprilisDeviceQnscale.onGetScaleData : error -  " + e.toString());
				}
			}

			/**
			 * 오프라인으로 저장된 데이터 수신
			 * @param device 장치 정보 객체
			 * @param storedDataList
			 */
			@Override
			public void onGetStoredScale(QNBleDevice device, List<QNScaleStoreData> storedDataList) {

			}

			/**
			 * 배터리 정보 수신
			 * @param device 장치 정보 객체
			 * @param electric 남은 배터리 용량
			 */
			@Override
			public void onGetElectric(QNBleDevice device, int electric) {

			}

			/**
			 * 측정 상태 변경 (Bluetooth 연결 상태에 대한 콜백이 아님)
			 * @param device 장치 정보 객체
			 * @param status 상태
			 */
			@Override
			public void onScaleStateChange(QNBleDevice device, int status) {

			}

		});

	}

	/**
	 * 장치 연결 해제
	 * @param device
	 * @param callbackContext 결과 콜백 컨텍스트
	 */
	private void disconnect(QNBleDevice device, CallbackContext callbackContext){

		// 장치 연결 해제
		this.instance.disconnectDevice(device, new QNResultCallback() {
			/**
			 * 장치 연결 해제 결과
			 * @param code 결과 코드
			 * @param msg 에러 메세지
			 */
			@Override
			public void onResult(int code, String msg) {
				Log.d(TAG, "disconnect result : " + code + ", " + msg);
			}
		});

		if(callbackContext != null) {
			try {
				// 성공으로 결과 반환
				callbackContext.success(new ObjectMapper().writeValueAsString(new AprilisDeviceQnscaleResponse(true, "device disconnected")));
			} catch (JsonProcessingException e) {
			}

		}
	}

}
