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

public class UserOwnershipControllerTests : IAsyncLifetime
{
    private const int UserA = 1;
    private const int UserB = 2;
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
        db.Users.AddRange(
            new User { Id = UserA, Provider = AuthProvider.Google, ExternalId = "ext-A", Email = "a@example.com" },
            new User { Id = UserB, Provider = AuthProvider.Google, ExternalId = "ext-B", Email = "b@example.com" });
        await db.SaveChangesAsync();
    }

    public async Task DisposeAsync() => await _connection.DisposeAsync();

    [Fact]
    public async Task GetHero_WithoutAuthenticatedPrincipal_ReturnsUnauthorized()
    {
        await using var db = CreateDbContext();
        var hero = await CreateHeroAsync(db, "Alex", userId: UserA, ownerDeviceId: DeviceA);
        var controller = HeroController.CreateForTests(db, new HeroTimeService());
        controller.ControllerContext = new ControllerContext { HttpContext = new DefaultHttpContext() };

        var actionResult = await controller.GetHero(hero.Id);

        Assert.IsType<UnauthorizedObjectResult>(actionResult.Result);
    }

    [Fact]
    public async Task PostHero_PersistsUserIdFromJwtAndDeviceIdMetadataFromHeader()
    {
        await using var db = CreateDbContext();
        var controller = CreateHeroController(db, userId: UserA, deviceId: DeviceA);

        var actionResult = await controller.PostHero(new CreateHeroRequest { Name = "Alex" });

        var created = Assert.IsType<CreatedAtActionResult>(actionResult.Result);
        Assert.IsType<HeroDto>(created.Value);
        var stored = await db.Heroes.SingleAsync();
        Assert.Equal(UserA, stored.UserId);
        Assert.Equal(DeviceA, ReadOwnerDeviceId(stored));
    }

    [Fact]
    public async Task PostHero_WithoutDeviceHeader_PersistsEmptyDeviceMetadata()
    {
        await using var db = CreateDbContext();
        var controller = CreateHeroController(db, userId: UserA, deviceId: null);

        var actionResult = await controller.PostHero(new CreateHeroRequest { Name = "Alex" });

        Assert.IsType<CreatedAtActionResult>(actionResult.Result);
        var stored = await db.Heroes.SingleAsync();
        Assert.Equal(UserA, stored.UserId);
        Assert.Equal(string.Empty, ReadOwnerDeviceId(stored));
    }

    [Fact]
    public async Task GetHero_ForOtherUser_ReturnsNotFound()
    {
        await using var db = CreateDbContext();
        var hero = await CreateHeroAsync(db, "Alex", userId: UserA, ownerDeviceId: DeviceA);
        var controller = CreateHeroController(db, userId: UserB, deviceId: DeviceB);

        var actionResult = await controller.GetHero(hero.Id);

        Assert.IsType<NotFoundResult>(actionResult.Result);
    }

    [Fact]
    public async Task GetCurrentHero_ForCurrentUser_ReturnsOwnedHero()
    {
        await using var db = CreateDbContext();
        var hero = await CreateHeroAsync(db, "Alex", userId: UserA, ownerDeviceId: DeviceA);
        var controller = CreateHeroController(db, userId: UserA, deviceId: DeviceA);
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
    public async Task PostTask_WithoutHeroId_UsesHeroOwnedByCurrentUser()
    {
        await using var db = CreateDbContext();
        var heroA = await CreateHeroAsync(db, "Alex", userId: UserA, ownerDeviceId: DeviceA);
        var heroB = await CreateHeroAsync(db, "Blair", userId: UserB, ownerDeviceId: DeviceB);
        var controller = CreateTaskController(db, userId: UserB, deviceId: DeviceB);

        var actionResult = await controller.PostTask(new CreateTaskRequest
        {
            Title = "User scoped task",
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

    private static HeroController CreateHeroController(ApplicationDbContext db, int userId, string? deviceId)
    {
        var controller = HeroController.CreateForTests(db, new HeroTimeService());
        controller.ControllerContext = new ControllerContext
        {
            HttpContext = TestAuthHelpers.CreateAuthenticatedHttpContext(userId, deviceId),
        };
        return controller;
    }

    private static TaskController CreateTaskController(ApplicationDbContext db, int userId, string? deviceId)
    {
        var controller = TaskController.CreateForTests(db, new GameEngineService(), new HeroTimeService());
        controller.ControllerContext = new ControllerContext
        {
            HttpContext = TestAuthHelpers.CreateAuthenticatedHttpContext(userId, deviceId),
        };
        return controller;
    }

    private static async Task<Hero> CreateHeroAsync(ApplicationDbContext db, string name, int userId, string ownerDeviceId)
    {
        var hero = new Hero
        {
            Name = name,
            UserId = userId,
            Gold = 100,
            Level = 1,
            CurrentHp = GameConstants.BaseHp,
            MaxHp = GameConstants.BaseHp,
            TimeZoneId = "UTC",
            OwnerDeviceId = ownerDeviceId,
        };
        db.Heroes.Add(hero);
        await db.SaveChangesAsync();
        return hero;
    }

    private static string ReadOwnerDeviceId(Hero hero)
    {
        var property = typeof(Hero).GetProperty("OwnerDeviceId");
        Assert.NotNull(property);
        return Assert.IsType<string>(property!.GetValue(hero));
    }

    private ApplicationDbContext CreateDbContext() => new(_options);
}
