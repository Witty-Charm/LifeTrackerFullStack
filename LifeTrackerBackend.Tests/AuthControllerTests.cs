using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using LifeTracker.Controllers;
using LifeTracker.Models;
using LifeTracker.Services.Auth;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;

namespace LifeTrackerBackend.Tests;

public class AuthControllerTests
{
    private static AuthResult SampleAuthResult(int userId = 42) => new(
        AccessToken: "jwt.access.token",
        AccessTokenExpiresAt: DateTimeOffset.UtcNow.AddMinutes(15),
        RefreshToken: "plain-refresh-token",
        RefreshTokenExpiresAt: DateTimeOffset.UtcNow.AddDays(30),
        User: new AuthenticatedUser(userId, "alex@example.com", "Alex", "https://example.com/a.png"));

    private static AuthController CreateController(IUserAuthService service, ClaimsPrincipal? principal = null)
    {
        var controller = new AuthController(service);
        controller.ControllerContext = new ControllerContext
        {
            HttpContext = new DefaultHttpContext
            {
                User = principal ?? new ClaimsPrincipal(new ClaimsIdentity()),
            },
        };
        return controller;
    }

    [Fact]
    public async Task SignInWithGoogle_returns_400_when_id_token_missing()
    {
        var controller = CreateController(new RecordingAuthService());

        var result = await controller.SignInWithGoogle(new GoogleSignInRequest("", null), CancellationToken.None);

        Assert.IsType<BadRequestObjectResult>(result.Result);
    }

    [Fact]
    public async Task SignInWithGoogle_returns_200_with_auth_payload_on_success()
    {
        var service = new RecordingAuthService { GoogleResult = SampleAuthResult() };
        var controller = CreateController(service);

        var result = await controller.SignInWithGoogle(
            new GoogleSignInRequest("id-token", "11111111-1111-1111-1111-111111111111"),
            CancellationToken.None);

        var ok = Assert.IsType<OkObjectResult>(result.Result);
        var body = Assert.IsType<AuthResponse>(ok.Value);
        Assert.Equal("jwt.access.token", body.AccessToken);
        Assert.Equal("plain-refresh-token", body.RefreshToken);
        Assert.Equal(42, body.User.Id);
        Assert.Equal("11111111-1111-1111-1111-111111111111", service.LastClaimDeviceId);
        Assert.Equal("id-token", service.LastIdToken);
    }

    [Fact]
    public async Task SignInWithGoogle_returns_401_when_google_token_invalid()
    {
        var service = new RecordingAuthService
        {
            GoogleException = new GoogleTokenVerificationException("bad audience"),
        };
        var controller = CreateController(service);

        var result = await controller.SignInWithGoogle(
            new GoogleSignInRequest("bad", null), CancellationToken.None);

        Assert.IsType<UnauthorizedObjectResult>(result.Result);
    }

    [Fact]
    public async Task Refresh_returns_400_when_token_missing()
    {
        var controller = CreateController(new RecordingAuthService());

        var result = await controller.Refresh(new RefreshRequest("", null), CancellationToken.None);

        Assert.IsType<BadRequestObjectResult>(result.Result);
    }

    [Fact]
    public async Task Refresh_returns_401_when_service_rejects_token()
    {
        var service = new RecordingAuthService
        {
            RefreshException = new AuthenticationFailedException("revoked"),
        };
        var controller = CreateController(service);

        var result = await controller.Refresh(new RefreshRequest("token", null), CancellationToken.None);

        Assert.IsType<UnauthorizedObjectResult>(result.Result);
    }

    [Fact]
    public async Task Refresh_returns_200_with_new_tokens()
    {
        var service = new RecordingAuthService { RefreshResult = SampleAuthResult() };
        var controller = CreateController(service);

        var result = await controller.Refresh(new RefreshRequest("old", "deviceB"), CancellationToken.None);

        var ok = Assert.IsType<OkObjectResult>(result.Result);
        var body = Assert.IsType<AuthResponse>(ok.Value);
        Assert.Equal("jwt.access.token", body.AccessToken);
        Assert.Equal("old", service.LastRefreshToken);
    }

    [Fact]
    public async Task Logout_without_authentication_returns_401()
    {
        var controller = CreateController(new RecordingAuthService());

        var result = await controller.Logout(new LogoutRequest(null), CancellationToken.None);

        Assert.IsType<UnauthorizedResult>(result);
    }

    [Fact]
    public async Task Logout_with_authenticated_user_revokes_via_service()
    {
        var service = new RecordingAuthService();
        var principal = BuildPrincipal(userId: 7);
        var controller = CreateController(service, principal);

        var result = await controller.Logout(new LogoutRequest("rt-1"), CancellationToken.None);

        Assert.IsType<NoContentResult>(result);
        Assert.Equal(7, service.LastSignOutUserId);
        Assert.Equal("rt-1", service.LastSignOutRefreshToken);
    }

    [Fact]
    public void Me_without_authentication_returns_401()
    {
        var controller = CreateController(new RecordingAuthService());

        var result = controller.Me();

        Assert.IsType<UnauthorizedResult>(result);
    }

    [Fact]
    public void Me_returns_user_payload_from_jwt_claims()
    {
        var principal = BuildPrincipal(userId: 11, email: "x@y", name: "XY");
        var controller = CreateController(new RecordingAuthService(), principal);

        var result = controller.Me();

        var ok = Assert.IsType<OkObjectResult>(result);
        Assert.NotNull(ok.Value);
    }

    private static ClaimsPrincipal BuildPrincipal(int userId, string email = "u@e", string name = "User")
    {
        var identity = new ClaimsIdentity(new[]
        {
            new Claim(JwtRegisteredClaimNames.Sub, userId.ToString()),
            new Claim(JwtRegisteredClaimNames.Email, email),
            new Claim(JwtRegisteredClaimNames.Name, name),
        }, authenticationType: "test");
        return new ClaimsPrincipal(identity);
    }

    private sealed class RecordingAuthService : IUserAuthService
    {
        public AuthResult? GoogleResult { get; set; }
        public Exception? GoogleException { get; set; }
        public AuthResult? RefreshResult { get; set; }
        public Exception? RefreshException { get; set; }

        public string? LastIdToken { get; private set; }
        public string? LastClaimDeviceId { get; private set; }
        public string? LastRefreshToken { get; private set; }
        public int? LastSignOutUserId { get; private set; }
        public string? LastSignOutRefreshToken { get; private set; }

        public Task<AuthResult> SignInWithGoogleAsync(string googleIdToken, string? claimDeviceId, CancellationToken ct)
        {
            LastIdToken = googleIdToken;
            LastClaimDeviceId = claimDeviceId;
            if (GoogleException is not null) throw GoogleException;
            return Task.FromResult(GoogleResult ?? throw new InvalidOperationException("GoogleResult not set"));
        }

        public Task<AuthResult> RefreshAsync(string refreshToken, string? deviceId, CancellationToken ct)
        {
            LastRefreshToken = refreshToken;
            if (RefreshException is not null) throw RefreshException;
            return Task.FromResult(RefreshResult ?? throw new InvalidOperationException("RefreshResult not set"));
        }

        public Task SignOutAsync(int userId, string? refreshToken, CancellationToken ct)
        {
            LastSignOutUserId = userId;
            LastSignOutRefreshToken = refreshToken;
            return Task.CompletedTask;
        }
    }
}
