using LifeTracker.Models;

namespace LifeTracker.Services;

public record ShopItemDto(int Id, string Name, string Description, int Price, int ItemType, int EffectValue);

public record BuyItemRequest(
    int HeroId,
    int ItemId,
    string? ClientTimeZone = null,
    DateTimeOffset? ClientLocalDateTime = null
);

public record BuyResultDto(int NewGold, int NewHp, int MaxHp, ShopItemDto PurchasedItem, string Message, string Effect, int XpBoostPercent, int XpBoostTasksRemaining);

public record PurchasedItemDto(int PurchaseId, ShopItemDto Item, DateTimeOffset PurchasedAt);

public interface IShopService
{
    Task<IEnumerable<ShopItemDto>> GetItemsAsync();
    Task<(BuyResultDto? Result, string? Error)> BuyItemAsync(int heroId, int itemId, string? clientTimeZone, DateTimeOffset? clientLocalDateTime);
    Task<IEnumerable<PurchasedItemDto>> GetInventoryAsync(int heroId);
}
