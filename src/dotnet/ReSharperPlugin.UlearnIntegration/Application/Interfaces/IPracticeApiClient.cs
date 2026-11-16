using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using ReSharperPlugin.UlearnIntegration.Domain.Models;

public interface IPracticeApiClient
{ 
    Task<Practice> GetSelectedPracticeFromFileAsync(CancellationToken ct = default);
}