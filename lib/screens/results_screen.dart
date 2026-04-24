import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/prescription_response.dart';
import '../providers/prescription_provider.dart';
import '../utils/constants.dart';
import '../widgets/prescription_widgets.dart';

/// Results screen displaying prescription analysis
class ResultsScreen extends ConsumerWidget {
  const ResultsScreen({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final prescriptionState = ref.watch(prescriptionProvider);
    final result = prescriptionState.result?.result;

    if (result == null) {
      return Scaffold(
        appBar: AppBar(
          backgroundColor: AppColors.primary,
          title: const Text(AppStrings.resultsTitle),
        ),
        body: const Center(
          child: Text('No results available'),
        ),
      );
    }

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.primary,
        elevation: 0,
        title: const Text(AppStrings.resultsTitle),
        centerTitle: true,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () {
              ref.read(prescriptionProvider.notifier).reset();
              Navigator.pop(context);
            },
          ),
        ],
      ),
      body: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Summary Card
            _SummaryCard(result: result),

            // Medicines Section
            _MedicinesSection(medicines: result.prescription),

            // Warnings Section (if any)
            if (result.drugInteractions.isNotEmpty ||
                result.drugDiseaseWarnings.isNotEmpty ||
                result.duplicateIngredientWarnings.isNotEmpty)
              _WarningsSection(result: result),

            // Processing Info
            _ProcessingInfoCard(result: result),

            const SizedBox(height: AppSpacing.lg),
          ],
        ),
      ),
    );
  }
}

/// Summary card showing overview
class _SummaryCard extends StatelessWidget {
  final PrescriptionResult result;

  const _SummaryCard({required this.result});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.all(AppSpacing.md),
      padding: const EdgeInsets.all(AppSpacing.lg),
      decoration: BoxDecoration(
        color: AppColors.brandBg,
        borderRadius: BorderRadius.circular(AppRadius.lg),
        border: Border.all(
          color: AppColors.primary.withOpacity(0.2),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Prescription Summary',
            style: AppTextStyles.headline4,
          ),
          const SizedBox(height: AppSpacing.lg),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [
              _StatBox(
                label: 'Medicines',
                value: result.prescription.length.toString(),
                color: AppColors.primary,
              ),
              _StatBox(
                label: 'Interactions',
                value: result.drugInteractions.length.toString(),
                color: AppColors.severityHigh,
              ),
              _StatBox(
                label: 'High Severity',
                value: result.highSeverityCount.toString(),
                color: AppColors.error,
              ),
            ],
          ),
        ],
      ),
    );
  }
}

/// Single stat box
class _StatBox extends StatelessWidget {
  final String label;
  final String value;
  final Color color;

  const _StatBox({
    required this.label,
    required this.value,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Text(
          value,
          style: TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.bold,
            color: color,
          ),
        ),
        const SizedBox(height: AppSpacing.sm),
        Text(
          label,
          style: AppTextStyles.labelSmall,
        ),
      ],
    );
  }
}

/// Medicines section
class _MedicinesSection extends StatelessWidget {
  final List<MedicineResult> medicines;

  const _MedicinesSection({required this.medicines});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.md,
            vertical: AppSpacing.lg,
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                AppStrings.medicinesSection,
                style: AppTextStyles.headline4,
              ),
              const SizedBox(height: AppSpacing.sm),
              Text(
                '${medicines.length} medicines found',
                style: AppTextStyles.bodySmall.copyWith(
                  color: AppColors.textSecondary,
                ),
              ),
            ],
          ),
        ),
        ListView.builder(
          physics: const NeverScrollableScrollPhysics(),
          shrinkWrap: true,
          itemCount: medicines.length,
          itemBuilder: (context, index) {
            return MedicineCard(
              medicine: medicines[index],
              index: index,
            );
          },
        ),
      ],
    );
  }
}

/// Warnings section
class _WarningsSection extends StatelessWidget {
  final PrescriptionResult result;

  const _WarningsSection({required this.result});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.md,
            vertical: AppSpacing.lg,
          ),
          child: Row(
            children: [
              Icon(
                Icons.warning_rounded,
                color: AppColors.warning,
                size: 24,
              ),
              const SizedBox(width: AppSpacing.md),
              Text(
                AppStrings.warningsSection,
                style: AppTextStyles.headline4,
              ),
            ],
          ),
        ),

        // Drug Interactions
        if (result.drugInteractions.isNotEmpty)
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Padding(
                padding: const EdgeInsets.only(
                  left: AppSpacing.md,
                  right: AppSpacing.md,
                  bottom: AppSpacing.md,
                ),
                child: Text(
                  AppStrings.interactionsSection,
                  style: AppTextStyles.bodyMedium.copyWith(
                    fontWeight: FontWeight.w600,
                    color: AppColors.severityHigh,
                  ),
                ),
              ),
              ListView.builder(
                physics: const NeverScrollableScrollPhysics(),
                shrinkWrap: true,
                itemCount: result.drugInteractions.length,
                itemBuilder: (context, index) {
                  return DrugInteractionCard(
                    interaction: result.drugInteractions[index],
                  );
                },
              ),
            ],
          ),

        // Drug-Disease Warnings
        if (result.drugDiseaseWarnings.isNotEmpty)
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Padding(
                padding: const EdgeInsets.only(
                  left: AppSpacing.md,
                  right: AppSpacing.md,
                  bottom: AppSpacing.md,
                ),
                child: Text(
                  AppStrings.diseaseWarningsSection,
                  style: AppTextStyles.bodyMedium.copyWith(
                    fontWeight: FontWeight.w600,
                    color: AppColors.info,
                  ),
                ),
              ),
              ListView.builder(
                physics: const NeverScrollableScrollPhysics(),
                shrinkWrap: true,
                itemCount: result.drugDiseaseWarnings.length,
                itemBuilder: (context, index) {
                  final warning = result.drugDiseaseWarnings[index];
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
                          Text(
                            warning.drugName ?? 'Unknown Drug',
                            style: AppTextStyles.bodyMedium.copyWith(
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                          if (warning.indicationText != null)
                            Padding(
                              padding: const EdgeInsets.only(top: AppSpacing.md),
                              child: Text(
                                warning.indicationText!,
                                style: AppTextStyles.bodySmall,
                              ),
                            ),
                        ],
                      ),
                    ),
                  );
                },
              ),
            ],
          ),

        // Duplicate Ingredient Warnings
        if (result.duplicateIngredientWarnings.isNotEmpty)
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Padding(
                padding: const EdgeInsets.only(
                  left: AppSpacing.md,
                  right: AppSpacing.md,
                  bottom: AppSpacing.md,
                ),
                child: Text(
                  AppStrings.duplicateWarningsSection,
                  style: AppTextStyles.bodyMedium.copyWith(
                    fontWeight: FontWeight.w600,
                    color: AppColors.warning,
                  ),
                ),
              ),
              ListView.builder(
                physics: const NeverScrollableScrollPhysics(),
                shrinkWrap: true,
                itemCount: result.duplicateIngredientWarnings.length,
                itemBuilder: (context, index) {
                  return DuplicateWarningCard(
                    warning: result.duplicateIngredientWarnings[index],
                  );
                },
              ),
            ],
          ),
      ],
    );
  }
}

/// Processing info card
class _ProcessingInfoCard extends StatelessWidget {
  final PrescriptionResult result;

  const _ProcessingInfoCard({required this.result});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.all(AppSpacing.md),
      padding: const EdgeInsets.all(AppSpacing.md),
      decoration: BoxDecoration(
        color: AppColors.surfaceVariant,
        borderRadius: BorderRadius.circular(AppRadius.md),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Analysis Details',
            style: AppTextStyles.bodyMedium.copyWith(
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: AppSpacing.md),
          Row(
            children: [
              Icon(
                Icons.info_outline,
                size: 16,
                color: AppColors.textSecondary,
              ),
              const SizedBox(width: AppSpacing.sm),
              Expanded(
                child: Text(
                  'This analysis is for informational purposes only. '
                  'Always consult with your healthcare provider.',
                  style: AppTextStyles.bodySmall.copyWith(
                    color: AppColors.textSecondary,
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
