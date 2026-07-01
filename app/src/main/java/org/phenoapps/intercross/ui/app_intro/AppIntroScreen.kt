package org.phenoapps.intercross.ui.app_intro

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.phenoapps.intercross.R
import org.phenoapps.intercross.ui.theme.IntercrossPreviewTheme

@Composable
internal fun IntroSlideContent(
    title: String,
    summary: String,
    iconRes: Int?,
    backgroundColor: Color = Color(0xFF1B5E20), // colorPrimaryDark
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor,
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 40.dp)
            )

            Spacer(Modifier.height(32.dp))

            iconRes?.let {
                Image(
                    painter = painterResource(it),
                    contentDescription = null,
                    modifier = Modifier.size(160.dp)
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = summary,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
internal fun RequiredSetupSlideContent(
    title: String,
    summary: String,
    permissionsGranted: Boolean,
    storageSet: Boolean,
    onPermissionsClick: () -> Unit = {},
    onStorageClick: () -> Unit = {},
    backgroundColor: Color = Color(0xFF1B5E20),
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor,
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 40.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(Modifier.height(40.dp))

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SetupActionRow(
                    title = stringResource(R.string.app_intro_permissions_title),
                    summary = stringResource(R.string.app_intro_permissions_summary),
                    iconRes = R.drawable.ic_configure_white,
                    statusRes = if (permissionsGranted) R.drawable.ic_check_white else R.drawable.ic_chevron_right_white,
                    onClick = onPermissionsClick
                )

                SetupActionRow(
                    title = stringResource(R.string.app_intro_storage_title),
                    summary = stringResource(R.string.app_intro_storage_summary),
                    iconRes = R.drawable.ic_storage_white,
                    statusRes = if (storageSet) R.drawable.ic_check_white else R.drawable.ic_chevron_right_white,
                    onClick = onStorageClick
                )
            }
        }
    }
}

@Composable
internal fun OptionalSetupSlideContent(
    title: String,
    summary: String,
    loadParents: Boolean,
    loadWishlist: Boolean,
    onParentsToggle: (Boolean) -> Unit = {},
    onWishlistToggle: (Boolean) -> Unit = {},
    backgroundColor: Color = Color(0xFF1B5E20),
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor,
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 40.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(Modifier.height(40.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_intro_load_sample_data_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OptionalSetupRow(
                    title = stringResource(R.string.app_intro_load_sample_parents_title),
                    summary = stringResource(R.string.app_intro_load_sample_parents_summary),
                    checked = loadParents,
                    onCheckedChange = onParentsToggle
                )

                OptionalSetupRow(
                    title = stringResource(R.string.app_intro_load_sample_wishlist_title),
                    summary = stringResource(R.string.app_intro_load_sample_wishlist_summary),
                    checked = loadWishlist,
                    onCheckedChange = onWishlistToggle
                )
            }
        }
    }
}

@Composable
private fun SetupActionRow(
    title: String,
    summary: String,
    iconRes: Int,
    statusRes: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(30.dp),
            tint = Color.White
        )
        
        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )
        }

        Spacer(Modifier.width(16.dp))

        Icon(
            painter = painterResource(statusRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = Color.White
        )
    }
}

@Composable
private fun OptionalSetupRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )
        }

        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Color.White,
                uncheckedColor = Color.White,
                checkmarkColor = Color(0xFF1B5E20)
            )
        )
    }
}

// ─── Previews ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "AppIntro - Welcome")
@Composable
internal fun IntroWelcomePreview() {
    IntercrossPreviewTheme {
        IntroSlideContent(
            title = "Welcome to Intercross!",
            summary = "Intercross is used to track crosses in plant breeding and genetics research programs. The app tracks parents, cross IDs, and metadata as crosses are created.",
            iconRes = null
        )
    }
}

@Preview(showBackground = true, name = "AppIntro - Required Setup")
@Composable
internal fun IntroRequiredSetupPreview() {
    IntercrossPreviewTheme {
        RequiredSetupSlideContent(
            title = "Required Setup",
            summary = "To get started using Intercross, grant the necessary permissions for the app and then define a folder where your data will be stored.",
            permissionsGranted = false,
            storageSet = false
        )
    }
}

@Preview(showBackground = true, name = "AppIntro - Optional Setup")
@Composable
internal fun IntroOptionalSetupPreview() {
    IntercrossPreviewTheme {
        OptionalSetupSlideContent(
            title = "Optional Setup",
            summary = "Optional settings customize how Intercross is set up for data collection.",
            loadParents = true,
            loadWishlist = false
        )
    }
}
