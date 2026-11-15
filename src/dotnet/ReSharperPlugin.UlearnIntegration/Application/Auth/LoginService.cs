using System.Threading;
using System.Threading.Tasks;
using ReSharperPlugin.UlearnIntegration.Application.Interfaces;
using ReSharperPlugin.UlearnIntegration.Domain.Models;

namespace ReSharperPlugin.UlearnIntegration.Application.Auth;

public class LoginService(IAuthApiClient authApiClient, ITokenStorage tokenStorage)
{
    public async Task<UserProfile> ExecuteAsync(string login, string password, CancellationToken ct)
    {
        var token = await authApiClient.LoginAsync(login, password, ct);
        await tokenStorage.SaveTokenAsync(token, ct);
        
        var accountInfo = await authApiClient.GetAccountInfoAsync(ct);
        return new UserProfile(accountInfo.Id, accountInfo.Login, accountInfo.Name);
    }
}