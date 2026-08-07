package com.zeper.player.core.ui.theme

import androidx.compose.ui.graphics.Color

// ── Primary Orange Palette ──────────────────────────────────────
val OrangePrimary = Color(0xFFF1730F)       // Dark Orange — main accent
val OrangeLight = Color(0xFFFFAD33)         // Lighter orange for hover/active
val OrangeDark = Color(0xFFE07800)          // Deeper orange for pressed states
val OrangeContainer = Color(0xFFFFF3E0)     // Very light orange for containers (light mode)
val OrangeContainerDark = Color(0xFF3D2200) // Dark orange container (dark mode)
val OnOrangeContainer = Color(0xFF2D1600)   // Text on light orange container
val OnOrangeContainerDark = Color(0xFFFFDDB3) // Text on dark orange container

// ── Secondary & Tertiary ────────────────────────────────────────
val AmberSecondary = Color(0xFFFFA726)      // Amber secondary accent
val AmberSecondaryDark = Color(0xFFFFB74D)
val TealTertiary = Color(0xFF26A69A)        // Teal for tertiary accents
val TealTertiaryDark = Color(0xFF4DB6AC)

// ── Surface & Background (Dark Mode) ───────────────────────────
val ZeperDarkBg = Color(0xFF0A0A0A)         // Near-black background
val ZeperSurface = Color(0xFF141414)        // Slightly lighter surface
val ZeperSurfaceHigh = Color(0xFF1E1E1E)    // Elevated surface (cards, sheets)
val ZeperSurfaceHighest = Color(0xFF282828) // Highest elevation

// ── Surface & Background (Light Mode) ──────────────────────────
val ZeperLightBg = Color(0xFFFFFBF7)        // Warm white background
val ZeperLightSurface = Color(0xFFFFF8F0)   // Warm surface
val ZeperLightSurfaceHigh = Color(0xFFFFFFFF) // Pure white cards

// ── Text & Icon Colors ─────────────────────────────────────────
val ZeperTextGray = Color(0xFF9E9E9E)       // Subtle gray text
val ZeperTextLight = Color(0xFFBDBDBD)      // Even lighter text
val ZeperDivider = Color(0xFF2A2A2A)        // Dark mode divider
val ZeperDividerLight = Color(0xFFE0E0E0)   // Light mode divider

// ── Semantic Colors ─────────────────────────────────────────────
val GreenBadge = Color(0xFF4CAF50)          // "New" or success badges
val RedBadge = Color(0xFFEF5350)            // Error or delete
val BlueBadge = Color(0xFF42A5F5)           // Info badges

// ── Legacy (kept for compatibility) ─────────────────────────────
val ZeperCyan = Color(0xFF00FFFF)
val ZeperOrange = OrangePrimary
