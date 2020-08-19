package cordova.plugin.qnscale;

import android.content.Intent;
import android.util.Log;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import android.content.Context;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.yolanda.health.qnblesdk.listener.QNBleDeviceDiscoveryListener;
import com.yolanda.health.qnblesdk.listener.QNResultCallback;
import com.yolanda.health.qnblesdk.listener.QNScaleDataListener;
import com.yolanda.health.qnblesdk.listener.QNBleConnectionChangeListener;
import com.yolanda.health.qnblesdk.out.QNBleApi;
import com.yolanda.health.qnblesdk.out.QNBleBroadcastDevice;
import com.yolanda.health.qnblesdk.out.QNBleDevice;
import com.yolanda.health.qnblesdk.out.QNBleKitchenDevice;
import com.yolanda.health.qnblesdk.out.QNScaleData;
import com.yolanda.health.qnblesdk.out.QNScaleItemData;
import com.yolanda.health.qnblesdk.out.QNScaleStoreData;
import com.yolanda.health.qnblesdk.out.QNUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import cordova.plugin.qnscale.QnscaleResponse;



/**
 * This class echoes a string called from JavaScript.
 */
public class Qnscale extends CordovaPlugin {

	/**
	 * 로깅용 태그
	 */
	private String TAG = "aprilis.autochek.care";
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
            String userId = args.getString(1);
            int height = args.getInt(2);
            String gender = args.getString(3);
            int year = args.getInt(4);
            int month = args.getInt(5);
            int day = args.getInt(6);

			// API 인스턴스를 가져온다.
			this.instance = QNBleApi.getInstance(this.cordova.getActivity().getApplicationContext());

            // 장치 연결
            this.connect(deviceId, userId, height, gender, year, month, day, callbackContext);

            return true;
        }
        // 동기화 요청
		else if (action.equals("syncData")) {

			// API 인스턴스가 유효하지 않거나 장치 정보가 유효하지 않은 경우
			if (this.instance == null || this.connectDevice == null) {
				try {
					Log.e(TAG, "execute syncData : API instance is null. please connect device first");

					// 타임 아웃 에러 반환
					callbackContext.error(new ObjectMapper().writeValueAsString(new QnscaleResponse(false, "API instance is null. please connect device first")));
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
					callbackContext.success(new ObjectMapper().writeValueAsString(new QnscaleResponse(true, "Device disconnected")));
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
	 * @param userId 사용자 아이디
	 * @param height 키
	 * @param gender 성별
	 * @param year 생년
	 * @param month 생월
	 * @param day 생일
	 * @param callbackContext 결과 콜백 컨텍스트
	 */
    private void connect(String deviceId, String userId, int height, String gender, int year, int month, int day, CallbackContext callbackContext) {

		Log.d(TAG, "try to connect : " + deviceId);

    	// 1분 동안 검색이 되지 않는 경우 타임 아웃 시키는 핸들러와 실행 생성
		this.connectionTimeoutHandler = new android.os.Handler();
		this.connectionTimeoutRunnable = new Runnable() {
            public void run() {
				try {

					// 장치 검색 중지
					Qnscale.this.instance.stopBleDeviceDiscovery(new QNResultCallback() {
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
					Log.e(TAG, "Qnscale.connect : Connection timeout");
					callbackContext.error(new ObjectMapper().writeValueAsString(new QnscaleResponse(false, "Connection timeout")));
				} catch (JsonProcessingException e) {
				}
            }
        };
		// 10초 뒤 타임 아웃 실행
		this.connectionTimeoutHandler.postDelayed(this.connectionTimeoutRunnable, 1000 * 10);

        // 설정 파일 경로
        String configFilePath = "file:///android_asset/123456789.qn";
        // SDK를 초기화 한다.
        this.instance.initSdk("123456789", configFilePath, new QNResultCallback() {
			/**
			 * 초기화 결과
			 * @param code 결과 코드
			 * @param msg 에러 메세지
			 */
			@Override
            public void onResult(int code, String msg) {

                Log.d(TAG, "API Initialized : " + code + ", " + msg);

				// 사용자 생년월일 저장
				Calendar calendar = Calendar.getInstance();
				calendar.set(year, month-1, day);
				// 사용자 정보 생성
				QNUser user = Qnscale.this.instance.buildUser(userId, height, gender, new Date(calendar.getTimeInMillis()), 0, new QNResultCallback(){
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
                Qnscale.this.startDiscovery(deviceId, user, callbackContext);
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
							Qnscale.this.instance.stopBleDeviceDiscovery(new QNResultCallback() {
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
							Qnscale.this.instance.setDataListener(new QNScaleDataListener() {
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
							Qnscale.this.instance.setBleConnectionChangeListener(new QNBleConnectionChangeListener() {
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
									//if(Qnscale.this.connectDevice == null)
									{
										Log.d(TAG, "BLUETOOTH DEVICE STATUS : CONNECTED " + device);

										// 타임 아웃 취소
										if(Qnscale.this.connectionTimeoutHandler != null) {
											Qnscale.this.connectionTimeoutHandler.removeCallbacks(Qnscale.this.connectionTimeoutRunnable);
											Qnscale.this.connectionTimeoutRunnable = null;
											Qnscale.this.connectionTimeoutHandler = null;
										}

										// 연결된 장치 정보 저장
										Qnscale.this.connectDevice = device;

										try {
											// 성공으로 결과 반환
											callbackContext.success(new ObjectMapper().writeValueAsString(new QnscaleResponse(true, "Device connected", device)));
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
									Qnscale.this.connectDevice = null;
								}

								// 연결 오류
								@Override
								public void onConnectError(QNBleDevice device, int errorCode) {
									Log.e(TAG, "BLUETOOTH DEVICE STATUS : ERROR " + errorCode);
								}

							});

							// 장치 연결
							Qnscale.this.instance.connectDevice(device, user, new QNResultCallback(){
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
							Log.e(TAG, "Qnscale.startDiscovery QNBleDeviceDiscoveryListener.onScanFail : Scan failure");
							callbackContext.error(new ObjectMapper().writeValueAsString(new QnscaleResponse(false, "Scan failure")));
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
					Log.e(TAG, "Qnscale.setDataListener : Sync. data timeout");
					callbackContext.error(new ObjectMapper().writeValueAsString(new QnscaleResponse(false, "Sync. data timeout")));
				} catch (JsonProcessingException e) {
				}
			}
		};
		// 10초 뒤 타임 아웃 실행
		this.syncDataTimeoutHandler.postDelayed(this.syncDataTimeoutRunnable, 1000 * 10);

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


//                String json = "{";
//                boolean isFirst = true;
//                for(QNScaleItemData key : data.getAllItem() ){
//                    if(isFirst){
//                        isFirst=false;
//                    } else {
//                        json += ",";
//                    }
//                    Log.d("detected", ""+key.getName()+" "+key.getValue());
//                    json += "\""+key.getName()+"\":"+key.getValue();
//                }
//                json += "}";
//
//				callbackContext.success(json);

				// 타임 아웃 취소
				if(Qnscale.this.syncDataTimeoutHandler != null) {
					Qnscale.this.syncDataTimeoutHandler.removeCallbacks(Qnscale.this.syncDataTimeoutRunnable);
					Qnscale.this.syncDataTimeoutRunnable = null;
					Qnscale.this.syncDataTimeoutHandler = null;
				}

				// 장치 연결 해제
				Qnscale.this.disconnect(device, null);

				Map<String, Double> keyValues = new HashMap<String, Double>();
                for(QNScaleItemData key : data.getAllItem() ){
					keyValues.put(key.getName(), key.getValue());
				}

				try {
					// 성공으로 측정 데이터 반환
					callbackContext.success(new ObjectMapper().writeValueAsString(new QnscaleResponse(true, "Data received", keyValues)));
				} catch (JsonProcessingException e) {
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
				callbackContext.success(new ObjectMapper().writeValueAsString(new QnscaleResponse(true, "device disconnected")));
			} catch (JsonProcessingException e) {
			}

		}
    }

}
