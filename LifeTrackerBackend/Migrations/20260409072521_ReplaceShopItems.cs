using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace LifeTracker.Migrations
{
    /// <inheritdoc />
    public partial class ReplaceShopItems : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.Sql("DELETE FROM Purchases;");
            migrationBuilder.Sql("DELETE FROM ShopItems;");

            migrationBuilder.InsertData(
                table: "ShopItems",
                columns: new[] { "Name", "Description", "Price", "ItemType", "EffectValue" },
                values: new object[,]
                {
                    { "Health Potion", "Restores 15 HP", 20, 1, 15 },
                    { "Elixir of Life", "Restores 50 HP", 75, 2, 50 },
                    { "XP Boost", "+25% XP for next 5 tasks", 60, 3, 25 },
                    { "Streak Shield", "Protects streak from breaking once", 80, 4, 1 },
                    { "Revival Token", "Removes recovery debuff instantly", 100, 5, 1 }
                });
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.Sql("DELETE FROM ShopItems;");
        }
    }
}
