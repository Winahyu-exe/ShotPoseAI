package com.shotposeai.analysis

data class MovementDefinition(
    val name: String,
    val level: Int,
    val badgeName: String,
    val submovements: List<String>
)

object MovementCatalog {
    val all = listOf(
        MovementDefinition("Throw-in", 1, "Pemula Throw-in", listOf("Posisi kaki", "Bola di belakang kepala", "Lemparan dua tangan")),
        MovementDefinition("Passing kaki bagian dalam", 2, "Passing Akurat", listOf("Kaki tumpu", "Kontak kaki bagian dalam", "Arah badan")),
        MovementDefinition("Passing kaki bagian luar", 3, "Pengarah Bola", listOf("Kaki tumpu", "Kontak kaki bagian luar", "Follow-through")),
        MovementDefinition("Passing punggung kaki", 4, "Pengumpan Andal", listOf("Ayunan kaki", "Kontak punggung kaki", "Keseimbangan")),
        MovementDefinition("Dribbling", 5, "Master Dribbling", listOf("Kontrol bola", "Langkah pendek", "Arah badan")),
        MovementDefinition("Kontrol bola", 6, "Pengendali Bola", listOf("Posisi badan", "Penerimaan bola", "Keseimbangan")),
        MovementDefinition("Heading", 7, "Heading Berani", listOf("Posisi badan", "Kontak dahi", "Arah kepala")),
        MovementDefinition("Shooting", 8, "Finisher", listOf("Kaki tumpu", "Ayunan kaki", "Follow-through")),
        MovementDefinition("Free kick", 9, "Spesialis Bola Mati", listOf("Awalan", "Kaki tumpu", "Follow-through")),
        MovementDefinition("Penalti", 10, "Eksekutor Penalti", listOf("Awalan", "Arah badan", "Keseimbangan"))
    )

    fun find(name: String): MovementDefinition =
        all.firstOrNull { it.name == name } ?: all.first()
}
