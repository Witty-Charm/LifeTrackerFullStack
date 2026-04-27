using System.Globalization;
using LifeTracker.Models;
using LifeTracker.Services.Time;

namespace LifeTracker.Services;

public interface IDailyScheduleService
{
    int ParseInterval(string? repeatPattern);
    DateOnly GetStartLocalDate(GameTask task, string heroTimeZoneId);
    bool IsScheduledOn(GameTask task, DateOnly localDate, string heroTimeZoneId);
    DateOnly NextScheduledOnOrAfter(GameTask task, DateOnly fromLocalDate, string heroTimeZoneId);
    IEnumerable<DateOnly> EnumerateScheduledDays(
        GameTask task,
        DateOnly fromInclusive,
        DateOnly toInclusive,
        string heroTimeZoneId);
}

public class DailyScheduleService : IDailyScheduleService
{
    private const string DailyPrefix = "DAILY";

    private readonly IHeroTimeService _heroTimeService;

    public DailyScheduleService(IHeroTimeService heroTimeService)
    {
        _heroTimeService = heroTimeService;
    }

    public int ParseInterval(string? repeatPattern)
    {
        if (string.IsNullOrWhiteSpace(repeatPattern)) return 1;
        var parts = repeatPattern.Split(':');
        if (parts.Length < 2) return 1;
        if (!string.Equals(parts[0], DailyPrefix, StringComparison.Ordinal)) return 1;
        if (!int.TryParse(parts[1], NumberStyles.Integer, CultureInfo.InvariantCulture, out var n)) return 1;
        return n >= 1 ? n : 1;
    }

    public DateOnly GetStartLocalDate(GameTask task, string heroTimeZoneId)
    {
        var anchor = task.DueDate ?? task.CreatedAt;
        return _heroTimeService.GetLocalDate(anchor, heroTimeZoneId);
    }

    public bool IsScheduledOn(GameTask task, DateOnly localDate, string heroTimeZoneId)
    {
        if (task.Type != TaskType.Daily) return false;
        var start = GetStartLocalDate(task, heroTimeZoneId);
        if (localDate < start) return false;
        var interval = ParseInterval(task.RepeatPattern);
        return ((localDate.DayNumber - start.DayNumber) % interval) == 0;
    }

    public DateOnly NextScheduledOnOrAfter(GameTask task, DateOnly fromLocalDate, string heroTimeZoneId)
    {
        if (task.Type != TaskType.Daily) return fromLocalDate;
        var start = GetStartLocalDate(task, heroTimeZoneId);
        if (fromLocalDate <= start) return start;
        var interval = ParseInterval(task.RepeatPattern);
        var diff = fromLocalDate.DayNumber - start.DayNumber;
        var remainder = diff % interval;
        if (remainder == 0) return fromLocalDate;
        return fromLocalDate.AddDays(interval - remainder);
    }

    public IEnumerable<DateOnly> EnumerateScheduledDays(
        GameTask task,
        DateOnly fromInclusive,
        DateOnly toInclusive,
        string heroTimeZoneId)
    {
        if (task.Type != TaskType.Daily) yield break;
        if (fromInclusive > toInclusive) yield break;

        var interval = ParseInterval(task.RepeatPattern);
        var first = NextScheduledOnOrAfter(task, fromInclusive, heroTimeZoneId);
        for (var d = first; d <= toInclusive; d = d.AddDays(interval))
        {
            yield return d;
        }
    }
}
