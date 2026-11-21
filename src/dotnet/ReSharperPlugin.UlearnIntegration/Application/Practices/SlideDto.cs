using System.Collections.Generic;

namespace ReSharperPlugin.UlearnIntegration.Application.Practices;
public sealed record SlideDto(string Id, string Title, string Html, IReadOnlyList<string> Hints)
{
    public IReadOnlyList<string> Hints { get; } = Hints;
    public string Html { get; } = Html;
    public string Title { get; } = Title;
    public string Id { get; } = Id;
}