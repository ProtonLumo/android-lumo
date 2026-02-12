package me.proton.android.lumo.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import me.proton.android.lumo.speech.SpeechRepository
import me.proton.android.lumo.speech.SpeechRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(ViewModelComponent::class)
abstract class SpeechBinder {

    @Binds
    @ViewModelScoped
    abstract fun speechRepository(speechRepository: SpeechRepositoryImpl): SpeechRepository
}
