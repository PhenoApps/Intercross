package org.phenoapps.intercross.application

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.phenoapps.brapi.account.BrapiAccountRepository
import org.phenoapps.brapi.account.BrapiPreferenceKeys
import org.phenoapps.intercross.util.KeyUtil

@Module
@InstallIn(SingletonComponent::class)
object ActivityModule {

    @Provides
    fun providesPreferences(@ApplicationContext context: Context): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    @Provides
    @Singleton
    fun providesBrapiPreferenceKeys(@ApplicationContext context: Context): BrapiPreferenceKeys {
        val keyUtil = KeyUtil(context)
        return BrapiPreferenceKeys(
            enabled = keyUtil.brapiEnabled,
            baseUrl = keyUtil.brapiUrl,
            displayName = keyUtil.brapiDisplayName,
            accessToken = keyUtil.brapiToken,
            idToken = keyUtil.brapiId,
            oidcUrl = keyUtil.brapiOidcUrl,
            oidcFlow = keyUtil.brapiOidcFlow,
            oidcClientId = keyUtil.brapiOidcClientId,
            oidcScope = keyUtil.brapiOidcScope,
        )
    }

    @Provides
    @Singleton
    fun providesBrapiAccountRepository(
        @ApplicationContext context: Context,
        prefs: SharedPreferences,
        keys: BrapiPreferenceKeys,
    ): BrapiAccountRepository = BrapiAccountRepository(context, prefs, keys)
}
