using Microsoft.AspNetCore.Mvc;
using LifeTracker.Services;

namespace LifeTracker.Controllers;

[ApiController]
[Route("api/[controller]")]
public class ShopController : ControllerBase
{
    private readonly IShopService _shopService;

    public ShopController(IShopService shopService) => _shopService = shopService;

    [HttpGet("items")]
    public async Task<ActionResult<IEnumerable<ShopItemDto>>> GetItems()
    {
        var items = await _shopService.GetItemsAsync();
        return Ok(items);
    }

    [HttpPost("buy")]
    public async Task<ActionResult<BuyResultDto>> BuyItem([FromBody] BuyItemRequest request)
    {
        var (result, error) = await _shopService.BuyItemAsync(
            request.HeroId,
            request.ItemId,
            request.ClientTimeZone,
            request.ClientLocalDateTime
        );
        if (error is not null)
            return BadRequest(new { message = error });
        return Ok(result);
    }

    [HttpGet("inventory/{heroId:int}")]
    public async Task<ActionResult<IEnumerable<PurchasedItemDto>>> GetInventory(int heroId)
    {
        var inventory = await _shopService.GetInventoryAsync(heroId);
        return Ok(inventory);
    }
}
