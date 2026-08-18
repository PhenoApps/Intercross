package org.phenoapps.intercross.util

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import org.phenoapps.brapi.account.BrapiAccountRepository
import org.phenoapps.brapi.account.BrapiPreferenceKeys
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BrapiAccountHelper @Inject constructor(
    @ApplicationContext context: Context,
    preferences: SharedPreferences,
    keyUtil: KeyUtil
) : BrapiAccountRepository(
    context = context,
    preferences = preferences,
    preferenceKeys = BrapiPreferenceKeys(
        enabled = keyUtil.brapiEnabled,
        baseUrl = keyUtil.brapiUrl,
        displayName = keyUtil.brapiDisplayName,
        accessToken = keyUtil.brapiToken,
        idToken = keyUtil.brapiId,
    ),
)