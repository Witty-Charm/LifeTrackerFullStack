namespace LifeTracker.Models;

public class Hero
{
    public int Id { get; set; }
    public string Name { get; set; } = string.Empty;
    public int Level { get; set; } = 1;
    public long CurrentXp { get; set; } = 0;
    public long TotalXpEarned { get; set; } = 0;
    
    public int CurrentHp { get; set; } = 100;
    public int MaxHp { get; set; } = 100;
    public int Gold { get; set; } = 0;
    
    public bool IsDead { get; set; } = false;
    public DateTime? DeathTime { get; set; }
    public int DeathCount { get; set; } = 0;
    
    public DateTime CreatedDate { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
    
    public ICollection<GameTask> Tasks { get; set; } = new List<GameTask>();
    public ICollection<Streak> Streaks { get; set; } = new List<Streak>();
    public EconomyBalance?  EconomyBalance { get; set; }

    public void GainXP(long amount)
    {
        if (IsDead) return;
        
        CurrentXp += amount;
        TotalXpEarned += amount;
        UpdatedAt = DateTime.UtcNow;
        
        while (CurrentXp >= GetXpRequiredForNextLevel())
        {
            CurrentXp -= GetXpRequiredForNextLevel();
            Level++;
            
            MaxHp = 50 + (Level * 5);
            CurrentHp = MaxHp;
        }
    }
    
    public void TakeDamage(int damage)
    {
        if (IsDead) return;
        
        CurrentHp -= damage;
        UpdatedAt = DateTime.UtcNow;
        
        if (CurrentHp <= 0)
        {
            Die();
        }
    }
    
    public long GetXpRequiredForNextLevel()
    {   
        const double baseXp = 100;
        const double exponent = 1.8;
        const double scaleFactor = 50.0;

        return (long)Math.Floor(baseXp * Math.Pow(Level, exponent) * (1 + Level / scaleFactor));
    }

    public void Die()
    {
        IsDead = true;
        DeathTime = DateTime.UtcNow;
        DeathCount++;
        
        
        CurrentHp = MaxHp / 4;

        long xpLoss = (long)(GetXpRequiredForNextLevel() * 0.10);
        CurrentXp = Math.Max(0, CurrentXp - xpLoss);

        int goldLoss = (int)(Gold * 0.20);
        Gold = Math.Max(0, Gold - goldLoss);
        
        IsDead = false;
        UpdatedAt = DateTime.UtcNow;

    }
}

