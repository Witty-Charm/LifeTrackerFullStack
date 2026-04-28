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

public class TaskControllerHabitResetTests : IAsyncLifetime
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

    public async Task DisposeAsync() => await _connection.DisposeAsync();

    [Fact]
    public async Task GetTask_HabitWithDailyResetPattern_ReturnsZeroCounters_WhenLocalDayHasRolledOver()
    {
        await using var db = CreateDbContext();
        var startUtc = new DateTimeOffset(2026, 4, 26, 9, 0, 0, TimeSpan.Zero);
        var hero = await CreateHeroAsync(db, createdAt: startUtc);

        var task = new GameTask
        {
            HeroId = hero.Id,
            Title = "Drink water",
            Type = TaskType.Habit,
            Difficulty = TaskDifficulty.Easy,
            Polarity = HabitPolarity.Both,
            IsActive = true,
            RepeatPattern = "RESET:DAILY",
            CompletionCount = 3,
            FailCount = 1,
            LastCompletedAt = startUtc,
            CreatedAt = startUtc,
            UpdatedAt = startUtc,
        };
        db.GameTasks.Add(task);
        await db.SaveChangesAsync();

        var queryUtc = new DateTimeOffset(2026, 4, 27, 10, 0, 0, TimeSpan.Zero);
        var controller = CreateController(db, new FixedHeroTimeService(queryUtc));

        var result = await controller.GetTask(task.Id);
        var ok = Assert.IsType<OkObjectResult>(result.Result);
        var dto = Assert.IsType<TaskDto>(ok.Value);

        // Compute-on-read: DTO surfaces zeros after the local day rolls over.
        Assert.Equal(0, dto.CompletionCount);
        Assert.Equal(0, dto.FailCount);

        // The persisted entity is untouched — no SaveChanges happened on this read path.
        var stored = await db.GameTasks.AsNoTracking().SingleAsync(t => t.Id == task.Id);
        Assert.Equal(3, stored.CompletionCount);
        Assert.Equal(1, stored.FailCount);
        Assert.Equal(startUtc, stored.UpdatedAt);
        Assert.Equal(startUtc, stored.LastCompletedAt);
    }

    [Fact]
    public async Task GetTasks_HabitWithDailyResetPattern_StaysIntact_WhenStillSameLocalDay()
    {
        await using var db = CreateDbContext();
        var startUtc = new DateTimeOffset(2026, 4, 26, 9, 0, 0, TimeSpan.Zero);
        var hero = await CreateHeroAsync(db, createdAt: startUtc);

        var task = new GameTask
        {
            HeroId = hero.Id,
            Title = "Read book",
            Type = TaskType.Habit,
            Difficulty = TaskDifficulty.Easy,
            Polarity = HabitPolarity.Both,
            IsActive = true,
            RepeatPattern = "RESET:DAILY",
            CompletionCount = 2,
            FailCount = 0,
            LastCompletedAt = startUtc,
            CreatedAt = startUtc,
            UpdatedAt = startUtc,
        };
        db.GameTasks.Add(task);
        await db.SaveChangesAsync();

        var queryUtc = new DateTimeOffset(2026, 4, 26, 22, 0, 0, TimeSpan.Zero);
        var controller = CreateController(db, new FixedHeroTimeService(queryUtc));

        var result = await controller.GetTasks(hero.Id);
        var ok = Assert.IsType<OkObjectResult>(result.Result);
        var list = Assert.IsAssignableFrom<IEnumerable<TaskDto>>(ok.Value);
        var dto = Assert.Single(list);

        Assert.Equal(2, dto.CompletionCount);
        Assert.Equal(0, dto.FailCount);
    }

    private static TaskController CreateController(ApplicationDbContext db, IHeroTimeService timeService)
    {
        var controller = TaskController.CreateForTests(db, new GameEngineService(), timeService);
        controller.ControllerContext = new ControllerContext
        {
            HttpContext = new DefaultHttpContext(),
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
            Gold = 0,
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
