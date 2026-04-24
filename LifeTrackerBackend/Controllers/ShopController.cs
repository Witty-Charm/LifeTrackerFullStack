using LifeTracker.Services;
using Microsoft.AspNetCore.Mvc;

namespace LifeTracker.Controllers;

[ApiController]
[Route("api/[controller]")]
public class ShopController : DeviceScopedControllerBase
{
    private readonly IShopService _shopService;

    public ShopController(IShopService shopService, ICurrentHeroService currentHeroService) : base(currentHeroService)
    {
        _shopService = shopService;
    }

    [HttpGet("items")]
    public async Task<ActionResult<IEnumerable<ShopItemDto>>> GetItems()
    {
        var items = await _shopService.GetItemsAsync();
        return Ok(items);
    }

    [HttpPost("buy")]
    public async Task<ActionResult<BuyResultDto>> BuyItem([FromBody] BuyItemRequest request)
    {
        _ = RequireCurrentDevice(out var errorResult);
        if (errorResult is not null)
            return errorResult;

        var ownedHero = await CurrentHeroService.GetOwnedHeroAsync(HttpContext, request.HeroId);
        if (ownedHero == null)
            return NotFound();

        var (result, error) = await _shopService.BuyItemAsync(
            request.HeroId,
            request.ItemId,
            request.ClientTimeZone,
            request.ClientLocalDateTime);
        if (error is not null)
            return BadRequest(new { message = error });
        return Ok(result);
    }

    [HttpGet("inventory/{heroId:int}")]
    public async Task<ActionResult<IEnumerable<PurchasedItemDto>>> GetInventory(int heroId)
    {
        _ = RequireCurrentDevice(out var errorResult);
        if (errorResult is not null)
            return errorResult;

        var ownedHero = await CurrentHeroService.GetOwnedHeroAsync(HttpContext, heroId);
        if (ownedHero == null)
            return NotFound();

        var inventory = await _shopService.GetInventoryAsync(heroId);
        return Ok(inventory);
    }
}
