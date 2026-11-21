using System;
using System.Linq;
using System.Net.Http;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Application.Parts;
using JetBrains.Application.Threading;
using JetBrains.ProjectModel;
using JetBrains.Rider.Model;
using JetBrains.Lifetimes;
using JetBrains.Rd.Tasks;
using JetBrains.ReSharper.Feature.Services.Protocol;
using Microsoft.Kiota.Abstractions;
using Microsoft.Kiota.Abstractions.Authentication;
using Microsoft.Kiota.Http.HttpClientLibrary;
using ReSharperPlugin.UlearnIntegration.Application.Auth;
using ReSharperPlugin.UlearnIntegration.Application.Interfaces;
using ReSharperPlugin.UlearnIntegration.Application.Practices;
using ReSharperPlugin.UlearnIntegration.Domain.Models;
using ReSharperPlugin.UlearnIntegration.Infrastructure.ApiClients;
using ReSharperPlugin.UlearnIntegration.Infrastructure.Tokens;
using UlearnApi.Client;
using Unit = JetBrains.Core.Unit;

namespace ReSharperPlugin.UlearnIntegration.Protocol;

[SolutionComponent(Instantiation.ContainerAsyncAnyThreadSafe)]
public class UlearnProtocolBinder
{
    private const string UserAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private readonly UlearnModel model;
    private readonly LoginService loginService;
    private readonly ITokenStorage tokenStorage;
    private readonly IAuthApiClient authApiClient;
    private readonly IShellLocks locks;
    private readonly Lifetime lifetime;
    private readonly PracticeService practiceService;

    public UlearnProtocolBinder(
        ISolution solution,
        Lifetime lifetime,
        IShellLocks locks,
        ITokenStorage tokenStorage,
        IPracticeContextProvider contextProvider)
    {
        this.lifetime = lifetime;
        this.locks = locks;
        this.tokenStorage = tokenStorage;

        var authProvider = new BaseBearerTokenAuthenticationProvider((UserTokenProvider)tokenStorage);

        var httpClient = new HttpClient();
        lifetime.AddDispose(httpClient);
        httpClient.DefaultRequestHeaders.UserAgent.ParseAdd(UserAgent);

        var requestAdapter = new HttpClientRequestAdapter(authProvider, httpClient: httpClient);
        var apiClient = new ApiClient(requestAdapter);

        authApiClient = new AuthApiClient(apiClient);
        loginService = new LoginService(authApiClient, tokenStorage);
        
        var practiceApiClient = new PracticeApiClient(apiClient, requestAdapter);
        practiceService = new PracticeService(contextProvider, practiceApiClient);

        model = solution.GetProtocolSolution().GetUlearnModel();

        model.Login.Set((reqLifetime, request) => HandleLoginAsync(request, reqLifetime));
        model.Logout.Set((_, _) => HandleLogoutAsync());
        model.GetPractice.Set((reqLifetime, request) => HandleGetPracticeAsync(request, reqLifetime));
        
        RestoreSessionAsync();

    }

    private async void RestoreSessionAsync()
    {
        try
        {
            var token = await tokenStorage.GetTokenAsync(lifetime);
            if (string.IsNullOrEmpty(token)) return;

            var accountInfo = await authApiClient.GetAccountInfoAsync(CancellationToken.None);
            var userProfile = new UserProfile(accountInfo.Id, accountInfo.Login, accountInfo.Name);
            var userInfo = new UserInfo(userProfile.Id, userProfile.Name, userProfile.Login);
            locks.ExecuteOrQueue(lifetime, "Ulearn.SetUser", () => { model.CurrentUser.Value = userInfo; });
        }
        catch (ApiException ex) when (ex.ResponseStatusCode == 401)
        {
            await tokenStorage.ClearTokenAsync(lifetime);
        }
        catch (Exception)
        {
        }
    }

    private async Task<LoginResponse> HandleLoginAsync(LoginRequest request, Lifetime reqLifetime)
    {
        var ct = reqLifetime.ToCancellationToken();

        try
        {
            var userProfile = await loginService.ExecuteAsync(request.Login, request.Password, ct);
            var userInfo = new UserInfo(userProfile.Id, userProfile.Name, userProfile.Login);
            locks.ExecuteOrQueue(lifetime, "Ulearn.SetUser", () => { model.CurrentUser.Value = userInfo; });

            return new LoginResponse(true, "Успешный вход");
        }
        catch (Exception ex)
        {
            return new LoginResponse(false, ex.Message);
        }
    }
    
    private async Task<Unit> HandleLogoutAsync()
    {
        await tokenStorage.ClearTokenAsync(lifetime);
        locks.ExecuteOrQueue(lifetime, "Ulearn.ClearUser", () => { model.CurrentUser.Value = null; });
        return Unit.Instance;
    }
    
    private async Task<PracticeResponse> HandleGetPracticeAsync(PracticeRequest request, Lifetime reqLifetime)
    {
        var ct = reqLifetime.ToCancellationToken();

        try
        {
            var practice = request.Refresh
                ? await practiceService.RefreshPracticeAsync(ct)
                : await practiceService.GetPracticeAsync(ct);

            if (practice == null)
                return new PracticeResponse(true, "В этом solution нет файла .ulearn с выбранной практикой.", null);

            var info = new PracticeInfo(practice.Id, practice.Title, practice.HtmlContent, practice.Hints.ToArray());
            return new PracticeResponse(true, string.Empty, info);
        }
        catch (OperationCanceledException)
        {
            throw;
        }
        catch (Exception ex)
        {
            return new PracticeResponse(false, ex.Message, null);
        }
    }
}