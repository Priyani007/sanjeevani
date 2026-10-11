package com.sanjeevani

import android.graphics.PointF
import com.sanjeevani.engine.AllergyPositioningAssessor
import com.sanjeevani.engine.AllergyPositionResult
import com.sanjeevani.engine.AllergicReactionModule
import com.sanjeevani.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** Unit tests for Allergic Reaction camera-based positioning assessments. */
class AllergyPositioningTest {

    private lateinit var assessor: AllergyPositioningAssessor

    @Before
    fun setUp() {
        assessor = AllergyPositioningAssessor()
    }

    // ────────────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────────────

    /** Minimal pose skeleton with 33 landmarks, all at the given positions with visibility=1. */
    private fun emptyPose(): List<NormalizedLandmark> =
        List(33) { NormalizedLandmark(0.5f, 0.5f, 0f, 0f) }

    private fun MutableList<NormalizedLandmark>.set(idx: Int, x: Float, y: Float, vis: Float = 1f) {
        this[idx] = NormalizedLandmark(x, y, 0f, vis)
    }

    private fun lyingPose(): List<NormalizedLandmark> {
        // Patient lying flat: shoulders and hips roughly at same Y; arms on opposite sides
        val p = emptyPose().toMutableList()
        // Left shoulder (11) on left side, right shoulder (12) on right side
        p.set(11, 0.30f, 0.50f)  // left shoulder
        p.set(12, 0.70f, 0.50f)  // right shoulder
        p.set(23, 0.30f, 0.50f)  // left hip
        p.set(24, 0.70f, 0.50f)  // right hip
        p.set(13, 0.15f, 0.55f)  // left elbow
        p.set(14, 0.85f, 0.55f)  // right elbow
        p.set(15, 0.10f, 0.55f)  // left wrist
        p.set(16, 0.90f, 0.55f)  // right wrist
        return p
    }

    private fun standingPose(): List<NormalizedLandmark> {
        // Person standing: shoulders above hips (low Y → high Y as we go down body)
        val p = emptyPose().toMutableList()
        p.set(11, 0.30f, 0.20f)  // left shoulder
        p.set(12, 0.70f, 0.20f)  // right shoulder
        p.set(23, 0.35f, 0.55f)  // left hip
        p.set(24, 0.65f, 0.55f)  // right hip
        p.set(13, 0.25f, 0.35f)  // left elbow
        p.set(14, 0.75f, 0.35f)  // right elbow
        p.set(15, 0.22f, 0.50f)  // left wrist
        p.set(16, 0.78f, 0.50f)  // right wrist
        return p
    }

    private fun flatLegPose(): List<NormalizedLandmark> {
        // Patient lying; legs flat (hip and ankle at same Y)
        val p = emptyPose().toMutableList()
        p.set(23, 0.30f, 0.50f); p.set(25, 0.30f, 0.70f); p.set(27, 0.30f, 0.90f)  // left leg
        p.set(24, 0.70f, 0.50f); p.set(26, 0.70f, 0.70f); p.set(28, 0.70f, 0.90f)  // right leg
        return p
    }

    private fun raisedLegPose(): List<NormalizedLandmark> {
        // Legs raised: ankle Y < hip Y (ankle higher on screen than hip)
        val p = emptyPose().toMutableList()
        p.set(23, 0.30f, 0.70f); p.set(25, 0.30f, 0.50f); p.set(27, 0.30f, 0.30f)  // left leg elevated
        p.set(24, 0.70f, 0.70f); p.set(26, 0.70f, 0.50f); p.set(28, 0.70f, 0.30f)  // right leg elevated
        return p
    }

    // ────────────────────────────────────────────────────────────────────────
    // assessPatientLying
    // ────────────────────────────────────────────────────────────────────────

    @Test
    fun `null pose returns inconclusive lying result`() {
        val (result, _) = assessor.assessPatientLying(null)
        assertEquals(AllergyPositionResult.INCONCLUSIVE, result)
    }

    @Test
    fun `pose with low visibility landmarks returns inconclusive`() {
        val p = emptyPose().toMutableList()
        p.set(11, 0.3f, 0.5f, 0.3f)  // below VIS_THRESHOLD
        p.set(12, 0.7f, 0.5f, 0.3f)
        val (result, _) = assessor.assessPatientLying(p)
        assertEquals(AllergyPositionResult.INCONCLUSIVE, result)
    }

    // ────────────────────────────────────────────────────────────────────────
    // assessLegElevation
    // ────────────────────────────────────────────────────────────────────────

    @Test
    fun `null pose returns inconclusive leg elevation`() {
        assertEquals(AllergyPositionResult.INCONCLUSIVE, assessor.assessLegElevation(null))
    }

    @Test
    fun `flat legs return incorrect after consistency frames`() {
        val pose = flatLegPose()
        var result = AllergyPositionResult.INCONCLUSIVE
        repeat(10) { result = assessor.assessLegElevation(pose) }
        assertEquals(AllergyPositionResult.INCORRECT, result)
    }

    @Test
    fun `raised legs return correct after consistency frames`() {
        val pose = raisedLegPose()
        var result = AllergyPositionResult.INCONCLUSIVE
        repeat(10) { result = assessor.assessLegElevation(pose) }
        assertEquals(AllergyPositionResult.CORRECT, result)
    }

    @Test
    fun `leg elevation is inconclusive until consistency threshold met`() {
        val pose = raisedLegPose()
        // First frame should be INCONCLUSIVE, not CORRECT
        val firstResult = assessor.assessLegElevation(pose)
        assertEquals(AllergyPositionResult.INCONCLUSIVE, firstResult)
    }

    @Test
    fun `transitioning from raised to flat resets and stabilises`() {
        val raised = raisedLegPose()
        val flat = flatLegPose()
        // Establish raised
        repeat(10) { assessor.assessLegElevation(raised) }
        // Switch to flat
        var result = AllergyPositionResult.INCONCLUSIVE
        repeat(10) { result = assessor.assessLegElevation(flat) }
        assertEquals(AllergyPositionResult.INCORRECT, result)
    }

    @Test
    fun `low visibility leg landmarks return inconclusive`() {
        val p = raisedLegPose().toMutableList()
        p.set(26, 0.7f, 0.5f, 0.2f)  // right knee below VIS_THRESHOLD
        p.set(28, 0.7f, 0.3f, 0.2f)  // right ankle below VIS_THRESHOLD
        // Force CORRECT on left leg to check right-fallback
        repeat(10) { assessor.assessLegElevation(p) }
        // Left leg is also low vis in emptyPose — should be inconclusive
        val emptyVis = emptyPose()
        assertEquals(AllergyPositionResult.INCONCLUSIVE, assessor.assessLegElevation(emptyVis))
    }

    // ────────────────────────────────────────────────────────────────────────
    // estimateThighTarget
    // ────────────────────────────────────────────────────────────────────────

    @Test
    fun `null pose gives null thigh target`() {
        assertNull(assessor.rawThighTarget(null))
    }

    @Test
    fun `low visibility hip gives null thigh target`() {
        val p = emptyPose().toMutableList()
        p.set(24, 0.7f, 0.6f, 0.3f)  // hip below threshold
        p.set(26, 0.7f, 0.8f)
        assertNull(assessor.rawThighTarget(p))
    }

    @Test
    fun `valid hip and knee return midpoint as thigh target`() {
        val p = emptyPose().toMutableList()
        p.set(24, 0.6f, 0.6f)   // right hip
        p.set(26, 0.8f, 0.8f)   // right knee
        // Use rawThighTarget which operates on NormalizedLandmark fields (not Android PointF stubs).
        val raw = assessor.rawThighTarget(p)
        assertNotNull(raw)
        // EMA initialises to exact midpoint on first call
        assertEquals(0.70f, raw!!.first,  0.001f)
        assertEquals(0.70f, raw.second, 0.001f)
    }

    // ────────────────────────────────────────────────────────────────────────
    // assessWristToThigh
    // ────────────────────────────────────────────────────────────────────────

    @Test
    fun `tracking lost when no hip knee landmarks`() {
        val state = assessor.assessWristToThigh(null, 0L)
        assertEquals(AllergyPositionResult.INCONCLUSIVE, state.result)
        assertNull(state.wristPosition)
    }

    @Test
    fun `wrist on thigh target returns correct after hold`() {
        val p = emptyPose().toMutableList()
        p.set(24, 0.60f, 0.60f)   // right hip
        p.set(26, 0.80f, 0.80f)   // right knee  → thigh target ~(0.70, 0.70)
        p.set(16, 0.70f, 0.70f)   // right wrist on target
        var state = assessor.assessWristToThigh(p, 0L)
        // Feed multiple frames with increasing timestamps past hold duration
        repeat(5) { i ->
            state = assessor.assessWristToThigh(p, (i + 1) * 800L)
        }
        assertEquals(AllergyPositionResult.CORRECT, state.result)
    }

    @Test
    fun `wrist far left of thigh returns MOVE_RIGHT`() {
        val p = emptyPose().toMutableList()
        p.set(24, 0.60f, 0.60f)
        p.set(26, 0.80f, 0.80f)   // thigh ~(0.70, 0.70)
        p.set(16, 0.20f, 0.70f)   // wrist far to the left (lower x)
        var state = assessor.assessWristToThigh(p, 0L)
        repeat(3) { i -> state = assessor.assessWristToThigh(p, (i + 1) * 800L) }
        // Wrist x (0.20) < thigh x (0.70) → dx negative → MOVE_RIGHT
        assertEquals(com.sanjeevani.model.SpatialAction.MOVE_RIGHT, state.correctiveAction)
        assertEquals(AllergyPositionResult.INCORRECT, state.result)
    }

    @Test
    fun `wrist far right of thigh returns MOVE_LEFT`() {
        val p = emptyPose().toMutableList()
        p.set(24, 0.60f, 0.60f)
        p.set(26, 0.80f, 0.80f)   // thigh ~(0.70, 0.70)
        p.set(16, 0.95f, 0.70f)   // wrist far right (high x)
        var state = assessor.assessWristToThigh(p, 0L)
        repeat(3) { i -> state = assessor.assessWristToThigh(p, (i + 1) * 800L) }
        assertEquals(com.sanjeevani.model.SpatialAction.MOVE_LEFT, state.correctiveAction)
    }

    @Test
    fun `wrist above thigh returns MOVE_DOWN`() {
        val p = emptyPose().toMutableList()
        p.set(24, 0.60f, 0.60f)
        p.set(26, 0.80f, 0.80f)   // thigh ~(0.70, 0.70)
        p.set(16, 0.70f, 0.20f)   // wrist high on screen (low Y)
        var state = assessor.assessWristToThigh(p, 0L)
        repeat(3) { i -> state = assessor.assessWristToThigh(p, (i + 1) * 800L) }
        // dy = 0.20 - 0.70 = negative → MOVE_DOWN (move toward higher Y = down screen)
        assertEquals(com.sanjeevani.model.SpatialAction.MOVE_DOWN, state.correctiveAction)
    }

    @Test
    fun `wrist below thigh returns MOVE_UP`() {
        val p = emptyPose().toMutableList()
        p.set(24, 0.60f, 0.60f)
        p.set(26, 0.80f, 0.80f)   // thigh ~(0.70, 0.70)
        p.set(16, 0.70f, 0.95f)   // wrist low on screen (high Y)
        var state = assessor.assessWristToThigh(p, 0L)
        repeat(3) { i -> state = assessor.assessWristToThigh(p, (i + 1) * 800L) }
        assertEquals(com.sanjeevani.model.SpatialAction.MOVE_UP, state.correctiveAction)
    }

    @Test
    fun `correct result breaks when wrist drifts far away`() {
        val p = emptyPose().toMutableList()
        p.set(24, 0.60f, 0.60f); p.set(26, 0.80f, 0.80f)
        p.set(16, 0.70f, 0.70f)  // on target
        repeat(6) { i -> assessor.assessWristToThigh(p, (i + 1) * 800L) }

        // Now move wrist far away
        val q = p.toMutableList()
        q.set(16, 0.20f, 0.70f)
        var state = assessor.assessWristToThigh(q, 7 * 800L)
        repeat(3) { i -> state = assessor.assessWristToThigh(q, (8 + i) * 800L) }
        assertNotEquals(AllergyPositionResult.CORRECT, state.result)
    }

    // ────────────────────────────────────────────────────────────────────────
    // AllergicReactionModule — phase voice and AR overlay
    // ────────────────────────────────────────────────────────────────────────

    @Test
    fun `module emits voice only on first process after phase change`() {
        val module = AllergicReactionModule()
        val frame = PerceptionFrame(0L, null, null, null, 1, 1)
        val r1 = module.process(frame, 0L)
        val r2 = module.process(frame, 100L)
        assertNotNull("First process should emit voice", r1.voiceText)
        assertNull("Second process in same phase should not repeat voice", r2.voiceText)
    }

    @Test
    fun `selectTrainingStep maps indices to correct phases`() {
        val module = AllergicReactionModule()
        module.selectTrainingStep(2)
        assertEquals(AllergicPhase.RAISE_LEGS.ordinal, module.currentPhaseOrdinal)

        module.selectTrainingStep(3)
        assertEquals(AllergicPhase.EPIPEN_READY.ordinal, module.currentPhaseOrdinal)

        module.selectTrainingStep(4)
        assertEquals(AllergicPhase.EPIPEN_INJECT.ordinal, module.currentPhaseOrdinal)

        module.selectTrainingStep(0)
        assertEquals(AllergicPhase.LAY_FLAT.ordinal, module.currentPhaseOrdinal)
    }

    @Test
    fun `reset returns module to LAY_FLAT with no assessment status`() {
        val module = AllergicReactionModule()
        module.selectTrainingStep(4)
        module.reset()
        assertEquals(AllergicPhase.LAY_FLAT.ordinal, module.currentPhaseOrdinal)
        assertEquals(0, module.currentTrainingStepIndex)
    }

    @Test
    fun `module generates non-empty statusText for every phase`() {
        val module = AllergicReactionModule()
        val frame = PerceptionFrame(0L, null, null, null, 1, 1)
        AllergicPhase.values().forEach { phase ->
            val stepIdx = when (phase) {
                AllergicPhase.LAY_FLAT      -> 0
                AllergicPhase.RAISE_LEGS    -> 2
                AllergicPhase.SAFE_POSITION -> 1
                AllergicPhase.EPIPEN_READY  -> 3
                AllergicPhase.EPIPEN_INJECT -> 4
                AllergicPhase.MONITORING    -> 5
            }
            module.selectTrainingStep(stepIdx)
            val result = module.process(frame, 0L)
            assertTrue(
                "statusText empty for phase $phase",
                result.overlay.statusText.isNotEmpty()
            )
        }
    }

    @Test
    fun `epipen phases show marker in overlay`() {
        val module = AllergicReactionModule()
        val p = emptyPose().toMutableList()
        p.set(24, 0.60f, 0.60f); p.set(26, 0.80f, 0.80f)
        val frame = PerceptionFrame(0L, p, null, null, 1, 1)

        module.selectTrainingStep(3)
        val readyResult = module.process(frame, 0L)
        assertTrue("EpiPen READY should show marker", readyResult.overlay.showEpiPenMarker)
        assertNotNull("EpiPen READY should have thigh target", readyResult.overlay.thighTarget)

        module.selectTrainingStep(4)
        val injectResult = module.process(frame, 0L)
        assertTrue("EpiPen INJECT should show marker", injectResult.overlay.showEpiPenMarker)
    }

    @Test
    fun `manual progression records distinct state without fabricating success`() {
        // AllergicReactionModule itself does not "succeed" a step — it only updates
        // currentAssessmentGreen based on camera evidence. Manual advancement is
        // handled by ModuleTrainingViewModel.continueManually(). This test verifies
        // that calling selectTrainingStep() without camera evidence does NOT mark
        // currentAssessmentGreen = true for phases that require camera observation.
        val module = AllergicReactionModule()
        module.selectTrainingStep(2)  // RAISE_LEGS — requires camera
        val frame = PerceptionFrame(0L, null, null, null, 1, 1)  // no camera
        module.process(frame, 0L)
        // Without any pose data, assessment must not be green
        assertFalse(
            "Manual step selection without camera evidence must not fabricate CORRECT",
            module.currentAssessmentGreen
        )
    }

    @Test
    fun `inject phase sets arrowFrom and arrowTo when wrist far from thigh`() {
        val module = AllergicReactionModule()
        module.selectTrainingStep(4)  // EPIPEN_INJECT

        val p = emptyPose().toMutableList()
        p.set(24, 0.60f, 0.60f); p.set(26, 0.80f, 0.80f)  // thigh landmarks
        p.set(16, 0.20f, 0.70f)   // wrist far from thigh → needs direction
        val frame = PerceptionFrame(0L, p, null, null, 1, 1)

        // Run a few frames so action stabilises past hold duration
        var result = module.process(frame, 0L)
        repeat(3) { i -> result = module.process(frame, (i + 1) * 800L) }

        assertNotNull("Arrow 'from' should be set when wrist is misplaced", result.overlay.arrowFrom)
        assertNotNull("Arrow 'to' should be set when wrist is misplaced", result.overlay.arrowTo)
    }

    @Test
    fun `resetAll on assessor clears leg streak`() {
        val pose = raisedLegPose()
        repeat(10) { assessor.assessLegElevation(pose) }
        assessor.resetAll()
        // After reset, first frame should be INCONCLUSIVE again
        assertEquals(AllergyPositionResult.INCONCLUSIVE, assessor.assessLegElevation(pose))
    }
}
