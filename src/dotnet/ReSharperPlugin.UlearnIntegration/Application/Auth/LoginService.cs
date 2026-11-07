using System.Threading;
using System.Threading.Tasks;
using ReSharperPlugin.UlearnIntegration.Application.Interfaces;
using ReSharperPlugin.UlearnIntegration.Domain.Models;

namespace ReSharperPlugin.UlearnIntegration.Application.Auth;

public class LoginService
{
    private readonly IAuthApiClient authApiClient;
    private readonly ITokenStorage tokenStorage;
    
    public LoginService(IAuthApiClient authApiClient, ITokenStorage tokenStorage)
    {
        this.authApiClient = authApiClient;
        this.tokenStorage = tokenStorage;
    }
    
    public async Task<UserProfile> ExecuteAsync(string login, string password, CancellationToken ct)
    {
        var token = await authApiClient.LoginAsync(login, password, ct);
        tokenStorage.SetToken(token);
        
        var accountInfo = await authApiClient.GetAccountInfoAsync(ct);
        return new UserProfile(accountInfo.Id, accountInfo.Login, accountInfo.Name);
    }
}