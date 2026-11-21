using System;
using System.IO;
using System.Text.Json;
using JetBrains.Application.Parts;
using JetBrains.ProjectModel;
using ReSharperPlugin.UlearnIntegration.Application.Interfaces;
using ReSharperPlugin.UlearnIntegration.Application.Practices;

namespace ReSharperPlugin.UlearnIntegration.Infrastructure.Storage;

[SolutionComponent(Instantiation.ContainerAsyncAnyThreadSafe)]
public class LocalPracticeContextProvider(ISolution solution): IPracticeContextProvider
{
    private const string ContextFileName = ".ulearn";

    public PracticeContext GetSelectedPracticeContext()
    {
        var jsonFilePath = solution.SolutionDirectory.Combine(ContextFileName).FullPath;

        if (!File.Exists(jsonFilePath))
            return null;

        PracticeContextFile context;
        try
        {
            context = JsonSerializer.Deserialize<PracticeContextFile>(
                File.ReadAllText(jsonFilePath),
                new JsonSerializerOptions { PropertyNameCaseInsensitive = true });
        }
        catch (JsonException ex)
        {
            throw new InvalidOperationException("Файл .ulearn повреждён.", ex);
        }

        if (context == null || string.IsNullOrEmpty(context.CourseId) || string.IsNullOrEmpty(context.SlideId))
            throw new InvalidOperationException("Файл конфигурации практики поврежден.");
        
        if (context.Version != 1)
            throw new InvalidOperationException($"Неподдерживаемая версия файла: {context.Version}.");

        return new PracticeContext(context.CourseId, context.SlideId);
    }
}