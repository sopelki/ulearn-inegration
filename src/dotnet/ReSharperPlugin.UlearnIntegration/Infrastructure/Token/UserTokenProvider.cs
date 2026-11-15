using System;
using System.Collections.Generic;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Application;
using Microsoft.Kiota.Abstractions.Authentication;
using ReSharperPlugin.UlearnIntegration.Application.Interfaces;

namespace ReSharperPlugin.UlearnIntegration.Infrastructure.Tokens;

[ShellComponent]
public class UserTokenProvider: IAccessTokenProvider, ITokenStorage
{
    private readonly string tokenFilePath;
    private string cachedToken;
    private bool isInitialized;
    public UserTokenProvider()
    {
        var appData = Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData);
        var dir = Path.Combine(appData, "UlearnIntegrationRider");
        
        Directory.CreateDirectory(dir);
        tokenFilePath = Path.Combine(dir, "auth_token.dat");
    }

    public Task<string> GetTokenAsync(CancellationToken ct = default)
    {
        if (isInitialized)
            return Task.FromResult(string.IsNullOrEmpty(cachedToken) ? null : cachedToken);
        
        cachedToken = ReadTokenFromFile();
        isInitialized = true;
        return Task.FromResult(string.IsNullOrEmpty(cachedToken) ? null : cachedToken);
    }

    public Task SaveTokenAsync(string token, CancellationToken ct = default)
    {
        ArgumentNullException.ThrowIfNull(token);

        cachedToken = token;
        isInitialized = true;
        WriteTokenToFile(token);

        return Task.CompletedTask;
    }

    public Task ClearTokenAsync(CancellationToken ct = default)
    {
        cachedToken = null;
        isInitialized = false;
        WriteTokenToFile(string.Empty);

        return Task.CompletedTask;
    }

    public Task<string> GetAuthorizationTokenAsync(
        Uri uri,
        Dictionary<string, object> additionalAuthenticationContext = null,
        CancellationToken cancellationToken = default)
    {
        if (!AllowedHostsValidator.IsUrlHostValid(uri))
            throw new ArgumentException($"The uri {uri} is not valid.");
        
        if (!isInitialized)
        {
            cachedToken = ReadTokenFromFile();
            isInitialized = true;
        }
        
        return Task.FromResult(string.IsNullOrEmpty(cachedToken) ? null : cachedToken);
    }

    public AllowedHostsValidator AllowedHostsValidator { get; } =
        new(["ulearn.me", "api.ulearn.me"]);
    
    private string ReadTokenFromFile()
    {
        try
        {
            if (File.Exists(tokenFilePath))
            {
                var token = File.ReadAllText(tokenFilePath).Trim();
                return string.IsNullOrEmpty(token) ? null : token;
            }
        }
        catch (Exception ex)
        {
            if (File.Exists(tokenFilePath))
                File.Delete(tokenFilePath);
        }
        
        return null;
    }

    private void WriteTokenToFile(string token)
    {
        try
        {
            var directory = Path.GetDirectoryName(tokenFilePath);
            if (!string.IsNullOrEmpty(directory) && !Directory.Exists(directory))
            {
                Directory.CreateDirectory(directory);
            }

            File.WriteAllText(tokenFilePath, token ?? string.Empty);
        }
        catch (Exception ex)
        {
            if (File.Exists(tokenFilePath))
                File.Delete(tokenFilePath);
        }
    }
}