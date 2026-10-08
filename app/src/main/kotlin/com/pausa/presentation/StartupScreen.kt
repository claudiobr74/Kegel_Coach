package com.pausa.presentation

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pausa.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

@Composable internal fun BrandSymbol(modifier:Modifier=Modifier) {
    Image(painterResource(R.drawable.ic_brand_symbol),contentDescription=null,modifier=modifier)
}

@Composable internal fun BrandName(modifier:Modifier=Modifier,compact:Boolean=false) {
    Text(buildAnnotatedString {
        withStyle(SpanStyle(color=MaterialTheme.colorScheme.onBackground)) {append("Kegel ")}
        withStyle(SpanStyle(color=MaterialTheme.colorScheme.primary)) {append("Coach")}
    },modifier=modifier,style=if(compact)MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineLarge,
        fontWeight=FontWeight.SemiBold)
}

/** One native animation per activity launch; no replay on navigation or rotation. */
@Composable internal fun StartupScreen(ready:Boolean,onFinished:()->Unit) {
    val finished by rememberUpdatedState(onFinished)
    val isReady by rememberUpdatedState(ready)
    val opacity=remember {Animatable(0f)}
    val nameOpacity=remember {Animatable(0f)}
    val scale=remember {Animatable(.96f)}
    val motionEnabled=remember {ValueAnimator.areAnimatorsEnabled()}
    LaunchedEffect(Unit) {
        if(motionEnabled) {
            coroutineScope {
                launch {opacity.animateTo(1f,tween(120))}
                scale.animateTo(1f,tween(400, easing=FastOutSlowInEasing))
            }
            coroutineScope {
                launch {nameOpacity.animateTo(1f,tween(120))}
                scale.animateTo(.97f,tween(100, easing=FastOutSlowInEasing))
                scale.animateTo(1f,tween(100, easing=FastOutSlowInEasing))
            }
        } else {
            opacity.snapTo(1f);nameOpacity.snapTo(1f);scale.snapTo(1f)
        }
        snapshotFlow {isReady}.first {it}
        if(motionEnabled) opacity.animateTo(0f,tween(120))
        finished()
    }
    Box(Modifier.fillMaxSize().safeDrawingPadding().clearAndSetSemantics {
        contentDescription="Kegel Coach"
    },contentAlignment=Alignment.Center) {
        Column(Modifier.padding(32.dp).graphicsLayer {alpha=opacity.value},
            horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.spacedBy(28.dp)) {
            BrandSymbol(Modifier.size(156.dp).graphicsLayer {
                scaleX=scale.value;scaleY=scale.value
            })
            BrandName(Modifier.graphicsLayer {alpha=nameOpacity.value})
        }
    }
}
