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

    // GET: api/Task
    [HttpGet]
    public async Task<ActionResult<IEnumerable<GameTask>>> GetTasks()
    {
        return await _context.GameTasks.ToListAsync();
    }

    // GET: api/Task/5
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

    // POST: api/Task
    [HttpPost]
    public async Task<ActionResult<GameTask>> PostTask(GameTask task)
    {
    _context.GameTasks.Add(task);
    // УДАЛИ ОТСЮДА НАЧИСЛЕНИЕ ОПЫТА! 
    // hero.GainXP(task.RewardXP); <- ЭТО ВЫРЕЗАЕМ
    await _context.SaveChangesAsync();
    return CreatedAtAction(nameof(GetTask), new { id = task.Id }, task);
    }

    // PUT: api/Task/5/complete
    [HttpPut("{id}/complete")]
    public async Task<IActionResult> CompleteTask(int id)
    {
    var task = await _context.GameTasks.FindAsync(id);
    if (task == null) return NotFound();

    task.IsCompleted = true; // Помечаем как выполненную

    var hero = await _context.Heroes.FirstOrDefaultAsync();
    if (hero != null)
    {
        hero.GainXP(task.RewardXP); // НАЧИСЛЯЕМ ТУТ
        _context.Entry(hero).State = EntityState.Modified;
    }

    await _context.SaveChangesAsync();
    return NoContent();
    }
}

