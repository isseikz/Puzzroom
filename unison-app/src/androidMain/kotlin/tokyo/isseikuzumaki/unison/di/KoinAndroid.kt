package tokyo.isseikuzumaki.unison.di

import android.content.Context
import org.koin.dsl.module
import tokyo.isseikuzumaki.unison.data.AndroidAudioEngine
import tokyo.isseikuzumaki.unison.data.AndroidAudioRepository
import tokyo.isseikuzumaki.unison.data.AudioEngine
import tokyo.isseikuzumaki.unison.data.AudioRepository

/**
 * Android-specific Koin module
 * Provides Android implementations of audio interfaces
 */
actual fun platformModule() = module {
    // AudioRepository - uses MediaExtractor/MediaCodec for decoding
    single<AudioRepository> {
        AndroidAudioRepository(get<Context>())
    }

    // AudioEngine - uses AudioTrack/AudioRecord for playback and recording
    factory<AudioEngine> {
        AndroidAudioEngine()
    }
}
