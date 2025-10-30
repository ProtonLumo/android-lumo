package me.proton.android.lumo.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColors(
    val primary: Color,

    val interactionNorm: Color,
    val interactionNormHover: Color,
    val interactionNormActive: Color,

    val interactionSecondary: Color,
    val interactionSecondaryHover: Color,
    val interactionSecondaryActive: Color,

    val reinforceNorm: Color,
    val reinforceNormHover: Color,
    val reinforceNormActive: Color,

    val linkNorm: Color,
    val linkHover: Color,
    val linkActive: Color,

    val textNorm: Color,
    val textWeak: Color,
    val textHint: Color,
    val textDisabled: Color,
    val textInvert: Color,

    val fieldNorm: Color,
    val fieldHover: Color,
    val fieldDisabled: Color,

    val backgroundNorm: Color,
    val backgroundWeak: Color,
    val backgroundStrong: Color,
    val backgroundInvert: Color,

    val interactionWeak: Color,
    val interactionWeakHover: Color,
    val interactionWeakActive: Color,

    val signalDanger: Color,
    val signalDangerHover: Color,
    val signalDangerActive: Color,

    val signalWarning: Color,
    val signalWarningHover: Color,
    val signalWarningActive: Color,

    val signalSuccess: Color,
    val signalSuccessHover: Color,
    val signalSuccessActive: Color,

    val signalInfo: Color,
    val signalInfoHover: Color,
    val signalInfoActive: Color,

    val borderNorm: Color,
    val borderWeak: Color,
    val focus: Color,
)

val LightColors = AppColors(
    primary = Color(0xFF6D4AFF),
    interactionNorm = Color(0xFF6D4AFF),
    interactionNormHover = Color(0xFF6243E6),
    interactionNormActive = Color(0xFF573BCC),
    interactionSecondary = Color(0xFFFFAC2F),
    interactionSecondaryHover = Color(0xFFE69A2A),
    interactionSecondaryActive = Color(0xFFD48F26),
    reinforceNorm = Color(0xFF372580),
    reinforceNormHover = Color(0xFF2F1E6F),
    reinforceNormActive = Color(0xFF27185F),
    linkNorm = Color(0xFF6D4AFF),
    linkHover = Color(0xFF6243E6),
    linkActive = Color(0xFF573BCC),
    textNorm = Color(0xFF0C0C14),
    textWeak = Color(0xFF5C5958),
    textHint = Color(0xFF8F8D8A),
    textDisabled = Color(0xFF9E9B98),
    textInvert = Color(0xFFFFFFFF),
    fieldNorm = Color(0xFFADABA8),
    fieldHover = Color(0xFF8F8D8A),
    fieldDisabled = Color(0xFFD1CFCD),
    backgroundNorm = Color(0xFFFFFFFF),
    backgroundWeak = Color(0xFFF8FAFC),
    backgroundStrong = Color(0xFFE5E8EB),
    backgroundInvert = Color(0xFF1A1A1A),
    interactionWeak = Color(0xFFEAE7E4),
    interactionWeakHover = Color(0xFFDEDBD9),
    interactionWeakActive = Color(0xFFD3D0CD),
    signalDanger = Color(0xFFDC3251),
    signalDangerHover = Color(0xFFC62D49),
    signalDangerActive = Color(0xFFB02841),
    signalWarning = Color(0xFFFF9900),
    signalWarningHover = Color(0xFFF27D00),
    signalWarningActive = Color(0xFFE66300),
    signalSuccess = Color(0xFF1EA885),
    signalSuccessHover = Color(0xFF1B9778),
    signalSuccessActive = Color(0xFF18866A),
    signalInfo = Color(0xFF239ECE),
    signalInfoHover = Color(0xFF208EB9),
    signalInfoActive = Color(0xFF1C7EA5),
    borderNorm = Color(0xFFD1CFCD),
    borderWeak = Color(0xFFEAE7E4),
    focus = Color(0xFF6D4AFF),
)

val DarkColors = AppColors(
    primary = Color(0xFF8A6EFF),
    interactionNorm = Color(0xFF6D4AFF),
    interactionNormHover = Color(0xFF7C5CFF),
    interactionNormActive = Color(0xFF8A6EFF),
    interactionSecondary = Color(0xFFFFAC2F),
    interactionSecondaryHover = Color(0xFFE69A2A),
    interactionSecondaryActive = Color(0xFFD48F26),
    reinforceNorm = Color(0xFF372580),
    reinforceNormHover = Color(0xFF2F1E6F),
    reinforceNormActive = Color(0xFF27185F),
    linkNorm = Color(0xFF9880FF),
    linkHover = Color(0xFFA898ED),
    linkActive = Color(0xFFBBABFF),
    textNorm = Color(0xFFFFFFFF),
    textWeak = Color(0xFFA7A4B5),
    textHint = Color(0xFF6D697D),
    textDisabled = Color(0xFF5B576B),
    textInvert = Color(0xFF1C1B24),
    fieldNorm = Color(0xFF5B576B),
    fieldHover = Color(0xFF6D697D),
    fieldDisabled = Color(0xFF3F3B4C),
    backgroundNorm = Color(0xFF16141C),
    backgroundWeak = Color(0xFF292733),
    backgroundStrong = Color(0xFF3F3B4C),
    backgroundInvert = Color(0xFFFFFFFF),
    interactionWeak = Color(0xFF4A4658),
    interactionWeakHover = Color(0xFF5C5969),
    interactionWeakActive = Color(0xFF6E6B79),
    signalDanger = Color(0xFFF5385A),
    signalDangerHover = Color(0xFFF64C6B),
    signalDangerActive = Color(0xFFF7607B),
    signalWarning = Color(0xFFFF9900),
    signalWarningHover = Color(0xFFFFA31A),
    signalWarningActive = Color(0xFFFFAD33),
    signalSuccess = Color(0xFF1EA885),
    signalSuccessHover = Color(0xFF35B191),
    signalSuccessActive = Color(0xFF4BB99D),
    signalInfo = Color(0xFF239ECE),
    signalInfoHover = Color(0xFF39A8D3),
    signalInfoActive = Color(0xFF4FB1D8),
    borderNorm = Color(0xFF4A4658),
    borderWeak = Color(0xFF343140),
    focus = Color(0xFF6D4AFF),
)

@Composable
fun AppColors.planSelectionBackground(isDarkTheme: Boolean): Color =
    if (isDarkTheme) backgroundWeak else backgroundNorm