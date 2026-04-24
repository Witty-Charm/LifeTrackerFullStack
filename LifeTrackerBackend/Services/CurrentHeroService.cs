using LifeTracker.Data;
using LifeTracker.Infrastructure;
using LifeTracker.Models;
using Microsoft.AspNetCore.Http;
using Microsoft.EntityFrameworkCore;

namespace LifeTracker.Services;

public interface ICurrentHeroService
{
    bool TryGetCurrentDeviceId(HttpContext httpContext, out string deviceId, out string errorMessage);
    Task<Hero?> GetCurrentHeroAsync(HttpContext httpContext, Func<IQueryable<Hero>, IQueryable<Hero>>? configureQuery = null, CancellationToken cancellationToken = default);
    Task<Hero?> GetOwnedHeroAsync(HttpContext httpContext, int heroId, Func<IQueryable<Hero>, IQueryable<Hero>>? configureQuery = null, CancellationToken cancellationToken = default);
}

public sealed class CurrentHeroService : ICurrentHeroService
{
    private readonly ApplicationDbContext _db;

    public CurrentHeroService(ApplicationDbContext db)
    {
        _db = db;
    }

    public bool TryGetCurrentDeviceId(HttpContext httpContext, out string deviceId, out string errorMessage)
    {
        var resolved = DeviceRequestContext.TryResolveCurrentDeviceId(httpContext, out deviceId, out errorMessage);
        if (resolved)
        {
            DeviceRequestContext.StoreCurrentDeviceId(httpContext, deviceId);
        }

        return resolved;
    }

    public Task<Hero?> GetCurrentHeroAsync(HttpContext httpContext, Func<IQueryable<Hero>, IQueryable<Hero>>? configureQuery = null, CancellationToken cancellationToken = default)
    {
        if (!TryGetCurrentDeviceId(httpContext, out var deviceId, out _))
            return Task.FromResult<Hero?>(null);

        var query = _db.Heroes.Where(hero => hero.OwnerDeviceId == deviceId);
        if (configureQuery is not null)
            query = configureQuery(query);

        return query.FirstOrDefaultAsync(cancellationToken);
    }

    public Task<Hero?> GetOwnedHeroAsync(HttpContext httpContext, int heroId, Func<IQueryable<Hero>, IQueryable<Hero>>? configureQuery = null, CancellationToken cancellationToken = default)
    {
        if (!TryGetCurrentDeviceId(httpContext, out var deviceId, out _))
            return Task.FromResult<Hero?>(null);

        var query = _db.Heroes.Where(hero => hero.OwnerDeviceId == deviceId && hero.Id == heroId);
        if (configureQuery is not null)
            query = configureQuery(query);

        return query.FirstOrDefaultAsync(cancellationToken);
    }
}
