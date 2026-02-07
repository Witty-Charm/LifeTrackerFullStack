using LifeTracker.Constants;

namespace LifeTracker.Models;

public enum TaskType
{
    Habit = 1,
    OneTime = 2
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
    
    public Hero? Hero { get; set; }
    public Streak? Streak { get; set; }
    
    /// <summary>
    /// Get base XP reward from GDD tables (before multipliers).
    /// </summary>
    public int GetBaseRewardXP()
    {
        var (xp, _) = Type == TaskType.Habit 
            ? GameConstants.GetHabitReward(Difficulty)
            : GameConstants.GetOneTimeReward(Difficulty);
        return xp;
    }

    /// <summary>
    /// Get Gold reward from GDD tables.
    /// </summary>
    public int GetGoldReward()
    {
        var (_, gold) = Type == TaskType.Habit 
            ? GameConstants.GetHabitReward(Difficulty)
            : GameConstants.GetOneTimeReward(Difficulty);
        return gold;
    }

    /// <summary>
    /// Get HP penalty for failing this task (from GDD tables).
    /// </summary>
    public int GetHpPenalty()
    {
        var (hpLoss, _) = Type == TaskType.Habit 
            ? GameConstants.GetHabitPenalty(Difficulty)
            : GameConstants.GetOneTimePenalty(Difficulty);
        return hpLoss;
    }

    /// <summary>
    /// Get Gold penalty for failing this task (from GDD tables).
    /// </summary>
    public int GetGoldPenalty()
    {
        var (_, goldLoss) = Type == TaskType.Habit 
            ? GameConstants.GetHabitPenalty(Difficulty)
            : GameConstants.GetOneTimePenalty(Difficulty);
        return goldLoss;
    }

    public bool IsOverdue() => 
        DueDate.HasValue && DateTime.UtcNow > DueDate.Value && !IsCompleted;
}
