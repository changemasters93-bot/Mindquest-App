import FirebaseAnalytics
import ComposeApp

/// Bridges Kotlin analytics calls to the Firebase Analytics iOS SDK.
///
/// Conforms to `IosAnalyticsTrackerBridge` (the Kotlin `Bridge` interface
/// exported from ComposeApp). Registered in `AppDelegate` at launch.
final class AnalyticsBridge: IosAnalyticsTrackerBridge {

    static let shared = AnalyticsBridge()
    private init() {}

    func logEvent(name: String, params: [String: Any]) {
        Analytics.logEvent(name, parameters: params)
    }

    func logScreenView(screenName: String, screenClass: String?) {
        var params: [String: Any] = [
            AnalyticsParameterScreenName: screenName
        ]
        if let screenClass = screenClass {
            params[AnalyticsParameterScreenClass] = screenClass
        }
        Analytics.logEvent(AnalyticsEventScreenView, parameters: params)
    }

    func setUserId(userId: String?) {
        Analytics.setUserID(userId)
    }

    func setUserProperty(name: String, value: String?) {
        Analytics.setUserProperty(value, forName: name)
    }

    func setEnabled(enabled: Bool) {
        Analytics.setAnalyticsCollectionEnabled(enabled)
    }
}
