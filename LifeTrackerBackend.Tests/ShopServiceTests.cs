using System;
using System.Linq;
using System.Threading.Tasks;
using LifeTracker.Data;
using LifeTracker.Models;
using LifeTracker.Services;
using LifeTracker.Services.Time;
using Microsoft.EntityFrameworkCore;
using Xunit;

namespace LifeTrackerBackend.Tests;

public class ShopServiceTests
{
    private static ApplicationDbContext CreateContext(string name)
    {
        var options = new DbContextOptionsBuilder<ApplicationDbContext>()
            .UseInMemoryDatabase(name)
            .Options;
        return new ApplicationDbContext(options);
    }

    private static ShopService CreateService(ApplicationDbContext db) => new(db, new HeroTimeService());

    private static T ReadProperty<T>(object source, string propertyName)
    {
        var property = source.GetType().GetProperty(propertyName);
        Assert.NotNull(property);
        return Assert.IsType<T>(property!.GetValue(source));
    }

    [Fact]
    public async Task BuyItem_HealthPotion_HealsAndSetsEffectText()
    {
        var db = CreateContext(nameof(BuyItem_HealthPotion_HealsAndSetsEffectText));
        var hero = new Hero { Id = 1, CurrentHp = 10, MaxHp = 20, Gold = 100 };
        var item = new ShopItem { Id = 1, Name = "Health Potion", ItemType = 1, EffectValue = 15, Price = 20 };
        db.Heroes.Add(hero);
        db.ShopItems.Add(item);
        await db.SaveChangesAsync();

        var service = CreateService(db);
        var (result, error) = await service.BuyItemAsync(hero.Id, item.Id, null, null);

        Assert.Null(error);
        Assert.NotNull(result);
        Assert.Equal(hero.MaxHp, db.Heroes.Single().CurrentHp); Assert.Equal("Restored 15 HP", result!.Effect);
    }

    [Fact]
    public async Task BuyItem_XpBoost_SetsFieldsAndEffectText()
    {
        var db = CreateContext(nameof(BuyItem_XpBoost_SetsFieldsAndEffectText));
        var hero = new Hero { Id = 1, Gold = 200 };
        var item = new ShopItem { Id = 1, Name = "XP Boost", ItemType = 3, EffectValue = 25, Price = 60 };
        db.Heroes.Add(hero);
        db.ShopItems.Add(item);
        await db.SaveChangesAsync();

        var service = CreateService(db);
        var (result, error) = await service.BuyItemAsync(hero.Id, item.Id, null, null);

        Assert.Null(error);
        Assert.NotNull(result);
        var updatedHero = db.Heroes.Single();
        Assert.Equal(25, updatedHero.XpBoostPercent);
        Assert.Equal(5, updatedHero.XpBoostTasksRemaining);
        Assert.Equal("+25% XP boost for next 5 tasks", result!.Effect);
    }

    [Fact]
    public async Task BuyItem_StreakShield_ActivatesAllStreaks()
    {
        var db = CreateContext(nameof(BuyItem_StreakShield_ActivatesAllStreaks));
        var hero = new Hero { Id = 1, Gold = 200 };
        var item = new ShopItem { Id = 1, Name = "Streak Shield", ItemType = 4, EffectValue = 1, Price = 80 };
        db.Heroes.Add(hero);
        db.ShopItems.Add(item);
        db.Streaks.Add(new Streak { Id = 1, HeroId = hero.Id, CurrentDays = 3 });
        db.Streaks.Add(new Streak { Id = 2, HeroId = hero.Id, CurrentDays = 5 });
        await db.SaveChangesAsync();

        var clientTimeZone = "UTC";
        var clientLocalDateTime = DateTimeOffset.UtcNow;

        var service = CreateService(db);
        var (result, error) = await service.BuyItemAsync(hero.Id, item.Id, clientTimeZone, clientLocalDateTime);

        Assert.Null(error);
        Assert.NotNull(result);
        var streaks = db.Streaks.ToList();
        Assert.All(streaks, s => Assert.True(s.IsShieldActive));
        Assert.All(streaks, s => Assert.NotNull(s.ShieldExpiresAtUtc));
    }

    [Fact]
    public async Task BuyItem_RevivalToken_ClearsRecovery()
    {
        var db = CreateContext(nameof(BuyItem_RevivalToken_ClearsRecovery));
        var hero = new Hero { Id = 1, Gold = 200, RecoveryEndsAt = DateTimeOffset.UtcNow.AddHours(2) };
        var item = new ShopItem { Id = 1, Name = "Revival Token", ItemType = 5, EffectValue = 1, Price = 100 };
        db.Heroes.Add(hero);
        db.ShopItems.Add(item);
        await db.SaveChangesAsync();

        var service = CreateService(db);
        var (result, error) = await service.BuyItemAsync(hero.Id, item.Id, null, null);

        Assert.Null(error);
        Assert.NotNull(result);
        Assert.Null(db.Heroes.Single().RecoveryEndsAt);
        Assert.Equal("Recovery debuff removed", result!.Effect);
        Assert.False(ReadProperty<bool>(result!, "RecoveryDebuffActive"));
        Assert.Equal(1.0, ReadProperty<double>(result!, "RecoveryMultiplier"));
    }

    [Fact]
    public async Task BuyItem_RevivalToken_RejectsPurchaseOutsideRecovery()
    {
        var db = CreateContext(nameof(BuyItem_RevivalToken_RejectsPurchaseOutsideRecovery));
        var hero = new Hero { Id = 1, Gold = 200, RecoveryEndsAt = null };
        var item = new ShopItem { Id = 1, Name = "Revival Token", ItemType = 5, EffectValue = 1, Price = 100 };
        db.Heroes.Add(hero);
        db.ShopItems.Add(item);
        await db.SaveChangesAsync();

        var service = CreateService(db);
        var (result, error) = await service.BuyItemAsync(hero.Id, item.Id, null, null);

        Assert.Null(result);
        Assert.Equal("Revival Token can only be used during recovery.", error);
        Assert.Equal(200, db.Heroes.Single().Gold);
        Assert.Empty(db.Purchases);
    }

    [Fact]
    public async Task BuyShield_SetsExpiry_ToLocalMidnight()
    {
        var db = CreateContext(nameof(BuyShield_SetsExpiry_ToLocalMidnight));
        var hero = new Hero { Id = 1, Gold = 200 };
        var item = new ShopItem { Id = 1, Name = "Streak Shield", ItemType = 4, EffectValue = 1, Price = 80 };
        db.Heroes.Add(hero);
        db.ShopItems.Add(item);
        db.Streaks.Add(new Streak { Id = 1, HeroId = hero.Id, CurrentDays = 3 });
        await db.SaveChangesAsync();

        var clientTimeZone = "Europe/Moscow";
        var clientLocalDateTime = new DateTimeOffset(2026, 4, 10, 23, 50, 0, TimeSpan.FromHours(3));

        var service = CreateService(db);
        var (result, error) = await service.BuyItemAsync(hero.Id, item.Id, clientTimeZone, clientLocalDateTime);

        Assert.Null(error);
        Assert.NotNull(result);

        var streak = db.Streaks.Single();
        Assert.True(streak.IsShieldActive);
        Assert.NotNull(streak.ShieldExpiresAtUtc);

        var expectedExpiry = new DateTimeOffset(2026, 4, 10, 21, 0, 0, TimeSpan.Zero);
        Assert.Equal(expectedExpiry, streak.ShieldExpiresAtUtc!.Value);
    }

    [Fact]
    public async Task BuyShield_RestoresSameDayBreak()
    {
        var db = CreateContext(nameof(BuyShield_RestoresSameDayBreak));
        var hero = new Hero { Id = 1, Gold = 200 };
        var item = new ShopItem { Id = 1, Name = "Streak Shield", ItemType = 4, EffectValue = 1, Price = 80 };
        db.Heroes.Add(hero);
        db.ShopItems.Add(item);

        var clientTimeZone = "Europe/Moscow";
        var breakTime = new DateTimeOffset(2026, 4, 10, 10, 0, 0, TimeSpan.FromHours(3));
        var purchaseTime = new DateTimeOffset(2026, 4, 10, 15, 0, 0, TimeSpan.FromHours(3));

        var streak = new Streak
        {
            Id = 1,
            HeroId = hero.Id,
            CurrentDays = 0,
            ShieldBackupCurrentDays = 7,
            ShieldBackupBreakAtUtc = breakTime,
            LastBreakLocalDate = "2026-04-10"
        };
        db.Streaks.Add(streak);
        await db.SaveChangesAsync();

        var service = CreateService(db);
        var (result, error) = await service.BuyItemAsync(hero.Id, item.Id, clientTimeZone, purchaseTime);

        Assert.Null(error);
        Assert.NotNull(result);

        var updatedStreak = db.Streaks.Single();
        Assert.Equal(7, updatedStreak.CurrentDays);
        Assert.Null(updatedStreak.ShieldBackupCurrentDays);
        Assert.Null(updatedStreak.ShieldBackupBreakAtUtc);
    }
}
