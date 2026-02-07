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
    public DateTime DailyResetAt { get; set; } = DateTime.UtcNow.Date;

    public decimal XpMultiplier { get; set; } = 1.0m;
    public decimal GoldMultiplier { get; set; } = 1.0m;
    public DateTime? MultiplierExpiresAt { get; set; }

    public bool IsInPenaltyPeriod { get; set; } = false;
    public DateTime? PenaltyEndsAt { get; set; }
    public decimal PenaltyMultiplier { get; set; } = 1.0m;

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    public Hero? Hero { get; set; }

    public void CheckDailyReset()
    {
        if (DateTime.UtcNow.Date > DailyResetAt.Date)
        {
            DailyResetAt = DateTime.UtcNow.Date;
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
        if (IsInPenaltyPeriod && PenaltyEndsAt.HasValue && DateTime.UtcNow <= PenaltyEndsAt.Value)
            multiplier *= PenaltyMultiplier;
        return multiplier;
    }

    public void ActivateDeathPenalty()
    {
        IsInPenaltyPeriod = true;
        PenaltyEndsAt = DateTime.UtcNow.AddHours(4);
        PenaltyMultiplier = 0.75m;
    }
}