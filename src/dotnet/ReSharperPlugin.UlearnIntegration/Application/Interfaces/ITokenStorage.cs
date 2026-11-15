using System.Threading;
using System.Threading.Tasks;

namespace ReSharperPlugin.UlearnIntegration.Application.Interfaces;

public interface ITokenStorage
{
    Task<string> GetTokenAsync(CancellationToken ct =  default);
    Task SaveTokenAsync(string token, CancellationToken ct =  default);
    Task ClearTokenAsync(CancellationToken ct =  default);
}