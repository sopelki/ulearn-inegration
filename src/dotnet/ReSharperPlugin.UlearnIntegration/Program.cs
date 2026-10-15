// // See https://aka.ms/new-console-template for more information
//
// using System;
// using System.Collections.Generic;
// using System.Net.Http;
// using System.Threading;
// using System.Threading.Tasks;
// using Microsoft.Kiota.Abstractions.Authentication;
// using Microsoft.Kiota.Http.HttpClientLibrary;
// using UlearnApi.Client;
// using UlearnApi.Client.Models;
//
//
// public class MyTokenHolder : IAccessTokenProvider
// {
// 	public string CurrentToken { get; set; } = "";
// 	public Task<string> GetAuthorizationTokenAsync(
// 		Uri uri, 
// 		Dictionary<string, object>? additionalAuthenticationContext = null, 
// 		CancellationToken cancellationToken = default)
// 	{
// 		return Task.FromResult(CurrentToken);
// 	}
//
// 	public AllowedHostsValidator AllowedHostsValidator { get; } = new(); // список куда можно отправлять токен(по умолчанию везде)
// }
//
// public class Program
// {
// 	public static async Task Main()
// 	{
// 		var tokenHolder = new MyTokenHolder();
// 		var authProvider = new BaseBearerTokenAuthenticationProvider(tokenHolder);
// 		var httpClient = new HttpClient();
// 		httpClient.DefaultRequestHeaders.UserAgent.ParseAdd("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
// 		var requestAdapter = new HttpClientRequestAdapter(authProvider, httpClient: httpClient);
// 		var apiClient = new ApiClient(requestAdapter);
// 		
// 		var loginData = new LoginPasswordParameters() // Юлин логин/пароль от ненужного аккаунта
// 		{
// 			Login = "parus",
// 			Password = "02072007"
// 		};
// 		
// 		try
// 		{
// 			Console.WriteLine("Начинаем процесс авторизации...");
// 			var loginResponse =  await RetryAsync(() => apiClient.Account.Login.PostAsync(loginData), maxRetries: 5);
// 			tokenHolder.CurrentToken = loginResponse?.Token ?? ""; 
// 			Console.WriteLine("Авторизация успешна!");
// 			var webEditorData = await apiClient.Account.GetAsync();
// 			if (webEditorData is not null)
// 			{
// 				Console.WriteLine("Запрос аккаунта прошёл успешно!");
// 				Console.WriteLine(webEditorData.User!.Login);
// 				Console.WriteLine(webEditorData.User.Email);
// 				Console.WriteLine(webEditorData.User.Id);
// 			}
// 		}
// 		catch (Exception ex)
// 		{
// 			Console.WriteLine($"Ошибка при логине: {ex.Message}");
// 		}
// 	}
//
// 	private static async Task<T> RetryAsync<T>(Func<Task<T>> action, int maxRetries = 3)
// 	{
// 		var delaySeconds = 2;
//
// 		for (var attempt = 1; attempt <= maxRetries; attempt++)
// 		{
// 			try
// 			{
// 				return await action();
// 			}
// 			catch (Exception ex) when (attempt < maxRetries)
// 			{
// 				Console.WriteLine($"[Retry] Попытка {attempt} из {maxRetries} не удалась ({ex.Message}).");
// 				Console.WriteLine($"[Retry] Следующая попытка через {delaySeconds} сек...");
// 				
// 				await Task.Delay(TimeSpan.FromSeconds(delaySeconds));
// 				delaySeconds *= 2;
// 			}
// 		}
// 		
// 		return await action(); 
// 	}
// }
