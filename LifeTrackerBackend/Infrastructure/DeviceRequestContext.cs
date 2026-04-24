using Microsoft.AspNetCore.Http;

namespace LifeTracker.Infrastructure;

public static class DeviceRequestContext
{
    public const string HeaderName = "X-Device-Id";
    public const string ItemKey = "CurrentDeviceId";

    public static bool TryResolveCurrentDeviceId(HttpContext? httpContext, out string deviceId, out string errorMessage)
    {
        if (httpContext is null)
        {
            deviceId = string.Empty;
            errorMessage = "Device context is unavailable.";
            return false;
        }

        if (httpContext.Items.TryGetValue(ItemKey, out var cachedValue) && cachedValue is string cachedDeviceId && Guid.TryParse(cachedDeviceId, out var cachedGuid))
        {
            deviceId = cachedGuid.ToString("D");
            errorMessage = string.Empty;
            return true;
        }

        var rawValue = httpContext.Request.Headers[HeaderName].ToString();
        if (string.IsNullOrWhiteSpace(rawValue))
        {
            deviceId = string.Empty;
            errorMessage = $"{HeaderName} header is required.";
            return false;
        }

        if (!Guid.TryParse(rawValue, out var parsedDeviceId))
        {
            deviceId = string.Empty;
            errorMessage = $"{HeaderName} must be a valid UUID.";
            return false;
        }

        deviceId = parsedDeviceId.ToString("D");
        errorMessage = string.Empty;
        return true;
    }

    public static void StoreCurrentDeviceId(HttpContext httpContext, string deviceId)
    {
        httpContext.Items[ItemKey] = deviceId;
    }
}
