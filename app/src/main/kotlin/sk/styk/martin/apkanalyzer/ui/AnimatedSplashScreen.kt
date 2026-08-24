package sk.styk.martin.apkanalyzer.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.PathInterpolator
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

private const val SPLASH_EXIT_ANIMATION_DURATION_MS = 250L
private val materialFastOutLinearIn = PathInterpolator(0.4f, 0f, 1f, 1f)

internal fun ComponentActivity.installAnimatedSplashScreen(): SplashScreen {
    val splashScreen = installSplashScreen()
    splashScreen.setKeepOnScreenCondition { true }
    splashScreen.setOnExitAnimationListener { splashScreenView ->
        val zoomIconOutX = ObjectAnimator.ofFloat(splashScreenView.iconView, View.SCALE_X, 1f, 1.4f)
        val zoomIconOutY = ObjectAnimator.ofFloat(splashScreenView.iconView, View.SCALE_Y, 1f, 1.4f)
        val fadeIconOut = ObjectAnimator.ofFloat(splashScreenView.iconView, View.ALPHA, 1f, 0f)
        val fadeBackgroundOut = ObjectAnimator.ofFloat(splashScreenView.view, View.ALPHA, 1f, 0f)
        AnimatorSet().apply {
            playTogether(zoomIconOutX, zoomIconOutY, fadeIconOut, fadeBackgroundOut)
            duration = SPLASH_EXIT_ANIMATION_DURATION_MS
            interpolator = materialFastOutLinearIn
            doOnEnd { splashScreenView.remove() }
            start()
        }
    }
    return splashScreen
}

@Composable
internal fun SplashScreen.KeepOnScreenWhile(condition: Boolean) {
    val currentCondition by rememberUpdatedState(condition)
    DisposableEffect(this) {
        setKeepOnScreenCondition { currentCondition }
        onDispose {}
    }
}
