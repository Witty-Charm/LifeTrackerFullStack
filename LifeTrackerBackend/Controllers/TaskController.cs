using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using LifeTracker.Data;
using LifeTracker.Models;

namespace LifeTracker.Controllers;

[ApiController]
[Route("api/[controller]")]
public class TaskController : ControllerBase
{
    private readonly ApplicationDbContext _context;

    public TaskController(ApplicationDbContext context)
    {
        _context = context;
    }

    [HttpGet]
    public async Task<ActionResult<IEnumerable<GameTask>>> GetTasks()
    {
        return await _context.GameTasks
            .Where(t => t.IsActive)
            .ToListAsync();
    }

    [HttpGet("{id}")]
    public async Task<ActionResult<GameTask>> GetTask(int id)
    {
        var task = await _context.GameTasks.FindAsync(id);
        if (task == null)
        {
            return NotFound();
        }

        return Ok(task);
    }

    [HttpPost]
    public async Task<ActionResult<GameTask>> PostTask([FromBody] GameTask task)
    {
        if (string.IsNullOrWhiteSpace(task.Title))
            return BadRequest("Title is required");

        if (task.HeroId == 0)
        {
            var hero = await _context.Heroes.FirstOrDefaultAsync();
            if (hero == null) return BadRequest("Hero not found");
            task.HeroId = hero.Id;
        }

        task.IsCompleted = false;
        task.CompletionCount = 0;
        task.FailCount = 0;
        task.CreatedAt = DateTime.UtcNow;

        _context.GameTasks.Add(task);
        await _context.SaveChangesAsync();

        return CreatedAtAction(nameof(GetTask), new { id = task.Id }, task);
    }

    [HttpPut("{id}/complete")]
    public async Task<ActionResult<CompleteTaskResponse>> CompleteTask(int id)
    {
        var task = await _context.GameTasks.FindAsync(id);
        if (task == null) return NotFound("Task not found");

        var hero = await _context.Heroes
            .Include(h => h.EconomyBalance)
            .FirstOrDefaultAsync(h => h.Id == task.HeroId);
        if (hero == null)
            return BadRequest("Hero not found");

        if (hero.IsDead)
            return BadRequest("Hero is dead");
        
        var economy = hero.EconomyBalance;
        if (economy == null)
        {
            economy = new EconomyBalance { HeroId = hero.Id };
            _context.EconomyBalances.Add(economy);
            hero.EconomyBalance = economy;
        }
        
        economy.CheckDailyReset();
        if (!economy.CanCompleteTask())
            return BadRequest("Daily task limit reached (50 tasks/day)");

        long xpReward = task.GetBaseRewardXP();
        int goldReward = task.GetGoldReward();

        xpReward = (long)(xpReward * economy.GetFinalXpMultiplier());

        hero.GainXP(xpReward);
        hero.Gold += goldReward;
        hero.UpdatedAt = DateTime.UtcNow;

        task.IsCompleted = true;
        task.CompletionCount++;
        task.LastCompletedAt = DateTime.UtcNow;
        
        if (task.Type == TaskType.Habit)
        {
            var streak = await _context.Streaks
                .FirstOrDefaultAsync(s => s.HeroId == hero.Id && s.TaskId == task.Id);

            if (streak == null) 
            {
                streak = new Streak { HeroId = hero.Id, TaskId = task.Id };
                _context.Streaks.Add(streak);
            }

            streak.RegisterSuccess();
        }

        economy.IncrementDailyCompletion(); 
        
            _context.EconomyBalances.Add(economy);

        await _context.SaveChangesAsync();

        return Ok(new CompleteTaskResponse
        {
            TaskId = task.Id,
            XpGained = xpReward,
            GoldGained = goldReward,
            NewLevel = hero.Level,
            NewXp = hero.CurrentXp,
            NewGold = hero.Gold,
            NewHp = hero.CurrentHp,
            Message = $"Task completed! +{xpReward} XP, +{goldReward} Gold"  // ← ИСПРАВЛЕНО: убрал пробелы около +
        });
    }

    [HttpPut("{id}/fail")]
    public async Task<ActionResult<FailTaskResponse>> FailTask(int id)
    {
        var task = await _context.GameTasks.FindAsync(id);
        if (task == null) return NotFound("Task not found");

        var hero = await _context.Heroes.FindAsync(task.HeroId);
        if (hero == null) return BadRequest("Hero not found");

        if (hero.IsDead)
            return BadRequest("Hero is dead");

        int hpDamage = task.GetFailPenalty();

        if (task.Type == TaskType.Habit)
        {
            var streak = await _context.Streaks
                .FirstOrDefaultAsync(s => s.HeroId == hero.Id && s.TaskId == task.Id);

            if (streak != null && streak.CurrentDays > 0)
                streak.Break();
        }

        hero.TakeDamage(hpDamage);
        bool heroDied = hero.IsDead;

        task.FailCount++;
        task.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync();

        return Ok(new FailTaskResponse
        {
            TaskId = task.Id,
            DamageDealt = hpDamage,
            NewHp = hero.CurrentHp,
            HeroDied = hero.IsDead,
            Message = hero.IsDead ? $"DEATH! You took {hpDamage} damage." : $"Task failed! Took {hpDamage} damage."
        });
    }

    [HttpDelete("{id}")]
    public async Task<IActionResult> DeleteTask(int id)
    {
        var task = await _context.GameTasks.FindAsync(id);
        if (task == null)
            return NotFound();
        
        _context.GameTasks.Remove(task);
        await _context.SaveChangesAsync();
        
        return NoContent();
    }
}

// ===== DTOs =====
public class CompleteTaskResponse
{
    public int TaskId { get; set; }
    public long XpGained { get; set; }
    public int GoldGained { get; set; }
    public int NewLevel { get; set; }
    public long NewXp { get; set; }
    public int NewGold { get; set; }
    public int NewHp { get; set; }
    public string Message { get; set; } = string.Empty;
}

public class FailTaskResponse
{
    public int TaskId { get; set; }
    public int DamageDealt { get; set; }
    public int NewHp { get; set; }
    public bool HeroDied { get; set; }
    public string Message { get; set; } = string.Empty;
}