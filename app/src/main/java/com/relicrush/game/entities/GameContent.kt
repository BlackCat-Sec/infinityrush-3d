package com.relicrush.game.entities

import android.graphics.Color
import com.relicrush.game.utils.GameConstants

object GameContent {
    val characters = listOf(
        CharacterDefinition(
            id = "maya",
            name = "Maya",
            title = "Temple Scout",
            unlockCost = 0,
            speedMultiplier = 1.0f,
            jumpMultiplier = 1.0f,
            palette = CharacterPalette(
                skinColor = Color.parseColor("#D7A37A"),
                primaryColor = Color.parseColor("#3B7A57"),
                accentColor = Color.parseColor("#E4C16F"),
                hairColor = Color.parseColor("#1F1B1A"),
                glowColor = Color.parseColor("#86F3B1")
            )
        ),
        CharacterDefinition(
            id = "arin",
            name = "Arin",
            title = "Relic Hunter",
            unlockCost = 2200,
            speedMultiplier = 1.08f,
            jumpMultiplier = 0.98f,
            palette = CharacterPalette(
                skinColor = Color.parseColor("#C88E6A"),
                primaryColor = Color.parseColor("#7D4F2A"),
                accentColor = Color.parseColor("#FFCC73"),
                hairColor = Color.parseColor("#261A14"),
                glowColor = Color.parseColor("#FFBF69")
            )
        ),
        CharacterDefinition(
            id = "nyra",
            name = "Nyra",
            title = "Bridge Runner",
            unlockCost = 3800,
            speedMultiplier = 1.02f,
            jumpMultiplier = 1.12f,
            palette = CharacterPalette(
                skinColor = Color.parseColor("#E1B896"),
                primaryColor = Color.parseColor("#375A7F"),
                accentColor = Color.parseColor("#79E0FF"),
                hairColor = Color.parseColor("#302727"),
                glowColor = Color.parseColor("#6BE6FF")
            )
        ),
        CharacterDefinition(
            id = "taro",
            name = "Taro",
            title = "Night Sentinel",
            unlockCost = 6200,
            speedMultiplier = 1.1f,
            jumpMultiplier = 1.06f,
            palette = CharacterPalette(
                skinColor = Color.parseColor("#A87054"),
                primaryColor = Color.parseColor("#241C42"),
                accentColor = Color.parseColor("#C2A8FF"),
                hairColor = Color.parseColor("#120D21"),
                glowColor = Color.parseColor("#B087FF")
            )
        )
    )

    val missions = listOf(
        MissionDefinition(
            type = MissionType.DISTANCE,
            title = "Scout The Trail",
            description = "Run 600m across the ruins",
            target = 600,
            rewardCoins = 350
        ),
        MissionDefinition(
            type = MissionType.COINS,
            title = "Treasure Fever",
            description = "Collect 120 coins",
            target = 120,
            rewardCoins = 420
        ),
        MissionDefinition(
            type = MissionType.DODGES,
            title = "Trap Dancer",
            description = "Survive 35 obstacles",
            target = 35,
            rewardCoins = 480
        ),
        MissionDefinition(
            type = MissionType.POWER_UPS,
            title = "Relic Charge",
            description = "Activate 6 power-ups",
            target = 6,
            rewardCoins = 540
        )
    )

    val dailyRewards = listOf(150, 250, 400, 650, 900, 1200, 1800)

    fun getCharacter(id: String): CharacterDefinition {
        return characters.firstOrNull { it.id == id } ?: characters.first { it.id == GameConstants.DEFAULT_UNLOCKED_CHARACTER }
    }
}
