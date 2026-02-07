namespace LifeTracker.Constants;

public static class GameConstants
{
    public const int BaseXp = 100;
    public const double XpExponent = 1.8;
    public const double ScaleFactor = 50.0;
    
    public const int BaseHp = 50;
    public const int HpPerLevel = 5;
    public const int MaxLevel = 999;
    
    public const double DeathHpResetPercent = 0.25;
    public const double DeathXpPenaltyPercent = 0.10;    
    public const double DeathGoldPenaltyPercent = 0.20;  
    public const double DeathStreakPenaltyPercent = 0.50; 
    public const int RecoveryDebuffHours = 4;            
    public const double RecoveryDebuffMultiplier = 0.75; 
    
    public const double StreakMultiplierCoeff = 0.15;
    public const int MaxFreezeCharges = 3;
    public const int StreakTierDays = 30;
    
    public const int DailyTaskCap = 50;
    
    public static double GetDifficultyMultiplier(TaskDifficulty difficulty) => difficulty switch
    {
        TaskDifficulty.Easy => 1.0,
        TaskDifficulty.Medium => 1.5,
        TaskDifficulty.Hard => 2.5,
        TaskDifficulty.Epic => 4.0,
        _ => 1.0
    };
    
    public static (int xp, int gold) GetHabitReward(TaskDifficulty difficulty) => difficulty switch
    {
        TaskDifficulty.Easy => (10, 5),
        TaskDifficulty.Medium => (25, 12),
        TaskDifficulty.Hard => (50, 25),
        TaskDifficulty.Epic => (100, 50),
        _ => (10, 5)
    };
    
    public static (int xp, int gold) GetOneTimeReward(TaskDifficulty difficulty) => difficulty switch
    {
        TaskDifficulty.Easy => (15, 8),
        TaskDifficulty.Medium => (35, 18),
        TaskDifficulty.Hard => (70, 35),
        TaskDifficulty.Epic => (150, 75),
        _ => (15, 8)
    };
    
    public static (int hpLoss, int goldLoss) GetHabitPenalty(TaskDifficulty difficulty) => difficulty switch
    {
        TaskDifficulty.Easy => (5, 0),
        TaskDifficulty.Medium => (10, 5),
        TaskDifficulty.Hard => (20, 15),
        TaskDifficulty.Epic => (35, 30),
        _ => (5, 0)
    };
    
    public static (int hpLoss, int goldLoss) GetOneTimePenalty(TaskDifficulty difficulty) => difficulty switch
    {
        TaskDifficulty.Easy => (3, 0),
        TaskDifficulty.Medium => (7, 5),
        TaskDifficulty.Hard => (15, 15),
        TaskDifficulty.Epic => (25, 30),
        _ => (3, 0)
    };
    
    public static (int xpPenalty, int goldPenalty, int cooldownHours) GetStreakBreakPenalty(int streakDays) => streakDays switch
    {
        <= 7 => (0, 0, 0),
        <= 30 => (50, 25, 24),
        <= 90 => (150, 75, 48),
        _ => (300, 150, 72)
    };
    
    public static long CalculateXpForLevel(int level) =>
        (long)Math.Floor(BaseXp * Math.Pow(level, XpExponent) * (1 + level / ScaleFactor));
    
    public static int CalculateMaxHp(int level) => BaseHp + (HpPerLevel * level);
    
    public static double CalculateStreakMultiplier(int streakDays) =>
        streakDays <= 0 ? 1.0 : 1.0 + Math.Log2(streakDays + 1) * StreakMultiplierCoeff;
    
    public static double CalculateLevelScaling(int heroLevel) => 1.0 + (heroLevel / 100.0);
}

public enum TaskDifficulty
{
    Easy = 1,
    Medium = 2,
    Hard = 3,
    Epic = 4
}
