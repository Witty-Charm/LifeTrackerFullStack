using LifeTracker.Models;
using TimeZoneConverter;

namespace LifeTracker.Services.Time;

public class HeroTimeService : IHeroTimeService
{
    private static readonly TimeSpan TimeZoneSwitchCooldown = TimeSpan.FromHours(24);

    public string NormalizeOrDefault(string? candidateTimeZoneId, string fallback = "UTC")
    {
        if (string.IsNullOrWhiteSpace(candidateTimeZoneId))
            return fallback;

        return IsValidIana(candidateTimeZoneId) ? candidateTimeZoneId : fallback;
    }

    public bool IsValidIana(string timeZoneId)
    {
        try
        {
            _ = ResolveTimeZoneInfo(timeZoneId);
            return true;
        }
        catch
        {
            return false;
        }
    }

    public DateOnly GetLocalDate(DateTimeOffset utcNow, string timeZoneId)
    {
        var tz = ResolveTimeZoneInfo(timeZoneId);
        var local = TimeZoneInfo.ConvertTime(utcNow, tz);
        return DateOnly.FromDateTime(local.Date);
    }

    public DateTimeOffset GetNextLocalMidnightUtc(DateTimeOffset utcNow, string timeZoneId)
    {
        var tz = ResolveTimeZoneInfo(timeZoneId);
        var localDate = GetLocalDate(utcNow, timeZoneId);
        var nextLocalMidnight = localDate.AddDays(1).ToDateTime(TimeOnly.MinValue, DateTimeKind.Unspecified);
        var utc = TimeZoneInfo.ConvertTimeToUtc(nextLocalMidnight, tz);
        return new DateTimeOffset(utc, TimeSpan.Zero);
    }

    public string FormatLocalDate(DateOnly localDate) => localDate.ToString("yyyy-MM-dd");

    public DateOnly ParseLocalDate(string value) => DateOnly.ParseExact(value, "yyyy-MM-dd");

    public string ResolveEffectiveTimeZone(Hero hero, DateTimeOffset utcNow)
    {
        var current = NormalizeOrDefault(hero.TimeZoneId, "UTC");
        if (string.IsNullOrWhiteSpace(hero.PendingTimeZoneId) || string.IsNullOrWhiteSpace(hero.TimeZoneSwitchAfterLocalDate))
            return current;

        var todayInCurrentZone = GetLocalDate(utcNow, current);
        var switchAfterDate = ParseLocalDate(hero.TimeZoneSwitchAfterLocalDate);
        if (todayInCurrentZone <= switchAfterDate)
            return current;

        var pending = NormalizeOrDefault(hero.PendingTimeZoneId, current);
        hero.TimeZoneId = pending;
        hero.PendingTimeZoneId = null;
        hero.TimeZoneSwitchAfterLocalDate = null;
        hero.UpdatedAt = utcNow;
        return pending;
    }

    public bool CanSwitchTimeZone(Hero hero, DateTimeOffset utcNow)
    {
        if (!hero.LastTimeZoneChangedAt.HasValue)
            return true;

        return utcNow - hero.LastTimeZoneChangedAt.Value >= TimeZoneSwitchCooldown;
    }

    private static TimeZoneInfo ResolveTimeZoneInfo(string timeZoneId)
    {
        try
        {
            return TimeZoneInfo.FindSystemTimeZoneById(timeZoneId);
        }
        catch (TimeZoneNotFoundException) when (OperatingSystem.IsWindows())
        {
            var windows = TZConvert.IanaToWindows(timeZoneId);
            return TimeZoneInfo.FindSystemTimeZoneById(windows);
        }
    }
}
