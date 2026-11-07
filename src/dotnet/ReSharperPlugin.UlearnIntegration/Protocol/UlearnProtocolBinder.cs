using System;
using System.Net.Http;
using System.Threading.Tasks;
using JetBrains.Application.Parts;
using JetBrains.Application.Threading;
using JetBrains.Core;
using JetBrains.ProjectModel;
using JetBrains.Rider.Model;
using JetBrains.Lifetimes;
using JetBrains.Rd.Tasks;
using JetBrains.ReSharper.Feature.Services.Protocol;
using Microsoft.Kiota.Abstractions.Authentication;
using Microsoft.Kiota.Http.HttpClientLibrary;
using ReSharperPlugin.UlearnIntegration.Application.Auth;
using ReSharperPlugin.UlearnIntegration.Application.Interfaces;
using ReSharperPlugin.UlearnIntegration.Infrastructure.ApiClients;
using ReSharperPlugin.UlearnIntegration.Infrastructure.Tokens;
using UlearnApi.Client;

namespace ReSharperPlugin.UlearnIntegration.Protocol;


[SolutionComponent(Instantiation.ContainerAsyncAnyThreadSafe)]
public class UlearnProtocolBinder
{
    private const string UserAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private readonly UlearnModel model;
    private readonly LoginService loginService;
    private readonly ITokenStorage tokenStorage;
    private readonly IShellLocks locks;
    private readonly Lifetime lifetime;

    public UlearnProtocolBinder(
        ISolution solution,
        Lifetime lifetime,
        IShellLocks locks)
    {
        this.lifetime = lifetime;
        this.locks = locks;

        var tockenProvider = new UserTokenProvider();
        tokenStorage = tockenProvider;
        
        var authProvider = new BaseBearerTokenAuthenticationProvider(tockenProvider);

        var httpClient = new HttpClient();
        lifetime.AddDispose(httpClient);
        httpClient.DefaultRequestHeaders.UserAgent.ParseAdd(UserAgent);
        
        var requestAdapter = new HttpClientRequestAdapter(authProvider, httpClient: httpClient);
        var apiClient = new ApiClient(requestAdapter);
        
        IAuthApiClient authApiClient1 = new AuthApiClient(apiClient);
        loginService = new LoginService(authApiClient1, tokenStorage);
        
        model = solution.GetProtocolSolution().GetUlearnModel();
        
        model.Login.Set((reqLifetime, request) => HandleLoginAsync(request, reqLifetime));
        model.Logout.Set((_, _) => HandleLogout());
    }
    
    private async Task<LoginResponse> HandleLoginAsync(LoginRequest request, Lifetime reqLifetime)
    {
        var ct = reqLifetime.ToCancellationToken();

        try
        {
            var userProfile = await loginService.ExecuteAsync(request.Login, request.Password, ct);
            var userInfo = new UserInfo(userProfile.Id, userProfile.Name, userProfile.Login);
            locks.ExecuteOrQueue(lifetime, "Ulearn.SetUser", () => 
            {
                model.CurrentUser.Value = userInfo;
            });

            return new LoginResponse(true, "Успешный вход");
        }
        catch (Exception ex)
        {
            return new LoginResponse(false, ex.Message);
        }
    }
    
    private Unit HandleLogout()
    {
        tokenStorage.SetToken("");
        locks.ExecuteOrQueue(lifetime, "Ulearn.ClearUser", () =>
        {
            model.CurrentUser.Value = null;
        });

        return Unit.Instance;
    }
}