namespace LifeTracker.Models;

public class HeroAchievement
{
    public int Id { get; set; }
    public int HeroId { get; set; }
    public string Key { get; set; } = string.Empty;
    public DateTime UnlockedAt { get; set; }
    public int GoldReward { get; set; }
}
