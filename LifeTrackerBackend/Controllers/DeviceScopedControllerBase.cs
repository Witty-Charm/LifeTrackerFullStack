using LifeTracker.Services;
using Microsoft.AspNetCore.Mvc;

namespace LifeTracker.Controllers;

public abstract class DeviceScopedControllerBase : ControllerBase
{
    protected DeviceScopedControllerBase(ICurrentHeroService currentHeroService)
    {
        CurrentHeroService = currentHeroService;
    }

    protected ICurrentHeroService CurrentHeroService { get; }

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
