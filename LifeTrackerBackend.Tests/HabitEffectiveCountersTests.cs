using LifeTracker.Constants;
using LifeTracker.Controllers;
using LifeTracker.Models;
using LifeTracker.Services.Time;

namespace LifeTrackerBackend.Tests;

public class HabitEffectiveCountersTests
{
    private static readonly HeroTimeService TimeService = new();
    private const string Utc = "UTC";

    private static GameTask BuildHabit(
        string? repeatPattern,
        DateTimeOffset lastCompletedAt,
        int completionCount = 3,
        int failCount = 2)
    {
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
            CreatedAt = lastCompletedAt,
            UpdatedAt = lastCompletedAt,
        };
    }

    private static void AssertNoMutation(GameTask task, int originalCompletionCount, int originalFailCount, DateTimeOffset originalUpdatedAt, DateTimeOffset? originalLastCompletedAt)
    {
        Assert.Equal(originalCompletionCount, task.CompletionCount);
        Assert.Equal(originalFailCount, task.FailCount);
        Assert.Equal(originalUpdatedAt, task.UpdatedAt);
        Assert.Equal(originalLastCompletedAt, task.LastCompletedAt);
    }

    [Fact]
    public void Effective_DailyBucketUnchanged_ReturnsRawCounters()
    {
        var today = new DateOnly(2026, 4, 26);
        var task = BuildHabit("RESET:DAILY", new DateTimeOffset(2026, 4, 26, 9, 0, 0, TimeSpan.Zero));
        var origCompletion = task.CompletionCount;
        var origFail = task.FailCount;
        var origUpdated = task.UpdatedAt;
        var origLast = task.LastCompletedAt;

        var (completion, fail) = TaskController.GetEffectiveHabitCounters(task, today, Utc, TimeService.GetLocalDate);

        Assert.Equal(3, completion);
        Assert.Equal(2, fail);
        AssertNoMutation(task, origCompletion, origFail, origUpdated, origLast);
    }

    [Fact]
    public void Effective_DailyBucketChanged_ReturnsZerosWithoutMutating()
    {
        var today = new DateOnly(2026, 4, 27);
        var task = BuildHabit("RESET:DAILY", new DateTimeOffset(2026, 4, 26, 9, 0, 0, TimeSpan.Zero));
        var origCompletion = task.CompletionCount;
        var origFail = task.FailCount;
        var origUpdated = task.UpdatedAt;
        var origLast = task.LastCompletedAt;

        var (completion, fail) = TaskController.GetEffectiveHabitCounters(task, today, Utc, TimeService.GetLocalDate);

        Assert.Equal(0, completion);
        Assert.Equal(0, fail);
        AssertNoMutation(task, origCompletion, origFail, origUpdated, origLast);
    }

    [Fact]
    public void Effective_WeeklySameIsoWeek_ReturnsRawCounters()
    {
        // 2026-04-20 (Mon) and 2026-04-26 (Sun) belong to the same ISO week.
        var today = new DateOnly(2026, 4, 26);
        var task = BuildHabit("RESET:WEEKLY", new DateTimeOffset(2026, 4, 20, 12, 0, 0, TimeSpan.Zero));
        var origCompletion = task.CompletionCount;
        var origFail = task.FailCount;
        var origUpdated = task.UpdatedAt;
        var origLast = task.LastCompletedAt;

        var (completion, fail) = TaskController.GetEffectiveHabitCounters(task, today, Utc, TimeService.GetLocalDate);

        Assert.Equal(3, completion);
        Assert.Equal(2, fail);
        AssertNoMutation(task, origCompletion, origFail, origUpdated, origLast);
    }

    [Fact]
    public void Effective_WeeklyNewIsoWeek_ReturnsZerosWithoutMutating()
    {
        var today = new DateOnly(2026, 4, 27);
        var task = BuildHabit("RESET:WEEKLY", new DateTimeOffset(2026, 4, 26, 12, 0, 0, TimeSpan.Zero));
        var origCompletion = task.CompletionCount;
        var origFail = task.FailCount;
        var origUpdated = task.UpdatedAt;
        var origLast = task.LastCompletedAt;

        var (completion, fail) = TaskController.GetEffectiveHabitCounters(task, today, Utc, TimeService.GetLocalDate);

        Assert.Equal(0, completion);
        Assert.Equal(0, fail);
        AssertNoMutation(task, origCompletion, origFail, origUpdated, origLast);
    }

    [Fact]
    public void Effective_MonthlySameMonth_ReturnsRawCounters()
    {
        var today = new DateOnly(2026, 4, 30);
        var task = BuildHabit("RESET:MONTHLY", new DateTimeOffset(2026, 4, 1, 12, 0, 0, TimeSpan.Zero));
        var origCompletion = task.CompletionCount;
        var origFail = task.FailCount;
        var origUpdated = task.UpdatedAt;
        var origLast = task.LastCompletedAt;

        var (completion, fail) = TaskController.GetEffectiveHabitCounters(task, today, Utc, TimeService.GetLocalDate);

        Assert.Equal(3, completion);
        Assert.Equal(2, fail);
        AssertNoMutation(task, origCompletion, origFail, origUpdated, origLast);
    }

    [Fact]
    public void Effective_MonthlyNewMonth_ReturnsZerosWithoutMutating()
    {
        var today = new DateOnly(2026, 5, 1);
        var task = BuildHabit("RESET:MONTHLY", new DateTimeOffset(2026, 4, 30, 23, 0, 0, TimeSpan.Zero));
        var origCompletion = task.CompletionCount;
        var origFail = task.FailCount;
        var origUpdated = task.UpdatedAt;
        var origLast = task.LastCompletedAt;

        var (completion, fail) = TaskController.GetEffectiveHabitCounters(task, today, Utc, TimeService.GetLocalDate);

        Assert.Equal(0, completion);
        Assert.Equal(0, fail);
        AssertNoMutation(task, origCompletion, origFail, origUpdated, origLast);
    }

    [Fact]
    public void Effective_NullPattern_ReturnsRawCounters()
    {
        var today = new DateOnly(2026, 5, 1);
        var task = BuildHabit(null, new DateTimeOffset(2025, 1, 1, 0, 0, 0, TimeSpan.Zero));
        var origCompletion = task.CompletionCount;
        var origFail = task.FailCount;
        var origUpdated = task.UpdatedAt;
        var origLast = task.LastCompletedAt;

        var (completion, fail) = TaskController.GetEffectiveHabitCounters(task, today, Utc, TimeService.GetLocalDate);

        Assert.Equal(3, completion);
        Assert.Equal(2, fail);
        AssertNoMutation(task, origCompletion, origFail, origUpdated, origLast);
    }

    [Fact]
    public void Effective_LegacyNonResetPattern_ReturnsRawCounters()
    {
        var today = new DateOnly(2026, 5, 1);
        var task = BuildHabit("DAILY:1", new DateTimeOffset(2025, 1, 1, 0, 0, 0, TimeSpan.Zero));
        var origCompletion = task.CompletionCount;
        var origFail = task.FailCount;
        var origUpdated = task.UpdatedAt;
        var origLast = task.LastCompletedAt;

        var (completion, fail) = TaskController.GetEffectiveHabitCounters(task, today, Utc, TimeService.GetLocalDate);

        Assert.Equal(3, completion);
        Assert.Equal(2, fail);
        AssertNoMutation(task, origCompletion, origFail, origUpdated, origLast);
    }

    [Fact]
    public void Effective_UnknownPeriodToken_ReturnsRawCounters()
    {
        var today = new DateOnly(2026, 5, 1);
        var task = BuildHabit("RESET:YEARLY", new DateTimeOffset(2025, 1, 1, 0, 0, 0, TimeSpan.Zero));
        var origCompletion = task.CompletionCount;
        var origFail = task.FailCount;
        var origUpdated = task.UpdatedAt;
        var origLast = task.LastCompletedAt;

        var (completion, fail) = TaskController.GetEffectiveHabitCounters(task, today, Utc, TimeService.GetLocalDate);

        Assert.Equal(3, completion);
        Assert.Equal(2, fail);
        AssertNoMutation(task, origCompletion, origFail, origUpdated, origLast);
    }

    [Fact]
    public void Effective_NonHabitTask_ReturnsRawCounters()
    {
        var today = new DateOnly(2026, 5, 1);
        var task = BuildHabit("RESET:DAILY", new DateTimeOffset(2025, 1, 1, 0, 0, 0, TimeSpan.Zero));
        task.Type = TaskType.Daily;
        var origCompletion = task.CompletionCount;
        var origFail = task.FailCount;
        var origUpdated = task.UpdatedAt;
        var origLast = task.LastCompletedAt;

        var (completion, fail) = TaskController.GetEffectiveHabitCounters(task, today, Utc, TimeService.GetLocalDate);

        Assert.Equal(3, completion);
        Assert.Equal(2, fail);
        AssertNoMutation(task, origCompletion, origFail, origUpdated, origLast);
    }

    [Fact]
    public void Effective_NoLastCompletedAt_UsesUpdatedAtAsAnchor()
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
        var origCompletion = task.CompletionCount;
        var origFail = task.FailCount;
        var origUpdated = task.UpdatedAt;

        var (completion, fail) = TaskController.GetEffectiveHabitCounters(task, today, Utc, TimeService.GetLocalDate);

        Assert.Equal(0, completion);
        Assert.Equal(0, fail);
        // Read-only path must not touch the entity even when the bucket has rolled over.
        Assert.Equal(origCompletion, task.CompletionCount);
        Assert.Equal(origFail, task.FailCount);
        Assert.Equal(origUpdated, task.UpdatedAt);
        Assert.Null(task.LastCompletedAt);
    }
}
