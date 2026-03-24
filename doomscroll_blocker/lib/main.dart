import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'screens/dashboard_screen.dart';
import 'screens/friction_unlock_screen.dart';
import 'screens/permissions_screen.dart';
import 'theme/app_colors.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setSystemUIOverlayStyle(
    const SystemUiOverlayStyle(
      statusBarColor: Colors.transparent,
      statusBarIconBrightness: Brightness.light,
      systemNavigationBarColor: AppColors.scaffoldBg,
      systemNavigationBarIconBrightness: Brightness.light,
    ),
  );
  runApp(const DoomscrollBlockerApp());
}

class DoomscrollBlockerApp extends StatelessWidget {
  const DoomscrollBlockerApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Doomscroll Blocker',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        brightness: Brightness.dark,
        scaffoldBackgroundColor: AppColors.scaffoldBg,
        colorScheme: const ColorScheme.dark(
          primary: AppColors.primary,
          secondary: AppColors.accent,
          surface: AppColors.cardBg,
        ),
        fontFamily: 'Roboto',
        useMaterial3: true,
      ),
      home: const AppShell(),
    );
  }
}

/// Root shell managing navigation between screens.
class AppShell extends StatefulWidget {
  const AppShell({super.key});

  @override
  State<AppShell> createState() => _AppShellState();
}

class _AppShellState extends State<AppShell> {
  String _currentScreen = 'dashboard';

  void _navigate(String screen) {
    setState(() => _currentScreen = screen);
  }

  @override
  Widget build(BuildContext context) {
    switch (_currentScreen) {
      case 'permissions':
        return PermissionsScreen(
          onBack: () => _navigate('dashboard'),
        );
      case 'friction':
        return FrictionUnlockScreen(
          onUnlocked: () => _navigate('dashboard'),
        );
      default:
        return DashboardScreen(
          onNavigateToPermissions: () => _navigate('permissions'),
          onNavigateToFriction: () => _navigate('friction'),
        );
    }
  }
}
