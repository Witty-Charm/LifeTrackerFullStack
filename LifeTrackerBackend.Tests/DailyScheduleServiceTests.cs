using LifeTracker.Models;
using LifeTracker.Services;
using LifeTracker.Services.Time;

namespace LifeTrackerBackend.Tests;

public class DailyScheduleServiceTests
{
    private readonly DailyScheduleService _svc = new(new HeroTimeService());

    [Theory]
    [InlineData(null, 1)]
    [InlineData("", 1)]
    [InlineData("   ", 1)]
    [InlineData("DAILY", 1)]            // missing colon → fallback
    [InlineData("DAILY:", 1)]           // empty interval → fallback
    [InlineData("DAILY:abc", 1)]
    [InlineData("DAILY:0", 1)]
    [InlineData("DAILY:-1", 1)]
    [InlineData("DAILY:1", 1)]
    [InlineData("DAILY:5", 5)]
    [InlineData("DAILY:30", 30)]
    [InlineData("WEEKLY:2", 1)]         // non-DAILY frequency → fallback
    [InlineData("daily:5", 1)]          // case-sensitive, falls back
    [InlineData("RESET:DAILY", 1)]
    public void ParseInterval_ReturnsExpected(string? pattern, int expected)
    {
        Assert.Equal(expected, _svc.ParseInterval(pattern));
    }

    [Fact]
    public void IsScheduledOn_NonDaily_AlwaysFalse()
    {
        var task = new GameTask { Type = TaskType.OneTime, RepeatPattern = "DAILY:1" };
        Assert.False(_svc.IsScheduledOn(task, DateOnly.FromDateTime(DateTime.UtcNow), "UTC"));
    }

    [Fact]
    public void IsScheduledOn_BeforeStart_False()
    {
        var start = new DateOnly(2026, 4, 24);
        var task = MakeDaily("DAILY:1", start);
        Assert.False(_svc.IsScheduledOn(task, start.AddDays(-1), "UTC"));
    }

    [Fact]
    public void IsScheduledOn_Interval1_AlwaysTrueOnOrAfterStart()
    {
        var start = new DateOnly(2026, 4, 24);
        var task = MakeDaily("DAILY:1", start);

        Assert.True(_svc.IsScheduledOn(task, start, "UTC"));
        Assert.True(_svc.IsScheduledOn(task, start.AddDays(1), "UTC"));
        Assert.True(_svc.IsScheduledOn(task, start.AddDays(7), "UTC"));
    }

    [Fact]
    public void IsScheduledOn_Interval3_OnlyMultiples()
    {
        var start = new DateOnly(2026, 4, 24);
        var task = MakeDaily("DAILY:3", start);

        Assert.True(_svc.IsScheduledOn(task, start, "UTC"));
        Assert.False(_svc.IsScheduledOn(task, start.AddDays(1), "UTC"));
        Assert.False(_svc.IsScheduledOn(task, start.AddDays(2), "UTC"));
        Assert.True(_svc.IsScheduledOn(task, start.AddDays(3), "UTC"));
        Assert.True(_svc.IsScheduledOn(task, start.AddDays(6), "UTC"));
        Assert.False(_svc.IsScheduledOn(task, start.AddDays(7), "UTC"));
    }

    [Fact]
    public void NextScheduledOnOrAfter_BeforeStart_ReturnsStart()
    {
        var start = new DateOnly(2026, 4, 24);
        var task = MakeDaily("DAILY:3", start);
        Assert.Equal(start, _svc.NextScheduledOnOrAfter(task, start.AddDays(-5), "UTC"));
    }

    [Fact]
    public void NextScheduledOnOrAfter_OnScheduledDay_ReturnsSameDay()
    {
        var start = new DateOnly(2026, 4, 24);
        var task = MakeDaily("DAILY:3", start);
        Assert.Equal(start.AddDays(3), _svc.NextScheduledOnOrAfter(task, start.AddDays(3), "UTC"));
    }

    [Fact]
    public void NextScheduledOnOrAfter_BetweenScheduledDays_ReturnsNext()
    {
        var start = new DateOnly(2026, 4, 24);
        var task = MakeDaily("DAILY:3", start);
        Assert.Equal(start.AddDays(3), _svc.NextScheduledOnOrAfter(task, start.AddDays(1), "UTC"));
        Assert.Equal(start.AddDays(3), _svc.NextScheduledOnOrAfter(task, start.AddDays(2), "UTC"));
        Assert.Equal(start.AddDays(6), _svc.NextScheduledOnOrAfter(task, start.AddDays(4), "UTC"));
    }

    [Fact]
    public void EnumerateScheduledDays_Interval1_YieldsEveryDay()
    {
        var start = new DateOnly(2026, 4, 24);
        var task = MakeDaily("DAILY:1", start);
        var days = _svc.EnumerateScheduledDays(task, start, start.AddDays(4), "UTC").ToList();
        Assert.Equal(5, days.Count);
        Assert.Equal(start, days[0]);
        Assert.Equal(start.AddDays(4), days[^1]);
    }

    [Fact]
    public void EnumerateScheduledDays_Interval3_YieldsOnlyMultiples()
    {
        var start = new DateOnly(2026, 4, 24);
        var task = MakeDaily("DAILY:3", start);
        var days = _svc.EnumerateScheduledDays(task, start, start.AddDays(10), "UTC").ToList();
        Assert.Equal(new[] { start, start.AddDays(3), start.AddDays(6), start.AddDays(9) }, days);
    }

    [Fact]
    public void EnumerateScheduledDays_EmptyRange_YieldsNothing()
    {
        var start = new DateOnly(2026, 4, 24);
        var task = MakeDaily("DAILY:1", start);
        Assert.Empty(_svc.EnumerateScheduledDays(task, start.AddDays(5), start.AddDays(3), "UTC"));
    }

    [Fact]
    public void GetStartLocalDate_PrefersDueDateOverCreatedAt()
    {
        var task = new GameTask
        {
            Type = TaskType.Daily,
            CreatedAt = new DateTimeOffset(2026, 1, 1, 0, 0, 0, TimeSpan.Zero),
            DueDate = new DateTimeOffset(2026, 4, 24, 0, 0, 0, TimeSpan.Zero),
        };
        Assert.Equal(new DateOnly(2026, 4, 24), _svc.GetStartLocalDate(task, "UTC"));
    }

    [Fact]
    public void GetStartLocalDate_FallsBackToCreatedAtWhenDueDateNull()
    {
        var task = new GameTask
        {
            Type = TaskType.Daily,
            CreatedAt = new DateTimeOffset(2026, 4, 24, 0, 0, 0, TimeSpan.Zero),
            DueDate = null,
        };
        Assert.Equal(new DateOnly(2026, 4, 24), _svc.GetStartLocalDate(task, "UTC"));
    }

    private static GameTask MakeDaily(string pattern, DateOnly startLocal) => new()
    {
        Type = TaskType.Daily,
        RepeatPattern = pattern,
        DueDate = new DateTimeOffset(startLocal.ToDateTime(TimeOnly.MinValue, DateTimeKind.Unspecified), TimeSpan.Zero),
        CreatedAt = new DateTimeOffset(startLocal.ToDateTime(TimeOnly.MinValue, DateTimeKind.Unspecified), TimeSpan.Zero),
    };
}
