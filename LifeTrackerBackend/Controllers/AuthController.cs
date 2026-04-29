using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using LifeTracker.Infrastructure;
using LifeTracker.Services.Auth;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace LifeTracker.Controllers;

[ApiController]
[Route("api/auth")]
public class AuthController : ControllerBase
{
    private readonly IUserAuthService _auth;

    public AuthController(IUserAuthService auth)
    {
        _auth = auth;
    }

    [HttpPost("google")]
    [AllowAnonymous]
    public async Task<ActionResult<AuthResponse>> SignInWithGoogle(
        [FromBody] GoogleSignInRequest request,
        CancellationToken cancellationToken)
    {
        if (request is null || string.IsNullOrWhiteSpace(request.IdToken))
            return BadRequest(new { message = "idToken is required." });

        try
        {
            var deviceId = ResolveDeviceIdForClaim(request.DeviceId);
            var result = await _auth.SignInWithGoogleAsync(request.IdToken, deviceId, cancellationToken);
            return Ok(AuthResponse.From(result));
        }
        catch (GoogleTokenVerificationException ex)
        {
            return Unauthorized(new { message = ex.Message });
        }
    }

    [HttpPost("refresh")]
    [AllowAnonymous]
    public async Task<ActionResult<AuthResponse>> Refresh(
        [FromBody] RefreshRequest request,
        CancellationToken cancellationToken)
    {
        if (request is null || string.IsNullOrWhiteSpace(request.RefreshToken))
            return BadRequest(new { message = "refreshToken is required." });

        try
        {
            var deviceId = ResolveDeviceIdForClaim(request.DeviceId);
            var result = await _auth.RefreshAsync(request.RefreshToken, deviceId, cancellationToken);
            return Ok(AuthResponse.From(result));
        }
        catch (AuthenticationFailedException ex)
        {
            return Unauthorized(new { message = ex.Message });
        }
    }

    [HttpPost("logout")]
    [Authorize(AuthenticationSchemes = JwtBearerDefaults.AuthenticationScheme)]
    public async Task<IActionResult> Logout(
        [FromBody] LogoutRequest? request,
        CancellationToken cancellationToken)
    {
        if (!TryGetCurrentUserId(out var userId))
            return Unauthorized();

        await _auth.SignOutAsync(userId, request?.RefreshToken, cancellationToken);
        return NoContent();
    }

    [HttpGet("me")]
    [Authorize(AuthenticationSchemes = JwtBearerDefaults.AuthenticationScheme)]
    public IActionResult Me()
    {
        if (!TryGetCurrentUserId(out var userId))
            return Unauthorized();

        return Ok(new
        {
            id = userId,
            email = User.FindFirstValue(JwtRegisteredClaimNames.Email)
                    ?? User.FindFirstValue(ClaimTypes.Email),
            name = User.FindFirstValue(JwtRegisteredClaimNames.Name)
                   ?? User.FindFirstValue(ClaimTypes.Name),
        });
    }

    private bool TryGetCurrentUserId(out int userId)
    {
        var raw = User.FindFirstValue(JwtRegisteredClaimNames.Sub)
                  ?? User.FindFirstValue(ClaimTypes.NameIdentifier);
        return int.TryParse(raw, out userId);
    }

    private string? ResolveDeviceIdForClaim(string? requestDeviceId)
    {
        if (!string.IsNullOrWhiteSpace(requestDeviceId)
            && Guid.TryParse(requestDeviceId, out var fromBody))
        {
            return fromBody.ToString("D");
        }

        if (DeviceRequestContext.TryResolveCurrentDeviceId(HttpContext, out var fromHeader, out _))
            return fromHeader;

        return null;
    }
}

public sealed record GoogleSignInRequest(string IdToken, string? DeviceId);

public sealed record RefreshRequest(string RefreshToken, string? DeviceId);

public sealed record LogoutRequest(string? RefreshToken);

public sealed record AuthResponse(
    string AccessToken,
    DateTimeOffset AccessTokenExpiresAt,
    string RefreshToken,
    DateTimeOffset RefreshTokenExpiresAt,
    AuthenticatedUserDto User)
{
    public static AuthResponse From(AuthResult result) => new(
        result.AccessToken,
        result.AccessTokenExpiresAt,
        result.RefreshToken,
        result.RefreshTokenExpiresAt,
        new AuthenticatedUserDto(result.User.Id, result.User.Email, result.User.DisplayName, result.User.AvatarUrl));
}

public sealed record AuthenticatedUserDto(int Id, string Email, string? DisplayName, string? AvatarUrl);
