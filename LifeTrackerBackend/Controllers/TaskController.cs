using LifeTracker.Constants;
using LifeTracker.Data;
using LifeTracker.Models;
using LifeTracker.Services;
using LifeTracker.Services.Achievements;
using LifeTracker.Services.Time;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace LifeTracker.Controllers;

[ApiController]
[Route("api/[controller]")]
public class TaskController : DeviceScopedControllerBase
{
    private readonly ApplicationDbContext _context;
    private readonly GameEngineService _gameEngine;
    private readonly AchievementService _achievementService;
    private readonly IHeroTimeService _heroTimeService;

    public TaskController(
        ApplicationDbContext context,
        GameEngineService gameEngine,
        AchievementService achievementService,
        IHeroTimeService heroTimeService,
        ICurrentHeroService currentHeroService)
        : base(currentHeroService)
    {
        _context = context;
        _gameEngine = gameEngine;
        _achievementService = achievementService;
        _heroTimeService = heroTimeService;
    }

    internal static TaskController CreateForTests(ApplicationDbContext context, GameEngineService gameEngine, IHeroTimeService heroTimeService) =>
        new(context, gameEngine, new AchievementService(context), heroTimeService, new CurrentHeroService(context));

    [HttpGet]
    public async Task<ActionResult<IEnumerable<TaskDto>>> GetTasks([FromQuery] int? heroId = null)
    {
        _ = RequireCurrentDevice(out var errorResult);
        if (errorResult is not null)
            return errorResult;

        var currentHero = await CurrentHeroService.GetCurrentHeroAsync(HttpContext);
        if (currentHero == null)
            return Ok(new List<TaskDto>());

        var effectiveHeroId = heroId ?? currentHero.Id;
        if (effectiveHeroId != currentHero.Id)
            return NotFound();

        var tasks = await _context.GameTasks
            .Include(t => t.Streak)
            .Include(t => t.Hero)
            .Where(t => t.IsActive && t.HeroId == effectiveHeroId)
            .ToListAsync();

        return Ok(tasks.Select(MapToDto).ToList());
    }

    [HttpGet("{id}")]
    public async Task<ActionResult<TaskDto>> GetTask(int id)
    {
        _ = RequireCurrentDevice(out var errorResult);
        if (errorResult is not null)
            return errorResult;

        var task = await LoadOwnedTaskAsync(id);
        if (task == null)
            return NotFound();

        return Ok(MapToDto(task));
    }

    [HttpPost]
    public async Task<ActionResult<TaskDto>> PostTask([FromBody] CreateTaskRequest request)
    {
        _ = RequireCurrentDevice(out var errorResult);
        if (errorResult is not null)
            return errorResult;

        if (string.IsNullOrWhiteSpace(request.Title))
            return BadRequest("Title is required");

        if (request.Type != TaskType.Habit && request.Polarity.HasValue)
            return BadRequest("Polarity is only allowed for Habit tasks");

        var polarity = request.Type == TaskType.Habit
            ? request.Polarity ?? HabitPolarity.Both
            : HabitPolarity.Both;

        var hero = request.HeroId.HasValue && request.HeroId.Value > 0
            ? await CurrentHeroService.GetOwnedHeroAsync(HttpContext, request.HeroId.Value)
            : await CurrentHeroService.GetCurrentHeroAsync(HttpContext);

        if (hero == null)
            return BadRequest("Hero not found");

        var task = new GameTask
        {
            HeroId = hero.Id,
            Title = request.Title,
            Description = request.Description ?? string.Empty,
            Type = request.Type,
            Difficulty = request.Difficulty,
            Polarity = polarity,
            DueDate = request.DueDate,
            RepeatPattern = request.RepeatPattern,
            ChecklistJson = request.ChecklistJson,
            RemindersJson = request.RemindersJson,
            IsCompleted = false,
            IsActive = true,
            CompletionCount = 0,
            FailCount = 0,
            CreatedAt = DateTimeOffset.UtcNow,
            UpdatedAt = DateTimeOffset.UtcNow,
        };

        _context.GameTasks.Add(task);
        await _context.SaveChangesAsync();

        if (task.Type == TaskType.Habit || task.Type == TaskType.Daily)
        {
            var streak = new Streak
            {
                HeroId = task.HeroId,
                TaskId = task.Id,
                CurrentDays = request.InitialStreak,
                LongestDays = request.InitialStreak,
                CreatedAt = DateTimeOffset.UtcNow,
                UpdatedAt = DateTimeOffset.UtcNow,
            };
            _context.Streaks.Add(streak);
            await _context.SaveChangesAsync();
        }

        var createdTask = await LoadOwnedTaskAsync(task.Id) ?? task;
        return CreatedAtAction(nameof(GetTask), new { id = task.Id }, MapToDto(createdTask));
    }

    [HttpPut("{id}/complete")]
    public async Task<ActionResult<CompleteTaskResponse>> CompleteTask(int id)
    {
        _ = RequireCurrentDevice(out var errorResult);
        if (errorResult is not null)
            return errorResult;

        var task = await LoadOwnedTaskAsync(id);
        if (task == null)
            return NotFound("Task not found");

        if (task.IsCompleted && task.Type == TaskType.OneTime)
            return BadRequest("Task is already completed");

        if (task.Type == TaskType.Habit && task.Polarity == HabitPolarity.Negative)
            return BadRequest("Negative habits cannot be completed");

        var hero = await LoadOwnedHeroForTaskAsync(task.HeroId);
        if (hero == null)
            return NotFound("Hero not found");

        if (hero.IsDead)
            return BadRequest(new
            {
                errorCode = "HERO_DEAD",
                error = "Hero is dead",
                message = "Use /api/Hero/{id}/respawn to continue playing",
            });

        var economy = hero.EconomyBalance;
        if (economy == null)
        {
            economy = new EconomyBalance { HeroId = hero.Id };
            _context.EconomyBalances.Add(economy);
            hero.EconomyBalance = economy;
        }

        var utcNow = DateTimeOffset.UtcNow;
        var effectiveTimeZone = _heroTimeService.ResolveEffectiveTimeZone(hero, utcNow);
        var todayLocalDate = _heroTimeService.GetLocalDate(utcNow, effectiveTimeZone);

        economy.CheckDailyReset(todayLocalDate);
        if (!economy.CanCompleteTask(todayLocalDate))
            return BadRequest(new
            {
                errorCode = "DAILY_LIMIT_REACHED",
                error = "Daily limit reached",
                message = $"You have completed {economy.DailyTaskCompletions}/{economy.MaxDailyCompletions} tasks today. Try again tomorrow!",
                dailyCompletions = economy.DailyTaskCompletions,
                maxDailyCompletions = economy.MaxDailyCompletions,
                resetTime = _heroTimeService.GetNextLocalMidnightUtc(utcNow, effectiveTimeZone),
            });

        Streak? streak = null;
        if (task.Type == TaskType.Habit || task.Type == TaskType.Daily)
        {
            streak = task.Streak ?? await _context.Streaks.FirstOrDefaultAsync(s => s.HeroId == hero.Id && s.TaskId == task.Id);

            if (streak == null)
            {
                streak = new Streak
                {
                    HeroId = hero.Id,
                    TaskId = task.Id,
                    CurrentDays = 0,
                    LongestDays = 0,
                };
                _context.Streaks.Add(streak);
            }

            streak.RegisterSuccess(todayLocalDate, utcNow);
        }

        await using var transaction = await _context.Database.BeginTransactionAsync();

        var (xpReward, goldReward, leveledUp, streakBonus) =
            _gameEngine.ApplyTaskCompletion(task, hero, streak, economy, todayLocalDate);

        if (!await SaveChangesWithSingleRetryAsync())
            return Conflict(new { errorCode = "CONCURRENCY_CONFLICT", message = "Task state changed concurrently. Please retry." });

        var unlockedAchievements = await _achievementService.EvaluateAndStageNewUnlocksAsync(hero.Id);

        if (!await SaveChangesWithSingleRetryAsync())
            return Conflict(new { errorCode = "CONCURRENCY_CONFLICT", message = "Task state changed concurrently. Please retry." });

        await transaction.CommitAsync();

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
            DeathCount = hero.DeathCount,
            XpBoostPercent = hero.XpBoostPercent,
            XpBoostTasksRemaining = hero.XpBoostTasksRemaining,
            UnlockedAchievements = unlockedAchievements.ToList(),
            StreakBonus = streakBonus,
            CurrentStreak = streak?.CurrentDays ?? 0,
            StreakMultiplier = streak?.GetStreakMultiplier() ?? 1.0,
            DailyCompletions = economy.DailyTaskCompletions,
            MaxDailyCompletions = economy.MaxDailyCompletions,
            Message = leveledUp
                ? $"LEVEL UP! You're now level {hero.Level}! +{xpReward} XP, +{goldReward} Gold"
                : $"Task completed! +{xpReward} XP, +{goldReward} Gold",
        });
    }

    [HttpPut("{id}/fail")]
    public async Task<ActionResult<FailTaskResponse>> FailTask(int id)
    {
        _ = RequireCurrentDevice(out var errorResult);
        if (errorResult is not null)
            return errorResult;

        var task = await LoadOwnedTaskAsync(id);
        if (task == null)
            return NotFound("Task not found");

        if (task.Type == TaskType.Habit && task.Polarity == HabitPolarity.Positive)
            return BadRequest("Positive habits cannot be failed");

        var hero = await LoadOwnedHeroForTaskAsync(task.HeroId);
        if (hero == null)
            return NotFound("Hero not found");

        if (hero.IsDead)
            return BadRequest(new
            {
                errorCode = "HERO_ALREADY_DEAD",
                error = "Hero is already dead",
                message = "Use /api/Hero/{id}/respawn to continue playing",
            });

        var economy = hero.EconomyBalance ?? new EconomyBalance { HeroId = hero.Id };
        var streak = task.Streak;

        var utcNow = DateTimeOffset.UtcNow;
        var effectiveTimeZone = _heroTimeService.ResolveEffectiveTimeZone(hero, utcNow);
        var todayLocalDate = _heroTimeService.GetLocalDate(utcNow, effectiveTimeZone);

        var failureResult = _gameEngine.ApplyTaskFailure(task, hero, streak, economy, todayLocalDate);

        if (!await SaveChangesWithSingleRetryAsync())
            return Conflict(new { errorCode = "CONCURRENCY_CONFLICT", message = "Task state changed concurrently. Please retry." });

        var response = new FailTaskResponse
        {
            Success = true,
            TaskId = task.Id,
            TaskTitle = task.Title,
            DamageDealt = failureResult.HpLost,
            GoldLost = failureResult.GoldLost,
            HeroId = hero.Id,
            NewHp = hero.CurrentHp,
            MaxHp = hero.MaxHp,
            NewGold = hero.Gold,
            CurrentLevel = hero.Level,
            CurrentXp = hero.CurrentXp,
            HeroDied = failureResult.HeroDied,
            DeathCount = hero.DeathCount,
            XpForNextLevel = hero.GetXpRequiredForNextLevel(),
            DailyCompletions = economy.DailyTaskCompletions,
            MaxDailyCompletions = economy.MaxDailyCompletions,
            XpBoostPercent = hero.XpBoostPercent,
            XpBoostTasksRemaining = hero.XpBoostTasksRemaining,
            StreakBroken = failureResult.StreakBroken,
            ShieldAbsorbed = failureResult.ShieldAbsorbed,
            StreakPenalty = failureResult.Penalty != null
                ? new StreakPenaltyDto
                {
                    StreakDays = failureResult.Penalty.StreakDays,
                    XpLost = failureResult.Penalty.XpLost,
                    GoldLost = failureResult.Penalty.GoldLost,
                    CooldownHours = failureResult.Penalty.CooldownHours,
                }
                : null,
            Message = GetFailureMessage(failureResult.HeroDied, failureResult.HpLost, failureResult.GoldLost, failureResult.StreakBroken, failureResult.Penalty),
        };

        return Ok(response);
    }

    [HttpPost("check-overdue")]
    public async Task<ActionResult<OverdueCheckResponse>> CheckOverdueTasks([FromQuery] int? heroId = null)
    {
        _ = RequireCurrentDevice(out var errorResult);
        if (errorResult is not null)
            return errorResult;

        var currentHero = await CurrentHeroService.GetCurrentHeroAsync(HttpContext);
        if (currentHero == null)
            return Ok(new OverdueCheckResponse
            {
                OverdueCount = 0,
                Message = "No overdue tasks found",
            });

        var effectiveHeroId = heroId ?? currentHero.Id;
        if (effectiveHeroId != currentHero.Id)
            return NotFound();

        var tasks = await _context.GameTasks
            .Include(t => t.Streak)
            .Include(t => t.Hero)
            .ThenInclude(h => h!.EconomyBalance)
            .Where(t => t.IsActive && !t.IsCompleted && t.OverdueProcessedAt == null && t.HeroId == effectiveHeroId)
            .ToListAsync();

        var overdueTasks = tasks.Where(t => t.IsOverdue()).ToList();
        if (!overdueTasks.Any())
            return Ok(new OverdueCheckResponse
            {
                OverdueCount = 0,
                Message = "No overdue tasks found",
            });

        var penalties = new List<OverdueTaskPenalty>();
        var shieldContexts = new Dictionary<int, ShieldConsumptionContext>();

        foreach (var task in overdueTasks)
        {
            var hero = task.Hero!;
            var economy = hero.EconomyBalance ?? new EconomyBalance { HeroId = hero.Id };
            var streak = task.Streak;

            var utcNow = DateTimeOffset.UtcNow;
            var effectiveTimeZone = _heroTimeService.ResolveEffectiveTimeZone(hero, utcNow);
            var todayLocalDate = _heroTimeService.GetLocalDate(utcNow, effectiveTimeZone);

            if (!shieldContexts.TryGetValue(hero.Id, out var shieldContext))
            {
                shieldContext = new ShieldConsumptionContext();
                shieldContexts[hero.Id] = shieldContext;
            }

            var failureResult = _gameEngine.ApplyTaskFailure(task, hero, streak, economy, todayLocalDate, shieldContext);
            task.OverdueProcessedAt = DateTimeOffset.UtcNow;

            penalties.Add(new OverdueTaskPenalty
            {
                TaskId = task.Id,
                TaskTitle = task.Title,
                DueDate = task.DueDate!.Value,
                HpLost = failureResult.HpLost,
                GoldLost = failureResult.GoldLost,
                HeroDied = failureResult.HeroDied,
                StreakBroken = failureResult.StreakBroken,
            });
        }

        foreach (var heroWithShieldUse in overdueTasks
                     .Select(t => t.Hero!)
                     .Where(h => shieldContexts.TryGetValue(h.Id, out var context) && context.AbsorbedAnyBreak)
                     .DistinctBy(h => h.Id))
        {
            heroWithShieldUse.IsShieldActive = false;
            heroWithShieldUse.ShieldActivatedAtUtc = null;
        }

        if (!await SaveChangesWithSingleRetryAsync())
            return Conflict(new { errorCode = "CONCURRENCY_CONFLICT", message = "Overdue state changed concurrently. Please retry." });

        return Ok(new OverdueCheckResponse
        {
            OverdueCount = overdueTasks.Count,
            Penalties = penalties,
            Message = $"Applied penalties for {overdueTasks.Count} overdue task(s)",
        });
    }

    [HttpDelete("{id}")]
    public async Task<IActionResult> DeleteTask(int id)
    {
        _ = RequireCurrentDevice(out var errorResult);
        if (errorResult is not null)
            return errorResult;

        var task = await LoadOwnedTaskAsync(id);
        if (task == null)
            return NotFound();

        task.IsActive = false;
        task.UpdatedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();

        return NoContent();
    }

    private Task<GameTask?> LoadOwnedTaskAsync(int taskId)
    {
        var deviceId = HttpContext.Items[LifeTracker.Infrastructure.DeviceRequestContext.ItemKey]?.ToString();

        return _context.GameTasks
            .Include(t => t.Streak)
            .Include(t => t.Hero)
            .ThenInclude(h => h!.EconomyBalance)
            .FirstOrDefaultAsync(t => t.Id == taskId && t.Hero != null && t.Hero.OwnerDeviceId == deviceId);
    }

    private Task<Hero?> LoadOwnedHeroForTaskAsync(int heroId) =>
        CurrentHeroService.GetOwnedHeroAsync(HttpContext, heroId, query => query.Include(h => h.EconomyBalance));

    private static TaskDto MapToDto(GameTask task) => new()
    {
        Id = task.Id,
        HeroId = task.HeroId,
        Title = task.Title,
        Description = task.Description,
        Type = task.Type,
        Difficulty = task.Difficulty,
        Polarity = task.Polarity,
        IsCompleted = task.IsCompleted,
        IsActive = task.IsActive,
        DueDate = task.DueDate,
        RepeatPattern = task.RepeatPattern,
        ChecklistJson = task.ChecklistJson,
        RemindersJson = task.RemindersJson,
        IsOverdue = task.IsOverdue(),
        CompletionCount = task.CompletionCount,
        FailCount = task.FailCount,
        LastCompletedAt = task.LastCompletedAt,
        OverdueProcessedAt = task.OverdueProcessedAt,
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
                IsShieldActive = task.Hero?.IsShieldActive ?? false,
                ShieldExpiresAtUtc = null,
            }
            : null,
    };

    private async Task<bool> SaveChangesWithSingleRetryAsync()
    {
        try
        {
            await _context.SaveChangesAsync();
            return true;
        }
        catch (DbUpdateConcurrencyException ex)
        {
            if (ex.Entries.Any(entry => entry.Entity is Hero))
                return false;

            foreach (var entry in ex.Entries)
            {
                var dbValues = await entry.GetDatabaseValuesAsync();
                if (dbValues == null)
                    return false;

                entry.OriginalValues.SetValues(dbValues);
            }

            try
            {
                await _context.SaveChangesAsync();
                return true;
            }
            catch (DbUpdateConcurrencyException)
            {
                return false;
            }
        }
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
            messages.Add("⚠️ Streak broken!");
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
    public HabitPolarity Polarity { get; set; }
    public bool IsCompleted { get; set; }
    public bool IsActive { get; set; }
    public DateTimeOffset? DueDate { get; set; }
    public string? RepeatPattern { get; set; }
    public string? ChecklistJson { get; set; }
    public string? RemindersJson { get; set; }
    public bool IsOverdue { get; set; }
    public int CompletionCount { get; set; }
    public int FailCount { get; set; }
    public DateTimeOffset? LastCompletedAt { get; set; }
    public DateTimeOffset? OverdueProcessedAt { get; set; }
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
    public DateTimeOffset? ShieldExpiresAtUtc { get; set; }
}

public class CreateTaskRequest
{
    public int? HeroId { get; set; }
    public string Title { get; set; } = string.Empty;
    public string? Description { get; set; }
    public TaskType Type { get; set; } = TaskType.OneTime;
    public TaskDifficulty Difficulty { get; set; } = TaskDifficulty.Easy;
    public HabitPolarity? Polarity { get; set; }
    public DateTimeOffset? DueDate { get; set; }
    public string? RepeatPattern { get; set; }
    public int InitialStreak { get; set; } = 0;
    public string? ChecklistJson { get; set; }
    public string? RemindersJson { get; set; }
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
    public int DeathCount { get; set; }
    public int CurrentStreak { get; set; }
    public double StreakMultiplier { get; set; }
    public int DailyCompletions { get; set; }
    public int MaxDailyCompletions { get; set; }
    public int XpBoostPercent { get; set; }
    public int XpBoostTasksRemaining { get; set; }
    public List<AchievementUnlock> UnlockedAchievements { get; set; } = new();
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
    public long XpForNextLevel { get; set; }
    public int DailyCompletions { get; set; }
    public int MaxDailyCompletions { get; set; }
    public int CurrentLevel { get; set; }
    public long CurrentXp { get; set; }
    public bool HeroDied { get; set; }
    public int DeathCount { get; set; }
    public bool StreakBroken { get; set; }
    public bool ShieldAbsorbed { get; set; }
    public StreakPenaltyDto? StreakPenalty { get; set; }
    public int XpBoostPercent { get; set; }
    public int XpBoostTasksRemaining { get; set; }
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
    public DateTimeOffset DueDate { get; set; }
    public int HpLost { get; set; }
    public int GoldLost { get; set; }
    public bool HeroDied { get; set; }
    public bool StreakBroken { get; set; }
}
