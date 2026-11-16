using System.Threading;
using System.Threading.Tasks;
using ReSharperPlugin.UlearnIntegration.Application.Interfaces;
using ReSharperPlugin.UlearnIntegration.Domain.Models;

namespace ReSharperPlugin.UlearnIntegration.Application.Practices;

public class PracticeService(IPracticeContextProvider contextProvider, IPracticeApiClient apiClient)
{
    private PracticeContext cachedContext;
    private Practice cachedPractice;

    public async Task<Practice> GetPracticeAsync(CancellationToken ct = default)
    {
        var context = contextProvider.GetSelectedPracticeContext();
        if (cachedPractice != null && cachedContext == context)
            return cachedPractice;

        return await LoadAsync(context, ct);
    }

    public Task<Practice> RefreshPracticeAsync(CancellationToken ct = default)
        => LoadAsync(contextProvider.GetSelectedPracticeContext(), ct);

    private async Task<Practice> LoadAsync(PracticeContext context, CancellationToken ct)
    {
        var slide = await apiClient.GetSlideDetailsAsync(context.CourseId, context.SlideId, ct);
        cachedContext = context;
        cachedPractice = new Practice(slide.Id, slide.Title, slide.Html, slide.Hints);
        return cachedPractice;
    }
}