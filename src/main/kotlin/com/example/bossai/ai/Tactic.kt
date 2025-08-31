package com.example.bossai.ai

/**
 * Enumeration of all tactics that our boss can execute.  These tactics
 * represent high‑level behaviour decisions.  The lightweight model will
 * choose one of these based on the current game state.  Additional
 * tactics can be added here and wired up in the entity and model.
 */
enum class Tactic {
    /** Idle state; the boss will roam or do nothing special. */
    IDLE,

    /** Performs a burst area of effect attack on nearby players. */
    BURST_AOE,

    /** Retreats or kites away from players when low on health. */
    KITE,

    /** Summons additional minions or helpers. */
    SUMMON
}