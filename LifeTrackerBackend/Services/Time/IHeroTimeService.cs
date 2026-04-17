using LifeTracker.Models;

namespace LifeTracker.Services.Time;

public interface IHeroTimeService
{
    string NormalizeOrDefault(string? candidateTimeZoneId, string fallback = "UTC");
    bool IsValidIana(string timeZoneId);
    DateOnly GetLocalDate(DateTimeOffset utcNow, string timeZoneId);
    DateTimeOffset GetNextLocalMidnightUtc(DateTimeOffset utcNow, string timeZoneId);
    string FormatLocalDate(DateOnly localDate);
    DateOnly ParseLocalDate(string value);
    string ResolveEffectiveTimeZone(Hero hero, DateTimeOffset utcNow);
    bool CanSwitchTimeZone(Hero hero, DateTimeOffset utcNow);
}
