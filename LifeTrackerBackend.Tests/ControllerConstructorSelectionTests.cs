using LifeTracker.Controllers;
using Microsoft.Extensions.DependencyInjection;

namespace LifeTrackerBackend.Tests;

public class ControllerConstructorSelectionTests
{
    [Fact]
    public void HeroController_DiFactory_CanBeCreatedWithoutConstructorAmbiguity()
    {
        var exception = Record.Exception(() => ActivatorUtilities.CreateFactory(typeof(HeroController), Type.EmptyTypes));

        Assert.Null(exception);
    }

    [Fact]
    public void TaskController_DiFactory_CanBeCreatedWithoutConstructorAmbiguity()
    {
        var exception = Record.Exception(() => ActivatorUtilities.CreateFactory(typeof(TaskController), Type.EmptyTypes));

        Assert.Null(exception);
    }

    [Fact]
    public void ShopController_DiFactory_CanBeCreatedWithoutConstructorAmbiguity()
    {
        var exception = Record.Exception(() => ActivatorUtilities.CreateFactory(typeof(ShopController), Type.EmptyTypes));

        Assert.Null(exception);
    }
}
