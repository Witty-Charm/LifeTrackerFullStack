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

public class StreakSeedingTests : IAsyncLifetime
{
    private const string TestDeviceId = "22222222-2222-2222-2222-222222222222";
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
    public async Task PostTask_DailyWithInitialStreak_SeedsCurrentAndLongestDays()
    {
        await using var db = CreateDbContext();
        var hero = await CreateHeroAsync(db);
        var controller = CreateController(db);

        var actionResult = await controller.PostTask(new CreateTaskRequest
        {
            HeroId = hero.Id,
            Title = "Seeded daily",
            Type = TaskType.Daily,
            Difficulty = TaskDifficulty.Easy,
            InitialStreak = 30,
        });

        var created = Assert.IsType<CreatedAtActionResult>(actionResult.Result);
        var dto = Assert.IsType<TaskDto>(created.Value);
        var streak = await db.Streaks.SingleAsync(s => s.TaskId == dto.Id);

        Assert.Equal(30, streak.CurrentDays);
        Assert.Equal(30, streak.LongestDays);
    }

    [Fact]
    public async Task CompleteTask_SeededStreak30_PreservesAndIncrements()
    {
        await using var db = CreateDbContext();
        var hero = await CreateHeroAsync(db);
        var (task, streak) = await CreateDailyWithSeededStreakAsync(db, hero.Id, 30);
        var controller = CreateController(db);

        var actionResult = await controller.CompleteTask(task.Id);

        var ok = Assert.IsType<OkObjectResult>(actionResult.Result);
        var response = Assert.IsType<CompleteTaskResponse>(ok.Value);

        var updatedStreak = await db.Streaks.SingleAsync(s => s.TaskId == task.Id);
        Assert.Equal(31, updatedStreak.CurrentDays);
        Assert.Equal(31, updatedStreak.LongestDays);
        Assert.Equal(31, response.CurrentStreak);
    }

    [Fact]
    public async Task CompleteTask_ZeroInitialStreak_FirstCompletionSetsStreakTo1()
    {
        await using var db = CreateDbContext();
        var hero = await CreateHeroAsync(db);
        var (task, streak) = await CreateDailyWithSeededStreakAsync(db, hero.Id, 0);
        var controller = CreateController(db);

        var actionResult = await controller.CompleteTask(task.Id);

        var ok = Assert.IsType<OkObjectResult>(actionResult.Result);
        var response = Assert.IsType<CompleteTaskResponse>(ok.Value);

        var updatedStreak = await db.Streaks.SingleAsync(s => s.TaskId == task.Id);
        Assert.Equal(1, updatedStreak.CurrentDays);
        Assert.Equal(1, updatedStreak.LongestDays);
        Assert.Equal(1, response.CurrentStreak);
    }

    [Fact]
    public async Task CompleteTask_SeededStreak_LongestDaysStaysCoherent()
    {
        await using var db = CreateDbContext();
        var hero = await CreateHeroAsync(db);
        var (task, streak) = await CreateDailyWithSeededStreakAsync(db, hero.Id, 50);
        var controller = CreateController(db);

        await controller.CompleteTask(task.Id);

        var updatedStreak = await db.Streaks.SingleAsync(s => s.TaskId == task.Id);
        Assert.Equal(51, updatedStreak.CurrentDays);
        Assert.Equal(51, updatedStreak.LongestDays);
    }

    [Fact]
    public void RegisterSuccess_FirstCompletion_SeededStreakContinues()
    {
        var streak = new Streak
        {
            CurrentDays = 15,
            LongestDays = 15,
        };

        var now = DateTimeOffset.UtcNow;
        var today = DateOnly.FromDateTime(now.UtcDateTime);

        streak.RegisterSuccess(today, now);

        Assert.Equal(16, streak.CurrentDays);
        Assert.Equal(16, streak.LongestDays);
        Assert.Equal(today.ToString("yyyy-MM-dd"), streak.LastCheckInLocalDate);
    }

    [Fact]
    public void RegisterSuccess_FirstCompletion_ZeroStreak_SetsTo1()
    {
        var streak = new Streak
        {
            CurrentDays = 0,
            LongestDays = 0,
        };

        var now = DateTimeOffset.UtcNow;
        var today = DateOnly.FromDateTime(now.UtcDateTime);

        streak.RegisterSuccess(today, now);

        Assert.Equal(1, streak.CurrentDays);
        Assert.Equal(1, streak.LongestDays);
    }

    [Fact]
    public async Task CompletionCap_AppliesToAllTaskTypes()
    {
        await using var db = CreateDbContext();
        var hero = await CreateHeroAsync(db);

        var economy = await db.EconomyBalances.SingleAsync(e => e.HeroId == hero.Id);
        economy.TaskCompletions = GameConstants.DailyTaskCap;
        economy.LastDailyResetLocalDate = DateOnly.FromDateTime(DateTime.UtcNow).ToString("yyyy-MM-dd");
        await db.SaveChangesAsync();

        var task = new GameTask
        {
            HeroId = hero.Id,
            Title = "Over-cap task",
            Type = TaskType.OneTime,
            Difficulty = TaskDifficulty.Easy,
            IsCompleted = false,
            IsActive = true,
        };
        db.GameTasks.Add(task);
        await db.SaveChangesAsync();

        var controller = CreateController(db);
        var actionResult = await controller.CompleteTask(task.Id);

        var badRequest = Assert.IsType<BadRequestObjectResult>(actionResult.Result);
        var body = badRequest.Value!;
        var errorCode = body.GetType().GetProperty("errorCode")!.GetValue(body)!.ToString();
        Assert.Equal("COMPLETION_LIMIT_REACHED", errorCode);
    }

    private static TaskController CreateController(ApplicationDbContext db)
    {
        var controller = TaskController.CreateForTests(db, new GameEngineService(), new HeroTimeService());
        controller.ControllerContext = new ControllerContext
        {
            HttpContext = new DefaultHttpContext()
        };
        controller.ControllerContext.HttpContext.Request.Headers["X-Device-Id"] = TestDeviceId;
        return controller;
    }

    private static async Task<Hero> CreateHeroAsync(ApplicationDbContext db)
    {
        var hero = new Hero
        {
            Name = "TestHero",
            Gold = 100,
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
            MaxCompletions = GameConstants.DailyTaskCap,
        });
        await db.SaveChangesAsync();

        return hero;
    }

    private static async Task<(GameTask task, Streak streak)> CreateDailyWithSeededStreakAsync(
        ApplicationDbContext db, int heroId, int initialStreak)
    {
        var task = new GameTask
        {
            HeroId = heroId,
            Title = $"Seeded daily ({initialStreak})",
            Type = TaskType.Daily,
            Difficulty = TaskDifficulty.Easy,
            Polarity = HabitPolarity.Both,
            IsCompleted = false,
            IsActive = true,
        };

        db.GameTasks.Add(task);
        await db.SaveChangesAsync();

        var streak = new Streak
        {
            HeroId = heroId,
            TaskId = task.Id,
            CurrentDays = initialStreak,
            LongestDays = initialStreak,
            CreatedAt = DateTimeOffset.UtcNow,
            UpdatedAt = DateTimeOffset.UtcNow,
        };

        db.Streaks.Add(streak);
        await db.SaveChangesAsync();

        return (task, streak);
    }

    private ApplicationDbContext CreateDbContext() => new(_options);
}
