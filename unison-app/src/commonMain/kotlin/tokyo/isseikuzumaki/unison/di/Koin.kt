package tokyo.isseikuzumaki.unison.di

import tokyo.isseikuzumaki.unison.screens.library.LibraryViewModel
import tokyo.isseikuzumaki.unison.screens.session.SessionViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/**
 * Koin dependency injection setup for Unison app
 */

// Expected platform-specific implementations
expect fun platformModule(): org.koin.core.module.Module

val viewModelModule = module {
    // LibraryViewModel - Global scope, lightweight
    factoryOf(::LibraryViewModel)

    // SessionViewModel - Scoped to navigation graph, heavy with audio data
    factoryOf(::SessionViewModel)
}

fun initKoin() {
    startKoin {
        modules(
            platformModule(),
            viewModelModule,
        )
    }
}
