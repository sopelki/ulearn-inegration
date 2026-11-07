using System;
using ReSharperPlugin.UlearnIntegration.Infrastructure.Resilience;
using System.Threading;
using System.Threading.Tasks;
using ReSharperPlugin.UlearnIntegration.Application.Interfaces;
using UlearnApi.Client;
using UlearnApi.Client.Models;

namespace ReSharperPlugin.UlearnIntegration.Infrastructure.ApiClients;

public class AuthApiClient(ApiClient apiClient) : IAuthApiClient
{
    public async Task<string> LoginAsync(string login, string password, CancellationToken ct)
    {
        var loginData = new LoginPasswordParameters { Login = login, Password = password };

        var response = await RetryPolicy.ExecuteAsync(() =>
            apiClient.Account.Login.PostAsync(loginData, cancellationToken: ct)
        );

        return string.IsNullOrEmpty(response?.Token)
            ? throw new InvalidOperationException("Не удалось получить токен от сервера Ulearn.")
            : response.Token;
    }

    public async Task<(string Id, string Login, string Name, string AvatarUrl)> GetAccountInfoAsync(
        CancellationToken ct)
    {
        var account = await RetryPolicy.ExecuteAsync(() =>
            apiClient.Account.GetAsync(cancellationToken: ct)
        );

        return account?.User == null
            ? throw new InvalidOperationException("Не удалось получить данные аккаунта.")
            : (account.User.Id, account.User.Login, account.User.VisibleName, account.User.AvatarUrl);
    }
}