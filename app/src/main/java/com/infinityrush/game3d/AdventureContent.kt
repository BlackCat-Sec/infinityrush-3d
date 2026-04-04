package com.infinityrush.game3d

enum class MissionType {
    COLLECT_COINS,
    SURVIVE_DISTANCE,
    DODGE_HAZARDS,
    USE_POWER_UPS
}

enum class RunnerCharacter(
    val id: String,
    val displayName: String,
    val title: String,
    val unlockCoins: Int,
    val skinTone: FloatArray,
    val hairColor: FloatArray,
    val outfitPrimary: FloatArray,
    val outfitSecondary: FloatArray,
    val gearColor: FloatArray
) {
    ARIA(
        id = "aria",
        displayName = "Aria",
        title = "Ruins Scout",
        unlockCoins = 0,
        skinTone = floatArrayOf(0.88f, 0.71f, 0.58f, 1f),
        hairColor = floatArrayOf(0.18f, 0.13f, 0.10f, 1f),
        outfitPrimary = floatArrayOf(0.20f, 0.56f, 0.74f, 1f),
        outfitSecondary = floatArrayOf(0.93f, 0.92f, 0.84f, 1f),
        gearColor = floatArrayOf(0.09f, 0.14f, 0.19f, 1f)
    ),
    KAI(
        id = "kai",
        displayName = "Kai",
        title = "Stone Runner",
        unlockCoins = 220,
        skinTone = floatArrayOf(0.69f, 0.51f, 0.38f, 1f),
        hairColor = floatArrayOf(0.10f, 0.09f, 0.08f, 1f),
        outfitPrimary = floatArrayOf(0.93f, 0.44f, 0.21f, 1f),
        outfitSecondary = floatArrayOf(0.96f, 0.84f, 0.63f, 1f),
        gearColor = floatArrayOf(0.27f, 0.16f, 0.08f, 1f)
    ),
    SELENE(
        id = "selene",
        displayName = "Selene",
        title = "Night Relic Hunter",
        unlockCoins = 520,
        skinTone = floatArrayOf(0.83f, 0.66f, 0.56f, 1f),
        hairColor = floatArrayOf(0.21f, 0.08f, 0.17f, 1f),
        outfitPrimary = floatArrayOf(0.46f, 0.37f, 0.84f, 1f),
        outfitSecondary = floatArrayOf(0.89f, 0.87f, 0.99f, 1f),
        gearColor = floatArrayOf(0.13f, 0.11f, 0.24f, 1f)
    ),
    TORIN(
        id = "torin",
        displayName = "Torin",
        title = "Temple Vanguard",
        unlockCoins = 920,
        skinTone = floatArrayOf(0.56f, 0.38f, 0.26f, 1f),
        hairColor = floatArrayOf(0.36f, 0.24f, 0.13f, 1f),
        outfitPrimary = floatArrayOf(0.16f, 0.70f, 0.50f, 1f),
        outfitSecondary = floatArrayOf(0.88f, 0.96f, 0.88f, 1f),
        gearColor = floatArrayOf(0.06f, 0.16f, 0.13f, 1f)
    );

    companion object {
        fun fromId(id: String?): RunnerCharacter {
            return entries.firstOrNull { it.id == id } ?: ARIA
        }
    }
}

data class MissionDefinition(
    val type: MissionType,
    val title: String,
    val description: String,
    val target: Int,
    val rewardCoins: Int
)

object AdventureContent {
    val missions = listOf(
        MissionDefinition(
            type = MissionType.COLLECT_COINS,
            title = "Treasure Sweep",
            description = "Collect coins from the ruins paths.",
            target = 30,
            rewardCoins = 80
        ),
        MissionDefinition(
            type = MissionType.SURVIVE_DISTANCE,
            title = "Long Escape",
            description = "Stay alive deep into the temple grounds.",
            target = 450,
            rewardCoins = 120
        ),
        MissionDefinition(
            type = MissionType.DODGE_HAZARDS,
            title = "Trap Master",
            description = "Pass hazard lines without getting clipped.",
            target = 18,
            rewardCoins = 140
        ),
        MissionDefinition(
            type = MissionType.USE_POWER_UPS,
            title = "Relic Channeler",
            description = "Activate ancient relic boosts.",
            target = 4,
            rewardCoins = 110
        )
    )
}
