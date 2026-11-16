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
    private const string ContextFileName = "ulearn-practice.json";

    public PracticeContext GetSelectedPracticeContext()
    {
        var solutionDir = solution.SolutionDirectory;
        var jsonFilePath = solutionDir.Combine(ContextFileName).FullPath;

        if (!File.Exists(jsonFilePath))
            throw new FileNotFoundException($"Файл с выбранной практикой не найден в корне солюшена: {jsonFilePath}");

        var jsonContent = File.ReadAllText(jsonFilePath);
        var context = JsonSerializer.Deserialize<PracticeContextFile>(jsonContent, new JsonSerializerOptions
        {
            PropertyNameCaseInsensitive = true
        });
        
        if (context.Version != 1)
            throw new InvalidOperationException($"Неподдерживаемая версия файла: {context.Version}.");

        if (context == null || string.IsNullOrEmpty(context.CourseId) || string.IsNullOrEmpty(context.SlideId))
            throw new InvalidOperationException("Файл конфигурации практики поврежден.");

        return new PracticeContext(context.CourseId, context.SlideId);
    }
}