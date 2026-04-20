namespace LifeTracker.Services.Achievements;

public sealed record AchievementDefinition(
    string Key,
    string Title,
    string Description,
    string Category,
    int Threshold,
    int SortOrder,
    int GoldReward);

public sealed record AchievementUnlock(
    string Key,
    string Title,
    string Description,
    string Category,
    int Threshold,
    int SortOrder,
    int GoldReward,
    bool Unlocked,
    DateTime UnlockedAt);

public sealed record AchievementListItem(
    string Key,
    string Title,
    string Description,
    string Category,
    int Threshold,
    int SortOrder,
    int GoldReward,
    bool Unlocked,
    DateTime? UnlockedAt);
