using Microsoft.EntityFrameworkCore;
using LifeTracker.Models;

namespace LifeTracker.Data;

public class ApplicationDbContext : DbContext
{
    public ApplicationDbContext(DbContextOptions<ApplicationDbContext> options)
        : base(options)
    {
    }

    public DbSet<Hero> Heroes { get; set; }
    public DbSet<GameTask> GameTasks { get; set; }
    public DbSet<Streak> Streaks { get; set; }
    public DbSet<EconomyBalance> EconomyBalances { get; set; }

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);

        modelBuilder.Entity<Hero>()
            .HasMany(h => h.Streaks)
            .WithOne(s => s.Hero)
            .HasForeignKey(s => s.HeroId)
            .OnDelete(DeleteBehavior.Cascade);

        modelBuilder.Entity<Hero>()
            .HasMany(h => h.Tasks)
            .WithOne(t => t.Hero)
            .HasForeignKey(t => t.HeroId)
            .OnDelete(DeleteBehavior.Cascade);

        modelBuilder.Entity<Hero>()
            .HasOne(h => h.EconomyBalance)
            .WithOne(e => e.Hero)
            .HasForeignKey<EconomyBalance>(e => e.HeroId)
            .OnDelete(DeleteBehavior.Cascade);

        modelBuilder.Entity<GameTask>()
            .HasOne(t => t.Streak)
            .WithOne(s => s.Task)
            .HasForeignKey<Streak>(s => s.TaskId)
            .OnDelete(DeleteBehavior.SetNull);

        modelBuilder.Entity<GameTask>()
            .Property(t => t.Difficulty)
            .HasConversion<int>();
    }
}