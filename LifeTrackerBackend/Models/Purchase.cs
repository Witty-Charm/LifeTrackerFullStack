namespace LifeTracker.Models;

public class Purchase
{
    public int Id { get; set; }
    public int HeroId { get; set; }
    public int ShopItemId { get; set; }
    public int GoldSpent { get; set; }
    public DateTimeOffset PurchasedAt { get; set; } = DateTimeOffset.UtcNow;

    public Hero? Hero { get; set; }
    public ShopItem? ShopItem { get; set; }
}
