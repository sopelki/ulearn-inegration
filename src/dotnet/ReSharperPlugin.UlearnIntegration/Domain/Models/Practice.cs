using System.Collections.Generic;

namespace ReSharperPlugin.UlearnIntegration.Domain.Models;

public class Practice(string id, string title, string htmlContent, IReadOnlyList<string> hints)
{
    public string Id { get; } = id;
    public string Title { get; } = title;
    public string HtmlContent { get; } = htmlContent;
    public IReadOnlyList<string> Hints { get; } = hints;
}