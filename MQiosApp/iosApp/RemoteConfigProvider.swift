import FirebaseRemoteConfig

/// Centralized Firebase Remote Config provider for iOS.
///
/// Owns initialization, default loading (from `RemoteConfigDefaults.plist`),
/// and fetch lifecycle. All remote-config consumers read values through
/// this provider, making it the single place to add new keys in the future.
///
/// Usage:
/// ```swift
/// RemoteConfigProvider.shared.initialize {
///     let value = RemoteConfigProvider.shared.getString(forKey: "my_key")
/// }
/// ```
final class RemoteConfigProvider {
    static let shared = RemoteConfigProvider()

    private let remoteConfig = RemoteConfig.remoteConfig()
    private var initialized = false

    private init() {}

    /// Initialize Remote Config: apply settings, load plist defaults, fetch & activate.
    /// - Parameter completion: Called after fetch completes (or fails with defaults).
    func initialize(completion: @escaping () -> Void) {
        guard !initialized else {
            completion()
            return
        }

        let settings = RemoteConfigSettings()
        settings.minimumFetchInterval = 3600 // 1 hour cache
        remoteConfig.configSettings = settings

        // Load defaults from RemoteConfigDefaults.plist
        remoteConfig.setDefaults(fromPlist: "RemoteConfigDefaults")

        remoteConfig.fetchAndActivate { [weak self] status, error in
            if let error = error {
                #if DEBUG
                print("🔴 [MQ_REMOTE_CONFIG] Fetch failed: \(error.localizedDescription)")
                #endif
                // Defaults from plist are still available.
            } else {
                #if DEBUG
                print("🟢 [MQ_REMOTE_CONFIG] Fetched & activated (status: \(status.rawValue))")
                #endif
            }

            self?.initialized = true
            completion()
        }
    }

    /// Read a string value (returns plist default if not yet fetched or key is missing).
    func getString(forKey key: String) -> String {
        return remoteConfig.configValue(forKey: key).stringValue ?? ""
    }

    /// Read a number value.
    func getNumber(forKey key: String) -> NSNumber {
        return remoteConfig.configValue(forKey: key).numberValue
    }

    /// Read a boolean value.
    func getBool(forKey key: String) -> Bool {
        return remoteConfig.configValue(forKey: key).boolValue
    }
}
