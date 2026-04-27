using System.Collections;
using System.Text.Json;
using System.Text.Json.Serialization;
using Microsoft.EntityFrameworkCore;
using LifeTracker.Configuration;
using LifeTracker.Data;
using LifeTracker.Services;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllers()
    .AddJsonOptions(options =>
    {
        options.JsonSerializerOptions.PropertyNamingPolicy = JsonNamingPolicy.CamelCase;
        options.JsonSerializerOptions.ReferenceHandler = ReferenceHandler.IgnoreCycles;
        options.JsonSerializerOptions.Converters.Add(new JsonStringEnumConverter());
        options.JsonSerializerOptions.DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull;
    });
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

var connectionString = RuntimeConfiguration.ResolveConnectionString(builder.Configuration);

builder.Services.AddDbContext<ApplicationDbContext>(options =>
    options.UseNpgsql(connectionString));

builder.Services.AddSingleton<GameEngineService>();
builder.Services.AddScoped<LifeTracker.Services.Achievements.AchievementService>();
builder.Services.AddScoped<IShopService, ShopService>();
builder.Services.AddScoped<ICurrentHeroService, CurrentHeroService>();
builder.Services.AddScoped<LifeTracker.Services.Time.IHeroTimeService, LifeTracker.Services.Time.HeroTimeService>();
builder.Services.AddScoped<IDailyScheduleService, DailyScheduleService>();
builder.Services.AddHttpContextAccessor();
builder.Services.AddProblemDetails();

var app = builder.Build();

using (var scope = app.Services.CreateScope())
{
    var db = scope.ServiceProvider.GetRequiredService<ApplicationDbContext>();
    db.Database.Migrate();

    if (!db.ShopItems.Any())
    {
        db.ShopItems.AddRange(
            new LifeTracker.Models.ShopItem { Name = "Health Potion",  Description = "Restores 15 HP",                     Price = 60,  ItemType = 1, EffectValue = 15 },
            new LifeTracker.Models.ShopItem { Name = "Elixir of Life", Description = "Restores 50 HP",                     Price = 200, ItemType = 2, EffectValue = 50 },
            new LifeTracker.Models.ShopItem { Name = "XP Boost",       Description = "+25% XP for next 5 tasks",           Price = 60,  ItemType = 3, EffectValue = 25 },
            new LifeTracker.Models.ShopItem { Name = "Streak Shield",  Description = "Protects streak from breaking once", Price = 250, ItemType = 4, EffectValue = 1  },
            new LifeTracker.Models.ShopItem { Name = "Revival Token",  Description = "Removes recovery debuff instantly",  Price = 100, ItemType = 5, EffectValue = 1  }
        );
        db.SaveChanges();
    }
}
app.UseSwagger();
app.UseSwaggerUI();

app.UseHttpsRedirection();
app.UseAuthorization();
app.MapControllers();

app.Urls.Add(RuntimeConfiguration.ResolveListenUrl(Environment.GetEnvironmentVariables()));
app.Run();
