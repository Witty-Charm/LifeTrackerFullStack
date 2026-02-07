using LifeTracker.Constants;
using LifeTracker.Models;

namespace LifeTracker.Services;

public class GameEngineService
{
    public long CalculateFinalXpReward(GameTask task, Hero hero, Streak? streak)
    {
        int baseXp = task.GetBaseRewardXP();
        double difficultyMult = GameConstants.GetDifficultyMultiplier(task.Difficulty);
        double streakMult = streak?.GetStreakMultiplier() ?? 1.0;
        double levelScaling = GameConstants.CalculateLevelScaling(hero.Level);
        double recoveryMult = hero.GetRecoveryMultiplier();
        
        double finalXp = baseXp * difficultyMult * streakMult * levelScaling * recoveryMult;
        return (long)Math.Floor(finalXp);
    }
    
    public int CalculateFinalGoldReward(GameTask task, Hero hero)
    {
        int baseGold = task.GetGoldReward();
        double recoveryMult = hero.GetRecoveryMultiplier();
        return (int)Math.Floor(baseGold * recoveryMult);
    }
    
    public (long xpGained, int goldGained) ApplyTaskCompletion(
        GameTask task, 
        Hero hero, 
        Streak? streak,
        EconomyBalance economy)
    {
        long xpReward = CalculateFinalXpReward(task, hero, streak);
        int goldReward = CalculateFinalGoldReward(task, hero);

        hero.GainXP(xpReward);
        hero.Gold += goldReward;
        hero.UpdatedAt = DateTime.UtcNow;

        economy.TotalXpEarned += xpReward;
        economy.TotalGoldEarned += goldReward;
        economy.IncrementDailyCompletion();
        economy.UpdatedAt = DateTime.UtcNow;

        task.IsCompleted = task.Type == TaskType.OneTime;
        task.CompletionCount++;
        task.LastCompletedAt = DateTime.UtcNow;
        task.UpdatedAt = DateTime.UtcNow;

        return (xpReward, goldReward);
    }
    
    public (int hpLost, int goldLost, bool heroDied) ApplyTaskFailure(
        GameTask task,
        Hero hero)
    {
        int hpPenalty = task.GetHpPenalty();
        int goldPenalty = task.GetGoldPenalty();

        hero.Gold = Math.Max(0, hero.Gold - goldPenalty);
        hero.TakeDamage(hpPenalty);

        task.FailCount++;
        task.UpdatedAt = DateTime.UtcNow;

        return (hpPenalty, goldPenalty, hero.IsDead);
    }
}
