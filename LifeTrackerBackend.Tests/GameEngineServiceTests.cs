using LifeTracker.Constants;
using LifeTracker.Models;
using LifeTracker.Services;
using Xunit;

namespace LifeTrackerBackend.Tests;

public class GameEngineServiceTests
{
    [Fact]
    public void ApplyTaskFailure_ManualFailWithActiveShieldAndBreakCandidate_AbsorbsBreakKeepsBasePenaltiesAndConsumesShield()
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
            Gold = 50,
            IsShieldActive = true,
            ShieldActivatedAtUtc = DateTimeOffset.UtcNow.AddMinutes(-1)
        };
        var streak = new Streak
        {
            CurrentDays = 5
        };
        var economy = new EconomyBalance();

        var expectedHpAfterBasePenalty = hero.CurrentHp - task.GetHpPenalty();
        var expectedGoldAfterBasePenalty = hero.Gold - task.GetGoldPenalty();

        var result = service.ApplyTaskFailure(task, hero, streak, economy, DateOnly.FromDateTime(DateTime.UtcNow));

        Assert.True(result.ShieldAbsorbed);
        Assert.False(result.StreakBroken);
        Assert.Equal(5, streak.CurrentDays);
        Assert.Equal(expectedHpAfterBasePenalty, hero.CurrentHp);
        Assert.Equal(expectedGoldAfterBasePenalty, hero.Gold);
        Assert.False(hero.IsShieldActive);
        Assert.Null(hero.ShieldActivatedAtUtc);
    }

    [Fact]
    public void ApplyTaskFailure_ManualFailWithActiveShieldButNoBreakCandidate_LeavesShieldActive()
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
            Gold = 50,
            IsShieldActive = true,
            ShieldActivatedAtUtc = DateTimeOffset.UtcNow.AddMinutes(-1)
        };
        var streak = new Streak
        {
            CurrentDays = 0
        };
        var economy = new EconomyBalance();

        var result = service.ApplyTaskFailure(task, hero, streak, economy, DateOnly.FromDateTime(DateTime.UtcNow));

        Assert.False(result.ShieldAbsorbed);
        Assert.False(result.StreakBroken);
        Assert.True(hero.IsShieldActive);
        Assert.NotNull(hero.ShieldActivatedAtUtc);
    }

    [Fact]
    public void CheckOverdueTasks_WithMultipleStreakBreaks_UsesOneShieldForWholeBatchThenConsumesIt()
    {
        var service = new GameEngineService();
        var hero = new Hero
        {
            CurrentHp = 100,
            MaxHp = 100,
            Gold = 100,
            IsShieldActive = true,
            ShieldActivatedAtUtc = DateTimeOffset.UtcNow.AddMinutes(-1)
        };
        var economy = new EconomyBalance();
        // Daily tasks are now handled by the controller-level missed-day pipeline; this
        // test exercises the GameEngineService.CheckOverdueTasks single-shot path used for
        // OneTime tasks, which is still selected by IsOverdue().
        var tasks = new List<GameTask>
        {
            new()
            {
                Id = 1,
                IsActive = true,
                IsCompleted = false,
                DueDate = DateTimeOffset.UtcNow.AddDays(-1),
                Type = TaskType.OneTime,
                Difficulty = TaskDifficulty.Easy,
                Title = "OneTime 1"
            },
            new()
            {
                Id = 2,
                IsActive = true,
                IsCompleted = false,
                DueDate = DateTimeOffset.UtcNow.AddDays(-1),
                Type = TaskType.OneTime,
                Difficulty = TaskDifficulty.Easy,
                Title = "OneTime 2"
            }
        };
        var streaks = new List<Streak>
        {
            new() { TaskId = 1, CurrentDays = 4 },
            new() { TaskId = 2, CurrentDays = 7 }
        };

        service.CheckOverdueTasks(tasks, hero, streaks, economy);

        Assert.Equal(4, streaks[0].CurrentDays);
        Assert.Equal(7, streaks[1].CurrentDays);
        Assert.False(hero.IsShieldActive);
        Assert.Null(hero.ShieldActivatedAtUtc);
    }

    [Fact]
    public void ApplyTaskFailure_AfterShieldConsumption_BreaksNormally()
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
            Gold = 50,
            IsShieldActive = false
        };
        var streak = new Streak
        {
            CurrentDays = 5
        };
        var economy = new EconomyBalance();

        var result = service.ApplyTaskFailure(task, hero, streak, economy, DateOnly.FromDateTime(DateTime.UtcNow));

        Assert.False(result.ShieldAbsorbed);
        Assert.True(result.StreakBroken);
        Assert.Equal(0, streak.CurrentDays);
        Assert.NotNull(result.Penalty);
    }
}
