var exec = require('cordova/exec');

exports.coolMethod = function (arg0, success, error) {
    exec(success, error, 'Qnscale', 'coolMethod', [arg0]);
};
exports.connectQnscale=function(success, error) {
    exec(success, error, 'Qnscale', 'connectQnscale', []);
}