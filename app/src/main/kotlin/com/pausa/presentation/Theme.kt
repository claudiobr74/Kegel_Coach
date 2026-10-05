package com.pausa.presentation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.sp
import com.pausa.R
import com.pausa.domain.AppTheme

private val light=lightColorScheme(primary=Color(0xFF165E59),onPrimary=Color.White,
    primaryContainer=Color(0xFFD7EEEA),onPrimaryContainer=Color(0xFF165E59),
    secondary=Color(0xFF536B5E),secondaryContainer=Color(0xFFE5EFEB),onSecondaryContainer=Color(0xFF165E59),
    surfaceTint=Color(0xFF165E59),outlineVariant=Color(0xFFC7D6D1),background=Color(0xFFF5F8F6),surface=Color.White,
    surfaceVariant=Color(0xFFE5EFEB),onSurface=Color(0xFF172C2D),onBackground=Color(0xFF172C2D),
    onSurfaceVariant=Color(0xFF526667),outline=Color(0xFFC7D6D1),error=Color(0xFFA43D3D))
private val dark=darkColorScheme(primary=Color(0xFF93D4C9),onPrimary=Color(0xFF093C38),
    primaryContainer=Color(0xFF234B4B),onPrimaryContainer=Color(0xFF93D4C9),
    secondary=Color(0xFFB6CCBD),secondaryContainer=Color(0xFF293C41),onSecondaryContainer=Color(0xFF93D4C9),
    surfaceTint=Color(0xFF93D4C9),outlineVariant=Color(0xFF435B60),background=Color(0xFF111E24),surface=Color(0xFF1B2B31),
    surfaceVariant=Color(0xFF293C41),onSurface=Color(0xFFE2EEEB),onBackground=Color(0xFFE2EEEB),
    onSurfaceVariant=Color(0xFFABC0BF),outline=Color(0xFF435B60),error=Color(0xFFFFB2AC))
@OptIn(ExperimentalTextApi::class)
private val inter=FontFamily(
    Font(R.font.inter,FontWeight.Normal,variationSettings=FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.inter,FontWeight.Medium,variationSettings=FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.inter,FontWeight.SemiBold,variationSettings=FontVariation.Settings(FontVariation.weight(600)))
)
private val typography=Typography(
    headlineLarge=TextStyle(fontFamily=inter,fontWeight=FontWeight.SemiBold,fontSize=28.sp,lineHeight=39.sp),
    headlineMedium=TextStyle(fontFamily=inter,fontWeight=FontWeight.SemiBold,fontSize=24.sp,lineHeight=34.sp),
    titleLarge=TextStyle(fontFamily=inter,fontWeight=FontWeight.SemiBold,fontSize=20.sp,lineHeight=28.sp),
    titleMedium=TextStyle(fontFamily=inter,fontWeight=FontWeight.SemiBold,fontSize=16.sp,lineHeight=22.sp),
    bodyLarge=TextStyle(fontFamily=inter,fontWeight=FontWeight.Normal,fontSize=16.sp,lineHeight=22.sp),
    bodyMedium=TextStyle(fontFamily=inter,fontWeight=FontWeight.Normal,fontSize=14.sp,lineHeight=20.sp),
    bodySmall=TextStyle(fontFamily=inter,fontWeight=FontWeight.Normal,fontSize=12.sp,lineHeight=17.sp),
    labelLarge=TextStyle(fontFamily=inter,fontWeight=FontWeight.SemiBold,fontSize=14.sp),
    labelSmall=TextStyle(fontFamily=inter,fontWeight=FontWeight.Medium,fontSize=11.sp)
)
@Composable fun PausaTheme(theme:AppTheme,content:@Composable ()->Unit) {
    MaterialTheme(colorScheme=if(theme==AppTheme.DARK || (theme==AppTheme.SYSTEM && isSystemInDarkTheme()))dark else light,
        typography=typography,content=content)
}
