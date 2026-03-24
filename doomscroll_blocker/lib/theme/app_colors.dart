import 'package:flutter/material.dart';

/// Dark, moody color palette for the doomscroll blocker app.
class AppColors {
  AppColors._();

  // Backgrounds
  static const Color scaffoldBg = Color(0xFF0D0D0D);
  static const Color cardBg = Color(0xFF1A1A2E);
  static const Color cardBgLight = Color(0xFF16213E);

  // Accents
  static const Color primary = Color(0xFF7B2FF7);
  static const Color primaryLight = Color(0xFFA855F7);
  static const Color accent = Color(0xFF00D4AA);
  static const Color warning = Color(0xFFFF6B35);
  static const Color danger = Color(0xFFEF4444);
  static const Color safe = Color(0xFF22C55E);

  // Text
  static const Color textPrimary = Color(0xFFF5F5F5);
  static const Color textSecondary = Color(0xFF9CA3AF);
  static const Color textMuted = Color(0xFF6B7280);

  // Gradients
  static const LinearGradient primaryGradient = LinearGradient(
    colors: [Color(0xFF7B2FF7), Color(0xFF00D4AA)],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );

  static const LinearGradient dangerGradient = LinearGradient(
    colors: [Color(0xFFEF4444), Color(0xFFFF6B35)],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );

  static const LinearGradient cardGradient = LinearGradient(
    colors: [Color(0xFF1A1A2E), Color(0xFF16213E)],
    begin: Alignment.topCenter,
    end: Alignment.bottomCenter,
  );
}
