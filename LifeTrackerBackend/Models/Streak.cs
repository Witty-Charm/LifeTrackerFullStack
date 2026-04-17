using LifeTracker.Constants;

namespace LifeTracker.Models;

public class Streak
{
    public int Id { get; set; }
    public int HeroId { get; set; }
    public int? TaskId { get; set; }

    public int CurrentDays { get; set; }
    public int LongestDays { get; set; }
    public DateTimeOffset? StartDate { get; set; }
    public DateTimeOffset? LastCheckIn { get; set; }

    public int FreezeCharges { get; set; } = 0;
    public DateTimeOffset? FreezeActiveUntil { get; set; }
    public bool IsShieldActive { get; set; } = false;
    public DateTimeOffset? ShieldExpiresAtUtc { get; set; }
    public bool ShieldFailConsumed { get; set; } = false;
    public int? ShieldBackupCurrentDays { get; set; }
    public DateTimeOffset? ShieldBackupBreakAtUtc { get; set; }

    public int TotalBreaks { get; set; } = 0;
    public DateTimeOffset? LastBreakDate { get; set; }
    public string? LastCheckInLocalDate { get; set; }
    public string? LastBreakLocalDate { get; set; }
    public byte[] RowVersion { get; set; } = Array.Empty<byte>();

    public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;
    public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.UtcNow;

    public Hero? Hero { get; set; }
    public GameTask? Task { get; set; }

    public double GetStreakMultiplier() => GameConstants.CalculateStreakMultiplier(CurrentDays);

    public int GetBonusXpPercent() => (int)((GetStreakMultiplier() - 1.0) * 100);

    public int GetStreakTier() => (CurrentDays / GameConstants.StreakTierDays) + 1;

    public bool IsFrozen()
    {
        if (FreezeActiveUntil == null) return false;
        if (DateTime.UtcNow > FreezeActiveUntil)
        {
            FreezeActiveUntil = null;
            return false;
        }

        return true;
    }

    public void RegisterSuccess(DateOnly localDate, DateTimeOffset now)
    {
        var localDateStr = localDate.ToString("yyyy-MM-dd");

        if (string.IsNullOrWhiteSpace(LastCheckInLocalDate))
        {
            StartDate = now;
            CurrentDays = 1;
        }
        else
        {
            var last = DateOnly.ParseExact(LastCheckInLocalDate, "yyyy-MM-dd");
            var daysDiff = localDate.DayNumber - last.DayNumber;

            if (daysDiff == 1)
            {
                CurrentDays++;
            }
            else if (daysDiff > 1 && !IsFrozen())
            {
                CurrentDays = 1;
                StartDate = now;
            }
        }

        LastCheckIn = now;
        LastCheckInLocalDate = localDateStr;

        if (CurrentDays > LongestDays)
            LongestDays = CurrentDays;

        UpdatedAt = now;
    }


    public void Break(DateOnly? localDate = null, DateTimeOffset? now = null)
    {
        var utcNow = now ?? DateTimeOffset.UtcNow;

        CurrentDays = 0;
        StartDate = null;
        TotalBreaks++;
        LastBreakDate = utcNow;
        LastBreakLocalDate = localDate?.ToString("yyyy-MM-dd");
        UpdatedAt = utcNow;
    }
}