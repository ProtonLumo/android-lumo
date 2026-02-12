package me.proton.android.lumo.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.getunleash.android.DefaultUnleash
import io.getunleash.android.Unleash
import io.getunleash.android.UnleashConfig
import me.proton.android.lumo.BuildConfig
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val POLLING_INTERVAL_MS = 3_00_000L // 5 minutes
    private const val METRICS_INTERVAL_MS = 3_00_000L // 5 minutes

    @Provides
    @Singleton
    fun appPrefers(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences(
            "lumo_prefs",
            Context.MODE_PRIVATE
        )

    @Provides
    @Singleton
    fun unleash(@ApplicationContext context: Context): Unleash {
        return DefaultUnleash(
            androidContext = context,
            unleashConfig = UnleashConfig.newBuilder(appName = "lumo")
                .proxyUrl(BuildConfig.UNLEASH_PROXY)
                .clientKey(BuildConfig.UNLEASH_CLIENT_KEY)
                .httpClient(
                    OkHttpClient.Builder()
                        .addInterceptor { chain ->
                            val original = chain.request()
                            val newReq = original.newBuilder()
                                .header("x-pm-appversion", "android-lumo@1.2.11")
                                .build()
                            chain.proceed(newReq)
                        }
                        .build()
                )
                .pollingStrategy.interval(POLLING_INTERVAL_MS)
                .metricsStrategy.interval(METRICS_INTERVAL_MS)
                .build()
        )
    }
}
