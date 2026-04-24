using System.Reflection;
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

public class DeviceOwnershipControllerTests : IAsyncLifetime
{
    private const string DeviceA = "11111111-1111-1111-1111-111111111111";
    private const string DeviceB = "22222222-2222-2222-2222-222222222222";
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
    public async Task GetHero_WithoutDeviceHeader_ReturnsBadRequest()
    {
        await using var db = CreateDbContext();
        var hero = await CreateHeroAsync(db, "Alex");
        var controller = CreateHeroController(db);
        controller.ControllerContext = new ControllerContext { HttpContext = new DefaultHttpContext() };

        var actionResult = await controller.GetHero(hero.Id);

        Assert.IsType<BadRequestObjectResult>(actionResult.Result);
    }

    [Fact]
    public async Task PostHero_PersistsOwnerDeviceIdFromHeader()
    {
        await using var db = CreateDbContext();
        var controller = CreateHeroController(db, DeviceA);

        var actionResult = await controller.PostHero(new CreateHeroRequest { Name = "Alex" });

        var created = Assert.IsType<CreatedAtActionResult>(actionResult.Result);
        Assert.IsType<HeroDto>(created.Value);
        var storedHero = await db.Heroes.SingleAsync();
        Assert.Equal(DeviceA, ReadOwnerDeviceId(storedHero));
    }

    [Fact]
    public async Task GetHero_ForOtherDevice_ReturnsNotFound()
    {
        await using var db = CreateDbContext();
        var hero = await CreateHeroAsync(db, "Alex", ownerDeviceId: DeviceA);
        var controller = CreateHeroController(db, DeviceB);

        var actionResult = await controller.GetHero(hero.Id);

        Assert.IsType<NotFoundResult>(actionResult.Result);
    }

    [Fact]
    public async Task GetCurrentHero_ForCurrentDevice_ReturnsOwnedHero()
    {
        await using var db = CreateDbContext();
        var hero = await CreateHeroAsync(db, "Alex", ownerDeviceId: DeviceA);
        var controller = CreateHeroController(db, DeviceA);
        var method = typeof(HeroController).GetMethod("GetCurrentHero", BindingFlags.Instance | BindingFlags.Public);

        Assert.NotNull(method);

        var task = Assert.IsAssignableFrom<Task>(method!.Invoke(controller, Array.Empty<object?>()));
        await task;
        var resultProperty = task.GetType().GetProperty("Result");
        Assert.NotNull(resultProperty);
        var actionResult = Assert.IsType<ActionResult<HeroDto>>(resultProperty!.GetValue(task));

        var ok = Assert.IsType<OkObjectResult>(actionResult.Result);
        var dto = Assert.IsType<HeroDto>(ok.Value);
        Assert.Equal(hero.Id, dto.Id);
    }

    [Fact]
    public async Task PostTask_WithoutHeroId_UsesHeroOwnedByCurrentDevice()
    {
        await using var db = CreateDbContext();
        var heroA = await CreateHeroAsync(db, "Alex", ownerDeviceId: DeviceA);
        var heroB = await CreateHeroAsync(db, "Blair", ownerDeviceId: DeviceB);
        var controller = CreateTaskController(db, DeviceB);

        var actionResult = await controller.PostTask(new CreateTaskRequest
        {
            Title = "Device scoped task",
            Type = TaskType.OneTime,
            Difficulty = TaskDifficulty.Easy,
        });

        var created = Assert.IsType<CreatedAtActionResult>(actionResult.Result);
        var dto = Assert.IsType<TaskDto>(created.Value);
        Assert.Equal(heroB.Id, dto.HeroId);
        Assert.NotEqual(heroA.Id, dto.HeroId);
    }

    [Fact]
    public void HeroController_DoesNotExposePublicGetHeroesEndpoint()
    {
        var getHeroes = typeof(HeroController).GetMethod("GetHeroes", BindingFlags.Instance | BindingFlags.Public);

        Assert.Null(getHeroes);
    }

    private static HeroController CreateHeroController(ApplicationDbContext db, string? deviceId = null)
    {
        var controller = HeroController.CreateForTests(db, new HeroTimeService());
        controller.ControllerContext = new ControllerContext { HttpContext = CreateHttpContext(deviceId) };
        return controller;
    }

    private static TaskController CreateTaskController(ApplicationDbContext db, string? deviceId = null)
    {
        var controller = TaskController.CreateForTests(db, new GameEngineService(), new HeroTimeService());
        controller.ControllerContext = new ControllerContext { HttpContext = CreateHttpContext(deviceId) };
        return controller;
    }

    private static DefaultHttpContext CreateHttpContext(string? deviceId)
    {
        var context = new DefaultHttpContext();
        if (!string.IsNullOrWhiteSpace(deviceId))
        {
            context.Request.Headers["X-Device-Id"] = deviceId;
        }

        return context;
    }

    private static async Task<Hero> CreateHeroAsync(ApplicationDbContext db, string name, string? ownerDeviceId = null)
    {
        var hero = new Hero
        {
            Name = name,
            Gold = 100,
            Level = 1,
            CurrentHp = GameConstants.BaseHp,
            MaxHp = GameConstants.BaseHp,
            TimeZoneId = "UTC",
        };

        if (ownerDeviceId is not null)
        {
            SetOwnerDeviceId(hero, ownerDeviceId);
        }

        db.Heroes.Add(hero);
        await db.SaveChangesAsync();
        return hero;
    }

    private static void SetOwnerDeviceId(Hero hero, string ownerDeviceId)
    {
        var property = typeof(Hero).GetProperty("OwnerDeviceId");
        Assert.NotNull(property);
        property!.SetValue(hero, ownerDeviceId);
    }

    private static string ReadOwnerDeviceId(Hero hero)
    {
        var property = typeof(Hero).GetProperty("OwnerDeviceId");
        Assert.NotNull(property);
        return Assert.IsType<string>(property!.GetValue(hero));
    }

    private ApplicationDbContext CreateDbContext() => new(_options);
}
