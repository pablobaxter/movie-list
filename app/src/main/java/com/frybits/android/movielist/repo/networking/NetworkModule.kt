package com.frybits.android.movielist.repo.networking

import android.content.Context
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.normalizedCache
import com.apollographql.apollo3.cache.normalized.sql.SqlNormalizedCacheFactory
import com.apollographql.apollo3.network.okHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private const val API_URL = "https://podium-fe-challenge-2021.netlify.app/.netlify/functions/graphql"

@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor {
                val request = it.request().newBuilder()
                    .addHeader("x-api-key", "da2-7dph3xgybbffjn3mik4jii4equ")
                    .build()

                return@addInterceptor it.proceed(request)
            }.build()
    }

    @Provides
    @Singleton
    fun provideApolloClient(okHttpClient: OkHttpClient, normalizedCacheFactory: NormalizedCacheFactory): ApolloClient {
        return ApolloClient.Builder()
            .serverUrl(API_URL)
            .okHttpClient(okHttpClient)
            .normalizedCache(normalizedCacheFactory)
            .build()
    }

    @Provides
    @Singleton
    fun provideMemoryCacheFactory(): MemoryCacheFactory {
        return MemoryCacheFactory(maxSizeBytes = 10 * 1024 * 1024, expireAfterMillis = TimeUnit.SECONDS.toMillis(15))
    }

    @Provides
    @Singleton
    fun provideSqlCacheFactory(@ApplicationContext context: Context): SqlNormalizedCacheFactory {
        return SqlNormalizedCacheFactory(context, "movies.db")
    }

    @Provides
    @Singleton
    fun provideChainedCacheFactory(memoryCacheFactory: MemoryCacheFactory, sqlNormalizedCacheFactory: SqlNormalizedCacheFactory): NormalizedCacheFactory {
        return memoryCacheFactory.chain(sqlNormalizedCacheFactory)
    }
}
