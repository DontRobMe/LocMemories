package com.ynov.helloworld.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

// region Composants partagés

/**
 * Ligne « icône + texte » utilisée pour les métadonnées d'une note (date, lieu…).
 *
 * L'icône est décorative : seul [text] est lu par les lecteurs d'écran.
 *
 * @param icon pictogramme affiché à gauche du texte.
 * @param color teinte appliquée à l'icône et au texte.
 */
@Composable
fun MetaLabel(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

/**
 * Titre de section.
 *
 * Marqué comme « titre » dans la sémantique : TalkBack permet de sauter
 * directement d'une section à l'autre.
 */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.semantics { heading() },
    )
}

// endregion
