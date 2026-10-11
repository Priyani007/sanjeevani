package com.sanjeevani.training

/**
 * Shared training step/option data model, reusing the CPR pattern.
 * Used by both StrokeTrainingContent and AllergyTrainingContent.
 */

data class ModuleTrainingOption(
    val id: String,          // "A", "B", "C", "D"
    val text: String,
    val feedback: String,    // shown after selection
    val isCorrect: Boolean,
    val isSafe: Boolean = true
)

/** A camera gate verifies only that the supported physical setup was observed. */
enum class ModuleCameraRequirement {
    NONE,
    FACE_REFERENCE_VISIBLE,
    BOTH_ARMS_VISIBLE,
    PATIENT_LYING_FLAT,
    THIGH_REFERENCE_VISIBLE
}

data class ModuleTrainingStep(
    val stepIndex: Int,
    val moduleType: String,
    val skillTag: String,
    val skillDescription: String,
    val stepTitle: String,
    val scenarioContext: String,
    val questionText: String,
    val options: List<ModuleTrainingOption>,
    val requiresPlacementSimulation: Boolean = false,
    val cameraRequirement: ModuleCameraRequirement = ModuleCameraRequirement.NONE
) {
    init {
        require(options.size == 4) { "Every training decision must have exactly four options" }
        require(options.map { it.id }.toSet() == setOf("A", "B", "C", "D")) {
            "Training options must be labelled A, B, C, and D"
        }
        require(options.count { it.isCorrect } == 1) { "Every training decision must have one correct option" }
    }

    val correctOption: ModuleTrainingOption get() = options.first { it.isCorrect }
}
