using LifeTracker.Constants;
using LifeTracker.Controllers;
using LifeTracker.Data;
using LifeTracker.Models;
using LifeTracker.Services;
using LifeTracker.Services.Time;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Data.Sqlite;
using Microsoft.EntityFrameworkCore;

namespace LifeTrackerBackend.Tests;

public class TaskControllerAchievementTests : IAsyncLifetime
{
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
            TimeZoneId = "UTC"
        };
        db.Heroes.Add(hero);
        await db.SaveChangesAsync();

        db.EconomyBalances.Add(new EconomyBalance
        {
            HeroId = hero.Id,
            TotalGoldEarned = 0,
            LastDailyResetLocalDate = "2026-04-20"
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
        var controller = new TaskController(db, new GameEngineService(), new HeroTimeService());

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
            TimeZoneId = "UTC"
        };
        db.Heroes.Add(hero);
        await db.SaveChangesAsync();

        db.EconomyBalances.Add(new EconomyBalance
        {
            HeroId = hero.Id,
            TotalGoldEarned = 0,
            LastDailyResetLocalDate = "2026-04-20"
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

        var controller = new TaskController(db, new GameEngineService(), new ThrowingAchievementService(db), new HeroTimeService());

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

    private static T ReadProperty<T>(object source, string propertyName)
    {
        var property = source.GetType().GetProperty(propertyName);
        Assert.NotNull(property);
        var value = property!.GetValue(source);
        Assert.NotNull(value);
        return (T)value!;
    }

    private ApplicationDbContext CreateDbContext() => new(_options);

    private sealed class ThrowingAchievementService(ApplicationDbContext db) : LifeTracker.Services.Achievements.AchievementService(db)
    {
        public override Task<IReadOnlyList<LifeTracker.Services.Achievements.AchievementUnlock>> EvaluateAndStageNewUnlocksAsync(int heroId, CancellationToken ct = default) =>
            throw new InvalidOperationException("Forced achievement failure");
    }
}
