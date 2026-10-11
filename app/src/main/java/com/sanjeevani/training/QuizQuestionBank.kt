package com.sanjeevani.training

/**
 * Static question bank.
 *
 * Content version: v1.0-2025
 * Reference: ERC Guidelines 2021, RCUK BLS Guidance 2021, ILCOR 2020 CoSTR,
 *            Anaphylaxis Campaign UK / World Allergy Organisation Position Paper 2020.
 *
 * These questions are designed for educational simulation only. They do not constitute
 * clinical advice and have not been validated in a real-emergency setting.
 * Medical review by a qualified clinician is required before deployment.
 */
object QuizQuestionBank {

    // ── Heart Attack Recognition ──────────────────────────────────────────────

    private val heartAttackQuestions = listOf(

        QuizQuestion(
            id = "HA_001",
            category = QuizCategory.HEART_ATTACK,
            difficulty = QuizDifficulty.BEGINNER,
            type = QuestionType.SINGLE_CHOICE,
            scenario = "A 55-year-old man in a shopping centre presses his hand to his chest. He describes a crushing pressure that spreads to his left arm. He looks pale and sweaty.",
            questionText = "What is the most appropriate immediate action?",
            options = listOf(
                QuizOption("A", "Ask him to sit down and wait to see if it passes.", "Waiting without calling for help is dangerous. Symptoms of a possible cardiac event require immediate emergency contact.", isSafeChoice = false),
                QuizOption("B", "Call 112 immediately and stay with him.", "Correct. Call emergency services without delay and stay with the person. Speed of response is critical.", isSafeChoice = true),
                QuizOption("C", "Offer him water and a paracetamol tablet.", "Paracetamol is not a treatment for cardiac events. Delaying emergency contact is dangerous.", isSafeChoice = false),
                QuizOption("D", "Ask him to walk to a nearby pharmacy.", "Physical exertion during a possible cardiac event can be harmful. Emergency services must be called.", isSafeChoice = false)
            ),
            correctOptionIds = setOf("B"),
            skillTags = listOf(QuizSkillTag.EMERGENCY_ESCALATION, QuizSkillTag.SYMPTOM_RECOGNITION),
            misconceptionTested = "Waiting to see if chest pain passes before calling for help",
            learningObjective = "Identify classic cardiac symptoms and escalate to emergency services without delay",
            isCriticalSafety = true
        ),

        QuizQuestion(
            id = "HA_002",
            category = QuizCategory.HEART_ATTACK,
            difficulty = QuizDifficulty.INTERMEDIATE,
            type = QuestionType.SINGLE_CHOICE,
            scenario = "A 48-year-old woman reports unexplained jaw pain, shortness of breath and nausea. She has no chest pain and initially attributes her symptoms to stress.",
            questionText = "What should you advise?",
            options = listOf(
                QuizOption("A", "Agree that it is probably stress and suggest rest.", "Attributing these symptoms to stress without medical evaluation is dangerous — they can be atypical cardiac signs.", isSafeChoice = false),
                QuizOption("B", "Recommend water and lying down quietly.", "This does not address the possibility of a cardiac event and causes dangerous delay.", isSafeChoice = false),
                QuizOption("C", "Treat these as potential cardiac warning signs and call 112.", "Correct. Jaw pain, breathlessness and nausea can be atypical presentations of a cardiac event. Emergency services should be called.", isSafeChoice = true),
                QuizOption("D", "Suggest she take antacids, as it may be indigestion.", "While symptoms can mimic indigestion, treating them as such without evaluation is dangerous. The safest approach is to call 112.", isSafeChoice = false)
            ),
            correctOptionIds = setOf("C"),
            skillTags = listOf(QuizSkillTag.ATYPICAL_PRESENTATION, QuizSkillTag.SYMPTOM_RECOGNITION, QuizSkillTag.EMERGENCY_ESCALATION),
            misconceptionTested = "Cardiac events always present with classic chest pain",
            learningObjective = "Recognise atypical cardiac presentations and escalate appropriately",
            isCriticalSafety = true
        ),

        QuizQuestion(
            id = "HA_003",
            category = QuizCategory.HEART_ATTACK,
            difficulty = QuizDifficulty.INTERMEDIATE,
            type = QuestionType.SINGLE_CHOICE,
            scenario = "Your colleague develops discomfort in her upper back during a meeting. She seems slightly breathless but says she otherwise feels fine and dismisses her symptoms.",
            questionText = "Which response is most appropriate?",
            options = listOf(
                QuizOption("A", "Reassure her that the symptoms will likely pass.", "Reassurance without evaluation is inappropriate when new symptoms of possible cardiac origin are present.", isSafeChoice = false),
                QuizOption("B", "Ask her to wait 30 minutes before deciding what to do.", "A 30-minute delay in the setting of possible cardiac symptoms is dangerous.", isSafeChoice = false),
                QuizOption("C", "Take the symptoms seriously and encourage calling 112 without delay.", "Correct. Back discomfort with breathlessness can represent an atypical cardiac presentation. Encourage emergency contact even if the person minimises symptoms.", isSafeChoice = true),
                QuizOption("D", "Advise her to drive herself to the nearest clinic.", "Driving during a possible cardiac event is unsafe. Emergency services should be called.", isSafeChoice = false)
            ),
            correctOptionIds = setOf("C"),
            skillTags = listOf(QuizSkillTag.ATYPICAL_PRESENTATION, QuizSkillTag.EMERGENCY_ESCALATION),
            misconceptionTested = "Self-dismissal of symptoms is a reliable indicator that a condition is not serious",
            learningObjective = "Recognise the risk of symptom minimisation and act promptly"
        ),

        QuizQuestion(
            id = "HA_004",
            category = QuizCategory.HEART_ATTACK,
            difficulty = QuizDifficulty.BEGINNER,
            type = QuestionType.SINGLE_CHOICE,
            scenario = "A 60-year-old man reports that severe chest pain started 10 minutes ago. The pain has now mostly subsided and he says he feels much better and does not want to bother anyone.",
            questionText = "What is the safest course of action?",
            options = listOf(
                QuizOption("A", "Agree that he seems fine now and let him continue his day.", "Symptom resolution does not confirm safety. Many cardiac events are preceded by episodes of pain that temporarily resolve.", isSafeChoice = false),
                QuizOption("B", "Strongly encourage him to be evaluated by emergency services even though the pain has eased.", "Correct. Transient chest pain that has resolved still requires emergency evaluation. This presentation is consistent with an unstable cardiac event.", isSafeChoice = true),
                QuizOption("C", "Suggest he rest for one hour and see if the pain returns.", "Delaying evaluation for an hour is dangerous. Emergency services should be called without waiting.", isSafeChoice = false),
                QuizOption("D", "Offer him an aspirin and monitor him here.", "Aspirin should only be given on dispatcher instruction. Avoiding emergency contact is dangerous regardless.", isSafeChoice = false)
            ),
            correctOptionIds = setOf("B"),
            skillTags = listOf(QuizSkillTag.EMERGENCY_ESCALATION, QuizSkillTag.DANGEROUS_DELAY),
            misconceptionTested = "Symptoms resolving means the danger has passed",
            learningObjective = "Understand that resolved chest pain still requires emergency evaluation",
            isCriticalSafety = true
        ),

        QuizQuestion(
            id = "HA_005",
            category = QuizCategory.HEART_ATTACK,
            difficulty = QuizDifficulty.ADVANCED,
            type = QuestionType.SINGLE_CHOICE,
            scenario = "A 52-year-old woman with known diabetes feels unusually fatigued, mildly short of breath and vaguely unwell for several hours. She has no chest pain.",
            questionText = "Regarding possible cardiac causes, what is the most appropriate response?",
            options = listOf(
                QuizOption("A", "The absence of chest pain makes a cardiac cause unlikely; no action needed.", "Absence of chest pain does not rule out a cardiac event, especially in people with diabetes who are at risk of silent or atypical presentations.", isSafeChoice = false),
                QuizOption("B", "Monitor symptoms and advise seeing a doctor the following day.", "An overnight delay for new symptoms of possible cardiac origin is dangerous.", isSafeChoice = false),
                QuizOption("C", "Recognise that diabetes increases risk of atypical cardiac presentation; seek emergency evaluation.", "Correct. Diabetes is associated with atypical and sometimes painless cardiac presentations. These symptoms require urgent medical evaluation.", isSafeChoice = true),
                QuizOption("D", "Treat for low blood sugar with a sweet drink and reassess in 30 minutes.", "Although this may address a hypoglycaemic episode, it delays evaluation of potential cardiac causes and is not the safest first priority when these broader symptoms are present.", isSafeChoice = false)
            ),
            correctOptionIds = setOf("C"),
            skillTags = listOf(QuizSkillTag.ATYPICAL_PRESENTATION, QuizSkillTag.SYMPTOM_RECOGNITION, QuizSkillTag.EMERGENCY_ESCALATION),
            misconceptionTested = "Chest pain is always the primary symptom of a cardiac event",
            learningObjective = "Understand that diabetes increases risk of silent or atypical cardiac events",
            isCriticalSafety = true
        ),

        QuizQuestion(
            id = "HA_006",
            category = QuizCategory.HEART_ATTACK,
            difficulty = QuizDifficulty.INTERMEDIATE,
            type = QuestionType.SINGLE_CHOICE,
            scenario = "A man in a restaurant is conscious with severe chest pain. You have already called 112. He asks whether he should take aspirin from the restaurant first-aid kit.",
            questionText = "What should you do about the aspirin?",
            options = listOf(
                QuizOption("A", "Give him 300 mg aspirin immediately from the first-aid kit.", "Aspirin may be indicated but should only be given following dispatcher instruction, which accounts for contraindications.", isSafeChoice = false),
                QuizOption("B", "Follow the instructions of the 112 emergency dispatcher regarding aspirin.", "Correct. Emergency dispatchers are trained to advise on aspirin based on the person's specific situation and contraindications.", isSafeChoice = true),
                QuizOption("C", "Aspirin has no role here; do not mention it.", "Aspirin may be indicated, but the decision should be made by or through emergency services.", isSafeChoice = false),
                QuizOption("D", "Give him any available painkiller to ease the pain.", "Non-aspirin painkillers are not indicated and some (e.g. ibuprofen) may be harmful in this setting.", isSafeChoice = false)
            ),
            correctOptionIds = setOf("B"),
            skillTags = listOf(QuizSkillTag.APPROPRIATE_ACTION, QuizSkillTag.EMERGENCY_ESCALATION),
            misconceptionTested = "Any available medication should be given to reduce a person's discomfort",
            learningObjective = "Follow dispatcher instructions regarding aspirin rather than acting unilaterally"
        )
    )

    // ── CPR & Cardiac Arrest ──────────────────────────────────────────────────

    private val cprQuestions = listOf(

        QuizQuestion(
            id = "CPR_001",
            category = QuizCategory.CPR_CARDIAC_ARREST,
            difficulty = QuizDifficulty.BEGINNER,
            type = QuestionType.SINGLE_CHOICE,
            scenario = "You find a 40-year-old woman lying motionless on the pavement. You tap her shoulder firmly and call her name. She does not respond.",
            questionText = "What is your next priority?",
            options = listOf(
                QuizOption("A", "Begin chest compressions immediately before doing anything else.", "Starting compressions before assessing breathing and activating emergency services is not the recommended sequence.", isSafeChoice = false),
                QuizOption("B", "Shout for help and check for normal breathing while calling or asking someone to call 112.", "Correct. Shouting for help and checking breathing while activating emergency services (or directing a bystander to do so) are the priority steps after confirming unresponsiveness.", isSafeChoice = true),
                QuizOption("C", "Place her in the recovery position.", "The recovery position is for an unconscious but breathing person. First check whether she is breathing.", isSafeChoice = false),
                QuizOption("D", "Wait for other bystanders to arrive before taking action.", "Waiting delays potentially life-saving intervention. Act immediately.", isSafeChoice = false)
            ),
            correctOptionIds = setOf("B"),
            skillTags = listOf(QuizSkillTag.CARDIAC_ARREST_RECOGNITION, QuizSkillTag.CPR_DECISION, QuizSkillTag.EMERGENCY_ESCALATION),
            misconceptionTested = "Compressions should start before any assessment",
            learningObjective = "Follow the correct sequence: check response, shout for help, check breathing, activate emergency services"
        ),

        QuizQuestion(
            id = "CPR_002",
            category = QuizCategory.CPR_CARDIAC_ARREST,
            difficulty = QuizDifficulty.INTERMEDIATE,
            type = QuestionType.SINGLE_CHOICE,
            scenario = "You find an adult man unresponsive. When you tilt his head back and look, listen and feel for breathing, you notice infrequent gasps with long pauses between them.",
            questionText = "What does this breathing pattern indicate?",
            options = listOf(
                QuizOption("A", "Normal breathing — place him in the recovery position.", "Infrequent gasping is not normal breathing. Treating it as normal breathing risks fatal delay in starting CPR.", isSafeChoice = false),
                QuizOption("B", "Agonal breathing — a sign of cardiac arrest. Begin CPR immediately.", "Correct. Agonal breathing (occasional gasps) is a recognised sign of cardiac arrest. Do not be reassured by it — start CPR and call 112.", isSafeChoice = true),
                QuizOption("C", "He is in deep sleep. Wait 2 minutes and reassess.", "An unresponsive adult with gasping breaths requires immediate action, not a wait-and-see approach.", isSafeChoice = false),
                QuizOption("D", "Airway obstruction — begin abdominal thrusts.", "Abdominal thrusts are for a choking person who is conscious. An unresponsive person with agonal breaths needs CPR.", isSafeChoice = false)
            ),
            correctOptionIds = setOf("B"),
            skillTags = listOf(QuizSkillTag.AGONAL_BREATHING, QuizSkillTag.CPR_DECISION),
            misconceptionTested = "Agonal gasping means the person is still breathing adequately",
            learningObjective = "Recognise agonal breathing as a sign of cardiac arrest requiring immediate CPR",
            isCriticalSafety = true
        ),

        QuizQuestion(
            id = "CPR_003",
            category = QuizCategory.CPR_CARDIAC_ARREST,
            difficulty = QuizDifficulty.BEGINNER,
            type = QuestionType.SINGLE_CHOICE,
            scenario = "You are alone with an adult who is unresponsive and not breathing normally. You have no mobile phone.",
            questionText = "What should you do?",
            options = listOf(
                QuizOption("A", "Shout for help while starting CPR. If no one responds, briefly stop after 1 minute to find a phone and call 112.", "Correct. Shouting for help first is ideal. If you are truly alone, current guidance (ERC/RCUK) suggests starting CPR and — for adults — briefly leaving after approximately 1 minute to call for help if no one responds.", isSafeChoice = true),
                QuizOption("B", "Continue CPR indefinitely without ever calling 112, as interrupting CPR is too risky.", "Without emergency services and a defibrillator, survival is significantly reduced. Calling 112 and obtaining an AED are essential.", isSafeChoice = false),
                QuizOption("C", "Leave immediately to find a phone and call 112 before beginning CPR.", "For adults, starting compressions before leaving is preferable where possible. For children, CPR for 1 minute before calling is advised.", isSafeChoice = false),
                QuizOption("D", "Stay next to the person and shout until help arrives.", "Staying without attempting CPR or calling for help is not appropriate.", isSafeChoice = false)
            ),
            correctOptionIds = setOf("A"),
            skillTags = listOf(QuizSkillTag.CPR_DECISION, QuizSkillTag.EMERGENCY_ESCALATION),
            misconceptionTested = "CPR should never be interrupted for any reason",
            learningObjective = "Balance starting CPR with activating emergency services when alone"
        ),

        QuizQuestion(
            id = "CPR_004",
            category = QuizCategory.CPR_CARDIAC_ARREST,
            difficulty = QuizDifficulty.INTERMEDIATE,
            type = QuestionType.SINGLE_CHOICE,
            scenario = "You are performing chest compressions on an unresponsive adult. Another bystander arrives with an AED and sets it up.",
            questionText = "What should happen next?",
            options = listOf(
                QuizOption("A", "Continue compressions and ignore the AED for now to avoid interruption.", "AED use should not be delayed. Early defibrillation significantly improves survival.", isSafeChoice = false),
                QuizOption("B", "Pause compressions as directed by the AED voice prompts, allow analysis, and deliver a shock if advised.", "Correct. Follow the AED voice prompts. Pause compressions for analysis and shock delivery, then resume compressions immediately afterwards.", isSafeChoice = true),
                QuizOption("C", "Tell the other person to wait until you complete a full 2-minute CPR cycle.", "Delaying AED use reduces the chance of survival. Use it as soon as it is available.", isSafeChoice = false),
                QuizOption("D", "Attach the AED pads while continuing compressions to avoid any pause.", "Attempting to apply pads while compressing risks harm and error. Follow the device's instructions.", isSafeChoice = false)
            ),
            correctOptionIds = setOf("B"),
            skillTags = listOf(QuizSkillTag.AED_USE, QuizSkillTag.CPR_DECISION),
            misconceptionTested = "CPR should not be interrupted for an AED",
            learningObjective = "Understand the priority of AED use and how to integrate it with CPR"
        ),

        QuizQuestion(
            id = "CPR_005",
            category = QuizCategory.CPR_CARDIAC_ARREST,
            difficulty = QuizDifficulty.INTERMEDIATE,
            type = QuestionType.SINGLE_CHOICE,
            scenario = "You are mid-way through CPR on a collapsed adult. Emergency services arrive and take over care.",
            questionText = "What is the most appropriate action?",
            options = listOf(
                QuizOption("A", "Continue compressions alongside the emergency team for extra effectiveness.", "Double compressions are not beneficial and may interfere with professional care.", isSafeChoice = false),
                QuizOption("B", "Step back, provide a brief handover of events, and follow the team's instructions.", "Correct. Hand over to emergency services with a brief account of what happened and what was done. This is the correct and safest response.", isSafeChoice = true),
                QuizOption("C", "Insist on completing 2 more minutes of CPR before handing over.", "The emergency team should take over immediately. Delaying handover is inappropriate.", isSafeChoice = false),
                QuizOption("D", "Continue CPR until the person regains consciousness.", "Decisions about when to stop CPR are made by trained emergency personnel, not bystanders.", isSafeChoice = false)
            ),
            correctOptionIds = setOf("B"),
            skillTags = listOf(QuizSkillTag.HANDOVER, QuizSkillTag.CPR_DECISION),
            misconceptionTested = "Bystanders should continue CPR even after emergency services arrive",
            learningObjective = "Understand correct handover to emergency services"
        ),

        QuizQuestion(
            id = "CPR_006",
            category = QuizCategory.CPR_CARDIAC_ARREST,
            difficulty = QuizDifficulty.BEGINNER,
            type = QuestionType.SINGLE_CHOICE,
            scenario = "You are about to begin chest compressions on an unresponsive adult. You need to position your hands correctly.",
            questionText = "Where should you place your hands for adult chest compressions?",
            options = listOf(
                QuizOption("A", "On the upper chest between the shoulder blades.", "Compressing the upper chest is ineffective and may cause injury.", isSafeChoice = false),
                QuizOption("B", "On the lower half of the breastbone (sternum), centre of the chest.", "Correct. Place the heel of your hand on the lower half of the sternum. This is the recommended hand position for adult CPR.", isSafeChoice = true),
                QuizOption("C", "On the side of the ribcage for broader pressure coverage.", "Compressing the ribcage sides is ineffective and may cause rib fractures.", isSafeChoice = false),
                QuizOption("D", "Over the heart, on the left side of the chest.", "The sternum (centre of the chest) is the correct compression point, not the left side.", isSafeChoice = false)
            ),
            correctOptionIds = setOf("B"),
            skillTags = listOf(QuizSkillTag.CPR_TECHNIQUE),
            misconceptionTested = "Compressions should be delivered over the heart on the left side",
            learningObjective = "Correctly identify the hand placement for adult chest compressions"
        ),

        QuizQuestion(
            id = "CPR_007",
            category = QuizCategory.CPR_CARDIAC_ARREST,
            difficulty = QuizDifficulty.ADVANCED,
            type = QuestionType.SINGLE_CHOICE,
            scenario = "A bystander has been doing CPR for 2 minutes and is tiring. Another bystander offers to swap.",
            questionText = "Which approach best maintains CPR quality?",
            options = listOf(
                QuizOption("A", "Continue with the same person to avoid any interruption to compressions.", "Compression quality drops significantly with fatigue. Regular swaps with minimal pause improve outcomes.", isSafeChoice = false),
                QuizOption("B", "Swap with minimal interruption, aiming to maintain 100–120 compressions per minute.", "Correct. Swapping rescuers minimises fatigue-related quality loss. The swap should be fast to minimise interruption.", isSafeChoice = true),
                QuizOption("C", "Pause CPR completely while the handover is organised.", "Pausing CPR unnecessarily reduces survival. The swap must be rapid.", isSafeChoice = false),
                QuizOption("D", "Switch rescuers every 30 seconds to ensure fresh compressions throughout.", "Swapping every 30 seconds creates too many interruptions and is impractical.", isSafeChoice = false)
            ),
            correctOptionIds = setOf("B"),
            skillTags = listOf(QuizSkillTag.CPR_QUALITY, QuizSkillTag.CPR_TECHNIQUE),
            misconceptionTested = "Keeping the same rescuer avoids all interruptions",
            learningObjective = "Understand the role of rescuer rotation in maintaining CPR quality"
        )
    )

    // ── Allergic Reactions ────────────────────────────────────────────────────

    private val allergyQuestions = listOf(

        QuizQuestion(
            id = "ALLERGY_001",
            category = QuizCategory.ALLERGIC_REACTION,
            difficulty = QuizDifficulty.BEGINNER,
            type = QuestionType.SINGLE_CHOICE,
            scenario = "A teenager at a school event develops hives (raised, itchy welts) across her arms after eating. She says she feels itchy but otherwise fine.",
            questionText = "How should you respond?",
            options = listOf(
                QuizOption("A", "Give antihistamine tablets and continue the event — this is clearly a mild reaction.", "Antihistamines alone are not sufficient for anaphylaxis and labelling the reaction 'mild' without assessment is premature. Symptoms can progress rapidly.", isSafeChoice = false),
                QuizOption("B", "Monitor closely for signs of breathing difficulty, throat swelling or collapse; be ready to call 112 if symptoms worsen.", "Correct. Hives may precede anaphylaxis. Close monitoring for systemic symptoms and preparedness to escalate are the safest approach.", isSafeChoice = true),
                QuizOption("C", "Ask her to walk to the school nurse unsupervised.", "A person who may be in early anaphylaxis should not be sent alone — they may deteriorate rapidly.", isSafeChoice = false),
                QuizOption("D", "Reassure her that hives alone are not serious and no action is needed.", "This false reassurance could lead to delayed treatment if symptoms progress.", isSafeChoice = false)
            ),
            correctOptionIds = setOf("B"),
            skillTags = listOf(QuizSkillTag.ANAPHYLAXIS_RECOGNITION, QuizSkillTag.MONITORING),
            misconceptionTested = "Hives without immediate severe symptoms mean a reaction is mild and will not progress",
            learningObjective = "Understand that localised allergic symptoms can progress to anaphylaxis and require close monitoring"
        ),

        QuizQuestion(
            id = "ALLERGY_002",
            category = QuizCategory.ALLERGIC_REACTION,
            difficulty = QuizDifficulty.INTERMEDIATE,
            type = QuestionType.SINGLE_CHOICE,
            scenario = "Minutes after eating peanuts, a 25-year-old man develops swelling of his lips and tongue, a hoarse voice, and reports that his throat feels tight. He is conscious but distressed.",
            questionText = "Which response is most appropriate?",
            options = listOf(
                QuizOption("A", "Give him water and ask him to breathe slowly.", "This does not treat anaphylaxis and causes dangerous delay. Airway involvement requires immediate action.", isSafeChoice = false),
                QuizOption("B", "Call 112 immediately. If he has a prescribed epinephrine auto-injector, prepare to administer it.", "Correct. Throat tightness, hoarse voice and tongue swelling indicate airway compromise — a medical emergency. Call 112 and administer prescribed epinephrine.", isSafeChoice = true),
                QuizOption("C", "Give antihistamines and wait 15 minutes to see if the reaction resolves.", "Antihistamines act too slowly for anaphylaxis with airway involvement. A 15-minute delay is dangerous.", isSafeChoice = false),
                QuizOption("D", "Have him sit quietly and see if the throat tightness eases.", "Waiting for airway symptoms to ease without intervention is extremely dangerous.", isSafeChoice = false)
            ),
            correctOptionIds = setOf("B"),
            skillTags = listOf(QuizSkillTag.ANAPHYLAXIS_RECOGNITION, QuizSkillTag.EMERGENCY_ESCALATION, QuizSkillTag.EPIPEN_DECISION),
            misconceptionTested = "Antihistamines can treat anaphylaxis with airway involvement",
            learningObjective = "Recognise anaphylaxis with airway involvement and respond immediately with epinephrine and 112",
            isCriticalSafety = true
        ),

        QuizQuestion(
            id = "ALLERGY_003",
            category = QuizCategory.ALLERGIC_REACTION,
            difficulty = QuizDifficulty.INTERMEDIATE,
            type = QuestionType.SINGLE_CHOICE,
            scenario = "You are about to use an epinephrine auto-injector on a conscious adult showing signs of anaphylaxis. A prescribed auto-injector is available.",
            questionText = "Where should the auto-injector be administered?",
            options = listOf(
                QuizOption("A", "Into the upper arm muscle (deltoid).", "The outer mid-thigh is the recommended injection site for an epinephrine auto-injector.", isSafeChoice = false),
                QuizOption("B", "Into the outer mid-thigh.", "Correct. The standard injection site is the outer mid-thigh. Injection can be given through clothing if necessary.", isSafeChoice = true),
                QuizOption("C", "Into the abdomen.", "The abdomen is not the recommended site for an auto-injector.", isSafeChoice = false),
                QuizOption("D", "Under the tongue (sublingual).", "Epinephrine auto-injectors are not designed for sublingual administration.", isSafeChoice = false)
            ),
            correctOptionIds = setOf("B"),
            skillTags = listOf(QuizSkillTag.EPIPEN_TECHNIQUE),
            misconceptionTested = "Epinephrine auto-injectors are given in the arm like a standard injection",
            learningObjective = "Identify the correct injection site for an epinephrine auto-injector"
        ),

        QuizQuestion(
            id = "ALLERGY_004",
            category = QuizCategory.ALLERGIC_REACTION,
            difficulty = QuizDifficulty.INTERMEDIATE,
            type = QuestionType.SINGLE_CHOICE,
            scenario = "After epinephrine has been given, the person's breathing improves slightly. They say they feel a bit better and ask to sit up.",
            questionText = "What position should the person be in while waiting for emergency services?",
            options = listOf(
                QuizOption("A", "Allow them to stand up and walk around slowly to aid circulation.", "Standing or walking during anaphylaxis is dangerous and can cause a sudden collapse due to blood pressure changes.", isSafeChoice = false),
                QuizOption("B", "Help them to sit in a comfortable chair.", "Sitting in a chair does not adequately protect against circulatory collapse. Lying flat with legs raised is preferred unless breathing is difficult.", isSafeChoice = false),
                QuizOption("C", "Keep them lying flat with legs raised, unless they have breathing difficulty, in which case a supported sitting position is acceptable.", "Correct. Lying flat with raised legs improves venous return. If breathing is compromised, a supported sitting position is acceptable. Never allow them to stand.", isSafeChoice = true),
                QuizOption("D", "Place them in the recovery position.", "The recovery position is for an unconscious but breathing person. This person is conscious — lying flat with legs raised is appropriate.", isSafeChoice = false)
            ),
            correctOptionIds = setOf("C"),
            skillTags = listOf(QuizSkillTag.ANAPHYLAXIS_POSITION, QuizSkillTag.MONITORING),
            misconceptionTested = "Allowing a person to sit up after epinephrine is safe once they feel better",
            learningObjective = "Apply correct positioning guidance for a conscious person after epinephrine"
        ),

        QuizQuestion(
            id = "ALLERGY_005",
            category = QuizCategory.ALLERGIC_REACTION,
            difficulty = QuizDifficulty.ADVANCED,
            type = QuestionType.SINGLE_CHOICE,
            scenario = "A person with known anaphylaxis risk receives epinephrine. Fifteen minutes later, symptoms return and worsen despite the earlier improvement.",
            questionText = "What does this indicate and what action is needed?",
            options = listOf(
                QuizOption("A", "The epinephrine worked. Symptom return is a normal part of recovery.", "Symptom return after apparent improvement is not normal — it may indicate a biphasic reaction requiring further intervention.", isSafeChoice = false),
                QuizOption("B", "A biphasic reaction may be occurring. A second epinephrine dose may be needed, and emergency services must attend.", "Correct. A biphasic anaphylactic reaction can occur hours after initial treatment. Emergency services must be present and a second dose of epinephrine may be required.", isSafeChoice = true),
                QuizOption("C", "The initial epinephrine dose was insufficient. Double the next dose.", "Epinephrine auto-injector doses are fixed. Administering a second auto-injector (if available and according to prescription instructions) may be appropriate — not doubling a dose.", isSafeChoice = false),
                QuizOption("D", "No further treatment is needed since 112 has already been called.", "Emergency services attendance is essential, but symptom recurrence may also require a second epinephrine dose before they arrive.", isSafeChoice = false)
            ),
            correctOptionIds = setOf("B"),
            skillTags = listOf(QuizSkillTag.BIPHASIC_REACTION, QuizSkillTag.EMERGENCY_ESCALATION, QuizSkillTag.MONITORING),
            misconceptionTested = "Improvement after epinephrine means the reaction is over",
            learningObjective = "Understand the risk of biphasic anaphylaxis and the need for ongoing monitoring and emergency care",
            isCriticalSafety = true
        ),

        QuizQuestion(
            id = "ALLERGY_006",
            category = QuizCategory.ALLERGIC_REACTION,
            difficulty = QuizDifficulty.BEGINNER,
            type = QuestionType.SINGLE_CHOICE,
            scenario = "A child at a birthday party is wheezing and flushed after eating cake. His mother says he has a known bee sting allergy, not a food allergy, and she has his prescribed EpiPen.",
            questionText = "Should the EpiPen be used?",
            options = listOf(
                QuizOption("A", "No — the EpiPen is prescribed for bee stings, not food reactions.", "Epinephrine is the treatment for anaphylaxis regardless of the trigger. The specific allergen does not change the treatment decision.", isSafeChoice = false),
                QuizOption("B", "Yes — if he is showing signs consistent with anaphylaxis, use epinephrine and call 112.", "Correct. Anaphylaxis from any cause should be treated with epinephrine if it is available. Call 112 immediately.", isSafeChoice = true),
                QuizOption("C", "Wait 5 minutes to see if the wheezing resolves before deciding.", "A 5-minute delay in anaphylaxis with respiratory involvement is dangerous.", isSafeChoice = false),
                QuizOption("D", "Only use the EpiPen if the child loses consciousness.", "Waiting for loss of consciousness before using epinephrine is far too late. The EpiPen should be used at the first signs of anaphylaxis.", isSafeChoice = false)
            ),
            correctOptionIds = setOf("B"),
            skillTags = listOf(QuizSkillTag.ANAPHYLAXIS_RECOGNITION, QuizSkillTag.EPIPEN_DECISION),
            misconceptionTested = "An EpiPen prescribed for one allergen should not be used for a different trigger",
            learningObjective = "Understand that epinephrine treats anaphylaxis regardless of trigger, and that the EpiPen should be used at the first signs",
            isCriticalSafety = true
        )
    )

    // ── Public API ────────────────────────────────────────────────────────────

    val allQuestions: List<QuizQuestion> =
        heartAttackQuestions + cprQuestions + allergyQuestions

    fun questionsForCategory(category: QuizCategory): List<QuizQuestion> =
        allQuestions.filter { it.category == category }

    fun questionsForSession(
        category: QuizCategory,
        difficulty: QuizDifficulty,
        maxCount: Int = 6
    ): List<QuizQuestion> {
        val categoryQs = questionsForCategory(category)
        // Primary: matching difficulty. Supplement with adjacent difficulty if needed.
        val matching = categoryQs.filter { it.difficulty == difficulty }
        val supplemented = if (matching.size >= maxCount) matching else {
            val adjacent = when (difficulty) {
                QuizDifficulty.BEGINNER     -> categoryQs.filter { it.difficulty == QuizDifficulty.INTERMEDIATE }
                QuizDifficulty.INTERMEDIATE -> categoryQs.filter { it.difficulty != QuizDifficulty.INTERMEDIATE }
                QuizDifficulty.ADVANCED     -> categoryQs.filter { it.difficulty == QuizDifficulty.INTERMEDIATE }
            }
            (matching + adjacent).distinctBy { it.id }
        }
        return supplemented.take(maxCount)
    }
}

