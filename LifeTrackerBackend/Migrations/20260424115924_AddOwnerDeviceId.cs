using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace LifeTracker.Migrations
{
    /// <inheritdoc />
    public partial class AddOwnerDeviceId : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<string>(
                name: "OwnerDeviceId",
                table: "Heroes",
                type: "text",
                nullable: false,
                defaultValue: "");

            migrationBuilder.CreateIndex(
                name: "IX_Heroes_OwnerDeviceId",
                table: "Heroes",
                column: "OwnerDeviceId");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropIndex(
                name: "IX_Heroes_OwnerDeviceId",
                table: "Heroes");

            migrationBuilder.DropColumn(
                name: "OwnerDeviceId",
                table: "Heroes");
        }
    }
}
