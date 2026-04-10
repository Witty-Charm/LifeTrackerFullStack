using Microsoft.EntityFrameworkCore;
using LifeTracker.Data;
using LifeTracker.Models;

namespace LifeTracker.Services;

public class ShopService : IShopService
{
    private readonly ApplicationDbContext _db;

    public ShopService(ApplicationDbContext db) => _db = db;

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


        if (item.ItemType == 4)
        {
            if (string.IsNullOrWhiteSpace(clientTimeZone))
                return (null, "Client timezone is required for shield purchase");
            if (clientLocalDateTime is null)
                return (null, "Client local datetime is required for shield purchase");
        }

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
                4 => await ApplyStreakShield(heroId, clientTimeZone!, clientLocalDateTime!.Value),
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

    private static DateTimeOffset ToNextLocalMidnightUtc(DateTimeOffset clientLocal, string ianaTz)
    {
        var tz = TimeZoneInfo.FindSystemTimeZoneById(ianaTz);
        var localDate = TimeZoneInfo.ConvertTime(clientLocal, tz).Date;
        var nextMidnightLocal = localDate.AddDays(1);
        var unspecified = new DateTime(nextMidnightLocal.Year, nextMidnightLocal.Month, nextMidnightLocal.Day, 0, 0, 0, DateTimeKind.Unspecified);
        var localWithOffset = TimeZoneInfo.ConvertTimeToUtc(unspecified, tz);
        return new DateTimeOffset(localWithOffset, TimeSpan.Zero);
    }

    private async Task<string> ApplyStreakShield(int heroId, string clientTimeZone, DateTimeOffset clientLocalDateTime)
    {
        var streaks = await _db.Streaks.Where(s => s.HeroId == heroId).ToListAsync();
        var now = DateTimeOffset.UtcNow;

        var activeShield = streaks.FirstOrDefault(s => s.IsShieldActive && s.ShieldExpiresAtUtc >= now);
        if (activeShield is not null)
            throw new InvalidOperationException("Shield already active until local midnight");

        var expiresUtc = ToNextLocalMidnightUtc(clientLocalDateTime, clientTimeZone);

        var tz = TimeZoneInfo.FindSystemTimeZoneById(clientTimeZone);
        var clientDate = TimeZoneInfo.ConvertTime(clientLocalDateTime, tz).Date;

        foreach (var streak in streaks)
        {
            streak.IsShieldActive = true;
            streak.ShieldExpiresAtUtc = expiresUtc;
            streak.ShieldFailConsumed = false;
            streak.UpdatedAt = now;

            if (streak.ShieldBackupBreakAtUtc.HasValue)
            {
                var breakLocalDate = TimeZoneInfo.ConvertTime(streak.ShieldBackupBreakAtUtc.Value, tz).Date;
                if (breakLocalDate == clientDate && streak.ShieldBackupCurrentDays.HasValue)
                {
                    streak.CurrentDays = streak.ShieldBackupCurrentDays.Value;
                    streak.ShieldBackupCurrentDays = null;
                    streak.ShieldBackupBreakAtUtc = null;
                }
            }
            else
            {
                streak.ShieldBackupCurrentDays = null;
                streak.ShieldBackupBreakAtUtc = null;
            }
        }

        return streaks.Count == 0 ? "" : $"Streak shield active until local midnight ({clientTimeZone})";
    }

    private static string ApplyRecoveryReset(Hero hero)
    {
        hero.RecoveryEndsAt = null;
        return "Recovery debuff removed";
    }
}
