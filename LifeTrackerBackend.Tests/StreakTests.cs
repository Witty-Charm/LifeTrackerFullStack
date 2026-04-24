using LifeTracker.Models;

namespace LifeTrackerBackend.Tests;

public class StreakTests
{
    [Fact]
    public void RegisterSuccess_WithSeededAnchorOnPreviousLocalDay_IncrementsSeededStreakOnFirstCompletion()
    {
        var createdLocalDate = DateOnly.FromDateTime(DateTime.UtcNow);
        var now = DateTimeOffset.UtcNow;
        var streak = new Streak
        {
            CurrentDays = 30,
            LongestDays = 30,
            LastCheckInLocalDate = createdLocalDate.AddDays(-1).ToString("yyyy-MM-dd")
        };

        streak.RegisterSuccess(createdLocalDate, now);

        Assert.Equal(31, streak.CurrentDays);
        Assert.Equal(31, streak.LongestDays);
        Assert.Equal(createdLocalDate.ToString("yyyy-MM-dd"), streak.LastCheckInLocalDate);
    }

    [Fact]
    public void RegisterSuccess_WithoutSeededAnchor_LeavesCreationStateEmptyAndStartsAtOneOnFirstCompletion()
    {
        var createdLocalDate = DateOnly.FromDateTime(DateTime.UtcNow);
        var now = DateTimeOffset.UtcNow;
        var streak = new Streak
        {
            CurrentDays = 0,
            LongestDays = 0,
            LastCheckInLocalDate = null
        };

        Assert.True(string.IsNullOrWhiteSpace(streak.LastCheckInLocalDate));

        streak.RegisterSuccess(createdLocalDate, now);

        Assert.Equal(1, streak.CurrentDays);
        Assert.Equal(1, streak.LongestDays);
        Assert.Equal(createdLocalDate.ToString("yyyy-MM-dd"), streak.LastCheckInLocalDate);
    }
}
