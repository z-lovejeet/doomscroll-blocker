import 'dart:async';
import 'package:flutter/material.dart';
import '../channels/native_communicator.dart';
import '../theme/app_colors.dart';

/// Main dashboard showing session timer, penalty status, and service state.
class DashboardScreen extends StatefulWidget {
  final VoidCallback onNavigateToPermissions;
  final VoidCallback onNavigateToFriction;

  const DashboardScreen({
    super.key,
    required this.onNavigateToPermissions,
    required this.onNavigateToFriction,
  });

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen>
    with WidgetsBindingObserver {
  Map<String, dynamic> stats = {};
  bool serviceRunning = false;
  bool overlayPermission = false;
  Timer? _pollTimer;
  StreamSubscription<int>? _timerSub;
  int liveSeconds = 0;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _loadAll();
    _startPolling();
    _listenToTimer();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _loadAll();
    }
  }

  void _startPolling() {
    _pollTimer = Timer.periodic(const Duration(seconds: 5), (_) => _loadAll());
  }

  void _listenToTimer() {
    _timerSub = NativeCommunicator.timerStream.listen((seconds) {
      if (mounted) setState(() => liveSeconds = seconds);
    });
  }

  Future<void> _loadAll() async {
    final s = await NativeCommunicator.getStats();
    final svc = await NativeCommunicator.getServiceStatus();
    final overlay = await NativeCommunicator.getOverlayPermissionStatus();

    if (!mounted) return;
    setState(() {
      stats = s;
      serviceRunning = svc;
      overlayPermission = overlay;
      liveSeconds = (s['sessionSeconds'] as int?) ?? 0;
    });

    // If friction unlock is pending, navigate there
    if (s['pendingFrictionUnlock'] == true) {
      widget.onNavigateToFriction();
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _pollTimer?.cancel();
    _timerSub?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final penaltiesToday = (stats['penaltiesToday'] as int?) ?? 0;
    final isInPenalty = (stats['isInPenalty'] as bool?) ?? false;
    final isDailyLocked = (stats['isDailyLocked'] as bool?) ?? false;
    final penaltyRemainingMs = (stats['penaltyRemainingMs'] as int?) ?? 0;
    final maxSession = (stats['maxSessionSeconds'] as int?) ?? 1800;
    final warningSeconds = (stats['warningSeconds'] as int?) ?? 1500;

    final progress = (liveSeconds / maxSession).clamp(0.0, 1.0);
    final minutesLeft =
        ((maxSession - liveSeconds) ~/ 60).clamp(0, maxSession ~/ 60);
    final secondsLeft = ((maxSession - liveSeconds) % 60).clamp(0, 59);

    final penaltyMins = (penaltyRemainingMs / 60000).floor();
    final penaltySecs = ((penaltyRemainingMs % 60000) / 1000).floor();

    // Determine status
    String statusText;
    Color statusColor;
    if (isDailyLocked) {
      statusText = "LOCKED UNTIL MIDNIGHT";
      statusColor = AppColors.danger;
    } else if (isInPenalty) {
      statusText = "PENALTY BOX";
      statusColor = AppColors.warning;
    } else if (liveSeconds >= warningSeconds) {
      statusText = "WARNING ZONE";
      statusColor = AppColors.warning;
    } else if (serviceRunning) {
      statusText = "MONITORING";
      statusColor = AppColors.safe;
    } else {
      statusText = "SERVICE OFF";
      statusColor = AppColors.textMuted;
    }

    return Scaffold(
      backgroundColor: AppColors.scaffoldBg,
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Header
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      gradient: AppColors.primaryGradient,
                      borderRadius: BorderRadius.circular(14),
                    ),
                    child: const Icon(Icons.shield, color: Colors.white, size: 28),
                  ),
                  const SizedBox(width: 14),
                  const Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        "Doomscroll Blocker",
                        style: TextStyle(
                          color: AppColors.textPrimary,
                          fontSize: 22,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      Text(
                        "Stay in control",
                        style: TextStyle(
                          color: AppColors.textSecondary,
                          fontSize: 13,
                        ),
                      ),
                    ],
                  ),
                  const Spacer(),
                  IconButton(
                    icon: const Icon(Icons.settings, color: AppColors.textSecondary),
                    onPressed: widget.onNavigateToPermissions,
                  ),
                ],
              ),
              const SizedBox(height: 32),

              // Status badge
              Center(
                child: Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
                  decoration: BoxDecoration(
                    color: statusColor.withAlpha(30),
                    borderRadius: BorderRadius.circular(30),
                    border: Border.all(color: statusColor.withAlpha(100)),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Container(
                        width: 8,
                        height: 8,
                        decoration: BoxDecoration(
                          color: statusColor,
                          shape: BoxShape.circle,
                        ),
                      ),
                      const SizedBox(width: 8),
                      Text(
                        statusText,
                        style: TextStyle(
                          color: statusColor,
                          fontSize: 13,
                          fontWeight: FontWeight.w600,
                          letterSpacing: 1.2,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 32),

              // Timer ring
              Center(
                child: SizedBox(
                  width: 220,
                  height: 220,
                  child: Stack(
                    alignment: Alignment.center,
                    children: [
                      SizedBox(
                        width: 220,
                        height: 220,
                        child: CircularProgressIndicator(
                          value: progress,
                          strokeWidth: 12,
                          strokeCap: StrokeCap.round,
                          backgroundColor: AppColors.cardBg,
                          valueColor: AlwaysStoppedAnimation<Color>(
                            liveSeconds >= warningSeconds
                                ? AppColors.danger
                                : AppColors.primary,
                          ),
                        ),
                      ),
                      Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          if (isInPenalty || isDailyLocked) ...[
                            Icon(
                              isDailyLocked ? Icons.lock : Icons.timer_off,
                              color: AppColors.danger,
                              size: 36,
                            ),
                            const SizedBox(height: 8),
                            Text(
                              isDailyLocked
                                  ? "LOCKED"
                                  : "${penaltyMins}m ${penaltySecs}s",
                              style: const TextStyle(
                                color: AppColors.textPrimary,
                                fontSize: 28,
                                fontWeight: FontWeight.w800,
                              ),
                            ),
                            Text(
                              isDailyLocked ? "Until midnight" : "Penalty left",
                              style: const TextStyle(
                                color: AppColors.textSecondary,
                                fontSize: 13,
                              ),
                            ),
                          ] else ...[
                            Text(
                              "${minutesLeft}m ${secondsLeft}s",
                              style: const TextStyle(
                                color: AppColors.textPrimary,
                                fontSize: 36,
                                fontWeight: FontWeight.w800,
                              ),
                            ),
                            const Text(
                              "remaining",
                              style: TextStyle(
                                color: AppColors.textSecondary,
                                fontSize: 14,
                              ),
                            ),
                          ],
                        ],
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 36),

              // Stats cards
              Row(
                children: [
                  Expanded(
                    child: _StatCard(
                      icon: Icons.warning_amber_rounded,
                      label: "Penalties",
                      value: "$penaltiesToday / 3",
                      color: penaltiesToday >= 3
                          ? AppColors.danger
                          : AppColors.warning,
                    ),
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: _StatCard(
                      icon: Icons.access_time_filled,
                      label: "Session",
                      value:
                          "${(liveSeconds ~/ 60)}m ${(liveSeconds % 60).toString().padLeft(2, '0')}s",
                      color: AppColors.primary,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 14),
              Row(
                children: [
                  Expanded(
                    child: _StatCard(
                      icon: Icons.verified_user,
                      label: "Service",
                      value: serviceRunning ? "Active" : "Off",
                      color: serviceRunning ? AppColors.safe : AppColors.danger,
                    ),
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: _StatCard(
                      icon: Icons.layers,
                      label: "Overlay",
                      value: overlayPermission ? "Granted" : "Needed",
                      color: overlayPermission
                          ? AppColors.safe
                          : AppColors.danger,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 28),

              // Permission warning
              if (!serviceRunning || !overlayPermission)
                GestureDetector(
                  onTap: widget.onNavigateToPermissions,
                  child: Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      gradient: AppColors.dangerGradient,
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: const Row(
                      children: [
                        Icon(Icons.error_outline, color: Colors.white, size: 24),
                        SizedBox(width: 12),
                        Expanded(
                          child: Text(
                            "Permissions required. Tap to configure.",
                            style: TextStyle(
                              color: Colors.white,
                              fontSize: 14,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                        Icon(Icons.arrow_forward_ios,
                            color: Colors.white70, size: 16),
                      ],
                    ),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}

class _StatCard extends StatelessWidget {
  final IconData icon;
  final String label;
  final String value;
  final Color color;

  const _StatCard({
    required this.icon,
    required this.label,
    required this.value,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: AppColors.cardBg,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: color.withAlpha(40)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: color, size: 22),
          const SizedBox(height: 10),
          Text(
            value,
            style: TextStyle(
              color: color,
              fontSize: 20,
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            label,
            style: const TextStyle(
              color: AppColors.textSecondary,
              fontSize: 12,
            ),
          ),
        ],
      ),
    );
  }
}
