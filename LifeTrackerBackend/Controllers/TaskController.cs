using LifeTracker.Constants;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using LifeTracker.Data;
using LifeTracker.Models;
using LifeTracker.Services;

namespace LifeTracker.Controllers;

[ApiController]
[Route("api/[controller]")]
public class TaskController : ControllerBase
{
    private readonly ApplicationDbContext _context;
    private readonly GameEngineService _gameEngine;

    public TaskController(ApplicationDbContext context, GameEngineService gameEngine)
    {
        _context = context;
        _gameEngine = gameEngine;
    }

    [HttpGet]
    public async Task<ActionResult<IEnumerable<TaskDto>>> GetTasks([FromQuery] int? heroId = null)
    {
        var query = _context.GameTasks
            .Include(t => t.Streak)
            .Where(t => t.IsActive);

        if (heroId.HasValue)
            query = query.Where(t => t.HeroId == heroId.Value);

        var tasks = await query.ToListAsync();

        var taskDtos = tasks.Select(t => new TaskDto
        {
            Id = t.Id,
            HeroId = t.HeroId,
            Title = t.Title,
            Description = t.Description,
            Type = t.Type,
            Difficulty = t.Difficulty,
            IsCompleted = t.IsCompleted,
            IsActive = t.IsActive,
            DueDate = t.DueDate,
            IsOverdue = t.IsOverdue(),
            CompletionCount = t.CompletionCount,
            FailCount = t.FailCount,
            LastCompletedAt = t.LastCompletedAt,
            BaseXp = t.GetBaseRewardXP(),
            BaseGold = t.GetGoldReward(),
            HpPenalty = t.GetHpPenalty(),
            GoldPenalty = t.GetGoldPenalty(),
            StreakInfo = t.Streak != null
                ? new StreakInfoDto
                {
                    CurrentDays = t.Streak.CurrentDays,
                    BonusXpPercent = t.Streak.GetBonusXpPercent(),
                    Multiplier = t.Streak.GetStreakMultiplier(),
                    IsFrozen = t.Streak.IsFrozen(),
                    IsShieldActive = t.Streak.IsShieldActive
                }
                : null
        }).ToList();

        return Ok(taskDtos);
    }

    [HttpGet("{id}")]
    public async Task<ActionResult<TaskDto>> GetTask(int id)
    {
        var task = await _context.GameTasks
            .Include(t => t.Streak)
            .FirstOrDefaultAsync(t => t.Id == id);

        if (task == null)
            return NotFound();

        var dto = new TaskDto
        {
            Id = task.Id,
            HeroId = task.HeroId,
            Title = task.Title,
            Description = task.Description,
            Type = task.Type,
            Difficulty = task.Difficulty,
            IsCompleted = task.IsCompleted,
            IsActive = task.IsActive,
            DueDate = task.DueDate,
            IsOverdue = task.IsOverdue(),
            CompletionCount = task.CompletionCount,
            FailCount = task.FailCount,
            LastCompletedAt = task.LastCompletedAt,
            BaseXp = task.GetBaseRewardXP(),
            BaseGold = task.GetGoldReward(),
            HpPenalty = task.GetHpPenalty(),
            GoldPenalty = task.GetGoldPenalty(),
            StreakInfo = task.Streak != null
                ? new StreakInfoDto
                {
                    CurrentDays = task.Streak.CurrentDays,
                    BonusXpPercent = task.Streak.GetBonusXpPercent(),
                    Multiplier = task.Streak.GetStreakMultiplier(),
                    IsFrozen = task.Streak.IsFrozen(),
                    IsShieldActive = task.Streak.IsShieldActive
                }
                : null
        };

        return Ok(dto);
    }

    [HttpPost]
    public async Task<ActionResult<TaskDto>> PostTask([FromBody] CreateTaskRequest request)
    {
        if (string.IsNullOrWhiteSpace(request.Title))
            return BadRequest("Title is required");

        int heroId = request.HeroId ?? 0;
        if (heroId == 0)
        {
            var hero = await _context.Heroes.FirstOrDefaultAsync();
            if (hero == null) return BadRequest("Hero not found");
            heroId = hero.Id;
        }

        var task = new GameTask
        {
            HeroId = heroId,
            Title = request.Title,
            Description = request.Description ?? string.Empty,
            Type = request.Type,
            Difficulty = request.Difficulty,
            DueDate = request.DueDate,
            RepeatPattern = request.RepeatPattern,
            IsCompleted = false,
            IsActive = true,
            CompletionCount = 0,
            FailCount = 0,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        _context.GameTasks.Add(task);
        await _context.SaveChangesAsync();

        if (task.Type == TaskType.Habit)
        {
            var streak = new Streak
            {
                HeroId = task.HeroId,
                TaskId = task.Id,
                CurrentDays = 0,
                LongestDays = 0,
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            };
            _context.Streaks.Add(streak);
            await _context.SaveChangesAsync();
        }

        return CreatedAtAction(nameof(GetTask), new { id = task.Id }, await GetTaskDto(task.Id));
    }

    [HttpPut("{id}/complete")]
    public async Task<ActionResult<CompleteTaskResponse>> CompleteTask(int id)
    {
        var task = await _context.GameTasks
            .Include(t => t.Streak)
            .FirstOrDefaultAsync(t => t.Id == id);

        if (task == null)
            return NotFound("Task not found");

        if (task.IsCompleted && task.Type == TaskType.OneTime)
            return BadRequest("Task is already completed");

        var hero = await _context.Heroes
            .Include(h => h.EconomyBalance)
            .FirstOrDefaultAsync(h => h.Id == task.HeroId);

        if (hero == null)
            return BadRequest("Hero not found");

        if (hero.IsDead)
            return BadRequest(new
            {
                error = "Hero is dead",
                message = "Use /api/Hero/{id}/respawn to continue playing"
            });

        var economy = hero.EconomyBalance;
        if (economy == null)
        {
            economy = new EconomyBalance { HeroId = hero.Id };
            _context.EconomyBalances.Add(economy);
            hero.EconomyBalance = economy;
        }

        economy.CheckDailyReset();
        if (!economy.CanCompleteTask())
            return BadRequest(new
            {
                error = "Daily limit reached",
                message =
                    $"You have completed {economy.DailyTaskCompletions}/{economy.MaxDailyCompletions} tasks today. Try again tomorrow!",
                dailyCompletions = economy.DailyTaskCompletions,
                maxDailyCompletions = economy.MaxDailyCompletions,
                resetTime = economy.DailyResetAt.AddDays(1)
            });

        Streak? streak = null;
        if (task.Type == TaskType.Habit)
        {
            streak = task.Streak ?? await _context.Streaks
                .FirstOrDefaultAsync(s => s.HeroId == hero.Id && s.TaskId == task.Id);

            if (streak == null)
            {
                streak = new Streak
                {
                    HeroId = hero.Id,
                    TaskId = task.Id,
                    CurrentDays = 0,
                    LongestDays = 0
                };
                _context.Streaks.Add(streak);
            }

            streak.RegisterSuccess();
        }

        var (xpReward, goldReward, leveledUp, streakBonus) =
            _gameEngine.ApplyTaskCompletion(task, hero, streak, economy);

        await _context.SaveChangesAsync();

        return Ok(new CompleteTaskResponse
        {
            Success = true,
            TaskId = task.Id,
            TaskTitle = task.Title,

            XpGained = xpReward,
            GoldGained = goldReward,

            HeroId = hero.Id,
            NewLevel = hero.Level,
            LeveledUp = leveledUp,
            NewXp = hero.CurrentXp,
            XpForNextLevel = hero.GetXpRequiredForNextLevel(),
            XpProgress = (double)hero.CurrentXp / hero.GetXpRequiredForNextLevel(),
            NewGold = hero.Gold,
            NewHp = hero.CurrentHp,
            MaxHp = hero.MaxHp,

            StreakBonus = streakBonus,
            CurrentStreak = streak?.CurrentDays ?? 0,
            StreakMultiplier = streak?.GetStreakMultiplier() ?? 1.0,

            DailyCompletions = economy.DailyTaskCompletions,
            MaxDailyCompletions = economy.MaxDailyCompletions,

            Message = leveledUp
                ? $"LEVEL UP! You're now level {hero.Level}! +{xpReward} XP, +{goldReward} Gold"
                : $"Task completed! +{xpReward} XP, +{goldReward} Gold"
        });
    }

    [HttpPut("{id}/fail")]
    public async Task<ActionResult<FailTaskResponse>> FailTask(int id)
    {
        var task = await _context.GameTasks
            .Include(t => t.Streak)
            .FirstOrDefaultAsync(t => t.Id == id);

        if (task == null)
            return NotFound("Task not found");

        var hero = await _context.Heroes
            .Include(h => h.EconomyBalance)
            .FirstOrDefaultAsync(h => h.Id == task.HeroId);

        if (hero == null)
            return BadRequest("Hero not found");

        if (hero.IsDead)
            return BadRequest(new
            {
                error = "Hero is already dead",
                message = "Use /api/Hero/{id}/respawn to continue playing"
            });

        var economy = hero.EconomyBalance ?? new EconomyBalance { HeroId = hero.Id };
        var streak = task.Streak;

        var (hpLost, goldLost, heroDied, streakBroken, streakPenalty) =
            _gameEngine.ApplyTaskFailure(task, hero, streak, economy);

        await _context.SaveChangesAsync();

        var response = new FailTaskResponse
        {
            Success = true,
            TaskId = task.Id,
            TaskTitle = task.Title,

            DamageDealt = hpLost,
            GoldLost = goldLost,

            HeroId = hero.Id,
            NewHp = hero.CurrentHp,
            MaxHp = hero.MaxHp,
            NewGold = hero.Gold,
            CurrentLevel = hero.Level,
            CurrentXp = hero.CurrentXp,

            HeroDied = heroDied,
            DeathCount = hero.DeathCount,

            StreakBroken = streakBroken,
            StreakPenalty = streakPenalty != null
                ? new StreakPenaltyDto
                {
                    StreakDays = streakPenalty.StreakDays,
                    XpLost = streakPenalty.XpLost,
                    GoldLost = streakPenalty.GoldLost,
                    CooldownHours = streakPenalty.CooldownHours
                }
                : null,

            Message = GetFailureMessage(heroDied, hpLost, goldLost, streakBroken, streakPenalty)
        };

        return Ok(response);
    }

    [HttpPost("check-overdue")]
    public async Task<ActionResult<OverdueCheckResponse>> CheckOverdueTasks([FromQuery] int? heroId = null)
    {
        var query = _context.GameTasks
            .Include(t => t.Streak)
            .Include(t => t.Hero)
            .ThenInclude(h => h!.EconomyBalance)
            .Where(t => t.IsActive && !t.IsCompleted);

        if (heroId.HasValue)
            query = query.Where(t => t.HeroId == heroId.Value);

        var tasks = await query.ToListAsync();
        var overdueTasks = tasks.Where(t => t.IsOverdue()).ToList();

        if (!overdueTasks.Any())
            return Ok(new OverdueCheckResponse
            {
                OverdueCount = 0,
                Message = "No overdue tasks found"
            });

        var penalties = new List<OverdueTaskPenalty>();

        foreach (var task in overdueTasks)
        {
            var hero = task.Hero!;
            var economy = hero.EconomyBalance ?? new EconomyBalance { HeroId = hero.Id };
            var streak = task.Streak;

            var (hpLost, goldLost, heroDied, streakBroken, streakPenalty) =
                _gameEngine.ApplyTaskFailure(task, hero, streak, economy);

            penalties.Add(new OverdueTaskPenalty
            {
                TaskId = task.Id,
                TaskTitle = task.Title,
                DueDate = task.DueDate!.Value,
                HpLost = hpLost,
                GoldLost = goldLost,
                HeroDied = heroDied,
                StreakBroken = streakBroken
            });
        }

        await _context.SaveChangesAsync();

        return Ok(new OverdueCheckResponse
        {
            OverdueCount = overdueTasks.Count,
            Penalties = penalties,
            Message = $"Applied penalties for {overdueTasks.Count} overdue task(s)"
        });
    }

    [HttpDelete("{id}")]
    public async Task<IActionResult> DeleteTask(int id)
    {
        var task = await _context.GameTasks.FindAsync(id);
        if (task == null)
            return NotFound();

        task.IsActive = false;
        task.UpdatedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();

        return NoContent();
    }

    private async Task<TaskDto> GetTaskDto(int taskId)
    {
        var task = await _context.GameTasks
            .Include(t => t.Streak)
            .FirstOrDefaultAsync(t => t.Id == taskId);

        if (task == null)
            throw new Exception("Task not found");

        return new TaskDto
        {
            Id = task.Id,
            HeroId = task.HeroId,
            Title = task.Title,
            Description = task.Description,
            Type = task.Type,
            Difficulty = task.Difficulty,
            IsCompleted = task.IsCompleted,
            IsActive = task.IsActive,
            DueDate = task.DueDate,
            IsOverdue = task.IsOverdue(),
            CompletionCount = task.CompletionCount,
            FailCount = task.FailCount,
            LastCompletedAt = task.LastCompletedAt,
            BaseXp = task.GetBaseRewardXP(),
            BaseGold = task.GetGoldReward(),
            HpPenalty = task.GetHpPenalty(),
            GoldPenalty = task.GetGoldPenalty(),
            StreakInfo = task.Streak != null
                ? new StreakInfoDto
                {
                    CurrentDays = task.Streak.CurrentDays,
                    BonusXpPercent = task.Streak.GetBonusXpPercent(),
                    Multiplier = task.Streak.GetStreakMultiplier(),
                    IsFrozen = task.Streak.IsFrozen(),
                    IsShieldActive = task.Streak.IsShieldActive
                }
                : null
        };
    }

    private string GetFailureMessage(bool died, int hp, int gold, bool streakBroken, StreakBreakPenalty? penalty)
    {
        var messages = new List<string>();

        if (died)
        {
            messages.Add($"💀 DEATH! You took {hp} damage and lost {gold} gold.");
            messages.Add("All streaks reduced by 50%!");
            messages.Add("Use /respawn to continue playing.");
        }
        else
        {
            messages.Add($"Task failed! -{hp} HP, -{gold} Gold");
        }

        if (streakBroken)
        {
            messages.Add($"⚠️ Streak broken!");
            if (penalty != null && penalty.XpLost > 0)
            {
                messages.Add($"Penalty: -{penalty.XpLost} XP, -{penalty.GoldLost} Gold");
                if (penalty.CooldownHours > 0)
                    messages.Add($"Cooldown: {penalty.CooldownHours} hours");
            }
        }

        return string.Join(" ", messages);
    }
}

public class TaskDto
{
    public int Id { get; set; }
    public int HeroId { get; set; }
    public string Title { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public TaskType Type { get; set; }
    public TaskDifficulty Difficulty { get; set; }
    public bool IsCompleted { get; set; }
    public bool IsActive { get; set; }
    public DateTime? DueDate { get; set; }
    public bool IsOverdue { get; set; }
    public int CompletionCount { get; set; }
    public int FailCount { get; set; }
    public DateTime? LastCompletedAt { get; set; }
    public int BaseXp { get; set; }
    public int BaseGold { get; set; }
    public int HpPenalty { get; set; }
    public int GoldPenalty { get; set; }
    public StreakInfoDto? StreakInfo { get; set; }
}

public class StreakInfoDto
{
    public int CurrentDays { get; set; }
    public int BonusXpPercent { get; set; }
    public double Multiplier { get; set; }
    public bool IsFrozen { get; set; }
    public bool IsShieldActive { get; set; }
}

public class CreateTaskRequest
{
    public int? HeroId { get; set; }
    public string Title { get; set; } = string.Empty;
    public string? Description { get; set; }
    public TaskType Type { get; set; } = TaskType.OneTime;
    public TaskDifficulty Difficulty { get; set; } = TaskDifficulty.Easy;
    public DateTime? DueDate { get; set; }
    public string? RepeatPattern { get; set; }
}

public class CompleteTaskResponse
{
    public bool Success { get; set; }
    public int TaskId { get; set; }
    public string TaskTitle { get; set; } = string.Empty;
    public long XpGained { get; set; }
    public int GoldGained { get; set; }
    public int HeroId { get; set; }
    public int NewLevel { get; set; }
    public bool LeveledUp { get; set; }
    public long NewXp { get; set; }
    public long XpForNextLevel { get; set; }
    public double XpProgress { get; set; }
    public int NewGold { get; set; }
    public int NewHp { get; set; }
    public int MaxHp { get; set; }
    public int StreakBonus { get; set; }
    public int CurrentStreak { get; set; }
    public double StreakMultiplier { get; set; }
    public int DailyCompletions { get; set; }
    public int MaxDailyCompletions { get; set; }
    public string Message { get; set; } = string.Empty;
}

public class FailTaskResponse
{
    public bool Success { get; set; }
    public int TaskId { get; set; }
    public string TaskTitle { get; set; } = string.Empty;
    public int DamageDealt { get; set; }
    public int GoldLost { get; set; }
    public int HeroId { get; set; }
    public int NewHp { get; set; }
    public int MaxHp { get; set; }
    public int NewGold { get; set; }
    public int CurrentLevel { get; set; }
    public long CurrentXp { get; set; }
    public bool HeroDied { get; set; }
    public int DeathCount { get; set; }
    public bool StreakBroken { get; set; }
    public StreakPenaltyDto? StreakPenalty { get; set; }
    public string Message { get; set; } = string.Empty;
}

public class StreakPenaltyDto
{
    public int StreakDays { get; set; }
    public int XpLost { get; set; }
    public int GoldLost { get; set; }
    public int CooldownHours { get; set; }
}

public class OverdueCheckResponse
{
    public int OverdueCount { get; set; }
    public List<OverdueTaskPenalty>? Penalties { get; set; }
    public string Message { get; set; } = string.Empty;
}

public class OverdueTaskPenalty
{
    public int TaskId { get; set; }
    public string TaskTitle { get; set; } = string.Empty;
    public DateTime DueDate { get; set; }
    public int HpLost { get; set; }
    public int GoldLost { get; set; }
    public bool HeroDied { get; set; }
    public bool StreakBroken { get; set; }
}