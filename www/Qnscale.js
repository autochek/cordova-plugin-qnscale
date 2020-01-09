var exec = require('cordova/exec');

exports.coolMethod = function (arg0, success, error) {
    exec(success, error, 'Qnscale', 'coolMethod', [arg0]);
};
exports.connectQnscale=function(arg0, arg1, arg2, arg3, arg4, success, error) {
    exec(success, error, 'Qnscale', 'connectQnscale', [arg0, arg1, arg2, arg3, arg4]);
}