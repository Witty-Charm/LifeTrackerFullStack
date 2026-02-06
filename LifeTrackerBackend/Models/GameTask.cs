namespace LifeTracker.Models;

public enum TaskType
{
    Habit = 1,
    OneTime = 2
}

public enum TaskDifficulty
{
    Easy = 1,
    Medium = 2,
    Hard = 3,
    Epic = 4
}

public class GameTask
{
    public int Id { get; set; }
    public int HeroId { get; set; }
    
    public string Title { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    
    public TaskType Type { get; set; } = TaskType.OneTime;           
    public TaskDifficulty Difficulty { get; set; } = TaskDifficulty.Easy;  
    
    public bool IsActive { get; set; } = true;                    
    public DateTime? DueDate { get; set; }                         
    public string? RepeatPattern { get; set; }                     
    
    public bool IsCompleted { get; set; } = false;
    public int CompletionCount { get; set; } = 0;               
    public int FailCount { get; set; } = 0;        
    
    public DateTime? LastCompletedAt { get; set; }     
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
    
    public int RewardXP { get; set; } = 10;
    
    public Hero? Hero { get; set; }
    public Streak? Streak { get; set; }
    
    public int GetBaseRewardXP()
    {
        double multiplier = Difficulty switch
        {
            TaskDifficulty.Easy => 1.0,
            TaskDifficulty.Medium => 1.5,
            TaskDifficulty.Hard => 2.5,
            TaskDifficulty.Epic => 4.0,
            _ => 1.0
        };
        return (int)Math.Ceiling(RewardXP * multiplier);
    }

    public int GetFailPenalty()
    {
        return Difficulty switch
        {
            TaskDifficulty.Easy => 5,
            TaskDifficulty.Medium => 10,
            TaskDifficulty.Hard => 20,
            TaskDifficulty.Epic => 35,
            _ => 5
        };
    }

    public int GetGoldReward()
    {
        return Difficulty switch
        {
            TaskDifficulty.Easy => 5,
            TaskDifficulty.Medium => 12,
            TaskDifficulty.Hard => 25,
            TaskDifficulty.Epic => 50,
            _ => 5
        };
    }

    public bool IsOverdue()
    {
        return DueDate.HasValue && DateTime.UtcNow > DueDate.Value && !IsCompleted;
    }
}

