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
        economy.IncrementDailyCompletion(todayLocalDate);
        economy.UpdatedAt = DateTime.UtcNow;

        task.IsCompleted = task.Type == TaskType.OneTime;
        task.CompletionCount++;
        task.LastCompletedAt = DateTime.UtcNow;
        task.UpdatedAt = DateTime.UtcNow;

        bool leveledUp = hero.Level > oldLevel;

        return (xpReward, goldReward, leveledUp, streakBonusPercent);
    }

    private static bool ShieldActiveNow(Streak streak) =>
        streak.IsShieldActive &&
        streak.ShieldExpiresAtUtc.HasValue &&
        DateTimeOffset.UtcNow <= streak.ShieldExpiresAtUtc.Value;

    public (int hpLost, int goldLost, bool heroDied, bool streakBroken, StreakBreakPenalty? penalty)
        ApplyTaskFailure(
            GameTask task,
            Hero hero,
            Streak? streak,
            EconomyBalance economy,
            DateOnly? todayLocalDate = null)
    {
        int hpPenalty = task.GetHpPenalty();
        int goldPenalty = task.GetGoldPenalty();

        if (streak != null && streak.IsShieldActive &&
            (!streak.ShieldExpiresAtUtc.HasValue || DateTimeOffset.UtcNow > streak.ShieldExpiresAtUtc))
        {
            streak.IsShieldActive = false;
            streak.ShieldFailConsumed = false;
            streak.ShieldExpiresAtUtc = null;
            streak.ShieldBackupCurrentDays = null;
            streak.ShieldBackupBreakAtUtc = null;
        }

        hero.Gold = Math.Max(0, hero.Gold - goldPenalty);
        hero.TakeDamage(hpPenalty);

        task.FailCount++;
        task.UpdatedAt = DateTime.UtcNow;

        if (streak != null && ShieldActiveNow(streak) && !streak.ShieldFailConsumed)
        {
            streak.ShieldFailConsumed = true;
            return (hpPenalty, goldPenalty, hero.IsDead, false, null);
        }

        bool streakBroken = false;
        StreakBreakPenalty? streakPenalty = null;

        if (streak != null && streak.CurrentDays > 0 && !streak.IsFrozen() && !streak.IsShieldActive)
        {
            var (xpPenalty, goldPenaltyFromStreak, cooldownHours) =
                GameConstants.GetStreakBreakPenalty(streak.CurrentDays);

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

            streak.ShieldBackupCurrentDays = streak.CurrentDays;
            streak.ShieldBackupBreakAtUtc = DateTimeOffset.UtcNow;

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

        return (hpPenalty, goldPenalty, hero.IsDead, streakBroken, streakPenalty);
    }


    public void CheckOverdueTasks(List<GameTask> tasks, Hero hero, List<Streak> streaks, EconomyBalance economy)
    {
        foreach (var task in tasks.Where(t => t.IsActive && t.IsOverdue()))
        {
            var streak = streaks.FirstOrDefault(s => s.TaskId == task.Id);
            ApplyTaskFailure(task, hero, streak, economy);
        }
    }
}

public class StreakBreakPenalty
{
    public int StreakDays { get; set; }
    public int XpLost { get; set; }
    public int GoldLost { get; set; }
    public int CooldownHours { get; set; }
}