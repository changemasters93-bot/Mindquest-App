import Foundation

/// Parses the `ios_update_config` / `android_update_config` JSON string
/// into typed values for the app-update check.
enum UpdateConfigParser {

    struct Result {
        let min: Int
        let latest: Int
        let message: String
        let storeUrl: String
    }

    static func parse(_ raw: String) -> Result {
        guard let data = raw.data(using: .utf8),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return .defaults
        }
        return Result(
            min: json["min_version_code"] as? Int ?? 1,
            latest: json["latest_version_code"] as? Int ?? 1,
            message: json["update_message"] as? String ?? Self.defaultMessage,
            storeUrl: json["store_url"] as? String ?? Self.defaultStoreUrl
        )
    }

    // MARK: - Defaults

    private static let defaultMessage = "A new version of Mindquest is available!"
    private static let defaultStoreUrl = "https://apps.apple.com/app/mindquest"

    private static var defaults: Result {
        Result(min: 1, latest: 1, message: defaultMessage, storeUrl: defaultStoreUrl)
    }
}
