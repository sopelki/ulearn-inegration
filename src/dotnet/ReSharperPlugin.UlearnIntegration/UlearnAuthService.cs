using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Application.Parts;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.Rd.Tasks;
using JetBrains.ReSharper.Feature.Services.Protocol;
using JetBrains.Rider.Model;
using Microsoft.Kiota.Abstractions.Authentication;
using Microsoft.Kiota.Http.HttpClientLibrary;
using UlearnApi.Client;
using UlearnApi.Client.Models;
using JetBrains.Util;
using JetBrains.Util.Logging;
using ILogger = JetBrains.Util.ILogger;

namespace ReSharperPlugin.UlearnIntegration;

public class MyTokenHolder : IAccessTokenProvider
{
    public string CurrentToken { get; set; } = "";

    public Task<string> GetAuthorizationTokenAsync(
        Uri uri,
        Dictionary<string, object>? additionalAuthenticationContext = null,
        CancellationToken cancellationToken = default) =>
        Task.FromResult(CurrentToken);

    public AllowedHostsValidator AllowedHostsValidator { get; } = new();
    // список куда можно отправлять токен(по умолчанию везде)
}

[SolutionComponent(Instantiation.ContainerAsyncAnyThreadSafe)]
public class UlearnAuthService
{
    private readonly ApiClient apiClient;
    private readonly MyTokenHolder tokenHolder;

    public UlearnAuthService(ISolution solution, Lifetime lifetime)
    {
        
        tokenHolder = new MyTokenHolder();
        var authProvider = new BaseBearerTokenAuthenticationProvider(tokenHolder);
        var httpClient = new HttpClient();
        lifetime.AddDispose(httpClient);
        httpClient.DefaultRequestHeaders.UserAgent.ParseAdd(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        var requestAdapter = new HttpClientRequestAdapter(authProvider, httpClient: httpClient);
        apiClient = new ApiClient(requestAdapter);
        
        var model = solution.GetProtocolSolution().GetUlearnModel();
        model.Login.Set((reqLifetime, request) => HandleLoginAsync(request, reqLifetime));
    }

    private async Task<LoginResponse> HandleLoginAsync(LoginRequest request, Lifetime reqLifetime)
    {
        try
        {
            var loginData = new LoginPasswordParameters
            {
                Login = request.Login, Password = request.Password
            };
            var cancellationToken = reqLifetime.ToCancellationToken();
            var loginResponse = await RetryAsync(() => apiClient.Account.Login.PostAsync(loginData, cancellationToken: cancellationToken), maxRetries: 3);
            if (loginResponse?.Token != null)
            {
                tokenHolder.CurrentToken = loginResponse.Token;
                return new LoginResponse(true, "Успешный вход!");
            }
            return new LoginResponse(false, "Не удалось получить токен от сервера.");
        }
        catch (Microsoft.Kiota.Abstractions.ApiException apiEx)
        {
            //ошибки от API Ulearn
            return new LoginResponse(false, $"Ошибка сервера ({apiEx.ResponseStatusCode}): {apiEx.Message}");
        }
        catch (Exception ex)
        {
            return new LoginResponse(false, $"Ошибка: {ex.Message}");
        }
        
    }
    
    private static async Task<T> RetryAsync<T>(Func<Task<T>> action, int maxRetries = 3)
    {
        var delaySeconds = 2;

        for (var attempt = 1; attempt <= maxRetries; attempt++)
        {
            try
            {
                return await action();
            }
            catch (Exception) when (attempt < maxRetries)
            {
                await Task.Delay(TimeSpan.FromSeconds(delaySeconds));
                delaySeconds *= 2;
            }
        }

        return await action();
    }
}