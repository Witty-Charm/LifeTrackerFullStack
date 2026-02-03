using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using LifeTracker.Data;
using LifeTracker.Models;

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
        {
            return NotFound();
        }

        return hero;
    }

    [HttpPost]
    public async Task<ActionResult<Hero>> PostHero(Hero hero)
    {
        if (hero.Level == 0) hero.Level = 1;
        if (hero.MaxXP == 0) hero.MaxXP = 100;
        if (hero.HP == 0) hero.HP = 100;

        _context.Heroes.Add(hero);
        await _context.SaveChangesAsync();

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
            {
                return NotFound();
            }
            else
            {
                throw;
            }
        }

        return NoContent();
    }

    private bool HeroExists(int id)
    {
        return _context.Heroes.Any(e => e.Id == id);
    }
}

