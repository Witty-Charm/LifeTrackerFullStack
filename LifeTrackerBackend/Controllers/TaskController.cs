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
    
        if (task.IsCompleted && task.Type == TaskType.OneTime) 
            return BadRequest("Task is already completed");
    
        var hero = await _context.Heroes
            .Include(h => h.EconomyBalance)
            .FirstOrDefaultAsync(h => h.Id == task.HeroId);
    
        if (hero == null) return BadRequest("Hero not found");
        if (hero.IsDead) return BadRequest("Hero is dead");
    
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
    
        // Get streak for multiplier calculation
        Streak? streak = null;
        if (task.Type == TaskType.Habit)
        {
            streak = await _context.Streaks
                .FirstOrDefaultAsync(s => s.HeroId == hero.Id && s.TaskId == task.Id);

            if (streak == null)
            { 
                streak = new Streak { HeroId = hero.Id, TaskId = task.Id };
                _context.Streaks.Add(streak);
            }
            streak.RegisterSuccess();
        }

        // Use GameEngineService for GDD-compliant reward calculation
        var (xpReward, goldReward) = _gameEngine.ApplyTaskCompletion(task, hero, streak, economy);
    
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
            Message = $"Task completed! +{xpReward} XP, +{goldReward} Gold"
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
            return BadRequest("Hero is dead. Use /api/Hero/{id}/respawn to continue.");

        // Break streak if applicable
        if (task.Type == TaskType.Habit)
        {
            var streak = await _context.Streaks
                .FirstOrDefaultAsync(s => s.HeroId == hero.Id && s.TaskId == task.Id);

            if (streak != null && streak.CurrentDays > 0)
                streak.Break();
        }

        // Use GameEngineService for GDD-compliant penalty calculation
        var (hpLost, goldLost, heroDied) = _gameEngine.ApplyTaskFailure(task, hero);

        await _context.SaveChangesAsync();

        return Ok(new FailTaskResponse
        {
            TaskId = task.Id,
            DamageDealt = hpLost,
            GoldLost = goldLost,
            NewHp = hero.CurrentHp,
            NewGold = hero.Gold,
            HeroDied = heroDied,
            Message = heroDied 
                ? $"DEATH! You took {hpLost} damage and lost {goldLost} gold." 
                : $"Task failed! -{hpLost} HP, -{goldLost} Gold"
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

// dtos
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
    public int GoldLost { get; set; }
    public int NewHp { get; set; }
    public int NewGold { get; set; }
    public bool HeroDied { get; set; }
    public string Message { get; set; } = string.Empty;
}