package ca.georgebrown.studygroupfinder.ui.profile

/** Edit Profile rules (FR-16). Pure functions so they're unit tested without Android. Each returns an error message or null. */
object ProfileValidator {
    const val MAX_NAME_LENGTH = 50
    const val MAX_PROGRAM_LENGTH = 60
    const val MAX_COURSES = 10

    // George Brown codes look like COMP3074 or GBC1001: 3-4 letters then 4 digits.
    private val courseCodePattern = Regex("^[A-Z]{3,4}\\d{4}$")

    fun validateName(name: String): String? = when {
        name.isBlank() -> "Name can't be empty."
        name.trim().length > MAX_NAME_LENGTH -> "Name must be $MAX_NAME_LENGTH characters or fewer."
        else -> null
    }

    fun validateProgram(program: String): String? =
        if (program.trim().length > MAX_PROGRAM_LENGTH) "Program must be $MAX_PROGRAM_LENGTH characters or fewer." else null

    /** Checks a course before it's added to [existing]. Expects the code already trimmed and upper-cased. */
    fun validateNewCourse(code: String, existing: List<String>): String? = when {
        code.isEmpty() -> "Type a course code first."
        !courseCodePattern.matches(code) -> "Use a code like COMP3074."
        code in existing -> "$code is already in your list."
        existing.size >= MAX_COURSES -> "You can list up to $MAX_COURSES courses."
        else -> null
    }

    /** Upper-cases and strips spaces so "comp 3074" becomes "COMP3074". */
    fun normalizeCourse(raw: String): String = raw.filterNot { it.isWhitespace() }.uppercase()
}
