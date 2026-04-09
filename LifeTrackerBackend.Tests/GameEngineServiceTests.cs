using System.Threading.Tasks;
using LifeTracker.Constants;
using LifeTracker.Models;
using LifeTracker.Services;
using Xunit;

namespace LifeTrackerBackend.Tests;

public class GameEngineServiceTests
{
    private readonly GameEngineService _svc = new();

    [Fact]
    public void CalculateFinalXpReward_AppliesHeroBoostMultiplicatively()
    {
        var task = new GameTask { Difficulty = TaskDifficulty.Easy, Type = TaskType.OneTime };
        var hero = new Hero { Level = 1, XpBoostPercent = 25, XpBoostTasksRemaining = 3 };
        var streak = new Streak { CurrentDays = 0 };
        var economy = new EconomyBalance { XpMultiplier = 2.0m };

        var xp = _svc.CalculateFinalXpReward(task, hero, streak, economy);

        // base easy one-time XP from GameConstants: assume >0, so ratio matters
        var baseOnlyHero = new Hero { Level = 1 };
        var xpBase = _svc.CalculateFinalXpReward(task, baseOnlyHero, streak, new EconomyBalance());

        // Expect multiplier = levelScaling*recovery(1)*streak(1)*economy(2.0)*heroBoost(1.25)
        var ratio = (double)xp / xpBase;
        Assert.InRange(ratio, 2.4, 2.6); // approximately 2 * 1.25 = 2.5, allow rounding
    }

    [Fact]
    public void ApplyTaskCompletion_DecrementsBoostTasksAndClearsWhenZero()
    {
        var task = new GameTask { Difficulty = TaskDifficulty.Easy, Type = TaskType.OneTime };
        var hero = new Hero { Level = 1, XpBoostPercent = 25, XpBoostTasksRemaining = 1 };
        var streak = new Streak { CurrentDays = 0 };
        var economy = new EconomyBalance();

        _svc.ApplyTaskCompletion(task, hero, streak, economy);

        Assert.Equal(0, hero.XpBoostTasksRemaining);
        Assert.Equal(0, hero.XpBoostPercent);
    }
}
