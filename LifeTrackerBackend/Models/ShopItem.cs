    namespace LifeTracker.Models;

    public class ShopItem
    {
        public int Id { get; set; }
        public string Name { get; set; } = string.Empty;
        public string Description { get; set; } = string.Empty;
        public int Price { get; set; }
        public int ItemType { get; set; }
        public int EffectValue { get; set; }
    }