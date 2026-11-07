namespace ReSharperPlugin.UlearnIntegration.Domain.Models;

// доменная модель пользователя
public class UserProfile(string id, string login, string name)
{
    public string Id { get; } = id;
    public string Login { get; } = login;
    public string Name { get; } = name;
}