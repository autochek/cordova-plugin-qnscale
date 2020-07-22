var exec = require('cordova/exec');

exports.connect = function(deviceId, userId, height, gender, year, month, day, success, error) {
    exec(success, error, 'Qnscale', 'connect', [deviceId, userId, height, gender, year, month, day]);
};

exports.syncData = function(success, error) {
	exec(success, error, 'Qnscale', 'syncData', []);
};

exports.disconnect = function(success, error) {
	exec(success, error, 'Qnscale', 'disconnect', []);
};