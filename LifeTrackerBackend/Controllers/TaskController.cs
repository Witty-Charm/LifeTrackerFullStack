using System.Globalization;
using LifeTracker.Constants;
using LifeTracker.Data;
using LifeTracker.Models;
using LifeTracker.Services;
using LifeTracker.Services.Achievements;
using LifeTracker.Services.Time;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;

namespace LifeTracker.Controllers;

[ApiController]
[Route("api/[controller]")]
public class TaskController : DeviceScopedControllerBase
{
    private readonly ApplicationDbContext _context;
    private readonly GameEngineService _gameEngine;
    private readonly AchievementService _achievementService;
    private readonly IHeroTimeService _heroTimeService;
    private readonly IDailyScheduleService _dailySchedule;
    private readonly ILogger<TaskController> _logger;

    public TaskController(
        ApplicationDbContext context,
        GameEngineService gameEngine,
        AchievementService achievementService,
        IHeroTimeService heroTimeService,
        IDailyScheduleService dailyScheduleService,
        ILogger<TaskController> logger,
        ICurrentHeroService currentHeroService)
        : base(currentHeroService)
    {
        _context = context;
        _gameEngine = gameEngine;
        _achievementService = achievementService;
        _heroTimeService = heroTimeService;
        _dailySchedule = dailyScheduleService;
        _logger = logger;
    }

    internal static TaskController CreateForTests(ApplicationDbContext context, GameEngineService gameEngine, IHeroTimeService heroTimeService) =>
        new(
            context,
            gameEngine,
            new AchievementService(context),
            heroTimeService,
            new DailyScheduleService(heroTimeService),
            NullLogger<TaskController>.Instance,
            new CurrentHeroService(context));

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
            .Include(t => t.DailyTaskCompletions)
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

        if (task.Type == TaskType.Daily)
        {
            var tz = _heroTimeService.ResolveEffectiveTimeZone(hero, DateTimeOffset.UtcNow);
            var startLocal = _dailySchedule.GetStartLocalDate(task, tz);
            task.LastMissedScheduledLocalDate = startLocal
                .AddDays(-1)
                .ToString("yyyy-MM-dd", CultureInfo.InvariantCulture);
        }

        _context.GameTasks.Add(task);
        await _context.SaveChangesAsync();

        if (task.Type == TaskType.Habit || task.Type == TaskType.Daily)
        {
            var streakCreatedAt = DateTimeOffset.UtcNow;
            var streak = new Streak
            {
                HeroId = task.HeroId,
                TaskId = task.Id,
                CurrentDays = request.InitialStreak,
                LongestDays = request.InitialStreak,
                CreatedAt = streakCreatedAt,
                UpdatedAt = streakCreatedAt,
            };

            if (request.InitialStreak > 0)
            {
                streak.StartDate = streakCreatedAt;
                streak.LastCheckInLocalDate = FormatSeedAnchorLocalDate(hero, streakCreatedAt);
            }

            _context.Streaks.Add(streak);
            await _context.SaveChangesAsync();
        }

        var createdTask = await LoadOwnedTaskAsync(task.Id) ?? task;
        return CreatedAtAction(nameof(GetTask), new { id = task.Id }, MapToDto(createdTask));
    }

    [HttpPut("{id}")]
    public async Task<ActionResult<TaskDto>> UpdateTask(int id, [FromBody] UpdateTaskRequest request)
    {
        _ = RequireCurrentDevice(out var errorResult);
        if (errorResult is not null)
            return errorResult;

        if (string.IsNullOrWhiteSpace(request.Title))
            return BadRequest("Title is required");

        var task = await LoadOwnedTaskAsync(id);
        if (task == null)
            return NotFound();

        task.Title = request.Title.Trim();
        task.Description = request.Description ?? string.Empty;
        task.Difficulty = request.Difficulty;

        if (task.Type == TaskType.Habit)
        {
            task.Polarity = request.Polarity ?? task.Polarity;
        }
        else
        {
            task.Polarity = HabitPolarity.Both;
        }

        var previousDueDate = task.DueDate;
        var previousRepeatPattern = task.RepeatPattern;
        task.DueDate = request.DueDate;

        if (task.Type == TaskType.Daily)
        {
            task.RepeatPattern = string.IsNullOrWhiteSpace(request.RepeatPattern)
                ? task.RepeatPattern
                : request.RepeatPattern;
            task.ChecklistJson = request.ChecklistJson;
            task.RemindersJson = request.RemindersJson;

            var scheduleChanged = previousDueDate != task.DueDate
                || !string.Equals(previousRepeatPattern, task.RepeatPattern, StringComparison.Ordinal);
            if (scheduleChanged)
            {
                var owningHero = task.Hero ?? await _context.Heroes.FindAsync(task.HeroId);
                if (owningHero != null)
                {
                    var tz = _heroTimeService.ResolveEffectiveTimeZone(owningHero, DateTimeOffset.UtcNow);
                    var newStartLocal = _dailySchedule.GetStartLocalDate(task, tz);
                    task.LastMissedScheduledLocalDate = newStartLocal
                        .AddDays(-1)
                        .ToString("yyyy-MM-dd", CultureInfo.InvariantCulture);
                }
            }
        }
        else if (task.Type == TaskType.Habit)
        {
            task.RepeatPattern = string.IsNullOrWhiteSpace(request.RepeatPattern)
                ? task.RepeatPattern
                : request.RepeatPattern;
            task.ChecklistJson = null;
            task.RemindersJson = null;
        }
        else
        {
            task.RepeatPattern = null;
            task.ChecklistJson = null;
            task.RemindersJson = null;
        }

        task.UpdatedAt = DateTimeOffset.UtcNow;
        await _context.SaveChangesAsync();

        var updated = await LoadOwnedTaskAsync(task.Id) ?? task;
        return Ok(MapToDto(updated));
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

        if (task.Type == TaskType.Daily)
            return BadRequest("Daily tasks use /api/Task/{id}/daily-state");

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

        Streak? streak = null;
        string? legacySeedAnchorLocalDate = null;
        if (task.Type == TaskType.Habit || task.Type == TaskType.Daily)
        {
            streak = task.Streak ?? await _context.Streaks.FirstOrDefaultAsync(s => s.HeroId == hero.Id && s.TaskId == task.Id);
            if (streak != null && streak.CurrentDays > 0 && string.IsNullOrWhiteSpace(streak.LastCheckInLocalDate))
            {
                legacySeedAnchorLocalDate = FormatSeedAnchorLocalDate(hero, streak.CreatedAt);
            }
        }

        var effectiveTimeZone = _heroTimeService.ResolveEffectiveTimeZone(hero, utcNow);
        var todayLocalDate = _heroTimeService.GetLocalDate(utcNow, effectiveTimeZone);

        ResetHabitCounterIfNeeded(task, todayLocalDate, effectiveTimeZone, _heroTimeService.GetLocalDate);

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

        if (task.Type == TaskType.Habit || task.Type == TaskType.Daily)
        {
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

            if (legacySeedAnchorLocalDate != null)
            {
                streak.LastCheckInLocalDate = legacySeedAnchorLocalDate;
                streak.StartDate ??= streak.CreatedAt;
                _logger.LogInformation(
                    "streak.seeded_legacy_backfill taskId={TaskId} heroId={HeroId} currentDays={CurrentDays} anchorLocalDate={AnchorLocalDate}",
                    task.Id,
                    hero.Id,
                    streak.CurrentDays,
                    streak.LastCheckInLocalDate);
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

    [HttpPut("{id}/daily-state")]
    public async Task<ActionResult<SetDailyTaskStateResponse>> SetDailyTaskState(int id, [FromBody] SetDailyTaskStateRequest request)
    {
        _ = RequireCurrentDevice(out var errorResult);
        if (errorResult is not null)
            return errorResult;

        if (string.IsNullOrWhiteSpace(request.LocalDate))
            return BadRequest("localDate is required");

        var task = await LoadOwnedTaskAsync(id);
        if (task == null)
            return NotFound("Task not found");

        if (task.Type != TaskType.Daily)
            return BadRequest("daily-state is only available for Daily tasks");

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

        var utcNow = DateTimeOffset.UtcNow;
        var effectiveTimeZone = _heroTimeService.ResolveEffectiveTimeZone(hero, utcNow);
        var todayLocalDate = _heroTimeService.GetLocalDate(utcNow, effectiveTimeZone);
        var requestedLocalDate = _heroTimeService.ParseLocalDate(request.LocalDate);
        if (requestedLocalDate != todayLocalDate)
            return BadRequest("Only today's daily state can be changed");

        if (request.IsChecked && !_dailySchedule.IsScheduledOn(task, todayLocalDate, effectiveTimeZone))
        {
            var nextScheduled = _dailySchedule.NextScheduledOnOrAfter(
                task,
                todayLocalDate.AddDays(1),
                effectiveTimeZone);
            return BadRequest(new
            {
                errorCode = "DAILY_NOT_SCHEDULED_TODAY",
                error = "Daily is not scheduled today",
                message = "This daily repeats on a longer interval; today is not a scheduled day.",
                nextScheduledLocalDate = nextScheduled.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture),
            });
        }

        var economy = hero.EconomyBalance;
        if (economy == null)
        {
            economy = new EconomyBalance { HeroId = hero.Id };
            _context.EconomyBalances.Add(economy);
            hero.EconomyBalance = economy;
        }

        economy.CheckDailyReset(todayLocalDate);

        var streak = task.Streak ?? await _context.Streaks.FirstOrDefaultAsync(s => s.HeroId == hero.Id && s.TaskId == task.Id);
        string? legacySeedAnchorLocalDate = null;
        if (streak != null && streak.CurrentDays > 0 && string.IsNullOrWhiteSpace(streak.LastCheckInLocalDate))
        {
            legacySeedAnchorLocalDate = FormatSeedAnchorLocalDate(hero, streak.CreatedAt);
        }

        var completion = await _context.DailyTaskCompletions.FirstOrDefaultAsync(c =>
            c.HeroId == hero.Id &&
            c.TaskId == task.Id &&
            c.LocalDate == request.LocalDate);

        if (request.IsChecked)
        {
            if (completion?.IsChecked == true)
            {
                return Ok(BuildDailyStateResponse(task, hero, economy, streak, true, 0, 0));
            }

            if (!economy.CanCompleteTask(todayLocalDate))
            {
                return BadRequest(new
                {
                    errorCode = "DAILY_LIMIT_REACHED",
                    error = "Daily limit reached",
                    message = $"You have completed {economy.DailyTaskCompletions}/{economy.MaxDailyCompletions} tasks today. Try again tomorrow!",
                    dailyCompletions = economy.DailyTaskCompletions,
                    maxDailyCompletions = economy.MaxDailyCompletions,
                    resetTime = _heroTimeService.GetNextLocalMidnightUtc(utcNow, effectiveTimeZone),
                });
            }

            await using var transaction = await _context.Database.BeginTransactionAsync();

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

            if (legacySeedAnchorLocalDate != null)
            {
                streak.LastCheckInLocalDate = legacySeedAnchorLocalDate;
                streak.StartDate ??= streak.CreatedAt;
                _logger.LogInformation(
                    "streak.seeded_legacy_backfill taskId={TaskId} heroId={HeroId} currentDays={CurrentDays} anchorLocalDate={AnchorLocalDate}",
                    task.Id,
                    hero.Id,
                    streak.CurrentDays,
                    streak.LastCheckInLocalDate);
            }

            var rewardXp = _gameEngine.CalculateFinalXpReward(task, hero, streak, economy);
            var rewardGold = _gameEngine.CalculateFinalGoldReward(task, hero, economy);
            var consumedXpBoostCharge = hero.XpBoostTasksRemaining > 0;
            var consumedLastXpBoostCharge = hero.XpBoostTasksRemaining == 1 && hero.XpBoostPercent > 0;
            var previousXpBoostPercent = hero.XpBoostPercent;
            var previousTaskCompletionCount = task.CompletionCount;
            var previousTaskLastCompletedAt = task.LastCompletedAt;
            var streakExistedBefore = completion?.StreakExistedBefore ?? (task.Streak != null || streak.CurrentDays > 0 || !string.IsNullOrWhiteSpace(streak.LastCheckInLocalDate));
            var previousStreakCurrentDays = streak.CurrentDays;
            var previousStreakLongestDays = streak.LongestDays;
            var previousStreakStartDate = streak.StartDate;
            var previousStreakLastCheckIn = streak.LastCheckIn;
            var previousStreakLastCheckInLocalDate = streak.LastCheckInLocalDate;

            streak.RegisterSuccess(todayLocalDate, utcNow);
            var (_, _, leveledUp, streakBonus) = _gameEngine.ApplyTaskCompletion(task, hero, streak, economy, todayLocalDate);

            completion ??= new DailyTaskCompletion
            {
                HeroId = hero.Id,
                TaskId = task.Id,
                LocalDate = request.LocalDate,
                CreatedAt = utcNow,
            };
            completion.IsChecked = true;
            completion.RewardXp = rewardXp;
            completion.RewardGold = rewardGold;
            completion.ConsumedXpBoostCharge = consumedXpBoostCharge;
            completion.ConsumedLastXpBoostCharge = consumedLastXpBoostCharge;
            completion.PreviousXpBoostPercent = previousXpBoostPercent;
            completion.PreviousTaskCompletionCount = previousTaskCompletionCount;
            completion.PreviousTaskLastCompletedAt = previousTaskLastCompletedAt;
            completion.StreakExistedBefore = streakExistedBefore;
            completion.PreviousStreakCurrentDays = previousStreakCurrentDays;
            completion.PreviousStreakLongestDays = previousStreakLongestDays;
            completion.PreviousStreakStartDate = previousStreakStartDate;
            completion.PreviousStreakLastCheckIn = previousStreakLastCheckIn;
            completion.PreviousStreakLastCheckInLocalDate = previousStreakLastCheckInLocalDate;
            completion.UpdatedAt = utcNow;
            if (completion.Id == 0)
            {
                _context.DailyTaskCompletions.Add(completion);
                task.DailyTaskCompletions.Add(completion);
            }

            if (!await SaveChangesWithSingleRetryAsync())
                return Conflict(new { errorCode = "CONCURRENCY_CONFLICT", message = "Task state changed concurrently. Please retry." });

            var unlockedAchievements = await _achievementService.EvaluateAndStageNewUnlocksAsync(hero.Id);
            if (!await SaveChangesWithSingleRetryAsync())
                return Conflict(new { errorCode = "CONCURRENCY_CONFLICT", message = "Task state changed concurrently. Please retry." });

            await transaction.CommitAsync();

            var response = BuildDailyStateResponse(task, hero, economy, streak, true, rewardXp, rewardGold);
            response.UnlockedAchievements = unlockedAchievements.ToList();
            response.LeveledUp = leveledUp;
            response.StreakBonus = streakBonus;
            return Ok(response);
        }

        if (completion?.IsChecked != true)
        {
            return Ok(BuildDailyStateResponse(task, hero, economy, streak, false, 0, 0));
        }

        hero.RollbackDailyCompletion(completion.RewardXp, completion.ConsumedXpBoostCharge, completion.ConsumedLastXpBoostCharge, completion.PreviousXpBoostPercent);
        hero.Gold = Math.Max(0, hero.Gold - completion.RewardGold);
        economy.TotalXpEarned = Math.Max(0, economy.TotalXpEarned - completion.RewardXp);
        economy.TotalGoldEarned = Math.Max(0, economy.TotalGoldEarned - completion.RewardGold);
        economy.DailyTaskCompletions = Math.Max(0, economy.DailyTaskCompletions - 1);
        economy.UpdatedAt = utcNow;

        task.CompletionCount = completion.PreviousTaskCompletionCount;
        task.LastCompletedAt = completion.PreviousTaskLastCompletedAt;
        task.UpdatedAt = utcNow;

        if (streak != null)
        {
            streak.CurrentDays = completion.PreviousStreakCurrentDays ?? 0;
            streak.LongestDays = completion.PreviousStreakLongestDays ?? streak.LongestDays;
            streak.StartDate = completion.PreviousStreakStartDate;
            streak.LastCheckIn = completion.PreviousStreakLastCheckIn;
            streak.LastCheckInLocalDate = completion.PreviousStreakLastCheckInLocalDate;
            streak.UpdatedAt = utcNow;
        }

        _context.DailyTaskCompletions.Remove(completion);
        task.DailyTaskCompletions.Remove(completion);

        if (!await SaveChangesWithSingleRetryAsync())
            return Conflict(new { errorCode = "CONCURRENCY_CONFLICT", message = "Task state changed concurrently. Please retry." });

        return Ok(BuildDailyStateResponse(task, hero, economy, streak, false, -completion.RewardXp, -completion.RewardGold));
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

        ResetHabitCounterIfNeeded(task, todayLocalDate, effectiveTimeZone, _heroTimeService.GetLocalDate);

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

        var penalties = new List<OverdueTaskPenalty>();
        var shieldContexts = new Dictionary<int, ShieldConsumptionContext>();
        var heroesWithAnyPenalty = new HashSet<int>();

        var legacyTasks = await _context.GameTasks
            .Include(t => t.Streak)
            .Include(t => t.Hero)
            .ThenInclude(h => h!.EconomyBalance)
            .Where(t => t.IsActive && !t.IsCompleted && t.OverdueProcessedAt == null
                        && t.Type != TaskType.Daily && t.HeroId == effectiveHeroId)
            .ToListAsync();

        foreach (var task in legacyTasks.Where(t => t.IsOverdue()))
        {
            var hero = task.Hero!;
            var economy = hero.EconomyBalance ?? new EconomyBalance { HeroId = hero.Id };
            var streak = task.Streak;

            var utcNow = DateTimeOffset.UtcNow;
            var effectiveTimeZone = _heroTimeService.ResolveEffectiveTimeZone(hero, utcNow);
            var todayLocalDate = _heroTimeService.GetLocalDate(utcNow, effectiveTimeZone);

            var shieldContext = GetOrCreateShieldContext(shieldContexts, hero.Id);
            var failureResult = _gameEngine.ApplyTaskFailure(task, hero, streak, economy, todayLocalDate, shieldContext);
            task.OverdueProcessedAt = utcNow;
            heroesWithAnyPenalty.Add(hero.Id);

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

        var dailies = await _context.GameTasks
            .Include(t => t.Streak)
            .Include(t => t.Hero)
            .ThenInclude(h => h!.EconomyBalance)
            .Include(t => t.DailyTaskCompletions)
            .Where(t => t.IsActive && t.Type == TaskType.Daily && t.HeroId == effectiveHeroId)
            .ToListAsync();

        var dailyPenaltyCount = ApplyDailyMissedDayPenalties(dailies, penalties, shieldContexts, heroesWithAnyPenalty);

        foreach (var heroId2 in heroesWithAnyPenalty)
        {
            if (!shieldContexts.TryGetValue(heroId2, out var ctx) || !ctx.AbsorbedAnyBreak) continue;
            var hero = legacyTasks.FirstOrDefault(t => t.HeroId == heroId2)?.Hero
                       ?? dailies.FirstOrDefault(t => t.HeroId == heroId2)?.Hero;
            if (hero == null) continue;
            hero.IsShieldActive = false;
            hero.ShieldActivatedAtUtc = null;
        }

        var totalCount = penalties.Count;
        if (totalCount == 0)
            return Ok(new OverdueCheckResponse
            {
                OverdueCount = 0,
                Message = "No overdue tasks found",
            });

        if (!await SaveChangesWithSingleRetryAsync())
            return Conflict(new { errorCode = "CONCURRENCY_CONFLICT", message = "Overdue state changed concurrently. Please retry." });

        return Ok(new OverdueCheckResponse
        {
            OverdueCount = totalCount,
            Penalties = penalties,
            Message = $"Applied penalties for {totalCount} overdue task(s)",
        });
    }

    private static ShieldConsumptionContext GetOrCreateShieldContext(
        Dictionary<int, ShieldConsumptionContext> shieldContexts, int heroId)
    {
        if (!shieldContexts.TryGetValue(heroId, out var ctx))
        {
            ctx = new ShieldConsumptionContext();
            shieldContexts[heroId] = ctx;
        }
        return ctx;
    }

    private int ApplyDailyMissedDayPenalties(
        IReadOnlyList<GameTask> dailies,
        List<OverdueTaskPenalty> penalties,
        Dictionary<int, ShieldConsumptionContext> shieldContexts,
        HashSet<int> heroesWithAnyPenalty)
    {
        var applied = 0;
        var utcNow = DateTimeOffset.UtcNow;

        foreach (var task in dailies)
        {
            var hero = task.Hero;
            if (hero == null) continue;

            var economy = hero.EconomyBalance ?? new EconomyBalance { HeroId = hero.Id };
            var tz = _heroTimeService.ResolveEffectiveTimeZone(hero, utcNow);
            var todayLocal = _heroTimeService.GetLocalDate(utcNow, tz);
            var yesterdayLocal = todayLocal.AddDays(-1);

            DateOnly? lastProcessed = null;
            if (!string.IsNullOrWhiteSpace(task.LastMissedScheduledLocalDate))
            {
                if (DateOnly.TryParseExact(
                        task.LastMissedScheduledLocalDate,
                        "yyyy-MM-dd",
                        CultureInfo.InvariantCulture,
                        DateTimeStyles.None,
                        out var parsed))
                {
                    lastProcessed = parsed;
                }
            }

            if (lastProcessed is null)
            {
                task.LastMissedScheduledLocalDate = yesterdayLocal.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture);
                task.UpdatedAt = utcNow;
                continue;
            }

            var fromLocal = _dailySchedule.NextScheduledOnOrAfter(task, lastProcessed.Value.AddDays(1), tz);
            if (fromLocal > yesterdayLocal) continue;

            var checkedDates = new HashSet<string>(
                task.DailyTaskCompletions
                    .Where(c => c.IsChecked)
                    .Select(c => c.LocalDate),
                StringComparer.Ordinal);

            foreach (var day in _dailySchedule.EnumerateScheduledDays(task, fromLocal, yesterdayLocal, tz))
            {
                var dayStr = day.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture);
                if (checkedDates.Contains(dayStr))
                {
                    task.LastMissedScheduledLocalDate = dayStr;
                    task.UpdatedAt = utcNow;
                    continue;
                }

                var shieldContext = GetOrCreateShieldContext(shieldContexts, hero.Id);
                var failureResult = _gameEngine.ApplyTaskFailure(task, hero, task.Streak, economy, day, shieldContext);
                heroesWithAnyPenalty.Add(hero.Id);
                applied++;

                penalties.Add(new OverdueTaskPenalty
                {
                    TaskId = task.Id,
                    TaskTitle = task.Title,
                    DueDate = day.ToDateTime(TimeOnly.MinValue, DateTimeKind.Unspecified),
                    HpLost = failureResult.HpLost,
                    GoldLost = failureResult.GoldLost,
                    HeroDied = failureResult.HeroDied,
                    StreakBroken = failureResult.StreakBroken,
                });

                task.LastMissedScheduledLocalDate = dayStr;
                task.UpdatedAt = utcNow;
            }
        }

        return applied;
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
            .Include(t => t.DailyTaskCompletions)
            .FirstOrDefaultAsync(t => t.Id == taskId && t.Hero != null && t.Hero.OwnerDeviceId == deviceId);
    }

    private Task<Hero?> LoadOwnedHeroForTaskAsync(int heroId) =>
        CurrentHeroService.GetOwnedHeroAsync(HttpContext, heroId, query => query.Include(h => h.EconomyBalance));

    private string FormatSeedAnchorLocalDate(Hero hero, DateTimeOffset streakCreatedAt)
    {
        var creationTimeZone = _heroTimeService.NormalizeOrDefault(hero.TimeZoneId, "UTC");
        var createdLocalDate = _heroTimeService.GetLocalDate(streakCreatedAt, creationTimeZone);
        return createdLocalDate.AddDays(-1).ToString("yyyy-MM-dd");
    }

    private SetDailyTaskStateResponse BuildDailyStateResponse(
        GameTask task,
        Hero hero,
        EconomyBalance economy,
        Streak? streak,
        bool isChecked,
        long xpDelta,
        int goldDelta) =>
        new()
        {
            Success = true,
            TaskId = task.Id,
            TaskTitle = task.Title,
            IsChecked = isChecked,
            XpDelta = xpDelta,
            GoldDelta = goldDelta,
            HeroId = hero.Id,
            NewLevel = hero.Level,
            LeveledUp = false,
            NewXp = hero.CurrentXp,
            XpForNextLevel = hero.GetXpRequiredForNextLevel(),
            XpProgress = (double)hero.CurrentXp / hero.GetXpRequiredForNextLevel(),
            NewGold = hero.Gold,
            NewHp = hero.CurrentHp,
            MaxHp = hero.MaxHp,
            DeathCount = hero.DeathCount,
            XpBoostPercent = hero.XpBoostPercent,
            XpBoostTasksRemaining = hero.XpBoostTasksRemaining,
            StreakBonus = streak?.GetBonusXpPercent() ?? 0,
            CurrentStreak = streak?.CurrentDays ?? 0,
            StreakMultiplier = streak?.GetStreakMultiplier() ?? 1.0,
            DailyCompletions = economy.DailyTaskCompletions,
            MaxDailyCompletions = economy.MaxDailyCompletions,
            Message = isChecked ? "Daily checked" : "Daily unchecked",
        };

    private TaskDto MapToDto(GameTask task)
    {
        string? todayLocalDateStr = null;
        DateOnly? todayLocalDate = null;
        string? effectiveTimeZone = null;
        if (task.Hero != null)
        {
            effectiveTimeZone = _heroTimeService.ResolveEffectiveTimeZone(task.Hero, DateTimeOffset.UtcNow);
            todayLocalDate = _heroTimeService.GetLocalDate(DateTimeOffset.UtcNow, effectiveTimeZone);
            todayLocalDateStr = todayLocalDate.Value.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture);
        }
        var isCheckedToday = todayLocalDateStr != null && task.DailyTaskCompletions.Any(c => c.LocalDate == todayLocalDateStr && c.IsChecked);

        var isScheduledToday = true;
        string? nextScheduledLocalDate = null;
        if (task.Type == TaskType.Daily && todayLocalDate.HasValue && effectiveTimeZone != null)
        {
            isScheduledToday = _dailySchedule.IsScheduledOn(task, todayLocalDate.Value, effectiveTimeZone);
            nextScheduledLocalDate = _dailySchedule
                .NextScheduledOnOrAfter(task, todayLocalDate.Value.AddDays(1), effectiveTimeZone)
                .ToString("yyyy-MM-dd", CultureInfo.InvariantCulture);
        }

        return new TaskDto
        {
            Id = task.Id,
            HeroId = task.HeroId,
            Title = task.Title,
            Description = task.Description,
            Type = task.Type,
            Difficulty = task.Difficulty,
            Polarity = task.Polarity,
            IsCompleted = task.IsCompleted,
            IsCheckedToday = isCheckedToday,
            IsActive = task.IsActive,
            DueDate = task.DueDate,
            RepeatPattern = task.RepeatPattern,
            ChecklistJson = task.ChecklistJson,
            RemindersJson = task.RemindersJson,
            IsOverdue = task.IsOverdue(),
            IsScheduledToday = isScheduledToday,
            NextScheduledLocalDate = nextScheduledLocalDate,
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
    }

    private const string HabitResetPatternPrefix = "RESET:";

    // Habitica-style reset counter. If the habit's RepeatPattern is "RESET:DAILY|WEEKLY|MONTHLY"
    // and the current period bucket (in hero's local time zone) differs from the bucket of the last
    // counter activity, reset CompletionCount and FailCount to 0 before the next increment.
    internal static void ResetHabitCounterIfNeeded(
        GameTask task,
        DateOnly todayLocalDate,
        string effectiveTimeZoneId,
        Func<DateTimeOffset, string, DateOnly> getLocalDate)
    {
        if (task.Type != TaskType.Habit)
            return;

        if (string.IsNullOrWhiteSpace(task.RepeatPattern))
            return;

        if (!task.RepeatPattern.StartsWith(HabitResetPatternPrefix, StringComparison.Ordinal))
            return;

        var periodToken = task.RepeatPattern.Substring(HabitResetPatternPrefix.Length).Trim();
        if (periodToken.Length == 0)
            return;

        // Anchor for the "previous" period bucket: last counter activity (complete or fail).
        // Fall back to UpdatedAt (CreatedAt for new tasks) when no activity has been recorded yet.
        var previousActivityUtc = task.LastCompletedAt ?? task.UpdatedAt;
        var previousLocalDate = getLocalDate(previousActivityUtc, effectiveTimeZoneId);

        bool bucketChanged = periodToken switch
        {
            "DAILY" => previousLocalDate != todayLocalDate,
            "WEEKLY" => GetIsoWeekBucket(previousLocalDate) != GetIsoWeekBucket(todayLocalDate),
            "MONTHLY" => (previousLocalDate.Year, previousLocalDate.Month) != (todayLocalDate.Year, todayLocalDate.Month),
            _ => false,
        };

        if (bucketChanged)
        {
            task.CompletionCount = 0;
            task.FailCount = 0;
        }
    }

    private static (int IsoYear, int IsoWeek) GetIsoWeekBucket(DateOnly localDate)
    {
        var dt = localDate.ToDateTime(TimeOnly.MinValue);
        var isoWeek = System.Globalization.ISOWeek.GetWeekOfYear(dt);
        var isoYear = System.Globalization.ISOWeek.GetYear(dt);
        return (isoYear, isoWeek);
    }

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
    public bool IsCheckedToday { get; set; }
    public bool IsActive { get; set; }
    public DateTimeOffset? DueDate { get; set; }
    public string? RepeatPattern { get; set; }
    public string? ChecklistJson { get; set; }
    public string? RemindersJson { get; set; }
    public bool IsOverdue { get; set; }
    public bool IsScheduledToday { get; set; } = true;
    public string? NextScheduledLocalDate { get; set; }

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

public class UpdateTaskRequest
{
    public string Title { get; set; } = string.Empty;
    public string? Description { get; set; }
    public TaskDifficulty Difficulty { get; set; } = TaskDifficulty.Easy;
    public HabitPolarity? Polarity { get; set; }
    public DateTimeOffset? DueDate { get; set; }
    public string? RepeatPattern { get; set; }
    public string? ChecklistJson { get; set; }
    public string? RemindersJson { get; set; }
}

public class SetDailyTaskStateRequest
{
    public string LocalDate { get; set; } = string.Empty;
    public bool IsChecked { get; set; }
}

public class SetDailyTaskStateResponse
{
    public bool Success { get; set; }
    public int TaskId { get; set; }
    public string TaskTitle { get; set; } = string.Empty;
    public bool IsChecked { get; set; }
    public long XpDelta { get; set; }
    public int GoldDelta { get; set; }
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
