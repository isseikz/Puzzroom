import XCTest

final class ConnectionScreenshotTests: XCTestCase {

    private let app = XCUIApplication()
    private let dir = "/Users/isseikz/AndroidStudioProjects/Puzzroom/iosApps/vibe-terminal/screenshots"

    override func setUpWithError() throws {
        continueAfterFailure = false
        // Delete any previously stored password for Demo Server (id=2) so the
        // SSH password dialog is guaranteed to appear.  The app process has the
        // Keychain entitlement; the UITest process does not.
        app.launchArguments = ["--delete-keychain", "vibe_password_2"]
        app.launch()
    }

    override func tearDownWithError() throws {
        app.terminate()
    }

    private func shot(_ name: String) {
        let s = XCUIScreen.main.screenshot()
        let a = XCTAttachment(screenshot: s); a.name = name; a.lifetime = .keepAlways; add(a)
        try? s.pngRepresentation.write(to: URL(fileURLWithPath: "\(dir)/\(name).png"))
    }

    func testCaptureConnectionFlow() throws {
        // "Demo Server" (34.57.69.23:2222 / reviewer) was seeded into the Room
        // DB directly; its password is seeded via the --seed-keychain launch arg.
        //
        // Button frames from hierarchy dump (402×874 logical pts):
        //   [3] Connect "Test"         frame=(16, 142, 370, 96)
        //   [4] Connect "Demo Server"  frame=(16, 254, 370, 96)

        // ── 1. Connection list ──────────────────────────────────────────────
        sleep(2)
        shot("01_connection_list")

        // ── 2. Tap "Demo Server" (second "Connect" button) ──────────────────
        let demoBtn = app.buttons.matching(
            NSPredicate(format: "label == 'Connect'")
        ).element(boundBy: 1)
        XCTAssertTrue(demoBtn.waitForExistence(timeout: 5))
        demoBtn.tap()
        sleep(1)
        shot("02_connecting")

        // ── 2b. Handle SSH password dialog (if Keychain lookup returned nil) ──
        // The "Enter SSH Password" dialog may appear when the stored credential
        // is absent.  Compose's password field is not a UITextField, so we use
        // a coordinate tap to focus it, type the password, then tap Connect.
        let dialogTitle = app.staticTexts["Enter SSH Password"]
        if dialogTitle.waitForExistence(timeout: 3) {
            // Allow dialog animation to settle, then tap password field.
            // y=0.50 reliably hits the password text field in the centre of the dialog.
            sleep(1)
            app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.50)).tap()
            sleep(1)
            app.typeText("R3v!ew#2026$Secure@VT")
            // Tap the dialog's "Connect" button.  The Compose sheet does not disable the
            // background list buttons in XCUITest's accessibility tree, so we pick the
            // "Connect" button with the greatest Y origin (i.e. the one in the dialog,
            // which is lower on screen than the list cards).
            sleep(1)
            let allConnects = app.buttons.matching(NSPredicate(format: "label == 'Connect'"))
            var dialogConnect: XCUIElement? = nil
            var maxY: CGFloat = -1
            for i in 0..<allConnects.count {
                let b = allConnects.element(boundBy: i)
                if b.frame.origin.y > maxY { maxY = b.frame.origin.y; dialogConnect = b }
            }
            dialogConnect?.tap()
        }

        sleep(6)
        shot("03_connected_terminal")

        // ── 3. Show terminal with keyboard row visible ─────────────────────
        // Compose terminal input isn't a UITextField so XCUITest typeText cannot
        // send characters to it.  Instead, capture the terminal with the fixed
        // key-row visible to demonstrate the custom keyboard UX.
        shot("04_terminal_with_keyboard_row")
    }
}
