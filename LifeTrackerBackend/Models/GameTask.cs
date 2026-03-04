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
    public DateTimeOffset? DueDate { get; set; }
    public string? RepeatPattern { get; set; }

    public bool IsCompleted { get; set; } = false;
    public int CompletionCount { get; set; } = 0;
    public int FailCount { get; set; } = 0;

    public DateTimeOffset? LastCompletedAt { get; set; }
    public DateTimeOffset? OverdueProcessedAt { get; set; }
    public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;
    public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.UtcNow;

    public Hero? Hero { get; set; }
    public Streak? Streak { get; set; }

    public int GetBaseRewardXP()
    {
        var (xp, _) = Type == TaskType.Habit
            ? GameConstants.GetHabitReward(Difficulty)
            : GameConstants.GetOneTimeReward(Difficulty);
        return xp;
    }

    public int GetGoldReward()
    {
        var (_, gold) = Type == TaskType.Habit
            ? GameConstants.GetHabitReward(Difficulty)
            : GameConstants.GetOneTimeReward(Difficulty);
        return gold;
    }

    public int GetHpPenalty()
    {
        var (hpLoss, _) = Type == TaskType.Habit
            ? GameConstants.GetHabitPenalty(Difficulty)
            : GameConstants.GetOneTimePenalty(Difficulty);
        return hpLoss;
    }

    public int GetGoldPenalty()
    {
        var (_, goldLoss) = Type == TaskType.Habit
            ? GameConstants.GetHabitPenalty(Difficulty)
            : GameConstants.GetOneTimePenalty(Difficulty);
        return goldLoss;
    }

    public bool IsOverdue() =>
        DueDate.HasValue && DateTimeOffset.UtcNow > DueDate.Value && !IsCompleted;
}