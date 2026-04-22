using LifeTracker.Data;
using Microsoft.EntityFrameworkCore.Infrastructure;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace LifeTracker.Migrations
{
    /// <inheritdoc />
    [DbContext(typeof(ApplicationDbContext))]
    [Migration("20260422110000_AddHabitPolarity")]
    public partial class AddHabitPolarity : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<int>(
                name: "Polarity",
                table: "GameTasks",
                type: "INTEGER",
                nullable: false,
                defaultValue: 3);

            migrationBuilder.Sql("UPDATE GameTasks SET Polarity = 3 WHERE Type = 1;");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "Polarity",
                table: "GameTasks");
        }
    }
}
