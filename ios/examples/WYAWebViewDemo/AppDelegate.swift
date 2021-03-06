//
//  AppDelegate.swift
//  WYAWebViewDemo
//
//  Created by 李世航 on 2018/9/8.
//  Copyright © 2018年 WeiYiAn. All rights reserved.
//

import UIKit
import WYAWebView
import UserNotifications
import WYAKit
@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplicationLaunchOptionsKey: Any]?) -> Bool {
        // Override point for customization after application launch.
        window = UIWindow(frame: UIScreen.main.bounds)
        window?.backgroundColor = UIColor.white
        #if DEBUG
        Bundle(path: "/Applications/InjectionIII.app/Contents/Resources/iOSInjection10.bundle")?.load()
        #endif

        let nav = UINavigationController(rootViewController: HomeViewController())
        window?.rootViewController = nav
        window?.makeKeyAndVisible()
        if #available(iOS 10.0, *) {
            UNUserNotificationCenter.current().requestAuthorization(options: [.alert,.sound,.badge]) { (success, error) in
                if success {
                    print("success")
                } else {
                    print("error")
                }
            }

        }else{
            if (UIApplication.shared.currentUserNotificationSettings?.types)!.rawValue != UIUserNotificationType.alert.rawValue|UIUserNotificationType.sound.rawValue|UIUserNotificationType.badge.rawValue {
                UIApplication.shared.registerUserNotificationSettings(UIUserNotificationSettings(types: UIUserNotificationType(rawValue: UIUserNotificationType.alert.rawValue|UIUserNotificationType.sound.rawValue|UIUserNotificationType.badge.rawValue), categories: nil))
            }
        }


        // 设置悬浮球

        WYAFloatBallManager.wya_addFloatVcS(["WYAHybridController","HybridViewController"])

        return true
    }
    
    /// 第三方应用打开本程序调用的方法
    ///
    /// - Parameters:
    ///   - app: <#app description#>
    ///   - url: <#url description#>
    ///   - options: <#options description#>
    /// - Returns: <#return value description#>
//    func application(_ app: UIApplication, open url: URL, options: [UIApplicationOpenURLOptionsKey : Any] = [:]) -> Bool {
//        let view = UIView(frame: (UIApplication.shared.keyWindow?.frame)!)
//        view.backgroundColor = .red
//        UIApplication.shared.keyWindow?.addSubview(view)
//        if url.absoluteString.hasPrefix("hybrid") {
//            // FIXME:第三方应用打开时需分解url获取相应的参数
////            UserDefaults.standard.set(<#T##value: Any?##Any?#>, forKey: "appParam")
//            return true
//        }
//        return false
//    }

    func applicationWillResignActive(_ application: UIApplication) {
        // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
        // Use this method to pause ongoing tasks, disable timers, and invalidate graphics rendering callbacks. Games should use this method to pause the game.
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
        // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
    }

    func applicationWillEnterForeground(_ application: UIApplication) {
        // Called as part of the transition from the background to the active state; here you can undo many of the changes made on entering the background.
    }

    func applicationDidBecomeActive(_ application: UIApplication) {
        // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
    }

    func applicationWillTerminate(_ application: UIApplication) {
        // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
    }


}


extension UIViewController{
    @objc func injected() {
        viewDidLoad()
        viewWillAppear(true)
        viewWillDisappear(true)
    }
}
