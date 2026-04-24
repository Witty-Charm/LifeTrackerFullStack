using LifeTracker.Infrastructure;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Filters;

namespace LifeTracker.Filters;

public sealed class RequireDeviceIdFilter : IAsyncActionFilter
{
    public async Task OnActionExecutionAsync(ActionExecutingContext context, ActionExecutionDelegate next)
    {
        if (!DeviceRequestContext.TryResolveCurrentDeviceId(context.HttpContext, out var deviceId, out var errorMessage))
        {
            context.Result = new BadRequestObjectResult(new { message = errorMessage });
            return;
        }

        DeviceRequestContext.StoreCurrentDeviceId(context.HttpContext, deviceId);
        await next();
    }
}
