using LifeTracker.Constants;

namespace LifeTracker.Models;

public class Streak
{
    public int Id { get; set; }
    public int HeroId { get; set; }
    public int? TaskId { get; set; }
    
    public int CurrentDays { get; set; }
    public int LongestDays { get; set; }
    public DateTime? StartDate { get; set; }
    public DateTime? LastCheckIn { get; set; }
    
    public int FreezeCharges { get; set; } = 0;
    public DateTime? FreezeActiveUntil { get; set; }
    public bool IsShieldActive { get; set; } = false;
    public DateTime? ShieldExpiresAt { get; set; }
    
    public int TotalBreaks { get; set; } = 0;
    public DateTime? LastBreakDate { get; set; }
    
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
    
    public Hero? Hero { get; set; }
    public GameTask? Task { get; set; }
    
    /// <summary>
    /// GDD Formula: 1 + log₂(streak_days + 1) × 0.15
    /// </summary>
    public double GetStreakMultiplier() => GameConstants.CalculateStreakMultiplier(CurrentDays);
    
    /// <summary>
    /// Get bonus XP percentage from streak.
    /// </summary>
    public int GetBonusXpPercent() => (int)((GetStreakMultiplier() - 1.0) * 100);

    /// <summary>
    /// GDD: streak_tier = floor(streak_days / 30) + 1
    /// Used for calculating freeze/shield costs.
    /// </summary>
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
                // Streak broken - reset
                CurrentDays = 1;
                StartDate = now;
            }
            // daysDiff == 0: same day, no change
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