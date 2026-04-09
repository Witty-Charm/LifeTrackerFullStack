using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace LifeTracker.Migrations
{
    /// <inheritdoc />
    public partial class UpdateHealItemPrices : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.Sql("UPDATE ShopItems SET Price = 60, EffectValue = 15 WHERE ItemType = 1;");
            migrationBuilder.Sql("UPDATE ShopItems SET Price = 200, EffectValue = 50 WHERE ItemType = 2;");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.Sql("UPDATE ShopItems SET Price = 20, EffectValue = 15 WHERE ItemType = 1;");
            migrationBuilder.Sql("UPDATE ShopItems SET Price = 75, EffectValue = 50 WHERE ItemType = 2;");
        }
    }
}
