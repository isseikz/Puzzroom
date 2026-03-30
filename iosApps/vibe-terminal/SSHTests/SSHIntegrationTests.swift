import XCTest
import VibeTerminal

final class SSHIntegrationTests: XCTestCase {

    // MARK: - SSH Shell

    func testSSHPasswordConnect() async throws {
        let repo = SshRepositoryIos()

        _ = try await withCheckedThrowingContinuation { (cont: CheckedContinuation<Any?, Error>) in
            repo.connect(
                host: TestConfig.sshHost, port: TestConfig.sshPort,
                username: TestConfig.sshUsername, password: TestConfig.sshPassword,
                initialCols: 80, initialRows: 24,
                initialWidthPx: 0, initialHeightPx: 0,
                startupCommand: nil
            ) { result, error in
                if let error { cont.resume(throwing: error) }
                else { cont.resume(returning: result) }
            }
        }

        XCTAssertTrue(repo.isConnected(), "SSH should be connected after connect()")

        try await withCheckedThrowingContinuation { (cont: CheckedContinuation<Void, Error>) in
            repo.sendInput(input: "whoami\n") { error in
                if let error { cont.resume(throwing: error) } else { cont.resume() }
            }
        }

        try await Task.sleep(nanoseconds: 2_000_000_000)

        try await withCheckedThrowingContinuation { (cont: CheckedContinuation<Void, Error>) in
            repo.disconnect { error in
                if let error { cont.resume(throwing: error) } else { cont.resume() }
            }
        }

        XCTAssertFalse(repo.isConnected(), "SSH should be disconnected after disconnect()")
    }

    // MARK: - SFTP

    func testSFTPListFiles() async throws {
        let repo = SshRepositoryIos()

        _ = try await withCheckedThrowingContinuation { (cont: CheckedContinuation<Any?, Error>) in
            repo.connect(
                host: TestConfig.sshHost, port: TestConfig.sshPort,
                username: TestConfig.sshUsername, password: TestConfig.sshPassword,
                initialCols: 80, initialRows: 24,
                initialWidthPx: 0, initialHeightPx: 0,
                startupCommand: nil
            ) { result, error in
                if let error { cont.resume(throwing: error) }
                else { cont.resume(returning: result) }
            }
        }

        XCTAssertTrue(repo.isConnected())

        let listResult = try await withCheckedThrowingContinuation { (cont: CheckedContinuation<Any?, Error>) in
            repo.listFiles(remotePath: "sftp-data") { result, error in
                if let error { cont.resume(throwing: error) }
                else { cont.resume(returning: result) }
            }
        }

        XCTAssertNotNil(listResult, "SFTP listFiles should return a result")
        print("SFTP listFiles result: \(listResult as Any)")

        try await withCheckedThrowingContinuation { (cont: CheckedContinuation<Void, Error>) in
            repo.disconnect { error in
                if let error { cont.resume(throwing: error) } else { cont.resume() }
            }
        }
    }

    func testSFTPReadFile() async throws {
        let repo = SshRepositoryIos()

        _ = try await withCheckedThrowingContinuation { (cont: CheckedContinuation<Any?, Error>) in
            repo.connect(
                host: TestConfig.sshHost, port: TestConfig.sshPort,
                username: TestConfig.sshUsername, password: TestConfig.sshPassword,
                initialCols: 80, initialRows: 24,
                initialWidthPx: 0, initialHeightPx: 0,
                startupCommand: nil
            ) { result, error in
                if let error { cont.resume(throwing: error) }
                else { cont.resume(returning: result) }
            }
        }

        let contentResult = try await withCheckedThrowingContinuation { (cont: CheckedContinuation<Any?, Error>) in
            repo.readFileContent(remotePath: "sftp-data/README.txt") { result, error in
                if let error { cont.resume(throwing: error) }
                else { cont.resume(returning: result) }
            }
        }

        XCTAssertNotNil(contentResult, "SFTP readFileContent should return content")
        print("README.txt content: \(contentResult as Any)")

        try await withCheckedThrowingContinuation { (cont: CheckedContinuation<Void, Error>) in
            repo.disconnect { error in
                if let error { cont.resume(throwing: error) } else { cont.resume() }
            }
        }
    }
}
