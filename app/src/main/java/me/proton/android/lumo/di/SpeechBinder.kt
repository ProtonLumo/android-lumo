package me.proton.android.lumo.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import me.proton.android.lumo.speech.SpeechRepository
import me.proton.android.lumo.speech.SpeechRepositoryImpl

@Module
@InstallIn(ViewModelComponent::class)
abstract class SpeechBinder {

    @Binds
    abstract fun speechRepository(speechRepository: SpeechRepositoryImpl): SpeechRepository
}