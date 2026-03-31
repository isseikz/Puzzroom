import XCTest

final class KeyboardTapTests: XCTestCase {

    private let app = XCUIApplication()
    private let dir = "/Users/isseikz/AndroidStudioProjects/Puzzroom/iosApps/vibe-terminal/screenshots"

    override func setUpWithError() throws {
        continueAfterFailure = false
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

    func testTapToShowKeyboard() throws {
        sleep(2)

        // Tap Demo Server (second Connect button)
        let demoBtn = app.buttons.matching(
            NSPredicate(format: "label == 'Connect'")
        ).element(boundBy: 1)
        XCTAssertTrue(demoBtn.waitForExistence(timeout: 5))
        demoBtn.tap()
        sleep(1)

        // Handle SSH password dialog
        let dialogTitle = app.staticTexts["Enter SSH Password"]
        if dialogTitle.waitForExistence(timeout: 3) {
            sleep(1)
            app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.50)).tap()
            sleep(1)
            app.typeText("R3v!ew#2026$Secure@VT")
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

        // Wait for terminal to connect
        sleep(6)
        shot("kb_01_connected_no_keyboard")

        // Tap terminal body to trigger keyboard
        app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.35)).tap()
        sleep(2)
        shot("kb_02_after_tap_keyboard_visible")

        // Verify keyboard appeared
        XCTAssertTrue(
            app.keyboards.firstMatch.waitForExistence(timeout: 5),
            "Software keyboard should appear after tapping the terminal"
        )
        shot("kb_03_keyboard_confirmed")
    }
}
