using LifeTracker.Services;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace LifeTracker.Controllers;

/// <summary>
/// Base class for controllers that operate on the currently authenticated
/// user's resources. Requires a valid Bearer JWT.
/// </summary>
[Authorize(AuthenticationSchemes = JwtBearerDefaults.AuthenticationScheme)]
public abstract class DeviceScopedControllerBase : ControllerBase
{
    protected DeviceScopedControllerBase(ICurrentHeroService currentHeroService)
    {
        CurrentHeroService = currentHeroService;
    }

    protected ICurrentHeroService CurrentHeroService { get; }

    /// <summary>
    /// Resolves the authenticated user id from the Bearer JWT. Returns null and
    /// sets <paramref name="errorResult"/> to a 401 response when no
    /// authenticated principal is present.
    /// </summary>
    protected int? RequireCurrentUser(out ActionResult? errorResult)
    {
        if (!CurrentHeroService.TryGetCurrentUserId(HttpContext, out var userId, out var errorMessage))
        {
            errorResult = Unauthorized(new { message = errorMessage });
            return null;
        }

        errorResult = null;
        return userId;
    }

    /// <summary>
    /// Resolves an optional X-Device-Id header for analytics / refresh-token
    /// bookkeeping. Returns null when the header is missing or invalid; never
    /// short-circuits the request.
    /// </summary>
    protected string? TryGetDeviceIdMetadata()
    {
        return CurrentHeroService.TryGetCurrentDeviceId(HttpContext, out var deviceId, out _)
            ? deviceId
            : null;
    }

    /// <summary>
    /// Legacy helper kept for backwards compatibility with existing controller
    /// methods. Prefer <see cref="RequireCurrentUser"/> in new code.
    /// </summary>
    [Obsolete("Use RequireCurrentUser. Device id is no longer the auth signal.")]
    protected string? RequireCurrentDevice(out ActionResult? errorResult)
    {
        if (!CurrentHeroService.TryGetCurrentDeviceId(HttpContext, out var deviceId, out var errorMessage))
        {
            errorResult = BadRequest(new { message = errorMessage });
            return null;
        }

        errorResult = null;
        return deviceId;
    }
}
