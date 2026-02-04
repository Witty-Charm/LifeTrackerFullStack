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
        return await _context.GameTasks.ToListAsync();
    }

    [HttpGet("{id}")]
    public async Task<ActionResult<GameTask>> GetTask(int id)
    {
        var task = await _context.GameTasks.FindAsync(id);

        if (task == null)
        {
            return NotFound();
        }

        return task;
    }

    [HttpPost]
    public async Task<ActionResult<GameTask>> PostTask(GameTask task)
    {
    _context.GameTasks.Add(task);
    await _context.SaveChangesAsync();
    return CreatedAtAction(nameof(GetTask), new { id = task.Id }, task);
    }

    [HttpPut("{id}/complete")]
    public async Task<IActionResult> CompleteTask(int id, [FromQuery] bool fail = false)
    {
    var task = await _context.GameTasks.FindAsync(id);
    if (task == null) return NotFound();

    var hero = await _context.Heroes.FirstOrDefaultAsync();
    if (hero == null) return BadRequest("Hero not found");

    if (fail) 
    {
        int damage = task.Difficulty switch {
            Difficulty.Easy => 5,
            Difficulty.Medium => 10,
            Difficulty.Hard => 20,
            _ => 10
        };
        hero.TakeDamage(damage);
    }
    else 
    {
        task.IsCompleted = true;
        
        int goldReward = task.Difficulty switch {
            Difficulty.Easy => 5,
            Difficulty.Medium => 15,
            Difficulty.Hard => 50,
            _ => 10
        };

        hero.GainXP(task.RewardXP);
        hero.Gold += goldReward;
    }

    _context.Entry(hero).State = EntityState.Modified;
    await _context.SaveChangesAsync();
    
    return Ok(hero);
    }
}

