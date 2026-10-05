package com.shotposeai.analysis

/** Public references located 2026-09-03; embedding availability is controlled by each video owner. */
object TechniqueVideos {
    data class Video(val id: String, val title: String) { val watchUrl get() = "https://www.youtube.com/watch?v=$id" }
    val byMovement = mapOf(
        "Throw-in" to Video("cbRRKSB9L1M", "How to master the soccer throw in"),
        "Passing kaki bagian dalam" to Video("oIpRuzvsU80", "The basics of passing"),
        "Passing kaki bagian luar" to Video("HJQ7FDufYTA", "How to use the outside of your foot"),
        "Passing punggung kaki" to Video("HZ5W1HDheTo", "Every passing technique in football"),
        "Dribbling" to Video("naEccnjzLxM", "The basics of dribbling"),
        "Kontrol bola" to Video("Z5_-hV5oc1s", "How to improve your first touch"),
        "Heading" to Video("jO9Vmsl_97U", "Attacking heading tutorial"),
        "Shooting" to Video("2wHXqTqVPFo", "How to shoot with power"),
        "Free kick" to Video("6mFfS1PE3Vo", "Free kick tutorial: knuckleball, curve, top spin"),
        "Penalti" to Video("8Rl4fCK_cHc", "How to shoot a soccer penalty")
    )
}
