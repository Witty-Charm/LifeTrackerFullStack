using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace LifeTracker.Migrations
{
    /// <inheritdoc />
    public partial class AddOverdueProcessedAt : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<DateTimeOffset>(
                name: "OverdueProcessedAt",
                table: "GameTasks",
                type: "TEXT",
                nullable: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "OverdueProcessedAt",
                table: "GameTasks");
        }
    }
}
