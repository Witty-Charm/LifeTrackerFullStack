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
public class HeroController : DeviceScopedControllerBase
{
    private readonly ApplicationDbContext _context;
    private readonly AchievementService _achievementService;
    private readonly IHeroTimeService _heroTimeService;

    public HeroController(
        ApplicationDbContext context,
        AchievementService achievementService,
        IHeroTimeService heroTimeService,
        ICurrentHeroService currentHeroService)
        : base(currentHeroService)
    {
        _context = context;
        _achievementService = achievementService;
        _heroTimeService = heroTimeService;
    }

    internal static HeroController CreateForTests(ApplicationDbContext context, IHeroTimeService heroTimeService) =>
        new(context, new AchievementService(context), heroTimeService, new CurrentHeroService(context));

    [HttpGet("me")]
    public async Task<ActionResult<HeroDto>> GetCurrentHero()
    {
        _ = RequireCurrentDevice(out var errorResult);
        if (errorResult is not null)
            return errorResult;

        var hero = await CurrentHeroService.GetCurrentHeroAsync(
            HttpContext,
            query => query.Include(h => h.EconomyBalance).Include(h => h.Streaks));

        if (hero == null)
            return NotFound();

        await ApplyDailyResetAsync(hero);
        return Ok(MapToDto(hero));
    }

    [HttpGet("{id}")]
    public async Task<ActionResult<HeroDto>> GetHero(int id)
    {
        _ = RequireCurrentDevice(out var errorResult);
        if (errorResult is not null)
            return errorResult;

        var hero = await CurrentHeroService.GetOwnedHeroAsync(
            HttpContext,
            id,
            query => query.Include(h => h.EconomyBalance).Include(h => h.Streaks));

        if (hero == null)
            return NotFound();

        await ApplyDailyResetAsync(hero);
        return Ok(MapToDto(hero));
    }

    [HttpPost]
    public async Task<ActionResult<HeroDto>> PostHero([FromBody] CreateHeroRequest request)
    {
        var deviceId = RequireCurrentDevice(out var errorResult);
        if (errorResult is not null)
            return errorResult;

        if (string.IsNullOrWhiteSpace(request.Name))
            return BadRequest("Hero name is required");

        var hero = new Hero
        {
            OwnerDeviceId = deviceId!,
            Name = request.Name,
            Level = 1,
            MaxHp = GameConstants.CalculateMaxHp(1),
            CurrentHp = GameConstants.CalculateMaxHp(1),
            CurrentXp = 0,
            TotalXpEarned = 0,
            Gold = request.StartingGold ?? 100,
            CreatedDate = DateTimeOffset.UtcNow,
            UpdatedAt = DateTimeOffset.UtcNow,
        };

        _context.Heroes.Add(hero);
        await _context.SaveChangesAsync();

        var utcNow = DateTimeOffset.UtcNow;
        var effectiveTimeZone = _heroTimeService.ResolveEffectiveTimeZone(hero, utcNow);
        var todayLocalDate = _heroTimeService.GetLocalDate(utcNow, effectiveTimeZone);

        var economy = new EconomyBalance
        {
            HeroId = hero.Id,
            TotalGoldEarned = hero.Gold,
            MaxDailyCompletions = GameConstants.DailyTaskCap,
            DailyResetAt = DateTimeOffset.UtcNow.Date,
            LastDailyResetLocalDate = _heroTimeService.FormatLocalDate(todayLocalDate),
            CreatedAt = DateTimeOffset.UtcNow,
            UpdatedAt = DateTimeOffset.UtcNow,
        };

        _context.EconomyBalances.Add(economy);
        await _context.SaveChangesAsync();

        hero.EconomyBalance = economy;

        return CreatedAtAction(nameof(GetHero), new { id = hero.Id }, MapToDto(hero));
    }

    [HttpGet("{id}/achievements")]
    public async Task<ActionResult<HeroAchievementsResponse>> GetAchievements(int id)
    {
        _ = RequireCurrentDevice(out var errorResult);
        if (errorResult is not null)
            return errorResult;

        var hero = await CurrentHeroService.GetOwnedHeroAsync(HttpContext, id);
        if (hero == null)
            return NotFound();

        var achievements = await _achievementService.GetAchievementsAsync(id);
        return Ok(new HeroAchievementsResponse
        {
            HeroId = id,
            TotalCount = achievements.Count,
            UnlockedCount = achievements.Count(x => x.Unlocked),
            Achievements = achievements.ToList(),
        });
    }

    [HttpGet("{id}/stats")]
    public async Task<ActionResult<HeroStatsDto>> GetHeroStats(int id)
    {
        _ = RequireCurrentDevice(out var errorResult);
        if (errorResult is not null)
            return errorResult;

        var hero = await CurrentHeroService.GetOwnedHeroAsync(
            HttpContext,
            id,
            query => query.Include(h => h.EconomyBalance).Include(h => h.Streaks.Where(s => s.CurrentDays > 0)));

        if (hero == null)
            return NotFound();

        var economy = hero.EconomyBalance ?? new EconomyBalance { HeroId = hero.Id };
        long xpForNextLevel = hero.GetXpRequiredForNextLevel();
        double xpProgress = xpForNextLevel > 0 ? (double)hero.CurrentXp / xpForNextLevel : 0.0;

        var utcNow = DateTimeOffset.UtcNow;
        var effectiveTimeZone = _heroTimeService.ResolveEffectiveTimeZone(hero, utcNow);
        var todayLocalDate = _heroTimeService.GetLocalDate(utcNow, effectiveTimeZone);

        economy.CheckDailyReset(todayLocalDate);
        await _context.SaveChangesAsync();

        return Ok(new HeroStatsDto
        {
            Id = hero.Id,
            Name = hero.Name,
            Level = hero.Level,
            CurrentXp = hero.CurrentXp,
            XpForNextLevel = xpForNextLevel,
            XpProgress = xpProgress,
            TotalXpEarned = hero.TotalXpEarned,
            CurrentHp = hero.CurrentHp,
            MaxHp = hero.MaxHp,
            HpPercent = (double)hero.CurrentHp / hero.MaxHp,
            IsDead = hero.IsDead,
            DeathCount = hero.DeathCount,
            DeathTime = hero.DeathTime,
            Gold = hero.Gold,
            TotalGoldEarned = economy.TotalGoldEarned,
            TotalGoldSpent = economy.TotalGoldSpent,
            DailyCompletions = economy.DailyTaskCompletions,
            DailyCompletionsMax = economy.MaxDailyCompletions,
            DailyProgress = (double)economy.DailyTaskCompletions / economy.MaxDailyCompletions,
            DailyResetTime = _heroTimeService.GetNextLocalMidnightUtc(utcNow, effectiveTimeZone),
            XpMultiplier = (double)economy.GetFinalXpMultiplier(),
            GoldMultiplier = (double)economy.GoldMultiplier,
            IsInPenaltyPeriod = economy.IsInPenaltyPeriod,
            PenaltyEndsAt = economy.PenaltyEndsAt,
            IsInRecovery = hero.IsInRecovery(),
            RecoveryEndsAt = hero.RecoveryEndsAt,
            RecoveryMultiplier = hero.GetRecoveryMultiplier(),
            ActiveStreaks = hero.Streaks.Count(s => s.CurrentDays > 0),
            LongestStreak = hero.Streaks.Any() ? hero.Streaks.Max(s => s.LongestDays) : 0,
            CreatedDate = hero.CreatedDate,
            UpdatedAt = hero.UpdatedAt,
        });
    }

    [HttpPatch("{id}/timezone")]
    public async Task<ActionResult<HeroTimeZoneUpdateResponse>> UpdateHeroTimeZone(int id, [FromBody] UpdateHeroTimeZoneRequest request)
    {
        _ = RequireCurrentDevice(out var errorResult);
        if (errorResult is not null)
            return errorResult;

        if (string.IsNullOrWhiteSpace(request.TimeZoneId))
            return BadRequest("timeZoneId is required");

        var hero = await CurrentHeroService.GetOwnedHeroAsync(HttpContext, id);
        if (hero == null)
            return NotFound("Hero not found");

        if (!_heroTimeService.IsValidIana(request.TimeZoneId))
            return BadRequest("Invalid IANA timezone id");

        var utcNow = DateTimeOffset.UtcNow;
        var currentTimeZone = _heroTimeService.ResolveEffectiveTimeZone(hero, utcNow);

        if (string.Equals(currentTimeZone, request.TimeZoneId, StringComparison.Ordinal))
        {
            await _context.SaveChangesAsync();
            Console.WriteLine($"[AUDIT] Hero timezone change skipped (same timezone). heroId={hero.Id}, timezone={currentTimeZone}, atUtc={utcNow:O}");
            return Ok(new HeroTimeZoneUpdateResponse(currentTimeZone, null, null, "Timezone unchanged"));
        }

        if (!_heroTimeService.CanSwitchTimeZone(hero, utcNow))
            return BadRequest("Timezone can be changed once every 24 hours");

        Console.WriteLine($"[AUDIT] Hero timezone change requested. heroId={hero.Id}, requested={request.TimeZoneId}, atUtc={utcNow:O}");

        var currentLocalDate = _heroTimeService.GetLocalDate(utcNow, currentTimeZone);
        hero.PendingTimeZoneId = request.TimeZoneId;
        hero.TimeZoneSwitchAfterLocalDate = _heroTimeService.FormatLocalDate(currentLocalDate);
        hero.LastTimeZoneChangedAt = utcNow;
        hero.UpdatedAt = utcNow;

        await _context.SaveChangesAsync();

        Console.WriteLine($"[AUDIT] Hero timezone change scheduled. heroId={hero.Id}, from={currentTimeZone}, to={hero.PendingTimeZoneId}, switchAfterLocalDate={hero.TimeZoneSwitchAfterLocalDate}, atUtc={utcNow:O}");

        return Ok(new HeroTimeZoneUpdateResponse(
            CurrentTimeZoneId: currentTimeZone,
            PendingTimeZoneId: hero.PendingTimeZoneId,
            SwitchAfterLocalDate: hero.TimeZoneSwitchAfterLocalDate,
            Message: "Timezone switch scheduled for next local day"));
    }

    [HttpPost("{id}/respawn")]
    public async Task<ActionResult<RespawnResponse>> RespawnHero(int id)
    {
        _ = RequireCurrentDevice(out var errorResult);
        if (errorResult is not null)
            return errorResult;

        var hero = await CurrentHeroService.GetOwnedHeroAsync(
            HttpContext,
            id,
            query => query.Include(h => h.EconomyBalance).Include(h => h.Streaks));

        if (hero == null)
            return NotFound("Hero not found");

        if (!hero.IsDead)
            return BadRequest("Hero is not dead");

        int hpBefore = hero.CurrentHp;
        hero.Respawn();

        var economy = hero.EconomyBalance;
        if (economy != null && economy.IsInPenaltyPeriod)
        {
            economy.IsInPenaltyPeriod = false;
            economy.PenaltyEndsAt = null;
        }

        await _context.SaveChangesAsync();

        return Ok(new RespawnResponse
        {
            Success = true,
            HeroId = hero.Id,
            HeroName = hero.Name,
            OldHp = hpBefore,
            NewHp = hero.CurrentHp,
            MaxHp = hero.MaxHp,
            RecoveryDebuffActive = true,
            RecoveryEndsAt = hero.RecoveryEndsAt,
            RecoveryMultiplier = hero.GetRecoveryMultiplier(),
            DeathCount = hero.DeathCount,
            Message = $"Welcome back, {hero.Name}! You respawned with {hero.CurrentHp}/{hero.MaxHp} HP. " +
                      $"Recovery debuff active for {GameConstants.RecoveryDebuffHours} hours " +
                      $"({(int)((1 - GameConstants.RecoveryDebuffMultiplier) * 100)}% reduced rewards).",
        });
    }

    [HttpPost("{id}/heal")]
    public async Task<ActionResult<HealResponse>> HealHero(int id, [FromQuery] int amount = 0)
    {
        _ = RequireCurrentDevice(out var errorResult);
        if (errorResult is not null)
            return errorResult;

        var hero = await CurrentHeroService.GetOwnedHeroAsync(HttpContext, id);
        if (hero == null)
            return NotFound("Hero not found");

        if (hero.IsDead)
            return BadRequest("Cannot heal a dead hero. Use /respawn first.");

        if (hero.CurrentHp >= hero.MaxHp)
            return BadRequest("Hero is already at full HP");

        int healAmount = amount > 0 ? amount : Math.Max(1, hero.MaxHp / 4);
        healAmount = Math.Min(healAmount, hero.MaxHp - hero.CurrentHp);

        int goldCost = (int)Math.Ceiling(healAmount * 4.0);

        if (hero.Gold < goldCost)
            return BadRequest($"Not enough gold. Need {goldCost} gold to heal {healAmount} HP.");

        hero.Gold -= goldCost;
        hero.CurrentHp += healAmount;
        hero.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync();

        return Ok(new HealResponse
        {
            Success = true,
            HeroId = hero.Id,
            HpHealed = healAmount,
            GoldSpent = goldCost,
            NewHp = hero.CurrentHp,
            MaxHp = hero.MaxHp,
            NewGold = hero.Gold,
            Message = $"Healed {healAmount} HP for {goldCost} gold",
        });
    }

    private async Task ApplyDailyResetAsync(Hero hero)
    {
        var utcNow = DateTimeOffset.UtcNow;
        var effectiveTimeZone = _heroTimeService.ResolveEffectiveTimeZone(hero, utcNow);
        var todayLocalDate = _heroTimeService.GetLocalDate(utcNow, effectiveTimeZone);
        hero.EconomyBalance?.CheckDailyReset(todayLocalDate);
        await _context.SaveChangesAsync();
    }

    private static HeroDto MapToDto(Hero hero)
    {
        var economy = hero.EconomyBalance ?? new EconomyBalance { HeroId = hero.Id };

        return new HeroDto
        {
            Id = hero.Id,
            Name = hero.Name,
            Level = hero.Level,
            Xp = hero.CurrentXp,
            MaxXP = hero.GetXpRequiredForNextLevel(),
            Hp = hero.CurrentHp,
            MaxHP = hero.MaxHp,
            Gold = hero.Gold,
            IsDead = hero.IsDead,
            DeathCount = hero.DeathCount,
            IsInRecovery = hero.IsInRecovery(),
            RecoveryMultiplier = hero.GetRecoveryMultiplier(),
            XpBoostPercent = hero.XpBoostPercent,
            XpBoostTasksRemaining = hero.XpBoostTasksRemaining,
            DailyCompletions = economy.DailyTaskCompletions,
            DailyCompletionsMax = economy.MaxDailyCompletions,
        };
    }
}

public class HeroDto
{
    public int Id { get; set; }
    public string Name { get; set; } = string.Empty;
    public int Level { get; set; }
    public long Xp { get; set; }
    public long MaxXP { get; set; }
    public int Hp { get; set; }
    public int MaxHP { get; set; }
    public int Gold { get; set; }
    public bool IsDead { get; set; }
    public int DeathCount { get; set; }
    public bool IsInRecovery { get; set; }
    public double RecoveryMultiplier { get; set; }
    public int XpBoostPercent { get; set; }
    public int XpBoostTasksRemaining { get; set; }
    public int DailyCompletions { get; set; }
    public int DailyCompletionsMax { get; set; }
}

public class CreateHeroRequest
{
    public string Name { get; set; } = string.Empty;
    public int? StartingGold { get; set; }
}

public class HeroAchievementsResponse
{
    public int HeroId { get; set; }
    public int TotalCount { get; set; }
    public int UnlockedCount { get; set; }
    public List<AchievementListItem> Achievements { get; set; } = new();
}

public class HeroStatsDto
{
    public int Id { get; set; }
    public string Name { get; set; } = string.Empty;
    public int Level { get; set; }
    public long CurrentXp { get; set; }
    public long XpForNextLevel { get; set; }
    public double XpProgress { get; set; }
    public long TotalXpEarned { get; set; }
    public int CurrentHp { get; set; }
    public int MaxHp { get; set; }
    public double HpPercent { get; set; }
    public int Gold { get; set; }
    public long TotalGoldEarned { get; set; }
    public long TotalGoldSpent { get; set; }
    public bool IsDead { get; set; }
    public int DeathCount { get; set; }
    public DateTimeOffset? DeathTime { get; set; }
    public int DailyCompletions { get; set; }
    public int DailyCompletionsMax { get; set; }
    public double DailyProgress { get; set; }
    public DateTimeOffset DailyResetTime { get; set; }
    public double XpMultiplier { get; set; }
    public double GoldMultiplier { get; set; }
    public bool IsInPenaltyPeriod { get; set; }
    public DateTimeOffset? PenaltyEndsAt { get; set; }
    public bool IsInRecovery { get; set; }
    public DateTimeOffset? RecoveryEndsAt { get; set; }
    public double RecoveryMultiplier { get; set; }
    public int ActiveStreaks { get; set; }
    public int LongestStreak { get; set; }
    public DateTimeOffset CreatedDate { get; set; }
    public DateTimeOffset UpdatedAt { get; set; }
}

public class RespawnResponse
{
    public bool Success { get; set; }
    public int HeroId { get; set; }
    public string HeroName { get; set; } = string.Empty;
    public int OldHp { get; set; }
    public int NewHp { get; set; }
    public int MaxHp { get; set; }
    public bool RecoveryDebuffActive { get; set; }
    public DateTimeOffset? RecoveryEndsAt { get; set; }
    public double RecoveryMultiplier { get; set; }
    public int DeathCount { get; set; }
    public string Message { get; set; } = string.Empty;
}

public class HealResponse
{
    public bool Success { get; set; }
    public int HeroId { get; set; }
    public int HpHealed { get; set; }
    public int GoldSpent { get; set; }
    public int NewHp { get; set; }
    public int MaxHp { get; set; }
    public int NewGold { get; set; }
    public string Message { get; set; } = string.Empty;
}

public record UpdateHeroTimeZoneRequest(string TimeZoneId);

public record HeroTimeZoneUpdateResponse(
    string CurrentTimeZoneId,
    string? PendingTimeZoneId,
    string? SwitchAfterLocalDate,
    string Message
);
