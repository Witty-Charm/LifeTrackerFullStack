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
    public async Task<ActionResult<IEnumerable<Hero>>> GetHeroes()
    {
        return await _context.Heroes.ToListAsync();
    }
    
    [HttpGet("{id}")]
    public async Task<ActionResult<Hero>> GetHero(int id)
    {
        var hero = await _context.Heroes.FindAsync(id);
        if (hero == null)
            return NotFound();
        return hero;
    }
    
    [HttpPost]
    public async Task<ActionResult<Hero>> PostHero(string name)
    {
        var hero = new Hero
        {
            Name = name,
            Level = 1,
            MaxHp = GameConstants.CalculateMaxHp(1),
            CurrentHp = GameConstants.CalculateMaxHp(1),
            CurrentXp = 0,
            Gold = 0,
            CreatedDate = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        _context.Heroes.Add(hero);
        await _context.SaveChangesAsync();
        
        var economy = new EconomyBalance 
        { 
            HeroId = hero.Id,
            MaxDailyCompletions = 50,
            DailyResetAt = DateTime.UtcNow.Date
        };
    
        _context.EconomyBalances.Add(economy);
        await _context.SaveChangesAsync();
        
        hero.EconomyBalance = economy;

        return CreatedAtAction(nameof(GetHero), new { id = hero.Id }, hero);
    }
    
    [HttpPut("{id}")]
    public async Task<IActionResult> PutHero(int id, Hero hero)
    {
        if (id != hero.Id)
        {
            return BadRequest();
        }

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

    private bool HeroExists(int id)
    {
        return _context.Heroes.Any(e => e.Id == id);
    }

    [HttpGet("1")]
    public async Task<ActionResult<Hero>> GetFirstHero()
    {
        var hero = await _context.Heroes
            .Include(h => h.EconomyBalance)
            .Include(h => h.Streaks)
            .FirstOrDefaultAsync();
        
        if (hero == null)
            return NotFound();
        
        return Ok(hero);
    }

    [HttpGet("{id}/stats")]
    public async Task<ActionResult<HeroStatsDto>> GetHeroStats(int id)
    {
        var hero = await _context.Heroes
            .Include(h => h.EconomyBalance)
            .FirstOrDefaultAsync(h => h.Id == id);
        
        if (hero == null)
            return NotFound();
        
        var economy = hero.EconomyBalance ?? new EconomyBalance { HeroId = hero.Id };
        long xpForNextLevel = hero.GetXpRequiredForNextLevel();
        double xpProgress = xpForNextLevel > 0 ? (double)hero.CurrentXp / xpForNextLevel : 0.0;

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
            Gold = hero.Gold,
            IsDead = hero.IsDead,
            DeathCount = hero.DeathCount,
            DailyCompletions = economy.DailyTaskCompletions,
            DailyCompletionsMax = economy.MaxDailyCompletions,
            XpMultiplier = (double)economy.GetFinalXpMultiplier(),
            IsInPenaltyPeriod = economy.IsInPenaltyPeriod,
            IsInRecovery = hero.IsInRecovery(),
            RecoveryEndsAt = hero.RecoveryEndsAt
        });
    }

    /// <summary>
    /// Respawn hero after death. Applies 4-hour recovery debuff.
    /// </summary>
    [HttpPut("{id}/respawn")]
    public async Task<ActionResult<RespawnResponse>> RespawnHero(int id)
    {
        var hero = await _context.Heroes.FindAsync(id);
        if (hero == null)
            return NotFound("Hero not found");
        
        if (!hero.IsDead)
            return BadRequest("Hero is not dead");
        
        hero.Respawn();
        await _context.SaveChangesAsync();
        
        return Ok(new RespawnResponse
        {
            HeroId = hero.Id,
            NewHp = hero.CurrentHp,
            MaxHp = hero.MaxHp,
            RecoveryEndsAt = hero.RecoveryEndsAt,
            Message = $"Hero respawned! Recovery debuff active for {GameConstants.RecoveryDebuffHours} hours (-25% rewards)."
        });
    }
}

// dto
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
    public int Gold { get; set; }
    public bool IsDead { get; set; }
    public int DeathCount { get; set; }
    public int DailyCompletions { get; set; }
    public int DailyCompletionsMax { get; set; }
    public double XpMultiplier { get; set; }
    public bool IsInPenaltyPeriod { get; set; }
    public bool IsInRecovery { get; set; }
    public DateTime? RecoveryEndsAt { get; set; }
}

public class RespawnResponse
{
    public int HeroId { get; set; }
    public int NewHp { get; set; }
    public int MaxHp { get; set; }
    public DateTime? RecoveryEndsAt { get; set; }
    public string Message { get; set; } = string.Empty;
}

