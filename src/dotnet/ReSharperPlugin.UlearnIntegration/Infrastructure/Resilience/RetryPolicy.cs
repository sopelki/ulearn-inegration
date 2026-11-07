using System;
using System.Threading.Tasks;

namespace ReSharperPlugin.UlearnIntegration.Infrastructure.Resilience;

public class RetryPolicy
{
    private const int DefaultMaxRetries = 3;
    private const int DefaultBaseDelaySeconds = 2;
    private const int ExponentialStep = 3;
    
    public static async Task<T> ExecuteAsync<T>(
        Func<Task<T>> action, 
        int maxRetries = DefaultMaxRetries, 
        int baseDelaySeconds = DefaultBaseDelaySeconds)
    {
        var attempt = 0;
        while (true)
        {
            try
            {
                return await action();
            }
            catch (Exception) when (attempt < maxRetries)
            {
                attempt++;
                var delay = TimeSpan.FromSeconds(Math.Pow(ExponentialStep, attempt - 1) * baseDelaySeconds);
                await Task.Delay(delay);
            }
        }
    }
}