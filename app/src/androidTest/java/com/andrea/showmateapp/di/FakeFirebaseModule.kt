package com.andrea.showmateapp.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.mockk
import javax.inject.Singleton

/**
 * Replaces FirebaseModule in instrumented tests.
 * Provides relaxed MockK fakes so tests that don't exercise Firebase
 * don't need a real Firebase connection or emulator.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [FirebaseModule::class]
)
object FakeFirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = mockk(relaxed = true)

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = mockk(relaxed = true)
}
