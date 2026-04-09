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

    public async Task<(BuyResultDto? Result, string? Error)> BuyItemAsync(int heroId, int itemId)
    {
        var hero = await _db.Heroes.FindAsync(heroId);
        if (hero is null)
            return (null, "Hero not found");

        if (hero.IsDead)
            return (null, "Dead heroes cannot shop. Respawn first.");

        var item = await _db.ShopItems.FindAsync(itemId);
        if (item is null)
            return (null, "Item not found");

        if (hero.Gold < item.Price)
            return (null, $"Not enough gold. Need {item.Price}, have {hero.Gold}.");

        hero.Gold -= item.Price;
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
            PurchasedItem: MapItemToDto(item),
            Message: $"Purchased {item.Name} for {item.Price} gold!"
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
}
