using System;
using System.Collections.Generic;
using System.IO;
using System.Runtime.InteropServices;
using System.Security.Cryptography;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Application;
using Microsoft.Kiota.Abstractions.Authentication;
using ReSharperPlugin.UlearnIntegration.Application.Interfaces;

namespace ReSharperPlugin.UlearnIntegration.Infrastructure.Tokens;

[ShellComponent]
public class UserTokenProvider: IAccessTokenProvider, ITokenStorage
{
    private readonly string tokenDirectory;
    private readonly string tokenFilePath;
    private string cachedToken;
    
    private readonly object sync = new();
    
    private DateTime? cachedStamp;
    
    public UserTokenProvider()
    {
        var appData = Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData);
        tokenDirectory = Path.Combine(appData, "UlearnIntegrationRider");
        tokenFilePath = Path.Combine(tokenDirectory, "auth_token.dat");
    }
    
    public AllowedHostsValidator AllowedHostsValidator { get; } =
        new(["ulearn.me", "api.ulearn.me"]);
    
    private static bool IsWindows => RuntimeInformation.IsOSPlatform(OSPlatform.Windows);

    public Task<string> GetTokenAsync(CancellationToken ct = default) => Task.FromResult(GetToken());
    
    public Task<string> GetAuthorizationTokenAsync(
        Uri uri,
        Dictionary<string, object> additionalAuthenticationContext = null,
        CancellationToken cancellationToken = default)
    {
        if (!AllowedHostsValidator.IsUrlHostValid(uri))
            throw new ArgumentException($"The uri {uri} is not valid.");
        
        return Task.FromResult(GetToken());
    }
    
    private string GetToken()
    {
        lock (sync)
        {
            try
            {
                if (!File.Exists(tokenFilePath))
                {
                    cachedToken = null;
                    cachedStamp = null;
                    return null;
                }

                var stamp = File.GetLastWriteTimeUtc(tokenFilePath);
                if (stamp == cachedStamp)
                    return cachedToken;

                cachedToken = Decode(File.ReadAllBytes(tokenFilePath));
                cachedStamp = stamp;
                return cachedToken;
            }
            catch (IOException)
            {
                return cachedToken;
            }
            catch (UnauthorizedAccessException)
            {
                return cachedToken;
            }
        }
    }

    public Task SaveTokenAsync(string token, CancellationToken ct = default)
    {
        ArgumentException.ThrowIfNullOrEmpty(token);

        lock (sync)
        {
            WriteTokenToFile(token);
            cachedToken = null;
            cachedStamp = null;
        }

        return Task.CompletedTask;
    }

    public Task ClearTokenAsync(CancellationToken ct = default)
    {
        lock (sync)
        {
            File.Delete(tokenFilePath);
            cachedToken = null;
            cachedStamp = null;
        }

        return Task.CompletedTask;
    }

    private void WriteTokenToFile(string token)
    {
        Directory.CreateDirectory(tokenDirectory);
        
        var data = Encode(token);
        using var fs = new FileStream(tokenFilePath, FileMode.Create, FileAccess.Write, FileShare.None);
        
        if (!IsWindows)
            RestrictToOwner(tokenFilePath);
        
        fs.Write(data, 0, data.Length);
    }
    
    [DllImport("libc", EntryPoint = "chmod", SetLastError = true)]
    private static extern int Chmod(string path, uint mode);

    private static void RestrictToOwner(string path)
    {
        const uint ownerReadWrite = 0x180;

        if (Chmod(path, ownerReadWrite) != 0)
            throw new IOException(
                $"Не удалось выставить права 600 на файл токена (errno {Marshal.GetLastWin32Error()}).");
    }
    
    private static byte[] Encode(string token)
    {
        var bytes = Encoding.UTF8.GetBytes(token);
        return IsWindows
            ? ProtectedData.Protect(bytes, null, DataProtectionScope.CurrentUser)
            : bytes;
    }
    
    private static string Decode(byte[] raw)
    {
        if (raw.Length == 0)
            return null;

        try
        {
            var bytes = IsWindows
                ? ProtectedData.Unprotect(raw, null, DataProtectionScope.CurrentUser)
                : raw;

            var token = Encoding.UTF8.GetString(bytes).Trim();
            return token.Length == 0 ? null : token;
        }
        catch (CryptographicException)
        {
            return null;
        }
    }
    
}