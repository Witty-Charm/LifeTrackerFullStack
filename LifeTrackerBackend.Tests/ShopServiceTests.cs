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
    public async Task BuyItem_StreakShield_ActivatesHeroShield()
    {
        var db = CreateContext(nameof(BuyItem_StreakShield_ActivatesHeroShield));
        var hero = new Hero { Id = 1, Gold = 200 };
        var item = new ShopItem { Id = 1, Name = "Streak Shield", ItemType = 4, EffectValue = 1, Price = 80 };
        db.Heroes.Add(hero);
        db.ShopItems.Add(item);
        db.Streaks.Add(new Streak { Id = 1, HeroId = hero.Id, CurrentDays = 3 });
        db.Streaks.Add(new Streak { Id = 2, HeroId = hero.Id, CurrentDays = 5 });
        await db.SaveChangesAsync();

        var service = CreateService(db);
        var (result, error) = await service.BuyItemAsync(hero.Id, item.Id, null, null);

        Assert.Null(error);
        Assert.NotNull(result);
        var updatedHero = db.Heroes.Single();
        Assert.True(updatedHero.IsShieldActive);
        Assert.NotNull(updatedHero.ShieldActivatedAtUtc);
        Assert.All(db.Streaks.ToList(), s => Assert.False(s.IsShieldActive));
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
        Assert.Equal("Revival Token is not needed right now.", error);
        Assert.Equal(200, db.Heroes.Single().Gold);
        Assert.Empty(db.Purchases);
    }

    [Fact]
    public async Task BuyItem_HealthPotion_RejectsWhenHpIsFull()
    {
        var db = CreateContext(nameof(BuyItem_HealthPotion_RejectsWhenHpIsFull));
        var hero = new Hero { Id = 1, CurrentHp = 20, MaxHp = 20, Gold = 100 };
        var item = new ShopItem { Id = 1, Name = "Health Potion", ItemType = 1, EffectValue = 15, Price = 20 };
        db.Heroes.Add(hero);
        db.ShopItems.Add(item);
        await db.SaveChangesAsync();

        var service = CreateService(db);
        var (result, error) = await service.BuyItemAsync(hero.Id, item.Id, null, null);

        Assert.Null(result);
        Assert.Equal("HP is already full.", error);
        Assert.Equal(100, db.Heroes.Single().Gold);
        Assert.Empty(db.Purchases);
    }

    [Fact]
    public async Task BuyItem_ElixirOfLife_RejectsWhenHpIsFull()
    {
        var db = CreateContext(nameof(BuyItem_ElixirOfLife_RejectsWhenHpIsFull));
        var hero = new Hero { Id = 1, CurrentHp = 50, MaxHp = 50, Gold = 150 };
        var item = new ShopItem { Id = 1, Name = "Elixir of Life", ItemType = 2, EffectValue = 100, Price = 40 };
        db.Heroes.Add(hero);
        db.ShopItems.Add(item);
        await db.SaveChangesAsync();

        var service = CreateService(db);
        var (result, error) = await service.BuyItemAsync(hero.Id, item.Id, null, null);

        Assert.Null(result);
        Assert.Equal("HP is already full.", error);
        Assert.Equal(150, db.Heroes.Single().Gold);
        Assert.Empty(db.Purchases);
    }

    [Fact]
    public async Task BuyItem_XpBoost_RejectsWhenBoostIsAlreadyActive()
    {
        var db = CreateContext(nameof(BuyItem_XpBoost_RejectsWhenBoostIsAlreadyActive));
        var hero = new Hero { Id = 1, Gold = 200, XpBoostPercent = 25, XpBoostTasksRemaining = 3 };
        var item = new ShopItem { Id = 1, Name = "XP Boost", ItemType = 3, EffectValue = 25, Price = 60 };
        db.Heroes.Add(hero);
        db.ShopItems.Add(item);
        await db.SaveChangesAsync();

        var service = CreateService(db);
        var (result, error) = await service.BuyItemAsync(hero.Id, item.Id, null, null);

        Assert.Null(result);
        Assert.Equal("XP Boost is already active.", error);
        Assert.Equal(200, db.Heroes.Single().Gold);
        Assert.Empty(db.Purchases);
    }

    [Fact]
    public async Task BuyItem_StreakShield_RejectsWhenShieldIsAlreadyActive()
    {
        var db = CreateContext(nameof(BuyItem_StreakShield_RejectsWhenShieldIsAlreadyActive));
        var hero = new Hero { Id = 1, Gold = 200, IsShieldActive = true, ShieldActivatedAtUtc = DateTimeOffset.UtcNow.AddMinutes(-5) };
        var item = new ShopItem { Id = 1, Name = "Streak Shield", ItemType = 4, EffectValue = 1, Price = 80 };
        db.Heroes.Add(hero);
        db.ShopItems.Add(item);
        await db.SaveChangesAsync();

        var service = CreateService(db);
        var (result, error) = await service.BuyItemAsync(hero.Id, item.Id, null, null);

        Assert.Null(result);
        Assert.Equal("Shield is already active.", error);
        Assert.Equal(200, db.Heroes.Single().Gold);
        Assert.Empty(db.Purchases);
    }

}
