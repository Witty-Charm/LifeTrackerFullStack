namespace LifeTracker.Models;

public class EconomyBalance
{
    public int Id { get; set; }
    public int HeroId { get; set; }

    public long TotalGoldEarned { get; set; } = 0;
    public long TotalGoldSpent { get; set; } = 0;
    public long TotalXpEarned { get; set; } = 0;

    public int DailyTaskCompletions { get; set; } = 0;
    public int MaxDailyCompletions { get; set; } = 50;
    public DateTimeOffset DailyResetAt { get; set; } = DateTimeOffset.UtcNow.Date;

    public decimal XpMultiplier { get; set; } = 1.0m;
    public decimal GoldMultiplier { get; set; } = 1.0m;
    public DateTimeOffset? MultiplierExpiresAt { get; set; }

    public bool IsInPenaltyPeriod { get; set; } = false;
    public DateTimeOffset? PenaltyEndsAt { get; set; }
    public decimal PenaltyMultiplier { get; set; } = 1.0m;

    public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;
    public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.UtcNow;

    public Hero? Hero { get; set; }

    public void CheckDailyReset()
    {
        if (DateTimeOffset.UtcNow.Date > DailyResetAt.Date)
        {
            DailyResetAt = DateTimeOffset.UtcNow.Date;
            DailyTaskCompletions = 0;
        }
    }

    public bool CanCompleteTask()
    {
        CheckDailyReset();
        return DailyTaskCompletions < MaxDailyCompletions;
    }

    public void IncrementDailyCompletion()
    {
        CheckDailyReset();
        DailyTaskCompletions++;
    }

    public decimal GetFinalXpMultiplier()
    {
        decimal multiplier = XpMultiplier;
        if (IsInPenaltyPeriod && PenaltyEndsAt.HasValue && DateTimeOffset.UtcNow <= PenaltyEndsAt.Value)
            multiplier *= PenaltyMultiplier;
        return multiplier;
    }

    public void ActivateDeathPenalty()
    {
        IsInPenaltyPeriod = true;
        PenaltyEndsAt = DateTimeOffset.UtcNow.AddHours(4);
        PenaltyMultiplier = 0.75m;
    }
}