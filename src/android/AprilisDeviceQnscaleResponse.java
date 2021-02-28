package cordova.plugin.qnscale;

/**
 * 응답 결과 클래스
 */
public class AprilisDeviceQnscaleResponse {
	/**
	 * 성공 여부
	 */
	public boolean result = false;
	/**
	 * 성공/에러 메세지
	 */
	public String message = "";
	/**
	 * 결과 데이터
	 */
	public Object data = null;

	/**
	 * 생성자
	 * @param result 성공 여부
	 * @param message 성공/에러 메세지
	 */
	public AprilisDeviceQnscaleResponse(boolean result, String message) {
		this.result = result;
		this.message = message;
	}

	/**
	 * 생성자
	 * @param result 성공 여부
	 * @param message 성공/에러 메세지
	 * @param data 결과 데이터
	 */
	public AprilisDeviceQnscaleResponse(boolean result, String message, Object data) {
		this.result = result;
		this.message = message;
		this.data = data;
	}
}
