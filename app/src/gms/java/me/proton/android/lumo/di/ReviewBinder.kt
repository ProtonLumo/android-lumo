package me.proton.android.lumo.di

import com.google.android.play.core.review.ReviewManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.android.lumo.review.InAppReviewManager
import me.proton.android.lumo.review.InAppReviewManagerImpl
import me.proton.android.lumo.review.ReviewRepository
import me.proton.android.lumo.review.ReviewRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReviewBinder {

    @Binds
    @Singleton
    abstract fun reviewRepository(reviewRepositoryImpl: ReviewRepositoryImpl): ReviewRepository

    @Binds
    @Singleton
    abstract fun reviewManager(reviewManagerImpl: InAppReviewManagerImpl): InAppReviewManager
}