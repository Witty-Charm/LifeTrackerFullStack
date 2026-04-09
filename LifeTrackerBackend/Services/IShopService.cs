using LifeTracker.Models;

namespace LifeTracker.Services;

public record ShopItemDto(int Id, string Name, string Description, int Price, int ItemType, int EffectValue);

public record BuyItemRequest(int HeroId, int ItemId);

public record BuyResultDto(int NewGold, ShopItemDto PurchasedItem, string Message);

public record PurchasedItemDto(int PurchaseId, ShopItemDto Item, DateTimeOffset PurchasedAt);

public interface IShopService
{
    Task<IEnumerable<ShopItemDto>> GetItemsAsync();
    Task<(BuyResultDto? Result, string? Error)> BuyItemAsync(int heroId, int itemId);
    Task<IEnumerable<PurchasedItemDto>> GetInventoryAsync(int heroId);
}
