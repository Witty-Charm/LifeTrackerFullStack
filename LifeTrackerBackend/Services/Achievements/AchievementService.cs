using LifeTracker.Data;
using LifeTracker.Models;
using Microsoft.EntityFrameworkCore;

namespace LifeTracker.Services.Achievements;

public class AchievementService
{
    private readonly ApplicationDbContext _db;

    private static readonly AchievementDefinition[] Definitions =
    [
        new("tasks_10", "Task Starter", "Complete 10 tasks.", "TasksCompleted", 10, 10, 25),
        new("tasks_25", "Task Grinder", "Complete 25 tasks.", "TasksCompleted", 25, 20, 75),
        new("tasks_50", "Task Veteran", "Complete 50 tasks.", "TasksCompleted", 50, 30, 150),
        new("streak_3", "On Fire", "Reach a 3-day streak.", "LongestStreak", 3, 40, 25),
        new("streak_7", "Week Warrior", "Reach a 7-day streak.", "LongestStreak", 7, 50, 75),
        new("streak_30", "Monthly Legend", "Reach a 30-day streak.", "LongestStreak", 30, 60, 300),
        new("level_5", "Rising Hero", "Reach level 5.", "LevelReached", 5, 70, 50),
        new("level_10", "Seasoned Hero", "Reach level 10.", "LevelReached", 10, 80, 150),
        new("level_20", "Elite Hero", "Reach level 20.", "LevelReached", 20, 90, 400)
    ];

    public AchievementService(ApplicationDbContext db)
    {
        _db = db;
    }

    public virtual async Task<IReadOnlyList<AchievementUnlock>> EvaluateAndStageNewUnlocksAsync(int heroId, CancellationToken ct = default)
    {
        var hero = await _db.Heroes.SingleAsync(x => x.Id == heroId, ct);
        var economy = await _db.EconomyBalances.SingleOrDefaultAsync(x => x.HeroId == heroId, ct);
        if (economy == null)
        {
            economy = new EconomyBalance
            {
                HeroId = heroId,
                LastDailyResetLocalDate = DateOnly.FromDateTime(DateTime.UtcNow).ToString("yyyy-MM-dd")
            };
            _db.EconomyBalances.Add(economy);
        }

        var totalCompletions = await _db.GameTasks
            .Where(x => x.HeroId == heroId)
            .Select(x => (int?)x.CompletionCount)
            .SumAsync(ct) ?? 0;

        var longestStreak = await _db.Streaks
            .Where(x => x.HeroId == heroId)
            .Select(x => (int?)x.LongestDays)
            .MaxAsync(ct) ?? 0;

        var unlockedAt = DateTime.SpecifyKind(DateTime.UtcNow, DateTimeKind.Utc);
        var newUnlocks = new List<AchievementUnlock>();

        foreach (var definition in Definitions.OrderBy(x => x.SortOrder))
        {
            if (!IsReached(definition, totalCompletions, longestStreak, hero.Level))
            {
                continue;
            }

            var affectedRows = await TryInsertUnlockAsync(heroId, definition, unlockedAt, ct);
            if (affectedRows != 1)
            {
                continue;
            }

            hero.Gold += definition.GoldReward;
            economy.TotalGoldEarned += definition.GoldReward;
            economy.UpdatedAt = DateTimeOffset.UtcNow;

            newUnlocks.Add(new AchievementUnlock(
                definition.Key,
                definition.Title,
                definition.Description,
                definition.Category,
                definition.Threshold,
                definition.SortOrder,
                definition.GoldReward,
                true,
                unlockedAt));
        }

        return newUnlocks;
    }

    public async Task<IReadOnlyList<AchievementListItem>> GetAchievementsAsync(int heroId, CancellationToken ct = default)
    {
        var existingUnlocks = await _db.HeroAchievements
            .Where(x => x.HeroId == heroId)
            .ToDictionaryAsync(x => x.Key, ct);

        return Definitions
            .OrderBy(x => x.SortOrder)
            .Select(definition =>
            {
                var unlocked = existingUnlocks.TryGetValue(definition.Key, out var unlock);
                return new AchievementListItem(
                    definition.Key,
                    definition.Title,
                    definition.Description,
                    definition.Category,
                    definition.Threshold,
                    definition.SortOrder,
                    definition.GoldReward,
                    unlocked,
                    unlock?.UnlockedAt);
            })
            .ToList();
    }

    private static bool IsReached(AchievementDefinition definition, int totalCompletions, int longestStreak, int level) =>
        definition.Category switch
        {
            "TasksCompleted" => totalCompletions >= definition.Threshold,
            "LongestStreak" => longestStreak >= definition.Threshold,
            "LevelReached" => level >= definition.Threshold,
            _ => false
        };

    private Task<int> TryInsertUnlockAsync(int heroId, AchievementDefinition definition, DateTime unlockedAt, CancellationToken ct) =>
        _db.Database.ExecuteSqlInterpolatedAsync(
            $@"INSERT INTO ""HeroAchievements"" (""HeroId"", ""Key"", ""UnlockedAt"", ""GoldReward"")
               VALUES ({heroId}, {definition.Key}, {unlockedAt}, {definition.GoldReward})
               ON CONFLICT(""HeroId"", ""Key"") DO NOTHING;",
            ct);
}
