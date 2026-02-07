using LifeTracker.Constants;

namespace LifeTracker.Models;

public class Hero
{
    public int Id { get; set; }
    public string Name { get; set; } = string.Empty;
    public int Level { get; set; } = 1;
    public long CurrentXp { get; set; } = 0;
    public long TotalXpEarned { get; set; } = 0;
    
    public int CurrentHp { get; set; } = GameConstants.BaseHp;
    public int MaxHp { get; set; } = GameConstants.BaseHp;
    public int Gold { get; set; } = 0;
    
    public bool IsDead { get; set; } = false;
    public DateTime? DeathTime { get; set; }
    public int DeathCount { get; set; } = 0;
    
    // Recovery debuff after death (GDD: 4-hour -25% rewards)
    public DateTime? RecoveryEndsAt { get; set; }
    
    public DateTime CreatedDate { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
    
    public ICollection<GameTask> Tasks { get; set; } = new List<GameTask>();
    public ICollection<Streak> Streaks { get; set; } = new List<Streak>();
    public EconomyBalance? EconomyBalance { get; set; }

    /// <summary>
    /// XP required for next level using GDD formula:
    /// floor(BASE_XP × level^EXPONENT × (1 + level/SCALE_FACTOR))
    /// </summary>
    public long GetXpRequiredForNextLevel() => GameConstants.CalculateXpForLevel(Level);

    /// <summary>
    /// Gain XP and level up if threshold is reached.
    /// Updates MaxHp on level up using GDD formula: BaseHp + HpPerLevel × Level
    /// </summary>
    public void GainXP(long amount)
    {
        if (IsDead) return;
        
        CurrentXp += amount;
        TotalXpEarned += amount;
        UpdatedAt = DateTime.UtcNow;
        
        while (CurrentXp >= GetXpRequiredForNextLevel() && Level < GameConstants.MaxLevel)
        {
            CurrentXp -= GetXpRequiredForNextLevel();
            Level++;
            
            // GDD: MaxHp = BaseHp + HpPerLevel × Level
            MaxHp = GameConstants.CalculateMaxHp(Level);
            CurrentHp = MaxHp; // Full heal on level up
        }
    }
    
    /// <summary>
    /// Take HP damage. Triggers death if HP reaches 0.
    /// </summary>
    public void TakeDamage(int damage)
    {
        if (IsDead) return;
        
        CurrentHp -= damage;
        UpdatedAt = DateTime.UtcNow;
        
        if (CurrentHp <= 0)
        {
            CurrentHp = 0;
            Die();
        }
    }

    /// <summary>
    /// GDD Option A: Soft Reset
    /// - HP reset to 25% of MaxHp
    /// - Lose 10% of current level XP
    /// - Lose 20% of Gold
    /// - IsDead remains TRUE until Respawn() is called
    /// </summary>
    public void Die()
    {
        IsDead = true;
        DeathTime = DateTime.UtcNow;
        DeathCount++;
        
        // GDD: Reset HP to 25% of MaxHp
        CurrentHp = (int)(MaxHp * GameConstants.DeathHpResetPercent);
        
        // GDD: Lose 10% of current level's required XP
        long xpLoss = (long)(GetXpRequiredForNextLevel() * GameConstants.DeathXpPenaltyPercent);
        CurrentXp = Math.Max(0, CurrentXp - xpLoss);

        // GDD: Lose 20% of Gold
        int goldLoss = (int)(Gold * GameConstants.DeathGoldPenaltyPercent);
        Gold = Math.Max(0, Gold - goldLoss);
        
        UpdatedAt = DateTime.UtcNow;
        // NOTE: IsDead stays TRUE - must call Respawn() to continue playing
    }

    /// <summary>
    /// Respawn the hero after death. Applies recovery debuff.
    /// </summary>
    public void Respawn()
    {
        if (!IsDead) return;
        
        IsDead = false;
        RecoveryEndsAt = DateTime.UtcNow.AddHours(GameConstants.RecoveryDebuffHours);
        UpdatedAt = DateTime.UtcNow;
    }

    /// <summary>
    /// Check if hero is in recovery period (reduced rewards).
    /// </summary>
    public bool IsInRecovery() => 
        RecoveryEndsAt.HasValue && DateTime.UtcNow < RecoveryEndsAt.Value;

    /// <summary>
    /// Get current reward multiplier (affected by recovery debuff).
    /// </summary>
    public double GetRecoveryMultiplier() => 
        IsInRecovery() ? GameConstants.RecoveryDebuffMultiplier : 1.0;
}
