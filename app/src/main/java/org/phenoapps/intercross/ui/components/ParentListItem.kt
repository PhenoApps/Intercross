package org.phenoapps.intercross.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.phenoapps.intercross.R
import org.phenoapps.intercross.ui.theme.AppTheme

/**
 * A reusable list item for parents, showing name, ID, sex icon, and optionally a checkbox and cross count.
 */
@Composable
fun ParentListItem(
    name: String,
    codeId: String,
    sex: Int,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onToggleSelection: (() -> Unit)? = null,
    isGroup: Boolean = false,
    crossCount: Int? = null,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onToggleSelection != null) Modifier.clickable { onToggleSelection() } else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) AppTheme.colors.primaryTransparent.copy(alpha = 0.1f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 2.dp else 4.dp),
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, AppTheme.colors.primary) else null
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (onToggleSelection != null) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onToggleSelection() },
                    modifier = Modifier.testTag("parent_checkbox_$codeId"),
                    colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = AppTheme.colors.primary)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = name, 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.text.primary
                )
                Text(
                    text = codeId, 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = AppTheme.colors.text.secondary
                )
            }
            if (isGroup) {
                Icon(
                    painter = painterResource(R.drawable.sack_outline),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF2196F3),
                )
            }
            Surface(
                shape = CircleShape,
                color = if (sex == 0) Color(0xFFE91E63) else Color(0xFF2196F3),
            ) {
                Text(
                    text = if (sex == 0) "♀" else "♂",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                )
            }
            if (crossCount != null) {
                Surface(
                    shape = CircleShape,
                    color = AppTheme.colors.lightGray,
                ) {
                    Text(
                        text = crossCount.toString(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = AppTheme.colors.text.primary
                    )
                }
            }
        }
    }
}
