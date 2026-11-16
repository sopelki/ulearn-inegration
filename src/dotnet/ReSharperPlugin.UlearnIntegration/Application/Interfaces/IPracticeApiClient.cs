
using System.Threading;
using System.Threading.Tasks;
using ReSharperPlugin.UlearnIntegration.Application.Practices;

namespace ReSharperPlugin.UlearnIntegration.Application.Interfaces;

public interface IPracticeApiClient
{
    Task<SlideDto> GetSlideDetailsAsync(string courseId, string slideId, CancellationToken ct = default);
}