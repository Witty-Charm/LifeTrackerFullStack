using LifeTracker.Constants;
using LifeTracker.Controllers;
using LifeTracker.Models;
using LifeTracker.Services.Time;

namespace LifeTrackerBackend.Tests;

public class HabitResetCounterTests
{
    private static readonly HeroTimeService TimeService = new();
    private const string Utc = "UTC";

    private static GameTask BuildHabit(
        string? repeatPattern,
        DateTimeOffset lastCompletedAt,
        int completionCount = 3,
        int failCount = 2)
    {
        var activity = lastCompletedAt;
        return new GameTask
        {
            Id = 1,
            HeroId = 1,
            Type = TaskType.Habit,
            Polarity = HabitPolarity.Both,
            Difficulty = TaskDifficulty.Easy,
            RepeatPattern = repeatPattern,
            CompletionCount = completionCount,
            FailCount = failCount,
            LastCompletedAt = lastCompletedAt,
            CreatedAt = activity,
            UpdatedAt = activity,
        };
    }

    [Fact]
    public void DailyReset_BucketUnchanged_LeavesCountersIntact()
    {
        var today = new DateOnly(2026, 4, 26);
        var task = BuildHabit("RESET:DAILY", new DateTimeOffset(2026, 4, 26, 9, 0, 0, TimeSpan.Zero));

        TaskController.ResetHabitCounterIfNeeded(task, today, Utc, TimeService.GetLocalDate);

        Assert.Equal(3, task.CompletionCount);
        Assert.Equal(2, task.FailCount);
    }

    [Fact]
    public void DailyReset_BucketChanged_ResetsCountersToZero()
    {
        var today = new DateOnly(2026, 4, 27);
        var task = BuildHabit("RESET:DAILY", new DateTimeOffset(2026, 4, 26, 9, 0, 0, TimeSpan.Zero));

        TaskController.ResetHabitCounterIfNeeded(task, today, Utc, TimeService.GetLocalDate);

        Assert.Equal(0, task.CompletionCount);
        Assert.Equal(0, task.FailCount);
    }

    [Fact]
    public void WeeklyReset_SameIsoWeek_LeavesCountersIntact()
    {
        // 2026-04-20 (Mon) and 2026-04-26 (Sun) belong to the same ISO week.
        var today = new DateOnly(2026, 4, 26);
        var task = BuildHabit("RESET:WEEKLY", new DateTimeOffset(2026, 4, 20, 12, 0, 0, TimeSpan.Zero));

        TaskController.ResetHabitCounterIfNeeded(task, today, Utc, TimeService.GetLocalDate);

        Assert.Equal(3, task.CompletionCount);
        Assert.Equal(2, task.FailCount);
    }

    [Fact]
    public void WeeklyReset_NewIsoWeek_ResetsCountersToZero()
    {
        // 2026-04-26 (Sun, ISO week 17) vs 2026-04-27 (Mon, ISO week 18).
        var today = new DateOnly(2026, 4, 27);
        var task = BuildHabit("RESET:WEEKLY", new DateTimeOffset(2026, 4, 26, 12, 0, 0, TimeSpan.Zero));

        TaskController.ResetHabitCounterIfNeeded(task, today, Utc, TimeService.GetLocalDate);

        Assert.Equal(0, task.CompletionCount);
        Assert.Equal(0, task.FailCount);
    }

    [Fact]
    public void MonthlyReset_SameMonth_LeavesCountersIntact()
    {
        var today = new DateOnly(2026, 4, 30);
        var task = BuildHabit("RESET:MONTHLY", new DateTimeOffset(2026, 4, 1, 12, 0, 0, TimeSpan.Zero));

        TaskController.ResetHabitCounterIfNeeded(task, today, Utc, TimeService.GetLocalDate);

        Assert.Equal(3, task.CompletionCount);
        Assert.Equal(2, task.FailCount);
    }

    [Fact]
    public void MonthlyReset_NewMonth_ResetsCountersToZero()
    {
        var today = new DateOnly(2026, 5, 1);
        var task = BuildHabit("RESET:MONTHLY", new DateTimeOffset(2026, 4, 30, 23, 0, 0, TimeSpan.Zero));

        TaskController.ResetHabitCounterIfNeeded(task, today, Utc, TimeService.GetLocalDate);

        Assert.Equal(0, task.CompletionCount);
        Assert.Equal(0, task.FailCount);
    }

    [Fact]
    public void NullRepeatPattern_NoOp()
    {
        var today = new DateOnly(2026, 5, 1);
        var task = BuildHabit(null, new DateTimeOffset(2025, 1, 1, 0, 0, 0, TimeSpan.Zero));

        TaskController.ResetHabitCounterIfNeeded(task, today, Utc, TimeService.GetLocalDate);

        Assert.Equal(3, task.CompletionCount);
        Assert.Equal(2, task.FailCount);
    }

    [Fact]
    public void LegacyNonResetPattern_NoOp()
    {
        var today = new DateOnly(2026, 5, 1);
        var task = BuildHabit("DAILY:1", new DateTimeOffset(2025, 1, 1, 0, 0, 0, TimeSpan.Zero));

        TaskController.ResetHabitCounterIfNeeded(task, today, Utc, TimeService.GetLocalDate);

        Assert.Equal(3, task.CompletionCount);
        Assert.Equal(2, task.FailCount);
    }

    [Fact]
    public void UnknownPeriodToken_NoOp()
    {
        var today = new DateOnly(2026, 5, 1);
        var task = BuildHabit("RESET:YEARLY", new DateTimeOffset(2025, 1, 1, 0, 0, 0, TimeSpan.Zero));

        TaskController.ResetHabitCounterIfNeeded(task, today, Utc, TimeService.GetLocalDate);

        Assert.Equal(3, task.CompletionCount);
        Assert.Equal(2, task.FailCount);
    }

    [Fact]
    public void NonHabitTask_NoOp()
    {
        var today = new DateOnly(2026, 5, 1);
        var task = BuildHabit("RESET:DAILY", new DateTimeOffset(2025, 1, 1, 0, 0, 0, TimeSpan.Zero));
        task.Type = TaskType.Daily;

        TaskController.ResetHabitCounterIfNeeded(task, today, Utc, TimeService.GetLocalDate);

        Assert.Equal(3, task.CompletionCount);
        Assert.Equal(2, task.FailCount);
    }

    [Fact]
    public void NoLastCompletedAt_UsesUpdatedAtAsAnchor()
    {
        var today = new DateOnly(2026, 4, 27);
        var task = new GameTask
        {
            Type = TaskType.Habit,
            RepeatPattern = "RESET:DAILY",
            CompletionCount = 5,
            FailCount = 1,
            LastCompletedAt = null,
            UpdatedAt = new DateTimeOffset(2026, 4, 26, 10, 0, 0, TimeSpan.Zero),
            CreatedAt = new DateTimeOffset(2026, 4, 26, 10, 0, 0, TimeSpan.Zero),
        };

        TaskController.ResetHabitCounterIfNeeded(task, today, Utc, TimeService.GetLocalDate);

        Assert.Equal(0, task.CompletionCount);
        Assert.Equal(0, task.FailCount);
    }
}
