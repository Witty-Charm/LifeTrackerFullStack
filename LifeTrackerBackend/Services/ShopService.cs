using Microsoft.EntityFrameworkCore;
using LifeTracker.Data;
using LifeTracker.Models;
using LifeTracker.Services.Time;

namespace LifeTracker.Services;

public class ShopService : IShopService
{
    private readonly ApplicationDbContext _db;
    private readonly IHeroTimeService _heroTimeService;

    public ShopService(ApplicationDbContext db, IHeroTimeService heroTimeService)
    {
        _db = db;
        _heroTimeService = heroTimeService;
    }

    public async Task<IEnumerable<ShopItemDto>> GetItemsAsync()
    {
        var items = await _db.ShopItems.ToListAsync();
        return items.Select(MapItemToDto);
    }

    public async Task<(BuyResultDto? Result, string? Error)> BuyItemAsync(int heroId, int itemId, string? clientTimeZone, DateTimeOffset? clientLocalDateTime)
    {
        var hero = await _db.Heroes.FindAsync(heroId);
        if (hero is null)
            return (null, "Hero not found");

        if (hero.IsDead)
            return (null, "Dead heroes cannot shop. Respawn first.");

        var item = await _db.ShopItems.FindAsync(itemId);
        if (item is null)
            return (null, "Item not found");


        if (item.ItemType == 4 && !string.IsNullOrWhiteSpace(clientTimeZone) && !_heroTimeService.IsValidIana(clientTimeZone))
            return (null, "Invalid IANA timezone id");

        if (hero.Gold < item.Price)
            return (null, $"Not enough gold. Need {item.Price}, have {hero.Gold}.");

        hero.Gold -= item.Price;

        string effectMessage;
        try
        {
            effectMessage = item.ItemType switch
            {
                1 or 2 => ApplyHeal(hero, item.EffectValue),
                3 => ApplyXpBoost(hero, item.EffectValue),
                4 => await ApplyStreakShield(hero, clientTimeZone, clientLocalDateTime),
                5 => ApplyRecoveryReset(hero),
                _ => string.Empty
            };
        }
        catch (InvalidOperationException ex)
        {
            return (null, ex.Message);
        }

        if (item.ItemType is 1 or 2)
        {

        }

        hero.UpdatedAt = DateTimeOffset.UtcNow;

        var economy = await _db.EconomyBalances.FirstOrDefaultAsync(e => e.HeroId == heroId);
        if (economy is not null)
        {
            economy.TotalGoldSpent += item.Price;
            economy.UpdatedAt = DateTimeOffset.UtcNow;
        }

        var purchase = new Purchase
        {
            HeroId = heroId,
            ShopItemId = itemId,
            GoldSpent = item.Price,
            PurchasedAt = DateTimeOffset.UtcNow,
        };
        _db.Purchases.Add(purchase);

        await _db.SaveChangesAsync();

        var result = new BuyResultDto(
            NewGold: hero.Gold,
            NewHp: hero.CurrentHp,
            MaxHp: hero.MaxHp,
            PurchasedItem: MapItemToDto(item),
            Message: $"Purchased {item.Name} for {item.Price} gold!",
            Effect: effectMessage,
            XpBoostPercent: hero.XpBoostPercent,
            XpBoostTasksRemaining: hero.XpBoostTasksRemaining
        );
        return (result, null);
    }

    public async Task<IEnumerable<PurchasedItemDto>> GetInventoryAsync(int heroId)
    {
        var purchases = await _db.Purchases
            .Include(p => p.ShopItem)
            .Where(p => p.HeroId == heroId)
            .AsNoTracking()
            .ToListAsync();

        return purchases
            .Where(p => p.ShopItem is not null)
            .OrderByDescending(p => p.PurchasedAt)
            .Select(p => new PurchasedItemDto(
                PurchaseId: p.Id,
                Item: MapItemToDto(p.ShopItem!),
                PurchasedAt: p.PurchasedAt
            ));
    }

    private static ShopItemDto MapItemToDto(ShopItem item) =>
        new(item.Id, item.Name, item.Description, item.Price, item.ItemType, item.EffectValue);

    private static string ApplyHeal(Hero hero, int healAmount)
    {
        hero.CurrentHp = Math.Min(hero.MaxHp, hero.CurrentHp + healAmount);
        return $"Restored {healAmount} HP";
    }

    private static string ApplyXpBoost(Hero hero, int percent)
    {
        hero.XpBoostPercent = percent;
        hero.XpBoostTasksRemaining = 5;
        return $"+{percent}% XP boost for next 5 tasks";
    }

    private async Task<string> ApplyStreakShield(Hero hero, string? clientTimeZone, DateTimeOffset? clientLocalDateTime)
    {
        var now = clientLocalDateTime ?? DateTimeOffset.UtcNow;
        var heroTimeZone = !string.IsNullOrWhiteSpace(clientTimeZone)
            ? clientTimeZone
            : _heroTimeService.ResolveEffectiveTimeZone(hero, now);
        var todayLocalDate = _heroTimeService.GetLocalDate(now, heroTimeZone);

        var streaks = await _db.Streaks.Where(s => s.HeroId == hero.Id).ToListAsync();

        var activeShield = streaks.FirstOrDefault(s => s.IsShieldActive && s.ShieldExpiresAtUtc >= now);
        if (activeShield is not null)
            throw new InvalidOperationException("Shield already active until local midnight");

        var expiresUtc = _heroTimeService.GetNextLocalMidnightUtc(now, heroTimeZone);

        foreach (var streak in streaks)
        {
            streak.IsShieldActive = true;
            streak.ShieldExpiresAtUtc = expiresUtc;
            streak.ShieldFailConsumed = false;
            streak.UpdatedAt = now;

            if (streak.ShieldBackupBreakAtUtc.HasValue)
            {
                var breakLocalDate = _heroTimeService.GetLocalDate(streak.ShieldBackupBreakAtUtc.Value, heroTimeZone);
                if (breakLocalDate == todayLocalDate &&
                    streak.ShieldBackupCurrentDays.HasValue &&
                    streak.LastBreakLocalDate == _heroTimeService.FormatLocalDate(todayLocalDate))
                {
                    streak.CurrentDays = streak.ShieldBackupCurrentDays.Value;
                    streak.LastBreakLocalDate = null;
                }
            }

            streak.ShieldBackupCurrentDays = null;
            streak.ShieldBackupBreakAtUtc = null;
        }

        return streaks.Count == 0 ? "" : $"Streak shield active until local midnight ({heroTimeZone})";
    }

    private static string ApplyRecoveryReset(Hero hero)
    {
        hero.RecoveryEndsAt = null;
        return "Recovery debuff removed";
    }
}
