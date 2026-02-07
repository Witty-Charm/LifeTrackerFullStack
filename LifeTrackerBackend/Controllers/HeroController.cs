using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using LifeTracker.Data;
using LifeTracker.Models;
using LifeTracker.Constants;

namespace LifeTracker.Controllers;

[ApiController]
[Route("api/[controller]")]
public class HeroController : ControllerBase
{
    private readonly ApplicationDbContext _context;

    public HeroController(ApplicationDbContext context)
    {
        _context = context;
    }

    [HttpGet]
    public async Task<ActionResult<IEnumerable<HeroDto>>> GetHeroes()
    {
        var heroes = await _context.Heroes
            .Include(h => h.EconomyBalance)
            .Include(h => h.Streaks)
            .ToListAsync();

        return Ok(heroes.Select(h => MapToDto(h)));
    }

    [HttpGet("{id}")]
    public async Task<ActionResult<HeroDto>> GetHero(int id)
    {
        var hero = await _context.Heroes
            .Include(h => h.EconomyBalance)
            .Include(h => h.Streaks)
            .FirstOrDefaultAsync(h => h.Id == id);

        if (hero == null)
            return NotFound();

        return Ok(MapToDto(hero));
    }

    [HttpGet("1")]
    public async Task<ActionResult<HeroDto>> GetFirstHero()
    {
        var hero = await _context.Heroes
            .Include(h => h.EconomyBalance)
            .Include(h => h.Streaks)
            .FirstOrDefaultAsync();

        if (hero == null)
            return NotFound();

        return Ok(MapToDto(hero));
    }

    [HttpPost]
    public async Task<ActionResult<HeroDto>> PostHero([FromBody] CreateHeroRequest request)
    {
        if (string.IsNullOrWhiteSpace(request.Name))
            return BadRequest("Hero name is required");

        var hero = new Hero
        {
            Name = request.Name,
            Level = 1,
            MaxHp = GameConstants.CalculateMaxHp(1),
            CurrentHp = GameConstants.CalculateMaxHp(1),
            CurrentXp = 0,
            TotalXpEarned = 0,
            Gold = request.StartingGold ?? 100,
            CreatedDate = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        _context.Heroes.Add(hero);
        await _context.SaveChangesAsync();

        var economy = new EconomyBalance
        {
            HeroId = hero.Id,
            TotalGoldEarned = hero.Gold,
            MaxDailyCompletions = GameConstants.DailyTaskCap,
            DailyResetAt = DateTime.UtcNow.Date,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        _context.EconomyBalances.Add(economy);
        await _context.SaveChangesAsync();

        hero.EconomyBalance = economy;

        return CreatedAtAction(nameof(GetHero), new { id = hero.Id }, MapToDto(hero));
    }

    [HttpPut("{id}")]
    public async Task<IActionResult> PutHero(int id, Hero hero)
    {
        if (id != hero.Id)
            return BadRequest();

        _context.Entry(hero).State = EntityState.Modified;

        try
        {
            await _context.SaveChangesAsync();
        }
        catch (DbUpdateConcurrencyException)
        {
            if (!HeroExists(id))
                return NotFound();
            else
                throw;
        }

        return NoContent();
    }

    [HttpGet("{id}/stats")]
    public async Task<ActionResult<HeroStatsDto>> GetHeroStats(int id)
    {
        var hero = await _context.Heroes
            .Include(h => h.EconomyBalance)
            .Include(h => h.Streaks.Where(s => s.CurrentDays > 0))
            .FirstOrDefaultAsync(h => h.Id == id);

        if (hero == null)
            return NotFound();

        var economy = hero.EconomyBalance ?? new EconomyBalance { HeroId = hero.Id };
        long xpForNextLevel = hero.GetXpRequiredForNextLevel();
        double xpProgress = xpForNextLevel > 0 ? (double)hero.CurrentXp / xpForNextLevel : 0.0;

        economy.CheckDailyReset();

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
            DailyResetTime = economy.DailyResetAt.AddDays(1),

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
            UpdatedAt = hero.UpdatedAt
        });
    }

    [HttpPost("{id}/respawn")]
    public async Task<ActionResult<RespawnResponse>> RespawnHero(int id)
    {
        var hero = await _context.Heroes
            .Include(h => h.EconomyBalance)
            .Include(h => h.Streaks)
            .FirstOrDefaultAsync(h => h.Id == id);

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
                      $"({(int)((1 - GameConstants.RecoveryDebuffMultiplier) * 100)}% reduced rewards)."
        });
    }

    [HttpPost("{id}/heal")]
    public async Task<ActionResult<HealResponse>> HealHero(int id, [FromQuery] int amount = 0)
    {
        var hero = await _context.Heroes.FindAsync(id);
        if (hero == null)
            return NotFound("Hero not found");

        if (hero.IsDead)
            return BadRequest("Cannot heal a dead hero. Use /respawn first.");

        if (hero.CurrentHp >= hero.MaxHp)
            return BadRequest("Hero is already at full HP");

        int healAmount = amount > 0 ? amount : (hero.MaxHp - hero.CurrentHp);
        healAmount = Math.Min(healAmount, hero.MaxHp - hero.CurrentHp);

        int goldCost = healAmount;

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
            Message = $"Healed {healAmount} HP for {goldCost} gold"
        });
    }

    private bool HeroExists(int id)
    {
        return _context.Heroes.Any(e => e.Id == id);
    }

    private HeroDto MapToDto(Hero hero)
    {
        var economy = hero.EconomyBalance ?? new EconomyBalance { HeroId = hero.Id };
        economy.CheckDailyReset();

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
            DailyCompletions = economy.DailyTaskCompletions,
            DailyCompletionsMax = economy.MaxDailyCompletions
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
    public int DailyCompletions { get; set; }
    public int DailyCompletionsMax { get; set; }
}

public class CreateHeroRequest
{
    public string Name { get; set; } = string.Empty;
    public int? StartingGold { get; set; }
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
    public DateTime? DeathTime { get; set; }
    public int DailyCompletions { get; set; }
    public int DailyCompletionsMax { get; set; }
    public double DailyProgress { get; set; }
    public DateTime DailyResetTime { get; set; }
    public double XpMultiplier { get; set; }
    public double GoldMultiplier { get; set; }
    public bool IsInPenaltyPeriod { get; set; }
    public DateTime? PenaltyEndsAt { get; set; }
    public bool IsInRecovery { get; set; }
    public DateTime? RecoveryEndsAt { get; set; }
    public double RecoveryMultiplier { get; set; }
    public int ActiveStreaks { get; set; }
    public int LongestStreak { get; set; }
    public DateTime CreatedDate { get; set; }
    public DateTime UpdatedAt { get; set; }
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
    public DateTime? RecoveryEndsAt { get; set; }
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