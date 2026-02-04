namespace LifeTracker.Models;

public class Hero
{
    public int Id { get; set; }
    public string Name { get; set; } = string.Empty;
    public int Level { get; set; } = 1;
    public int XP { get; set; } = 0;
    public int MaxXP { get; set; } = 100;
    public int HP { get; set; } = 100;
    public int maxHP { get; set; } = 100;
    public int Gold { get; set; } = 0;

    public void GainXP(int amount)
    {
        XP += amount;
        while (XP >= MaxXP)
        {
            XP -= MaxXP;
            Level++;
            MaxXP = Level * 100;
            HP = maxHP;
        }
    }

    public void TakeDamage(int damage)
    {
        HP -= damage;
        if (HP <= 0)
        {
            HP = maxHP / 2;
            XP = 0;
            Gold = Math.Max(0, Gold - 10);
        }
    }
}

