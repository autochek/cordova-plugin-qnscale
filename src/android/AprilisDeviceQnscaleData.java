package cordova.plugins.aprilis.device.qnscale;

/**
 * 데이터 응답 클래스
 */
public class AprilisDeviceQnscaleData {
	/**
	 * 체중
	 */
	private double weight = 0;
	/**
	 * 골량
	 */
	private double boneMass = 0;
	/**
	 * BMI
	 */
	private double bmi = 0;
	/**
	 * BMR
	 */
	private double bmr = 0;
	/**
	 * 신진 대사 연령
	 */
	private double metabolicAge = 0;
	/**
	 * 체지방률
	 */
	private double bodyFatRate = 0;
	/**
	 * 체형 지수
	 */
	private double bodyType = 0;
	/**
	 * 근육량
	 */
	private double muscleMass = 0;
	/**
	 * 체수분율
	 */
	private double bodyWaterRate = 0;
	/**
	 * 피하 지방
	 */
	private double subcutaneousFat = 0;
	/**
	 * 단백질
	 */
	private double protein = 0;
	/**
	 * 근육 비율
	 */
	private double muscleRate = 0;
	/**
	 * 내장 지방
	 */
	private double visceralFat = 0;

	@Override
	public String toString() {
		return "AprilisDeviceQnscaleData{" +
				"weight=" + weight +
				", boneMass=" + boneMass +
				", bmi=" + bmi +
				", bmr=" + bmr +
				", metabolicAge=" + metabolicAge +
				", bodyFatRate=" + bodyFatRate +
				", bodyType=" + bodyType +
				", muscleMass=" + muscleMass +
				", bodyWaterRate=" + bodyWaterRate +
				", subcutaneousFat=" + subcutaneousFat +
				", protein=" + protein +
				", muscleRate=" + muscleRate +
				", visceralFat=" + visceralFat +
				'}';
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	public double getBoneMass() {
		return boneMass;
	}

	public void setBoneMass(double boneMass) {
		this.boneMass = boneMass;
	}

	public double getBmi() {
		return bmi;
	}

	public void setBmi(double bmi) {
		this.bmi = bmi;
	}

	public double getBmr() {
		return bmr;
	}

	public void setBmr(double bmr) {
		this.bmr = bmr;
	}

	public double getMetabolicAge() {
		return metabolicAge;
	}

	public void setMetabolicAge(double metabolicAge) {
		this.metabolicAge = metabolicAge;
	}

	public double getBodyFatRate() {
		return bodyFatRate;
	}

	public void setBodyFatRate(double bodyFatRate) {
		this.bodyFatRate = bodyFatRate;
	}

	public double getBodyType() {
		return bodyType;
	}

	public void setBodyType(double bodyType) {
		this.bodyType = bodyType;
	}

	public double getMuscleMass() {
		return muscleMass;
	}

	public void setMuscleMass(double muscleMass) {
		this.muscleMass = muscleMass;
	}

	public double getBodyWaterRate() {
		return bodyWaterRate;
	}

	public void setBodyWaterRate(double bodyWaterRate) {
		this.bodyWaterRate = bodyWaterRate;
	}

	public double getSubcutaneousFat() {
		return subcutaneousFat;
	}

	public void setSubcutaneousFat(double subcutaneousFat) {
		this.subcutaneousFat = subcutaneousFat;
	}

	public double getProtein() {
		return protein;
	}

	public void setProtein(double protein) {
		this.protein = protein;
	}

	public double getMuscleRate() {
		return muscleRate;
	}

	public void setMuscleRate(double muscleRate) {
		this.muscleRate = muscleRate;
	}

	public double getVisceralFat() {
		return visceralFat;
	}

	public void setVisceralFat(double visceralFat) {
		this.visceralFat = visceralFat;
	}
}
