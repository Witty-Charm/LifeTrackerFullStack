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

    public int XpBoostPercent { get; set; } = 0;
    public int XpBoostTasksRemaining { get; set; } = 0;

    public bool IsDead { get; set; } = false;
    public DateTimeOffset? DeathTime { get; set; }
    public int DeathCount { get; set; } = 0;

    public DateTimeOffset? RecoveryEndsAt { get; set; }

    public DateTimeOffset CreatedDate { get; set; } = DateTimeOffset.UtcNow;
    public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.UtcNow;

    public ICollection<GameTask> Tasks { get; set; } = new List<GameTask>();
    public ICollection<Streak> Streaks { get; set; } = new List<Streak>();
    public EconomyBalance? EconomyBalance { get; set; }

    public long GetXpRequiredForNextLevel() => GameConstants.CalculateXpForLevel(Level);

    public void GainXP(long amount)
    {
        if (IsDead) return;

        CurrentXp += amount;
        TotalXpEarned += amount;
        UpdatedAt = DateTimeOffset.UtcNow;

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
        UpdatedAt = DateTimeOffset.UtcNow;

        if (CurrentHp <= 0)
        {
            CurrentHp = 0;
            Die();
        }
    }

    public void Die()
    {
        IsDead = true;
        DeathTime = DateTimeOffset.UtcNow;
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
        RecoveryEndsAt = DateTimeOffset.UtcNow.AddHours(GameConstants.RecoveryDebuffHours);
        UpdatedAt = DateTimeOffset.UtcNow;
    }

    public bool IsInRecovery() =>
        RecoveryEndsAt.HasValue && DateTimeOffset.UtcNow < RecoveryEndsAt.Value;

    public double GetRecoveryMultiplier() =>
        IsInRecovery() ? GameConstants.RecoveryDebuffMultiplier : 1.0;
}