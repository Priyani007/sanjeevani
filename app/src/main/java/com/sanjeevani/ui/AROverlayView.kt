package com.sanjeevani.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Looper
import android.view.View
import com.sanjeevani.model.AROverlaySpec
import com.sanjeevani.model.EmergencyType
import com.sanjeevani.model.FSMState
import android.view.animation.LinearInterpolator

class AROverlayView(context: Context) : View(context) {

    var overlaySpec: AROverlaySpec? = null
        set(value) {
            val previous = field
            field = value
            if (value != null && value.confirmationEvent > lastConfirmationEvent) {
                lastConfirmationEvent = value.confirmationEvent
                startPositionReadyAnimation()
            }
            updateCompressionSphereAnimation(value)
            updateHeartPulseAnimation(value)
            invalidate()
        }

    private var lastConfirmationEvent = 0L
    private fun sx(x: Float): Float {
        val s = overlaySpec ?: return x * width
        return OverlayCoordinates.fit(x, 0f, s.imageWidth, s.imageHeight, width, height).first
    }
    private fun sy(y: Float): Float {
        val s = overlaySpec ?: return y * height
        return OverlayCoordinates.fit(0f, y, s.imageWidth, s.imageHeight, width, height).second
    }
    private var stepCardScale = 1f
    private var stepCardAnimator: ValueAnimator? = null
    private var spherePulseFraction = 0f
    private var spherePulseAnimator: ValueAnimator? = null
    private val sphereColorEvaluator = ArgbEvaluator()
    private var heartPulseFraction = 0f
    private var heartPulseAnimator: ValueAnimator? = null

    // ── Paints ────────────────────────────────────────────────────────────────

    private val targetPaintGreen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GREEN; style = Paint.Style.STROKE; strokeWidth = 6f
    }
    private val targetPaintRed = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED; style = Paint.Style.STROKE; strokeWidth = 6f
    }
    private val targetFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(40, 0, 255, 0); style = Paint.Style.FILL
    }
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 80, 0); style = Paint.Style.STROKE
        strokeWidth = 8f; strokeCap = Paint.Cap.ROUND
    }
    private val skeletonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 0, 200, 255); style = Paint.Style.STROKE; strokeWidth = 6f
    }
    private val jointDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val jointOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 0, 0, 0); style = Paint.Style.STROKE; strokeWidth = 2f
    }
    private val handDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW; style = Paint.Style.FILL
    }
    private val statusTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 48f; typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(4f, 2f, 2f, Color.BLACK)
    }
    private val guidancePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 200, 0); textSize = 44f; typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(4f, 2f, 2f, Color.BLACK)
    }
    private val ratePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 56f; typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(4f, 2f, 2f, Color.BLACK)
    }
    private val rateBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(160, 0, 0, 0); style = Paint.Style.FILL
    }
    private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 32f; typeface = Typeface.DEFAULT_BOLD
    }
    // iOS-style step card paints
    private val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val cardTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 46f; typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(2f, 1f, 1f, Color.argb(120, 0, 0, 0))
    }
    private val cardBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 34f
    }
    private val cardStatusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 38f; typeface = Typeface.DEFAULT_BOLD
    }
    private val comprCountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 50f; typeface = Typeface.DEFAULT_BOLD
    }
    private val comprTimePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 220, 50); textSize = 40f; typeface = Typeface.DEFAULT_BOLD
    }
    private val comprCounterBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(210, 0, 0, 0); style = Paint.Style.FILL
    }
    private val sphereCountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 76f; typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        setShadowLayer(5f, 0f, 2f, Color.BLACK)
    }
    private val sphereCountLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 27f; typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        setShadowLayer(4f, 0f, 2f, Color.BLACK)
    }
    // iOS CPR header badge paints
    private val cprBadgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(230, 190, 40, 40); style = Paint.Style.FILL
    }
    private val cprBadgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 38f; typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(2f, 1f, 1f, Color.argb(120, 0, 0, 0))
    }
    private val cprSubtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 26f
        setShadowLayer(2f, 1f, 1f, Color.argb(120, 0, 0, 0))
    }
    // iOS vitals panel paints
    private val vitalsPanelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 20, 20, 30); style = Paint.Style.FILL
    }
    private val vitalsPanelBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(60, 255, 255, 255); style = Paint.Style.STROKE; strokeWidth = 1.5f
    }
    private val vitalsLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 255, 255, 255); textSize = 22f
    }
    private val vitalsBpmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 46f; typeface = Typeface.DEFAULT_BOLD
    }
    private val vitalsGreenDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(50, 205, 50); style = Paint.Style.FILL
    }
    private val vitalsHeartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(140, 180, 180, 180); style = Paint.Style.FILL; textSize = 54f
    }
    // Pre-allocated paints for draw methods (avoids per-frame GC pressure)
    private val sphereGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val sphereBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sphereHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(120, 255, 255, 255); style = Paint.Style.FILL
    }
    private val cprSubBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 0, 0, 0); style = Paint.Style.FILL
    }
    private val heartDynamicPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val safeRangePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 34, 139, 34); style = Paint.Style.FILL
    }
    private val progressBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val progressTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(80, 255, 255, 255); style = Paint.Style.FILL
    }
    private val epiPenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 140, 0); style = Paint.Style.STROKE; strokeWidth = 6f
    }
    private val epiPenFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(60, 255, 140, 0); style = Paint.Style.FILL
    }
    private val shoulderBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 5f; strokeCap = Paint.Cap.ROUND
    }
    // Face mesh paint kept for potential future use but not rendered (adds visual noise)

    private var epiPenPulseRadius = 40f
    private var epiPenPulseGrowing = true

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val spec = overlaySpec ?: return

        // ── iOS-matching CPR header badge (top-center) ────────────────────────
        if (spec.showCPRBadge) drawCPRHeaderBadge(canvas, spec)

        // ── iOS-matching vitals panel (upper-left) ────────────────────────────
        if (spec.showVitalsPanel) drawVitalsPanel(canvas)

        // ── Multi-emergency overlays (drawn first, behind CPR elements) ────────
        drawEmergencyBadge(canvas, spec)
        drawEpiPenMarker(canvas, spec)
        drawPhaseProgressBar(canvas, spec)
        drawStrokeAsymmetryBars(canvas, spec)

        // ── Skeleton bones — iOS yellow, thin (mirrors BodySkeleton.swift yellow bones) ──
        skeletonPaint.strokeWidth = 6f
        spec.skeletonLines.forEach { (from, to) ->
            skeletonPaint.color = Color.argb(220, 255, 214, 10)   // iOS systemYellow
            canvas.drawLine(sx(from.x), sy(from.y), sx(to.x), sy(to.y), skeletonPaint)
        }

        // ── Joint dots (heatmap colored, large and precise like iOS) ─────────
        spec.jointPoints.forEach { (pt, color) ->
            val px = sx(pt.x)
            val py = sy(pt.y)
            val radius = 22f
            // Dark outline for contrast against any background
            jointOutlinePaint.strokeWidth = 3f
            canvas.drawCircle(px, py, radius + 2f, jointOutlinePaint)
            jointDotPaint.color = color
            canvas.drawCircle(px, py, radius, jointDotPaint)
        }

        // ── Sternum target: iOS-style large blue sphere with sheen ────────────
        spec.sternumTarget?.let { t ->
            val px = sx(t.x)
            val py = sy(t.y)
            val baseRadius = 58f
            val pulseActive = spherePulseAnimator != null
            val r = if (pulseActive) {
                baseRadius * (0.65f + 0.35f * spherePulseFraction)
            } else {
                baseRadius
            }
            val pulseColor = if (pulseActive) {
                sphereColorEvaluator.evaluate(
                    spherePulseFraction,
                    Color.argb(200, 0, 120, 255),
                    Color.argb(240, 255, 60, 60)
                ) as Int
            } else {
                Color.argb(255, 30, 130, 220)
            }
            // Outer glow
            sphereGlowPaint.color = Color.argb(
                70,
                Color.red(pulseColor),
                Color.green(pulseColor),
                Color.blue(pulseColor)
            )
            canvas.drawCircle(px, py, r + 18f, sphereGlowPaint)
            // Main sphere body — iOS cyan-blue
            val highlightColor = sphereColorEvaluator.evaluate(
                0.32f,
                pulseColor,
                Color.WHITE
            ) as Int
            sphereBodyPaint.shader = android.graphics.RadialGradient(
                px - r * 0.28f, py - r * 0.28f, r,
                intArrayOf(highlightColor, pulseColor),
                floatArrayOf(0f, 1f),
                android.graphics.Shader.TileMode.CLAMP
            )
            canvas.drawCircle(px, py, r, sphereBodyPaint)
            // Specular highlight (top-left white spot)
            canvas.drawCircle(px - r * 0.28f, py - r * 0.28f, r * 0.32f, sphereHighlightPaint)

            if (spec.state == FSMState.COMPRESSION_ACTIVE) {
                val bpm = spec.compressionRate ?: 0f
                val paceColor = when {
                    bpm <= 0f -> Color.WHITE
                    bpm < 100f || bpm > 120f -> Color.rgb(255, 59, 48)
                    else -> Color.rgb(52, 211, 153)
                }
                sphereCountPaint.color = paceColor
                sphereCountLabelPaint.color = paceColor
                val countY = py - baseRadius - 70f
                canvas.drawText(spec.compressionCount.toString(), px, countY, sphereCountPaint)
                canvas.drawText(if (spec.countReliable) "estimated cycles" else "push hard and fast", px, countY + 35f, sphereCountLabelPaint)
                canvas.drawText("Pacing cue • not depth", px, py + baseRadius + 50f, sphereCountLabelPaint)
            }
        }

        // ── Hand marker ───────────────────────────────────────────────────────
        spec.leftHandCenter?.let { h ->
            canvas.drawCircle(sx(h.x), sy(h.y), 20f, handDotPaint)
        }

        // ── Correction arrow ──────────────────────────────────────────────────
        spec.arrowFrom?.let { from ->
            spec.arrowTo?.let { to ->
                val fx = sx(from.x); val fy = sy(from.y)
                val tx = sx(to.x); val ty = sy(to.y)
                canvas.drawLine(fx, fy, tx, ty, arrowPaint)
                drawArrowHead(canvas, fx, fy, tx, ty)
            }
        }

        // ── iOS-style step card (upper-right) ─────────────────────────────────
        drawStepCard(canvas, spec)

        // ── Guidance direction arrow text (center-bottom, only while positioning) ─
        if (spec.guidanceText.isNotEmpty()) {
            val isHandPlacementCorrection = spec.state == FSMState.HAND_POSITIONING
            guidancePaint.color = if (isHandPlacementCorrection) {
                Color.rgb(255, 59, 48) // 0xFFFF3B30
            } else {
                Color.rgb(255, 200, 0)
            }
            guidancePaint.typeface = Typeface.DEFAULT_BOLD
            val tw = guidancePaint.measureText(spec.guidanceText)
            canvas.drawText(spec.guidanceText, (width - tw) / 2f, height * 0.82f, guidancePaint)
        }

        // ── Compression rate badge (bottom, shown during active CPR) ──────────
        spec.compressionRate?.let { bpm ->
            val bpmText = if (bpm > 0f) "CPR: ${bpm.toInt()} BPM" else "Begin compressions"
            val tw = ratePaint.measureText(bpmText)
            val bx = (width - tw) / 2f - 20f
            val by = height - 60f
            canvas.drawRoundRect(bx, by - 60f, bx + tw + 40f, by + 10f, 16f, 16f, rateBgPaint)
            ratePaint.color = when {
                bpm in 100f..120f -> Color.GREEN
                bpm > 0f -> Color.rgb(255, 180, 0)
                else -> Color.WHITE
            }
            canvas.drawText(bpmText, bx + 20f, by, ratePaint)
        }
    }

    // "🫀 CPR/Heart Attack" pill + subtitle — matches iOS CardiacArrestView header
    private fun drawCPRHeaderBadge(canvas: Canvas, spec: AROverlaySpec) {
        val label = "CPR guidance"
        val sublabel = "Adult prototype • follow 112"
        val pill_h = 56f; val pill_r = 14f; val pad = 20f
        val tw = cprBadgeTextPaint.measureText(label)
        val left = (width - tw) / 2f - pad
        val right = (width + tw) / 2f + pad
        val top = 55f; val bottom = top + pill_h
        canvas.drawRoundRect(left, top, right, bottom, pill_r, pill_r, cprBadgeBgPaint)
        canvas.drawText(label, left + pad, top + pill_h * 0.72f, cprBadgeTextPaint)
        // Subtitle below the pill
        val sw = cprSubtitlePaint.measureText(sublabel)
        val subBgLeft = (width - sw) / 2f - 16f
        val subBgTop = bottom + 6f
        canvas.drawRoundRect(subBgLeft, subBgTop, subBgLeft + sw + 32f, subBgTop + 36f, 8f, 8f, cprSubBgPaint)
        canvas.drawText(sublabel, subBgLeft + 16f, subBgTop + 26f, cprSubtitlePaint)
    }

    // Vitals panel: shows real BPM from compressionRate with color coding + pulsing heart
    private fun drawVitalsPanel(canvas: Canvas) {
        val spec = overlaySpec ?: return
        val panelW = width * 0.35f
        val panelH = 230f
        val left = 16f; val top = height * 0.28f
        val right = left + panelW; val bottom = top + panelH
        val r = 20f

        // Frosted-glass background
        canvas.drawRoundRect(left, top, right, bottom, r, r, vitalsPanelBgPaint)
        canvas.drawRoundRect(left, top, right, bottom, r, r, vitalsPanelBorderPaint)

        // Header row: green dot + "CPR Active"
        canvas.drawCircle(left + 22f, top + 22f, 7f, vitalsGreenDotPaint)
        canvas.drawText("CPR Active", left + 34f, top + 30f, vitalsLabelPaint)

        // Pulsing heart — scale 1.0→1.3 driven by heartPulseFraction
        val heartScale = 1f + 0.3f * heartPulseFraction
        val bpm = spec.compressionRate ?: 0f
        val heartColor = when {
            bpm in 100f..120f -> Color.rgb(255, 80, 80)    // good range: red heart
            bpm > 0f -> Color.rgb(255, 160, 40)             // out of range: orange
            else -> Color.argb(140, 160, 160, 160)          // no compressions: grey
        }
        heartDynamicPaint.color = heartColor
        heartDynamicPaint.textSize = 60f * heartScale
        val heartStr = "♥"
        val hx = (left + right) / 2f - heartDynamicPaint.measureText(heartStr) / 2f
        canvas.drawText(heartStr, hx, top + 120f, heartDynamicPaint)

        // BPM display: real value or "--"
        val bpmStr = if (bpm > 0f) "${bpm.toInt()} BPM" else "-- BPM"
        vitalsBpmPaint.color = when {
            bpm in 100f..120f -> Color.rgb(52, 211, 153)    // green — safe range
            bpm in 90f..130f -> Color.WHITE
            bpm > 0f -> Color.rgb(255, 59, 48)              // red — out of range
            else -> Color.WHITE
        }
        val bx = (left + right) / 2f - vitalsBpmPaint.measureText(bpmStr) / 2f
        canvas.drawText(bpmStr, bx, top + 175f, vitalsBpmPaint)

        // "safe range" badge when BPM 100-120
        if (bpm in 100f..120f) {
            val badgeStr = "safe range"
            val sw = vitalsLabelPaint.measureText(badgeStr)
            val bBgLeft = (left + right) / 2f - sw / 2f - 8f
            val bBgTop = bottom - 38f
            canvas.drawRoundRect(bBgLeft, bBgTop, bBgLeft + sw + 16f, bBgTop + 26f, 8f, 8f, safeRangePaint)
            canvas.drawText(badgeStr, bBgLeft + 8f, bBgTop + 18f, vitalsLabelPaint)
        }
    }

    // Draw a floating iOS-style step instruction card in the upper-right area
    private fun drawStepCard(canvas: Canvas, spec: AROverlaySpec) {
        if (spec.stepCardTitle.isEmpty()) return

        val cardLeft = width * 0.05f
        val cardRight = width - 20f
        val cardTop = height * 0.14f
        val cardW = cardRight - cardLeft
        val padding = 24f
        val lineH = 46f
        val radius = 24f

        // Measure body text (wrap at card width)
        val bodyLines = wrapText(spec.stepCardInstruction, cardBodyPaint, cardW - padding * 2)
        val statusLines = wrapText(spec.stepCardStatus, cardStatusPaint, cardW - padding * 2)
        val cardH = padding + lineH + 12f + (bodyLines.size * lineH * 0.9f) + 20f + statusLines.size * lineH + padding

        val saveCount = canvas.save()
        if (spec.stepCardTitle == "1. Position Check" && stepCardScale != 1f) {
            canvas.scale(
                stepCardScale,
                stepCardScale,
                (cardLeft + cardRight) / 2f,
                cardTop + cardH / 2f
            )
        }

        // Card background
        cardBgPaint.color = if (spec.stepCardBgColor != 0) spec.stepCardBgColor else Color.argb(210, 50, 50, 50)
        canvas.drawRoundRect(cardLeft, cardTop, cardRight, cardTop + cardH, radius, radius, cardBgPaint)

        var y = cardTop + padding + lineH * 0.75f

        // Title row: icon + title
        val titleText = "${spec.stepCardIcon}  ${spec.stepCardTitle}"
        canvas.drawText(titleText, cardLeft + padding, y, cardTitlePaint)
        y += lineH + 12f

        // Body instruction (multi-line)
        for (line in bodyLines) {
            canvas.drawText(line, cardLeft + padding, y, cardBodyPaint)
            y += lineH * 0.9f
        }
        y += 16f

        // Status line (bold white)
        for (line in statusLines) {
            canvas.drawText(line, cardLeft + padding, y, cardStatusPaint)
            y += lineH
        }

        // Compression counter below card (only during compressions)
        if (spec.compressionCount > 0 || spec.elapsedSecs > 0) {
            val counterTop = cardTop + cardH + 16f
            val counterH = 110f
            canvas.drawRoundRect(cardLeft, counterTop, cardRight, counterTop + counterH, radius, radius, comprCounterBgPaint)
            canvas.drawText("Compressions: ${spec.compressionCount}", cardLeft + padding, counterTop + 52f, comprCountPaint)
            val mm = spec.elapsedSecs / 60
            val ss = spec.elapsedSecs % 60
            canvas.drawText("Time: %02d:%02d".format(mm, ss), cardLeft + padding, counterTop + 98f, comprTimePaint)
        }
        canvas.restoreToCount(saveCount)
    }

    private fun isPositionRed(color: Int): Boolean =
        Color.red(color) == 185 && Color.green(color) == 40 && Color.blue(color) == 40

    private fun isPositionGreen(color: Int): Boolean =
        Color.red(color) == 34 && Color.green(color) == 139 && Color.blue(color) == 34

    private fun startPositionReadyAnimation() {
        val startAnimation = {
            stepCardAnimator?.cancel()
            stepCardAnimator = ValueAnimator.ofFloat(1f, 1.08f, 0.97f, 1f).apply {
                duration = 400L
                addUpdateListener { animator ->
                    stepCardScale = animator.animatedValue as Float
                    invalidate()
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationCancel(animation: Animator) {
                        stepCardScale = 1f
                        invalidate()
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        stepCardScale = 1f
                        invalidate()
                    }
                })
                start()
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) startAnimation() else post(startAnimation)
    }

    private fun updateCompressionSphereAnimation(spec: AROverlaySpec?) {
        val shouldPulse = spec?.state == FSMState.COMPRESSION_ACTIVE

        val updateAnimation = {
            if (shouldPulse) {
                if (spherePulseAnimator == null) {
                    spherePulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                        duration = 30000L / (spec?.pacingBpm ?: 110).coerceIn(100, 120)
                        interpolator = LinearInterpolator()
                        repeatCount = ValueAnimator.INFINITE
                        repeatMode = ValueAnimator.REVERSE
                        addUpdateListener { animator ->
                            spherePulseFraction = animator.animatedValue as Float
                            invalidate()
                        }
                        start()
                    }
                }
            } else {
                spherePulseAnimator?.cancel()
                spherePulseAnimator = null
                spherePulseFraction = 0f
                invalidate()
            }
        }

        if (Looper.myLooper() == Looper.getMainLooper()) updateAnimation() else post(updateAnimation)
    }

    private fun updateHeartPulseAnimation(spec: AROverlaySpec?) {
        val shouldPulse = spec != null && spec.showVitalsPanel && (spec.compressionRate ?: 0f) > 0f

        val updateAnimation = {
            if (shouldPulse) {
                if (heartPulseAnimator == null) {
                    // Pulse at ~75 BPM rhythm (800ms cycle) — slightly slower than compressions
                    heartPulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                        duration = 400L
                        repeatCount = ValueAnimator.INFINITE
                        repeatMode = ValueAnimator.REVERSE
                        addUpdateListener { animator ->
                            heartPulseFraction = animator.animatedValue as Float
                            invalidate()
                        }
                        start()
                    }
                }
            } else {
                heartPulseAnimator?.cancel()
                heartPulseAnimator = null
                heartPulseFraction = 0f
                invalidate()
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) updateAnimation() else post(updateAnimation)
    }

    override fun onDetachedFromWindow() {
        stepCardAnimator?.cancel()
        stepCardAnimator = null
        spherePulseAnimator?.cancel()
        spherePulseAnimator = null
        spherePulseFraction = 0f
        heartPulseAnimator?.cancel()
        heartPulseAnimator = null
        heartPulseFraction = 0f
        super.onDetachedFromWindow()
    }

    // Simple word-wrap: split instruction into lines that fit within maxW pixels
    private fun wrapText(text: String, paint: Paint, maxW: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = ""
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxW) {
                current = candidate
            } else {
                if (current.isNotEmpty()) lines.add(current)
                current = word
            }
        }
        if (current.isNotEmpty()) lines.add(current)
        return lines
    }

    private fun emergencyColor(type: EmergencyType): Int = when (type) {
        EmergencyType.FAST_STROKE       -> Color.rgb(167, 139, 250)
        EmergencyType.HEART_ATTACK      -> Color.rgb(249, 115, 22)
        EmergencyType.ALLERGIC_REACTION -> Color.rgb(78, 204, 168)
        else -> Color.TRANSPARENT
    }

    private fun drawEmergencyBadge(canvas: Canvas, spec: AROverlaySpec) {
        val type = spec.emergencyType
        // CPR and HEART_ATTACK both use the CPR-style header badge; skip the generic badge for them
        if (type == EmergencyType.UNKNOWN || type == EmergencyType.CPR || type == EmergencyType.HEART_ATTACK) return
        val color = emergencyColor(type)
        val label = when (type) {
            EmergencyType.FAST_STROKE       -> "REPORTED STROKE CONCERN"
            EmergencyType.HEART_ATTACK      -> "HEART ATTACK"
            EmergencyType.ALLERGIC_REACTION -> "REPORTED ALLERGY CONCERN"
            else -> return
        }
        val tw = badgeTextPaint.measureText(label)
        val padding = 20f; val pillH = 52f
        val left = 16f; val top = 110f
        val right = left + tw + padding * 2f
        val bottom = top + pillH
        badgePaint.color = Color.argb(220, Color.red(color), Color.green(color), Color.blue(color))
        canvas.drawRoundRect(left, top, right, bottom, pillH / 2f, pillH / 2f, badgePaint)
        canvas.drawText(label, left + padding, top + pillH * 0.68f, badgeTextPaint)
    }

    private fun drawEpiPenMarker(canvas: Canvas, spec: AROverlaySpec) {
        if (!spec.showEpiPenMarker) return
        val target = spec.thighTarget ?: return   // right outer thigh, per HALO right_upLeg_joint
        val px = sx(target.x)
        val py = sy(target.y)

        // Green = "locate injection site" (EPIPEN_READY); red = "inject now" (EPIPEN_INJECT)
        val inject = spec.statusText.contains("Inject", ignoreCase = true)
        epiPenPaint.color = if (inject) Color.rgb(220, 40, 40) else Color.rgb(50, 220, 100)
        epiPenFillPaint.color = if (inject) Color.argb(60, 220, 40, 40) else Color.argb(60, 50, 220, 100)

        if (epiPenPulseGrowing) {
            epiPenPulseRadius += 2f
            if (epiPenPulseRadius > 70f) epiPenPulseGrowing = false
        } else {
            epiPenPulseRadius -= 2f
            if (epiPenPulseRadius < 35f) epiPenPulseGrowing = true
        }

        canvas.drawCircle(px, py, epiPenPulseRadius, epiPenFillPaint)
        canvas.drawCircle(px, py, epiPenPulseRadius, epiPenPaint)

        // "X" crosshair: two short lines through centre, matching HALO's X marker appearance
        val arm = epiPenPulseRadius * 0.55f
        epiPenPaint.strokeWidth = 8f
        canvas.drawLine(px - arm, py - arm, px + arm, py + arm, epiPenPaint)
        canvas.drawLine(px + arm, py - arm, px - arm, py + arm, epiPenPaint)
        epiPenPaint.strokeWidth = 6f  // restore default

        postInvalidateOnAnimation()
    }

    private fun drawPhaseProgressBar(canvas: Canvas, spec: AROverlaySpec) {
        val progress = spec.phaseProgress
        if (progress <= 0f) return
        val barY = height * 0.85f
        val barH = 10f
        val color = emergencyColor(spec.emergencyType)
        if (color == Color.TRANSPARENT) return

        canvas.drawRoundRect(0f, barY, width.toFloat(), barY + barH, barH / 2f, barH / 2f, progressTrackPaint)
        progressBarPaint.color = color
        val fillWidth = (width * progress).coerceAtLeast(0f)
        if (fillWidth > 0f) {
            canvas.drawRoundRect(0f, barY, fillWidth, barY + barH, barH / 2f, barH / 2f, progressBarPaint)
        }
    }

    private fun drawStrokeAsymmetryBars(canvas: Canvas, spec: AROverlaySpec) {
        if (spec.emergencyType != EmergencyType.FAST_STROKE) return
        val lsy = spec.leftShoulderY; val rsy = spec.rightShoulderY
        if (lsy == 0f && rsy == 0f) return

        val color = emergencyColor(EmergencyType.FAST_STROKE)
        shoulderBarPaint.color = Color.argb(180, Color.red(color), Color.green(color), Color.blue(color))

        val barTop = height * 0.3f; val barBottom = height * 0.7f
        // Left shoulder column
        val lx = width * 0.25f
        canvas.drawLine(lx, barTop, lx, barBottom, shoulderBarPaint)
        // Marker at shoulder Y
        val lyPx = lsy * height
        canvas.drawCircle(lx, lyPx, 12f, shoulderBarPaint)

        // Right shoulder column
        val rx = width * 0.75f
        canvas.drawLine(rx, barTop, rx, barBottom, shoulderBarPaint)
        val ryPx = rsy * height
        canvas.drawCircle(rx, ryPx, 12f, shoulderBarPaint)
    }

    private fun drawArrowHead(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float) {
        val dx = x2 - x1; val dy = y2 - y1
        val len = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        if (len < 1f) return
        val ux = dx / len; val uy = dy / len
        val size = 30f
        val ax = x2 - size * ux + size * 0.5f * (-uy)
        val ay = y2 - size * uy + size * 0.5f * ux
        val bx = x2 - size * ux - size * 0.5f * (-uy)
        val by = y2 - size * uy - size * 0.5f * ux
        canvas.drawLine(x2, y2, ax, ay, arrowPaint)
        canvas.drawLine(x2, y2, bx, by, arrowPaint)
    }
}
