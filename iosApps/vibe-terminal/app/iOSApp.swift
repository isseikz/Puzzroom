import SwiftUI
import Security

@main
struct iOSApp: App {
    init() {
        seedKeychainIfUITest()
        deleteKeychainIfUITest()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

/// When launched by a UITest with `--seed-keychain id:password` arguments,
/// write those entries into the Keychain so the test can connect without
/// requiring the form to be filled.  This is only active under UITest launches.
/// When launched with `--delete-keychain key [key ...]`, remove those Keychain
/// entries so the SSH password dialog appears in UITests regardless of prior state.
private func deleteKeychainIfUITest() {
    let args = CommandLine.arguments
    guard let idx = args.firstIndex(of: "--delete-keychain"), idx + 1 < args.count else { return }
    let service = "tokyo.isseikuzumaki.vibeterminal"
    for key in args[(idx + 1)...] {
        guard !key.hasPrefix("-") else { break }
        let q: [String: Any] = [
            kSecClass as String:       kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key
        ]
        let st = SecItemDelete(q as CFDictionary)
        print("[UITest delete] \(key): \(st == 0 || st == errSecItemNotFound ? "ok" : "err \(st)")")
    }
}

private func seedKeychainIfUITest() {
    let args = CommandLine.arguments
    guard let idx = args.firstIndex(of: "--seed-keychain"), idx + 1 < args.count else { return }
    let service = "tokyo.isseikuzumaki.vibeterminal"
    for pair in args[(idx + 1)...] {
        guard pair.contains(":") else { break }
        let parts = pair.split(separator: ":", maxSplits: 1)
        guard parts.count == 2 else { break }
        let key  = String(parts[0])
        let value = String(parts[1])
        // Delete existing, then add
        let q: [String: Any] = [
            kSecClass as String:       kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key
        ]
        SecItemDelete(q as CFDictionary)
        var addQ = q
        addQ[kSecValueData as String] = value.data(using: .utf8)!
        let st = SecItemAdd(addQ as CFDictionary, nil)
        print("[UITest seed] \(key): \(st == 0 ? "ok" : "err \(st)")")
    }
}
