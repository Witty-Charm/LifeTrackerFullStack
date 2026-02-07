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
    
    public DateTime? RecoveryEndsAt { get; set; }
    
    public DateTime CreatedDate { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
    
    public ICollection<GameTask> Tasks { get; set; } = new List<GameTask>();
    public ICollection<Streak> Streaks { get; set; } = new List<Streak>();
    public EconomyBalance? EconomyBalance { get; set; }
    
    public long GetXpRequiredForNextLevel() => GameConstants.CalculateXpForLevel(Level);
    
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
            
            MaxHp = GameConstants.CalculateMaxHp(Level);
            CurrentHp = MaxHp;
        }
    }
    
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
    
    public void Die()
    {
        IsDead = true;
        DeathTime = DateTime.UtcNow;
        DeathCount++;
        
        CurrentHp = (int)(MaxHp * GameConstants.DeathHpResetPercent);
        
        long xpLoss = (long)(GetXpRequiredForNextLevel() * GameConstants.DeathXpPenaltyPercent);
        CurrentXp = Math.Max(0, CurrentXp - xpLoss);
        
        int goldLoss = (int)(Gold * GameConstants.DeathGoldPenaltyPercent);
        Gold = Math.Max(0, Gold - goldLoss);
        
        UpdatedAt = DateTime.UtcNow;
    }
    
    public void Respawn()
    {
        if (!IsDead) return;
        
        IsDead = false;
        RecoveryEndsAt = DateTime.UtcNow.AddHours(GameConstants.RecoveryDebuffHours);
        UpdatedAt = DateTime.UtcNow;
    }
    
    public bool IsInRecovery() => 
        RecoveryEndsAt.HasValue && DateTime.UtcNow < RecoveryEndsAt.Value;
    
    public double GetRecoveryMultiplier() => 
        IsInRecovery() ? GameConstants.RecoveryDebuffMultiplier : 1.0;
}
