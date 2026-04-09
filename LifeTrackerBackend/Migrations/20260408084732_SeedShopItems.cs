using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace LifeTracker.Migrations
{
    /// <inheritdoc />
    public partial class SeedShopItems : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
{
    migrationBuilder.InsertData(
        table: "ShopItems",
        columns: new[] { "Name", "Description", "Price", "ItemType", "EffectValue" },
        values: new object[,]
        {
            { "Health Potion", "Restores 15 HP", 20, 1, 15 },
            { "Mana Potion",   "Restores 20 MP", 25, 2, 20 }
        });
}

protected override void Down(MigrationBuilder migrationBuilder)
{
    migrationBuilder.Sql("DELETE FROM ShopItems WHERE Name IN ('Health Potion','Mana Potion')");
}
    }
}
