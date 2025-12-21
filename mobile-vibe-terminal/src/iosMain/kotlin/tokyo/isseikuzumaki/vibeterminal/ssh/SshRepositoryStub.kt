package tokyo.isseikuzumaki.vibeterminal.ssh

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import tokyo.isseikuzumaki.vibeterminal.domain.repository.SshRepository
import java.io.File

class SshRepositoryStub : SshRepository {
    override suspend fun connect(host: String, port: Int, username: String, password: String): Result<Unit> {
        return Result.failure(NotImplementedError("SSH not supported on iOS yet"))
    }
    override suspend fun disconnect() {}
    override fun isConnected(): Boolean = false
    override suspend fun executeCommand(command: String): Result<String> {
        return Result.failure(NotImplementedError("SSH not supported on iOS yet"))
    }
    override fun getOutputStream(): Flow<String> = flowOf()
    override suspend fun sendInput(input: String) {}
    override suspend fun downloadFile(remotePath: String, localFile: File): Result<Unit> {
        return Result.failure(NotImplementedError("SFTP not supported on iOS yet"))
    }
}
