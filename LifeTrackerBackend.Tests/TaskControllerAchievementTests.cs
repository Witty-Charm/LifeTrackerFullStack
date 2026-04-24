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

public class TaskControllerAchievementTests : IAsyncLifetime
{
    private const string TestDeviceId = "11111111-1111-1111-1111-111111111111";
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
    }

    public async Task DisposeAsync()
    {
        await _connection.DisposeAsync();
    }

    [Fact]
    public async Task CompleteTask_TaskThresholdReached_AppliesAchievementRewardAndReturnsUnlockPayload()
    {
        await using var db = CreateDbContext();

        var hero = new Hero
        {
            Name = "Alex",
            Gold = 0,
            Level = 1,
            CurrentHp = 100,
            MaxHp = 100,
            TimeZoneId = "UTC",
            OwnerDeviceId = TestDeviceId,
        };
        db.Heroes.Add(hero);
        await db.SaveChangesAsync();

        db.EconomyBalances.Add(new EconomyBalance
        {
            HeroId = hero.Id,
            TotalGoldEarned = 0,
            LastDailyResetLocalDate = "2026-04-20",
            MaxDailyCompletions = GameConstants.DailyTaskCap
        });

        var task = new GameTask
        {
            HeroId = hero.Id,
            Title = "Threshold task",
            Type = TaskType.OneTime,
            Difficulty = TaskDifficulty.Easy,
            CompletionCount = 9,
            IsCompleted = false,
            IsActive = true,
        };
        db.GameTasks.Add(task);
        await db.SaveChangesAsync();

        var expectedBaseGold = task.GetGoldReward();
        var controller = CreateController(db);

        var actionResult = await controller.CompleteTask(task.Id);

        var ok = Assert.IsType<OkObjectResult>(actionResult.Result);
        var response = Assert.IsType<CompleteTaskResponse>(ok.Value);
        var updatedHero = await db.Heroes.SingleAsync(x => x.Id == hero.Id);
        var updatedEconomy = await db.EconomyBalances.SingleAsync(x => x.HeroId == hero.Id);
        var storedUnlocks = await db.Set<HeroAchievement>().ToListAsync();

        Assert.Equal(expectedBaseGold + 25, response.NewGold);
        Assert.Equal(expectedBaseGold + 25, updatedHero.Gold);
        Assert.Equal(expectedBaseGold + 25, updatedEconomy.TotalGoldEarned);
        Assert.Single(storedUnlocks);

        var unlockedAchievements = Assert.IsAssignableFrom<System.Collections.IEnumerable>(ReadProperty<object>(response, "UnlockedAchievements"));
        var achievement = Assert.Single(unlockedAchievements.Cast<object>());
        Assert.Equal("tasks_10", ReadProperty<string>(achievement, "Key"));
        Assert.Equal("Task Starter", ReadProperty<string>(achievement, "Title"));
        Assert.Equal("Complete 10 tasks.", ReadProperty<string>(achievement, "Description"));
        Assert.Equal("TasksCompleted", ReadProperty<string>(achievement, "Category"));
        Assert.Equal(10, ReadProperty<int>(achievement, "Threshold"));
        Assert.Equal(10, ReadProperty<int>(achievement, "SortOrder"));
        Assert.Equal(25, ReadProperty<int>(achievement, "GoldReward"));
        Assert.True(ReadProperty<bool>(achievement, "Unlocked"));
        Assert.Equal(DateTimeKind.Utc, ReadProperty<DateTime>(achievement, "UnlockedAt").Kind);
    }

    [Fact]
    public async Task CompleteTask_AchievementPhaseFails_RollsBackBaseCompletionRewardAndUnlocks()
    {
        await using var db = CreateDbContext();

        var hero = new Hero
        {
            Name = "Alex",
            Gold = 0,
            Level = 1,
            CurrentHp = 100,
            MaxHp = 100,
            TimeZoneId = "UTC",
            OwnerDeviceId = TestDeviceId,
        };
        db.Heroes.Add(hero);
        await db.SaveChangesAsync();

        db.EconomyBalances.Add(new EconomyBalance
        {
            HeroId = hero.Id,
            TotalGoldEarned = 0,
            LastDailyResetLocalDate = "2026-04-20",
            MaxDailyCompletions = GameConstants.DailyTaskCap
        });

        var task = new GameTask
        {
            HeroId = hero.Id,
            Title = "Rollback task",
            Type = TaskType.OneTime,
            Difficulty = TaskDifficulty.Easy,
            CompletionCount = 9,
            IsCompleted = false,
            IsActive = true,
        };
        db.GameTasks.Add(task);
        await db.SaveChangesAsync();

        var controller = new TaskController(db, new GameEngineService(), new ThrowingAchievementService(db), new HeroTimeService(), Microsoft.Extensions.Logging.Abstractions.NullLogger<TaskController>.Instance, new CurrentHeroService(db));
        controller.ControllerContext = new ControllerContext
        {
            HttpContext = new DefaultHttpContext()
        };
        controller.ControllerContext.HttpContext.Request.Headers["X-Device-Id"] = TestDeviceId;

        await Assert.ThrowsAsync<InvalidOperationException>(() => controller.CompleteTask(task.Id));

        await using var verificationDb = CreateDbContext();
        var storedTask = await verificationDb.GameTasks.SingleAsync(x => x.Id == task.Id);
        var storedHero = await verificationDb.Heroes.SingleAsync(x => x.Id == hero.Id);
        var storedEconomy = await verificationDb.EconomyBalances.SingleAsync(x => x.HeroId == hero.Id);
        var storedUnlocks = await verificationDb.Set<HeroAchievement>().ToListAsync();

        Assert.False(storedTask.IsCompleted);
        Assert.Equal(9, storedTask.CompletionCount);
        Assert.Equal(0, storedHero.Gold);
        Assert.Equal(0, storedEconomy.TotalGoldEarned);
        Assert.Empty(storedUnlocks);
    }

    [Fact]
    public async Task PostTask_HabitWithoutPolarity_DefaultsToBoth()
    {
        await using var db = CreateDbContext();
        var hero = await CreateHeroAsync(db);
        var controller = CreateController(db);

        var actionResult = await controller.PostTask(new CreateTaskRequest
        {
            HeroId = hero.Id,
            Title = "Habit without polarity",
            Type = TaskType.Habit,
            Difficulty = TaskDifficulty.Easy
        });

        var created = Assert.IsType<CreatedAtActionResult>(actionResult.Result);
        var dto = Assert.IsType<TaskDto>(created.Value);
        var storedTask = await db.GameTasks.SingleAsync(x => x.Id == dto.Id);

        Assert.Equal(HabitPolarity.Both, dto.Polarity);
        Assert.Equal(HabitPolarity.Both, storedTask.Polarity);
    }

    [Fact]
    public async Task PostTask_NonHabitWithPolarity_ReturnsBadRequest()
    {
        await using var db = CreateDbContext();
        var hero = await CreateHeroAsync(db);
        var controller = CreateController(db);

        var actionResult = await controller.PostTask(new CreateTaskRequest
        {
            HeroId = hero.Id,
            Title = "One time with polarity",
            Type = TaskType.OneTime,
            Difficulty = TaskDifficulty.Easy,
            Polarity = HabitPolarity.Positive
        });

        var badRequest = Assert.IsType<BadRequestObjectResult>(actionResult.Result);
        Assert.Equal("Polarity is only allowed for Habit tasks", badRequest.Value);
    }

    [Fact]
    public async Task FailTask_PositiveHabit_ReturnsBadRequest()
    {
        await using var db = CreateDbContext();
        var hero = await CreateHeroAsync(db);
        var task = await CreateTaskAsync(db, hero.Id, TaskType.Habit, HabitPolarity.Positive);
        var controller = CreateController(db);

        var actionResult = await controller.FailTask(task.Id);

        var badRequest = Assert.IsType<BadRequestObjectResult>(actionResult.Result);
        Assert.Equal("Positive habits cannot be failed", badRequest.Value);
    }

    [Fact]
    public async Task CompleteTask_NegativeHabit_ReturnsBadRequest()
    {
        await using var db = CreateDbContext();
        var hero = await CreateHeroAsync(db);
        var task = await CreateTaskAsync(db, hero.Id, TaskType.Habit, HabitPolarity.Negative);
        var controller = CreateController(db);

        var actionResult = await controller.CompleteTask(task.Id);

        var badRequest = Assert.IsType<BadRequestObjectResult>(actionResult.Result);
        Assert.Equal("Negative habits cannot be completed", badRequest.Value);
    }

    [Fact]
    public async Task HabitWithBothPolarity_AllowsCompleteAndFail()
    {
        await using var db = CreateDbContext();
        var hero = await CreateHeroAsync(db);
        var task = await CreateTaskAsync(db, hero.Id, TaskType.Habit, HabitPolarity.Both);
        var controller = CreateController(db);

        var completeResult = await controller.CompleteTask(task.Id);
        var failResult = await controller.FailTask(task.Id);

        Assert.IsType<OkObjectResult>(completeResult.Result);
        Assert.IsType<OkObjectResult>(failResult.Result);
    }

    [Fact]
    public async Task GetTask_ReturnsHeroShieldStateInStreakInfoAndNullShieldExpiry()
    {
        await using var db = CreateDbContext();
        var hero = await CreateHeroAsync(db);
        hero.IsShieldActive = true;
        hero.ShieldActivatedAtUtc = DateTimeOffset.UtcNow;
        await db.SaveChangesAsync();

        var task = await CreateTaskAsync(db, hero.Id, TaskType.Daily, HabitPolarity.Both);
        var controller = CreateController(db);

        var actionResult = await controller.GetTask(task.Id);

        var ok = Assert.IsType<OkObjectResult>(actionResult.Result);
        var dto = Assert.IsType<TaskDto>(ok.Value);
        Assert.NotNull(dto.StreakInfo);
        Assert.True(dto.StreakInfo!.IsShieldActive);
        Assert.Null(dto.StreakInfo.ShieldExpiresAtUtc);
    }

    [Fact]
    public async Task CheckOverdueTasks_WithMultipleBreakCandidates_UsesSingleHeroShieldAcrossBatchAndConsumesIt()
    {
        await using var db = CreateDbContext();
        var hero = await CreateHeroAsync(db);
        hero.IsShieldActive = true;
        hero.ShieldActivatedAtUtc = DateTimeOffset.UtcNow;
        await db.SaveChangesAsync();

        var overdueOne = await CreateOverdueDailyTaskAsync(db, hero.Id, "Overdue 1", 4);
        var overdueTwo = await CreateOverdueDailyTaskAsync(db, hero.Id, "Overdue 2", 6);
        var controller = CreateController(db);

        var actionResult = await controller.CheckOverdueTasks(hero.Id);

        var ok = Assert.IsType<OkObjectResult>(actionResult.Result);
        var response = Assert.IsType<OverdueCheckResponse>(ok.Value);
        Assert.Equal(2, response.OverdueCount);

        var updatedHero = await db.Heroes.SingleAsync(x => x.Id == hero.Id);
        var updatedStreaks = await db.Streaks.Where(x => x.HeroId == hero.Id).OrderBy(x => x.TaskId).ToListAsync();
        Assert.False(updatedHero.IsShieldActive);
        Assert.Null(updatedHero.ShieldActivatedAtUtc);
        Assert.Equal(4, updatedStreaks[0].CurrentDays);
        Assert.Equal(6, updatedStreaks[1].CurrentDays);
        Assert.All(response.Penalties!, penalty => Assert.False(penalty.StreakBroken));

        var manualFailResult = await controller.FailTask(overdueOne.Id);
        var manualOk = Assert.IsType<OkObjectResult>(manualFailResult.Result);
        var manualResponse = Assert.IsType<FailTaskResponse>(manualOk.Value);
        Assert.True(manualResponse.StreakBroken);
    }

    [Fact]
    public async Task PostTask_DailyWithInitialStreak_PersistsSeededAnchorMetadataForNewRecord()
    {
        await using var db = CreateDbContext();
        var hero = await CreateHeroAsync(db);
        var utcNow = new DateTimeOffset(2026, 04, 24, 12, 00, 00, TimeSpan.Zero);
        var timeService = new FixedHeroTimeService(utcNow);
        var createdLocalDate = timeService.GetLocalDate(utcNow, timeService.ResolveEffectiveTimeZone(hero, utcNow));
        var controller = CreateController(db, timeService);

        var actionResult = await controller.PostTask(new CreateTaskRequest
        {
            HeroId = hero.Id,
            Title = "Seeded daily",
            Type = TaskType.Daily,
            Difficulty = TaskDifficulty.Easy,
            InitialStreak = 30
        });

        var created = Assert.IsType<CreatedAtActionResult>(actionResult.Result);
        var dto = Assert.IsType<TaskDto>(created.Value);
        var streak = await db.Streaks.SingleAsync(x => x.TaskId == dto.Id);

        Assert.Equal(30, streak.CurrentDays);
        Assert.Equal(30, streak.LongestDays);
        Assert.Equal(createdLocalDate.AddDays(-1).ToString("yyyy-MM-dd"), streak.LastCheckInLocalDate);
    }

    [Fact]
    public async Task PostTask_DailyWithoutInitialStreak_DoesNotPersistSeededAnchorMetadata()
    {
        await using var db = CreateDbContext();
        var hero = await CreateHeroAsync(db);
        var utcNow = new DateTimeOffset(2026, 04, 24, 12, 00, 00, TimeSpan.Zero);
        var timeService = new FixedHeroTimeService(utcNow);
        var controller = CreateController(db, timeService);

        var actionResult = await controller.PostTask(new CreateTaskRequest
        {
            HeroId = hero.Id,
            Title = "Fresh daily",
            Type = TaskType.Daily,
            Difficulty = TaskDifficulty.Easy,
            InitialStreak = 0
        });

        var created = Assert.IsType<CreatedAtActionResult>(actionResult.Result);
        var dto = Assert.IsType<TaskDto>(created.Value);
        var streak = await db.Streaks.SingleAsync(x => x.TaskId == dto.Id);

        Assert.Equal(0, streak.CurrentDays);
        Assert.Equal(0, streak.LongestDays);
        Assert.True(string.IsNullOrWhiteSpace(streak.LastCheckInLocalDate));
    }

    [Fact]
    public async Task CompleteTask_LegacySeededDailyWithMissingLastCheckInBackfillsAnchorBeforeRegisteringSuccess()
    {
        await using var db = CreateDbContext();
        var utcNow = new DateTimeOffset(2026, 04, 24, 12, 00, 00, TimeSpan.Zero);
        var hero = await CreateHeroAsync(db, timeZoneId: "UTC", createdAt: utcNow);
        var task = await CreateTaskAsync(db, hero.Id, TaskType.Daily, HabitPolarity.Both, createdAt: utcNow);
        var completionLocalDate = DateOnly.FromDateTime(utcNow.UtcDateTime);

        db.Streaks.Add(new Streak
        {
            HeroId = hero.Id,
            TaskId = task.Id,
            CurrentDays = 30,
            LongestDays = 30,
            CreatedAt = task.CreatedAt,
            UpdatedAt = task.CreatedAt,
            LastCheckInLocalDate = null
        });
        await db.SaveChangesAsync();

        var controller = CreateController(db, new FixedHeroTimeService(utcNow));

        var actionResult = await controller.CompleteTask(task.Id);

        var ok = Assert.IsType<OkObjectResult>(actionResult.Result);
        var response = Assert.IsType<CompleteTaskResponse>(ok.Value);
        var streak = await db.Streaks.SingleAsync(x => x.TaskId == task.Id);

        Assert.Equal(31, response.CurrentStreak);
        Assert.Equal(31, streak.CurrentDays);
        Assert.Equal(31, streak.LongestDays);
        Assert.Equal(completionLocalDate.ToString("yyyy-MM-dd"), streak.LastCheckInLocalDate);
    }

    [Fact]
    public async Task CheckOverdueTasks_SeededDailyWithoutSameDayCompletion_BreaksStreakOnNextLocalDay()
    {
        await using var db = CreateDbContext();
        var createdUtc = new DateTimeOffset(2026, 04, 24, 00, 00, 00, TimeSpan.Zero);
        var overdueUtc = createdUtc.AddDays(1).AddHours(12);
        var hero = await CreateHeroAsync(db, timeZoneId: "UTC", createdAt: createdUtc);
        var task = new GameTask
        {
            HeroId = hero.Id,
            Title = "Seeded overdue daily",
            Type = TaskType.Daily,
            Difficulty = TaskDifficulty.Easy,
            Polarity = HabitPolarity.Both,
            IsCompleted = false,
            IsActive = true,
            DueDate = createdUtc.AddHours(-1),
            CompletionCount = 0,
            FailCount = 0,
            CreatedAt = createdUtc,
            UpdatedAt = createdUtc
        };
        db.GameTasks.Add(task);
        await db.SaveChangesAsync();

        var createdLocalDate = DateOnly.FromDateTime(createdUtc.UtcDateTime);
        db.Streaks.Add(new Streak
        {
            HeroId = hero.Id,
            TaskId = task.Id,
            CurrentDays = 30,
            LongestDays = 30,
            CreatedAt = createdUtc,
            UpdatedAt = createdUtc,
            LastCheckInLocalDate = createdLocalDate.AddDays(-1).ToString("yyyy-MM-dd")
        });
        await db.SaveChangesAsync();

        var controller = CreateController(db, new FixedHeroTimeService(overdueUtc));

        var actionResult = await controller.CheckOverdueTasks(hero.Id);

        var ok = Assert.IsType<OkObjectResult>(actionResult.Result);
        var response = Assert.IsType<OverdueCheckResponse>(ok.Value);
        var streak = await db.Streaks.SingleAsync(x => x.TaskId == task.Id);

        Assert.Equal(1, response.OverdueCount);
        Assert.Single(response.Penalties!);
        Assert.True(response.Penalties![0].StreakBroken);
        Assert.Equal(0, streak.CurrentDays);
        Assert.Equal(overdueUtc.Date.ToString("yyyy-MM-dd"), streak.LastBreakLocalDate);
    }

    [Fact]
    public async Task CompleteTask_LegacySeededDaily_BackfillsAnchorUsingCreationTimezoneInsteadOfCurrentTimezone()
    {
        await using var db = CreateDbContext();
        var createdUtc = new DateTimeOffset(2026, 04, 24, 10, 30, 00, TimeSpan.Zero);
        var completionUtc = new DateTimeOffset(2026, 04, 25, 12, 00, 00, TimeSpan.Zero);
        var hero = await CreateHeroAsync(db, timeZoneId: "America/New_York", createdAt: createdUtc);
        hero.PendingTimeZoneId = "Asia/Tokyo";
        hero.TimeZoneSwitchAfterLocalDate = "2026-04-24";
        await db.SaveChangesAsync();

        var task = await CreateTaskAsync(db, hero.Id, TaskType.Daily, HabitPolarity.Both, createdAt: createdUtc);
        db.Streaks.Add(new Streak
        {
            HeroId = hero.Id,
            TaskId = task.Id,
            CurrentDays = 30,
            LongestDays = 30,
            CreatedAt = createdUtc,
            UpdatedAt = createdUtc,
            LastCheckInLocalDate = null
        });
        await db.SaveChangesAsync();

        var controller = CreateController(db, new FixedHeroTimeService(completionUtc));

        var actionResult = await controller.CompleteTask(task.Id);

        var ok = Assert.IsType<OkObjectResult>(actionResult.Result);
        var response = Assert.IsType<CompleteTaskResponse>(ok.Value);
        var streak = await db.Streaks.SingleAsync(x => x.TaskId == task.Id);

        Assert.Equal(1, response.CurrentStreak);
        Assert.Equal(1, streak.CurrentDays);
        Assert.Equal(30, streak.LongestDays);
        Assert.Equal("2026-04-25", streak.LastCheckInLocalDate);
    }

    private static T ReadProperty<T>(object source, string propertyName)
    {
        var property = source.GetType().GetProperty(propertyName);
        Assert.NotNull(property);
        var value = property!.GetValue(source);
        Assert.NotNull(value);
        return (T)value!;
    }

    private static TaskController CreateController(ApplicationDbContext db, IHeroTimeService? timeService = null)
    {
        var controller = TaskController.CreateForTests(db, new GameEngineService(), timeService ?? new HeroTimeService());
        controller.ControllerContext = new ControllerContext
        {
            HttpContext = new DefaultHttpContext()
        };
        controller.ControllerContext.HttpContext.Request.Headers["X-Device-Id"] = TestDeviceId;
        return controller;
    }

    private static async Task<Hero> CreateHeroAsync(ApplicationDbContext db, string timeZoneId = "UTC", DateTimeOffset? createdAt = null)
    {
        var heroCreatedAt = createdAt ?? DateTimeOffset.UtcNow;
        var hero = new Hero
        {
            Name = "Alex",
            Gold = 100,
            Level = 1,
            CurrentHp = 100,
            MaxHp = 100,
            TimeZoneId = timeZoneId,
            OwnerDeviceId = TestDeviceId,
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

    private static async Task<GameTask> CreateTaskAsync(
        ApplicationDbContext db,
        int heroId,
        TaskType type,
        HabitPolarity polarity,
        DateTimeOffset? createdAt = null)
    {
        var taskCreatedAt = createdAt ?? DateTimeOffset.UtcNow;
        var task = new GameTask
        {
            HeroId = heroId,
            Title = $"{type} task",
            Type = type,
            Difficulty = TaskDifficulty.Easy,
            Polarity = polarity,
            IsCompleted = false,
            IsActive = true,
            CompletionCount = 0,
            FailCount = 0,
            CreatedAt = taskCreatedAt,
            UpdatedAt = taskCreatedAt,
        };

        db.GameTasks.Add(task);
        await db.SaveChangesAsync();

        if (type == TaskType.Habit || type == TaskType.Daily)
        {
            db.Streaks.Add(new Streak
            {
                HeroId = heroId,
                TaskId = task.Id,
                CurrentDays = 0,
                LongestDays = 0,
                CreatedAt = taskCreatedAt,
                UpdatedAt = taskCreatedAt,
            });
            await db.SaveChangesAsync();
        }

        return task;
    }

    private static async Task<GameTask> CreateOverdueDailyTaskAsync(ApplicationDbContext db, int heroId, string title, int streakDays)
    {
        var task = new GameTask
        {
            HeroId = heroId,
            Title = title,
            Type = TaskType.Daily,
            Difficulty = TaskDifficulty.Easy,
            Polarity = HabitPolarity.Both,
            IsCompleted = false,
            IsActive = true,
            DueDate = DateTimeOffset.UtcNow.AddDays(-1),
            CompletionCount = 0,
            FailCount = 0
        };

        db.GameTasks.Add(task);
        await db.SaveChangesAsync();

        db.Streaks.Add(new Streak
        {
            HeroId = heroId,
            TaskId = task.Id,
            CurrentDays = streakDays,
            LongestDays = streakDays,
            CreatedAt = DateTimeOffset.UtcNow,
            UpdatedAt = DateTimeOffset.UtcNow,
        });
        await db.SaveChangesAsync();

        return task;
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

    private sealed class ThrowingAchievementService(ApplicationDbContext db) : LifeTracker.Services.Achievements.AchievementService(db)
    {
        public override Task<IReadOnlyList<LifeTracker.Services.Achievements.AchievementUnlock>> EvaluateAndStageNewUnlocksAsync(int heroId, CancellationToken ct = default) =>
            throw new InvalidOperationException("Forced achievement failure");
    }
}
