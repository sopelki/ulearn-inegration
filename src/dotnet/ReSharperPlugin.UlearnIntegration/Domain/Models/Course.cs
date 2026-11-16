using System.Collections.Generic;

namespace ReSharperPlugin.UlearnIntegration.Domain.Models;

public class Course(string id, string title, string apiUrl = null)
{
    public string Id { get; } = id;
    public string Title { get; } = title;
    public string ApiUrl { get; } = apiUrl;
    
    public IReadOnlyList<Unit> Units { get; private set; } = [];
    public bool IsLoaded { get; private set; }

    public void SetUnits(IReadOnlyList<Unit> units)
    {
        Units = units;
        IsLoaded = true;
    }
}

public class Unit(string id, string title, IReadOnlyList<PracticeShortInfo> practices)
{
    public string Id { get; } = id;
    public string Title { get; } = title;
    public IReadOnlyList<PracticeShortInfo> Practices { get; } = practices;
}

public class PracticeShortInfo(string id, string title, int maxScore)
{
    public string Id { get; } = id;
    public string Title { get; } = title;
    public int MaxScore { get; } = maxScore;
}