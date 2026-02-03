namespace LifeTracker.Models;

public enum Difficulty
{
    Easy,
    Medium,
    Hard
}

public class GameTask
{
    public int Id { get; set; }
    public string Title { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public Difficulty Difficulty { get; set; }
    public bool IsCompleted { get; set; } = false;
    public int RewardXP { get; set; }
}

