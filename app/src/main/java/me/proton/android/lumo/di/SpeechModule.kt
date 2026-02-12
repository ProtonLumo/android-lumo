package me.proton.android.lumo.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import me.proton.android.lumo.speech.SpeechRecognitionManager
import javax.inject.Singleton

@Module
@InstallIn(ViewModelComponent::class)
object SpeechModule {

    @Provides
    @ViewModelScoped
    fun speechRecognitionManager(@ApplicationContext context: Context) : SpeechRecognitionManager =
        SpeechRecognitionManager(context)
}
