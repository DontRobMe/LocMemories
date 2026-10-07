package com.ynov.helloworld.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ynov.helloworld.ui.theme.HelloWorldTheme
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

// region Contenu

/**
 * Page de l'introduction.
 *
 * @property icon pictogramme central de l'illustration.
 * @property title titre court de la page.
 * @property text explication en une ou deux phrases.
 */
private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val text: String,
)

private val pages = listOf(
    OnboardingPage(
        icon = Icons.Outlined.EditNote,
        title = "Bienvenue dans votre carnet",
        text = "Notez vos idées, vos découvertes et vos souvenirs, où que vous soyez.",
    ),
    OnboardingPage(
        icon = Icons.Outlined.PhotoCamera,
        title = "Une photo pour chaque souvenir",
        text = "Ajoutez une photo prise sur le moment ou choisie dans votre galerie.",
    ),
    OnboardingPage(
        icon = Icons.Outlined.MyLocation,
        title = "Chaque note a son lieu",
        text = "La position est enregistrée automatiquement. L'autorisation vous sera demandée à la première note.",
    ),
    OnboardingPage(
        icon = Icons.Outlined.Map,
        title = "Retrouvez tout sur la carte",
        text = "Explorez vos notes sur une carte interactive et revivez vos parcours.",
    ),
)

// endregion

// region Écran

/**
 * Introduction présentée au premier lancement, sous forme de carrousel.
 *
 * - balayage horizontal entre les pages, ou boutons « Suivant » / « Commencer » ;
 * - bouton « Passer » pour aller directement à l'application ;
 * - indicateur de progression annoncé par TalkBack (« Page 2 sur 4 ») ;
 * - illustration légèrement animée en fonction du défilement.
 *
 * @param onFinish fin de l'introduction (dernière page validée ou « Passer »).
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .systemBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            TextButton(
                onClick = onFinish,
                modifier = Modifier.alpha(if (isLastPage) 0f else 1f),
                enabled = !isLastPage,
            ) { Text("Passer") }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { index ->
            PageContent(pages[index], offset = { pageOffset(pagerState, index) })
        }

        PageIndicator(
            count = pages.size,
            current = pagerState.currentPage,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 24.dp),
        )

        Button(
            onClick = {
                if (isLastPage) onFinish()
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .height(56.dp),
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        ) {
            AnimatedContent(
                targetState = isLastPage,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "onboarding-button",
            ) { last ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (last) "Commencer" else "Suivant")
                    Icon(
                        if (last) Icons.Outlined.Check else Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = ButtonDefaults.IconSpacing)
                            .size(ButtonDefaults.IconSize),
                    )
                }
            }
        }
    }
}

// endregion

// region Composants privés

/** Distance (en pages) entre la page [index] et la position courante du carrousel. */
private fun pageOffset(state: PagerState, index: Int): Float =
    ((state.currentPage - index) + state.currentPageOffsetFraction).absoluteValue

/**
 * Contenu d'une page : illustration puis textes centrés.
 *
 * @param offset distance à la page courante, utilisée pour réduire et estomper
 *   l'illustration pendant le balayage. Fournie sous forme de lambda et lue uniquement
 *   dans `graphicsLayer` : le balayage ne déclenche que des redessins, aucune recomposition.
 */
@Composable
private fun PageContent(page: OnboardingPage, offset: () -> Float) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Illustration(
            icon = page.icon,
            modifier = Modifier.graphicsLayer {
                val progress = offset().coerceIn(0f, 1f)
                val scale = 1f - 0.25f * progress
                scaleX = scale
                scaleY = scale
                alpha = 1f - 0.6f * progress
            },
        )
        Text(
            page.title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 48.dp)
                .semantics { heading() },
        )
        Text(
            page.text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

/** Illustration décorative : pictogramme dans un disque, entouré de pastilles colorées. */
@Composable
private fun Illustration(icon: ImageVector, modifier: Modifier = Modifier) {
    Box(modifier.size(240.dp), contentAlignment = Alignment.Center) {
        Bubble(MaterialTheme.colorScheme.tertiaryContainer, 56.dp, x = (-92).dp, y = (-72).dp)
        Bubble(MaterialTheme.colorScheme.secondaryContainer, 36.dp, x = 96.dp, y = (-88).dp)
        Bubble(MaterialTheme.colorScheme.secondaryContainer, 28.dp, x = (-84).dp, y = 92.dp)
        Bubble(MaterialTheme.colorScheme.tertiaryContainer, 44.dp, x = 90.dp, y = 76.dp)
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(168.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(80.dp),
                )
            }
        }
    }
}

/** Pastille décorative positionnée relativement au centre de l'illustration. */
@Composable
private fun Bubble(color: Color, size: Dp, x: Dp, y: Dp) {
    Box(
        Modifier
            .offset(x, y)
            .size(size)
            .background(color, CircleShape)
    )
}

/**
 * Indicateur de progression : un point par page, la page courante en pilule allongée.
 *
 * Exposé à TalkBack comme un seul élément « Page n sur N ».
 */
@Composable
private fun PageIndicator(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = "Page ${current + 1} sur $count"
        },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val selected = index == current
            val width by animateDpAsState(if (selected) 28.dp else 8.dp, label = "indicator-width")
            val color by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                label = "indicator-color",
            )
            Box(
                Modifier
                    .height(8.dp)
                    .width(width)
                    .background(color, CircleShape)
            )
        }
    }
}

// endregion

// region Previews

@ThemePreviews
@Composable
private fun OnboardingScreenPreview() {
    HelloWorldTheme {
        OnboardingScreen(onFinish = {})
    }
}

// endregion
