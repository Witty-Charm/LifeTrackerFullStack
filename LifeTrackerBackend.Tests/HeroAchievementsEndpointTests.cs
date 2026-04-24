using LifeTracker.Controllers;
using LifeTracker.Data;
using LifeTracker.Models;
using LifeTracker.Services.Achievements;
using LifeTracker.Services.Time;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Data.Sqlite;
using Microsoft.EntityFrameworkCore;

namespace LifeTrackerBackend.Tests;

public class HeroAchievementsEndpointTests : IAsyncLifetime
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
    public async Task GetAchievements_NoUnlocks_ReturnsFullLockedCatalogInStableOrder()
    {
        await using var db = CreateDbContext();

        var hero = new Hero
        {
            OwnerDeviceId = TestDeviceId,
            Name = "Alex",
            Gold = 0,
            Level = 1,
            CurrentHp = 100,
            MaxHp = 100,
            TimeZoneId = "UTC"
        };
        db.Heroes.Add(hero);
        await db.SaveChangesAsync();

        var controller = HeroController.CreateForTests(db, new HeroTimeService());
        controller.ControllerContext = new ControllerContext
        {
            HttpContext = new DefaultHttpContext()
        };
        controller.ControllerContext.HttpContext.Request.Headers["X-Device-Id"] = TestDeviceId;

        var actionResult = await controller.GetAchievements(hero.Id);

        var ok = Assert.IsType<OkObjectResult>(actionResult.Result);
        var payload = ok.Value!;
        var totalCount = ReadProperty<int>(payload, "TotalCount");
        var unlockedCount = ReadProperty<int>(payload, "UnlockedCount");
        var items = Assert.IsAssignableFrom<System.Collections.IEnumerable>(ReadProperty<object>(payload, "Achievements"));
        var ordered = items.Cast<object>().ToList();

        Assert.Equal(9, totalCount);
        Assert.Equal(0, unlockedCount);
        Assert.Equal(9, ordered.Count);
        Assert.All(ordered, item => Assert.False(ReadProperty<bool>(item, "Unlocked")));
        Assert.Equal("tasks_10", ReadProperty<string>(ordered[0], "Key"));
        Assert.Equal(10, ReadProperty<int>(ordered[0], "SortOrder"));
        Assert.Equal("level_20", ReadProperty<string>(ordered[^1], "Key"));
        Assert.Equal(90, ReadProperty<int>(ordered[^1], "SortOrder"));
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
}
