import 'package:flutter/material.dart';
import '../models/prescription_response.dart';
import '../utils/constants.dart';

/// Widget to display a single medicine
class MedicineCard extends StatelessWidget {
  final MedicineResult medicine;
  final int index;

  const MedicineCard({
    Key? key,
    required this.medicine,
    required this.index,
  }) : super(key: key);

  /// Get confidence badge color based on confidence level
  Color _getConfidenceColor() {
    final confidence = medicine.normalizationConfidence ?? 0;
    if (confidence >= 0.8) return AppColors.success;
    if (confidence >= 0.6) return AppColors.warning;
    return AppColors.error;
  }

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 1,
      margin: const EdgeInsets.symmetric(
        horizontal: AppSpacing.md,
        vertical: AppSpacing.sm,
      ),
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.md),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Header with medicine name and confidence
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  width: 36,
                  height: 36,
                  decoration: BoxDecoration(
                    color: AppColors.brandBg,
                    borderRadius: BorderRadius.circular(AppRadius.md),
                  ),
                  child: Center(
                    child: Text(
                      '${index + 1}',
                      style: AppTextStyles.labelLarge,
                    ),
                  ),
                ),
                const SizedBox(width: AppSpacing.md),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        medicine.medicineName ?? 'Unknown',
                        style: AppTextStyles.bodyLarge,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                      ),
                      if (medicine.normalizedName != null &&
                          medicine.normalizedName !=
                              medicine.medicineName)
                        Padding(
                          padding: const EdgeInsets.only(top: AppSpacing.xs),
                          child: Text(
                            'Normalized: ${medicine.normalizedName}',
                            style: AppTextStyles.bodySmall,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                    ],
                  ),
                ),
                if (medicine.normalizationConfidence != null)
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: AppSpacing.sm,
                      vertical: AppSpacing.xs,
                    ),
                    decoration: BoxDecoration(
                      color: _getConfidenceColor().withOpacity(0.1),
                      borderRadius: BorderRadius.circular(AppRadius.sm),
                    ),
                    child: Text(
                      '${(medicine.normalizationConfidence! * 100).toStringAsFixed(0)}%',
                      style: TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.w600,
                        color: _getConfidenceColor(),
                      ),
                    ),
                  ),
              ],
            ),
            const SizedBox(height: AppSpacing.md),

            // Medicine details grid
            Wrap(
              spacing: AppSpacing.md,
              runSpacing: AppSpacing.md,
              children: [
                if (medicine.dosage != null && medicine.dosage!.isNotEmpty)
                  _MedicineDetailChip(
                    label: 'Dosage',
                    value: medicine.dosage!,
                  ),
                if (medicine.frequency != null && medicine.frequency!.isNotEmpty)
                  _MedicineDetailChip(
                    label: 'Frequency',
                    value: medicine.frequency!,
                  ),
                if (medicine.duration != null && medicine.duration!.isNotEmpty)
                  _MedicineDetailChip(
                    label: 'Duration',
                    value: medicine.duration!,
                  ),
                if (medicine.quantity != null && medicine.quantity!.isNotEmpty)
                  _MedicineDetailChip(
                    label: 'Quantity',
                    value: medicine.quantity!,
                  ),
              ],
            ),

            // RxCUI Badge (if available)
            if (medicine.rxcui != null && medicine.rxcui!.isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(top: AppSpacing.md),
                child: Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: AppSpacing.sm,
                    vertical: AppSpacing.xs,
                  ),
                  decoration: BoxDecoration(
                    color: AppColors.brandBg,
                    borderRadius: BorderRadius.circular(AppRadius.sm),
                  ),
                  child: Text(
                    'RxCUI: ${medicine.rxcui}',
                    style: AppTextStyles.labelSmall,
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

/// Small detail chip for medicine information
class _MedicineDetailChip extends StatelessWidget {
  final String label;
  final String value;

  const _MedicineDetailChip({
    required this.label,
    required this.value,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.sm,
        vertical: AppSpacing.xs,
      ),
      decoration: BoxDecoration(
        color: AppColors.surfaceVariant,
        borderRadius: BorderRadius.circular(AppRadius.sm),
      ),
      child: RichText(
        text: TextSpan(
          children: [
            TextSpan(
              text: '$label: ',
              style: AppTextStyles.labelSmall.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
            TextSpan(
              text: value,
              style: AppTextStyles.labelSmall,
            ),
          ],
        ),
      ),
    );
  }
}

/// Widget to display drug-drug interactions
class DrugInteractionCard extends StatelessWidget {
  final DrugInteractionResult interaction;

  const DrugInteractionCard({
    Key? key,
    required this.interaction,
  }) : super(key: key);

  Color _getSeverityColor() {
    switch (interaction.severity.toUpperCase()) {
      case 'HIGH':
        return AppColors.severityHigh;
      case 'MODERATE':
        return AppColors.severityModerate;
      case 'LOW':
        return AppColors.severityLow;
      default:
        return AppColors.textSecondary;
    }
  }

  @override
  Widget build(BuildContext context) {
    final severityColor = _getSeverityColor();

    return Card(
      elevation: 1,
      margin: const EdgeInsets.symmetric(
        horizontal: AppSpacing.md,
        vertical: AppSpacing.sm,
      ),
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.md),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Severity badge and drugs
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: AppSpacing.sm,
                    vertical: AppSpacing.xs,
                  ),
                  decoration: BoxDecoration(
                    color: severityColor.withOpacity(0.1),
                    border: Border.all(color: severityColor, width: 1),
                    borderRadius: BorderRadius.circular(AppRadius.sm),
                  ),
                  child: Text(
                    interaction.severity.toUpperCase(),
                    style: TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.w700,
                      color: severityColor,
                    ),
                  ),
                ),
                const SizedBox(width: AppSpacing.md),
                Expanded(
                  child: Text(
                    '${interaction.drug1Name} ↔ ${interaction.drug2Name}',
                    style: AppTextStyles.bodyMedium.copyWith(
                      fontWeight: FontWeight.w600,
                    ),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ],
            ),

            // Description
            if (interaction.description != null &&
                interaction.description!.isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(top: AppSpacing.md),
                child: Text(
                  interaction.description!,
                  style: AppTextStyles.bodySmall,
                  maxLines: 3,
                  overflow: TextOverflow.ellipsis,
                ),
              ),

            // Source
            if (interaction.source != null)
              Padding(
                padding: const EdgeInsets.only(top: AppSpacing.sm),
                child: Text(
                  'Source: ${interaction.source}',
                  style: AppTextStyles.labelSmall,
                ),
              ),
          ],
        ),
      ),
    );
  }
}

/// Widget to display duplicate ingredient warnings
class DuplicateWarningCard extends StatelessWidget {
  final DuplicateIngredientWarning warning;

  const DuplicateWarningCard({
    Key? key,
    required this.warning,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Card(
      color: AppColors.warning.withOpacity(0.1),
      elevation: 1,
      margin: const EdgeInsets.symmetric(
        horizontal: AppSpacing.md,
        vertical: AppSpacing.sm,
      ),
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.md),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(
                  Icons.warning_rounded,
                  color: AppColors.warning,
                  size: 24,
                ),
                const SizedBox(width: AppSpacing.md),
                Expanded(
                  child: Text(
                    'Duplicate Active Ingredient',
                    style: AppTextStyles.bodyMedium.copyWith(
                      fontWeight: FontWeight.w600,
                      color: AppColors.warning,
                    ),
                  ),
                ),
              ],
            ),
            Padding(
              padding: const EdgeInsets.only(top: AppSpacing.md),
              child: Text(
                warning.message,
                style: AppTextStyles.bodySmall,
              ),
            ),
            Padding(
              padding: const EdgeInsets.only(top: AppSpacing.sm),
              child: Text(
                'Medicines: ${warning.medicines.join(', ')}',
                style: AppTextStyles.bodySmall.copyWith(
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// Loading indicator widget
class PrescriptionLoadingIndicator extends StatelessWidget {
  final double progress;

  const PrescriptionLoadingIndicator({
    Key? key,
    required this.progress,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        CircularProgressIndicator(
          value: progress > 0 ? progress : null,
          backgroundColor: AppColors.outlineVariant,
          valueColor: AlwaysStoppedAnimation<Color>(AppColors.primary),
        ),
        const SizedBox(height: AppSpacing.lg),
        Text(
          'Processing Prescription...',
          style: AppTextStyles.bodyLarge.copyWith(
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: AppSpacing.sm),
        Text(
          '${(progress * 100).toStringAsFixed(0)}%',
          style: AppTextStyles.bodySmall,
        ),
      ],
    );
  }
}
