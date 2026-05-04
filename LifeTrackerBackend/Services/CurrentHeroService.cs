using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using LifeTracker.Data;
using LifeTracker.Infrastructure;
using LifeTracker.Models;
using Microsoft.AspNetCore.Http;
using Microsoft.EntityFrameworkCore;

namespace LifeTracker.Services;

public interface ICurrentHeroService
{
    bool TryGetCurrentUserId(HttpContext httpContext, out int userId, out string errorMessage);

    bool TryGetCurrentDeviceId(HttpContext httpContext, out string deviceId, out string errorMessage);

    Task<Hero?> GetCurrentHeroAsync(
        HttpContext httpContext,
        Func<IQueryable<Hero>, IQueryable<Hero>>? configureQuery = null,
        CancellationToken cancellationToken = default);

    Task<Hero?> GetOwnedHeroAsync(
        HttpContext httpContext,
        int heroId,
        Func<IQueryable<Hero>, IQueryable<Hero>>? configureQuery = null,
        CancellationToken cancellationToken = default);
}

public sealed class CurrentHeroService : ICurrentHeroService
{
    private readonly ApplicationDbContext _db;

    public CurrentHeroService(ApplicationDbContext db)
    {
        _db = db;
    }

    public bool TryGetCurrentUserId(HttpContext httpContext, out int userId, out string errorMessage)
    {
        userId = 0;
        var principal = httpContext.User;
        if (principal?.Identity?.IsAuthenticated != true)
        {
            errorMessage = "Authentication is required.";
            return false;
        }

        var raw = principal.FindFirst(JwtRegisteredClaimNames.Sub)?.Value
                  ?? principal.FindFirst(ClaimTypes.NameIdentifier)?.Value;

        if (string.IsNullOrWhiteSpace(raw) || !int.TryParse(raw, out userId) || userId <= 0)
        {
            errorMessage = "Authenticated principal is missing a valid user id.";
            return false;
        }

        errorMessage = string.Empty;
        return true;
    }

    public bool TryGetCurrentDeviceId(HttpContext httpContext, out string deviceId, out string errorMessage)
    {
        var resolved = DeviceRequestContext.TryResolveCurrentDeviceId(httpContext, out deviceId, out errorMessage);
        if (resolved)
            DeviceRequestContext.StoreCurrentDeviceId(httpContext, deviceId);

        return resolved;
    }

    public Task<Hero?> GetCurrentHeroAsync(
        HttpContext httpContext,
        Func<IQueryable<Hero>, IQueryable<Hero>>? configureQuery = null,
        CancellationToken cancellationToken = default)
    {
        if (!TryGetCurrentUserId(httpContext, out var userId, out _))
            return Task.FromResult<Hero?>(null);

        var query = _db.Heroes.Where(hero => hero.UserId == userId);
        if (configureQuery is not null)
            query = configureQuery(query);

        return query.FirstOrDefaultAsync(cancellationToken);
    }

    public Task<Hero?> GetOwnedHeroAsync(
        HttpContext httpContext,
        int heroId,
        Func<IQueryable<Hero>, IQueryable<Hero>>? configureQuery = null,
        CancellationToken cancellationToken = default)
    {
        if (!TryGetCurrentUserId(httpContext, out var userId, out _))
            return Task.FromResult<Hero?>(null);

        var query = _db.Heroes.Where(hero => hero.UserId == userId && hero.Id == heroId);
        if (configureQuery is not null)
            query = configureQuery(query);

        return query.FirstOrDefaultAsync(cancellationToken);
    }
}
