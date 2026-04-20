using LifeTracker.Constants;
using LifeTracker.Data;
using LifeTracker.Models;
using LifeTracker.Services.Achievements;
using Microsoft.Data.Sqlite;
using Microsoft.EntityFrameworkCore;

namespace LifeTrackerBackend.Tests;

public class AchievementServiceTests : IAsyncLifetime
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
    public async Task EvaluateAndStageNewUnlocksAsync_RepeatedEvaluation_AwardsGoldExactlyOnce()
    {
        await using var db = CreateDbContext();

        var hero = new Hero
        {
            Name = "Alex",
            Gold = 0,
            Level = 1,
            CurrentHp = 100,
            MaxHp = 100,
        };
        db.Heroes.Add(hero);
        await db.SaveChangesAsync();

        var economy = new EconomyBalance
        {
            HeroId = hero.Id,
            TotalGoldEarned = 0,
            LastDailyResetLocalDate = "2026-04-20",
        };
        db.EconomyBalances.Add(economy);
        db.GameTasks.Add(new GameTask
        {
            HeroId = hero.Id,
            Title = "Ten times",
            Type = TaskType.OneTime,
            Difficulty = TaskDifficulty.Easy,
            CompletionCount = 10,
        });
        await db.SaveChangesAsync();

        var service = new AchievementService(db);

        var firstUnlocks = await service.EvaluateAndStageNewUnlocksAsync(hero.Id);
        await db.SaveChangesAsync();

        var secondUnlocks = await service.EvaluateAndStageNewUnlocksAsync(hero.Id);
        await db.SaveChangesAsync();

        var storedUnlocks = await db.Set<HeroAchievement>().OrderBy(x => x.Key).ToListAsync();
        var updatedHero = await db.Heroes.SingleAsync(x => x.Id == hero.Id);
        var updatedEconomy = await db.EconomyBalances.SingleAsync(x => x.HeroId == hero.Id);

        Assert.Single(firstUnlocks);
        Assert.Empty(secondUnlocks);
        Assert.Single(storedUnlocks);
        Assert.Equal("tasks_10", storedUnlocks[0].Key);
        Assert.Equal(25, storedUnlocks[0].GoldReward);
        Assert.Equal(DateTimeKind.Utc, storedUnlocks[0].UnlockedAt.Kind);
        Assert.Equal(25, updatedHero.Gold);
        Assert.Equal(25, updatedEconomy.TotalGoldEarned);
    }

    [Fact]
    public async Task EvaluateAndStageNewUnlocksAsync_TwoDbContexts_AwardsGoldExactlyOnce()
    {
        await using var seedDb = CreateDbContext();

        var hero = new Hero
        {
            Name = "Alex",
            Gold = 0,
            Level = 1,
            CurrentHp = 100,
            MaxHp = 100,
        };
        seedDb.Heroes.Add(hero);
        await seedDb.SaveChangesAsync();

        seedDb.EconomyBalances.Add(new EconomyBalance
        {
            HeroId = hero.Id,
            TotalGoldEarned = 0,
            LastDailyResetLocalDate = "2026-04-20",
        });
        seedDb.GameTasks.Add(new GameTask
        {
            HeroId = hero.Id,
            Title = "Ten times",
            Type = TaskType.OneTime,
            Difficulty = TaskDifficulty.Easy,
            CompletionCount = 10,
        });
        await seedDb.SaveChangesAsync();

        await using var firstDb = CreateDbContext();
        await using var secondDb = CreateDbContext();

        var firstService = new AchievementService(firstDb);
        var secondService = new AchievementService(secondDb);

        var firstUnlocks = await firstService.EvaluateAndStageNewUnlocksAsync(hero.Id);
        await firstDb.SaveChangesAsync();

        var secondUnlocks = await secondService.EvaluateAndStageNewUnlocksAsync(hero.Id);
        await secondDb.SaveChangesAsync();

        await using var verificationDb = CreateDbContext();
        var storedUnlocks = await verificationDb.Set<HeroAchievement>().ToListAsync();
        var updatedHero = await verificationDb.Heroes.SingleAsync(x => x.Id == hero.Id);
        var updatedEconomy = await verificationDb.EconomyBalances.SingleAsync(x => x.HeroId == hero.Id);

        Assert.Single(firstUnlocks);
        Assert.Empty(secondUnlocks);
        Assert.Single(storedUnlocks);
        Assert.Equal(25, updatedHero.Gold);
        Assert.Equal(25, updatedEconomy.TotalGoldEarned);
    }

    private ApplicationDbContext CreateDbContext() => new(_options);
}
