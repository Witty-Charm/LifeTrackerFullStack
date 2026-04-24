using LifeTracker.Constants;
using LifeTracker.Models;

namespace LifeTracker.Services;

public class GameEngineService
{
    public long CalculateFinalXpReward(GameTask task, Hero hero, Streak? streak, EconomyBalance economy)
    {
        int baseXp = task.GetBaseRewardXP();
        double difficultyMult = GameConstants.GetDifficultyMultiplier(task.Difficulty);
        double streakMult = streak?.GetStreakMultiplier() ?? 1.0;
        double levelScaling = GameConstants.CalculateLevelScaling(hero.Level);
        double recoveryMult = hero.GetRecoveryMultiplier();
        double economyMult = (double)economy.GetFinalXpMultiplier();

        double heroBoostMult = 1.0;
        if (hero.XpBoostTasksRemaining > 0 && hero.XpBoostPercent > 0)
            heroBoostMult += hero.XpBoostPercent / 100.0;

        double finalXp = baseXp * difficultyMult * streakMult * levelScaling * recoveryMult * economyMult * heroBoostMult;

        return (long)Math.Floor(finalXp);
    }

    public int CalculateFinalGoldReward(GameTask task, Hero hero, EconomyBalance economy)
    {
        int baseGold = task.GetGoldReward();
        double recoveryMult = hero.GetRecoveryMultiplier();
        double economyMult = (double)economy.GoldMultiplier;

        double finalGold = baseGold * recoveryMult * economyMult;

        return (int)Math.Floor(finalGold);
    }


    public (long xpGained, int goldGained, bool leveledUp, int streakBonusPercent) ApplyTaskCompletion(
        GameTask task,
        Hero hero,
        Streak? streak,
        EconomyBalance economy,
        DateOnly todayLocalDate)
    {
        int oldLevel = hero.Level;
        int streakBonusPercent = streak?.GetBonusXpPercent() ?? 0;

        long xpReward = CalculateFinalXpReward(task, hero, streak, economy);
        int goldReward = CalculateFinalGoldReward(task, hero, economy);

        hero.GainXP(xpReward);
        hero.Gold += goldReward;

        if (hero.XpBoostTasksRemaining > 0)
        {
            hero.XpBoostTasksRemaining--;
            if (hero.XpBoostTasksRemaining <= 0)
            {
                hero.XpBoostTasksRemaining = 0;
                hero.XpBoostPercent = 0;
            }
        }

        hero.UpdatedAt = DateTime.UtcNow;

        economy.TotalXpEarned += xpReward;
        economy.TotalGoldEarned += goldReward;
        economy.IncrementCompletion(todayLocalDate);
        economy.UpdatedAt = DateTime.UtcNow;

        task.IsCompleted = task.Type == TaskType.OneTime;
        task.CompletionCount++;
        task.LastCompletedAt = DateTime.UtcNow;
        task.UpdatedAt = DateTime.UtcNow;

        bool leveledUp = hero.Level > oldLevel;

        return (xpReward, goldReward, leveledUp, streakBonusPercent);
    }

    public TaskFailureResult ApplyTaskFailure(
            GameTask task,
            Hero hero,
            Streak? streak,
            EconomyBalance economy,
            DateOnly? todayLocalDate = null,
            ShieldConsumptionContext? shieldContext = null)
    {
        int hpPenalty = task.GetHpPenalty();
        int goldPenalty = task.GetGoldPenalty();

        hero.Gold = Math.Max(0, hero.Gold - goldPenalty);
        hero.TakeDamage(hpPenalty);

        task.FailCount++;
        task.UpdatedAt = DateTime.UtcNow;

        bool streakBroken = false;
        bool shieldAbsorbed = false;
        StreakBreakPenalty? streakPenalty = null;

        var canBreakStreak = streak != null && streak.CurrentDays > 0 && !streak.IsFrozen();
        var shouldAbsorbBreak = canBreakStreak && hero.IsShieldActive;

        if (shouldAbsorbBreak)
        {
            shieldAbsorbed = true;
            if (shieldContext != null)
            {
                shieldContext.AbsorbedAnyBreak = true;
            }
            else
            {
                hero.IsShieldActive = false;
                hero.ShieldActivatedAtUtc = null;
            }
        }
        else if (canBreakStreak)
        {
            var (xpPenalty, goldPenaltyFromStreak, cooldownHours) =
                GameConstants.GetStreakBreakPenalty(streak!.CurrentDays);

            if (xpPenalty > 0)
                hero.CurrentXp = Math.Max(0, hero.CurrentXp - xpPenalty);

            if (goldPenaltyFromStreak > 0)
                hero.Gold = Math.Max(0, hero.Gold - goldPenaltyFromStreak);

            streakPenalty = new StreakBreakPenalty
            {
                StreakDays = streak.CurrentDays,
                XpLost = xpPenalty,
                GoldLost = goldPenaltyFromStreak,
                CooldownHours = cooldownHours
            };

            streak.Break(todayLocalDate);
            streakBroken = true;
        }

        if (hero.IsDead)
        {
            economy.ActivateDeathPenalty();

            if (streak != null && streak.CurrentDays > 0)
            {
                int reduction = (int)(streak.CurrentDays * GameConstants.DeathStreakPenaltyPercent);
                streak.CurrentDays = Math.Max(0, streak.CurrentDays - reduction);
                streak.UpdatedAt = DateTime.UtcNow;
            }
        }

        return new TaskFailureResult(hpPenalty, goldPenalty, hero.IsDead, streakBroken, shieldAbsorbed, streakPenalty);
    }


    public void CheckOverdueTasks(List<GameTask> tasks, Hero hero, List<Streak> streaks, EconomyBalance economy)
    {
        var shieldContext = new ShieldConsumptionContext();

        foreach (var task in tasks.Where(t => t.IsActive && t.IsOverdue()))
        {
            var streak = streaks.FirstOrDefault(s => s.TaskId == task.Id);
            ApplyTaskFailure(task, hero, streak, economy, shieldContext: shieldContext);
        }

        if (shieldContext.AbsorbedAnyBreak)
        {
            hero.IsShieldActive = false;
            hero.ShieldActivatedAtUtc = null;
        }
    }
}

public sealed record TaskFailureResult(
    int HpLost,
    int GoldLost,
    bool HeroDied,
    bool StreakBroken,
    bool ShieldAbsorbed,
    StreakBreakPenalty? Penalty
);

public sealed class ShieldConsumptionContext
{
    public bool AbsorbedAnyBreak { get; set; }
}

public class StreakBreakPenalty
{
    public int StreakDays { get; set; }
    public int XpLost { get; set; }
    public int GoldLost { get; set; }
    public int CooldownHours { get; set; }
}