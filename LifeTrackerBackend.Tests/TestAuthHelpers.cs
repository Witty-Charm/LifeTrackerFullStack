using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using Microsoft.AspNetCore.Http;

namespace LifeTrackerBackend.Tests;

internal static class TestAuthHelpers
{
    /// <summary>
    /// Creates an HttpContext with an authenticated principal carrying a `sub` claim
    /// equal to <paramref name="userId"/>. Optionally also sets the X-Device-Id header.
    /// </summary>
    internal static DefaultHttpContext CreateAuthenticatedHttpContext(int userId, string? deviceId = null)
    {
        var context = new DefaultHttpContext();
        ApplyAuth(context, userId);
        if (!string.IsNullOrWhiteSpace(deviceId))
            context.Request.Headers["X-Device-Id"] = deviceId;
        return context;
    }

    /// <summary>
    /// Replaces the principal on an existing HttpContext with one carrying
    /// <paramref name="userId"/> as the `sub` claim.
    /// </summary>
    internal static void ApplyAuth(HttpContext context, int userId)
    {
        var identity = new ClaimsIdentity(
            new[] { new Claim(JwtRegisteredClaimNames.Sub, userId.ToString()) },
            authenticationType: "test");
        context.User = new ClaimsPrincipal(identity);
    }
}
