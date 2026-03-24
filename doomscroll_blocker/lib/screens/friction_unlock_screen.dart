import 'package:flutter/material.dart';
import '../channels/native_communicator.dart';
import '../theme/app_colors.dart';

/// "Walk of Shame" screen. Forces the user to type a deliberate sentence
/// to unlock their next Reels session after a penalty cooldown expires.
class FrictionUnlockScreen extends StatefulWidget {
  final VoidCallback onUnlocked;

  const FrictionUnlockScreen({super.key, required this.onUnlocked});

  @override
  State<FrictionUnlockScreen> createState() => _FrictionUnlockScreenState();
}

class _FrictionUnlockScreenState extends State<FrictionUnlockScreen> {
  final TextEditingController _controller = TextEditingController();
  bool _isLoading = false;
  bool _showError = false;

  static const String requiredText =
      "I am opening Reels intentionally and I will close it when my time is up";

  Future<void> _attemptUnlock() async {
    setState(() {
      _isLoading = true;
      _showError = false;
    });

    final success = await NativeCommunicator.unlockPenalty(_controller.text);

    if (!mounted) return;
    if (success) {
      widget.onUnlocked();
    } else {
      setState(() {
        _isLoading = false;
        _showError = true;
      });
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.scaffoldBg,
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const SizedBox(height: 40),

              // Icon
              Center(
                child: Container(
                  padding: const EdgeInsets.all(20),
                  decoration: BoxDecoration(
                    color: AppColors.warning.withAlpha(20),
                    shape: BoxShape.circle,
                    border: Border.all(
                      color: AppColors.warning.withAlpha(60),
                      width: 2,
                    ),
                  ),
                  child: const Icon(
                    Icons.edit_note_rounded,
                    color: AppColors.warning,
                    size: 48,
                  ),
                ),
              ),
              const SizedBox(height: 28),

              // Title
              const Center(
                child: Text(
                  "Walk of Shame",
                  style: TextStyle(
                    color: AppColors.textPrimary,
                    fontSize: 28,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ),
              const SizedBox(height: 8),
              const Center(
                child: Text(
                  "Your penalty has expired. To unlock your next\nsession, type the following sentence exactly:",
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    color: AppColors.textSecondary,
                    fontSize: 14,
                    height: 1.5,
                  ),
                ),
              ),
              const SizedBox(height: 28),

              // Required text display
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: AppColors.cardBg,
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: AppColors.primary.withAlpha(60)),
                ),
                child: const Text(
                  '"$requiredText"',
                  style: TextStyle(
                    color: AppColors.primaryLight,
                    fontSize: 16,
                    fontWeight: FontWeight.w500,
                    fontStyle: FontStyle.italic,
                    height: 1.6,
                  ),
                ),
              ),
              const SizedBox(height: 24),

              // Input field
              TextField(
                controller: _controller,
                maxLines: 3,
                style: const TextStyle(
                  color: AppColors.textPrimary,
                  fontSize: 15,
                ),
                decoration: InputDecoration(
                  hintText: "Type the sentence here...",
                  hintStyle: const TextStyle(color: AppColors.textMuted),
                  filled: true,
                  fillColor: AppColors.cardBg,
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(16),
                    borderSide: BorderSide.none,
                  ),
                  enabledBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(16),
                    borderSide: BorderSide(
                      color: _showError
                          ? AppColors.danger.withAlpha(120)
                          : AppColors.cardBg,
                    ),
                  ),
                  focusedBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(16),
                    borderSide: const BorderSide(color: AppColors.primary),
                  ),
                  contentPadding: const EdgeInsets.all(18),
                ),
              ),

              if (_showError) ...[
                const SizedBox(height: 12),
                const Text(
                  "Text doesn't match. Type it exactly as shown above.",
                  style: TextStyle(
                    color: AppColors.danger,
                    fontSize: 13,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ],

              const SizedBox(height: 28),

              // Submit button
              SizedBox(
                width: double.infinity,
                height: 56,
                child: ElevatedButton(
                  onPressed: _isLoading ? null : _attemptUnlock,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.primary,
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                    ),
                    elevation: 0,
                  ),
                  child: _isLoading
                      ? const SizedBox(
                          width: 24,
                          height: 24,
                          child: CircularProgressIndicator(
                            color: Colors.white,
                            strokeWidth: 2.5,
                          ),
                        )
                      : const Text(
                          "Unlock Session",
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.w700,
                          ),
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
