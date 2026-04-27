using LifeTracker.Constants;

namespace LifeTracker.Models;

public enum TaskType
{
    Habit = 1,
    OneTime = 2,
    Daily = 3
}

public enum HabitPolarity
{
    Positive = 1,
    Negative = 2,
    Both = 3
}

public class GameTask
{
    public int Id { get; set; }
    public int HeroId { get; set; }

    public string Title { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;

    public TaskType Type { get; set; } = TaskType.OneTime;
    public TaskDifficulty Difficulty { get; set; } = TaskDifficulty.Easy;
    public HabitPolarity Polarity { get; set; } = HabitPolarity.Both;

    public bool IsActive { get; set; } = true;
    public DateTimeOffset? DueDate { get; set; }
    public string? RepeatPattern { get; set; }
    public string? ChecklistJson { get; set; }
    public string? RemindersJson { get; set; }

    public bool IsCompleted { get; set; } = false;
    public int CompletionCount { get; set; } = 0;
    public int FailCount { get; set; } = 0;

    public DateTimeOffset? LastCompletedAt { get; set; }
    public DateTimeOffset? OverdueProcessedAt { get; set; }

    /// <summary>
    /// For Daily tasks only. The most recent scheduled local date (yyyy-MM-dd) up to and
    /// including which the missed-day penalty pipeline has already accounted for. Used by
    /// <c>POST /api/Task/check-overdue</c> to advance the cursor and apply at most one penalty
    /// per missed scheduled day. Null on legacy tasks; initialized lazily without back-fill.
    /// Unused for OneTime/Habit tasks.
    /// </summary>
    public string? LastMissedScheduledLocalDate { get; set; }

    public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;
    public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.UtcNow;

    public Hero? Hero { get; set; }
    public Streak? Streak { get; set; }
    public ICollection<DailyTaskCompletion> DailyTaskCompletions { get; set; } = new List<DailyTaskCompletion>();

    public int GetBaseRewardXP()
    {
        var (xp, _) = Type == TaskType.Habit || Type == TaskType.Daily
            ? GameConstants.GetHabitReward(Difficulty)
            : GameConstants.GetOneTimeReward(Difficulty);
        return xp;
    }

    public int GetGoldReward()
    {
        var (_, gold) = Type == TaskType.Habit || Type == TaskType.Daily
            ? GameConstants.GetHabitReward(Difficulty)
            : GameConstants.GetOneTimeReward(Difficulty);
        return gold;
    }

    public int GetHpPenalty()
    {
        var (hpLoss, _) = Type == TaskType.Habit || Type == TaskType.Daily
            ? GameConstants.GetHabitPenalty(Difficulty)
            : GameConstants.GetOneTimePenalty(Difficulty);
        return hpLoss;
    }

    public int GetGoldPenalty()
    {
        var (_, goldLoss) = Type == TaskType.Habit || Type == TaskType.Daily
            ? GameConstants.GetHabitPenalty(Difficulty)
            : GameConstants.GetOneTimePenalty(Difficulty);
        return goldLoss;
    }

    /// <summary>
    /// True iff this task should be flagged as overdue in the UI / single-shot penalty path.
    /// For Daily tasks this always returns false; their missed-day pipeline runs through
    /// <c>CheckOverdueTasks</c> using <see cref="LastMissedScheduledLocalDate"/> instead.
    /// For Daily, <see cref="DueDate"/> is the schedule *start* date, not a deadline.
    /// </summary>
    public bool IsOverdue()
    {
        if (Type == TaskType.Daily) return false;
        return DueDate.HasValue && DateTimeOffset.UtcNow > DueDate.Value && !IsCompleted;
    }
}