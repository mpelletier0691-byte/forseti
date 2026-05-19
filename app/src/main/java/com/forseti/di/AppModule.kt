package com.forseti.di

import android.content.Context
import androidx.room.Room
import com.forseti.data.db.ForsetiDatabase
import com.forseti.data.db.MIGRATIONS
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ForsetiDatabase =
        Room.databaseBuilder(context, ForsetiDatabase::class.java, "forseti.db")
            .addMigrations(*MIGRATIONS)
            // Room 2.6.x: no-arg form. The boolean overload (drop-all-tables hint)
            // didn't appear until 2.7, and we don't ship downgrade migrations anyway.
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides fun provideBookmarkDao(db: ForsetiDatabase) = db.bookmarkDao()
    @Provides fun provideNoteDao(db: ForsetiDatabase) = db.noteDao()
    @Provides fun provideDeadlineDao(db: ForsetiDatabase) = db.deadlineDao()
    @Provides fun provideCaseDao(db: ForsetiDatabase) = db.caseDao()

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }
    }
}
