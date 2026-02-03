namespace LifeTracker.Models;

public class Hero
{
    public int Id { get; set; }
    public string Name { get; set; } = string.Empty;
    public int Level { get; set; } = 1;
    public int XP { get; set; } = 0;
    public int MaxXP { get; set; } = 100;
    public int HP { get; set; } = 100;

    public void GainXP(int amount)
    {
        XP += amount;
        
        while (XP >= MaxXP)
        {
            XP -= MaxXP;
            Level++;
            MaxXP = Level * 100;
        }
    }
}

