import UIKit
import ComposeApp
import FirebaseCore
import FirebaseRemoteConfig

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
        #endif

        // ── Firebase init ────────────────────────────────────────────
        FirebaseApp.configure()

        #if DEBUG
        print("🟢 [MQ_IOS] Firebase configured")
        #endif

        // ── Remote Config: load defaults from plist, fetch & activate ─
        RemoteConfigProvider.shared.initialize { [weak self] in
            self?.applyUpdateConfig()
        }

        #if DEBUG
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

    // ── Pass Remote Config values to Kotlin ──────────────────────────
    private func applyUpdateConfig() {
        let raw = RemoteConfigProvider.shared.getString(forKey: "ios_update_config")
        let config = UpdateConfigParser.parse(raw)

        #if DEBUG
        print("🟢 [MQ_IOS] Update config: min=\(config.min), latest=\(config.latest)")
        #endif

        IosAppUpdateChecker.shared.setConfig(
            minVersionCode: Int32(config.min),
            latestVersionCode: Int32(config.latest),
            updateMessage: config.message,
            storeUrl: config.storeUrl
        )
    }
}
