package cordova.plugin.qnscale;

import android.content.Intent;
import android.util.Log;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import android.content.Context;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


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


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



/**
 * This class echoes a string called from JavaScript.
 */
public class Qnscale extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("coolMethod")) {
            String message = args.getString(0);
            this.coolMethod(message, callbackContext);
            return true;
        }
        if (action.equals("connectQnscale")) {
            this.connectQnscale(callbackContext);
            return true;
        }

        return false;
    }

    public void coolMethod(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            callbackContext.success("Echo from Coolmethod : "+message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }


    private String tag = "QnTest";
    private Context context;
    private QNBleApi instance;

    public void connectQnscale(CallbackContext callbackContext){

        callbackContext.success("connectQnscale called!");
        this.context = this.cordova.getActivity().getApplicationContext();
        this.instance = QNBleApi.getInstance(this.context);

        String configFilePath = "file:///android_asset/123456789.qn";
        this.instance.initSdk("123456789", configFilePath, new QNResultCallback() {
            @Override
            public void onResult(int code, String msg) {
                Log.d(tag, "Initialization File "+code + msg);
                Qnscale.this.startDiscovery();
            }
        });

    }


    private void startDiscovery(){
        this.instance.setBleDeviceDiscoveryListener(
                new QNBleDeviceDiscoveryListener() {
                    @Override
                    public void onDeviceDiscover(QNBleDevice device) {
                        // This method callback device object, can be processed by the device object
                        Log.d(tag, "onDeviceDiscover "+device);
                        Qnscale.this.connectDevice(device);

                    }

                    @Override
                    public void onStartScan() {
                        Log.d(tag, "startScan");
                        //Start scanning back
                    }

                    @Override
                    public void onStopScan() {
                        Log.d(tag, "stopScan");
                        //End the scan back
                    }

                    @Override
                    public void onScanFail(int code) {
                        Log.d(tag, "scanFail"+code);
                        // Scan the failed callback, there will be an error code to return, you can refer to the error code details page
                    }

                    @Override
                    public void onBroadcastDeviceDiscover(QNBleBroadcastDevice device) {
                        Log.d(tag, "Broadcast device discover"+device);
                        //The data callback of the broadcast scale, which will only call back the data of the broadcast scale.
                    }

                    @Override
                    public void onKitchenDeviceDiscover(QNBleKitchenDevice qnBleKitchenDevice) {

                    }
                }
        );

        this.instance.startBleDeviceDiscovery(new QNResultCallback() {
            @Override
            public void onResult(int code, String msg) {
                //This method does not return to the device, but indicates whether the scan was successfully started.
                Log.d(tag,code+":"+msg);

            }
        });
    }

    private void connectDevice(QNBleDevice device){
        this.instance.stopBleDeviceDiscovery(
            new QNResultCallback() {
                @Override
                public void onResult(int code, String msg) {
                    Log.d(tag, "stopBleDeviceDiscovery callback " + code + " - " + msg);
                }
            }
        );

        Calendar calendar = Calendar.getInstance();
        calendar.set(1989,9,13);
        QNUser user = instance.buildUser("testuser", 170, "male", new Date(calendar.getTimeInMillis()), 0, new QNResultCallback(){
            @Override
            public void onResult(int i, String s) {
                Log.d(tag, "buildUser onResult "+i+" "+s);
            }
        });




        this.instance.setDataListener(new QNScaleDataListener() {
            @Override
            public void onGetUnsteadyWeight(QNBleDevice device, double weight) {
                //The method is to receive unstable weight data. In one measurement, the method will call back multiple times until the data is stable and get the complete data.
            }

            @Override
            public void onGetScaleData(QNBleDevice device, QNScaleData data) {
                //This method is receiving complete measurement data

                for(QNScaleItemData key : data.getAllItem() ){
                    Log.d("detected", ""+key.getName()+" "+key.getValue());
                }
            }

            @Override
            public void onGetStoredScale(QNBleDevice device, List<QNScaleStoreData> storedDataList) {
                // The method is to receive the scale end storage data, the storage data processing method can refer to the demo, you can also define
            }

            @Override
            public void onGetElectric(QNBleDevice device, int electric) {
                // This is the percentage of the power obtained, only the amount of electricity obtained by the charging scale makes sense
            }

            @Override
            public void onScaleStateChange(QNBleDevice qnBleDevice, int i) {

            }


        });


        this.instance.connectDevice(device, user, new QNResultCallback(){

            @Override
            public void onResult(int i, String s) {
                Log.d(tag, ""+i+" : "+s);
            }
        });
    }

}
