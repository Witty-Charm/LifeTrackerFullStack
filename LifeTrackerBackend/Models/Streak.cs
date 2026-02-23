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
    public DateTimeOffset? ShieldExpiresAt { get; set; }

    public int TotalBreaks { get; set; } = 0;
    public DateTimeOffset? LastBreakDate { get; set; }

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

    public void RegisterSuccess()
    {
        var now = DateTime.UtcNow;

        if (StartDate == null)
        {
            StartDate = now;
            CurrentDays = 1;
        }
        else
        {
            var last = LastCheckIn ?? StartDate.Value;
            int daysDiff = (now.Date - last.Date).Days;

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

        if (CurrentDays > LongestDays)
            LongestDays = CurrentDays;

        UpdatedAt = now;
    }


    public void Break()
    {
        CurrentDays = 0;
        StartDate = null;
        TotalBreaks++;
        LastBreakDate = DateTime.UtcNow;
        UpdatedAt = DateTime.UtcNow;
    }
}