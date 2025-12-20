package tokyo.isseikuzumaki.vibeterminal.di

import org.koin.dsl.module
import tokyo.isseikuzumaki.vibeterminal.domain.repository.SshRepository
import tokyo.isseikuzumaki.vibeterminal.ssh.MinaSshdRepository

actual fun platformModule() = module {
    factory<SshRepository> { MinaSshdRepository() }
}
