package sk.styk.martin.apkanalyzer.core.uilibrary.animation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.navigation3.ui.NavDisplay

internal const val NAV_DURATION_STANDARD = 350
internal const val NAV_DURATION_FADE = 50

fun bottomEntryMetadata(): Map<String, Any> = NavDisplay.transitionSpec {
    slideInVertically(tween(NAV_DURATION_STANDARD, easing = LinearOutSlowInEasing)) { it } +
        fadeIn(tween(NAV_DURATION_STANDARD)) togetherWith
        ExitTransition.KeepUntilTransitionsFinished
} + NavDisplay.popTransitionSpec {
    EnterTransition.None togetherWith
        slideOutVertically(tween(NAV_DURATION_STANDARD, easing = FastOutLinearInEasing)) { it } +
        fadeOut(tween(NAV_DURATION_FADE, delayMillis = NAV_DURATION_STANDARD - NAV_DURATION_FADE))
} + NavDisplay.predictivePopTransitionSpec { _ ->
    EnterTransition.None togetherWith
        slideOutVertically(tween(NAV_DURATION_STANDARD, easing = FastOutLinearInEasing)) { it } +
        fadeOut(tween(NAV_DURATION_FADE, delayMillis = NAV_DURATION_STANDARD - NAV_DURATION_FADE))
}

fun slideFromEndEntryMetadata(): Map<String, Any> = NavDisplay.transitionSpec {
    slideInHorizontally(tween(NAV_DURATION_STANDARD, easing = LinearOutSlowInEasing)) { it } togetherWith
        slideOutHorizontally(tween(NAV_DURATION_STANDARD, easing = FastOutLinearInEasing)) { -it / 3 }
} + NavDisplay.popTransitionSpec {
    slideInHorizontally(tween(NAV_DURATION_STANDARD, easing = LinearOutSlowInEasing)) { -it / 3 } togetherWith
        slideOutHorizontally(tween(NAV_DURATION_STANDARD, easing = FastOutLinearInEasing)) { it }
} + NavDisplay.predictivePopTransitionSpec { _ ->
    slideInHorizontally(tween(NAV_DURATION_STANDARD, easing = LinearOutSlowInEasing)) { -it / 3 } togetherWith
        slideOutHorizontally(tween(NAV_DURATION_STANDARD, easing = FastOutLinearInEasing)) { it }
}
