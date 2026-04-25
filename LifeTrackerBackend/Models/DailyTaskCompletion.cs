namespace LifeTracker.Models;

public class DailyTaskCompletion
{
    public int Id { get; set; }
    public int HeroId { get; set; }
    public int TaskId { get; set; }
    public string LocalDate { get; set; } = string.Empty;
    public bool IsChecked { get; set; }

    public long RewardXp { get; set; }
    public int RewardGold { get; set; }
    public bool ConsumedXpBoostCharge { get; set; }
    public bool ConsumedLastXpBoostCharge { get; set; }
    public int PreviousXpBoostPercent { get; set; }

    public int PreviousTaskCompletionCount { get; set; }
    public DateTimeOffset? PreviousTaskLastCompletedAt { get; set; }

    public bool StreakExistedBefore { get; set; }
    public int? PreviousStreakCurrentDays { get; set; }
    public int? PreviousStreakLongestDays { get; set; }
    public DateTimeOffset? PreviousStreakStartDate { get; set; }
    public DateTimeOffset? PreviousStreakLastCheckIn { get; set; }
    public string? PreviousStreakLastCheckInLocalDate { get; set; }

    public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;
    public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.UtcNow;
    public byte[] RowVersion { get; set; } = Array.Empty<byte>();

    public Hero? Hero { get; set; }
    public GameTask? Task { get; set; }
}
