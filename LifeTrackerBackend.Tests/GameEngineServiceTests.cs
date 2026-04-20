using LifeTracker.Constants;
using LifeTracker.Models;
using LifeTracker.Services;
using Xunit;

namespace LifeTrackerBackend.Tests;

public class GameEngineServiceTests
{
    [Fact]
    public void ApplyTaskFailure_ActiveShieldFirstFail_SetsShieldAbsorbedWithoutBreakingStreak()
    {
        var service = new GameEngineService();
        var task = new GameTask
        {
            Type = TaskType.Habit,
            Difficulty = TaskDifficulty.Easy,
            Title = "Test habit"
        };
        var hero = new Hero
        {
            CurrentHp = 100,
            MaxHp = 100,
            Gold = 50
        };
        var streak = new Streak
        {
            CurrentDays = 5,
            IsShieldActive = true,
            ShieldExpiresAtUtc = DateTimeOffset.UtcNow.AddHours(1),
            ShieldFailConsumed = false
        };
        var economy = new EconomyBalance();

        var result = service.ApplyTaskFailure(task, hero, streak, economy, DateOnly.FromDateTime(DateTime.UtcNow));

        Assert.True(result.ShieldAbsorbed);
        Assert.False(result.StreakBroken);
        Assert.True(streak.ShieldFailConsumed);
        Assert.Equal(5, streak.CurrentDays);
        Assert.True(result.HpLost > 0);
        Assert.True(result.GoldLost >= 0);
    }

    [Fact]
    public void ApplyTaskFailure_ConsumedShield_DoesNotSetShieldAbsorbedAndBreaksStreak()
    {
        var service = new GameEngineService();
        var task = new GameTask
        {
            Type = TaskType.Habit,
            Difficulty = TaskDifficulty.Easy,
            Title = "Test habit"
        };
        var hero = new Hero
        {
            CurrentHp = 100,
            MaxHp = 100,
            Gold = 50
        };
        var streak = new Streak
        {
            CurrentDays = 5,
            IsShieldActive = false,
            ShieldExpiresAtUtc = null,
            ShieldFailConsumed = true
        };
        var economy = new EconomyBalance();

        var result = service.ApplyTaskFailure(task, hero, streak, economy, DateOnly.FromDateTime(DateTime.UtcNow));

        Assert.False(result.ShieldAbsorbed);
        Assert.True(result.StreakBroken);
        Assert.Equal(0, streak.CurrentDays);
    }

    [Fact]
    public void ApplyTaskFailure_ExpiredShield_ClearsShieldAndDoesNotSetShieldAbsorbed()
    {
        var service = new GameEngineService();
        var task = new GameTask
        {
            Type = TaskType.Habit,
            Difficulty = TaskDifficulty.Easy,
            Title = "Test habit"
        };
        var hero = new Hero
        {
            CurrentHp = 100,
            MaxHp = 100,
            Gold = 50
        };
        var streak = new Streak
        {
            CurrentDays = 5,
            IsShieldActive = true,
            ShieldExpiresAtUtc = DateTimeOffset.UtcNow.AddMinutes(-1),
            ShieldFailConsumed = false,
            ShieldBackupCurrentDays = 5,
            ShieldBackupBreakAtUtc = DateTimeOffset.UtcNow.AddHours(-2)
        };
        var economy = new EconomyBalance();

        var result = service.ApplyTaskFailure(task, hero, streak, economy, DateOnly.FromDateTime(DateTime.UtcNow));

        Assert.False(result.ShieldAbsorbed);
        Assert.True(result.StreakBroken);
        Assert.False(streak.IsShieldActive);
        Assert.Null(streak.ShieldExpiresAtUtc);
        Assert.Equal(5, streak.ShieldBackupCurrentDays);
        Assert.NotNull(streak.ShieldBackupBreakAtUtc);
    }
}
