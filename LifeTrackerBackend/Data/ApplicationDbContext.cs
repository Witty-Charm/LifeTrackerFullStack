using Microsoft.EntityFrameworkCore;
using LifeTracker.Models;

namespace LifeTracker.Data;

public class ApplicationDbContext : DbContext
{
    public ApplicationDbContext(DbContextOptions<ApplicationDbContext> options)
        : base(options)
    {
    }

    public override int SaveChanges()
    {
        StampConcurrencyTokens();
        return base.SaveChanges();
    }

    public override Task<int> SaveChangesAsync(CancellationToken cancellationToken = default)
    {
        StampConcurrencyTokens();
        return base.SaveChangesAsync(cancellationToken);
    }

    public DbSet<Hero> Heroes { get; set; }
    public DbSet<GameTask> GameTasks { get; set; }
    public DbSet<Streak> Streaks { get; set; }
    public DbSet<EconomyBalance> EconomyBalances { get; set; }
    public DbSet<DailyTaskCompletion> DailyTaskCompletions { get; set; }
    public DbSet<ShopItem> ShopItems { get; set; }
    public DbSet<Purchase> Purchases { get; set; }
    public DbSet<HeroAchievement> HeroAchievements { get; set; }

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

        modelBuilder.Entity<Hero>()
            .HasMany(h => h.DailyTaskCompletions)
            .WithOne(c => c.Hero)
            .HasForeignKey(c => c.HeroId)
            .OnDelete(DeleteBehavior.Cascade);

        modelBuilder.Entity<GameTask>()
            .HasOne(t => t.Streak)
            .WithOne(s => s.Task)
            .HasForeignKey<Streak>(s => s.TaskId)
            .OnDelete(DeleteBehavior.SetNull);

        modelBuilder.Entity<GameTask>()
            .HasMany(t => t.DailyTaskCompletions)
            .WithOne(c => c.Task)
            .HasForeignKey(c => c.TaskId)
            .OnDelete(DeleteBehavior.Cascade);

        modelBuilder.Entity<GameTask>()
            .Property(t => t.Difficulty)
            .HasConversion<int>();

        modelBuilder.Entity<GameTask>()
            .Property(t => t.Polarity)
            .HasConversion<int>()
            .HasDefaultValue(HabitPolarity.Both);

        modelBuilder.Entity<Hero>()
            .Property(h => h.OwnerDeviceId)
            .IsRequired();

        modelBuilder.Entity<Hero>()
            .HasIndex(h => h.OwnerDeviceId);

        modelBuilder.Entity<Hero>()
            .Property(h => h.TimeZoneId)
            .HasDefaultValue("UTC");

        modelBuilder.Entity<Hero>()
            .Property(h => h.IsShieldActive)
            .HasDefaultValue(false);

        modelBuilder.Entity<Hero>()
            .Property(h => h.RowVersion)
            .IsConcurrencyToken();

        modelBuilder.Entity<Purchase>()
            .HasOne(p => p.Hero)
            .WithMany()
            .HasForeignKey(p => p.HeroId)
            .OnDelete(DeleteBehavior.Cascade);

        modelBuilder.Entity<Purchase>()
            .HasOne(p => p.ShopItem)
            .WithMany()
            .HasForeignKey(p => p.ShopItemId)
            .OnDelete(DeleteBehavior.Restrict);

        modelBuilder.Entity<Purchase>()
            .HasIndex(p => p.HeroId);

        modelBuilder.Entity<HeroAchievement>()
            .HasIndex(a => new { a.HeroId, a.Key })
            .IsUnique();

        modelBuilder.Entity<HeroAchievement>()
            .Property(a => a.UnlockedAt)
            .HasConversion(
                value => value,
                value => DateTime.SpecifyKind(value, DateTimeKind.Utc));

        modelBuilder.Entity<DailyTaskCompletion>()
            .HasIndex(c => new { c.HeroId, c.TaskId, c.LocalDate })
            .IsUnique();

        modelBuilder.Entity<DailyTaskCompletion>()
            .Property(c => c.RowVersion)
            .IsConcurrencyToken();

        modelBuilder.Entity<Streak>()
            .Property(s => s.RowVersion)
            .IsConcurrencyToken();

        modelBuilder.Entity<EconomyBalance>()
            .Property(e => e.RowVersion)
            .IsConcurrencyToken();
    }

    private void StampConcurrencyTokens()
    {
        foreach (var entry in ChangeTracker.Entries<Hero>()
                     .Where(e => e.State is EntityState.Added or EntityState.Modified))
        {
            entry.Entity.RowVersion = Guid.NewGuid().ToByteArray();
        }

        foreach (var entry in ChangeTracker.Entries<Streak>()
                     .Where(e => e.State is EntityState.Added or EntityState.Modified))
        {
            entry.Entity.RowVersion = Guid.NewGuid().ToByteArray();
        }

        foreach (var entry in ChangeTracker.Entries<EconomyBalance>()
                     .Where(e => e.State is EntityState.Added or EntityState.Modified))
        {
            entry.Entity.RowVersion = Guid.NewGuid().ToByteArray();
        }

        foreach (var entry in ChangeTracker.Entries<DailyTaskCompletion>()
                     .Where(e => e.State is EntityState.Added or EntityState.Modified))
        {
            entry.Entity.RowVersion = Guid.NewGuid().ToByteArray();
        }
    }
}
