using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Kiota.Abstractions.Authentication;
using ReSharperPlugin.UlearnIntegration.Application.Interfaces;

namespace ReSharperPlugin.UlearnIntegration.Infrastructure.Tokens;

public class UserTokenProvider : IAccessTokenProvider, ITokenStorage
{
    private string currentToken = "";
    
    public string GetToken() => string.IsNullOrEmpty(currentToken) ? null : currentToken;
    
    public void SetToken(string token)
    {
        currentToken = token ?? throw new ArgumentNullException(nameof(token));
    }

    public Task<string> GetAuthorizationTokenAsync(
        Uri uri,
        Dictionary<string, object> additionalAuthenticationContext = null,
        CancellationToken cancellationToken = default) =>
        AllowedHostsValidator.IsUrlHostValid(uri)
            ? Task.FromResult(currentToken)
            : throw new ArgumentException($"The uri {uri} is not valid.");

    public AllowedHostsValidator AllowedHostsValidator { get; } =
        new(["ulearn.me", "api.ulearn.me"]);
}