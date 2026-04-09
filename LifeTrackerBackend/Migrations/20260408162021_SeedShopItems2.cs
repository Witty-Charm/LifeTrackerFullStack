using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace LifeTracker.Migrations
{
    /// <inheritdoc />
    public partial class SeedShopItems2 : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
{
    migrationBuilder.InsertData(
        table: "ShopItems",
        columns: new[] { "Name", "Description", "Price", "ItemType", "EffectValue" },
        values: new object[,]
        {
            { "Wooden Sword",  "A basic training sword. +2 STR.", 50, 2, 2 },
            { "Leather Armor", "Basic protection. +2 CON.", 100, 3, 2 },
        });
}

protected override void Down(MigrationBuilder migrationBuilder)
{
    migrationBuilder.Sql("DELETE FROM ShopItems WHERE Name IN ('Wooden Sword','Leather Armor')");
}
    }
}
