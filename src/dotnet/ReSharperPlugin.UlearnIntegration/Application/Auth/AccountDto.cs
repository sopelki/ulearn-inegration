namespace ReSharperPlugin.UlearnIntegration.Application.Interfaces;

public record AccountDto(string Id, string Login, string Name, string AvatarUrl)
{
    public string Id { get; } = Id;
    public string Login { get; } = Login;
    public string Name { get; } = Name;
    public string AvatarUrl { get; } = AvatarUrl;
}