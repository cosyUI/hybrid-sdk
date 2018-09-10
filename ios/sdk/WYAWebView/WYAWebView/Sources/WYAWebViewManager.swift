//
//  WYAWebViewManager.swift
//  Alamofire
//
//  Created by 李世航 on 2018/8/15.
//

import UIKit

import Alamofire


protocol WebViewDelegate {
    func getNativeActionResult(obj:Any) -> Void
}

class WYAWebViewManager: NSObject {
    
    var nativeDelegate : WebViewDelegate?
    
    let netManager = NetworkReachabilityManager(host: "www.apple.com")
    
    
    
    func nativeAction(_ method:String, params:[String : String]) -> Void {
        /// 第一种方式
        let methodAndParams = method + "WithParams:"
        let sel = NSSelectorFromString(methodAndParams)
        
        /// 第二种方式
//        let sel : Selector = "aaaWithAsd:"
        self.performSelector(inBackground: sel, with: params)
        
        //执行原生过程
//        let string = system.connectionType()
        
        //返回原生方法执行结果
//        self.nativeDelegate?.getNativeActionResult(obj: string)
    }
    
    
}

extension WYAWebViewManager {
    /// 拆分url获取方法名形成字典
    ///
    /// - Parameter urlString: urlString
    /// - Returns: dic
    func cutString(urlString:String) -> NSMutableDictionary {
        print(urlString)
        let dic = NSMutableDictionary()
        if !urlString.contains("//") {
            return dic
        }
        let array = urlString.components(separatedBy: "//")
        dic.setValue(array.first ?? "不存在", forKey: "protocol")
        
        let lastString = array.last
        if !(lastString?.contains("?"))! {
            dic.setValue(lastString, forKey: "method")
            return dic
        }
        let methodArray = lastString?.components(separatedBy: "?")
        dic.setValue(methodArray?.first ?? "不存在", forKey: "method")
        
        let queryString = methodArray?.last
        if (queryString != nil) {
            let arra = queryString?.components(separatedBy: "=")
            
            dic.setValue(arra?.last, forKey: "id")
        }
        
        return dic
    }
    
    
    /// 字典转js字符串传递参数
    ///
    /// - Parameter dic: 字典
    /// - Returns: js字符串
    @objc func mutableDicToJSString(dic:NSMutableDictionary) -> String {
        let jsCopyTwo = "\(dic)"
        
        let a = jsCopyTwo.replacingOccurrences(of: "\n", with: "")
        let b = a.replacingOccurrences(of: " ", with: "")
        let c = b.replacingOccurrences(of: ";", with: "")
        let d = c.replacingOccurrences(of: "=", with: ":")
        
        var f : String?
        
        for value in dic.allValues {
            let str1 = value as! String
            let str2 = "'"
            
            
            
            let string = str2 + str1 + str2
            
            f = d.replacingOccurrences(of: (value as! String), with: string)
        }
        
        return f!
    }
    
    /// json字符串转字典
    ///
    /// - Parameter jsonString: json字符串
    /// - Returns: 字典
    func jsonStringToMutableDic(jsonString: String) -> Any {
        var dic : Any
        
        let data:Data = jsonString.data(using : .utf8)!
        
        do{
            dic = try JSONSerialization.jsonObject(with: data, options: JSONSerialization.ReadingOptions.mutableContainers)
        }catch{
            dic = [String:String]()
            print(error)
        }
        return dic
    }
    
    func getDictionaryFromJSONString(jsonString:String) ->NSDictionary{
        
        let jsonData:Data = jsonString.data(using: .utf8)!
        
        let dict = try? JSONSerialization.jsonObject(with: jsonData, options: .mutableContainers)
        if dict != nil {
            return dict as! NSDictionary
        }
        return NSDictionary()
        
        
    }
    
    func getJSONStringFromDictionary(dictionary:NSDictionary) -> String {
        if (!JSONSerialization.isValidJSONObject(dictionary)) {
            print("无法解析出JSONString")
            return ""
        }
        let data : NSData! = try? JSONSerialization.data(withJSONObject: dictionary, options: []) as NSData!
        let JSONString = NSString(data:data as Data,encoding: String.Encoding.utf8.rawValue)
        return JSONString! as String
        
    }
}

extension WYAWebViewManager {
    func registerSystemNotice(){
        
        weak var weak = self
        let note = NotificationCenter.default
        let device = UIDevice.current
        device.isBatteryMonitoringEnabled = true
        
        self.netManager?.listener = { status in
            print("网络状态: \(status)")
            //            self.netManager?.networkReachabilityStatus
            
            if (self.netManager?.isReachable)! {
                print("有网")
            }else{
                print("没网")
            }
        }
        self.netManager?.startListening()
        
        note.addObserver(forName: NSNotification.Name.UIDeviceBatteryLevelDidChange, object: nil, queue: OperationQueue.main) { (not) in
            //电池电量变化调用这个
            print("电池电量变化")
            print(not.userInfo as Any)
            let params = NSMutableDictionary(capacity: 0)
            params.setValue("1", forKey: "status")
            params.setValue("调用成功", forKey: "msg")
            
            let subParams = NSMutableDictionary(capacity: 0)
            subParams.setValue(device.batteryLevel, forKey: "level")
            
            switch device.batteryState {
            case .unknown:
                subParams.setValue(false, forKey: "isPlugged")
                break
                
            case .unplugged:
                subParams.setValue(false, forKey: "isPlugged")
                break
                
            case .charging:
                subParams.setValue(true, forKey: "isPlugged")
                break
                
            case .full:
                subParams.setValue(false, forKey: "isPlugged")
                break
                
            }
            params.setValue(subParams, forKey: "data")
            
            //            let string = self.mutableDicToJSString(dic: params)
            
            //            self.nativeDelegate?.getNativeActionResult(obj: string)
        }
        
        note.addObserver(forName: NSNotification.Name.UIDeviceBatteryStateDidChange, object: nil, queue: OperationQueue.main) { (not) in
            //检测电池状态
            print("电池状态")
            print(not.userInfo as Any)
        }
        
        note.addObserver(forName: NSNotification.Name.UIApplicationDidEnterBackground, object: nil, queue: OperationQueue.main) { (not) in
            //进入后台
            print("后台")
        }
        
        note.addObserver(forName: NSNotification.Name.UIApplicationDidBecomeActive, object: nil, queue: OperationQueue.main) { (not) in
            //进入前台
            print("前台")
        }
        
        note.addObserver(forName: NSNotification.Name.UIApplicationWillResignActive, object: nil, queue: OperationQueue.main) { (not) in
            //进入休眠
            print("休眠")
        }
        
        note.addObserver(forName: NSNotification.Name.UIKeyboardWillShow, object: nil, queue: OperationQueue.main) { (not) in
            //键盘弹出
            print("键盘将要弹出")
        }
        
        note.addObserver(forName: NSNotification.Name.UIKeyboardDidShow, object: nil, queue: OperationQueue.main) { (not) in
            //键盘弹出
            print("键盘已经弹出")
        }
        
        note.addObserver(forName: NSNotification.Name.UIKeyboardWillHide, object: nil, queue: OperationQueue.main) { (not) in
            //键盘将要消失
            print("键盘消失")
        }
        
        note.addObserver(forName: NSNotification.Name.UIKeyboardDidHide, object: nil, queue: OperationQueue.main) { (not) in
            //键盘消失
            print("键盘消失")
        }
        
        note.addObserver(forName: NSNotification.Name.UIApplicationUserDidTakeScreenshot, object: nil, queue: OperationQueue.main) { (not) in
            //截屏
            print("截屏")
            print(not)
            
            UIGraphicsBeginImageContextWithOptions((UIApplication.shared.keyWindow?.frame.size)!, false, UIScreen.main.scale)
            UIApplication.shared.keyWindow?.layer.render(in: UIGraphicsGetCurrentContext()!)
            
            let image = UIGraphicsGetImageFromCurrentImageContext()
            UIGraphicsEndImageContext()
            
            print(image as Any)
        }
        
        
    }
}

extension WYAWebViewManager {
    func getNetWork(params:[String : String]) -> Void {
        //获取网络状态
        
        //回传信息
        self.nativeDelegate?.getNativeActionResult(obj: "sss")
    }
}
