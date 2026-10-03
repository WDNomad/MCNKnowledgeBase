package ru.mcn.knowledgebase.domain.model

const val PRIVATE_SECTION_ID = "baza-znaniy-abonentov-mc-nmobile-fizicheskikh-lits"

enum class Audience(val storedValue: String, val startRoute: String) {
    BUSINESS("business", "categories"),
    PRIVATE("private", "subsections/$PRIVATE_SECTION_ID");

    companion object {
        fun fromStoredValue(value: String?): Audience? = entries.firstOrNull { it.storedValue == value }
    }
}
