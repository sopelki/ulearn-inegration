using ReSharperPlugin.UlearnIntegration.Application.Practices;


namespace ReSharperPlugin.UlearnIntegration.Application.Interfaces;

public interface IPracticeContextProvider
{
    PracticeContext GetSelectedPracticeContext();
}