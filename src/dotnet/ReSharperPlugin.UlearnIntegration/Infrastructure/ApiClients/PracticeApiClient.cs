using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Application.Parts;
using JetBrains.ProjectModel;
using ReSharperPlugin.UlearnIntegration.Domain.Models;
using ReSharperPlugin.UlearnIntegration.Infrastructure.Resilience;
using UlearnApi.Client;

namespace ReSharperPlugin.UlearnIntegration.Application.Interfaces;

[SolutionComponent(Instantiation.ContainerAsyncAnyThreadSafe)]
public class PracticeApiClient(ApiClient apiClient, ISolution solution): IPracticeApiClient
{
    private const string ContextFileName = "ulearn-practice.json";

    public async Task<Practice> GetSelectedPracticeFromFileAsync(CancellationToken ct = default)
    {
        var solutionDir = solution.SolutionDirectory;
        var jsonFilePath = solutionDir.Combine(ContextFileName).FullPath;
        
        if (!File.Exists(jsonFilePath))
            throw new FileNotFoundException($"Файл с выбранной практикой не найден в корне солюшена: {jsonFilePath}");
        
        var jsonContent = File.ReadAllText(jsonFilePath);
        var context = JsonSerializer.Deserialize<PracticeContext>(jsonContent, new JsonSerializerOptions
        {
            PropertyNameCaseInsensitive = true
        });
        
        if (context == null || string.IsNullOrEmpty(context.CourseId) || string.IsNullOrEmpty(context.SlideId))
            throw new InvalidOperationException("Файл конфигурации практики поврежден.");
        
        return await FetchSlideDetailsAsync(context.CourseId, context.SlideId, ct);
    }

    private async Task<Practice> FetchSlideDetailsAsync(string courseId, string slideId, CancellationToken ct)
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
        
        return new Practice(response.Id.ToString(), response.Title ?? "Без названия", htmlContent, hints);
    }
}