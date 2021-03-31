var exec = require('cordova/exec');

exports.connect = function(deviceId, connectionTimeoutSec, userId, gender, year, height, success, error) {
    exec(success, error, 'AprilisDeviceQnscale', 'connect', [deviceId, connectionTimeoutSec, userId, gender, year, height]);
};

exports.syncData = function(deviceId, success, error) {
	exec(success, error, 'AprilisDeviceQnscale', 'syncData', [deviceId]);
};

exports.disconnect = function(deviceId, success, error) {
	exec(success, error, 'AprilisDeviceQnscale', 'disconnect', [deviceId]);
};
