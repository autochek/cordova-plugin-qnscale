var exec = require('cordova/exec');

exports.connect = function(deviceId, connectionTimeoutSec, userId, gender, year, height, success, error) {
    exec(success, error, 'AprilisDeviceQnscale', 'connect', [deviceId, connectionTimeoutSec, userId, gender, year, height]);
};

exports.syncData = function(success, error) {
	exec(success, error, 'AprilisDeviceQnscale', 'syncData', []);
};

exports.disconnect = function(success, error) {
	exec(success, error, 'AprilisDeviceQnscale', 'disconnect', []);
};
