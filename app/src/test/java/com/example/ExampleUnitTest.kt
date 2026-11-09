package com.example

import com.example.data.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun admin_authorization_check() {
        val adminProfile = UserProfile(
            userId = "admin-1",
            name = "Swami Ji",
            email = "swamiramesh9785@gmail.com",
            role = "admin"
        )
        assertTrue(adminProfile.isAdmin)

        val adminProfileCaseInsensitive = UserProfile(
            userId = "admin-2",
            name = "Swami Ji",
            email = "SWAMIRAMESH9785@GMAIL.COM",
            role = "student"
        )
        assertTrue(adminProfileCaseInsensitive.isAdmin)

        val studentProfile = UserProfile(
            userId = "student-1",
            name = "Ramesh Kumar",
            email = "student@gmail.com",
            role = "student"
        )
        assertFalse(studentProfile.isAdmin)
    }

    @Test
    fun score_calculation_with_negative_marking() {
        val correctCount = 18
        val incorrectCount = 4
        val positiveMark = 1.0
        val negativeMark = 0.25

        val rawScore = (correctCount * positiveMark) - (incorrectCount * negativeMark)
        assertEquals(17.0, rawScore, 0.001)

        val totalAttempted = correctCount + incorrectCount
        val accuracy = (correctCount.toDouble() / totalAttempted) * 100.0
        assertEquals(81.81, accuracy, 0.01)
    }
}
