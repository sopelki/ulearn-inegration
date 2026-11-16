using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Application.Parts;
using JetBrains.ProjectModel;
using ReSharperPlugin.UlearnIntegration.Application.Interfaces;
using ReSharperPlugin.UlearnIntegration.Application.Practices;
using ReSharperPlugin.UlearnIntegration.Infrastructure.Resilience;
using UlearnApi.Client;

namespace ReSharperPlugin.UlearnIntegration.Infrastructure.ApiClients;

[SolutionComponent(Instantiation.ContainerAsyncAnyThreadSafe)]
public class PracticeApiClient(ApiClient apiClient, ISolution solution): IPracticeApiClient
{
    public async Task<SlideDto> GetSlideDetailsAsync(string courseId, string slideId, CancellationToken ct)
    {
        var response = await RetryPolicy.ExecuteAsync(() =>
            apiClient.Slides[courseId][Guid.Parse(slideId)].GetAsync(cancellationToken: ct)
        );

        if (response == null)
            throw new InvalidOperationException($"Не удалось получить данные практики '{slideId}' для курса '{courseId}'.");

        var htmlContent = string.Empty;
        if (response.Blocks != null)
        {
            var htmlBlocks = response.Blocks
                .Where(b => b.GetType().GetProperty("$type")?.GetValue(b)?.ToString() == "html" || 
                            b.ToString()?.Contains("Html") == true)
                .Select(b => b.GetType().GetProperty("Content")?.GetValue(b)?.ToString())
                .Where(content => !string.IsNullOrEmpty(content));

            htmlContent = string.Join("\n", htmlBlocks);
        }
        
        var hints = new List<string>();
        if (response.Blocks != null)
        {
            foreach (var block in response.Blocks)
            {
                var hintsProperty = block.GetType().GetProperty("RenderedHints");
                if (hintsProperty?.GetValue(block) is IEnumerable<string> blockHints)
                {
                    hints.AddRange(blockHints);
                }
            }
        }
        
        return new SlideDto(response.Id.ToString(), response.Title ?? "Без названия", htmlContent, hints);
    }
}