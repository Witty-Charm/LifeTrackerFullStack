using System.ComponentModel.DataAnnotations.Schema;

namespace LifeTracker.Models;

public class EconomyBalance
{
    public int Id { get; set; }
    public int HeroId { get; set; }

    public long TotalGoldEarned { get; set; } = 0;
    public long TotalGoldSpent { get; set; } = 0;
    public long TotalXpEarned { get; set; } = 0;

    [Column("DailyTaskCompletions")]
    public int TaskCompletions { get; set; } = 0;
    [Column("MaxDailyCompletions")]
    public int MaxCompletions { get; set; } = Constants.GameConstants.DailyTaskCap;
    public DateTimeOffset DailyResetAt { get; set; } = DateTimeOffset.UtcNow.Date;
    public string? LastDailyResetLocalDate { get; set; }

    public decimal XpMultiplier { get; set; } = 1.0m;
    public decimal GoldMultiplier { get; set; } = 1.0m;
    public DateTimeOffset? MultiplierExpiresAt { get; set; }

    public bool IsInPenaltyPeriod { get; set; } = false;
    public DateTimeOffset? PenaltyEndsAt { get; set; }
    public decimal PenaltyMultiplier { get; set; } = 1.0m;

    public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;
    public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.UtcNow;
    public byte[] RowVersion { get; set; } = Array.Empty<byte>();

    public Hero? Hero { get; set; }

    public void CheckDailyReset(DateOnly todayLocalDate)
    {
        var todayLocalDateStr = todayLocalDate.ToString("yyyy-MM-dd");

        if (string.IsNullOrWhiteSpace(LastDailyResetLocalDate))
        {
            LastDailyResetLocalDate = todayLocalDateStr;
            return;
        }

        var last = DateOnly.ParseExact(LastDailyResetLocalDate, "yyyy-MM-dd");
        if (todayLocalDate > last)
        {
            DailyResetAt = DateTimeOffset.UtcNow.Date;
            TaskCompletions = 0;
            LastDailyResetLocalDate = todayLocalDateStr;
        }
    }

    public bool CanCompleteTask(DateOnly todayLocalDate)
    {
        CheckDailyReset(todayLocalDate);
        return TaskCompletions < MaxCompletions;
    }

    public void IncrementCompletion(DateOnly todayLocalDate)
    {
        CheckDailyReset(todayLocalDate);
        TaskCompletions++;
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
