package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.MockExam
import com.example.data.model.Question
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val TAG = "MockRepository"

class MockRepository(private val context: Context) {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
        context.applicationContext.getString(R.string.firestore_database_id)
    )

    fun observePublishedMocks(): Flow<List<MockExam>> {
        return db.collection("mocks")
            .whereEqualTo("isPublished", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(MockExam::class.java)
            }
            .catch { error ->
                Log.e(TAG, "Error observing published mocks: ${error.message}", error)
                // Fallback to default bundled defence mocks if offline or first launch
                emit(getDefaultMocks().filter { it.isPublished })
            }
            .flowOn(Dispatchers.IO)
    }

    fun observeAllMocksForAdmin(): Flow<List<MockExam>> {
        return db.collection("mocks")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(MockExam::class.java)
            }
            .catch { error ->
                Log.e(TAG, "Error observing admin mocks: ${error.message}", error)
                emit(getDefaultMocks())
            }
            .flowOn(Dispatchers.IO)
    }

    suspend fun getMockById(mockId: String): MockExam? = withContext(Dispatchers.IO) {
        try {
            val doc = db.collection("mocks").document(mockId).get().await()
            if (doc.exists()) {
                doc.toObject(MockExam::class.java)
            } else {
                getDefaultMocks().find { it.id == mockId }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching mock $mockId: ${e.message}", e)
            getDefaultMocks().find { it.id == mockId }
        }
    }

    suspend fun saveMock(mock: MockExam): String = withContext(Dispatchers.IO) {
        val user = Firebase.auth.currentUser
            ?: throw IllegalStateException("Must be signed in as administrator")

        val docRef = if (mock.id.isNotBlank()) {
            db.collection("mocks").document(mock.id)
        } else {
            db.collection("mocks").document()
        }

        val payload = mutableMapOf<String, Any>(
            "title" to mock.title,
            "description" to mock.description,
            "category" to mock.category,
            "durationMinutes" to mock.durationMinutes,
            "totalMarks" to mock.totalMarks,
            "positiveMark" to mock.positiveMark,
            "negativeMark" to mock.negativeMark,
            "isPublished" to mock.isPublished,
            "questionCount" to mock.questions.size,
            "questions" to mock.questions,
            "createdBy" to (mock.createdBy.ifBlank { user.uid }),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        if (mock.id.isBlank()) {
            payload["createdAt"] = FieldValue.serverTimestamp()
        }

        docRef.set(payload).await()
        docRef.id
    }

    suspend fun togglePublishStatus(mockId: String, currentStatus: Boolean) = withContext(Dispatchers.IO) {
        db.collection("mocks").document(mockId)
            .update(
                mapOf(
                    "isPublished" to !currentStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
    }

    suspend fun deleteMock(mockId: String) = withContext(Dispatchers.IO) {
        db.collection("mocks").document(mockId).delete().await()
    }

    suspend fun seedInitialMocksIfEmpty() = withContext(Dispatchers.IO) {
        try {
            val user = Firebase.auth.currentUser ?: return@withContext
            if (user.email?.trim()?.equals(ADMIN_EMAIL, ignoreCase = true) != true) return@withContext

            val existing = db.collection("mocks").limit(1).get().await()
            if (existing.isEmpty) {
                Log.d(TAG, "Seeding high-yield defence mock exams...")
                for (mock in getDefaultMocks()) {
                    saveMock(mock)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Skipping mock seeding: ${e.message}")
        }
    }

    companion object {
        fun getDefaultMocks(): List<MockExam> {
            return listOf(
                MockExam(
                    id = "iaf-group-x-full-01",
                    title = "IAF Group X (Technical) - Full Mock 01",
                    description = "Comprehensive All-India Mock for Indian Air Force Group X Aspirants covering Physics, Mathematics, and English as per official Agniveervayu syllabus.",
                    category = "Air Force Group X",
                    durationMinutes = 60,
                    totalMarks = 70.0,
                    positiveMark = 1.0,
                    negativeMark = 0.25,
                    isPublished = true,
                    questionCount = 10,
                    createdBy = "admin",
                    questions = listOf(
                        Question(
                            id = "q1",
                            number = 1,
                            section = "Physics",
                            textEnglish = "What is the dimensional formula of universal gravitational constant (G)?",
                            textHindi = "सार्वत्रिक गुरुत्वाकर्षण नियतांक (G) का विमीय सूत्र क्या है?",
                            optionsEnglish = listOf("[M⁻¹ L³ T⁻²]", "[M¹ L² T⁻¹]", "[M⁻¹ L² T⁻²]", "[M⁰ L³ T⁻²]"),
                            optionsHindi = listOf("[M⁻¹ L³ T⁻²]", "[M¹ L² T⁻¹]", "[M⁻¹ L² T⁻²]", "[M⁰ L³ T⁻²]"),
                            correctOptionIndex = 0,
                            explanationEnglish = "From Newton's law of gravitation, F = G(m1 m2)/r². Therefore G = Fr²/(m1 m2) = [M L T⁻²][L²]/[M²] = [M⁻¹ L³ T⁻²].",
                            explanationHindi = "न्यूटन के गुरुत्वाकर्षण नियम से, F = G(m1 m2)/r²। अतः G = [M⁻¹ L³ T⁻²] प्राप्त होता है।"
                        ).toMap(),
                        Question(
                            id = "q2",
                            number = 2,
                            section = "Physics",
                            textEnglish = "A projectile is thrown at an angle of 45° with velocity 20 m/s. What is its maximum height? (Take g = 10 m/s²)",
                            textHindi = "एक प्रक्षेप्य को 45° के कोण पर 20 m/s के वेग से फेंका जाता है। इसकी अधिकतम ऊंचाई क्या होगी? (g = 10 m/s²)",
                            optionsEnglish = listOf("10 m", "20 m", "5 m", "15 m"),
                            optionsHindi = listOf("10 मीटर", "20 मीटर", "5 मीटर", "15 मीटर"),
                            correctOptionIndex = 0,
                            explanationEnglish = "H_max = (u² sin²θ) / (2g) = (20² * sin²45°) / (2 * 10) = (400 * 0.5) / 20 = 200 / 20 = 10 m.",
                            explanationHindi = "H_max = (u² sin²θ) / (2g) = (400 * 1/2) / 20 = 10 मीटर।"
                        ).toMap(),
                        Question(
                            id = "q3",
                            number = 3,
                            section = "Mathematics",
                            textEnglish = "If A is a square matrix of order 3 and |A| = 4, then find |adj(A)|.",
                            textHindi = "यदि A कोटि 3 का एक वर्ग आव्यूह है और |A| = 4 है, तो |adj(A)| का मान ज्ञात कीजिए।",
                            optionsEnglish = listOf("16", "64", "4", "12"),
                            optionsHindi = listOf("16", "64", "4", "12"),
                            correctOptionIndex = 0,
                            explanationEnglish = "For a square matrix of order n, |adj(A)| = |A|^(n - 1). Here n = 3, so |adj(A)| = 4^(3 - 1) = 4² = 16.",
                            explanationHindi = "कोटि n के वर्ग आव्यूह के लिए, |adj(A)| = |A|^(n - 1) = 4² = 16 होता है।"
                        ).toMap(),
                        Question(
                            id = "q4",
                            number = 4,
                            section = "Mathematics",
                            textEnglish = "Evaluate: ∫ (sec²x / (1 + tan x)) dx.",
                            textHindi = "मान ज्ञात कीजिए: ∫ (sec²x / (1 + tan x)) dx.",
                            optionsEnglish = listOf("log|1 + tan x| + C", "tan x + C", "sec x + C", "log|sec x| + C"),
                            optionsHindi = listOf("log|1 + tan x| + C", "tan x + C", "sec x + C", "log|sec x| + C"),
                            correctOptionIndex = 0,
                            explanationEnglish = "Let t = 1 + tan x, then dt = sec²x dx. The integral becomes ∫ (1/t) dt = log|t| + C = log|1 + tan x| + C.",
                            explanationHindi = "t = 1 + tan x मानकर अवकलन करने पर dt = sec²x dx मिलता है, अतः उत्तर log|1 + tan x| + C है।"
                        ).toMap(),
                        Question(
                            id = "q5",
                            number = 5,
                            section = "English",
                            textEnglish = "Choose the correct synonym for 'VALOR':",
                            textHindi = "'VALOR' का सही पर्यायवाची शब्द चुनिए:",
                            optionsEnglish = listOf("Bravery", "Cowardice", "Hesitation", "Fear"),
                            optionsHindi = listOf("Bravery (शौर्य/वीरता)", "Cowardice (कायरता)", "Hesitation (संकोच)", "Fear (भय)"),
                            correctOptionIndex = 0,
                            explanationEnglish = "Valor means great courage in the face of danger, especially in battle. Synonym is Bravery.",
                            explanationHindi = "Valor का अर्थ युद्ध में असाधारण वीरता या साहस होता है। इसका सटीक पर्यायवाची Bravery है।"
                        ).toMap(),
                        Question(
                            id = "q6",
                            number = 6,
                            section = "English",
                            textEnglish = "Identify the correctly spelled word:",
                            textHindi = "सही वर्तनी वाले शब्द की पहचान करें:",
                            optionsEnglish = listOf("Lieutenant", "Leutenant", "Lieutenent", "Leftenant"),
                            optionsHindi = listOf("Lieutenant", "Leutenant", "Lieutenent", "Leftenant"),
                            correctOptionIndex = 0,
                            explanationEnglish = "The correct spelling of the military commission rank is 'Lieutenant'.",
                            explanationHindi = "सैन्य अधिकारी रैंक की सही वर्तनी 'Lieutenant' है।"
                        ).toMap(),
                        Question(
                            id = "q7",
                            number = 7,
                            section = "Physics",
                            textEnglish = "The work done in moving a charge across an equipotential surface is:",
                            textHindi = "एक समविभव पृष्ठ पर किसी आवेश को ले जाने में किया गया कार्य क्या होता है?",
                            optionsEnglish = listOf("Zero", "Positive", "Negative", "Infinite"),
                            optionsHindi = listOf("शून्य", "धनात्मक", "ऋणात्मक", "अनंत"),
                            correctOptionIndex = 0,
                            explanationEnglish = "Work done W = q * ΔV. On an equipotential surface, ΔV = 0, so work done is always zero.",
                            explanationHindi = "समविभव पृष्ठ पर विभवांतर ΔV = 0 होता है, अतः W = q * ΔV = 0 (शून्य)।"
                        ).toMap(),
                        Question(
                            id = "q8",
                            number = 8,
                            section = "Mathematics",
                            textEnglish = "Find the derivative of sin(log x) with respect to x:",
                            textHindi = "sin(log x) का x के सापेक्ष अवकलन ज्ञात कीजिए:",
                            optionsEnglish = listOf("cos(log x) / x", "-cos(log x) / x", "cos(1/x)", "sin(1/x)"),
                            optionsHindi = listOf("cos(log x) / x", "-cos(log x) / x", "cos(1/x)", "sin(1/x)"),
                            correctOptionIndex = 0,
                            explanationEnglish = "Using chain rule: d/dx [sin(log x)] = cos(log x) * d/dx(log x) = cos(log x) / x.",
                            explanationHindi = "शृंखला नियम से d/dx [sin(log x)] = cos(log x) * (1/x) = cos(log x) / x।"
                        ).toMap(),
                        Question(
                            id = "q9",
                            number = 9,
                            section = "Physics",
                            textEnglish = "Which electromagnetic radiation has the highest frequency?",
                            textHindi = "किस विद्युत चुंबकीय विकिरण की आवृत्ति सर्वाधिक होती है?",
                            optionsEnglish = listOf("Gamma rays", "X-rays", "Ultraviolet rays", "Radio waves"),
                            optionsHindi = listOf("गामा किरणें (Gamma rays)", "एक्स-रे (X-rays)", "पराबैंगनी किरणें", "रेडियो तरंगें"),
                            correctOptionIndex = 0,
                            explanationEnglish = "Gamma rays have the shortest wavelength and hence the highest frequency in the electromagnetic spectrum.",
                            explanationHindi = "विद्युत चुंबकीय स्पेक्ट्रम में गामा किरणों की तरंगदैर्घ्य न्यूनतम तथा आवृत्ति सर्वाधिक होती है।"
                        ).toMap(),
                        Question(
                            id = "q10",
                            number = 10,
                            section = "English",
                            textEnglish = "Change into Indirect Speech: He said to me, 'Where do you live?'",
                            textHindi = "परोक्ष कथन में बदलें: He said to me, 'Where do you live?'",
                            optionsEnglish = listOf(
                                "He asked me where I lived.",
                                "He asked me where did I live.",
                                "He inquired me where I live.",
                                "He asked me where do I live."
                            ),
                            optionsHindi = listOf(
                                "He asked me where I lived.",
                                "He asked me where did I live.",
                                "He inquired me where I live.",
                                "He asked me where do I live."
                            ),
                            correctOptionIndex = 0,
                            explanationEnglish = "In wh-questions, the reported question adopts assertive sentence order (subject + past tense verb). 'where I lived'.",
                            explanationHindi = "प्रश्नवाचक वाक्यों में interrogative order बदलकर assertive order (subject + V2) हो जाता है: 'where I lived'।"
                        ).toMap()
                    )
                ),
                MockExam(
                    id = "iaf-group-y-raga-01",
                    title = "IAF Group Y (Non-Tech) - RAGA & English",
                    description = "Specialized test for Indian Air Force Group Y aspirants containing Reasoning, General Awareness, and English.",
                    category = "Air Force Group Y",
                    durationMinutes = 45,
                    totalMarks = 50.0,
                    positiveMark = 1.0,
                    negativeMark = 0.25,
                    isPublished = true,
                    questionCount = 8,
                    createdBy = "admin",
                    questions = listOf(
                        Question(
                            id = "y1",
                            number = 1,
                            section = "Reasoning",
                            textEnglish = "Complete the series: 3, 7, 15, 31, 63, ?",
                            textHindi = "शृंखला को पूरा करें: 3, 7, 15, 31, 63, ?",
                            optionsEnglish = listOf("127", "126", "125", "128"),
                            optionsHindi = listOf("127", "126", "125", "128"),
                            correctOptionIndex = 0,
                            explanationEnglish = "Pattern: (x * 2) + 1. 3*2+1=7; 7*2+1=15; 15*2+1=31; 31*2+1=63; 63*2+1 = 127.",
                            explanationHindi = "पैटर्न: प्रत्येक पद में 2 से गुणा कर 1 जोड़ा गया है। 63 * 2 + 1 = 127।"
                        ).toMap(),
                        Question(
                            id = "y2",
                            number = 2,
                            section = "General Awareness",
                            textEnglish = "What is the motto of the Indian Air Force?",
                            textHindi = "भारतीय वायु सेना का आदर्श वाक्य क्या है?",
                            optionsEnglish = listOf(
                                "Touch the Sky with Glory (Nabhaḥ Spṛśaṁ Dīptam)",
                                "Service Before Self",
                                "May the Lord of Waters be auspicious unto us",
                                "Valour and Wisdom"
                            ),
                            optionsHindi = listOf(
                                "नभः स्पृशं दीप्तम् (Touch the Sky with Glory)",
                                "सेवा परमो धर्मः",
                                "शं नो वरुणः",
                                "वीरता और विवेक"
                            ),
                            correctOptionIndex = 0,
                            explanationEnglish = "The motto of the Indian Air Force is 'Nabhaḥ Spṛśaṁ Dīptam' (Touch the Sky with Glory), taken from the 11th chapter of the Gita.",
                            explanationHindi = "भारतीय वायुसेना का आदर्श वाक्य 'नभः स्पृशं दीप्तम्' (Touch the Sky with Glory) है जो भगवद्गीता के 11वें अध्याय से लिया गया है।"
                        ).toMap(),
                        Question(
                            id = "y3",
                            number = 3,
                            section = "General Awareness",
                            textEnglish = "Where is the headquarters of the Western Air Command situated?",
                            textHindi = "पश्चिमी वायु कमान का मुख्यालय कहाँ स्थित है?",
                            optionsEnglish = listOf("New Delhi", "Prayagraj", "Shillong", "Gandhinagar"),
                            optionsHindi = listOf("नई दिल्ली", "प्रयागराज", "शिलांग", "गांधीनगर"),
                            correctOptionIndex = 0,
                            explanationEnglish = "The Western Air Command of the Indian Air Force is headquartered in Subroto Park, New Delhi.",
                            explanationHindi = "भारतीय वायु सेना की पश्चिमी वायु कमान का मुख्यालय सुब्रतो पार्क, नई दिल्ली में स्थित है।"
                        ).toMap(),
                        Question(
                            id = "y4",
                            number = 4,
                            section = "Reasoning",
                            textEnglish = "If 'AIR' is coded as '28' (1+9+18), how will 'SKY' be coded?",
                            textHindi = "यदि 'AIR' का कोड '28' है (1+9+18), तो 'SKY' का कोड क्या होगा?",
                            optionsEnglish = listOf("55", "54", "53", "56"),
                            optionsHindi = listOf("55", "54", "53", "56"),
                            correctOptionIndex = 0,
                            explanationEnglish = "S=19, K=11, Y=25. Sum = 19 + 11 + 25 = 55.",
                            explanationHindi = "अक्षरों के स्थान मान: S=19, K=11, Y=25. कुल योग = 19 + 11 + 25 = 55।"
                        ).toMap(),
                        Question(
                            id = "y5",
                            number = 5,
                            section = "General Awareness",
                            textEnglish = "Air Force Day in India is celebrated every year on:",
                            textHindi = "भारत में वायु सेना दिवस प्रतिवर्ष किस दिन मनाया जाता है?",
                            optionsEnglish = listOf("8th October", "15th January", "4th December", "1st February"),
                            optionsHindi = listOf("8 अक्टूबर", "15 जनवरी", "4 दिसंबर", "1 फरवरी"),
                            correctOptionIndex = 0,
                            explanationEnglish = "Indian Air Force Day is celebrated on 8th October, commemorating the official establishment of IAF in 1932.",
                            explanationHindi = "भारतीय वायु सेना की स्थापना 8 अक्टूबर 1932 को हुई थी, इसलिए हर साल 8 अक्टूबर को वायु सेना दिवस मनाया जाता है।"
                        ).toMap(),
                        Question(
                            id = "y6",
                            number = 6,
                            section = "Reasoning",
                            textEnglish = "Pointing to a man, a woman said, 'His mother is the only daughter of my mother.' How is the woman related to the man?",
                            textHindi = "एक पुरुष की ओर इशारा करते हुए एक महिला ने कहा, 'उसकी माँ मेरी माँ की इकलौती बेटी है।' महिला उस पुरुष से किस प्रकार संबंधित है?",
                            optionsEnglish = listOf("Mother", "Sister", "Aunt", "Grandmother"),
                            optionsHindi = listOf("माँ (Mother)", "बहन (Sister)", "मौसी/चाची (Aunt)", "दादी/नानी (Grandmother)"),
                            correctOptionIndex = 0,
                            explanationEnglish = "'Only daughter of my mother' means the woman herself. So the woman is the man's mother.",
                            explanationHindi = "'मेरी माँ की इकलौती बेटी' स्वयं वह महिला है। अतः वह महिला उस पुरुष की माँ है।"
                        ).toMap(),
                        Question(
                            id = "y7",
                            number = 7,
                            section = "English",
                            textEnglish = "Fill in the blank: The commander asked the troops to ________ their weapons.",
                            textHindi = "रिक्त स्थान भरें: The commander asked the troops to ________ their weapons.",
                            optionsEnglish = listOf("lay down", "lie down", "laid down", "lain down"),
                            optionsHindi = listOf("lay down (समर्पण करना/रखना)", "lie down", "laid down", "lain down"),
                            correctOptionIndex = 0,
                            explanationEnglish = "'Lay down' means to put down or surrender arms.",
                            explanationHindi = "'Lay down weapons' एक मानक मुहावरा है जिसका अर्थ हथियार डालना या समर्पित करना होता है।"
                        ).toMap(),
                        Question(
                            id = "y8",
                            number = 8,
                            section = "General Awareness",
                            textEnglish = "Which supersonic cruise missile was jointly developed by India and Russia?",
                            textHindi = "भारत और रूस द्वारा संयुक्त रूप से किस सुपरसोनिक क्रूज मिसाइल का विकास किया गया?",
                            optionsEnglish = listOf("BrahMos", "Agni-V", "Prithvi", "Akash"),
                            optionsHindi = listOf("ब्रह्मोस (BrahMos)", "अग्नि-V", "पृथ्वी", "आकाश"),
                            correctOptionIndex = 0,
                            explanationEnglish = "BrahMos is named after Brahmaputra and Moskva rivers, jointly developed by DRDO and NPO Mashinostroyeniya.",
                            explanationHindi = "ब्रह्मोस (BrahMos) भारत की ब्रह्मपुत्र और रूस की मोस्कवा नदी के नाम पर विकसित सुपरसोनिक क्रूज मिसाइल है।"
                        ).toMap()
                    )
                ),
                MockExam(
                    id = "navy-ssr-aa-model",
                    title = "Indian Navy SSR / MR Practice Exam",
                    description = "Sailor entry benchmark exam testing General Science, Mathematics, and General Knowledge for Indian Navy aspirants.",
                    category = "Indian Navy",
                    durationMinutes = 60,
                    totalMarks = 100.0,
                    positiveMark = 1.0,
                    negativeMark = 0.25,
                    isPublished = true,
                    questionCount = 5,
                    createdBy = "admin",
                    questions = listOf(
                        Question(
                            id = "n1",
                            number = 1,
                            section = "General Knowledge",
                            textEnglish = "What is the motto of the Indian Navy?",
                            textHindi = "भारतीय नौसेना का आदर्श वाक्य क्या है?",
                            optionsEnglish = listOf(
                                "Shaṁ No Varuṇaḥ (May the Lord of Oceans be auspicious unto us)",
                                "Nabhaḥ Spṛśaṁ Dīptam",
                                "Service Before Self",
                                "Vayam Rakshamah"
                            ),
                            optionsHindi = listOf(
                                "शं नो वरुणः (Shaṁ No Varuṇaḥ)",
                                "नभः स्पृशं दीप्तम्",
                                "सेवा परमो धर्मः",
                                "वयम् रक्षामः"
                            ),
                            correctOptionIndex = 0,
                            explanationEnglish = "The motto of the Indian Navy is 'Shaṁ No Varuṇaḥ', invoked from the Rigveda.",
                            explanationHindi = "भारतीय नौसेना का आदर्श वाक्य ऋग्वेद से लिया गया 'शं नो वरुणः' है।"
                        ).toMap(),
                        Question(
                            id = "n2",
                            number = 2,
                            section = "General Science",
                            textEnglish = "What device is used to measure depth of the ocean using sound waves?",
                            textHindi = "ध्वनि तरंगों का उपयोग करके समुद्र की गहराई मापने के लिए किस उपकरण का उपयोग किया जाता है?",
                            optionsEnglish = listOf("SONAR / Fathometer", "RADAR", "Barometer", "Hydrometer"),
                            optionsHindi = listOf("सोनार / फेदोमीटर", "रडार", "बैरोमीटर", "हाइड्रोमीटर"),
                            correctOptionIndex = 0,
                            explanationEnglish = "SONAR (Sound Navigation and Ranging) or a Fathometer measures water depth through acoustic reflection.",
                            explanationHindi = "ध्वनि तरंगों के परावर्तन द्वारा गहराई मापने हेतु सोनार (SONAR) तथा फेदोमीटर का उपयोग किया जाता है।"
                        ).toMap(),
                        Question(
                            id = "n3",
                            number = 3,
                            section = "General Knowledge",
                            textEnglish = "India's first indigenously designed aircraft carrier is:",
                            textHindi = "भारत का पहला स्वदेशी विमानवाहक पोत कौन सा है?",
                            optionsEnglish = listOf("INS Vikrant", "INS Vikramaditya", "INS Viraat", "INS Arihant"),
                            optionsHindi = listOf("आईएनएस विक्रांत (INS Vikrant)", "आईएनएस विक्रमादित्य", "आईएनएस विराट", "आईएनएस अरिहंत"),
                            correctOptionIndex = 0,
                            explanationEnglish = "INS Vikrant (IAC-1) was constructed by Cochin Shipyard Limited and commissioned in 2022.",
                            explanationHindi = "आईएनएस विक्रांत (INS Vikrant) कोचीन शिपयार्ड में निर्मित भारत का प्रथम स्वदेशी विमानवाहक पोत है।"
                        ).toMap(),
                        Question(
                            id = "n4",
                            number = 4,
                            section = "Mathematics",
                            textEnglish = "If sin θ + cos θ = √2, then find the value of θ (0 < θ < 90°):",
                            textHindi = "यदि sin θ + cos θ = √2 है, तो θ का मान ज्ञात कीजिए (0 < θ < 90°):",
                            optionsEnglish = listOf("45°", "30°", "60°", "0°"),
                            optionsHindi = listOf("45°", "30°", "60°", "0°"),
                            correctOptionIndex = 0,
                            explanationEnglish = "Dividing by √2: (1/√2)sin θ + (1/√2)cos θ = 1 => cos(θ - 45°) = 1 => θ - 45° = 0 => θ = 45°.",
                            explanationHindi = "sin 45° = 1/√2 तथा cos 45° = 1/√2, अतः 1/√2 + 1/√2 = 2/√2 = √2। अतः θ = 45°।"
                        ).toMap(),
                        Question(
                            id = "n5",
                            number = 5,
                            section = "General Science",
                            textEnglish = "Why do ships float on water despite having massive iron and steel hulls?",
                            textHindi = "भारी लोहे और स्टील से बने होने के बावजूद जहाज पानी पर क्यों तैरते हैं?",
                            optionsEnglish = listOf(
                                "Archimedes' principle: buoyant force equals weight of displaced water",
                                "Surface tension of seawater",
                                "Magnetic repulsion of the seabed",
                                "Viscosity of saline water"
                            ),
                            optionsHindi = listOf(
                                "आर्किमिडीज का सिद्धांत: उत्प्लावन बल विस्थापित जल के भार के बराबर होता है",
                                "समुद्री जल का पृष्ठ तनाव",
                                "समुद्र तल का चुंबकीय प्रतिकर्षण",
                                "लवणीय जल की श्यानता"
                            ),
                            correctOptionIndex = 0,
                            explanationEnglish = "Due to the hollow hull design, a ship displaces a volume of water whose weight is equal to or greater than its own weight (Archimedes' Principle).",
                            explanationHindi = "जहाज का ढांचा खोखला बनाया जाता है जिससे विस्थापित जल का भार जहाज के कुल भार के बराबर हो जाता है।"
                        ).toMap()
                    )
                ),
                MockExam(
                    id = "icg-navik-gd-01",
                    title = "Indian Coast Guard Navik GD Model Exam",
                    description = "Section I & II combined high-yield examination for Indian Coast Guard Navik General Duty aspirants.",
                    category = "Indian Coast Guard",
                    durationMinutes = 45,
                    totalMarks = 50.0,
                    positiveMark = 1.0,
                    negativeMark = 0.0, // No negative mark in ICG Stage 1
                    isPublished = true,
                    questionCount = 5,
                    createdBy = "admin",
                    questions = listOf(
                        Question(
                            id = "c1",
                            number = 1,
                            section = "General Awareness",
                            textEnglish = "What is the motto of the Indian Coast Guard?",
                            textHindi = "भारतीय तटरक्षक बल (ICG) का आदर्श वाक्य क्या है?",
                            optionsEnglish = listOf("Vayam Rakshamah (We Protect)", "Shaṁ No Varuṇaḥ", "Nabhaḥ Spṛśaṁ Dīptam", "Sadaiva Tatpar"),
                            optionsHindi = listOf("वयम् रक्षामः (We Protect)", "शं नो वरुणः", "नभः स्पृशं दीप्तम्", "सदैव तत्पर"),
                            correctOptionIndex = 0,
                            explanationEnglish = "The motto of Indian Coast Guard is 'Vayam Rakshamah', translating to 'We Protect'.",
                            explanationHindi = "भारतीय तटरक्षक बल का आदर्श वाक्य 'वयम् रक्षामः' (We Protect) है।"
                        ).toMap(),
                        Question(
                            id = "c2",
                            number = 2,
                            section = "Physics",
                            textEnglish = "The unit of electric potential difference is:",
                            textHindi = "विद्युत विभवांतर का मात्रक क्या है?",
                            optionsEnglish = listOf("Volt", "Ampere", "Ohm", "Coulomb"),
                            optionsHindi = listOf("वोल्ट (Volt)", "एम्पीयर", "ओम", "कूलम्ब"),
                            correctOptionIndex = 0,
                            explanationEnglish = "Potential difference is measured in Volts (Joule/Coulomb).",
                            explanationHindi = "विद्युत विभवांतर की SI इकाई वोल्ट (V) है।"
                        ).toMap(),
                        Question(
                            id = "c3",
                            number = 3,
                            section = "Mathematics",
                            textEnglish = "What is the slope of the line parallel to the x-axis?",
                            textHindi = "x-अक्ष के समांतर रेखा की प्रवणता (Slope) क्या होती है?",
                            optionsEnglish = listOf("0", "1", "-1", "Undefined"),
                            optionsHindi = listOf("0", "1", "-1", "अपरिभाषित"),
                            correctOptionIndex = 0,
                            explanationEnglish = "A line parallel to the x-axis has inclination θ = 0°, so slope m = tan(0°) = 0.",
                            explanationHindi = "x-अक्ष के समांतर रेखा का झुकाव कोण 0° होता है, इसलिए ढाल m = tan 0° = 0।"
                        ).toMap(),
                        Question(
                            id = "c4",
                            number = 4,
                            section = "Reasoning",
                            textEnglish = "Find the odd one out:",
                            textHindi = "विषम शब्द को चुनिए:",
                            optionsEnglish = listOf("Fighter Jet", "Submarine", "Aircraft Carrier", "Destroyer"),
                            optionsHindi = listOf("लड़ाकू विमान (Fighter Jet)", "पनडुब्बी (Submarine)", "विमानवाहक पोत", "विध्वंसक (Destroyer)"),
                            correctOptionIndex = 0,
                            explanationEnglish = "Submarine, Aircraft Carrier, and Destroyer are all naval surface/sub-surface marine vessels, whereas Fighter Jet operates in the air.",
                            explanationHindi = "पनडुब्बी, विमानवाहक पोत और विध्वंसक जलसेना के पोत हैं जबकि लड़ाकू विमान वायुसेना का साधन है।"
                        ).toMap(),
                        Question(
                            id = "c5",
                            number = 5,
                            section = "English",
                            textEnglish = "Choose the antonym of 'HOSTILE':",
                            textHindi = "'HOSTILE' का विलोम शब्द चुनिए:",
                            optionsEnglish = listOf("Friendly", "Aggressive", "Bitter", "Violent"),
                            optionsHindi = listOf("Friendly (मित्रवत/सौहार्दपूर्ण)", "Aggressive (आक्रामक)", "Bitter (कटु)", "Violent (हिंसक)"),
                            correctOptionIndex = 0,
                            explanationEnglish = "Hostile means showing or feeling active opposition or unfriendliness. The antonym is Friendly.",
                            explanationHindi = "Hostile का अर्थ शत्रुतापूर्ण होता है, जिसका विलोम Friendly (मित्रवत) है।"
                        ).toMap()
                    )
                ),
                MockExam(
                    id = "current-affairs-defence-01",
                    title = "Defence Current Affairs & GK Speed Test",
                    description = "Quick 20-minute daily speed test featuring military exercises, defence appointments, and national security updates.",
                    category = "Current Affairs",
                    durationMinutes = 20,
                    totalMarks = 25.0,
                    positiveMark = 1.0,
                    negativeMark = 0.25,
                    isPublished = true,
                    questionCount = 5,
                    createdBy = "admin",
                    questions = listOf(
                        Question(
                            id = "ca1",
                            number = 1,
                            section = "Current Affairs",
                            textEnglish = "What is the name of the annual joint military exercise between India and France?",
                            textHindi = "भारत और फ्रांस के बीच वार्षिक संयुक्त सैन्य अभ्यास का क्या नाम है?",
                            optionsEnglish = listOf("SHAKTI", "VARUNA", "GARUDA", "YUDH ABHYAS"),
                            optionsHindi = listOf("शक्ति (SHAKTI)", "वरुण (VARUNA)", "गरुड़ (GARUDA)", "युद्ध अभ्यास"),
                            correctOptionIndex = 0,
                            explanationEnglish = "Exercise SHAKTI is the bilateral army exercise between India and France (VARUNA is naval, GARUDA is air force).",
                            explanationHindi = "भारत और फ्रांस के बीच थलसेना अभ्यास 'शक्ति' (SHAKTI) कहलाता है।"
                        ).toMap(),
                        Question(
                            id = "ca2",
                            number = 2,
                            section = "General Awareness",
                            textEnglish = "The highest wartime gallantry decoration in India is:",
                            textHindi = "भारत में सर्वोच्च युद्धकालीन वीरता पुरस्कार कौन सा है?",
                            optionsEnglish = listOf("Param Vir Chakra (PVC)", "Maha Vir Chakra", "Vir Chakra", "Ashoka Chakra"),
                            optionsHindi = listOf("परमवीर चक्र (PVC)", "महावीर चक्र", "वीर चक्र", "अशोक चक्र"),
                            correctOptionIndex = 0,
                            explanationEnglish = "The Param Vir Chakra (PVC) is India's highest military decoration for most conspicuous bravery in the presence of the enemy.",
                            explanationHindi = "शत्रु की उपस्थिति में अदम्य साहस और बलिदान के लिए सर्वोच्च पुरस्कार 'परमवीर चक्र' प्रदान किया जाता है।"
                        ).toMap(),
                        Question(
                            id = "ca3",
                            number = 3,
                            section = "Physics",
                            textEnglish = "The escape velocity from the surface of Earth is approximately:",
                            textHindi = "पृथ्वी की सतह से पलायन वेग का मान लगभग कितना है?",
                            optionsEnglish = listOf("11.2 km/s", "9.8 km/s", "8.0 km/s", "14.2 km/s"),
                            optionsHindi = listOf("11.2 किमी/सेकंड", "9.8 किमी/सेकंड", "8.0 किमी/सेकंड", "14.2 किमी/सेकंड"),
                            correctOptionIndex = 0,
                            explanationEnglish = "Escape velocity v_e = √(2gR) ≈ 11.2 km/s for Earth.",
                            explanationHindi = "पृथ्वी के लिए पलायन वेग √(2gR) = 11.2 km/s होता है।"
                        ).toMap(),
                        Question(
                            id = "ca4",
                            number = 4,
                            section = "Mathematics",
                            textEnglish = "Find the probability of rolling a sum of 7 with two fair six-sided dice:",
                            textHindi = "दो निष्पक्ष पासों को फेंकने पर योग 7 आने की प्रायिकता क्या है?",
                            optionsEnglish = listOf("1/6", "1/12", "7/36", "1/36"),
                            optionsHindi = listOf("1/6", "1/12", "7/36", "1/36"),
                            correctOptionIndex = 0,
                            explanationEnglish = "Favorable outcomes for sum 7: (1,6),(2,5),(3,4),(4,3),(5,2),(6,1) = 6 outcomes. Total = 36. Probability = 6/36 = 1/6.",
                            explanationHindi = "योग 7 आने के 6 अनुकूल परिणाम हैं: (1,6), (2,5), (3,4), (4,3), (5,2), (6,1)। प्रायिकता = 6/36 = 1/6।"
                        ).toMap(),
                        Question(
                            id = "ca5",
                            number = 5,
                            section = "English",
                            textEnglish = "Choose the correct one-word substitution for: 'A person who loves one's country and is ready to defend it':",
                            textHindi = "'वह व्यक्ति जो अपने देश से प्रेम करता है और उसकी रक्षा के लिए तत्पर रहता है' के लिए एक शब्द चुनें:",
                            optionsEnglish = listOf("Patriot", "Mercenary", "Martyr", "Diplomat"),
                            optionsHindi = listOf("Patriot (देशभक्त)", "Mercenary (भाड़े का सैनिक)", "Martyr (शहीद)", "Diplomat (राजनयिक)"),
                            correctOptionIndex = 0,
                            explanationEnglish = "A patriot is a person who vigorously supports and defends their country.",
                            explanationHindi = "अपने देश से अगाध प्रेम करने और उसकी रक्षा के लिए तत्पर रहने वाले को Patriot (देशभक्त) कहते हैं।"
                        ).toMap()
                    )
                )
            )
        }
    }
}
