import FoodLens

@objc(Foodlens) class Foodlens : CDVPlugin {
    
    

  @objc(coolMethod:)
  func coolMethod(command: CDVInvokedUrlCommand) {
    var pluginResult = CDVPluginResult(
      status: CDVCommandStatus_ERROR
    )

    let echo = command.arguments[0] as? String ?? ""

    if echo != nil && echo.count > 0 {
        pluginResult = CDVPluginResult(
            status: CDVCommandStatus_OK,
            messageAs: echo
        )
    }
    else {
        pluginResult = CDVPluginResult(
            status: CDVCommandStatus_ERROR
        )
    }

    self.commandDelegate!.send(
      pluginResult, 
      callbackId: command.callbackId
    )
  }
    
    @objc(launchFoodlensUI:)
    func launchFoodlensUI(command: CDVInvokedUrlCommand) {
        
        class FlHandler : UserServiceResultHandler {
            var parent:Foodlens
            var command:CDVInvokedUrlCommand
            
            init(parent p:Foodlens, command c:CDVInvokedUrlCommand){
                self.parent = p
                self.command = c
            }
            func onSuccess(_ result : RecognitionResult){

                let pluginResult = CDVPluginResult(
                  status: CDVCommandStatus_OK,
                  messageAs: result.toJSONString()
                
                )
                self.parent.commandDelegate.send(
                  pluginResult,
                  callbackId: self.command.callbackId
                )
                /*
                let jsonstring:String = result.toJSONString()!
                let jsondata = jsonstring.data(using: .utf8)!
                
                do{
                    if let jsonArray = try JSONSerialization.jsonObject(with: jsondata, options : .allowFragments) as? [Dictionary<String,Any>]{
                        
                        let pluginResult = CDVPluginResult(
                          status: CDVCommandStatus_OK,
                          messageAs: jsonArray
                        
                        )
                        self.parent.commandDelegate.send(
                          pluginResult,
                          callbackId: self.command.callbackId
                        )
                        
                    }else{
                        
                    }
                }catch let error as NSError {
                    let pluginResult = CDVPluginResult(
                      status: CDVCommandStatus_ERROR,
                      messageAs: error.localizedDescription
                    )
                    self.parent.commandDelegate.send(
                      pluginResult,
                      callbackId: self.command.callbackId
                    )
                }
                
                */
            }
            func onCancel(){
                let pluginResult = CDVPluginResult(
                  status: CDVCommandStatus_NO_RESULT
                )
                self.parent.commandDelegate.send(
                  pluginResult,
                  callbackId: self.command.callbackId
                )
            }
            func onError(_ error : BaseError){
                let pluginResult = CDVPluginResult(
                  status: CDVCommandStatus_ERROR,
                  messageAs: error.getMessage()
                )
                self.parent.commandDelegate.send(
                  pluginResult,
                  callbackId: self.command.callbackId
                )
            }
        }
        
        
        FoodLens.uiServiceMode = .userSelectedWithCandidates

        let uiService = FoodLens.createUIService(accessToken: "dc76c8d0493411e9b4750800200c9a66")
        
        uiService.startUIService(parent: self.viewController, completionHandler:FlHandler(parent:self, command:command))
        
        
    }
    
}

    
