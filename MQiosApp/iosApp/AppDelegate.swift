import UIKit
import ComposeApp

@main
class AppDelegate: UIResponder, UIApplicationDelegate {
    var window: UIWindow?

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        #if DEBUG
        // ── Crash diagnostics (debug only) ───────────────────────────
        NSSetUncaughtExceptionHandler { exception in
            print("🔴 UNCAUGHT EXCEPTION: \(exception)")
            print("🔴 Reason: \(exception.reason ?? "nil")")
            print("🔴 Call stack:\n\(exception.callStackSymbols.joined(separator: "\n"))")
        }
        print("🟡 [MQ_IOS] AppDelegate: didFinishLaunching START")
        print("🟡 [MQ_IOS] Creating MainViewController...")
        #endif

        let mainViewController = MainViewControllerKt.MainViewController()

        #if DEBUG
        print("🟢 [MQ_IOS] MainViewController created OK")
        #endif

        window = UIWindow(frame: UIScreen.main.bounds)
        window?.rootViewController = mainViewController
        window?.makeKeyAndVisible()

        #if DEBUG
        print("🟢 [MQ_IOS] Window made visible, returning true")
        #endif

        return true
    }

    // ── Deep link handling for OAuth callback (mindquest://callback) ──
    func application(
        _ app: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey: Any] = [:]
    ) -> Bool {
        #if DEBUG
        print("🟡 [MQ_IOS] AppDelegate: deep link received → \(url)")
        print("🟡 [MQ_IOS]   scheme=\(url.scheme ?? "nil"), host=\(url.host ?? "nil")")
        #endif
        // Supabase Kotlin SDK handles the deep link automatically
        // via the Auth plugin's internal listener.
        return true
    }
}
