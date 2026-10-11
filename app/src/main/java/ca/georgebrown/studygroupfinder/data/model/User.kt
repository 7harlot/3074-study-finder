package ca.georgebrown.studygroupfinder.data.model

/**
 * A student account. Mirrors the "User" entity in the design proposal (Section 7)
 * so the Firestore version later is a drop-in swap.
 */
data class User(
    val userId: String,
    val name: String,
    val email: String,
    val program: String = "",
    val enrolledCourses: List<String> = emptyList(),
    val profilePhotoUrl: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
)
