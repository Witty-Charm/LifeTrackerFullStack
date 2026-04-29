using System.Globalization;
using LifeTracker.Constants;
using LifeTracker.Controllers;
using LifeTracker.Data;
using LifeTracker.Models;
using LifeTracker.Services;
using LifeTracker.Services.Time;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Data.Sqlite;
using Microsoft.EntityFrameworkCore;

namespace LifeTrackerBackend.Tests;

public class TaskControllerDailyScheduleTests : IAsyncLifetime
{
    private const string TestDeviceId = "11111111-1111-1111-1111-111111111111";
    private const int TestUserId = 1;
    private readonly SqliteConnection _connection = new("Data Source=:memory:");
    private DbContextOptions<ApplicationDbContext> _options = null!;

    public async Task InitializeAsync()
    {
        await _connection.OpenAsync();
        _options = new DbContextOptionsBuilder<ApplicationDbContext>()
            .UseSqlite(_connection)
            .Options;
        await using var db = CreateDbContext();
        await db.Database.EnsureCreatedAsync();
        db.Users.Add(new User { Id = TestUserId, Provider = AuthProvider.Google, ExternalId = "ext-" + TestUserId, Email = "user@example.com" });
        await db.SaveChangesAsync();
    }

    public async Task DisposeAsync() => await _connection.DisposeAsync();

    [Fact]
    public async Task PostTask_Daily_InitializesLastMissedScheduledLocalDateToDayBeforeStart()
    {
        await using var db = CreateDbContext();
        var hero = await CreateHeroAsync(db);
        var startUtc = new DateTimeOffset(2026, 4, 24, 10, 0, 0, TimeSpan.Zero);
        var time = new FixedHeroTimeService(startUtc);
        var controller = CreateController(db, time);

        var actionResult = await controller.PostTask(new CreateTaskRequest
        {
            HeroId = hero.Id,
            Title = "Daily 1",
            Type = TaskType.Daily,
            Difficulty = TaskDifficulty.Easy,
            DueDate = startUtc,
            RepeatPattern = "DAILY:1",
        });

        var created = Assert.IsType<CreatedAtActionResult>(actionResult.Result);
        var dto = Assert.IsType<TaskDto>(created.Value);
        var stored = await db.GameTasks.SingleAsync(t => t.Id == dto.Id);

        Assert.Equal("2026-04-23", stored.LastMissedScheduledLocalDate);
    }

    [Fact]
    public async Task PutTask_DailyChangingDueDate_ResetsCursorToDayBeforeNewStart()
    {
        await using var db = CreateDbContext();
        var hero = await CreateHeroAsync(db);
        var originalUtc = new DateTimeOffset(2026, 4, 24, 10, 0, 0, TimeSpan.Zero);
        var time = new FixedHeroTimeService(originalUtc);
        var controller = CreateController(db, time);

        var createResult = await controller.PostTask(new CreateTaskRequest
        {
            HeroId = hero.Id,
            Title = "Movable daily",
            Type = TaskType.Daily,
            Difficulty = TaskDifficulty.Easy,
            DueDate = originalUtc,
            RepeatPattern = "DAILY:1",
        });
        var dto = Assert.IsType<TaskDto>(((CreatedAtActionResult)createResult.Result!).Value);

        var newStartUtc = new DateTimeOffset(2026, 5, 1, 10, 0, 0, TimeSpan.Zero);
        var updateResult = await controller.UpdateTask(dto.Id, new UpdateTaskRequest
        {
            Title = "Movable daily",
            Difficulty = TaskDifficulty.Easy,
            DueDate = newStartUtc,
            RepeatPattern = "DAILY:1",
        });
        Assert.IsType<OkObjectResult>(updateResult.Result);

        var stored = await db.GameTasks.SingleAsync(t => t.Id == dto.Id);
        Assert.Equal("2026-04-30", stored.LastMissedScheduledLocalDate);
    }

    [Fact]
    public async Task SetDailyTaskState_NotScheduledToday_ReturnsBadRequest()
    {
        await using var db = CreateDbContext();
        var startUtc = new DateTimeOffset(2026, 4, 24, 12, 0, 0, TimeSpan.Zero);
        var hero = await CreateHeroAsync(db, createdAt: startUtc);

        var task = new GameTask
        {
            HeroId = hero.Id,
            Title = "3-day daily",
            Type = TaskType.Daily,
            Difficulty = TaskDifficulty.Easy,
            Polarity = HabitPolarity.Both,
            IsActive = true,
            DueDate = startUtc,
            RepeatPattern = "DAILY:3",
            LastMissedScheduledLocalDate = "2026-04-23",
            CreatedAt = startUtc,
            UpdatedAt = startUtc,
        };
        db.GameTasks.Add(task);
        await db.SaveChangesAsync();

        var todayUtc = startUtc.AddDays(1);
        var controller = CreateController(db, new FixedHeroTimeService(todayUtc));

        var result = await controller.SetDailyTaskState(task.Id, new SetDailyTaskStateRequest
        {
            LocalDate = "2026-04-25",
            IsChecked = true,
        });

        var bad = Assert.IsType<BadRequestObjectResult>(result.Result);
        Assert.NotNull(bad.Value);
    }

    [Fact]
    public async Task SetDailyTaskState_ScheduledToday_AcceptsCheck()
    {
        await using var db = CreateDbContext();
        var startUtc = new DateTimeOffset(2026, 4, 24, 12, 0, 0, TimeSpan.Zero);
        var hero = await CreateHeroAsync(db, createdAt: startUtc);

        var task = new GameTask
        {
            HeroId = hero.Id,
            Title = "Every-day daily",
            Type = TaskType.Daily,
            Difficulty = TaskDifficulty.Easy,
            Polarity = HabitPolarity.Both,
            IsActive = true,
            DueDate = startUtc,
            RepeatPattern = "DAILY:1",
            LastMissedScheduledLocalDate = "2026-04-23",
            CreatedAt = startUtc,
            UpdatedAt = startUtc,
        };
        db.GameTasks.Add(task);
        await db.SaveChangesAsync();
        db.Streaks.Add(new Streak
        {
            HeroId = hero.Id, TaskId = task.Id, CurrentDays = 0, LongestDays = 0,
            CreatedAt = startUtc, UpdatedAt = startUtc,
        });
        await db.SaveChangesAsync();

        var controller = CreateController(db, new FixedHeroTimeService(startUtc));
        var result = await controller.SetDailyTaskState(task.Id, new SetDailyTaskStateRequest
        {
            LocalDate = "2026-04-24",
            IsChecked = true,
        });

        var ok = Assert.IsType<OkObjectResult>(result.Result);
        var resp = Assert.IsType<SetDailyTaskStateResponse>(ok.Value);
        Assert.True(resp.IsChecked);
    }

    [Fact]
    public async Task CheckOverdueTasks_LegacyDailyWithNullCursor_InitializesWithoutBackfill()
    {
        await using var db = CreateDbContext();
        var startUtc = new DateTimeOffset(2026, 1, 1, 12, 0, 0, TimeSpan.Zero);
        var hero = await CreateHeroAsync(db, createdAt: startUtc);

        var task = new GameTask
        {
            HeroId = hero.Id,
            Title = "Legacy daily",
            Type = TaskType.Daily,
            Difficulty = TaskDifficulty.Easy,
            Polarity = HabitPolarity.Both,
            IsActive = true,
            DueDate = startUtc,
            RepeatPattern = "DAILY:1",
            LastMissedScheduledLocalDate = null,
            CreatedAt = startUtc,
            UpdatedAt = startUtc,
        };
        db.GameTasks.Add(task);
        await db.SaveChangesAsync();
        db.Streaks.Add(new Streak
        {
            HeroId = hero.Id, TaskId = task.Id, CurrentDays = 0, LongestDays = 0,
            CreatedAt = startUtc, UpdatedAt = startUtc,
        });
        await db.SaveChangesAsync();

        var checkUtc = startUtc.AddDays(30);
        var controller = CreateController(db, new FixedHeroTimeService(checkUtc));
        var result = await controller.CheckOverdueTasks(hero.Id);
        var ok = Assert.IsType<OkObjectResult>(result.Result);
        var resp = Assert.IsType<OverdueCheckResponse>(ok.Value);
        var stored = await db.GameTasks.SingleAsync(t => t.Id == task.Id);
        var heroAfter = await db.Heroes.SingleAsync(h => h.Id == hero.Id);

        Assert.Equal(0, resp.OverdueCount);
        Assert.Empty(resp.Penalties ?? new List<OverdueTaskPenalty>());
        Assert.Equal("2026-01-30", stored.LastMissedScheduledLocalDate);
        Assert.Equal(100, heroAfter.CurrentHp);
        _ = heroAfter;
    }

    [Fact]
    public async Task CheckOverdueTasks_DailyMissedThreeDays_AppliesThreePenaltiesAndAdvancesCursor()
    {
        await using var db = CreateDbContext();
        var startUtc = new DateTimeOffset(2026, 4, 20, 12, 0, 0, TimeSpan.Zero);
        var hero = await CreateHeroAsync(db, createdAt: startUtc);
        hero.CurrentHp = 100_000;
        hero.MaxHp = 100_000;
        await db.SaveChangesAsync();

        var task = new GameTask
        {
            HeroId = hero.Id,
            Title = "Streaky daily",
            Type = TaskType.Daily,
            Difficulty = TaskDifficulty.Easy,
            Polarity = HabitPolarity.Both,
            IsActive = true,
            DueDate = startUtc,
            RepeatPattern = "DAILY:1",
            LastMissedScheduledLocalDate = "2026-04-19",
            CreatedAt = startUtc,
            UpdatedAt = startUtc,
        };
        db.GameTasks.Add(task);
        await db.SaveChangesAsync();
        db.Streaks.Add(new Streak
        {
            HeroId = hero.Id, TaskId = task.Id, CurrentDays = 5, LongestDays = 5,
            CreatedAt = startUtc, UpdatedAt = startUtc,
        });
        await db.SaveChangesAsync();

        var checkUtc = new DateTimeOffset(2026, 4, 24, 12, 0, 0, TimeSpan.Zero);
        var controller = CreateController(db, new FixedHeroTimeService(checkUtc));

        var result = await controller.CheckOverdueTasks(hero.Id);
        var ok = Assert.IsType<OkObjectResult>(result.Result);
        var resp = Assert.IsType<OverdueCheckResponse>(ok.Value);
        var stored = await db.GameTasks.SingleAsync(t => t.Id == task.Id);
        var streakAfter = await db.Streaks.SingleAsync(s => s.TaskId == task.Id);

        Assert.Equal(4, resp.OverdueCount);
        Assert.Equal(4, resp.Penalties!.Count);
        Assert.Equal(0, streakAfter.CurrentDays);
        Assert.Single(resp.Penalties!, p => p.StreakBroken);
        Assert.Equal("2026-04-23", stored.LastMissedScheduledLocalDate);
    }

    [Fact]
    public async Task CheckOverdueTasks_RunTwiceSameDay_DoesNotDoublePenalize()
    {
        await using var db = CreateDbContext();
        var startUtc = new DateTimeOffset(2026, 4, 22, 12, 0, 0, TimeSpan.Zero);
        var hero = await CreateHeroAsync(db, createdAt: startUtc);
        hero.CurrentHp = 100_000; hero.MaxHp = 100_000;
        await db.SaveChangesAsync();

        var task = new GameTask
        {
            HeroId = hero.Id,
            Title = "Idempotent daily",
            Type = TaskType.Daily,
            Difficulty = TaskDifficulty.Easy,
            Polarity = HabitPolarity.Both,
            IsActive = true,
            DueDate = startUtc,
            RepeatPattern = "DAILY:1",
            LastMissedScheduledLocalDate = "2026-04-21",
            CreatedAt = startUtc,
            UpdatedAt = startUtc,
        };
        db.GameTasks.Add(task);
        await db.SaveChangesAsync();
        db.Streaks.Add(new Streak
        {
            HeroId = hero.Id, TaskId = task.Id, CurrentDays = 0, LongestDays = 0,
            CreatedAt = startUtc, UpdatedAt = startUtc,
        });
        await db.SaveChangesAsync();

        var checkUtc = new DateTimeOffset(2026, 4, 24, 12, 0, 0, TimeSpan.Zero);
        var controller = CreateController(db, new FixedHeroTimeService(checkUtc));

        var first = await controller.CheckOverdueTasks(hero.Id);
        var firstResp = Assert.IsType<OverdueCheckResponse>(((OkObjectResult)first.Result!).Value);
        Assert.Equal(2, firstResp.OverdueCount);

        var second = await controller.CheckOverdueTasks(hero.Id);
        var secondResp = Assert.IsType<OverdueCheckResponse>(((OkObjectResult)second.Result!).Value);
        Assert.Equal(0, secondResp.OverdueCount);
    }

    [Fact]
    public async Task CheckOverdueTasks_DailyInterval3_OnlyPenalizesScheduledMissedDays()
    {
        await using var db = CreateDbContext();
        var startUtc = new DateTimeOffset(2026, 4, 18, 12, 0, 0, TimeSpan.Zero);
        var hero = await CreateHeroAsync(db, createdAt: startUtc);
        hero.CurrentHp = 100_000; hero.MaxHp = 100_000;
        await db.SaveChangesAsync();

        var task = new GameTask
        {
            HeroId = hero.Id,
            Title = "Every 3 days",
            Type = TaskType.Daily,
            Difficulty = TaskDifficulty.Easy,
            Polarity = HabitPolarity.Both,
            IsActive = true,
            DueDate = startUtc,
            RepeatPattern = "DAILY:3",
            LastMissedScheduledLocalDate = "2026-04-17",
            CreatedAt = startUtc,
            UpdatedAt = startUtc,
        };
        db.GameTasks.Add(task);
        await db.SaveChangesAsync();
        db.Streaks.Add(new Streak
        {
            HeroId = hero.Id, TaskId = task.Id, CurrentDays = 0, LongestDays = 0,
            CreatedAt = startUtc, UpdatedAt = startUtc,
        });
        await db.SaveChangesAsync();

        var checkUtc = new DateTimeOffset(2026, 4, 25, 12, 0, 0, TimeSpan.Zero);
        var controller = CreateController(db, new FixedHeroTimeService(checkUtc));

        var result = await controller.CheckOverdueTasks(hero.Id);
        var resp = Assert.IsType<OverdueCheckResponse>(((OkObjectResult)result.Result!).Value);
        var stored = await db.GameTasks.SingleAsync(t => t.Id == task.Id);

        Assert.Equal(3, resp.OverdueCount);
        Assert.Equal("2026-04-24", stored.LastMissedScheduledLocalDate);
    }

    [Fact]
    public async Task CheckOverdueTasks_DailyWithCompletionOnMissedDay_SkipsThatDay()
    {
        await using var db = CreateDbContext();
        var startUtc = new DateTimeOffset(2026, 4, 22, 12, 0, 0, TimeSpan.Zero);
        var hero = await CreateHeroAsync(db, createdAt: startUtc);
        hero.CurrentHp = 100_000; hero.MaxHp = 100_000;
        await db.SaveChangesAsync();

        var task = new GameTask
        {
            HeroId = hero.Id,
            Title = "Partially completed",
            Type = TaskType.Daily,
            Difficulty = TaskDifficulty.Easy,
            Polarity = HabitPolarity.Both,
            IsActive = true,
            DueDate = startUtc,
            RepeatPattern = "DAILY:1",
            LastMissedScheduledLocalDate = "2026-04-21",
            CreatedAt = startUtc,
            UpdatedAt = startUtc,
        };
        db.GameTasks.Add(task);
        await db.SaveChangesAsync();
        db.DailyTaskCompletions.Add(new DailyTaskCompletion
        {
            HeroId = hero.Id,
            TaskId = task.Id,
            LocalDate = "2026-04-23",
            IsChecked = true,
        });
        db.Streaks.Add(new Streak
        {
            HeroId = hero.Id, TaskId = task.Id, CurrentDays = 0, LongestDays = 0,
            CreatedAt = startUtc, UpdatedAt = startUtc,
        });
        await db.SaveChangesAsync();

        var checkUtc = new DateTimeOffset(2026, 4, 25, 12, 0, 0, TimeSpan.Zero);
        var controller = CreateController(db, new FixedHeroTimeService(checkUtc));

        var result = await controller.CheckOverdueTasks(hero.Id);
        var resp = Assert.IsType<OverdueCheckResponse>(((OkObjectResult)result.Result!).Value);
        Assert.Equal(2, resp.OverdueCount);
    }

    [Fact]
    public async Task GetTask_DailyWithInterval3_DtoExposesScheduleFields()
    {
        await using var db = CreateDbContext();
        var startUtc = new DateTimeOffset(2026, 4, 24, 10, 0, 0, TimeSpan.Zero);
        var hero = await CreateHeroAsync(db, createdAt: startUtc);

        var task = new GameTask
        {
            HeroId = hero.Id,
            Title = "Schedule DTO",
            Type = TaskType.Daily,
            Difficulty = TaskDifficulty.Easy,
            Polarity = HabitPolarity.Both,
            IsActive = true,
            DueDate = startUtc,
            RepeatPattern = "DAILY:3",
            LastMissedScheduledLocalDate = "2026-04-23",
            CreatedAt = startUtc,
            UpdatedAt = startUtc,
        };
        db.GameTasks.Add(task);
        await db.SaveChangesAsync();

        var queryUtc = new DateTimeOffset(2026, 4, 25, 10, 0, 0, TimeSpan.Zero);
        var controller = CreateController(db, new FixedHeroTimeService(queryUtc));

        var result = await controller.GetTask(task.Id);
        var ok = Assert.IsType<OkObjectResult>(result.Result);
        var dto = Assert.IsType<TaskDto>(ok.Value);

        Assert.False(dto.IsScheduledToday);
        Assert.Equal("2026-04-27", dto.NextScheduledLocalDate);
    }

    private static TaskController CreateController(ApplicationDbContext db, IHeroTimeService timeService)
    {
        var controller = TaskController.CreateForTests(db, new GameEngineService(), timeService);
        controller.ControllerContext = new ControllerContext

        {

            HttpContext = TestAuthHelpers.CreateAuthenticatedHttpContext(TestUserId, TestDeviceId)

        };
        return controller;
    }

    private static async Task<Hero> CreateHeroAsync(ApplicationDbContext db, string timeZoneId = "UTC", DateTimeOffset? createdAt = null)
    {
        var heroCreatedAt = createdAt ?? DateTimeOffset.UtcNow;
        var hero = new Hero
        {
            Name = "Alex",
            Gold = 0,
            Level = 1,
            CurrentHp = 100,
            MaxHp = 100,
            TimeZoneId = timeZoneId,
            OwnerDeviceId = TestDeviceId,
            UserId = TestUserId,
            CreatedDate = heroCreatedAt,
            UpdatedAt = heroCreatedAt,
        };
        db.Heroes.Add(hero);
        await db.SaveChangesAsync();

        db.EconomyBalances.Add(new EconomyBalance
        {
            HeroId = hero.Id,
            TotalGoldEarned = 0,
            LastDailyResetLocalDate = "2026-04-20",
            MaxDailyCompletions = GameConstants.DailyTaskCap,
            CreatedAt = heroCreatedAt,
            UpdatedAt = heroCreatedAt,
        });
        await db.SaveChangesAsync();
        return hero;
    }

    private ApplicationDbContext CreateDbContext() => new(_options);

    private sealed class FixedHeroTimeService(DateTimeOffset utcNow) : IHeroTimeService
    {
        private readonly HeroTimeService _inner = new();

        public string NormalizeOrDefault(string? candidateTimeZoneId, string fallback = "UTC") =>
            _inner.NormalizeOrDefault(candidateTimeZoneId, fallback);

        public bool IsValidIana(string timeZoneId) => _inner.IsValidIana(timeZoneId);

        public DateOnly GetLocalDate(DateTimeOffset utcNowValue, string timeZoneId) =>
            _inner.GetLocalDate(ShouldUseFixedNow(utcNowValue) ? utcNow : utcNowValue, timeZoneId);

        public DateTimeOffset GetNextLocalMidnightUtc(DateTimeOffset utcNowValue, string timeZoneId) =>
            _inner.GetNextLocalMidnightUtc(ShouldUseFixedNow(utcNowValue) ? utcNow : utcNowValue, timeZoneId);

        public string FormatLocalDate(DateOnly localDate) => _inner.FormatLocalDate(localDate);

        public DateOnly ParseLocalDate(string value) => _inner.ParseLocalDate(value);

        public string ResolveEffectiveTimeZone(Hero hero, DateTimeOffset utcNowValue) =>
            _inner.ResolveEffectiveTimeZone(hero, ShouldUseFixedNow(utcNowValue) ? utcNow : utcNowValue);

        public bool CanSwitchTimeZone(Hero hero, DateTimeOffset utcNowValue) =>
            _inner.CanSwitchTimeZone(hero, ShouldUseFixedNow(utcNowValue) ? utcNow : utcNowValue);

        private static bool ShouldUseFixedNow(DateTimeOffset utcNowValue) =>
            Math.Abs((utcNowValue - DateTimeOffset.UtcNow).TotalMinutes) < 5;
    }
}
