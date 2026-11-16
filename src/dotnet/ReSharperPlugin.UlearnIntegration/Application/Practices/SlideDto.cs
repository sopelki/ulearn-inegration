using System.Collections.Generic;

namespace ReSharperPlugin.UlearnIntegration.Application.Practices;
public sealed record SlideDto(string Id, string Title, string Html, IReadOnlyList<string> Hints)
{
    public string Id { get; } = Id;
    public string Title { get; } = Title;
    public string Html { get; } = Html;
    public IReadOnlyList<string> Hints { get; } = Hints;
}