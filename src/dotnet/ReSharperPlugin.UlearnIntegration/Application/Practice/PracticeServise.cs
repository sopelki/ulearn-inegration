using System.Threading;
using System.Threading.Tasks;
using ReSharperPlugin.UlearnIntegration.Domain.Models;

namespace ReSharperPlugin.UlearnIntegration.Application.Interfaces;

public class PracticeServise(IPracticeApiClient practiceApiClient)
{
    private Practice currentPractice;
    
    public async Task<Practice> GetPracticeAsync(CancellationToken ct = default)
    {
        if (currentPractice != null)
        {
            return currentPractice;
        }
        currentPractice = await practiceApiClient.GetSelectedPracticeFromFileAsync(ct);
        return currentPractice;
    }
    
    public async Task<Practice> RefreshPracticeAsync(CancellationToken ct = default)
    {
        currentPractice = await practiceApiClient.GetSelectedPracticeFromFileAsync(ct);
        return currentPractice;
    }
}