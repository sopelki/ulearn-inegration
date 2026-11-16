using System.Threading;
using System.Threading.Tasks;

namespace ReSharperPlugin.UlearnIntegration.Application.Interfaces;

public interface IAuthApiClient
{
    Task<string> LoginAsync(string login, string password, CancellationToken ct);
    Task<AccountDto> GetAccountInfoAsync(CancellationToken ct);
}